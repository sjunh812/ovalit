import { type Context, Hono } from "hono";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { FRIEND_CAP, friendCount, friendsFull, relationsTo, sendRequest } from "../relations";
import { rawJson, riotFor } from "../riot";
import { requireSession } from "../session";
import * as validate from "../validate";

export const friends = new Hono<AppEnv>();

friends.use(requireSession);

// 친구는 FRIEND_CAP명까지라 한 번에 다 내려준다. 한도를 바꾸기 전에 쌓인 줄이 있어도 응답이 한도를 넘지 않게 LIMIT을 건다.
friends.get("/", async (c) => {
  const { results } = await c.env.DB.prepare(
    `SELECT u.puuid, u.game_name, u.tag_line, u.stats_public, f.created_at
     FROM friendships f JOIN users u ON u.id = CASE WHEN f.user_a = ?1 THEN f.user_b ELSE f.user_a END
     WHERE f.user_a = ?1 OR f.user_b = ?1
     ORDER BY f.created_at DESC
     LIMIT ?2`,
  )
    .bind(c.var.user.id, FRIEND_CAP)
    .all<{ puuid: string; game_name: string; tag_line: string; stats_public: number; created_at: number }>();
  return c.json(
    results.map((row) => ({
      puuid: row.puuid,
      gameName: row.game_name,
      tagLine: row.tag_line,
      statsPublic: row.stats_public === 1,
      since: row.created_at,
    })),
  );
});

// 받은 요청은 여러 사람이 보내 쌓일 수 있다. 친구 한도보다 많이 받아도 다 수락할 수 없으니 최근 것부터 한도만큼만 준다.
friends.get("/requests", async (c) => {
  const db = c.env.DB;
  const [received, sent] = await db.batch<Record<string, unknown>>([
    db
      .prepare(
        `SELECT u.puuid, u.game_name, u.tag_line, r.source, r.created_at
         FROM friend_requests r JOIN users u ON u.id = r.from_user
         WHERE r.to_user = ? ORDER BY r.created_at DESC LIMIT ?`,
      )
      .bind(c.var.user.id, FRIEND_CAP),
    db
      .prepare(
        `SELECT u.puuid FROM friend_requests r JOIN users u ON u.id = r.to_user
         WHERE r.from_user = ? ORDER BY r.created_at DESC LIMIT ?`,
      )
      .bind(c.var.user.id, FRIEND_CAP),
  ]);
  return c.json({
    received: received!.results.map((row) => ({
      puuid: row.puuid,
      gameName: row.game_name,
      tagLine: row.tag_line,
      source: row.source,
      createdAt: row.created_at,
    })),
    sent: sent!.results.map((row) => row.puuid),
  });
});

/** 스코어보드에서 보내는 요청입니다. 닉네임 검색이 없으니 같이 뛴 경기가 요청의 근거입니다. */
friends.post("/requests", async (c) => {
  const body = await validate.jsonBody(c);
  const target = validate.puuid(body.puuid);
  const matchId = validate.matchId(body.matchId);
  const me = c.var.user;
  if (target === me.puuid) throw new ApiError(400, "cannot_friend_self");

  // 경기부터 확인한다. 상대가 앱을 쓰는지는 같이 뛴 사람에게만 알려준다.
  const players = await riotFor(c).participants(matchId);
  if (!players.has(me.puuid) || !players.has(target)) throw new ApiError(403, "not_played_together");

  const related = (await relationsTo(c.env.DB, me.id, [target])).get(target);
  if (!related) throw new ApiError(404, "not_app_user");
  return sendRequest(c, related.id, related.relation, "scoreboard");
});

/**
 * 받은 요청을 수락합니다. 두 사람 중 한쪽이라도 친구가 [FRIEND_CAP]명이면 409 `friends_full`(내 쪽)이나
 * `their_friends_full`(상대 쪽)이고, 요청은 지우지 않고 남겨 친구를 정리한 뒤 다시 수락할 수 있게 합니다.
 */
friends.post("/requests/:puuid/accept", async (c) => {
  const other = validate.puuid(c.req.param("puuid"));
  const me = c.var.user.id;
  const db = c.env.DB;
  const otherId = "(SELECT id FROM users WHERE puuid = ?1)";
  const befriended = `EXISTS (SELECT 1 FROM friendships WHERE user_a = MIN(${otherId}, ?2) AND user_b = MAX(${otherId}, ?2))`;
  // 한 배치는 한 트랜잭션으로 돈다. 요청이 있고 두 사람 모두 자리가 있을 때만 친구가 된다. 요청은 친구가 됐을 때만 양쪽을
  // 같이 지운다. 동시에 여러 요청을 수락해도 한도 검사와 넣기가 한 문장이라 한도를 넘지 않는다.
  const [, forward] = await db.batch([
    db
      .prepare(
        `INSERT INTO friendships (user_a, user_b, created_at)
         SELECT MIN(from_user, to_user), MAX(from_user, to_user), ?3 FROM friend_requests
         WHERE from_user = ${otherId} AND to_user = ?2
           AND ${friendCount("?2")} < ?4 AND ${friendCount(otherId)} < ?4
         ON CONFLICT DO NOTHING`,
      )
      .bind(other, me, Date.now(), FRIEND_CAP),
    db.prepare(`DELETE FROM friend_requests WHERE from_user = ${otherId} AND to_user = ?2 AND ${befriended}`).bind(other, me),
    db.prepare(`DELETE FROM friend_requests WHERE from_user = ?2 AND to_user = ${otherId} AND ${befriended}`).bind(other, me),
  ]);
  if (forward!.meta.changes === 0) throw await acceptRefusal(db, me, other);
  return c.json({ status: "friends" });
});

// 수락이 들어가지 않은 까닭을 찾는다. 요청이 남아 있으면 한도에 걸린 것이다.
async function acceptRefusal(db: D1Database, me: number, other: string): Promise<ApiError> {
  const otherId = "(SELECT id FROM users WHERE puuid = ?2)";
  const row = await db
    .prepare(
      `SELECT EXISTS (SELECT 1 FROM friend_requests WHERE from_user = ${otherId} AND to_user = ?1) AS requested,
         ${friendCount("?1")} AS mine, ${friendCount(otherId)} AS theirs`,
    )
    .bind(me, other)
    .first<{ requested: number; mine: number; theirs: number }>();
  if (!row?.requested) return new ApiError(404, "request_not_found");
  return friendsFull(row.mine, row.theirs) ?? new ApiError(404, "request_not_found");
}

friends.post("/requests/:puuid/decline", async (c) => {
  const other = validate.puuid(c.req.param("puuid"));
  const result = await c.env.DB.prepare(
    "DELETE FROM friend_requests WHERE from_user = (SELECT id FROM users WHERE puuid = ?) AND to_user = ?",
  )
    .bind(other, c.var.user.id)
    .run();
  if (result.meta.changes === 0) throw new ApiError(404, "request_not_found");
  return c.body(null, 204);
});

friends.delete("/:puuid", async (c) => {
  const other = validate.puuid(c.req.param("puuid"));
  const db = c.env.DB;
  const otherId = "(SELECT id FROM users WHERE puuid = ?1)";
  const [removed] = await db.batch([
    db
      .prepare(
        `DELETE FROM friendships
         WHERE user_a = (SELECT MIN(id, ?2) FROM users WHERE puuid = ?1)
           AND user_b = (SELECT MAX(id, ?2) FROM users WHERE puuid = ?1)`,
      )
      .bind(other, c.var.user.id),
    // ㅇㅂㅇ은 친구만 부를 수 있으니 끊으면 서로 띄운 ㅇㅂㅇ에서도 빠진다.
    db
      .prepare(
        `DELETE FROM ping_members
         WHERE (user_id = ?2 AND ping_id IN (SELECT id FROM pings WHERE host = ${otherId}))
            OR (user_id = ${otherId} AND ping_id IN (SELECT id FROM pings WHERE host = ?2))`,
      )
      .bind(other, c.var.user.id),
  ]);
  if (removed!.meta.changes === 0) throw new ApiError(404, "not_friend");
  return c.body(null, 204);
});

/** 친구의 경기 목록과 경기는 서로 수락한 친구이고 그 친구가 전적을 공개했을 때만 엽니다. 아니면 403입니다. */
async function assertVisibleFriend(c: Context<AppEnv>, puuid: string): Promise<void> {
  const row = await c.env.DB.prepare(
    `SELECT u.stats_public FROM users u
     JOIN friendships f ON f.user_a = MIN(u.id, ?1) AND f.user_b = MAX(u.id, ?1)
     WHERE u.puuid = ?2`,
  )
    .bind(c.var.user.id, puuid)
    .first<{ stats_public: number }>();
  if (!row) throw new ApiError(403, "not_friend");
  if (row.stats_public !== 1) throw new ApiError(403, "stats_private");
}

friends.get("/:puuid/matchlist", async (c) => {
  const friend = validate.puuid(c.req.param("puuid"));
  await assertVisibleFriend(c, friend);
  return rawJson(c, await riotFor(c).friendMatchlist(friend));
});

friends.get("/:puuid/matches/:matchId", async (c) => {
  const friend = validate.puuid(c.req.param("puuid"));
  await assertVisibleFriend(c, friend);
  const riot = riotFor(c);
  const matchId = validate.matchId(c.req.param("matchId"));
  const match = await riot.matchWith(matchId, friend);
  if (!match) throw new ApiError(403, "friend_not_in_match");
  // 내가 같이 뛴 경기는 /riot/matches에서도 그대로 보이니 가리지 않는다.
  if (match.players.has(c.var.user.puuid)) return rawJson(c, match.raw);
  return rawJson(c, riot.redactedFor(matchId, match, friend, c.var.user.puuid));
});
