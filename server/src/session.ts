import { createMiddleware } from "hono/factory";
import { randomToken, sha256 } from "./crypto";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";

const SESSION_TTL_MS = 90 * 24 * 60 * 60 * 1000;
const BEARER = /^Bearer ([A-Za-z0-9_-]{43})$/;
// 크론에 맡기지 않고 누가 로그인하든 만료된 세션을 같이 치운다. 쌓인 게 많아도 요청 하나가 지우는 줄은 50개로 묶는다.
const CLEANUP_LIMIT = 50;

export async function createSession(db: D1Database, userId: number): Promise<{ token: string; expiresAt: number }> {
  const token = randomToken();
  const now = Date.now();
  const expiresAt = now + SESSION_TTL_MS;
  await db.batch([
    db
      .prepare("DELETE FROM sessions WHERE rowid IN (SELECT rowid FROM sessions WHERE expires_at <= ? LIMIT ?)")
      .bind(now, CLEANUP_LIMIT),
    db
      .prepare("INSERT INTO sessions (token_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")
      .bind(await sha256(token), userId, now, expiresAt),
  ]);
  return { token, expiresAt };
}

interface SessionRow {
  id: number;
  puuid: string;
  game_name: string;
  tag_line: string;
  stats_public: number;
  remind_before: number;
  expires_at: number;
}

/** 전적에 닿는 경로는 모두 이 미들웨어 뒤에 둡니다. RSO 인증 전에는 어떤 전적도 요청하지 않습니다. */
export const requireSession = createMiddleware<AppEnv>(async (c, next) => {
  const match = BEARER.exec(c.req.header("Authorization") ?? "");
  if (!match) throw new ApiError(401, "unauthorized");
  const hash = await sha256(match[1]!);
  const row = await c.env.DB.prepare(
    `SELECT u.id, u.puuid, u.game_name, u.tag_line, u.stats_public, u.remind_before, s.expires_at
     FROM sessions s JOIN users u ON u.id = s.user_id
     WHERE s.token_hash = ?`,
  )
    .bind(hash)
    .first<SessionRow>();
  if (!row) throw new ApiError(401, "unauthorized");
  if (row.expires_at <= Date.now()) {
    await c.env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(hash).run();
    throw new ApiError(401, "unauthorized");
  }
  c.set("sessionHash", hash);
  c.set("user", {
    id: row.id,
    puuid: row.puuid,
    gameName: row.game_name,
    tagLine: row.tag_line,
    statsPublic: row.stats_public === 1,
    remindBefore: row.remind_before,
  });
  await next();
});
