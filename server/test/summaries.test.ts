import { createExecutionContext, createScheduledController, waitOnExecutionContext } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { createScheduled } from "../src/scheduled";
import { makeFriends, setup, type TestUser, userId, watchDb } from "./helpers";

type T = ReturnType<typeof setup>;

const DAY = 24 * 60 * 60 * 1000;
const ACT = "0f2c0b6a-6f0e-4b2a-9a51-2b4e8a7e3f11";
const NEXT_ACT = "6a9f3c1e-2d4b-4e8a-b1c7-9d2e5f8a0b33";

// 그 주의 월요일(UTC)을 YYYY-MM-DD로 돌려준다
function monday(weeksAgo = 0, now = Date.now()): string {
  const day = new Date(now - weeksAgo * 7 * DAY);
  const back = (day.getUTCDay() + 6) % 7;
  return new Date(day.getTime() - back * DAY).toISOString().slice(0, 10);
}

function row(overrides: Record<string, unknown> = {}) {
  return {
    act: ACT,
    queue: "competitive",
    matches: 5,
    rounds: 110,
    kills: 80,
    deaths: 70,
    assists: 30,
    score: 24_000,
    damage: 15_400,
    head: 60,
    body: 180,
    leg: 20,
    ...overrides,
  };
}

async function upload(t: T, user: TestUser, week: string, rows: unknown[]): Promise<Response> {
  return t.call("PUT", `/me/summaries/${week}`, user.token, { rows });
}

async function summariesOf(t: T, user: TestUser): Promise<Record<string, unknown>[]> {
  const res = await t.call("GET", "/friends/summaries", user.token);
  expect(res.status).toBe(200);
  return res.json();
}

async function storedRows(user: TestUser): Promise<number> {
  const result = await env.DB.prepare("SELECT COUNT(*) AS n FROM weekly_summaries WHERE user_id = ?")
    .bind(await userId(user.puuid))
    .first<{ n: number }>();
  return result!.n;
}

describe("주간 요약", () => {
  it("세션이 없으면 올리지도 받지도 못한다", async () => {
    const t = setup();
    expect((await t.call("PUT", `/me/summaries/${monday()}`, undefined, { rows: [row()] })).status).toBe(401);
    expect((await t.call("GET", "/friends/summaries")).status).toBe(401);
  });

  it("서로 수락한 친구가 올린 요약을 합계 그대로 받는다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    await makeFriends(me, friend);

    expect((await upload(t, friend, monday(), [row()])).status).toBe(204);

    expect(await summariesOf(t, me)).toEqual([{ puuid: friend.puuid, week: monday(), ...row() }]);
  });

  // 비교 대상은 본인의 과거와 서로 수락한 친구뿐이다(CLAUDE.md 지켜야 할 선)
  it("친구가 아니거나 요청만 보낸 사이, 전적을 비공개로 둔 친구의 요약은 내주지 않는다", async () => {
    const t = setup();
    const me = await t.login("me");
    const stranger = await t.login("stranger");
    const hidden = await t.login("hidden");
    await makeFriends(me, hidden);
    await upload(t, stranger, monday(), [row()]);
    await upload(t, hidden, monday(), [row()]);
    await t.call("PATCH", "/me", hidden.token, { statsPublic: false });

    expect(await summariesOf(t, me)).toEqual([]);
  });

  it("친구를 끊으면 요약을 지우지 않고 내주지만 않으며 연동을 해제하면 지운다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    await makeFriends(me, friend);
    await upload(t, friend, monday(), [row()]);

    expect((await t.call("DELETE", `/friends/${friend.puuid}`, me.token)).status).toBe(204);
    expect(await summariesOf(t, me)).toEqual([]);
    expect(await storedRows(friend)).toBe(1);

    const id = await userId(friend.puuid);
    expect((await t.call("DELETE", "/me", friend.token)).status).toBe(204);
    const left = await env.DB.prepare("SELECT COUNT(*) AS n FROM weekly_summaries WHERE user_id = ?").bind(id).first<{ n: number }>();
    expect(left!.n).toBe(0);
  });

  // 비율을 받으면 친구 앱이 기간을 넓힐 때 평균의 평균을 만든다(CLAUDE.md 계산에서 지키는 것들)
  it("합계만 받고 비율, 소수, 음수, 모르는 필드는 400이다", async () => {
    const t = setup();
    const me = await t.login();
    for (const bad of [
      [row({ kd: 1.14 })],
      [row({ kills: 80.5 })],
      [row({ deaths: -1 })],
      [row({ queue: "deathmatch" })],
      [row({ act: "not-an-act" })],
      [row(), row()],
      [row(), row({ queue: "unrated" }), row({ act: NEXT_ACT }), row({ act: NEXT_ACT, queue: "unrated" }), row({ matches: 1 })],
    ]) {
      const res = await upload(t, me, monday(), bad);
      expect(res.status).toBe(400);
      expect(await res.json()).toEqual({ error: "invalid_summary" });
    }
    expect(await storedRows(me)).toBe(0);
  });

  it("월요일이 아니거나 너무 오래되거나 앞선 주는 400이다", async () => {
    const t = setup();
    const me = await t.login();
    const tuesday = new Date(new Date(`${monday()}T00:00:00Z`).getTime() + DAY).toISOString().slice(0, 10);
    for (const week of [tuesday, monday(11), monday(-2), "2026-13-01", "nope"]) {
      const res = await upload(t, me, week, [row()]);
      expect(res.status).toBe(400);
    }
  });

  it("같은 주를 다시 올리면 그 주의 줄을 통째로 바꾸고 빈 목록이면 지운다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    await makeFriends(me, friend);
    await upload(t, friend, monday(), [row(), row({ queue: "unrated", matches: 2 })]);

    await upload(t, friend, monday(), [row({ matches: 6, act: NEXT_ACT })]);
    expect(await summariesOf(t, me)).toEqual([{ puuid: friend.puuid, week: monday(), ...row({ matches: 6, act: NEXT_ACT }) }]);

    await upload(t, friend, monday(), []);
    expect(await summariesOf(t, me)).toEqual([]);
  });

  // 새 경기가 없는데 같은 값을 다시 올리면 D1 하루 쓰기 한도만 쓴다
  it("같은 값을 다시 올리면 D1에 쓰지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    await upload(t, me, monday(), [row()]);

    const watched = watchDb(t.env.DB);
    const quiet = setup({ DB: watched.db });
    const again = await quiet.call("PUT", `/me/summaries/${monday()}`, me.token, { rows: [row()] });
    expect(again.status).toBe(204);
    expect(watched.sql.filter((sql) => /^\s*(INSERT|DELETE|UPDATE)/i.test(sql) && sql.includes("weekly_summaries"))).toEqual([]);
  });

  it("한 시간에 30번 넘게 올리면 429다", async () => {
    const t = setup();
    const me = await t.login();
    for (let i = 0; i < 30; i++) expect((await upload(t, me, monday(), [row({ matches: i })])).status).toBe(204);
    const res = await upload(t, me, monday(), [row({ matches: 31 })]);
    expect(res.status).toBe(429);
    expect(res.headers.get("Retry-After")).toBeTruthy();
  });

  it("since로 그 주부터만 받는다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    await makeFriends(me, friend);
    await upload(t, friend, monday(3), [row()]);
    await upload(t, friend, monday(), [row({ matches: 2 })]);

    const res = await t.call("GET", `/friends/summaries?since=${monday(1)}`, me.token);
    expect((await res.json<{ week: string }[]>()).map((it) => it.week)).toEqual([monday()]);
  });

  it("크론은 둘 수 있는 주가 지난 요약을 지운다", async () => {
    const t = setup();
    const me = await t.login();
    const id = await userId(me.puuid);
    await upload(t, me, monday(), [row()]);
    await env.DB.prepare(
      `INSERT INTO weekly_summaries (user_id, week, act, queue, matches, rounds, kills, deaths, assists, score, damage, head, body, leg, updated_at)
       VALUES (?, ?, ?, 'competitive', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)`,
    )
      .bind(id, monday(12), ACT)
      .run();

    const ctx = createExecutionContext();
    await createScheduled({ fetch: t.upstream.fetch })(createScheduledController({ cron: "*/5 * * * *" }), t.env, ctx);
    await waitOnExecutionContext(ctx);

    const { results } = await env.DB.prepare("SELECT week FROM weekly_summaries WHERE user_id = ?").bind(id).all<{ week: string }>();
    expect(results.map((it) => it.week)).toEqual([monday()]);
  });
});
