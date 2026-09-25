import type { Context } from "hono";
import type { AppEnv, Env } from "./env";
import { ApiError } from "./errors";
import { MemoryCache } from "./memory";
import { send } from "./upstream";

const RIOT_HOST = "https://kr.api.riotgames.com";
const DAY = 24 * 60 * 60;
// 끝난 경기는 결과가 바뀌지 않는다. 다시 받으면 레이트 리밋만 쓴다.
const MATCH_TTL = 30 * DAY;
const CONTENT_TTL = 6 * 60 * 60;
const STATUS_TTL = 60;
const MB = 1024 * 1024;
// 한 문장에 (match_id, puuid) 쌍을 50개까지 넣는다. 바인딩은 쿼리당 100개까지다.
const PLAYERS_PER_INSERT = 50;

// isolate가 살아 있는 동안 요청끼리 나눠 쓴다. isolate 메모리는 128MB라 셋을 합쳐 20MB 안쪽으로 둔다.
const memory = {
  content: new MemoryCache({ maxEntries: 1, maxBytes: 10 * MB }),
  status: new MemoryCache({ maxEntries: 1, maxBytes: 1 * MB }),
  matches: new MemoryCache({ maxEntries: Number.POSITIVE_INFINITY, maxBytes: 8 * MB }),
};

/** 테스트가 메모리 캐시를 비우고 D1이나 Cache API만으로 도는지 볼 때 씁니다. */
export function clearMemoryCaches(): void {
  for (const cache of Object.values(memory)) cache.clear();
}

/** Cache API는 요청이 들어온 도메인 단위로 갈려서 캐시 키 주소도 우리 도메인 아래로 잡습니다. */
export function cacheKey(origin: string, path: string): Request {
  return new Request(`${origin}/__riot${path}`);
}

export interface MatchData {
  matchInfo?: { isCompleted?: boolean };
  players?: { puuid?: unknown }[];
  [key: string]: unknown;
}

export interface RiotMatch {
  /** Riot이 준 원문입니다. 가릴 것이 없으면 다시 직렬화하지 않고 그대로 내려보냅니다. */
  raw: string;
  data: MatchData;
}

/**
 * Riot 호출과 캐시를 한곳에 모았습니다. isolate 메모리, Cache API, Riot 순서로 찾습니다. Cache API는
 * 사용자 정의 도메인에서만 실제로 담기고(`*.workers.dev`에서는 담기지 않습니다) 메모리는 isolate가
 * 내려가면 비어서, 둘 다 있으면 쓰는 정도입니다. 경기 참가자만 필요할 때는 D1의 `match_players`를 봅니다.
 */
export class Riot {
  constructor(
    private readonly env: Env,
    private readonly upstream: typeof fetch,
    private readonly origin: string,
  ) {}

  /** 새 경기가 끝나자마자 목록에 보여야 해서 담아 두지 않습니다. */
  matchlist(puuid: string): Promise<string> {
    return this.fetchText(`/val/match/v1/matchlists/by-puuid/${puuid}`);
  }

  async match(matchId: string): Promise<RiotMatch> {
    const path = `/val/match/v1/matches/${matchId}`;
    const hit = await this.lookup(memory.matches, path, MATCH_TTL);
    if (hit !== undefined) return { raw: hit, data: parse(hit) };
    const raw = await this.fetchText(path);
    const data = parse(raw);
    // 진행 중인 경기가 오면 담지도 적지도 않는다. 참가자와 결과가 아직 바뀔 수 있다.
    if (data.matchInfo?.isCompleted !== false) {
      await Promise.all([this.remember(memory.matches, path, raw, MATCH_TTL), this.recordPlayers(matchId, data)]);
    }
    return { raw, data };
  }

  /** 참가자만 필요할 때 부릅니다. D1에 적어 둔 경기면 Riot을 부르지 않습니다. */
  async participants(matchId: string): Promise<Set<string>> {
    return (await this.recordedPlayers(matchId)) ?? playersIn((await this.match(matchId)).data);
  }

  /**
   * `puuid`가 뛴 경기일 때만 경기를 받고, 아니면 `undefined`입니다. D1에 적어 둔 경기면 거기서 먼저
   * 걸러서 남의 경기를 Riot에 묻지 않습니다.
   */
  async matchWith(matchId: string, puuid: string): Promise<RiotMatch | undefined> {
    const recorded = await this.recordedPlayers(matchId);
    if (recorded && !recorded.has(puuid)) return undefined;
    const match = await this.match(matchId);
    return playersIn(match.data).has(puuid) ? match : undefined;
  }

  content(): Promise<string> {
    return this.cached(memory.content, "/val/content/v1/contents?locale=ko-KR", CONTENT_TTL);
  }

  status(): Promise<string> {
    return this.cached(memory.status, "/val/status/v1/platform-data", STATUS_TTL);
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

  private async fetchText(path: string): Promise<string> {
    const apiKey = this.env.RIOT_API_KEY;
    if (!apiKey) throw new ApiError(503, "riot_key_missing");
    const res = await send(this.upstream, `${RIOT_HOST}${path}`, { headers: { "X-Riot-Token": apiKey } });
    return res.text();
  }

  private async lookup(store: MemoryCache, path: string, ttlSeconds: number): Promise<string | undefined> {
    const remembered = store.get(path);
    if (remembered !== undefined) return remembered;
    const hit = await caches.default.match(cacheKey(this.origin, path));
    if (!hit) return undefined;
    const body = await hit.text();
    // Cache API에 남은 기한을 몰라 메모리에는 기한을 처음부터 다시 준다. 점검 안내는 길어야 2분까지 늦을 수 있다.
    store.set(path, body, ttlSeconds);
    return body;
  }

  private async remember(store: MemoryCache, path: string, body: string, ttlSeconds: number): Promise<void> {
    store.set(path, body, ttlSeconds);
    const headers = { "Content-Type": "application/json", "Cache-Control": `max-age=${ttlSeconds}` };
    await caches.default.put(cacheKey(this.origin, path), new Response(body, { headers }));
  }
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

export function riotFor(c: Context<AppEnv>): Riot {
  return new Riot(c.env, c.var.upstream, new URL(c.req.url).origin);
}

export function playersIn(match: MatchData): Set<string> {
  const players = Array.isArray(match.players) ? match.players : [];
  return new Set(players.map((player) => player?.puuid).filter((puuid) => typeof puuid === "string"));
}

export function rawJson(c: Context, body: string, cacheControl?: string): Response {
  const headers: Record<string, string> = { "Content-Type": "application/json; charset=utf-8" };
  if (cacheControl) headers["Cache-Control"] = cacheControl;
  return c.body(body, 200, headers);
}
