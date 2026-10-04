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

// ㅇㅂㅇ 미리 알림을 시작 몇 분 전에 받을지 고를 수 있는 값이다. 0은 받지 않는다. 스키마의 CHECK와 같아야 한다.
const REMIND_BEFORE_CHOICES = [0, 10, 30, 60];

function profile(user: User) {
  return {
    puuid: user.puuid,
    gameName: user.gameName,
    tagLine: user.tagLine,
    statsPublic: user.statsPublic,
    remindBefore: user.remindBefore,
  };
}

me.get("/", (c) => c.json(profile(c.var.user)));

/** 보낸 값만 바꿉니다. 아무것도 보내지 않으면 400입니다. 두 요청이 겹쳐도 서로의 값을 되돌리지 않게 보낸 열만 씁니다. */
me.patch("/", async (c) => {
  const body = await jsonBody(c);
  const hasStatsPublic = "statsPublic" in body;
  const hasRemindBefore = "remindBefore" in body;
  if (!hasStatsPublic && !hasRemindBefore) throw new ApiError(400, "invalid_body");
  if (hasStatsPublic && typeof body.statsPublic !== "boolean") throw new ApiError(400, "invalid_stats_public");
  if (hasRemindBefore && !REMIND_BEFORE_CHOICES.includes(body.remindBefore as number)) {
    throw new ApiError(400, "invalid_remind_before");
  }
  const row = await c.env.DB.prepare(
    `UPDATE users SET stats_public = COALESCE(?1, stats_public), remind_before = COALESCE(?2, remind_before), updated_at = ?3
     WHERE id = ?4 RETURNING stats_public, remind_before`,
  )
    .bind(hasStatsPublic ? (body.statsPublic ? 1 : 0) : null, hasRemindBefore ? body.remindBefore : null, Date.now(), c.var.user.id)
    .first<{ stats_public: number; remind_before: number }>();
  return c.json(profile({ ...c.var.user, statsPublic: row!.stats_public === 1, remindBefore: row!.remind_before }));
});

/**
 * 연동 해제입니다. 사용자 한 줄을 지우면 세션, 친구, 요청, 초대, ㅇㅂㅇ과 불려 간 자리, 기기 토큰이 스키마의
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
