import type { Context } from "hono";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";

export type Relation = "friend" | "requested_me" | "request_sent" | "app_user";

interface RelationRow {
  id: number;
  puuid: string;
  friend: number;
  requested_me: number;
  request_sent: number;
}

export interface Related {
  id: number;
  relation: Relation;
}

// ?1이 내 id라 PUUID에 쓸 수 있는 바인딩은 99개다. D1은 쿼리당 바인딩을 100개까지 받는다.
const MAX_PUUIDS = 99;

/**
 * `puuids` 중 우리 앱에 연동한 사람마다 나와의 관계를 PUUID → `{ id, relation }`으로 돌려줍니다. 연동하지 않은 사람과
 * 나 자신은 결과에 없습니다. 99명을 넘기면 400 `too_many_players`입니다.
 */
export async function relationsTo(db: D1Database, meId: number, puuids: string[]): Promise<Map<string, Related>> {
  if (puuids.length === 0) return new Map();
  if (puuids.length > MAX_PUUIDS) throw new ApiError(400, "too_many_players");
  const placeholders = puuids.map((_, i) => `?${i + 2}`).join(", ");
  const { results } = await db
    .prepare(
      `SELECT u.id, u.puuid,
         EXISTS (SELECT 1 FROM friendships f WHERE f.user_a = MIN(u.id, ?1) AND f.user_b = MAX(u.id, ?1)) AS friend,
         EXISTS (SELECT 1 FROM friend_requests r WHERE r.from_user = u.id AND r.to_user = ?1) AS requested_me,
         EXISTS (SELECT 1 FROM friend_requests r WHERE r.from_user = ?1 AND r.to_user = u.id) AS request_sent
       FROM users u
       WHERE u.puuid IN (${placeholders}) AND u.id <> ?1`,
    )
    .bind(meId, ...puuids)
    .all<RelationRow>();
  return new Map(
    results.map((row) => [
      row.puuid,
      {
        id: row.id,
        relation: row.friend ? "friend" : row.requested_me ? "requested_me" : row.request_sent ? "request_sent" : "app_user",
      },
    ]),
  );
}

/**
 * 지금 관계에 맞춰 친구 요청을 보냅니다. 새로 보내면 201, 이미 보내 둔 요청이면 200 `{status: "requested"}`입니다.
 * 이미 친구면 409 `already_friends`, 상대가 먼저 보냈으면 409 `already_requested_you`입니다.
 */
export async function sendRequest(
  c: Context<AppEnv>,
  targetId: number,
  relation: Relation,
  source: "scoreboard" | "invite_link",
): Promise<Response> {
  // 스코어보드는 친구가 아닌 사람에게만 요청 버튼을 띄운다. 친구에게 요청이 왔다면 앱이 옛 관계를 들고 있는 것이라
  // 409로 알린다. 초대 링크는 친구가 다시 눌러도 되는 자리라 여기까지 오기 전에 200으로 답한다.
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
