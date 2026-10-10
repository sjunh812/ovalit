import { createExecutionContext, waitOnExecutionContext } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { createApp } from "../src/app";
import { base64url } from "../src/crypto";
import type { Env } from "../src/env";
import { clearAccessTokens } from "../src/push";
import { cacheKey, clearMemoryCaches, clearQuotas } from "../src/riot";
import { clearAuthQuotas } from "../src/routes/auth";
import { clearUsedStates } from "../src/state";
import { clearRiotBlocks } from "../src/upstream";

// 실제 키는 쓰지 않는다. wrangler가 .dev.vars를 읽어 오더라도 여기 값으로 덮어쓴다.
// RSO_CLIENT_SECRET이 있으면 /auth/dev가 닫혀서 login()을 못 쓰니 비워 두고, RSO를 보는 테스트만 넣는다.
const TEST_SECRETS = {
  RIOT_API_KEY: "test-riot-key",
  RSO_CLIENT_ID: "test-client",
  RSO_CLIENT_SECRET: undefined,
  STATE_SECRET: "test-state-secret-0123456789abcdef",
  DEV_LOGIN: "true",
  FCM_SERVICE_ACCOUNT: undefined,
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
  // isolate 메모리에 남은 레이트 리밋, 사용자 호출 한도, FCM 액세스 토큰이 다음 테스트로 이어지지 않게 비운다.
  clearRiotBlocks();
  clearQuotas();
  clearAuthQuotas();
  clearUsedStates();
  clearAccessTokens();
  const upstream = new FakeUpstream();
  const app = createApp({ fetch: upstream.fetch });
  const testEnv: Env = { ...env, ...TEST_SECRETS, ...overrides };

  // waitUntil로 넘긴 일(알림)이 끝날 때까지 기다렸다가 응답을 돌려준다. 그래야 알림을 바로 들여다볼 수 있다.
  async function call(method: string, path: string, token?: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const ctx = createExecutionContext();
    const init = { method, headers, body: body === undefined ? undefined : JSON.stringify(body) };
    const res = await app.request(path, init, testEnv, ctx);
    await waitOnExecutionContext(ctx);
    return res;
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

/** isolate 메모리는 모두 비우고 Cache API에서는 `riotPaths`만 지웁니다. 그 뒤에도 Riot을 안 부르면 D1에서 답한 것입니다. */
export async function forgetCaches(...riotPaths: string[]): Promise<void> {
  clearMemoryCaches();
  for (const path of riotPaths) await caches.default.delete(cacheKey("http://localhost", path));
}

export function matchPath(matchId: string): string {
  return `/val/match/v1/matches/${matchId}`;
}

export async function recordPlayers(matchId: string, puuids: string[], recordedAt = Date.now()): Promise<void> {
  await env.DB.prepare("INSERT INTO match_players (match_id, puuids, recorded_at) VALUES (?, ?, ?)")
    .bind(matchId, JSON.stringify(puuids), recordedAt)
    .run();
}

export async function recordedPlayers(matchId: string): Promise<string[]> {
  const row = await env.DB.prepare("SELECT puuids FROM match_players WHERE match_id = ?").bind(matchId).first<{ puuids: string }>();
  return row ? (JSON.parse(row.puuids) as string[]) : [];
}

export interface FixturePlayer {
  puuid: string;
  gameName: string;
  tagLine?: string;
  partyId?: string;
}

/**
 * VAL-MATCH-V1 모양을 흉내 낸 경기입니다.
 * 앞 절반이 Blue, 뒤 절반이 Red이고 라운드마다 i번째가 맞은편 i번째를 잡습니다. Blue에는 앱을 안 쓰는 코치가 한 명 붙습니다.
 * 가리기 테스트가 킬, 피해량, 라운드 기록, 코치 자리의 PUUID까지 보게 합니다.
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
    coaches: [{ puuid: makePuuid(), teamId: "Blue" }],
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

/** [user]에게 앱 사용자 [n]명을 바로 친구로 맺어 줍니다. 친구 한도를 볼 때 씁니다. */
export async function addFriends(user: TestUser, n: number): Promise<void> {
  const me = await userId(user.puuid);
  const now = Date.now();
  const insertUser = env.DB.prepare(
    "INSERT INTO users (puuid, game_name, tag_line, created_at, updated_at) VALUES (?, 'filler', 'KR1', ?, ?)",
  );
  const befriend = env.DB.prepare(
    "INSERT INTO friendships (user_a, user_b, created_at) SELECT MIN(id, ?1), MAX(id, ?1), ?2 FROM users WHERE puuid = ?3",
  );
  await env.DB.batch(
    Array.from({ length: n }, () => makePuuid()).flatMap((puuid) => [insertUser.bind(puuid, now, now), befriend.bind(me, now, puuid)]),
  );
}

/**
 * D1을 감싸 준비한 SQL을 `sql`에 적어 둡니다.
 * `failBatch`를 주면 `batch`가 그 에러로 실패하고, `failSql`을 주면 그 패턴에 맞는 SQL을 돌릴 때 실패합니다.
 * 로그인처럼 `batch`를 쓰는 준비를 마친 뒤 `t.env.DB`에 넣어 씁니다.
 */
export function watchDb(
  db: D1Database,
  options: { failBatch?: Error; failSql?: { pattern: RegExp; error: Error } } = {},
): { db: D1Database; sql: string[] } {
  const sql: string[] = [];
  const failing = (statement: D1PreparedStatement, error: Error): D1PreparedStatement =>
    new Proxy(statement, {
      get(target, prop) {
        if (prop === "bind") return (...values: unknown[]) => failing(target.bind(...values), error);
        if (prop === "run" || prop === "all" || prop === "first" || prop === "raw") return () => Promise.reject(error);
        const value: unknown = Reflect.get(target, prop, target);
        return typeof value === "function" ? value.bind(target) : value;
      },
    });
  const watched = new Proxy(db, {
    get(target, prop) {
      if (prop === "prepare") {
        return (query: string) => {
          sql.push(query);
          const statement = target.prepare(query);
          return options.failSql?.pattern.test(query) ? failing(statement, options.failSql.error) : statement;
        };
      }
      if (prop === "batch" && options.failBatch) return () => Promise.reject(options.failBatch);
      const value: unknown = Reflect.get(target, prop, target);
      return typeof value === "function" ? value.bind(target) : value;
    },
  });
  return { db: watched, sql };
}

/** 이름이 붙은 에러입니다. 로그에 이름만 남는지 볼 때 씁니다. */
export function namedError(name: string, message: string): Error {
  const error = new Error(message);
  error.name = name;
  return error;
}

export async function sendRequestRow(from: TestUser, to: TestUser, source = "scoreboard"): Promise<void> {
  await env.DB.prepare("INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, ?, ?)")
    .bind(await userId(from.puuid), await userId(to.puuid), source, Date.now())
    .run();
}
