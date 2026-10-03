import { Hono } from "hono";
import type { AppEnv, User } from "../env";
import { ApiError } from "../errors";
import { requireSession } from "../session";
import { jsonBody, text } from "../validate";

// 앱을 다시 깔거나 기기를 바꾸면 토큰이 새로 나온다. 옛 토큰은 FCM이 404를 줄 때 지우는데, 그 전까지는 알림 하나가
// 토큰 수만큼 하위 요청을 쓰니 사람마다 최근 셋만 둔다.
const MAX_PUSH_TOKENS = 3;
const MAX_PUSH_TOKEN_LENGTH = 4096;

export const me = new Hono<AppEnv>();

me.use(requireSession);

function profile(user: User) {
  return { puuid: user.puuid, gameName: user.gameName, tagLine: user.tagLine, statsPublic: user.statsPublic };
}

me.get("/", (c) => c.json(profile(c.var.user)));

me.patch("/", async (c) => {
  const { statsPublic } = await jsonBody(c);
  if (typeof statsPublic !== "boolean") throw new ApiError(400, "invalid_stats_public");
  await c.env.DB.prepare("UPDATE users SET stats_public = ?, updated_at = ? WHERE id = ?")
    .bind(statsPublic ? 1 : 0, Date.now(), c.var.user.id)
    .run();
  return c.json(profile({ ...c.var.user, statsPublic }));
});

/**
 * 연동 해제입니다. 사용자 한 줄을 지우면 세션, 친구, 요청, 초대, 오발있과 불려 간 자리, 기기 토큰이 스키마의
 * CASCADE로 같이 지워집니다.
 */
me.delete("/", async (c) => {
  await c.env.DB.prepare("DELETE FROM users WHERE id = ?").bind(c.var.user.id).run();
  return c.body(null, 204);
});

/** 다른 계정이 쓰던 토큰이면 지금 계정으로 옮깁니다. 한 기기에서 계정을 바꿔 연동하면 알림도 새 계정 것만 받습니다. */
me.put("/push-token", async (c) => {
  const token = text((await jsonBody(c)).token, MAX_PUSH_TOKEN_LENGTH, "invalid_push_token");
  const db = c.env.DB;
  const userId = c.var.user.id;
  await db.batch([
    db
      .prepare(
        `INSERT INTO push_tokens (token, user_id, updated_at) VALUES (?1, ?2, ?3)
         ON CONFLICT (token) DO UPDATE SET user_id = excluded.user_id, updated_at = excluded.updated_at`,
      )
      .bind(token, userId, Date.now()),
    db
      .prepare(
        `DELETE FROM push_tokens WHERE user_id = ?1 AND token NOT IN (
           SELECT token FROM push_tokens WHERE user_id = ?1 ORDER BY updated_at DESC, rowid DESC LIMIT ?2)`,
      )
      .bind(userId, MAX_PUSH_TOKENS),
  ]);
  return c.body(null, 204);
});

/** 내 토큰일 때만 지웁니다. 없어도 204입니다. 로그아웃하기 전에 부릅니다. */
me.delete("/push-token", async (c) => {
  const token = text((await jsonBody(c)).token, MAX_PUSH_TOKEN_LENGTH, "invalid_push_token");
  await c.env.DB.prepare("DELETE FROM push_tokens WHERE token = ? AND user_id = ?").bind(token, c.var.user.id).run();
  return c.body(null, 204);
});
