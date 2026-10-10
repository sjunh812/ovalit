import { ApiError } from "./errors";
import { logFailure } from "./log";

// Retry-After 없이 오는 application·method 429도 있다. 그때는 이만큼 쉬고 다시 부른다.
const DEFAULT_RETRY_AFTER_SECONDS = 10;
// service 429는 Riot 뒤쪽 서비스가 바쁘다는 뜻이라 우리 키의 몫과 상관없다. 이 isolate에서만 그 경로를 잠깐 쉰다.
const SERVICE_BACKOFF_SECONDS = 5;
// 다른 isolate가 받은 429를 D1에서 읽어 오는 간격이다. 막힌 동안 다른 isolate가 이만큼은 더 부를 수 있다.
const SYNC_INTERVAL_MS = 5_000;
// PUUID(78자)와 경기 ID(UUID 36자)처럼 경로에 끼운 값이다. 같은 method 한도를 쓰는 경로를 하나로 묶는다.
const ID_SEGMENT = /^[A-Za-z0-9_-]{32,}$/;

// Riot 레이트 리밋은 사용자마다가 아니라 앱 전체에 걸린다.
// 429를 받고도 계속 부르면 막힌 동안에도 몫이 깎이고, 되풀이하면 키가 막힐 수 있다.
// 막힌 범위(호스트나 호스트와 경로 틀)마다 풀리는 시각을 isolate 메모리에 두고, 다른 isolate와는 D1 `riot_blocks`로 나눈다.
const blockedUntil = new Map<string, number>();
let syncedAt = 0;

/**
 * Riot으로 요청을 보내고 실패를 앱이 읽을 코드로 바꿉니다.
 * 429는 `Retry-After`를 붙인 503 `riot_rate_limited`이고, 404는 404 `not_found`, 401·403(우리 키 문제), 5xx, 연결 실패는 모두 502 `riot_unavailable`입니다.
 *
 * 429는 `X-Rate-Limit-Type`에 따라 막는 범위가 다릅니다.
 * `application`은 우리 키의 몫이라 그 호스트 전체를, `method`는 그 경로만 `Retry-After`까지 막고 D1에 적어 다른 isolate도 부르지 않게 합니다.
 * `service`나 종류가 없는 429는 Riot 쪽 서비스가 바쁜 것이라 이 isolate에서만 그 경로를 잠깐 쉽니다.
 */
export async function send(upstream: typeof fetch, db: D1Database, url: string, init?: RequestInit): Promise<Response> {
  await throwIfBlocked(db, url);
  let res: Response;
  try {
    res = await upstream(url, init);
  } catch {
    throw new ApiError(502, "riot_unavailable");
  }
  if (res.ok) return res;
  // 실패한 응답의 본문은 읽지 않는다. 그대로 두면 연결이 본문을 다 받을 때까지 묶여 있다.
  await res.body?.cancel().catch(() => {});
  if (res.status === 429) throw await rateLimitedBy(db, url, res);
  if (res.status === 404) throw new ApiError(404, "not_found");
  throw new ApiError(502, "riot_unavailable");
}

/**
 * `url`이 Riot 레이트 리밋에 걸린 범위면 503 `riot_rate_limited`를 던집니다. Riot은 부르지 않습니다.
 * 다른 isolate가 적은 차단은 5초에 한 번까지만 D1에서 읽어 옵니다.
 */
export async function throwIfBlocked(db: D1Database, url: string): Promise<void> {
  await sync(db);
  const { host, method } = scopes(url);
  const now = Date.now();
  const until = Math.max(activeUntil(host, now), activeUntil(method, now));
  if (until > now) throw rateLimited(Math.ceil((until - now) / 1000));
}

/** 테스트가 isolate 메모리의 차단을 비울 때 씁니다. D1에 적은 차단은 남으니 새로 뜬 isolate처럼 다시 읽어 옵니다. */
export function clearRiotBlocks(): void {
  blockedUntil.clear();
  syncedAt = 0;
}

async function rateLimitedBy(db: D1Database, url: string, res: Response): Promise<ApiError> {
  const type = res.headers.get("X-Rate-Limit-Type");
  const retryAfter = retryAfterSeconds(res.headers.get("Retry-After"));
  const { host, method } = scopes(url);
  if (type === "application" || type === "method") {
    const seconds = retryAfter ?? DEFAULT_RETRY_AFTER_SECONDS;
    const scope = type === "application" ? host : method;
    const until = Date.now() + seconds * 1000;
    block(scope, until);
    await share(db, scope, until);
    return rateLimited(seconds);
  }
  const seconds = retryAfter ?? SERVICE_BACKOFF_SECONDS;
  block(method, Date.now() + seconds * 1000);
  return rateLimited(seconds);
}

// 429를 받을 때만 한 줄 쓴다. 범위마다 한 줄이라 늘어나지 않는다.
async function share(db: D1Database, scope: string, until: number): Promise<void> {
  try {
    await db
      .prepare(
        `INSERT INTO riot_blocks (scope, blocked_until) VALUES (?, ?)
         ON CONFLICT (scope) DO UPDATE SET blocked_until = MAX(blocked_until, excluded.blocked_until)`,
      )
      .bind(scope, until)
      .run();
  } catch (err) {
    logFailure("riot_blocks", err);
  }
}

// 요청이 몰려도 한 isolate에서 5초에 한 번만 읽는다. 읽다 실패하면 이 isolate가 아는 차단만으로 간다.
async function sync(db: D1Database): Promise<void> {
  const now = Date.now();
  if (now - syncedAt < SYNC_INTERVAL_MS) return;
  syncedAt = now;
  try {
    const { results } = await db
      .prepare("SELECT scope, blocked_until FROM riot_blocks WHERE blocked_until > ?")
      .bind(now)
      .all<{ scope: string; blocked_until: number }>();
    for (const row of results) block(row.scope, row.blocked_until);
  } catch (err) {
    logFailure("riot_blocks", err);
  }
}

function block(scope: string, until: number): void {
  blockedUntil.set(scope, Math.max(blockedUntil.get(scope) ?? 0, until));
}

function activeUntil(scope: string, now: number): number {
  const until = blockedUntil.get(scope);
  if (until === undefined) return 0;
  if (until <= now) {
    blockedUntil.delete(scope);
    return 0;
  }
  return until;
}

function scopes(url: string): { host: string; method: string } {
  const { host, pathname } = new URL(url);
  const template = pathname
    .split("/")
    .map((segment) => (ID_SEGMENT.test(segment) ? "{id}" : segment))
    .join("/");
  return { host, method: `${host}${template}` };
}

function retryAfterSeconds(header: string | null): number | undefined {
  const seconds = Number(header);
  return header !== null && Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : undefined;
}

function rateLimited(seconds: number): ApiError {
  return new ApiError(503, "riot_rate_limited", { "Retry-After": String(seconds) });
}
