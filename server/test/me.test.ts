import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { setup, userId } from "./helpers";

async function count(sql: string, ...binds: unknown[]): Promise<number> {
  const row = await env.DB.prepare(sql).bind(...binds).first<{ n: number }>();
  return row!.n;
}

describe("내 계정", () => {
  it("전적 공개를 끄고 켤 수 있다", async () => {
    const t = setup();
    const user = await t.login();
    const off = await t.call("PATCH", "/me", user.token, { statsPublic: false });
    expect(await off.json()).toMatchObject({ statsPublic: false });
    expect(await (await t.call("GET", "/me", user.token)).json()).toMatchObject({ statsPublic: false });
    await t.call("PATCH", "/me", user.token, { statsPublic: true });
    expect(await (await t.call("GET", "/me", user.token)).json()).toMatchObject({ statsPublic: true });
  });

  it("전적 공개 값은 참거짓만 받는다", async () => {
    const t = setup();
    const user = await t.login();
    expect((await t.call("PATCH", "/me", user.token, { statsPublic: "no" })).status).toBe(400);
  });

  it("연동을 해제하면 세션, 친구, 요청, 초대가 모두 지워진다", async () => {
    const t = setup();
    const me = await t.login("me");
    const friend = await t.login("friend");
    const other = await t.login("other");
    const meId = await userId(me.puuid);
    const friendId = await userId(friend.puuid);
    const otherId = await userId(other.puuid);
    const now = Date.now();
    await env.DB.batch([
      env.DB.prepare("INSERT INTO friendships (user_a, user_b, created_at) VALUES (?, ?, ?)").bind(
        Math.min(meId, friendId),
        Math.max(meId, friendId),
        now,
      ),
      env.DB.prepare("INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, 'scoreboard', ?)").bind(
        meId,
        otherId,
        now,
      ),
      env.DB.prepare("INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, 'invite_link', ?)").bind(
        otherId,
        meId,
        now,
      ),
      env.DB.prepare("INSERT INTO invites (code, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)").bind(
        `CASCADE${String(now).slice(-5)}`,
        meId,
        now,
        now + 1000,
      ),
      env.DB.prepare("INSERT INTO login_codes (code_hash, user_id, challenge, expires_at) VALUES (?, ?, 'c', ?)").bind(
        `hash-${now}`,
        meId,
        now + 1000,
      ),
    ]);

    expect((await t.call("DELETE", "/me", me.token)).status).toBe(204);

    expect(await count("SELECT COUNT(*) AS n FROM users WHERE id = ?", meId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM sessions WHERE user_id = ?", meId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM friendships WHERE user_a = ?1 OR user_b = ?1", meId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM friend_requests WHERE from_user = ?1 OR to_user = ?1", meId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM invites WHERE user_id = ?", meId)).toBe(0);
    expect(await count("SELECT COUNT(*) AS n FROM login_codes WHERE user_id = ?", meId)).toBe(0);
    expect((await t.call("GET", "/me", me.token)).status).toBe(401);
    // 상대 계정은 그대로 남는다.
    expect((await t.call("GET", "/me", friend.token)).status).toBe(200);
  });
});
