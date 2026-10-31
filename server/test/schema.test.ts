import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { createApp } from "../src/app";
import { setup } from "./helpers";

describe("뼈대", () => {
  it("health는 세션 없이 열린다", async () => {
    const res = await createApp().request("/health", {}, env);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ ok: true });
  });

  it("본문은 16KB까지 받고 넘으면 세션을 보기 전에 413이다", async () => {
    const t = setup();
    const user = await t.login();
    const big = await t.call("PUT", "/me/push-token", user.token, { token: "x".repeat(16 * 1024) });
    expect(big.status).toBe(413);
    expect(await big.json()).toEqual({ error: "payload_too_large" });
    expect((await t.call("POST", "/auth/session", undefined, { code: "x".repeat(16 * 1024) })).status).toBe(413);
    // 가장 긴 기기 토큰(4096자)은 들어간다.
    expect((await t.call("PUT", "/me/push-token", user.token, { token: "x".repeat(4096) })).status).toBe(204);
  });

  it("없는 경로는 JSON 404다", async () => {
    const res = await createApp().request("/nope", {}, env);
    expect(res.status).toBe(404);
    expect(await res.json()).toEqual({ error: "not_found" });
  });

  it("친구 한 쌍은 작은 id가 앞에 오는 한 줄로만 들어간다", async () => {
    const now = Date.now();
    const insert = env.DB.prepare(
      "INSERT INTO users (puuid, game_name, tag_line, created_at, updated_at) VALUES (?, 'a', 'kr1', ?, ?) RETURNING id",
    );
    const a = await insert.bind(`schema-a-${now}`, now, now).first<{ id: number }>();
    const b = await insert.bind(`schema-b-${now}`, now, now).first<{ id: number }>();
    const [small, large] = [a!.id, b!.id].sort((x, y) => x - y);
    const friendship = "INSERT INTO friendships (user_a, user_b, created_at) VALUES (?, ?, ?)";
    await expect(env.DB.prepare(friendship).bind(large, small, now).run()).rejects.toThrow();
    await env.DB.prepare(friendship).bind(small, large, now).run();
  });

  it("친구 요청 출처는 스코어보드와 초대 링크뿐이다", async () => {
    const now = Date.now();
    const insert = env.DB.prepare(
      "INSERT INTO users (puuid, game_name, tag_line, created_at, updated_at) VALUES (?, 'a', 'kr1', ?, ?) RETURNING id",
    );
    const a = await insert.bind(`source-a-${now}`, now, now).first<{ id: number }>();
    const b = await insert.bind(`source-b-${now}`, now, now).first<{ id: number }>();
    const request = "INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, ?, ?)";
    await expect(env.DB.prepare(request).bind(a!.id, b!.id, "search", now).run()).rejects.toThrow();
    await env.DB.prepare(request).bind(a!.id, b!.id, "invite_link", now).run();
  });

  it("오발있 대답은 정해 둔 넷이고 제안한 시각은 다른 시간에만 있다", async () => {
    const now = Date.now();
    const insert = env.DB.prepare(
      "INSERT INTO users (puuid, game_name, tag_line, created_at, updated_at) VALUES (?, 'a', 'kr1', ?, ?) RETURNING id",
    );
    const host = await insert.bind(`ping-host-${now}`, now, now).first<{ id: number }>();
    const friend = await insert.bind(`ping-friend-${now}`, now, now).first<{ id: number }>();
    const pingId = `schema-${now}`;
    await env.DB.prepare("INSERT INTO pings (id, host, starts_at, created_at, expires_at) VALUES (?, ?, ?, ?, ?)")
      .bind(pingId, host!.id, now, now, now + 3_600_000)
      .run();
    const member = env.DB.prepare(
      "INSERT INTO ping_members (ping_id, user_id, position, answer, proposed_at, updated_at) VALUES (?, ?, 1, ?, ?, ?)",
    );
    for (const [answer, proposedAt] of [["maybe", null], ["yes", now], ["other_time", null]] as const) {
      await expect(member.bind(pingId, friend!.id, answer, proposedAt, now).run()).rejects.toThrow();
    }
    await member.bind(pingId, friend!.id, "other_time", now, now).run();
  });
});
