import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { makeFriends, sendRequestRow, setup, type TestUser } from "./helpers";

type T = ReturnType<typeof setup>;

async function invite(t: T, user: TestUser) {
  const res = await t.call("POST", "/invites", user.token);
  expect(res.status).toBe(201);
  return res.json<{ code: string; url: string; expiresAt: number }>();
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
    const page = await res.text();
    expect(page).toContain(`href="ovalit://invite/${code}"`);
    expect(page).not.toMatch(/https?:\/\//);
    expect(page).not.toMatch(/<script|<img|<link/i);
  });

  it("모양이 틀린 코드의 링크 페이지는 404다", async () => {
    const t = setup();
    const res = await t.call("GET", "/i/%3Cscript%3E");
    expect(res.status).toBe(404);
  });
});
