import { describe, expect, it } from "vitest";
import { makeFriends, matchFixture, matchUrl, RIOT, sendRequestRow, setup, strangers } from "./helpers";

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
});
