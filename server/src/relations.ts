import { ApiError } from "./errors";

export type Relation = "friend" | "requested_me" | "request_sent" | "app_user";

interface RelationRow {
  id: number;
  puuid: string;
  stats_public: number;
  friend: number;
  requested_me: number;
  request_sent: number;
}

export interface Related {
  id: number;
  statsPublic: boolean;
  relation: Relation;
}

// 바인딩은 쿼리당 100개까지다. 한 경기 인원은 이보다 훨씬 적다.
const MAX_PUUIDS = 99;

/** `puuids` 중 우리 앱에 연동한 사람과 나의 관계입니다. 연동하지 않은 사람은 결과에 없습니다. */
export async function relationsTo(db: D1Database, meId: number, puuids: string[]): Promise<Map<string, Related>> {
  if (puuids.length === 0) return new Map();
  if (puuids.length > MAX_PUUIDS) throw new ApiError(400, "too_many_players");
  const placeholders = puuids.map((_, i) => `?${i + 2}`).join(", ");
  const { results } = await db
    .prepare(
      `SELECT u.id, u.puuid, u.stats_public,
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
        statsPublic: row.stats_public === 1,
        relation: row.friend ? "friend" : row.requested_me ? "requested_me" : row.request_sent ? "request_sent" : "app_user",
      },
    ]),
  );
}
