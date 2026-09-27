import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { relationsTo } from "../src/relations";
import {
  makeFriends,
  matchFixture,
  matchUrl,
  RIOT,
  recordedPlayers,
  sendRequestRow,
  setup,
  strangers,
  userId,
} from "./helpers";

afterEach(() => {
  vi.useRealTimers();
});

describe("내 경기", () => {
  it("내 PUUID로 경기 목록을 받고 키는 헤더로만 보낸다", async () => {
    const t = setup();
    const me = await t.login();
    const url = `${RIOT}/val/match/v1/matchlists/by-puuid/${me.puuid}`;
    t.upstream.json(url, { puuid: me.puuid, history: [] });
    const res = await t.call("GET", "/riot/matchlist", me.token);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ puuid: me.puuid, history: [] });
    const [call] = t.upstream.callsTo(url);
    expect(call!.headers.get("X-Riot-Token")).toBe("test-riot-key");
  });

  it("내가 뛴 경기만 열린다", async () => {
    const t = setup();
    const me = await t.login();
    const mine = crypto.randomUUID();
    const others = crypto.randomUUID();
    t.upstream.json(matchUrl(mine), matchFixture(mine, [me, ...strangers(9)]));
    t.upstream.json(matchUrl(others), matchFixture(others, strangers(10)));
    expect((await t.call("GET", `/riot/matches/${mine}`, me.token)).status).toBe(200);
    const denied = await t.call("GET", `/riot/matches/${others}`, me.token);
    expect(denied.status).toBe(403);
    expect(await denied.json()).toEqual({ error: "not_in_match" });
  });

  it("끝난 경기는 한 번만 Riot에서 받는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    const first = await (await t.call("GET", `/riot/matches/${id}`, me.token)).json();
    const second = await (await t.call("GET", `/riot/matches/${id}`, me.token)).json();
    expect(second).toEqual(first);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
  });

  it("대문자로 온 경기 ID도 같은 캐시를 쓴다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    await t.call("GET", `/riot/matches/${id}`, me.token);
    await t.call("GET", `/riot/matches/${id.toUpperCase()}`, me.token);
    expect(t.upstream.calls).toHaveLength(1);
  });

  it("끝나지 않은 경기는 담아 두지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    const live = matchFixture(id, [me, ...strangers(9)]);
    live.matchInfo.isCompleted = false;
    t.upstream.json(matchUrl(id), live);
    await t.call("GET", `/riot/matches/${id}`, me.token);
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(2);
  });

  it("matchInfo가 없는 응답은 끝난 경기로 치지 않아 담지도 적지도 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    const { matchInfo: _, ...withoutInfo } = matchFixture(id, [me, ...strangers(9)]);
    t.upstream.json(matchUrl(id), withoutInfo);
    expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(2);
    expect(await recordedPlayers(id)).toEqual([]);
  });

  it("모양이 틀린 경기 ID는 Riot에 보내지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const res = await t.call("GET", "/riot/matches/..%2F..%2Fsecret", me.token);
    expect(res.status).toBe(400);
    expect(t.upstream.calls).toHaveLength(0);
  });
});

describe("Riot 에러 옮기기", () => {
  async function matchWith(response: () => Response | Promise<Response>) {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.on(matchUrl(id), response);
    return t.call("GET", `/riot/matches/${id}`, me.token);
  }

  it("429는 Retry-After를 붙인 503이다", async () => {
    const res = await matchWith(() => new Response(null, { status: 429, headers: { "Retry-After": "12" } }));
    expect(res.status).toBe(503);
    expect(res.headers.get("Retry-After")).toBe("12");
    expect(await res.json()).toEqual({ error: "riot_rate_limited" });
  });

  it.each([401, 403, 500, 503])("%i는 502 riot_unavailable이다", async (status) => {
    const res = await matchWith(() => new Response(null, { status }));
    expect(res.status).toBe(502);
    expect(await res.json()).toEqual({ error: "riot_unavailable" });
  });

  it("404는 그대로 404다", async () => {
    const res = await matchWith(() => new Response(null, { status: 404 }));
    expect(res.status).toBe(404);
    expect(await res.json()).toEqual({ error: "not_found" });
  });

  it("연결이 끊기면 502다", async () => {
    const res = await matchWith(() => {
      throw new TypeError("network down");
    });
    expect(res.status).toBe(502);
  });

  it("키가 없으면 Riot을 부르지 않고 503이다", async () => {
    const t = setup({ RIOT_API_KEY: undefined });
    const me = await t.login();
    const res = await t.call("GET", "/riot/matchlist", me.token);
    expect(res.status).toBe(503);
    expect(await res.json()).toEqual({ error: "riot_key_missing" });
    expect(t.upstream.calls).toHaveLength(0);
  });
});

describe("Riot 레이트 리밋", () => {
  it("429를 받으면 Retry-After가 지날 때까지 같은 호스트는 부르지 않고 503을 준다", async () => {
    const t = setup();
    const me = await t.login();
    const limited = crypto.randomUUID();
    const next = crypto.randomUUID();
    t.upstream.on(matchUrl(limited), () => new Response(null, { status: 429, headers: { "Retry-After": "12" } }));
    t.upstream.json(matchUrl(next), matchFixture(next, [me, ...strangers(9)]));
    await t.call("GET", `/riot/matches/${limited}`, me.token);

    for (const path of [`/riot/matches/${next}`, "/riot/matchlist"]) {
      const res = await t.call("GET", path, me.token);
      expect(res.status).toBe(503);
      expect(await res.json()).toEqual({ error: "riot_rate_limited" });
      const retryAfter = Number(res.headers.get("Retry-After"));
      expect(retryAfter).toBeGreaterThan(0);
      expect(retryAfter).toBeLessThanOrEqual(12);
    }
    expect(t.upstream.calls).toHaveLength(1);
  });

  it("Retry-After 없이 온 429는 10초 동안 막고, 지나면 다시 부른다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    let limited = true;
    const fixture = matchFixture(id, [me, ...strangers(9)]);
    t.upstream.on(matchUrl(id), () => (limited ? new Response(null, { status: 429 }) : Response.json(fixture)));

    const first = await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(first.status).toBe(503);
    expect(first.headers.get("Retry-After")).toBe("10");
    limited = false;
    vi.advanceTimersByTime(9_000);
    const blocked = await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(blocked.status).toBe(503);
    expect(blocked.headers.get("Retry-After")).toBe("1");
    vi.advanceTimersByTime(1_000);
    expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(2);
  });
});

describe("사용자 호출 한도", () => {
  it("Riot이 없다고 한 경기 ID는 10분 동안 다시 묻지 않는다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.on(matchUrl(id), () => new Response(null, { status: 404 }));
    for (let i = 0; i < 2; i++) {
      const res = await t.call("GET", `/riot/matches/${id}`, me.token);
      expect(res.status).toBe(404);
      expect(await res.json()).toEqual({ error: "not_found" });
    }
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
    vi.advanceTimersByTime(10 * 60 * 1000);
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(2);
  });

  it("내 경기 목록은 10초에 한 번만 Riot에 가고, 그 사이에는 429를 준다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const url = (puuid: string) => `${RIOT}/val/match/v1/matchlists/by-puuid/${puuid}`;
    for (const user of [me, other]) t.upstream.json(url(user.puuid), { puuid: user.puuid, history: [] });

    expect((await t.call("GET", "/riot/matchlist", me.token)).status).toBe(200);
    vi.advanceTimersByTime(4_000);
    const again = await t.call("GET", "/riot/matchlist", me.token);
    expect(again.status).toBe(429);
    expect(again.headers.get("Retry-After")).toBe("6");
    expect(await again.json()).toEqual({ error: "too_many_requests" });
    // 한도는 사람마다 따로 센다.
    expect((await t.call("GET", "/riot/matchlist", other.token)).status).toBe(200);
    vi.advanceTimersByTime(6_000);
    expect((await t.call("GET", "/riot/matchlist", me.token)).status).toBe(200);
    expect(t.upstream.callsTo(url(me.puuid))).toHaveLength(2);
  });

  it("캐시에 없는 경기 상세는 한 사람이 1분에 120번까지만 Riot에 간다", async () => {
    const t = setup();
    const me = await t.login();
    const cached = crypto.randomUUID();
    t.upstream.json(matchUrl(cached), matchFixture(cached, [me, ...strangers(9)]));
    expect((await t.call("GET", `/riot/matches/${cached}`, me.token)).status).toBe(200);
    // 나머지 119번은 Riot이 모르는 경기로 채운다. 404도 Riot에 한 번 간 것이다.
    for (let i = 0; i < 119; i++) {
      const id = crypto.randomUUID();
      t.upstream.on(matchUrl(id), () => new Response(null, { status: 404 }));
      expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(404);
    }

    const over = crypto.randomUUID();
    t.upstream.json(matchUrl(over), matchFixture(over, [me, ...strangers(9)]));
    const res = await t.call("GET", `/riot/matches/${over}`, me.token);
    expect(res.status).toBe(429);
    expect(await res.json()).toEqual({ error: "too_many_requests" });
    expect(Number(res.headers.get("Retry-After"))).toBeGreaterThan(0);
    expect(t.upstream.callsTo(matchUrl(over))).toHaveLength(0);

    // 담아 둔 경기는 Riot에 가지 않으니 계속 열리고, 다른 사람의 몫은 따로 센다.
    expect((await t.call("GET", `/riot/matches/${cached}`, me.token)).status).toBe(200);
    const other = await t.login();
    const theirs = crypto.randomUUID();
    t.upstream.json(matchUrl(theirs), matchFixture(theirs, [other, ...strangers(9)]));
    expect((await t.call("GET", `/riot/matches/${theirs}`, other.token)).status).toBe(200);
  });
});

describe("스코어보드의 앱 사용자", () => {
  it("같이 뛴 사람 중 앱 사용자만 관계와 함께 돌려준다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    const requestedMe = await t.login("requestedMe");
    const requestSent = await t.login("requestSent");
    const appUser = await t.login("appUser");
    const outsider = strangers(1)[0]!;
    await makeFriends(me, friend);
    await sendRequestRow(requestedMe, me);
    await sendRequestRow(me, requestSent);
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, friend, requestedMe, requestSent, appUser, outsider, ...strangers(4)]));

    const res = await t.call("GET", `/riot/matches/${id}/app-users`, me.token);
    expect(res.status).toBe(200);
    const users = await res.json<{ puuid: string; relation: string }[]>();
    expect(new Map(users.map((u) => [u.puuid, u.relation]))).toEqual(
      new Map([
        [friend.puuid, "friend"],
        [requestedMe.puuid, "requested_me"],
        [requestSent.puuid, "request_sent"],
        [appUser.puuid, "app_user"],
      ]),
    );
  });

  it("내가 안 뛴 경기의 앱 사용자는 알려주지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const appUser = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [appUser, ...strangers(9)]));
    expect((await t.call("GET", `/riot/matches/${id}/app-users`, me.token)).status).toBe(403);
  });

  it("관계는 한 번에 99명까지 묻고, 넘기면 too_many_players다", async () => {
    const t = setup();
    const me = await t.login();
    const meId = await userId(me.puuid);
    const puuids = strangers(100).map((p) => p.puuid);
    await expect(relationsTo(env.DB, meId, puuids.slice(0, 99))).resolves.toEqual(new Map());
    await expect(relationsTo(env.DB, meId, puuids)).rejects.toMatchObject({ status: 400, code: "too_many_players" });
  });
});
