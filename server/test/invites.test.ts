import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { addFriends, makeFriends, sendRequestRow, setup, type TestUser, userId } from "./helpers";

type T = ReturnType<typeof setup>;

async function invite(t: T, user: TestUser) {
  const res = await t.call("POST", "/invites", user.token);
  expect(res.status).toBe(201);
  return res.json<{ code: string; url: string; expiresAt: number }>();
}

async function uses(code: string): Promise<number> {
  const row = await env.DB.prepare("SELECT uses FROM invites WHERE code = ?").bind(code).first<{ uses: number }>();
  return row!.uses;
}

describe("초대 링크", () => {
  it("7일짜리 링크를 만든다", async () => {
    const t = setup();
    const me = await t.login();
    const { code, url, expiresAt } = await invite(t, me);
    expect(code).toMatch(/^[2-9A-HJ-NP-Z]{12}$/);
    expect(url).toBe(`http://localhost/i/${code}`);
    expect(expiresAt - Date.now()).toBeGreaterThan(6.9 * 24 * 60 * 60 * 1000);
  });

  it("살아 있는 링크가 있으면 새로 만들지 않고 그 링크를 준다", async () => {
    const t = setup();
    const me = await t.login();
    const first = await invite(t, me);
    const again = await t.call("POST", "/invites", me.token);
    expect(again.status).toBe(200);
    expect(await again.json()).toEqual(first);
    const row = await env.DB.prepare("SELECT COUNT(*) AS n FROM invites WHERE user_id = ?")
      .bind(await userId(me.puuid))
      .first<{ n: number }>();
    expect(row!.n).toBe(1);
  });

  it("기한이 하루도 안 남은 링크는 다시 주지 않고 새로 만든다", async () => {
    const t = setup();
    const me = await t.login();
    const old = await invite(t, me);
    await env.DB.prepare("UPDATE invites SET expires_at = ? WHERE code = ?").bind(Date.now() + 60 * 60 * 1000, old.code).run();
    const fresh = await invite(t, me);
    expect(fresh.code).not.toBe(old.code);
    expect(fresh.expiresAt - Date.now()).toBeGreaterThan(6.9 * 24 * 60 * 60 * 1000);
  });

  it("만료된 초대는 다른 사람이 링크를 만들 때도 치운다", async () => {
    const t = setup();
    const inviter = await t.login();
    const someoneElse = await t.login();
    const { code } = await invite(t, inviter);
    await env.DB.prepare("UPDATE invites SET expires_at = ? WHERE code = ?").bind(Date.now() - 1, code).run();
    await invite(t, someoneElse);
    expect(await env.DB.prepare("SELECT 1 FROM invites WHERE code = ?").bind(code).first()).toBeNull();
  });

  it("받은 사람이 열면 초대한 사람에게 요청이 가고, 수락하면 친구가 된다", async () => {
    const t = setup();
    const inviter = await t.login("inviter");
    const guest = await t.login("guest");
    const { code } = await invite(t, inviter);

    const res = await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(res.status).toBe(201);
    expect(await res.json()).toEqual({ status: "requested" });
    const requests = await (await t.call("GET", "/friends/requests", inviter.token)).json();
    expect(requests).toMatchObject({ received: [{ puuid: guest.puuid, source: "invite_link" }] });

    await t.call("POST", `/friends/requests/${guest.puuid}/accept`, inviter.token);
    const friends = await (await t.call("GET", "/friends", guest.token)).json<{ puuid: string }[]>();
    expect(friends.map((f) => f.puuid)).toEqual([inviter.puuid]);
  });

  it("한 링크를 여럿이 쓸 수 있다", async () => {
    const t = setup();
    const inviter = await t.login();
    const { code } = await invite(t, inviter);
    for (const guest of [await t.login(), await t.login()]) {
      expect((await t.call("POST", `/invites/${code}/redeem`, guest.token)).status).toBe(201);
    }
  });

  it("링크 하나로는 요청을 20개까지 만들고, 다 쓰면 새 링크를 준다", async () => {
    const t = setup();
    const inviter = await t.login();
    const { code } = await invite(t, inviter);
    expect((await t.call("POST", `/invites/${code}/redeem`, (await t.login()).token)).status).toBe(201);
    expect(await uses(code)).toBe(1);

    await env.DB.prepare("UPDATE invites SET uses = 20 WHERE code = ?").bind(code).run();
    const late = await t.login();
    const res = await t.call("POST", `/invites/${code}/redeem`, late.token);
    expect(res.status).toBe(410);
    expect(await res.json()).toEqual({ error: "invite_used_up" });
    expect(await (await t.call("GET", "/friends/requests", inviter.token)).json()).toMatchObject({ received: [expect.anything()] });
    // 하루 한도는 요청을 만든 만큼만 센다
    const row = await env.DB.prepare("SELECT requests_today FROM users WHERE puuid = ?").bind(late.puuid).first<{ requests_today: number }>();
    expect(row!.requests_today).toBe(0);

    const fresh = await invite(t, inviter);
    expect(fresh.code).not.toBe(code);
  });

  it("이미 친구거나 요청이 가 있으면 링크 횟수를 쓰지 않는다", async () => {
    const t = setup();
    const inviter = await t.login();
    const friend = await t.login();
    const guest = await t.login();
    await makeFriends(inviter, friend);
    const { code } = await invite(t, inviter);
    await t.call("POST", `/invites/${code}/redeem`, friend.token);
    await t.call("POST", `/invites/${code}/redeem`, guest.token);
    await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(await uses(code)).toBe(1);
  });

  // 한도에 걸린 사람이 거듭 눌러 남의 링크를 다 쓰게 하지 못한다
  it("하루 요청 한도에 걸리면 링크 횟수를 쓰지 않는다", async () => {
    const t = setup();
    const inviter = await t.login();
    const guest = await t.login();
    const today = Math.floor(Date.now() / (24 * 60 * 60 * 1000));
    await env.DB.prepare("UPDATE users SET requests_day = ?, requests_today = 30 WHERE puuid = ?").bind(today, guest.puuid).run();
    const { code } = await invite(t, inviter);
    for (let i = 0; i < 3; i++) {
      expect((await t.call("POST", `/invites/${code}/redeem`, guest.token)).status).toBe(429);
    }
    expect(await uses(code)).toBe(0);
  });

  it("초대한 사람의 친구가 200명이면 요청을 만들지 않는다", async () => {
    const t = setup();
    const inviter = await t.login();
    const guest = await t.login();
    await addFriends(inviter, 200);
    const { code } = await invite(t, inviter);
    const res = await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(res.status).toBe(409);
    expect(await res.json()).toEqual({ error: "their_friends_full" });
    expect(await uses(code)).toBe(0);
  });

  it("내 링크는 내가 쓸 수 없다", async () => {
    const t = setup();
    const me = await t.login();
    const { code } = await invite(t, me);
    const res = await t.call("POST", `/invites/${code}/redeem`, me.token);
    expect(res.status).toBe(400);
    expect(await res.json()).toEqual({ error: "own_invite" });
  });

  it("만료된 링크는 410이다", async () => {
    const t = setup();
    const inviter = await t.login();
    const guest = await t.login();
    const { code } = await invite(t, inviter);
    await env.DB.prepare("UPDATE invites SET expires_at = ? WHERE code = ?").bind(Date.now() - 1, code).run();
    const res = await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(res.status).toBe(410);
    expect(await res.json()).toEqual({ error: "invite_expired" });
  });

  it("없는 링크는 404다", async () => {
    const t = setup();
    const guest = await t.login();
    expect((await t.call("POST", "/invites/ZZZZZZZZZZZZ/redeem", guest.token)).status).toBe(404);
    expect((await t.call("POST", "/invites/nope/redeem", guest.token)).status).toBe(404);
  });

  it("이미 친구면 아무것도 바꾸지 않는다", async () => {
    const t = setup();
    const inviter = await t.login();
    const guest = await t.login();
    await makeFriends(inviter, guest);
    const { code } = await invite(t, inviter);
    const res = await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ status: "already_friends" });
    expect(await (await t.call("GET", "/friends/requests", inviter.token)).json()).toEqual({ received: [], sent: [] });
  });

  it("초대한 사람이 먼저 요청을 보냈으면 수락하라고 409를 준다", async () => {
    const t = setup();
    const inviter = await t.login();
    const guest = await t.login();
    await sendRequestRow(inviter, guest);
    const { code } = await invite(t, inviter);
    const res = await t.call("POST", `/invites/${code}/redeem`, guest.token);
    expect(res.status).toBe(409);
    expect(await res.json()).toEqual({ error: "already_requested_you" });
  });

  it("링크 페이지는 앱 딥링크만 걸고 밖에서 아무것도 불러오지 않는다", async () => {
    const t = setup();
    const inviter = await t.login();
    const { code } = await invite(t, inviter);
    const res = await t.call("GET", `/i/${code}`);
    expect(res.status).toBe(200);
    expect(res.headers.get("Content-Type")).toContain("text/html");
    expect(res.headers.get("Content-Security-Policy")).toContain("default-src 'none'");
    expect(res.headers.get("Content-Security-Policy")).toContain("frame-ancestors 'none'");
    const page = await res.text();
    expect(page).toContain(`href="intent://localhost/i/${code}#Intent;scheme=http;package=com.ovalit;end"`);
    expect(page).not.toContain("ovalit://");
    // 미리보기 태그의 주소도 이 서버를 가리킨다.
    for (const [address] of page.matchAll(/https?:\/\/[^"]+/g)) expect(address).toMatch(/^http:\/\/localhost\//);
    expect(page).not.toMatch(/<script|<img|<link/i);
  });

  it("단톡방 미리보기 카드에 쓸 Open Graph 태그를 둔다", async () => {
    const t = setup();
    const inviter = await t.login("초대한사람");
    const { code } = await invite(t, inviter);
    const page = await (await t.call("GET", `/i/${code.toLowerCase()}`)).text();
    const og = new Map([...page.matchAll(/<meta property="og:([a-z:_]+)" content="([^"]*)">/g)].map((m) => [m[1], m[2]]));
    expect(og.get("title")).toBe("오발있 친구 초대");
    expect(og.get("description")).toBe("친구가 오발있에서 같이 기록을 보자고 초대했어요");
    expect(og.get("url")).toBe(`http://localhost/i/${code}`);
    expect(og.get("image")).toMatch(/^http:\/\/localhost\/og\/invite-v\d+\.png$/);
    expect(og.get("image:width")).toBe("1200");
    expect(og.get("image:height")).toBe("630");
    // 링크를 누가 만들었는지는 카드에 싣지 않는다.
    expect(page).not.toContain("초대한사람");
  });

  it("모양이 틀린 코드의 링크 페이지는 404다", async () => {
    const t = setup();
    const res = await t.call("GET", "/i/%3Cscript%3E");
    expect(res.status).toBe(404);
  });
});
