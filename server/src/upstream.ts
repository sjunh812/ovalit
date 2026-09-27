import { ApiError } from "./errors";

// Retry-After 없이 오는 429도 있다. 그때는 이만큼 쉬고 다시 부른다.
const DEFAULT_RETRY_AFTER_SECONDS = 10;

// Riot 레이트 리밋은 사용자마다가 아니라 앱 전체에 걸린다. 429를 받고도 계속 부르면 막힌 동안에도 몫이 깎이니,
// 호스트마다 풀리는 시각을 isolate 메모리에 두고 그때까지는 부르지 않는다.
const blockedUntil = new Map<string, number>();

/**
 * Riot으로 요청을 보내고 실패를 앱이 읽을 코드로 바꿉니다. 429는 `Retry-After`를 붙인 503 `riot_rate_limited`이고,
 * 401·403(우리 키 문제), 5xx, 연결 실패는 모두 502 `riot_unavailable`입니다. 429를 받은 호스트는 `Retry-After`가
 * 지날 때까지 Riot을 부르지 않고 바로 503 `riot_rate_limited`를 던집니다.
 */
export async function send(upstream: typeof fetch, url: string, init?: RequestInit): Promise<Response> {
  throwIfBlocked(url);
  let res: Response;
  try {
    res = await upstream(url, init);
  } catch {
    throw new ApiError(502, "riot_unavailable");
  }
  if (res.ok) return res;
  if (res.status === 429) {
    const seconds = retryAfterSeconds(res.headers.get("Retry-After"));
    blockedUntil.set(new URL(url).host, Date.now() + seconds * 1000);
    throw rateLimited(seconds);
  }
  if (res.status === 404) throw new ApiError(404, "not_found");
  throw new ApiError(502, "riot_unavailable");
}

/** `url`의 호스트가 Riot 레이트 리밋에 걸려 있으면 503 `riot_rate_limited`를 던집니다. Riot은 부르지 않습니다. */
export function throwIfBlocked(url: string): void {
  const host = new URL(url).host;
  const until = blockedUntil.get(host);
  if (until === undefined) return;
  const remaining = until - Date.now();
  if (remaining <= 0) {
    blockedUntil.delete(host);
    return;
  }
  throw rateLimited(Math.ceil(remaining / 1000));
}

/** 테스트끼리 막힌 호스트가 이어지지 않게 비울 때 씁니다. */
export function clearRiotBlocks(): void {
  blockedUntil.clear();
}

function retryAfterSeconds(header: string | null): number {
  const seconds = Number(header);
  return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : DEFAULT_RETRY_AFTER_SECONDS;
}

function rateLimited(seconds: number): ApiError {
  return new ApiError(503, "riot_rate_limited", { "Retry-After": String(seconds) });
}
