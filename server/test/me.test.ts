import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { setup, userId } from "./helpers";

afterEach(() => {
  vi.useRealTimers();
});

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

  it("오발있 미리 알림 시간을 바꿔도 전적 공개는 그대로다", async () => {
    const t = setup();
    const user = await t.login();
    await t.call("PATCH", "/me", user.token, { statsPublic: false });
    const changed = await t.call("PATCH", "/me", user.token, { remindBefore: 30 });
    expect(await changed.json()).toMatchObject({ statsPublic: false, remindBefore: 30 });
    expect(await (await t.call("GET", "/me", user.token)).json()).toMatchObject({ statsPublic: false, remindBefore: 30 });
  });

  it("미리 알림은 0, 10, 30, 60분만 받는다", async () => {
    const t = setup();
    const user = await t.login();
    expect((await t.call("PATCH", "/me", user.token, { remindBefore: 15 })).status).toBe(400);
    expect((await t.call("PATCH", "/me", user.token, { remindBefore: "10" })).status).toBe(400);
    expect((await t.call("PATCH", "/me", user.token, {})).status).toBe(400);
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

  it("설정 바꾸기는 한 사람이 한 시간에 30번까지이고 넘으면 쓰지 않고 429다", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const t = setup();
    const user = await t.login();
    const other = await t.login();
    for (let i = 0; i < 30; i++) expect((await t.call("PATCH", "/me", user.token, { statsPublic: i % 2 === 0 })).status).toBe(200);
    const over = await t.call("PATCH", "/me", user.token, { statsPublic: true });
    expect(over.status).toBe(429);
    expect(await over.json()).toEqual({ error: "too_many_requests" });
    expect(over.headers.get("Retry-After")).toBe("3600");
    expect(await (await t.call("GET", "/me", user.token)).json()).toMatchObject({ statsPublic: false });
    // 한도는 사람마다 따로 센다.
    expect((await t.call("PATCH", "/me", other.token, { statsPublic: false })).status).toBe(200);
    vi.advanceTimersByTime(60 * 60 * 1000);
    expect((await t.call("PATCH", "/me", user.token, { statsPublic: true })).status).toBe(200);
  });

  it("기기 토큰 맡기기와 지우기는 합쳐서 한 시간에 30번까지다", async () => {
    const t = setup();
    const user = await t.login();
    for (let i = 0; i < 15; i++) {
      expect((await t.call("PUT", "/me/push-token", user.token, { token: `device-${i}` })).status).toBe(204);
      expect((await t.call("DELETE", "/me/push-token", user.token, { token: `device-${i}` })).status).toBe(204);
    }
    const over = await t.call("PUT", "/me/push-token", user.token, { token: "device-over" });
    expect(over.status).toBe(429);
    expect(await count("SELECT COUNT(*) AS n FROM push_tokens WHERE token = ?", "device-over")).toBe(0);
  });
});
