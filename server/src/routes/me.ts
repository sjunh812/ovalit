import { Hono } from "hono";
import type { AppEnv, User } from "../env";
import { ApiError } from "../errors";
import { perUser } from "../limit";
import { notify } from "../push";
import { requireSession } from "../session";
import { SUMMARY_COLUMNS, type SummaryRow, sameRows, summaryRows, week } from "../summaries";
import { jsonBody, text } from "../validate";

// 앱을 다시 깔거나 기기를 바꾸면 토큰이 새로 나온다.
// 옛 토큰은 FCM이 404를 줄 때 지우는데 그 전까지는 알림 하나가 토큰 수만큼 하위 요청을 쓰니 사람마다 최근 셋만 둔다.
const MAX_PUSH_TOKENS = 3;
const MAX_PUSH_TOKEN_LENGTH = 4096;
const HOUR_MS = 60 * 60 * 1000;

// D1에 쓰는 경로를 사람마다 센다.
// D1 하루 쓰기 한도(10만 행)는 모두의 로그인과 같이 쓰니 계정 하나가 되풀이해 불러 다 쓰지 못하게 한다.
// 앱은 설정을 바꿀 때, 켤 때마다 토큰을 맡길 때, 새 경기를 받았을 때만 부르니 넉넉히 둔다.
// 시작 기준선이다.
const settingsWrites = perUser(30, HOUR_MS);
const pushTokenWrites = perUser(30, HOUR_MS);
const summaryUploads = perUser(30, HOUR_MS);

export const me = new Hono<AppEnv>();

me.use(requireSession);

// ㅇㅂㅇ 미리 알림을 시작 몇 분 전에 받을지 고를 수 있는 값이다.
// 0은 받지 않는다.
// 스키마의 CHECK와 같아야 한다.
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
me.patch("/", settingsWrites, async (c) => {
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
 * 그 주의 고정 칸 합계를 올립니다(docs/backend.md 친구 비교 요약).
 * 그 주의 줄을 통째로 바꿉니다.
 * 빈 목록이면 그 주의 요약을 지웁니다.
 * 같은 값을 다시 올리면 D1에 쓰지 않고 204입니다.
 */
me.put("/summaries/:week", summaryUploads, async (c) => {
  const weekStart = week(c.req.param("week"));
  const rows = summaryRows((await jsonBody(c)).rows);

  const db = c.env.DB;
  const user = c.var.user.id;
  const { results: stored } = await db
    .prepare(`SELECT act, queue, ${SUMMARY_COLUMNS.join(", ")} FROM weekly_summaries WHERE user_id = ? AND week = ?`)
    .bind(user, weekStart)
    .all<SummaryRow>();
  if (sameRows(stored, rows)) return c.body(null, 204);

  const now = Date.now();
  const insert = `INSERT INTO weekly_summaries (user_id, week, act, queue, ${SUMMARY_COLUMNS.join(", ")}, updated_at)
    VALUES (?, ?, ?, ?, ${SUMMARY_COLUMNS.map(() => "?").join(", ")}, ?)`;
  // 한 배치는 한 트랜잭션이라 지우고 넣는 사이에 친구가 빈 주를 받지 않는다
  await db.batch([
    db.prepare("DELETE FROM weekly_summaries WHERE user_id = ? AND week = ?").bind(user, weekStart),
    ...rows.map((row) =>
      db.prepare(insert).bind(user, weekStart, row.act, row.queue, ...SUMMARY_COLUMNS.map((column) => row[column]), now),
    ),
  ]);
  return c.body(null, 204);
});

/**
 * 연동 해제입니다.
 * 사용자 한 줄을 지우면 세션, 친구, 요청, 초대, ㅇㅂㅇ과 불려 간 자리, 기기 토큰이 스키마의 CASCADE로 같이 지워집니다.
 * 띄워 둔 ㅇㅂㅇ이 살아 있으면 불려 간 친구에게 호스트가 취소했을 때와 같은 알림을 보냅니다.
 */
me.delete("/", async (c) => {
  const db = c.env.DB;
  const user = c.var.user;
  // 지운 뒤에는 누구를 불렀는지 알 수 없어 먼저 읽는다
  const { results: invited } = await db
    .prepare(
      `SELECT m.ping_id, m.user_id FROM pings p JOIN ping_members m ON m.ping_id = p.id
       WHERE p.host = ? AND p.canceled = 0 AND p.expires_at > ?`,
    )
    .bind(user.id, Date.now())
    .all<{ ping_id: string; user_id: number }>();
  await db.prepare("DELETE FROM users WHERE id = ?").bind(user.id).run();
  notify(
    c,
    invited.map((row) => ({ userId: row.user_id, data: { type: "ping_cancel", pingId: row.ping_id, hostName: user.gameName } })),
  );
  return c.body(null, 204);
});

/**
 * 다른 계정이 쓰던 토큰이면 지금 계정으로 옮깁니다.
 * 한 기기에서 계정을 바꿔 연동하면 알림도 새 계정 것만 받습니다.
 * 토큰은 이 요청의 세션에 묶여 로그아웃하거나 세션이 끝나면 같이 지워집니다(migrations/0012).
 */
me.put("/push-token", pushTokenWrites, async (c) => {
  const token = text((await jsonBody(c)).token, MAX_PUSH_TOKEN_LENGTH, "invalid_push_token");
  const db = c.env.DB;
  const userId = c.var.user.id;
  await db.batch([
    db
      .prepare(
        `INSERT INTO push_tokens (token, user_id, session_hash, updated_at) VALUES (?1, ?2, ?3, ?4)
         ON CONFLICT (token) DO UPDATE
         SET user_id = excluded.user_id, session_hash = excluded.session_hash, updated_at = excluded.updated_at`,
      )
      .bind(token, userId, c.var.sessionHash, Date.now()),
    db
      .prepare(
        `DELETE FROM push_tokens WHERE user_id = ?1 AND token NOT IN (
           SELECT token FROM push_tokens WHERE user_id = ?1 ORDER BY updated_at DESC, rowid DESC LIMIT ?2)`,
      )
      .bind(userId, MAX_PUSH_TOKENS),
  ]);
  return c.body(null, 204);
});

/**
 * 내 토큰일 때만 지웁니다.
 * 지울 토큰이 없어도 204입니다.
 * 로그아웃하면 서버가 같이 지우니 알림만 끌 때 부릅니다.
 */
me.delete("/push-token", pushTokenWrites, async (c) => {
  const token = text((await jsonBody(c)).token, MAX_PUSH_TOKEN_LENGTH, "invalid_push_token");
  await c.env.DB.prepare("DELETE FROM push_tokens WHERE token = ? AND user_id = ?").bind(token, c.var.user.id).run();
  return c.body(null, 204);
});
