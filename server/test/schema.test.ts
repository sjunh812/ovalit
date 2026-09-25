import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { createApp } from "../src/app";

describe("뼈대", () => {
  it("health는 세션 없이 열린다", async () => {
    const res = await createApp().request("/health", {}, env);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ ok: true });
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
});
