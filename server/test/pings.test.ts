import { createExecutionContext, createScheduledController, waitOnExecutionContext } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { randomToken } from "../src/crypto";
import { createScheduled, WEEKLY_CRON } from "../src/scheduled";
import { makeFriends, makePuuid, sendRequestRow, setup, type TestUser, userId } from "./helpers";

type T = ReturnType<typeof setup>;

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const EVERY_FIVE_MINUTES = "*/5 * * * *";
const PROJECT = "ovalit-test";
const CLIENT_EMAIL = "push@ovalit-test.iam.gserviceaccount.com";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SEND_URL = `https://fcm.googleapis.com/v1/projects/${PROJECT}/messages:send`;

interface Member {
  puuid: string;
  gameName: string;
  tagLine: string;
  answer: string;
  proposedAt: number | null;
  updatedAt: number;
}

interface Ping {
  id: string;
  host: { puuid: string; gameName: string; tagLine: string };
  startsAt: number;
  createdAt: number;
  expiresAt: number;
  members: Member[];
}

interface Sent {
  token?: string;
  topic?: string;
  data: Record<string, string>;
  android: unknown;
}

let keys: CryptoKeyPair;
let serviceAccount: string;

// RSA 키는 만드는 데 시간이 걸려 파일마다 한 번만 만든다.
beforeAll(async () => {
  keys = (await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  )) as CryptoKeyPair;
  const der = new Uint8Array((await crypto.subtle.exportKey("pkcs8", keys.privateKey)) as ArrayBuffer);
  const lines = btoa(String.fromCharCode(...der)).match(/.{1,64}/g)!.join("\n");
  serviceAccount = JSON.stringify({
    type: "service_account",
    project_id: PROJECT,
    client_email: CLIENT_EMAIL,
    private_key: `-----BEGIN PRIVATE KEY-----\n${lines}\n-----END PRIVATE KEY-----\n`,
  });
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

function withPush(): T {
  return setup({ FCM_SERVICE_ACCOUNT: serviceAccount });
}

/** FCM 대신 답하고 보낸 메시지를 모읍니다. `answer`로 토큰마다 응답을 바꿉니다. */
function fakeFcm(t: T, answer: (token: string) => Response = () => Response.json({ name: "projects/x/messages/1" })): Sent[] {
  const sent: Sent[] = [];
  t.upstream.json(TOKEN_URL, { access_token: "fcm-access", expires_in: 3599, token_type: "Bearer" });
  t.upstream.on(SEND_URL, async (req) => {
    const { message } = await req.json<{ message: Sent }>();
    sent.push(message);
    return message.token ? answer(message.token) : Response.json({ name: "projects/x/messages/2" });
  });
  return sent;
}

async function friendOf(t: T, host: TestUser, gameName = "friend"): Promise<TestUser> {
  const friend = await t.login(gameName);
  await makeFriends(host, friend);
  return friend;
}

async function open(t: T, host: TestUser, friends: TestUser[], startsAt = Date.now() + 2 * HOUR): Promise<Ping> {
  const res = await t.call("POST", "/pings", host.token, { friends: friends.map((friend) => friend.puuid), startsAt });
  expect(res.status).toBe(201);
  return (await res.json<{ ping: Ping }>()).ping;
}

async function reply(t: T, user: TestUser, ping: Ping, answer: string, proposedAt?: number): Promise<Response> {
  return t.call("POST", `/pings/${ping.id}/reply`, user.token, { answer, proposedAt });
}

async function visible(t: T, user: TestUser): Promise<Ping[]> {
  return (await (await t.call("GET", "/pings", user.token)).json<{ pings: Ping[] }>()).pings;
}

async function failure(res: Response): Promise<{ status: number; body: unknown }> {
  return { status: res.status, body: await res.json() };
}

async function registerToken(t: T, user: TestUser, token = `device-${randomToken(8)}`): Promise<string> {
  expect((await t.call("PUT", "/me/push-token", user.token, { token })).status).toBe(204);
  return token;
}

async function tokenOwner(token: string): Promise<number | undefined> {
  const row = await env.DB.prepare("SELECT user_id FROM push_tokens WHERE token = ?").bind(token).first<{ user_id: number }>();
  return row?.user_id;
}

async function reminded(ping: Ping): Promise<number> {
  const row = await env.DB.prepare("SELECT reminded FROM pings WHERE id = ?").bind(ping.id).first<{ reminded: number }>();
  return row!.reminded;
}

async function count(sql: string, ...binds: unknown[]): Promise<number> {
  return (await env.DB.prepare(sql).bind(...binds).first<{ n: number }>())!.n;
}

/** 취소한 오발있을 `createdAts` 시각에 띄운 것처럼 바로 넣습니다. 하루 한도를 볼 때 씁니다. */
async function pastPings(host: TestUser, createdAts: number[]): Promise<void> {
  const id = await userId(host.puuid);
  await env.DB.batch(
    createdAts.map((createdAt) =>
      env.DB.prepare(
        "INSERT INTO pings (id, host, starts_at, created_at, expires_at, canceled) VALUES (?, ?, ?, ?, ?, 1)",
      ).bind(randomToken(16), id, createdAt, createdAt, createdAt + HOUR),
    ),
  );
}

async function runCron(t: T, cron: string): Promise<void> {
  const ctx = createExecutionContext();
  await createScheduled({ fetch: t.upstream.fetch })(createScheduledController({ cron }), t.env, ctx);
  await waitOnExecutionContext(ctx);
}

function decodeJson(part: string): Record<string, unknown> {
  const bytes = Uint8Array.from(atob(part.replaceAll("-", "+").replaceAll("_", "/")), (char) => char.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>;
}

describe("오발있 띄우기", () => {
  it("띄우면 호스트와 부른 친구에게 보이고 부르지 않은 사람에게는 안 보인다", async () => {
    const t = setup();
    const host = await t.login("host");
    const a = await friendOf(t, host, "a");
    const b = await friendOf(t, host, "b");
    const left = await friendOf(t, host, "left");
    // 같이 불린 둘이 서로 친구라 누구에게나 진짜 PUUID로 보인다. 가리는 건 아래 묶음에서 본다.
    await makeFriends(a, b);
    const startsAt = Date.now() + HOUR;

    const res = await t.call("POST", "/pings", host.token, { friends: [b.puuid, a.puuid], startsAt });
    expect(res.status).toBe(201);
    const { ping } = await res.json<{ ping: Ping }>();
    const pending = { tagLine: "KR1", answer: "pending", proposedAt: null, updatedAt: ping.createdAt };
    expect(ping).toEqual({
      id: expect.stringMatching(/^[A-Za-z0-9_-]{22}$/),
      host: { puuid: host.puuid, gameName: "host", tagLine: "KR1" },
      startsAt,
      createdAt: expect.any(Number),
      expiresAt: startsAt + HOUR,
      // 고른 순서 그대로다.
      members: [
        { puuid: b.puuid, gameName: "b", ...pending },
        { puuid: a.puuid, gameName: "a", ...pending },
      ],
    });

    for (const user of [host, a, b]) expect(await visible(t, user)).toEqual([ping]);
    expect(await visible(t, left)).toEqual([]);
  });

  it("불려 간 오발있이 여럿이면 최근에 띄운 것부터 온다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const me = await t.login("me");
    const first = await t.login("first");
    const second = await t.login("second");
    await makeFriends(first, me);
    await makeFriends(second, me);
    const older = await open(t, first, [me]);
    vi.advanceTimersByTime(1000);
    const newer = await open(t, second, [me]);
    expect((await visible(t, me)).map((ping) => ping.id)).toEqual([newer.id, older.id]);
  });

  it("서로 수락한 친구가 아니면 부를 수 없다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const requested = await t.login();
    await sendRequestRow(host, requested);
    const startsAt = Date.now() + HOUR;
    for (const other of [requested.puuid, makePuuid()]) {
      const res = await t.call("POST", "/pings", host.token, { friends: [friend.puuid, other], startsAt });
      expect(await failure(res)).toEqual({ status: 403, body: { error: "not_friend" } });
    }
    expect(await visible(t, host)).toEqual([]);
    expect(await visible(t, friend)).toEqual([]);
  });

  it("부를 친구는 겹치지 않게 한 명에서 네 명까지 고른다", async () => {
    const t = setup();
    const host = await t.login();
    const friends = [await friendOf(t, host), await friendOf(t, host)];
    const [a] = friends.map((friend) => friend.puuid) as [string];
    const startsAt = Date.now() + HOUR;
    const bodies: unknown[] = [
      { startsAt },
      { friends: [], startsAt },
      { friends: a, startsAt },
      { friends: [a, a], startsAt },
      { friends: [host.puuid], startsAt },
      { friends: ["not-a-puuid"], startsAt },
      { friends: [a, makePuuid(), makePuuid(), makePuuid(), makePuuid()], startsAt },
      { friends: [a] },
      { friends: [a], startsAt: String(startsAt) },
      { friends: [a], startsAt: startsAt + 0.5 },
      [a],
    ];
    for (const body of bodies) {
      expect(await failure(await t.call("POST", "/pings", host.token, body))).toEqual({
        status: 400,
        body: { error: "invalid_body" },
      });
    }
  });

  it("시작 시각은 1분 전부터 24시간 뒤까지 받는다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const now = Date.now();
    for (const startsAt of [now - MINUTE - 1, now + DAY + 1]) {
      const res = await t.call("POST", "/pings", host.token, { friends: [friend.puuid], startsAt });
      expect(await failure(res)).toEqual({ status: 400, body: { error: "invalid_time" } });
    }
    const early = await open(t, host, [friend], now - MINUTE);
    expect((await t.call("DELETE", `/pings/${early.id}`, host.token)).status).toBe(204);
    await open(t, host, [friend], now + DAY);
  });

  it("띄운 오발있이 끝나기 전에는 하나 더 띄울 수 없다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    await open(t, host, [friend], Date.now() + 10 * MINUTE);

    const again = await t.call("POST", "/pings", host.token, { friends: [friend.puuid], startsAt: Date.now() + HOUR });
    expect(await failure(again)).toEqual({ status: 409, body: { error: "ping_active" } });
    // 불려 간 친구는 자기 오발있을 따로 띄울 수 있다.
    await open(t, friend, [host]);

    vi.advanceTimersByTime(10 * MINUTE + HOUR);
    await open(t, host, [friend]);
  });

  it("24시간 안에 열 번 띄웠으면 429와 Retry-After를 준다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const now = Date.now();
    // 가장 오래된 것이 23시간 전이라 한 시간 뒤에 한 번 더 띄울 수 있다.
    await pastPings(
      host,
      Array.from({ length: 10 }, (_, i) => now - 23 * HOUR + i * MINUTE),
    );

    const res = await t.call("POST", "/pings", host.token, { friends: [friend.puuid], startsAt: now + HOUR });
    expect(res.status).toBe(429);
    expect(res.headers.get("Retry-After")).toBe("3600");
    expect(await res.json()).toEqual({ error: "too_many_requests" });
    expect(await visible(t, friend)).toEqual([]);

    vi.advanceTimersByTime(HOUR);
    await open(t, host, [friend]);
  });

  it("24시간이 지난 오발있은 하루 한도에 세지 않는다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const now = Date.now();
    await pastPings(
      host,
      Array.from({ length: 10 }, (_, i) => now - DAY - i * MINUTE),
    );
    await open(t, host, [friend]);
  });
});

describe("대답", () => {
  it("부른 친구만 대답할 수 있다", async () => {
    const t = setup();
    const host = await t.login();
    const invited = await friendOf(t, host, "invited");
    const notInvited = await friendOf(t, host);
    const stranger = await t.login();
    const ping = await open(t, host, [invited]);

    for (const user of [host, notInvited, stranger]) {
      expect(await failure(await reply(t, user, ping, "yes"))).toEqual({ status: 404, body: { error: "not_found" } });
    }
    for (const id of [randomToken(16), "nope"]) {
      const res = await t.call("POST", `/pings/${id}/reply`, invited.token, { answer: "yes" });
      expect(res.status).toBe(404);
    }

    const res = await reply(t, invited, ping, "yes");
    expect(res.status).toBe(200);
    const { ping: answered } = await res.json<{ ping: Ping }>();
    expect(answered.members).toEqual([
      { puuid: invited.puuid, gameName: "invited", tagLine: "KR1", answer: "yes", proposedAt: null, updatedAt: expect.any(Number) },
    ]);
    expect(await visible(t, host)).toEqual([answered]);
  });

  it("다른 시간은 제안 시각과 함께 보낸다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const ping = await open(t, host, [friend]);
    const later = Date.now() + 3 * HOUR;

    for (const body of [{ answer: "other_time" }, { answer: "other_time", proposedAt: String(later) }, { answer: "maybe" }, {}]) {
      expect(await failure(await t.call("POST", `/pings/${ping.id}/reply`, friend.token, body))).toEqual({
        status: 400,
        body: { error: "invalid_body" },
      });
    }
    expect(await failure(await reply(t, friend, ping, "other_time", Date.now() + DAY + MINUTE))).toEqual({
      status: 400,
      body: { error: "invalid_time" },
    });

    const proposed = await (await reply(t, friend, ping, "other_time", later)).json<{ ping: Ping }>();
    expect(proposed.ping.members[0]).toMatchObject({ answer: "other_time", proposedAt: later });
    // 가기로 하면 제안한 시각은 버린다.
    const yes = await (await reply(t, friend, ping, "yes", later)).json<{ ping: Ping }>();
    expect(yes.ping.members[0]).toMatchObject({ answer: "yes", proposedAt: null });
  });
});

describe("시간 바꾸기", () => {
  it("시간을 바꾸면 그 시간을 제안한 친구는 가기로 하고 나머지는 다시 묻는다", async () => {
    const t = setup();
    const host = await t.login();
    const yes = await friendOf(t, host, "yes");
    const proposer = await friendOf(t, host, "proposer");
    const no = await friendOf(t, host, "no");
    const ping = await open(t, host, [yes, proposer, no], Date.now() + HOUR);
    const later = Date.now() + 2 * HOUR;
    await reply(t, yes, ping, "yes");
    await reply(t, proposer, ping, "other_time", later);
    await reply(t, no, ping, "no");
    await env.DB.prepare("UPDATE pings SET reminded = 1 WHERE id = ?").bind(ping.id).run();

    const res = await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: later });
    expect(res.status).toBe(200);
    const { ping: moved } = await res.json<{ ping: Ping }>();
    expect(moved).toMatchObject({ startsAt: later, expiresAt: later + HOUR });
    expect(moved.members.map(({ gameName, answer, proposedAt }) => ({ gameName, answer, proposedAt }))).toEqual([
      { gameName: "yes", answer: "pending", proposedAt: null },
      { gameName: "proposer", answer: "yes", proposedAt: null },
      { gameName: "no", answer: "pending", proposedAt: null },
    ]);
    expect(await reminded(ping)).toBe(0);
  });

  it("같은 시간으로 다시 바꾸면 대답을 건드리지 않는다", async () => {
    const t = setup();
    const host = await t.login();
    const proposer = await friendOf(t, host);
    const ping = await open(t, host, [proposer]);
    const later = Date.now() + 3 * HOUR;
    await reply(t, proposer, ping, "other_time", later);
    await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: later });
    const again = await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: later });
    expect((await again.json<{ ping: Ping }>()).ping.members[0]).toMatchObject({ answer: "yes" });
  });

  it("호스트만 시간을 바꿀 수 있다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const ping = await open(t, host, [friend]);
    const startsAt = Date.now() + 3 * HOUR;
    const res = await t.call("POST", `/pings/${ping.id}/time`, friend.token, { startsAt });
    expect(await failure(res)).toEqual({ status: 404, body: { error: "not_found" } });
    const late = await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: Date.now() + 2 * DAY });
    expect(await failure(late)).toEqual({ status: 400, body: { error: "invalid_time" } });
    expect((await visible(t, host))[0]!.startsAt).toBe(ping.startsAt);
  });
});

describe("친구 더 부르기", () => {
  async function invite(t: T, user: TestUser, ping: Ping, friends: TestUser[]): Promise<Response> {
    return t.call("POST", `/pings/${ping.id}/members`, user.token, { friends: friends.map((friend) => friend.puuid) });
  }

  it("호스트가 친구를 더 부르면 앞사람 뒤에 붙고 그 친구에게도 보인다", async () => {
    const t = setup();
    const host = await t.login("host");
    const a = await friendOf(t, host, "a");
    const b = await friendOf(t, host, "b");
    const ping = await open(t, host, [a]);

    const res = await invite(t, host, ping, [b]);
    expect(res.status).toBe(200);
    const { ping: grown } = await res.json<{ ping: Ping }>();
    expect(grown.members.map((member) => [member.gameName, member.answer])).toEqual([
      ["a", "pending"],
      ["b", "pending"],
    ]);
    expect((await visible(t, b)).map((seen) => seen.id)).toEqual([ping.id]);
    expect(await count("SELECT position AS n FROM ping_members WHERE user_id = ?", await userId(b.puuid))).toBe(2);
  });

  it("못 간다고 한 친구는 자리를 비운 것으로 쳐서 넷까지 다시 채운다", async () => {
    const t = setup();
    const host = await t.login("host");
    const four = [await friendOf(t, host), await friendOf(t, host), await friendOf(t, host), await friendOf(t, host)];
    const fifth = await friendOf(t, host);
    const sixth = await friendOf(t, host);
    const ping = await open(t, host, four);

    expect(await failure(await invite(t, host, ping, [fifth]))).toEqual({ status: 409, body: { error: "ping_full" } });
    await reply(t, four[0]!, ping, "no");
    expect((await invite(t, host, ping, [fifth])).status).toBe(200);
    expect(await failure(await invite(t, host, ping, [sixth]))).toEqual({ status: 409, body: { error: "ping_full" } });
  });

  it("이미 부른 친구나 친구가 아닌 사람은 더할 수 없다", async () => {
    const t = setup();
    const host = await t.login();
    const a = await friendOf(t, host);
    const stranger = await t.login();
    const ping = await open(t, host, [a]);

    expect(await failure(await invite(t, host, ping, [a]))).toEqual({ status: 409, body: { error: "already_invited" } });
    expect(await failure(await invite(t, host, ping, [stranger]))).toEqual({ status: 403, body: { error: "not_friend" } });
  });

  it("호스트만 더 부를 수 있고 끝났거나 취소한 오발있에는 못 부른다", async () => {
    const t = setup();
    const host = await t.login();
    const a = await friendOf(t, host);
    const b = await friendOf(t, host);
    await makeFriends(a, b);
    const ping = await open(t, host, [a]);

    expect((await invite(t, a, ping, [b])).status).toBe(404);
    expect((await t.call("DELETE", `/pings/${ping.id}`, host.token)).status).toBe(204);
    expect((await invite(t, host, ping, [b])).status).toBe(404);
  });
});

describe("취소", () => {
  it("호스트만 취소할 수 있고 취소하면 아무에게도 보이지 않는다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    const ping = await open(t, host, [friend]);

    expect((await t.call("DELETE", `/pings/${ping.id}`, friend.token)).status).toBe(404);
    expect((await t.call("DELETE", `/pings/${ping.id}`, host.token)).status).toBe(204);

    expect(await visible(t, host)).toEqual([]);
    expect(await visible(t, friend)).toEqual([]);
    expect((await reply(t, friend, ping, "yes")).status).toBe(404);
    expect((await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: Date.now() + HOUR })).status).toBe(404);
    expect((await t.call("DELETE", `/pings/${ping.id}`, host.token)).status).toBe(404);
    await open(t, host, [friend]);
  });
});

describe("친구 끊기와 연동 해제", () => {
  it("친구를 끊으면 서로 띄운 오발있에서 빠진다", async () => {
    const t = setup();
    const host = await t.login("host");
    const leaving = await friendOf(t, host, "leaving");
    const staying = await friendOf(t, host, "staying");
    const hostPing = await open(t, host, [leaving, staying]);
    const leavingPing = await open(t, leaving, [host]);

    expect((await t.call("DELETE", `/friends/${leaving.puuid}`, host.token)).status).toBe(204);

    const [seenByHost] = await visible(t, host);
    expect(seenByHost).toMatchObject({ id: hostPing.id, members: [{ gameName: "staying" }] });
    expect(await visible(t, leaving)).toEqual([{ ...leavingPing, members: [] }]);
    expect(await visible(t, staying)).toHaveLength(1);
  });

  it("연동을 해제하면 띄운 오발있, 불려 간 자리, 기기 토큰이 같이 지워진다", async () => {
    const t = setup();
    const host = await t.login("host");
    const friend = await friendOf(t, host, "friend");
    const other = await friendOf(t, host, "other");
    await open(t, host, [friend, other]);
    const friendPing = await open(t, friend, [host]);
    await registerToken(t, host);
    const hostId = await userId(host.puuid);

    expect((await t.call("DELETE", "/me", host.token)).status).toBe(204);

    expect(await count("SELECT COUNT(*) AS n FROM pings WHERE host = ?", hostId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM ping_members WHERE user_id = ?", hostId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM push_tokens WHERE user_id = ?", hostId)).toBe(0);
    expect(await visible(t, other)).toEqual([]);
    // 호스트가 남아 있는 오발있은 부른 사람만 빠진다.
    expect(await visible(t, friend)).toEqual([{ ...friendPing, members: [] }]);
  });
});

describe("같이 불린 사람 가리기", () => {
  it("나와 친구가 아닌 사람은 PUUID만 가리고 이름과 대답은 보여 준다", async () => {
    const t = setup();
    const host = await t.login("host");
    const me = await friendOf(t, host, "me");
    const myFriend = await friendOf(t, host, "myFriend");
    const stranger = await friendOf(t, host, "stranger");
    await makeFriends(me, myFriend);
    // 한쪽만 보낸 요청으로는 친구가 아니다.
    await sendRequestRow(me, stranger);
    const ping = await open(t, host, [me, myFriend, stranger]);
    const later = Date.now() + 3 * HOUR;
    await reply(t, stranger, ping, "other_time", later);

    const [seen] = await visible(t, me);
    expect(seen!.host).toEqual({ puuid: host.puuid, gameName: "host", tagLine: "KR1" });
    expect(seen!.members.map(({ puuid, gameName, tagLine, answer, proposedAt }) => ({ puuid, gameName, tagLine, answer, proposedAt }))).toEqual([
      { puuid: me.puuid, gameName: "me", tagLine: "KR1", answer: "pending", proposedAt: null },
      { puuid: myFriend.puuid, gameName: "myFriend", tagLine: "KR1", answer: "pending", proposedAt: null },
      { puuid: "anon-3", gameName: "stranger", tagLine: "KR1", answer: "other_time", proposedAt: later },
    ]);
    const replied = await (await reply(t, me, ping, "yes")).json<{ ping: Ping }>();
    expect(replied.ping.members.map((member) => member.puuid)).toEqual([me.puuid, myFriend.puuid, "anon-3"]);

    const [fromStranger] = await visible(t, stranger);
    expect(fromStranger!.members.map((member) => member.puuid)).toEqual(["anon-1", "anon-2", stranger.puuid]);
    // 호스트는 부른 사람 모두와 친구라 아무도 가리지 않는다.
    const [fromHost] = await visible(t, host);
    expect(fromHost!.members.map((member) => member.puuid)).toEqual([me.puuid, myFriend.puuid, stranger.puuid]);
  });

  it("가린 이름은 그 오발있 안에서 늘 같고 다른 오발있과 이어지지 않는다", async () => {
    const t = setup();
    const host = await t.login("host");
    const otherHost = await t.login("otherHost");
    const leaving = await friendOf(t, host, "leaving");
    const me = await friendOf(t, host, "me");
    const stranger = await friendOf(t, host, "stranger");
    await makeFriends(otherHost, me);
    await makeFriends(otherHost, stranger);
    const first = await open(t, host, [leaving, me, stranger]);
    const second = await open(t, otherHost, [stranger, me]);
    const strangerIn = async (pingId: string) =>
      (await visible(t, me)).find((ping) => ping.id === pingId)!.members.find((member) => member.gameName === "stranger")!.puuid;

    expect(await strangerIn(first.id)).toBe("anon-3");
    expect(await strangerIn(first.id)).toBe("anon-3");
    expect(await strangerIn(second.id)).toBe("anon-1");
    // 앞사람이 빠져도 번호를 당기지 않는다.
    expect((await t.call("DELETE", `/friends/${leaving.puuid}`, host.token)).status).toBe(204);
    expect(await strangerIn(first.id)).toBe("anon-3");
  });
});

describe("기기 토큰", () => {
  it("다른 계정이 쓰던 토큰을 등록하면 나에게 옮겨 온다", async () => {
    const t = setup();
    const before = await t.login();
    const after = await t.login();
    const token = await registerToken(t, before);
    expect(await tokenOwner(token)).toBe(await userId(before.puuid));
    await registerToken(t, after, token);
    expect(await tokenOwner(token)).toBe(await userId(after.puuid));
    expect(await count("SELECT COUNT(*) AS n FROM push_tokens WHERE token = ?", token)).toBe(1);
  });

  it("남의 토큰은 지울 수 없다", async () => {
    const t = setup();
    const owner = await t.login();
    const other = await t.login();
    const token = await registerToken(t, owner);
    expect((await t.call("DELETE", "/me/push-token", other.token, { token })).status).toBe(204);
    expect(await tokenOwner(token)).toBe(await userId(owner.puuid));
    expect((await t.call("DELETE", "/me/push-token", owner.token, { token })).status).toBe(204);
    expect(await tokenOwner(token)).toBeUndefined();
  });

  it("한 사람에게 최근 토큰 셋만 둔다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const user = await t.login();
    const tokens: string[] = [];
    for (let i = 0; i < 3; i++) {
      tokens.push(await registerToken(t, user));
      vi.advanceTimersByTime(1);
    }
    // 첫 토큰을 다시 등록하면 가장 최근 것이 되어 두 번째가 밀려난다.
    await registerToken(t, user, tokens[0]);
    vi.advanceTimersByTime(1);
    const newest = await registerToken(t, user);
    const { results } = await env.DB.prepare("SELECT token FROM push_tokens WHERE user_id = ?")
      .bind(await userId(user.puuid))
      .all<{ token: string }>();
    expect(new Set(results.map((row) => row.token))).toEqual(new Set([tokens[0], tokens[2], newest]));
  });

  it("토큰은 1자에서 4096자까지 받는다", async () => {
    const t = setup();
    const user = await t.login();
    for (const token of ["", "x".repeat(4097), 42, undefined]) {
      expect(await failure(await t.call("PUT", "/me/push-token", user.token, { token }))).toEqual({
        status: 400,
        body: { error: "invalid_push_token" },
      });
    }
    await registerToken(t, user, "x".repeat(4096));
  });
});

describe("알림", () => {
  it("서비스 계정이 없으면 아무것도 보내지 않는다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend]);
    await reply(t, friend, ping, "yes");
    await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: Date.now() + HOUR });
    await t.call("DELETE", `/pings/${ping.id}`, host.token);
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("띄우면 부른 친구의 기기마다 ping_new가 간다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const a = await friendOf(t, host, "a");
    const b = await friendOf(t, host, "b");
    const c = await friendOf(t, host, "c");
    const phone = await registerToken(t, a);
    const tablet = await registerToken(t, a);
    const bPhone = await registerToken(t, b);
    await registerToken(t, host);
    const startsAt = Date.now() + HOUR;
    const ping = await open(t, host, [a, b, c], startsAt);

    const android = { priority: "HIGH", ttl: "3600s" };
    const base = { type: "ping_new", pingId: ping.id, startsAt: String(startsAt), hostName: "host" };
    expect(sent).toHaveLength(3);
    expect(sent).toEqual(
      expect.arrayContaining([
        { token: phone, data: { ...base, others: "b,c" }, android },
        { token: tablet, data: { ...base, others: "b,c" }, android },
        { token: bPhone, data: { ...base, others: "a,c" }, android },
      ]),
    );
    for (const call of t.upstream.callsTo(SEND_URL)) expect(call.headers.get("Authorization")).toBe("Bearer fcm-access");
  });

  it("친구를 더 부르면 더한 친구에게만 ping_new가 간다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const a = await friendOf(t, host, "a");
    const b = await friendOf(t, host, "b");
    await registerToken(t, a);
    const bPhone = await registerToken(t, b);
    const startsAt = Date.now() + HOUR;
    const ping = await open(t, host, [a], startsAt);
    sent.length = 0;

    expect((await t.call("POST", `/pings/${ping.id}/members`, host.token, { friends: [b.puuid] })).status).toBe(200);

    expect(sent).toEqual([
      {
        token: bPhone,
        data: { type: "ping_new", pingId: ping.id, startsAt: String(startsAt), hostName: "host", others: "a" },
        android: { priority: "HIGH", ttl: "3600s" },
      },
    ]);
  });

  it("액세스 토큰은 서비스 계정 키로 서명한 JWT로 받고 만료 5분 전까지 다시 쓴다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = withPush();
    fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend]);

    const [exchange] = t.upstream.callsTo(TOKEN_URL);
    const form = new URLSearchParams(new TextDecoder().decode(await exchange!.arrayBuffer()));
    expect(form.get("grant_type")).toBe("urn:ietf:params:oauth:grant-type:jwt-bearer");
    const [header, claims, signature] = form.get("assertion")!.split(".") as [string, string, string];
    expect(decodeJson(header)).toEqual({ alg: "RS256", typ: "JWT" });
    const payload = decodeJson(claims);
    expect(payload).toMatchObject({
      iss: CLIENT_EMAIL,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: TOKEN_URL,
    });
    expect(Number(payload.exp) - Number(payload.iat)).toBe(3600);
    const signatureBytes = Uint8Array.from(atob(signature.replaceAll("-", "+").replaceAll("_", "/")), (char) =>
      char.charCodeAt(0),
    );
    const signed = new TextEncoder().encode(`${header}.${claims}`);
    expect(await crypto.subtle.verify("RSASSA-PKCS1-v1_5", keys.publicKey, signatureBytes, signed)).toBe(true);

    vi.advanceTimersByTime((3599 - 300) * 1000 - 1);
    await reply(t, friend, ping, "no");
    expect(t.upstream.callsTo(TOKEN_URL)).toHaveLength(1);
    vi.advanceTimersByTime(1);
    await reply(t, friend, ping, "yes");
    expect(t.upstream.callsTo(TOKEN_URL)).toHaveLength(2);
  });

  it("대답하면 호스트에게 ping_reply가 가고 같은 대답을 다시 누르면 보내지 않는다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const friend = await friendOf(t, host, "friend");
    const hostPhone = await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend]);
    sent.length = 0;
    const later = Date.now() + 3 * HOUR;

    await reply(t, friend, ping, "other_time", later);
    await reply(t, friend, ping, "yes");
    await reply(t, friend, ping, "yes");

    const base = { type: "ping_reply", pingId: ping.id, startsAt: String(ping.startsAt), memberName: "friend" };
    expect(sent).toEqual([
      { token: hostPhone, data: { ...base, answer: "other_time", proposedAt: String(later) }, android: expect.anything() },
      { token: hostPhone, data: { ...base, answer: "yes", proposedAt: "" }, android: expect.anything() },
    ]);
  });

  it("시간을 바꾸거나 취소하면 부른 친구들에게 알린다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const a = await friendOf(t, host);
    const b = await friendOf(t, host);
    await registerToken(t, host);
    const tokens = [await registerToken(t, a), await registerToken(t, b)];
    const ping = await open(t, host, [a, b]);
    const later = Date.now() + 3 * HOUR;

    sent.length = 0;
    await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: later });
    expect(sent.map((message) => message.token).sort()).toEqual([...tokens].sort());
    for (const message of sent) {
      expect(message.data).toEqual({ type: "ping_time", pingId: ping.id, startsAt: String(later), hostName: "host" });
    }

    sent.length = 0;
    await t.call("DELETE", `/pings/${ping.id}`, host.token);
    expect(sent.map((message) => message.token).sort()).toEqual([...tokens].sort());
    for (const message of sent) expect(message.data).toEqual({ type: "ping_cancel", pingId: ping.id, hostName: "host" });
  });

  it("FCM이 받을 수 없다고 한 토큰은 지우고 보낸 내용이 틀렸다는 응답에는 남긴다", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const t = withPush();
    const fcmError = (status: number, details: unknown[]) =>
      Response.json({ error: { code: status, status: "x", details } }, { status });
    const answers: Record<string, Response> = {
      gone: new Response(null, { status: 404 }),
      unregistered: fcmError(400, [{ "@type": "FcmError", errorCode: "UNREGISTERED" }]),
      malformed: fcmError(400, [{ "@type": "FcmError", errorCode: "INVALID_ARGUMENT" }]),
      badPayload: fcmError(400, [
        { "@type": "FcmError", errorCode: "INVALID_ARGUMENT" },
        { "@type": "BadRequest", fieldViolations: [{ field: "message.data[0].value" }] },
      ]),
      serverDown: fcmError(503, [{ "@type": "FcmError", errorCode: "UNAVAILABLE" }]),
      fine: Response.json({ name: "ok" }),
    };
    const byToken = new Map<string, Response>();
    fakeFcm(t, (token) => byToken.get(token)!);
    const host = await t.login();
    const friends = [await friendOf(t, host), await friendOf(t, host), await friendOf(t, host)];
    const tokens: Record<string, string> = {};
    for (const [i, name] of Object.keys(answers).entries()) {
      tokens[name] = await registerToken(t, friends[i % friends.length]!);
      byToken.set(tokens[name], answers[name]!);
    }

    await open(t, host, friends);

    for (const name of ["gone", "unregistered", "malformed"]) expect(await tokenOwner(tokens[name]!)).toBeUndefined();
    for (const name of ["badPayload", "serverDown", "fine"]) expect(await tokenOwner(tokens[name]!)).toBeDefined();
  });

  it("알림이 실패해도 요청은 성공한다", async () => {
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const t = withPush();
    t.upstream.on(TOKEN_URL, () => {
      throw new TypeError("network down");
    });
    const host = await t.login();
    const friend = await friendOf(t, host);
    const token = await registerToken(t, friend);
    const ping = await open(t, host, [friend]);
    expect(await visible(t, friend)).toEqual([ping]);
    expect(await tokenOwner(token)).toBeDefined();
    // 에러 메시지에 토큰이 섞일 수 있어 이름만 남긴다.
    expect(errors).toHaveBeenCalledWith("push", "TypeError");
  });

  it("서비스 계정을 읽을 수 없으면 보내지 않는다", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const t = setup({ FCM_SERVICE_ACCOUNT: "{not json" });
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, friend);
    await open(t, host, [friend]);
    expect(t.upstream.calls).toHaveLength(0);
  });
});

describe("크론", () => {
  it("10분 안에 시작하는 오발있은 호스트와 가기로 한 친구에게 한 번만 알린다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const a = await friendOf(t, host, "a");
    const no = await friendOf(t, host, "no");
    const c = await friendOf(t, host, "c");
    const going = [await registerToken(t, host), await registerToken(t, a), await registerToken(t, c)];
    await registerToken(t, no);
    const ping = await open(t, host, [a, no, c], Date.now() + 5 * MINUTE);
    await reply(t, a, ping, "yes");
    await reply(t, no, ping, "no");
    await reply(t, c, ping, "yes");
    sent.length = 0;

    await runCron(t, EVERY_FIVE_MINUTES);
    const reminders = sent.filter((message) => message.data.pingId === ping.id);
    expect(reminders.map((message) => message.token).sort()).toEqual([...going].sort());
    for (const message of reminders) {
      expect(message.data).toEqual({ type: "ping_remind", pingId: ping.id, startsAt: String(ping.startsAt), names: "host,a,c" });
    }
    expect(await reminded(ping)).toBe(1);

    sent.length = 0;
    await runCron(t, EVERY_FIVE_MINUTES);
    expect(sent.filter((message) => message.data.pingId === ping.id)).toEqual([]);
  });

  it("크론 두 번이 겹쳐도 한 번만 알린다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend], Date.now() + 5 * MINUTE);
    await reply(t, friend, ping, "yes");
    sent.length = 0;

    await Promise.all([runCron(t, EVERY_FIVE_MINUTES), runCron(t, EVERY_FIVE_MINUTES)]);
    expect(sent.filter((message) => message.data.pingId === ping.id)).toHaveLength(2);
  });

  it("10분보다 뒤에 시작하면 아직 알리지 않는다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend], Date.now() + 20 * MINUTE);
    await reply(t, friend, ping, "yes");
    sent.length = 0;

    await runCron(t, EVERY_FIVE_MINUTES);
    expect(sent.filter((message) => message.data.pingId === ping.id)).toEqual([]);
    expect(await reminded(ping)).toBe(0);
  });

  it("아무도 가기로 하지 않았으면 알리지 않는다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend], Date.now() + 5 * MINUTE);
    sent.length = 0;

    await runCron(t, EVERY_FIVE_MINUTES);
    expect(sent.filter((message) => message.data.pingId === ping.id)).toEqual([]);
  });

  it("알린 뒤에 시간을 바꾸면 새 시간 전에 다시 알린다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend], Date.now() + 5 * MINUTE);
    await reply(t, friend, ping, "yes");
    await runCron(t, EVERY_FIVE_MINUTES);

    const later = Date.now() + 8 * MINUTE;
    await t.call("POST", `/pings/${ping.id}/time`, host.token, { startsAt: later });
    await reply(t, friend, ping, "yes");
    sent.length = 0;
    await runCron(t, EVERY_FIVE_MINUTES);
    const reminders = sent.filter((message) => message.data.type === "ping_remind" && message.data.pingId === ping.id);
    expect(reminders).toHaveLength(2);
    for (const message of reminders) expect(message.data.startsAt).toBe(String(later));
  });

  it("사람마다 고른 시간에 한 번씩 알린다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login("host");
    const early = await friendOf(t, host, "early");
    const off = await friendOf(t, host, "off");
    const hostToken = await registerToken(t, host);
    const earlyToken = await registerToken(t, early);
    await registerToken(t, off);
    await t.call("PATCH", "/me", early.token, { remindBefore: 60 });
    await t.call("PATCH", "/me", off.token, { remindBefore: 0 });
    const ping = await open(t, host, [early, off], Date.now() + 40 * MINUTE);
    await reply(t, early, ping, "yes");
    await reply(t, off, ping, "yes");
    sent.length = 0;

    // 40분 전에는 한 시간 전을 고른 친구만 받는다. 호스트는 기본값 10분이다.
    await runCron(t, EVERY_FIVE_MINUTES);
    expect(sent.filter((message) => message.data.type === "ping_remind").map((message) => message.token)).toEqual([earlyToken]);

    sent.length = 0;
    await env.DB.prepare("UPDATE pings SET starts_at = ? WHERE id = ?").bind(Date.now() + 5 * MINUTE, ping.id).run();
    await runCron(t, EVERY_FIVE_MINUTES);
    expect(sent.filter((message) => message.data.type === "ping_remind").map((message) => message.token)).toEqual([hostToken]);
  });

  it("월요일 크론은 지난주 리포트 알림을 주제로 한 번 보낸다", async () => {
    const t = withPush();
    const sent = fakeFcm(t);
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    await registerToken(t, friend);
    const ping = await open(t, host, [friend], Date.now() + 5 * MINUTE);
    await reply(t, friend, ping, "yes");
    sent.length = 0;

    await runCron(t, WEEKLY_CRON);
    expect(sent).toEqual([{ topic: "weekly_report", data: { type: "weekly_report" }, android: { priority: "NORMAL" } }]);
    // 곧 시작하는 오발있은 5분 크론이 맡는다.
    expect(await reminded(ping)).toBe(0);
  });

  it("서비스 계정이 없으면 크론도 아무것도 보내지 않는다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await friendOf(t, host);
    await registerToken(t, host);
    const ping = await open(t, host, [friend], Date.now() + 5 * MINUTE);
    await reply(t, friend, ping, "yes");
    await runCron(t, EVERY_FIVE_MINUTES);
    await runCron(t, WEEKLY_CRON);
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("끝나고 하루가 지난 오발있은 불려 간 자리와 함께 지운다", async () => {
    const t = setup();
    const host = await t.login();
    const friend = await t.login();
    const [hostId, friendId] = [await userId(host.puuid), await userId(friend.puuid)];
    const now = Date.now();
    const old = randomToken(16);
    const recent = randomToken(16);
    const insert = (id: string, startsAt: number) => [
      env.DB.prepare("INSERT INTO pings (id, host, starts_at, created_at, expires_at) VALUES (?, ?, ?, ?, ?)").bind(
        id,
        hostId,
        startsAt,
        startsAt - HOUR,
        startsAt + HOUR,
      ),
      env.DB.prepare("INSERT INTO ping_members (ping_id, user_id, position, updated_at) VALUES (?, ?, 1, ?)").bind(
        id,
        friendId,
        now,
      ),
    ];
    await env.DB.batch([...insert(old, now - DAY - HOUR - MINUTE), ...insert(recent, now - DAY - HOUR + MINUTE)]);

    await runCron(t, EVERY_FIVE_MINUTES);

    expect(await count("SELECT COUNT(*) AS n FROM pings WHERE id = ?", old)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM ping_members WHERE ping_id = ?", old)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM pings WHERE id = ?", recent)).toBe(1);
  });
});
