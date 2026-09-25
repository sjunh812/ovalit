import { type Context, Hono } from "hono";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { redactMatch } from "../redact";
import { type Relation, relationsTo } from "../relations";
import { playersIn, rawJson, riotFor } from "../riot";
import { requireSession } from "../session";
import * as validate from "../validate";

export const friends = new Hono<AppEnv>();

friends.use(requireSession);

friends.get("/", async (c) => {
  const { results } = await c.env.DB.prepare(
    `SELECT u.puuid, u.game_name, u.tag_line, u.stats_public, f.created_at
     FROM friendships f JOIN users u ON u.id = CASE WHEN f.user_a = ?1 THEN f.user_b ELSE f.user_a END
     WHERE f.user_a = ?1 OR f.user_b = ?1
     ORDER BY f.created_at DESC`,
  )
    .bind(c.var.user.id)
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

friends.get("/requests", async (c) => {
  const db = c.env.DB;
  const [received, sent] = await db.batch<Record<string, unknown>>([
    db
      .prepare(
        `SELECT u.puuid, u.game_name, u.tag_line, r.source, r.created_at
         FROM friend_requests r JOIN users u ON u.id = r.from_user
         WHERE r.to_user = ? ORDER BY r.created_at DESC`,
      )
      .bind(c.var.user.id),
    db
      .prepare("SELECT u.puuid FROM friend_requests r JOIN users u ON u.id = r.to_user WHERE r.from_user = ?")
      .bind(c.var.user.id),
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

export async function sendRequest(
  c: Context<AppEnv>,
  targetId: number,
  relation: Relation,
  source: "scoreboard" | "invite_link",
): Promise<Response> {
  if (relation === "friend") throw new ApiError(409, "already_friends");
  // 상대가 먼저 보냈으면 수락은 따로 누르게 한다. 요청을 겹쳐 두면 누가 누구를 기다리는지 흐려진다.
  if (relation === "requested_me") throw new ApiError(409, "already_requested_you");
  if (relation === "request_sent") return c.json({ status: "requested" });
  await c.env.DB.prepare(
    "INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
  )
    .bind(c.var.user.id, targetId, source, Date.now())
    .run();
  return c.json({ status: "requested" }, 201);
}

friends.post("/requests/:puuid/accept", async (c) => {
  const other = validate.puuid(c.req.param("puuid"));
  const me = c.var.user.id;
  const db = c.env.DB;
  const otherId = "(SELECT id FROM users WHERE puuid = ?1)";
  // 한 배치는 한 트랜잭션으로 돈다. 요청이 있을 때만 친구가 되고 양쪽 요청이 같이 지워진다.
  const [, forward] = await db.batch([
    db
      .prepare(
        `INSERT INTO friendships (user_a, user_b, created_at)
         SELECT MIN(from_user, to_user), MAX(from_user, to_user), ?3 FROM friend_requests
         WHERE from_user = ${otherId} AND to_user = ?2
         ON CONFLICT DO NOTHING`,
      )
      .bind(other, me, Date.now()),
    db.prepare(`DELETE FROM friend_requests WHERE from_user = ${otherId} AND to_user = ?2`).bind(other, me),
    db.prepare(`DELETE FROM friend_requests WHERE from_user = ?2 AND to_user = ${otherId}`).bind(other, me),
  ]);
  if (forward!.meta.changes === 0) throw new ApiError(404, "request_not_found");
  return c.json({ status: "friends" });
});

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
  const result = await c.env.DB.prepare(
    `DELETE FROM friendships
     WHERE user_a = (SELECT MIN(id, ?2) FROM users WHERE puuid = ?1)
       AND user_b = (SELECT MAX(id, ?2) FROM users WHERE puuid = ?1)`,
  )
    .bind(other, c.var.user.id)
    .run();
  if (result.meta.changes === 0) throw new ApiError(404, "not_friend");
  return c.body(null, 204);
});

/** 프로필과 경기는 서로 수락한 친구이고 그 친구가 전적을 공개했을 때만 엽니다. */
async function visibleFriend(c: Context<AppEnv>, puuid: string): Promise<string> {
  const row = await c.env.DB.prepare(
    `SELECT u.stats_public FROM users u
     JOIN friendships f ON f.user_a = MIN(u.id, ?1) AND f.user_b = MAX(u.id, ?1)
     WHERE u.puuid = ?2`,
  )
    .bind(c.var.user.id, puuid)
    .first<{ stats_public: number }>();
  if (!row) throw new ApiError(403, "not_friend");
  if (row.stats_public !== 1) throw new ApiError(403, "stats_private");
  return puuid;
}

friends.get("/:puuid/matchlist", async (c) => {
  const friend = await visibleFriend(c, validate.puuid(c.req.param("puuid")));
  return rawJson(c, await riotFor(c).matchlist(friend));
});

friends.get("/:puuid/matches/:matchId", async (c) => {
  const friend = await visibleFriend(c, validate.puuid(c.req.param("puuid")));
  const match = await riotFor(c).matchWith(validate.matchId(c.req.param("matchId")), friend);
  if (!match) throw new ApiError(403, "friend_not_in_match");
  // 내가 같이 뛴 경기면 다른 사람 기록도 원래 볼 수 있다.
  if (playersIn(match.data).has(c.var.user.puuid)) return rawJson(c, match.raw);
  return c.json(redactMatch(match.data, new Set([friend, c.var.user.puuid])));
});
