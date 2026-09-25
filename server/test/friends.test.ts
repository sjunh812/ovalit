import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { makeFriends, matchFixture, matchUrl, RIOT, sendRequestRow, setup, strangers, type TestUser, userId } from "./helpers";

type T = ReturnType<typeof setup>;

function playedMatch(t: T, ...players: { puuid: string; gameName: string }[]): string {
  const id = crypto.randomUUID();
  t.upstream.json(matchUrl(id), matchFixture(id, [...players, ...strangers(10 - players.length)]));
  return id;
}

async function friendsOf(t: T, user: TestUser): Promise<string[]> {
  const list = await (await t.call("GET", "/friends", user.token)).json<{ puuid: string }[]>();
  return list.map((f) => f.puuid);
}

async function requestRows(from: TestUser, to: TestUser): Promise<number> {
  const row = await env.DB.prepare("SELECT COUNT(*) AS n FROM friend_requests WHERE from_user = ? AND to_user = ?")
    .bind(await userId(from.puuid), await userId(to.puuid))
    .first<{ n: number }>();
  return row!.n;
}

describe("스코어보드 친구 요청", () => {
  it("같이 뛴 경기가 있으면 요청이 가고 상대의 받은 요청에 뜬다", async () => {
    const t = setup();
    const me = await t.login("me");
    const other = await t.login("other");
    const matchId = playedMatch(t, me, other);

    const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
    expect(res.status).toBe(201);
    expect(await res.json()).toEqual({ status: "requested" });

    const received = await (await t.call("GET", "/friends/requests", other.token)).json();
    expect(received).toMatchObject({
      received: [{ puuid: me.puuid, gameName: "me", tagLine: "KR1", source: "scoreboard" }],
      sent: [],
    });
    const sent = await (await t.call("GET", "/friends/requests", me.token)).json();
    expect(sent).toEqual({ received: [], sent: [other.puuid] });
  });

  it("같이 뛰지 않은 경기로는 요청할 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const withoutMe = playedMatch(t, other);
    const withoutOther = playedMatch(t, me);
    for (const matchId of [withoutMe, withoutOther]) {
      const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
      expect(res.status).toBe(403);
      expect(await res.json()).toEqual({ error: "not_played_together" });
    }
    expect(await requestRows(me, other)).toBe(0);
  });

  it("앱을 안 쓰는 사람에게는 요청할 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const [outsider] = strangers(1);
    const matchId = playedMatch(t, me, outsider!);
    const res = await t.call("POST", "/friends/requests", me.token, { puuid: outsider!.puuid, matchId });
    expect(res.status).toBe(404);
    expect(await res.json()).toEqual({ error: "not_app_user" });
  });

  it("나에게는 요청할 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const matchId = playedMatch(t, me);
    expect((await t.call("POST", "/friends/requests", me.token, { puuid: me.puuid, matchId })).status).toBe(400);
  });

  it("상대가 먼저 보냈으면 수락하라고 409를 준다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await sendRequestRow(other, me);
    const matchId = playedMatch(t, me, other);
    const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
    expect(res.status).toBe(409);
    expect(await res.json()).toEqual({ error: "already_requested_you" });
  });

  it("이미 친구면 409다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await makeFriends(me, other);
    const matchId = playedMatch(t, me, other);
    const res = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
    expect(await res.json()).toEqual({ error: "already_friends" });
  });

  it("같은 요청을 다시 보내도 한 줄만 남는다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const matchId = playedMatch(t, me, other);
    await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
    const again = await t.call("POST", "/friends/requests", me.token, { puuid: other.puuid, matchId });
    expect(again.status).toBe(200);
    expect(await requestRows(me, other)).toBe(1);
  });
});

describe("수락, 거절, 끊기", () => {
  it("수락하면 서로의 친구 목록에 뜨고 요청은 지워진다", async () => {
    const t = setup();
    const me = await t.login("me");
    const other = await t.login("other");
    await sendRequestRow(other, me);
    const res = await t.call("POST", `/friends/requests/${other.puuid}/accept`, me.token);
    expect(await res.json()).toEqual({ status: "friends" });

    expect(await friendsOf(t, me)).toEqual([other.puuid]);
    expect(await friendsOf(t, other)).toEqual([me.puuid]);
    const [friend] = await (await t.call("GET", "/friends", me.token)).json<Record<string, unknown>[]>();
    expect(friend).toMatchObject({ puuid: other.puuid, gameName: "other", tagLine: "KR1", statsPublic: true });
    expect(friend!.since).toEqual(expect.any(Number));
    expect(await requestRows(other, me)).toBe(0);
  });

  it("받지 않은 요청은 수락할 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await sendRequestRow(me, other);
    const res = await t.call("POST", `/friends/requests/${other.puuid}/accept`, me.token);
    expect(res.status).toBe(404);
    expect(await friendsOf(t, me)).toEqual([]);
  });

  it("거절하면 요청만 지워지고 친구가 되지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await sendRequestRow(other, me);
    expect((await t.call("POST", `/friends/requests/${other.puuid}/decline`, me.token)).status).toBe(204);
    expect(await requestRows(other, me)).toBe(0);
    expect(await friendsOf(t, me)).toEqual([]);
    expect((await t.call("POST", `/friends/requests/${other.puuid}/decline`, me.token)).status).toBe(404);
  });

  it("끊으면 양쪽 목록에서 빠진다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await makeFriends(me, other);
    expect((await t.call("DELETE", `/friends/${other.puuid}`, me.token)).status).toBe(204);
    expect(await friendsOf(t, me)).toEqual([]);
    expect(await friendsOf(t, other)).toEqual([]);
    expect((await t.call("DELETE", `/friends/${other.puuid}`, me.token)).status).toBe(404);
  });
});

describe("친구 경기", () => {
  it("친구가 아니면 경기 목록을 볼 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    const res = await t.call("GET", `/friends/${other.puuid}/matchlist`, me.token);
    expect(res.status).toBe(403);
    expect(await res.json()).toEqual({ error: "not_friend" });
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("한쪽만 요청한 사이로는 볼 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const other = await t.login();
    await sendRequestRow(me, other);
    await sendRequestRow(other, me);
    expect((await t.call("GET", `/friends/${other.puuid}/matchlist`, me.token)).status).toBe(403);
  });

  it("친구의 경기 목록은 친구 PUUID로 받는다", async () => {
    const t = setup();
    const me = await t.login();
    const friend = await t.login();
    await makeFriends(me, friend);
    const url = `${RIOT}/val/match/v1/matchlists/by-puuid/${friend.puuid}`;
    t.upstream.json(url, { puuid: friend.puuid, history: [] });
    const res = await t.call("GET", `/friends/${friend.puuid}/matchlist`, me.token);
    expect(res.status).toBe(200);
    expect(t.upstream.callsTo(url)).toHaveLength(1);
  });

  it("전적을 공개하지 않은 친구의 경기는 열리지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const friend = await t.login();
    await makeFriends(me, friend);
    await t.call("PATCH", "/me", friend.token, { statsPublic: false });
    const matchId = playedMatch(t, me, friend);
    for (const path of [`/friends/${friend.puuid}/matchlist`, `/friends/${friend.puuid}/matches/${matchId}`]) {
      const res = await t.call("GET", path, me.token);
      expect(res.status).toBe(403);
      expect(await res.json()).toEqual({ error: "stats_private" });
    }
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("친구가 없는 경기는 열리지 않는다", async () => {
    const t = setup();
    const me = await t.login();
    const friend = await t.login();
    await makeFriends(me, friend);
    const matchId = playedMatch(t, await t.login());
    expect((await t.call("GET", `/friends/${friend.puuid}/matches/${matchId}`, me.token)).status).toBe(403);
  });

  it("같이 뛴 경기는 가리지 않고 그대로 준다", async () => {
    const t = setup();
    const me = await t.login();
    const friend = await t.login();
    await makeFriends(me, friend);
    const id = crypto.randomUUID();
    const fixture = matchFixture(id, [me, friend, ...strangers(8)]);
    t.upstream.json(matchUrl(id), fixture);
    const res = await t.call("GET", `/friends/${friend.puuid}/matches/${id}`, me.token);
    expect(await res.json()).toEqual(fixture);
  });

  describe("내가 안 뛴 친구 경기", () => {
    async function redacted() {
      const t = setup();
      const me = await t.login("me");
      const friend = await t.login("friend");
      // 앱을 쓰지만 나와 친구가 아닌 사람도 가려야 한다.
      const appUser = await t.login("appUser");
      await makeFriends(me, friend);
      const others = [appUser, ...strangers(8)];
      const id = crypto.randomUUID();
      const party = crypto.randomUUID();
      const fixture = matchFixture(id, [
        { ...friend, partyId: party },
        { ...others[0]!, partyId: party },
        ...others.slice(1),
      ]);
      t.upstream.json(matchUrl(id), fixture);
      const res = await t.call("GET", `/friends/${friend.puuid}/matches/${id}`, me.token);
      const body = await res.text();
      return { res, body, match: JSON.parse(body) as typeof fixture, fixture, friend, others, party };
    }

    it("친구가 아닌 사람의 PUUID, 이름, 카드가 하나도 남지 않는다", async () => {
      const { res, body, match, others } = await redacted();
      expect(res.status).toBe(200);
      for (const other of others) {
        expect(body).not.toContain(other.puuid);
        expect(body).not.toContain(`"${other.gameName}"`);
      }
      for (const player of match.players.slice(1)) {
        expect(player.puuid).toMatch(/^anon-\d+$/);
        expect(player.gameName).toBe("");
        expect(player.tagLine).toBe("");
        expect(player).not.toHaveProperty("playerCard");
        expect(player).not.toHaveProperty("playerTitle");
      }
    });

    it("친구 본인은 그대로 남는다", async () => {
      const { match, friend } = await redacted();
      expect(match.players[0]).toMatchObject({ puuid: friend.puuid, gameName: "friend", tagLine: "KR1" });
      expect(match.players[0]).toHaveProperty("playerCard");
    });

    it("킬, 어시스트, 피해량이 가린 뒤에도 같은 사람을 가리킨다", async () => {
      const { match, fixture } = await redacted();
      const alias = new Map(fixture.players.map((p, i) => [p.puuid, match.players[i]!.puuid]));
      expect(new Set(alias.values()).size).toBe(fixture.players.length);

      fixture.roundResults.forEach((round, r) => {
        const after = match.roundResults[r]!;
        expect(after.bombPlanter).toBe(alias.get(round.bombPlanter));
        round.playerStats.forEach((stats, s) => {
          const out = after.playerStats[s]!;
          expect(out.puuid).toBe(alias.get(stats.puuid));
          stats.kills.forEach((kill, k) => {
            const killOut = out.kills[k]!;
            expect(killOut.killer).toBe(alias.get(kill!.killer));
            expect(killOut.victim).toBe(alias.get(kill!.victim));
            expect(killOut.assistants).toEqual(kill!.assistants.map((a) => alias.get(a)));
            expect(killOut.playerLocations.map((l) => l.puuid)).toEqual(kill!.playerLocations.map((l) => alias.get(l.puuid)));
          });
          expect(out.damage[0]!.receiver).toBe(alias.get(stats.damage[0]!.receiver));
        });
      });
    });

    it("파티 ID는 이 경기 안에서만 통하는 이름으로 바뀌고 같은 파티는 같이 묶인다", async () => {
      const { body, match, fixture, party } = await redacted();
      expect(body).not.toContain(party);
      for (const player of fixture.players) expect(body).not.toContain(player.partyId);
      expect(match.players[0]!.partyId).toBe(match.players[1]!.partyId);
      expect(match.players[0]!.partyId).toMatch(/^party-\d+$/);
      expect(match.players[2]!.partyId).not.toBe(match.players[0]!.partyId);
    });

    it("같은 경기를 다시 열어도 같은 이름으로 가린다", async () => {
      const first = await redacted();
      const t = setup();
      const me = await t.login();
      await makeFriends(me, first.friend);
      t.upstream.json(matchUrl(first.fixture.matchInfo.matchId), first.fixture);
      const again = await t.call("GET", `/friends/${first.friend.puuid}/matches/${first.fixture.matchInfo.matchId}`, me.token);
      expect(await again.text()).toBe(first.body);
    });
  });
});
