import type { Context } from "hono";
import type { AppEnv, Env } from "./env";
import { ApiError } from "./errors";
import { send } from "./upstream";

const RIOT_HOST = "https://kr.api.riotgames.com";
const DAY = 24 * 60 * 60;
// 끝난 경기는 결과가 바뀌지 않는다. 다시 받으면 레이트 리밋만 쓴다.
const MATCH_TTL = 30 * DAY;
const CONTENT_TTL = 6 * 60 * 60;
const STATUS_TTL = 60;

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
 * Riot 호출과 캐시를 한곳에 모았습니다. 캐시는 데이터센터마다 따로이고 사용자 정의 도메인에서만
 * 실제로 담깁니다(workers.dev에서는 담기지 않습니다).
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
    const hit = await this.fromCache(path);
    if (hit !== undefined) return { raw: hit, data: parse(hit) };
    const raw = await this.fetchText(path);
    const data = parse(raw);
    if (data.matchInfo?.isCompleted !== false) await this.toCache(path, raw, MATCH_TTL);
    return { raw, data };
  }

  content(): Promise<string> {
    return this.cached("/val/content/v1/contents?locale=ko-KR", CONTENT_TTL);
  }

  status(): Promise<string> {
    return this.cached("/val/status/v1/platform-data", STATUS_TTL);
  }

  private async cached(path: string, ttlSeconds: number): Promise<string> {
    const hit = await this.fromCache(path);
    if (hit !== undefined) return hit;
    const raw = await this.fetchText(path);
    await this.toCache(path, raw, ttlSeconds);
    return raw;
  }

  private async fetchText(path: string): Promise<string> {
    const apiKey = this.env.RIOT_API_KEY;
    if (!apiKey) throw new ApiError(503, "riot_key_missing");
    const res = await send(this.upstream, `${RIOT_HOST}${path}`, { headers: { "X-Riot-Token": apiKey } });
    return res.text();
  }

  // Cache API는 요청이 들어온 도메인 단위로 갈려서 캐시 키 주소도 우리 도메인 아래로 잡는다.
  private cacheKey(path: string): Request {
    return new Request(`${this.origin}/__riot${path}`);
  }

  private async fromCache(path: string): Promise<string | undefined> {
    const hit = await caches.default.match(this.cacheKey(path));
    return hit ? hit.text() : undefined;
  }

  private async toCache(path: string, body: string, ttlSeconds: number): Promise<void> {
    const headers = { "Content-Type": "application/json", "Cache-Control": `max-age=${ttlSeconds}` };
    await caches.default.put(this.cacheKey(path), new Response(body, { headers }));
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

export function playersOf(match: MatchData): Set<string> {
  const players = Array.isArray(match.players) ? match.players : [];
  return new Set(players.map((player) => player?.puuid).filter((puuid) => typeof puuid === "string"));
}

export function rawJson(c: Context, body: string, cacheControl?: string): Response {
  const headers: Record<string, string> = { "Content-Type": "application/json; charset=utf-8" };
  if (cacheControl) headers["Cache-Control"] = cacheControl;
  return c.body(body, 200, headers);
}
