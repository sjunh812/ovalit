import type { Context } from "hono";
import type { AppEnv, Env, User } from "./env";
import { ApiError } from "./errors";
import { MemoryCache, Quota } from "./memory";
import { send, throwIfBlocked } from "./upstream";

const RIOT_HOST = "https://kr.api.riotgames.com";
const DAY = 24 * 60 * 60;
// 끝난 경기는 결과가 바뀌지 않는다. 다시 받으면 레이트 리밋만 쓴다.
const MATCH_TTL = 30 * DAY;
const CONTENT_TTL = 6 * 60 * 60;
const STATUS_TTL = 60;
// Riot이 없다고 한 경기 ID를 기억해 두고, 아무 ID나 넣은 요청이 그때마다 Riot 몫을 쓰지 않게 한다.
// 막 끝난 경기가 잠깐 404일 수도 있어서 길게 두지 않는다.
const MISSING_TTL = 10 * 60;
const MB = 1024 * 1024;
// D1은 쿼리당 바인딩을 100개까지 받아서 INSERT 하나에 50쌍까지 넣는다.
const PLAYERS_PER_INSERT = 50;

// isolate가 살아 있는 동안 요청끼리 나눠 쓴다. isolate 메모리는 128MB라 캐시는 모두 합쳐 40MB 안쪽으로 두고 나머지는
// 요청을 처리하는 데 남긴다. 경기 원문은 한 판에 800KB 안팎이라 글자당 2바이트로 세도 15판쯤 담긴다.
const memory = {
  content: new MemoryCache({ maxEntries: 1, maxBytes: 10 * MB }),
  status: new MemoryCache({ maxEntries: 1, maxBytes: 1 * MB }),
  matches: new MemoryCache({ maxEntries: Number.POSITIVE_INFINITY, maxBytes: 24 * MB }),
  missing: new MemoryCache({ maxEntries: 10_000, maxBytes: Number.POSITIVE_INFINITY }),
};

// 한 사람이 Riot 몫을 몰아 쓰지 못하게 사용자마다 센다. isolate 메모리에서 세니 막는다기보다 줄이는 정도다.
const quotas = {
  // 경기 ID 목록은 새 경기가 바로 보여야 해서 담아 두지 않는다. 대신 연달아 당겨도 10초에 한 번만 Riot에 간다.
  matchlist: new Quota(1, 10_000),
  // 앱도 같은 친구의 경기는 1분에 한 번까지만 새로 받는다.
  friendMatchlist: new Quota(1, 60_000),
  // 캐시에 없어 Riot에 가는 경기 상세다. 첫 수집 50경기가 한꺼번에 와도 걸리지 않게 넉넉히 둔다.
  matchDetails: new Quota(120, 60_000),
};

/** 테스트가 메모리 캐시를 비우고 D1이나 Cache API만으로 도는지 볼 때 씁니다. */
export function clearMemoryCaches(): void {
  for (const cache of Object.values(memory)) cache.clear();
}

/** 테스트끼리 사용자 호출 한도가 이어지지 않게 비울 때 씁니다. */
export function clearQuotas(): void {
  for (const quota of Object.values(quotas)) quota.clear();
}

/** Cache API는 요청이 들어온 도메인 안에서만 담기므로 키 주소도 그 도메인으로 만듭니다. */
export function cacheKey(origin: string, path: string): Request {
  return new Request(`${origin}/__riot${path}`);
}

export interface MatchData {
  matchInfo?: { isCompleted?: boolean };
  players?: { puuid?: unknown }[];
  [key: string]: unknown;
}

/** Riot이 준 경기 원문입니다. 가릴 것이 없으면 파싱하지도 다시 직렬화하지도 않고 그대로 내려보냅니다. */
export class RiotMatch {
  private parsed: MatchData | undefined;
  private knownPlayers: Set<string> | undefined;

  constructor(
    readonly raw: string,
    parsed?: MatchData,
    players?: Set<string>,
  ) {
    this.parsed = parsed;
    this.knownPlayers = players;
  }

  /** 처음 읽을 때 원문을 파싱합니다. 요청마다 새로 만든 객체라 그 자리에서 고쳐도 캐시는 그대로입니다. */
  get data(): MatchData {
    return (this.parsed ??= parse(this.raw));
  }

  /** 참가자 PUUID입니다. D1에 적어 둔 참가자를 받았으면 원문을 파싱하지 않고 그것을 씁니다. */
  get players(): Set<string> {
    return (this.knownPlayers ??= playersIn(this.data));
  }
}

/**
 * Riot 호출과 캐시를 한곳에 모았습니다. isolate 메모리, Cache API, Riot 순서로 찾습니다. 메모리는 isolate가 내려가거나
 * 요청이 다른 isolate로 가면 비어 있고, Cache API는 사용자 정의 도메인에서만 담깁니다(`*.workers.dev`에서는 아무것도
 * 담기지 않습니다). 그래서 둘 다 비어 있을 수 있습니다. 경기 참가자만 필요할 때는 D1의 `match_players`를 먼저 봅니다.
 */
export class Riot {
  constructor(
    private readonly env: Env,
    private readonly upstream: typeof fetch,
    private readonly origin: string,
    private readonly caller: Pick<User, "id" | "puuid">,
  ) {}

  /** 내 경기 ID 목록입니다. 새 경기가 끝나자마자 보여야 해서 담아 두지 않습니다. */
  matchlist(): Promise<string> {
    return this.fetchText(`/val/match/v1/matchlists/by-puuid/${this.caller.puuid}`, {
      quota: quotas.matchlist,
      key: String(this.caller.id),
    });
  }

  /** 친구의 경기 ID 목록입니다. 서로 친구이고 전적을 공개했는지는 부르는 쪽이 먼저 확인합니다. */
  friendMatchlist(puuid: string): Promise<string> {
    return this.fetchText(`/val/match/v1/matchlists/by-puuid/${puuid}`, {
      quota: quotas.friendMatchlist,
      key: `${this.caller.id}:${puuid}`,
    });
  }

  /** 참가자만 필요할 때 부릅니다. D1에 적어 둔 경기면 Riot을 부르지 않습니다. */
  async participants(matchId: string): Promise<Set<string>> {
    return (await this.recordedPlayers(matchId)) ?? (await this.match(matchId)).players;
  }

  /**
   * `puuid`가 뛴 경기일 때만 경기를 받고, 아니면 `undefined`입니다. D1에 적어 둔 경기면 거기서 먼저
   * 걸러서 남의 경기를 Riot에 묻지 않습니다.
   */
  async matchWith(matchId: string, puuid: string): Promise<RiotMatch | undefined> {
    const recorded = await this.recordedPlayers(matchId);
    if (recorded && !recorded.has(puuid)) return undefined;
    const match = await this.match(matchId, recorded);
    return match.players.has(puuid) ? match : undefined;
  }

  content(): Promise<string> {
    return this.cached(memory.content, "/val/content/v1/contents?locale=ko-KR", CONTENT_TTL);
  }

  status(): Promise<string> {
    return this.cached(memory.status, "/val/status/v1/platform-data", STATUS_TTL);
  }

  // recorded는 D1에 이미 적힌 참가자다. 넘겨받았으면 Riot에서 새로 받아도 다시 적지 않는다.
  private async match(matchId: string, recorded?: Set<string>): Promise<RiotMatch> {
    const path = `/val/match/v1/matches/${matchId}`;
    if (memory.missing.get(path) !== undefined) throw new ApiError(404, "not_found");
    const hit = await this.lookup(memory.matches, path, MATCH_TTL);
    if (hit !== undefined) return new RiotMatch(hit, undefined, recorded);
    let raw: string;
    try {
      raw = await this.fetchText(path, { quota: quotas.matchDetails, key: String(this.caller.id) });
    } catch (err) {
      if (err instanceof ApiError && err.code === "not_found") memory.missing.set(path, "", MISSING_TTL);
      throw err;
    }
    const data = parse(raw);
    // 끝났다고 적힌 경기만 담고 적는다. 진행 중이거나 matchInfo가 없으면 참가자와 결과가 아직 바뀔 수 있다. 참가자는 부른 사람이
    // 뛴 경기일 때만 적는다. 남의 경기 ID를 마구 물어 D1 쓰기 한도(하루 10만 행)를 쓰게 할 수 없다.
    if (data.matchInfo?.isCompleted === true) {
      const record = !recorded && playersIn(data).has(this.caller.puuid);
      await Promise.all([
        this.remember(memory.matches, path, raw, MATCH_TTL).catch(logFailure("match_cache")),
        record ? this.recordPlayers(matchId, data).catch(logFailure("match_players")) : undefined,
      ]);
    }
    return new RiotMatch(raw, data, recorded);
  }

  private async recordedPlayers(matchId: string): Promise<Set<string> | undefined> {
    const { results } = await this.env.DB.prepare("SELECT puuid FROM match_players WHERE match_id = ?")
      .bind(matchId)
      .all<{ puuid: string }>();
    return results.length > 0 ? new Set(results.map((row) => row.puuid)) : undefined;
  }

  private async recordPlayers(matchId: string, data: MatchData): Promise<void> {
    const players = [...playersIn(data)];
    const db = this.env.DB;
    const statements: D1PreparedStatement[] = [];
    for (let i = 0; i < players.length; i += PLAYERS_PER_INSERT) {
      const chunk = players.slice(i, i + PLAYERS_PER_INSERT);
      statements.push(
        db
          .prepare(`INSERT OR IGNORE INTO match_players (match_id, puuid) VALUES ${chunk.map(() => "(?, ?)").join(", ")}`)
          .bind(...chunk.flatMap((puuid) => [matchId, puuid])),
      );
    }
    if (statements.length > 0) await db.batch(statements);
  }

  private async cached(store: MemoryCache, path: string, ttlSeconds: number): Promise<string> {
    const hit = await this.lookup(store, path, ttlSeconds);
    if (hit !== undefined) return hit;
    const raw = await this.fetchText(path);
    await this.remember(store, path, raw, ttlSeconds);
    return raw;
  }

  private async fetchText(path: string, limit?: { quota: Quota; key: string }): Promise<string> {
    const apiKey = this.env.RIOT_API_KEY;
    if (!apiKey) throw new ApiError(503, "riot_key_missing");
    const url = `${RIOT_HOST}${path}`;
    // Riot이 막혀 있어 어차피 부르지 않을 요청으로 사용자 몫을 깎지 않는다.
    throwIfBlocked(url);
    if (limit) spend(limit.quota, limit.key);
    const res = await send(this.upstream, url, { headers: { "X-Riot-Token": apiKey } });
    return res.text();
  }

  private async lookup(store: MemoryCache, path: string, ttlSeconds: number): Promise<string | undefined> {
    const remembered = store.get(path);
    if (remembered !== undefined) return remembered;
    const hit = await caches.default.match(cacheKey(this.origin, path));
    if (!hit) return undefined;
    const body = await hit.text();
    // Cache API에 남은 기한을 몰라 메모리에는 기한을 처음부터 다시 준다. 그래서 점검 안내는 길게는 2분,
    // 콘텐츠는 12시간까지 늦게 바뀔 수 있다.
    store.set(path, body, ttlSeconds);
    return body;
  }

  private async remember(store: MemoryCache, path: string, body: string, ttlSeconds: number): Promise<void> {
    store.set(path, body, ttlSeconds);
    const headers = { "Content-Type": "application/json", "Cache-Control": `max-age=${ttlSeconds}` };
    await caches.default.put(cacheKey(this.origin, path), new Response(body, { headers }));
  }
}

function spend(quota: Quota, key: string): void {
  const retryAfter = quota.take(key);
  if (retryAfter !== undefined) throw new ApiError(429, "too_many_requests", { "Retry-After": String(retryAfter) });
}

// 담거나 적는 건 다음 요청을 아끼려는 것이라, 실패해도 Riot이 준 경기는 그대로 내려보낸다.
// D1 에러 메시지에는 SQL과 PUUID가 섞일 수 있어 이름만 남긴다.
function logFailure(label: string): (err: unknown) => void {
  return (err) => console.error(label, err instanceof Error ? err.name : typeof err);
}

function parse(raw: string): MatchData {
  let data: unknown;
  try {
    data = JSON.parse(raw);
  } catch {
    data = null;
  }
  if (typeof data !== "object" || data === null || Array.isArray(data)) throw new ApiError(502, "riot_unavailable");
  return data as MatchData;
}

/** 세션 뒤에서만 부릅니다. 사용자별 호출 한도를 세션의 사용자로 셉니다. */
export function riotFor(c: Context<AppEnv>): Riot {
  return new Riot(c.env, c.var.upstream, new URL(c.req.url).origin, c.var.user);
}

function playersIn(match: MatchData): Set<string> {
  const players = Array.isArray(match.players) ? match.players : [];
  return new Set(players.map((player) => player?.puuid).filter((puuid) => typeof puuid === "string"));
}

export function rawJson(c: Context, body: string, cacheControl?: string): Response {
  const headers: Record<string, string> = { "Content-Type": "application/json; charset=utf-8" };
  if (cacheControl) headers["Cache-Control"] = cacheControl;
  return c.body(body, 200, headers);
}
