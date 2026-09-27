import { afterEach, describe, expect, it, vi } from "vitest";
import { cacheKey, clearMemoryCaches } from "../src/riot";
import {
  forgetCaches,
  makeFriends,
  matchFixture,
  matchPath,
  matchUrl,
  namedError,
  recordedPlayers,
  recordPlayers,
  setup,
  strangers,
  watchDb,
} from "./helpers";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("경기 캐시", () => {
  it("Cache API가 담지 못해도 isolate 메모리에서 꺼내 Riot을 다시 부르지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    await t.call("GET", `/riot/matches/${id}`, me.token);
    // workers.dev처럼 Cache API에 아무것도 남지 않은 상태를 만든다.
    await caches.default.delete(cacheKey("http://localhost", matchPath(id)));
    expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
  });

  it("isolate가 새로 떠 메모리가 비어도 Cache API에서 꺼낸다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    await t.call("GET", `/riot/matches/${id}`, me.token);
    clearMemoryCaches();
    expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
  });

  it("800KB 경기 여섯 판은 Cache API 없이도 메모리에 다 남는다", async () => {
    const t = setup();
    const me = await t.login();
    const ids = Array.from({ length: 6 }, () => crypto.randomUUID());
    for (const id of ids) {
      t.upstream.json(matchUrl(id), { ...matchFixture(id, [me, ...strangers(9)]), padding: "x".repeat(800_000) });
      expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    }
    for (const id of ids) await caches.default.delete(cacheKey("http://localhost", matchPath(id)));
    for (const id of ids) expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    for (const id of ids) expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
  });

  it("메모리에서 꺼낸 경기는 파싱하지 않고 원문 그대로 내려보낸다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    const first = await (await t.call("GET", `/riot/matches/${id}`, me.token)).text();
    const parse = vi.spyOn(JSON, "parse");
    const again = await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(await again.text()).toBe(first);
    expect(parse.mock.calls.filter(([text]) => typeof text === "string" && text.includes(id))).toEqual([]);
  });

  it("Cache API에 담지 못해도 경기는 내려보내고 에러 이름만 남긴다", async () => {
    const t = setup();
    const me = await t.login();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(caches.default, "put").mockRejectedValue(namedError("CacheDown", `put ${me.puuid}`));
    const id = crypto.randomUUID();
    const fixture = matchFixture(id, [me, ...strangers(9)]);
    t.upstream.json(matchUrl(id), fixture);
    const res = await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual(fixture);
    expect(errors.mock.calls).toEqual([["match_cache", "CacheDown"]]);
    expect(await recordedPlayers(id)).toHaveLength(10);
  });
});

describe("경기 참가자 기록", () => {
  it("끝난 경기를 처음 받으면 참가자 PUUID만 적는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    const players = [me, ...strangers(9)];
    t.upstream.json(matchUrl(id), matchFixture(id, players));
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(new Set(await recordedPlayers(id))).toEqual(new Set(players.map((p) => p.puuid)));
  });

  it("참가자를 D1에 적지 못해도 경기는 내려보내고 에러 이름만 남긴다", async () => {
    const t = setup();
    const me = await t.login();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    t.env.DB = watchDb(t.env.DB, { failBatch: namedError("D1Down", `INSERT ${me.puuid}`) }).db;
    const id = crypto.randomUUID();
    const fixture = matchFixture(id, [me, ...strangers(9)]);
    t.upstream.json(matchUrl(id), fixture);
    const res = await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual(fixture);
    expect(errors.mock.calls).toEqual([["match_players", "D1Down"]]);
  });

  it("적어 둔 경기를 Riot에서 다시 받아도 참가자를 또 적지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    const players = [me, ...strangers(9)];
    await recordPlayers(id, players.map((p) => p.puuid));
    t.upstream.json(matchUrl(id), matchFixture(id, players));
    const watched = watchDb(t.env.DB);
    t.env.DB = watched.db;
    expect((await t.call("GET", `/riot/matches/${id}`, me.token)).status).toBe(200);
    expect(t.upstream.callsTo(matchUrl(id))).toHaveLength(1);
    expect(watched.sql.filter((sql) => sql.includes("INSERT"))).toEqual([]);
  });

  it("끝나지 않은 경기는 적지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    const live = matchFixture(id, [me, ...strangers(9)]);
    live.matchInfo.isCompleted = false;
    t.upstream.json(matchUrl(id), live);
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect(await recordedPlayers(id)).toEqual([]);
  });

  it("적어 둔 경기의 앱 사용자는 두 번째부터 Riot 없이 알려준다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, other, ...strangers(8)]));
    await t.call("GET", `/riot/matches/${id}/app-users`, me.token);
    await forgetCaches(matchPath(id));
    const before = t.upstream.calls.length;

    const res = await t.call("GET", `/riot/matches/${id}/app-users`, me.token);
    expect(await res.json()).toEqual([{ puuid: other.puuid, relation: "app_user" }]);
    expect(t.upstream.calls.length - before).toBe(0);
  });

  it("적어 둔 경기로 보내는 친구 요청은 Riot을 부르지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const id = crypto.randomUUID();
    await recordPlayers(id, [me.puuid, other.puuid, ...strangers(8).map((p) => p.puuid)]);
    const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId: id });
    expect(res.status).toBe(201);
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("적어 둔 경기에 둘이 같이 없으면 Riot을 부르지 않고 막는다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const id = crypto.randomUUID();
    await recordPlayers(id, [me.puuid, ...strangers(9).map((p) => p.puuid)]);
    const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId: id });
    expect(res.status).toBe(403);
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("적어 둔 경기에 내가 없으면 경기 원문을 Riot에 묻지 않고 403이다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    await recordPlayers(id, strangers(10).map((p) => p.puuid));
    for (const path of [`/riot/matches/${id}`, `/riot/matches/${id}/app-users`]) {
      expect((await t.call("GET", path, me.token)).status).toBe(403);
    }
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("적어 둔 경기에 친구가 없으면 친구 경기도 Riot에 묻지 않고 403이다", async () => {
    const t = setup();
    const me = await t.login();
    const friend = await t.login();
    await makeFriends(me, friend);
    const id = crypto.randomUUID();
    await recordPlayers(id, [me.puuid, ...strangers(9).map((p) => p.puuid)]);
    const res = await t.call("GET", `/friends/${friend.puuid}/matches/${id}`, me.token);
    expect(await res.json()).toEqual({ error: "friend_not_in_match" });
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("연동을 해제해도 경기 참가 기록은 남는다", async () => {
    const t = setup();
    const me = await t.login();
    const id = crypto.randomUUID();
    t.upstream.json(matchUrl(id), matchFixture(id, [me, ...strangers(9)]));
    await t.call("GET", `/riot/matches/${id}`, me.token);
    expect((await t.call("DELETE", "/me", me.token)).status).toBe(204);
    expect(await recordedPlayers(id)).toContain(me.puuid);
  });
});
