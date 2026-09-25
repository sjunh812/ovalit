import { env } from "cloudflare:workers";
import { createApp } from "../src/app";
import { base64url } from "../src/crypto";
import type { Env } from "../src/env";
import { cacheKey, clearMemoryCaches } from "../src/riot";

// 실제 키는 쓰지 않는다. 테스트는 .dev.vars를 읽지 않도록 값을 여기서 덮어쓴다.
const TEST_SECRETS = {
  RIOT_API_KEY: "test-riot-key",
  RSO_CLIENT_ID: "test-client",
  RSO_CLIENT_SECRET: "test-secret",
  DEV_LOGIN: "true",
} satisfies Partial<Env>;

export function makePuuid(): string {
  // 58바이트를 base64url로 바꾸면 PUUID와 같은 78자가 된다.
  return base64url(crypto.getRandomValues(new Uint8Array(58)));
}

type Handler = (req: Request) => Response | Promise<Response>;

/** Riot 대신 응답하고 어떤 요청이 나갔는지 적어 둡니다. 등록하지 않은 주소는 500을 돌려줍니다. */
export class FakeUpstream {
  readonly calls: Request[] = [];
  private readonly routes = new Map<string, Handler>();

  on(url: string, handler: Handler): this {
    this.routes.set(url, handler);
    return this;
  }

  json(url: string, body: unknown, status = 200): this {
    return this.on(url, () => Response.json(body, { status }));
  }

  callsTo(url: string): Request[] {
    return this.calls.filter((req) => req.url === url);
  }

  readonly fetch: typeof fetch = async (input, init) => {
    const req = new Request(input, init);
    this.calls.push(req.clone());
    const handler = this.routes.get(req.url);
    return handler ? handler(req) : new Response("unexpected upstream call", { status: 500 });
  };
}

export interface TestUser {
  puuid: string;
  gameName: string;
  tagLine: string;
  token: string;
}

export function setup(overrides: Partial<Env> = {}) {
  const upstream = new FakeUpstream();
  const app = createApp({ fetch: upstream.fetch });
  const testEnv: Env = { ...env, ...TEST_SECRETS, ...overrides };

  function call(method: string, path: string, token?: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    return Promise.resolve(
      app.request(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) }, testEnv),
    );
  }

  async function login(gameName = "tester", puuid = makePuuid()): Promise<TestUser> {
    const res = await call("POST", "/auth/dev", undefined, { puuid, gameName, tagLine: "KR1" });
    if (res.status !== 200) throw new Error(`dev login failed: ${res.status}`);
    const { token } = await res.json<{ token: string }>();
    return { puuid, gameName, tagLine: "KR1", token };
  }

  return { upstream, app, env: testEnv, call, login };
}

export const RIOT = "https://kr.api.riotgames.com";

export function matchUrl(matchId: string): string {
  return `${RIOT}/val/match/v1/matches/${matchId}`;
}

/** isolate 메모리와 Cache API를 모두 비웁니다. 그 뒤에도 Riot을 안 부르면 D1에서 답한 것입니다. */
export async function forgetCaches(...riotPaths: string[]): Promise<void> {
  clearMemoryCaches();
  for (const path of riotPaths) await caches.default.delete(cacheKey("http://localhost", path));
}

export function matchPath(matchId: string): string {
  return `/val/match/v1/matches/${matchId}`;
}

export async function recordPlayers(matchId: string, puuids: string[]): Promise<void> {
  await env.DB.batch(
    puuids.map((puuid) => env.DB.prepare("INSERT INTO match_players (match_id, puuid) VALUES (?, ?)").bind(matchId, puuid)),
  );
}

export async function recordedPlayers(matchId: string): Promise<string[]> {
  const { results } = await env.DB.prepare("SELECT puuid FROM match_players WHERE match_id = ?").bind(matchId).all<{ puuid: string }>();
  return results.map((row) => row.puuid);
}

export interface FixturePlayer {
  puuid: string;
  gameName: string;
  tagLine?: string;
  partyId?: string;
}

/**
 * VAL-MATCH-V1 모양을 흉내 낸 경기입니다. 앞 절반이 Blue, 뒤 절반이 Red이고 라운드마다 i번째가
 * 맞은편 i번째를 잡습니다. 가리기 테스트가 킬, 피해량, 라운드 기록 안의 PUUID까지 보게 합니다.
 */
export function matchFixture(matchId: string, players: FixturePlayer[], rounds = 2) {
  const half = Math.ceil(players.length / 2);
  const team = (i: number) => (i < half ? "Blue" : "Red");
  const opponent = (i: number) => players[(i + half) % players.length]!.puuid;
  const kills = (round: number) =>
    players.slice(0, half).map((killer, i) => ({
      timeSinceGameStartMillis: round * 100_000 + i * 1000,
      timeSinceRoundStartMillis: i * 1000,
      killer: killer.puuid,
      victim: opponent(i),
      victimLocation: { x: i, y: i },
      assistants: [players[(i + 1) % half]!.puuid],
      playerLocations: players.map((p, j) => ({ puuid: p.puuid, viewRadians: 0, location: { x: j, y: j } })),
      finishingDamage: { damageType: "Weapon", damageItem: "9C82E19D-4575-0200-1A81-3EACF00CF872", isSecondaryFireMode: false },
    }));
  return {
    matchInfo: {
      matchId,
      mapId: "/Game/Maps/Ascent/Ascent",
      gameLengthMillis: 1_800_000,
      gameStartMillis: 1_790_000_000_000,
      provisioningFlowId: "Matchmaking",
      isCompleted: true,
      customGameName: "",
      queueId: "competitive",
      gameMode: "/Game/GameModes/Bomb/BombGameMode.BombGameMode_C",
      isRanked: true,
      seasonId: "4c4b8cff-43eb-13d3-8f14-96b783c90cd2",
    },
    players: players.map((p, i) => ({
      puuid: p.puuid,
      gameName: p.gameName,
      tagLine: p.tagLine ?? "KR1",
      teamId: team(i),
      partyId: p.partyId ?? crypto.randomUUID(),
      characterId: "add6443a-41bd-e414-f6ad-e58d267f4e95",
      stats: { score: 4000, roundsPlayed: rounds, kills: rounds, deaths: rounds, assists: rounds, playtimeMillis: 1_800_000 },
      competitiveTier: 12,
      playerCard: "9fb348bc-41a0-91ad-8a3e-818035c4e561",
      playerTitle: "d13e579c-435e-44d4-cec2-6eae5a3c5ed4",
      accountLevel: 120,
    })),
    coaches: [],
    teams: [
      { teamId: "Blue", won: true, roundsPlayed: rounds, roundsWon: rounds, numPoints: rounds },
      { teamId: "Red", won: false, roundsPlayed: rounds, roundsWon: 0, numPoints: 0 },
    ],
    roundResults: Array.from({ length: rounds }, (_, round) => ({
      roundNum: round,
      roundResult: "Eliminated",
      winningTeam: "Blue",
      bombPlanter: players[0]!.puuid,
      playerStats: players.map((p, i) => ({
        puuid: p.puuid,
        kills: i < half ? [kills(round)[i]] : [],
        damage: [{ receiver: opponent(i), damage: 150, legshots: 0, bodyshots: 2, headshots: 1 }],
        score: 200,
        economy: { loadoutValue: 3900, weapon: "9C82E19D-4575-0200-1A81-3EACF00CF872", armor: "", remaining: 800, spent: 3900 },
        ability: {},
      })),
    })),
  };
}

export async function userId(puuid: string): Promise<number> {
  const row = await env.DB.prepare("SELECT id FROM users WHERE puuid = ?").bind(puuid).first<{ id: number }>();
  if (!row) throw new Error("no such user");
  return row.id;
}

export function strangers(n: number): FixturePlayer[] {
  return Array.from({ length: n }, (_, i) => ({ puuid: makePuuid(), gameName: `stranger${i}`, tagLine: `S${i}` }));
}

/** 요청과 수락을 거치지 않고 바로 친구로 맺습니다. 친구가 된 뒤의 동작만 볼 때 씁니다. */
export async function makeFriends(a: TestUser, b: TestUser): Promise<void> {
  const [x, y] = [await userId(a.puuid), await userId(b.puuid)].sort((m, n) => m - n);
  await env.DB.prepare("INSERT INTO friendships (user_a, user_b, created_at) VALUES (?, ?, ?)").bind(x, y, Date.now()).run();
}

export async function sendRequestRow(from: TestUser, to: TestUser, source = "scoreboard"): Promise<void> {
  await env.DB.prepare("INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, ?, ?)")
    .bind(await userId(from.puuid), await userId(to.puuid), source, Date.now())
    .run();
}
