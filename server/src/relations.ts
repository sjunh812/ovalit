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

/** 한 사람의 친구는 이만큼까지입니다. 친구 탭과 친구 비교가 한 번에 받는 목록도 이 수로 묶습니다. 시작 기준선입니다. */
export const FRIEND_CAP = 200;
// 하루(UTC)에 새로 보낼 수 있는 친구 요청이다. 몇 판 같이 뛴 사람에게 보내기에는 넉넉하고 아무에게나 뿌리는 건 막는다. 시작 기준선이다.
export const DAILY_REQUEST_LIMIT = 30;
// 초대 링크 하나로 만들 수 있는 요청이다. 단톡방 하나에 올리는 데는 넉넉하고, 밖으로 퍼진 링크는 여기서 멈춘다. 시작 기준선이다.
export const INVITE_REQUEST_LIMIT = 20;

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * 친구 수를 세는 SQL 조각입니다. 한 쌍을 (작은 id, 큰 id) 한 줄로 두니 어느 쪽에 있든 셉니다. OR로 묶으면 색인을 못 타서
 * 기본 키와 `friendships_user_b` 색인을 하나씩 타게 나눕니다.
 */
export function friendCount(user: string): string {
  return `((SELECT COUNT(*) FROM friendships WHERE user_a = ${user}) + (SELECT COUNT(*) FROM friendships WHERE user_b = ${user}))`;
}

/** 두 사람 중 한쪽이라도 친구가 다 찼으면 409입니다. 내 쪽이면 `friends_full`, 상대 쪽이면 `their_friends_full`입니다. */
export function friendsFull(mine: number, theirs: number): ApiError | null {
  if (mine >= FRIEND_CAP) return new ApiError(409, "friends_full");
  if (theirs >= FRIEND_CAP) return new ApiError(409, "their_friends_full");
  return null;
}

/**
 * 오늘(UTC) 보낸 요청을 하나 더 셉니다. 이미 [DAILY_REQUEST_LIMIT]번 보냈으면 세지 않고 429 `too_many_friend_requests`를
 * 던집니다. `Retry-After`는 다음 날 0시(UTC)까지입니다.
 */
async function countRequest(db: D1Database, userId: number, now: number): Promise<void> {
  const today = Math.floor(now / DAY_MS);
  const counted = await db
    .prepare(
      `UPDATE users
       SET requests_today = CASE WHEN requests_day = ?2 THEN requests_today + 1 ELSE 1 END, requests_day = ?2
       WHERE id = ?1 AND (requests_day <> ?2 OR requests_today < ?3)`,
    )
    .bind(userId, today, DAILY_REQUEST_LIMIT)
    .run();
  if (counted.meta.changes === 0) {
    const retryAfter = Math.max(1, Math.ceil(((today + 1) * DAY_MS - now) / 1000));
    throw new ApiError(429, "too_many_friend_requests", { "Retry-After": String(retryAfter) });
  }
}

/** 초대 링크로 보낸 요청입니다. [uses]는 링크를 찾을 때 읽은 값이라 다 쓴 링크는 하루 한도를 세기 전에 거릅니다. */
export interface InviteUse {
  code: string;
  uses: number;
}

/**
 * 지금 관계에 맞춰 친구 요청을 보냅니다. 새로 보내면 201, 이미 보내 둔 요청이면 200 `{status: "requested"}`입니다.
 * 이미 친구면 409 `already_friends`, 상대가 먼저 보냈으면 409 `already_requested_you`입니다.
 *
 * 새로 보낼 때만 한도를 봅니다. 두 사람 중 한쪽이라도 친구가 [FRIEND_CAP]명이면 409, 오늘 [DAILY_REQUEST_LIMIT]번 보냈으면
 * 429, 초대 링크로 [INVITE_REQUEST_LIMIT]번 요청을 만들었으면 410 `invite_used_up`입니다.
 */
export async function sendRequest(
  c: Context<AppEnv>,
  targetId: number,
  relation: Relation,
  source: "scoreboard" | "invite_link",
  invite?: InviteUse,
): Promise<Response> {
  // 스코어보드는 친구가 아닌 사람에게만 요청 버튼을 띄운다. 친구에게 요청이 왔다면 앱이 옛 관계를 들고 있는 것이라
  // 409로 알린다. 초대 링크는 친구가 다시 눌러도 되는 자리라 여기까지 오기 전에 200으로 답한다.
  if (relation === "friend") throw new ApiError(409, "already_friends");
  // 상대가 먼저 보냈으면 수락은 따로 누르게 한다. 요청을 겹쳐 두면 누가 누구를 기다리는지 흐려진다.
  if (relation === "requested_me") throw new ApiError(409, "already_requested_you");
  if (relation === "request_sent") return c.json({ status: "requested" });
  if (invite && invite.uses >= INVITE_REQUEST_LIMIT) throw new ApiError(410, "invite_used_up");

  const db = c.env.DB;
  const me = c.var.user.id;
  const counts = await db
    .prepare(`SELECT ${friendCount("?1")} AS mine, ${friendCount("?2")} AS theirs`)
    .bind(me, targetId)
    .first<{ mine: number; theirs: number }>();
  const full = friendsFull(counts!.mine, counts!.theirs);
  if (full) throw full;

  // 하루 한도를 링크보다 먼저 센다. 한도에 걸린 사람이 거듭 눌러도 남의 링크 횟수는 줄지 않는다.
  await countRequest(db, me, Date.now());
  if (invite) {
    const used = await db
      .prepare("UPDATE invites SET uses = uses + 1 WHERE code = ? AND uses < ?")
      .bind(invite.code, INVITE_REQUEST_LIMIT)
      .run();
    if (used.meta.changes === 0) throw new ApiError(410, "invite_used_up");
  }
  await db
    .prepare("INSERT INTO friend_requests (from_user, to_user, source, created_at) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")
    .bind(me, targetId, source, Date.now())
    .run();
  return c.json({ status: "requested" }, 201);
}
