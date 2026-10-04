import { Hono } from "hono";
import { randomToken } from "../crypto";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { notify } from "../push";
import { requireSession } from "../session";
import * as validate from "../validate";

const MINUTE_MS = 60 * 1000;
const DAY_MS = 24 * 60 * MINUTE_MS;
/** 오발있은 시작하고 한 시간 동안 살아 있습니다. `expires_at`은 늘 `starts_at`에 이만큼 더한 값입니다. */
export const PING_LENGTH_MS = 60 * MINUTE_MS;
// 발로란트 파티는 다섯 명까지라 나를 빼면 넷이다.
const MAX_FRIENDS = 4;
// "지금"을 골라 보내는 사이에 시각이 조금 지나도 받는다.
const PAST_GRACE_MS = MINUTE_MS;
const MAX_LEAD_MS = DAY_MS;
// 부를 때마다 친구들에게 알림이 가서 하루 열 번까지만 띄운다. isolate 메모리는 비워질 수 있어 D1의 줄로 센다.
const DAILY_LIMIT = 10;
// randomToken(16)이 만드는 base64url 22자다.
const PING_ID = /^[A-Za-z0-9_-]{22}$/;
const ANSWERS = new Set(["yes", "no", "other_time"]);

type Answer = "pending" | "yes" | "other_time" | "no";

interface PingMember {
  puuid: string;
  gameName: string;
  tagLine: string;
  answer: Answer;
  proposedAt: number | null;
  updatedAt: number;
}

interface Ping {
  id: string;
  host: { puuid: string; gameName: string; tagLine: string };
  startsAt: number;
  createdAt: number;
  expiresAt: number;
  members: PingMember[];
}

interface PingRow {
  id: string;
  starts_at: number;
  created_at: number;
  expires_at: number;
  host_puuid: string;
  host_name: string;
  host_tag: string;
  user_id: number | null;
  position: number | null;
  friend: number;
  puuid: string | null;
  game_name: string | null;
  tag_line: string | null;
  answer: Answer | null;
  proposed_at: number | null;
  updated_at: number | null;
}

// ?1은 보는 사람이다. 부른 사람마다 나와 친구인지를 friendships 기본 키로 한 번씩 찾는다.
// 친구가 모두 연동을 해제해도 호스트에게는 오발있이 보여야 해서 부른 사람 쪽은 LEFT JOIN이다.
const SELECT_PINGS = `
  SELECT p.id, p.starts_at, p.created_at, p.expires_at,
    h.puuid AS host_puuid, h.game_name AS host_name, h.tag_line AS host_tag,
    m.user_id, m.position, u.puuid, u.game_name, u.tag_line, m.answer, m.proposed_at, m.updated_at,
    EXISTS (SELECT 1 FROM friendships f WHERE f.user_a = MIN(m.user_id, ?1) AND f.user_b = MAX(m.user_id, ?1)) AS friend
  FROM pings p
  JOIN users h ON h.id = p.host
  LEFT JOIN ping_members m ON m.ping_id = p.id
  LEFT JOIN users u ON u.id = m.user_id`;

export const pings = new Hono<AppEnv>();

pings.use(requireSession);

/** 내가 띄웠거나 불려 간 오발있 중 취소하지 않았고 끝나지 않은 것입니다. 최근에 띄운 것부터 옵니다. */
pings.get("/", async (c) => {
  const now = Date.now();
  // host와 user_id 색인을 하나씩 타게 나눠 찾는다. OR로 묶으면 표를 끝까지 읽는다.
  const { results } = await c.env.DB.prepare(
    `${SELECT_PINGS}
     WHERE p.id IN (
         SELECT id FROM pings WHERE host = ?1 AND expires_at > ?2
         UNION SELECT ping_id FROM ping_members WHERE user_id = ?1)
       AND p.canceled = 0 AND p.expires_at > ?2
     ORDER BY p.created_at DESC, p.id, m.position`,
  )
    .bind(c.var.user.id, now)
    .all<PingRow>();
  return c.json({ pings: toPings(results, c.var.user.id) });
});

pings.post("/", async (c) => {
  const body = await validate.jsonBody(c);
  const me = c.var.user;
  const puuids = friendList(body.friends, me.puuid);
  const now = Date.now();
  const startsAt = startTime(body.startsAt, now);
  const db = c.env.DB;

  const placeholders = puuids.map((_, i) => `?${i + 2}`).join(", ");
  const { results: friends } = await db
    .prepare(
      `SELECT u.id, u.puuid, u.game_name FROM users u
       JOIN friendships f ON f.user_a = MIN(u.id, ?1) AND f.user_b = MAX(u.id, ?1)
       WHERE u.puuid IN (${placeholders})`,
    )
    .bind(me.id, ...puuids)
    .all<{ id: number; puuid: string; game_name: string }>();
  if (friends.length !== puuids.length) throw new ApiError(403, "not_friend");
  const invited = puuids.map((puuid) => friends.find((friend) => friend.puuid === puuid)!);

  const id = randomToken(16);
  // 살아 있는 오발있과 하루 한도를 INSERT 조건으로 건다. 보내기를 두 번 눌러 요청이 겹쳐도 하나만 들어간다.
  // 부른 사람은 오발있이 들어갔을 때만 넣는다. json_each의 key가 배열 순번이라 고른 순서가 position이 된다.
  const [created] = await db.batch([
    db
      .prepare(
        `INSERT INTO pings (id, host, starts_at, created_at, expires_at)
         SELECT ?1, ?2, ?3, ?4, ?5
         WHERE NOT EXISTS (SELECT 1 FROM pings WHERE host = ?2 AND canceled = 0 AND expires_at > ?4)
           AND (SELECT COUNT(*) FROM pings WHERE host = ?2 AND created_at > ?6) < ?7`,
      )
      .bind(id, me.id, startsAt, now, startsAt + PING_LENGTH_MS, now - DAY_MS, DAILY_LIMIT),
    db
      .prepare(
        `INSERT INTO ping_members (ping_id, user_id, position, updated_at)
         SELECT ?1, value, key + 1, ?2 FROM json_each(?3) WHERE EXISTS (SELECT 1 FROM pings WHERE id = ?1)`,
      )
      .bind(id, now, JSON.stringify(invited.map((friend) => friend.id))),
  ]);
  if (created!.meta.changes === 0) throw await refusal(db, me.id, now);

  notify(
    c,
    invited.map((friend) => ({
      userId: friend.id,
      data: {
        type: "ping_new",
        pingId: id,
        startsAt: String(startsAt),
        hostName: me.gameName,
        others: invited
          .filter((other) => other.id !== friend.id)
          .map((other) => other.game_name)
          .join(","),
      },
    })),
  );
  return c.json({ ping: await pingById(db, id, me.id) }, 201);
});

pings.post("/:id/reply", async (c) => {
  const id = pingId(c.req.param("id"));
  const body = await validate.jsonBody(c);
  const answer = body.answer;
  if (typeof answer !== "string" || !ANSWERS.has(answer)) throw new ApiError(400, "invalid_body");
  const now = Date.now();
  // 가기로 했거나 안 간다고 했으면 제안한 시각은 버린다.
  const proposedAt = answer === "other_time" ? startTime(body.proposedAt, now) : null;
  const me = c.var.user;
  const db = c.env.DB;

  const current = await db
    .prepare(
      `SELECT p.host, p.starts_at, m.answer, m.proposed_at
       FROM pings p JOIN ping_members m ON m.ping_id = p.id AND m.user_id = ?2
       WHERE p.id = ?1 AND p.canceled = 0 AND p.expires_at > ?3`,
    )
    .bind(id, me.id, now)
    .first<{ host: number; starts_at: number; answer: Answer; proposed_at: number | null }>();
  if (!current) throw new ApiError(404, "not_found");

  // 같은 대답을 다시 누른 것이면 호스트에게 알림을 또 보내지 않는다.
  if (current.answer !== answer || current.proposed_at !== proposedAt) {
    // 못 간다고 한 사람은 자리를 비운 것이라, 다시 오겠다고 하면 그 사이 다른 친구로 넷이 찼는지 본다
    const updated = await db
      .prepare(
        `UPDATE ping_members SET answer = ?3, proposed_at = ?4, updated_at = ?5
         WHERE ping_id = ?1 AND user_id = ?2
           AND (?3 = 'no' OR answer != 'no'
             OR (SELECT COUNT(*) FROM ping_members WHERE ping_id = ?1 AND answer != 'no') < ?6)`,
      )
      .bind(id, me.id, answer, proposedAt, now, MAX_FRIENDS)
      .run();
    if (updated.meta.changes === 0) throw new ApiError(409, "ping_full");
    notify(c, [
      {
        userId: current.host,
        data: {
          type: "ping_reply",
          pingId: id,
          startsAt: String(current.starts_at),
          memberName: me.gameName,
          answer,
          proposedAt: proposedAt === null ? "" : String(proposedAt),
        },
      },
    ]);
  }
  return c.json({ ping: await pingById(db, id, me.id) });
});

/**
 * 호스트가 시작 시각을 옮깁니다. 그 시각을 제안한 친구는 가기로 한 것으로 바꾸고, 나머지는 가기로 했던 친구까지 모두
 * 다시 묻습니다. 시각이 바뀌면 전에 한 대답이 그대로 맞는지 알 수 없습니다.
 */
pings.post("/:id/time", async (c) => {
  const id = pingId(c.req.param("id"));
  const body = await validate.jsonBody(c);
  const now = Date.now();
  const startsAt = startTime(body.startsAt, now);
  const me = c.var.user;
  const db = c.env.DB;

  const current = await db
    .prepare("SELECT starts_at FROM pings WHERE id = ? AND host = ? AND canceled = 0 AND expires_at > ?")
    .bind(id, me.id, now)
    .first<{ starts_at: number }>();
  if (!current) throw new ApiError(404, "not_found");

  // 같은 시각으로 다시 오면 그대로 둔다. 버튼을 두 번 눌렀을 때 방금 가기로 바뀐 친구가 다시 대기로 돌아가지 않게 한다.
  if (current.starts_at !== startsAt) {
    const active = "SELECT 1 FROM pings WHERE id = ?1 AND host = ?2 AND canceled = 0 AND expires_at > ?3";
    // 못 간다고 한 친구에게도 다시 묻는다. 다만 그 뒤로 친구를 더 불러 다섯 명 이상이 됐으면 다시 물을 때 넷을 넘으니 그대로 둔다.
    await db.batch([
      db
        .prepare(
          `UPDATE ping_members
           SET answer = CASE
                 WHEN answer = 'no' AND (SELECT COUNT(*) FROM ping_members WHERE ping_id = ?1) > ?5 THEN 'no'
                 WHEN proposed_at = ?4 THEN 'yes'
                 ELSE 'pending' END,
               proposed_at = NULL, reminded = 0, updated_at = ?3
           WHERE ping_id = ?1 AND EXISTS (${active})`,
        )
        .bind(id, me.id, now, startsAt, MAX_FRIENDS),
      db
        .prepare(
          `UPDATE pings SET starts_at = ?4, expires_at = ?5, reminded = 0
           WHERE id = ?1 AND host = ?2 AND canceled = 0 AND expires_at > ?3`,
        )
        .bind(id, me.id, now, startsAt, startsAt + PING_LENGTH_MS),
    ]);
    const data = { type: "ping_time", pingId: id, startsAt: String(startsAt), hostName: me.gameName } as const;
    notify(c, (await memberIds(db, id)).map((userId) => ({ userId, data })));
  }
  return c.json({ ping: await pingById(db, id, me.id) });
});

/**
 * 호스트가 친구를 더 부릅니다. 누가 못 간다고 했거나 깜빡 빠뜨린 친구를 나중에 더할 때 씁니다. 못 간다고 한 사람은 자리를
 * 비운 것으로 쳐서, 그 사람을 뺀 인원이 넷을 넘지 않을 때까지 받습니다. 더한 친구만 새로 불렸다는 알림을 받습니다.
 */
pings.post("/:id/members", async (c) => {
  const id = pingId(c.req.param("id"));
  const body = await validate.jsonBody(c);
  const me = c.var.user;
  const puuids = friendList(body.friends, me.puuid);
  const now = Date.now();
  const db = c.env.DB;

  const current = await db
    .prepare(
      `SELECT p.starts_at,
         (SELECT COUNT(*) FROM ping_members WHERE ping_id = p.id AND answer != 'no') AS seats
       FROM pings p WHERE p.id = ?1 AND p.host = ?2 AND p.canceled = 0 AND p.expires_at > ?3`,
    )
    .bind(id, me.id, now)
    .first<{ starts_at: number; seats: number }>();
  if (!current) throw new ApiError(404, "not_found");
  if (current.seats + puuids.length > MAX_FRIENDS) throw new ApiError(409, "ping_full");

  const placeholders = puuids.map((_, i) => `?${i + 3}`).join(", ");
  const { results: friends } = await db
    .prepare(
      `SELECT u.id, u.puuid, u.game_name,
         EXISTS (SELECT 1 FROM ping_members m WHERE m.ping_id = ?2 AND m.user_id = u.id) AS invited
       FROM users u
       JOIN friendships f ON f.user_a = MIN(u.id, ?1) AND f.user_b = MAX(u.id, ?1)
       WHERE u.puuid IN (${placeholders})`,
    )
    .bind(me.id, id, ...puuids)
    .all<{ id: number; puuid: string; game_name: string; invited: number }>();
  if (friends.length !== puuids.length) throw new ApiError(403, "not_friend");
  if (friends.some((friend) => friend.invited)) throw new ApiError(409, "already_invited");
  const added = puuids.map((puuid) => friends.find((friend) => friend.puuid === puuid)!);

  // 자리와 살아 있는지를 INSERT 조건으로 다시 건다. 두 번 눌러 요청이 겹쳐도 넷을 넘지 않는다. 자리는 앞사람 뒤로 이어
  // 붙인다. 이미 쓴 자리 번호는 anon-N이라 다시 쓰지 않는다.
  const inserted = await db
    .prepare(
      `INSERT INTO ping_members (ping_id, user_id, position, updated_at)
       SELECT ?1, j.value, (SELECT COALESCE(MAX(position), 0) FROM ping_members WHERE ping_id = ?1) + j.key + 1, ?3
       FROM json_each(?4) j
       WHERE EXISTS (SELECT 1 FROM pings WHERE id = ?1 AND host = ?2 AND canceled = 0 AND expires_at > ?3)
         AND (SELECT COUNT(*) FROM ping_members WHERE ping_id = ?1 AND answer != 'no') + json_array_length(?4) <= ?5
         AND NOT EXISTS (SELECT 1 FROM ping_members WHERE ping_id = ?1 AND user_id = j.value)`,
    )
    .bind(id, me.id, now, JSON.stringify(added.map((friend) => friend.id)), MAX_FRIENDS)
    .run();
  if (inserted.meta.changes !== added.length) throw new ApiError(409, "ping_full");

  const { results: everyone } = await db
    .prepare("SELECT m.user_id, u.game_name FROM ping_members m JOIN users u ON u.id = m.user_id WHERE m.ping_id = ? ORDER BY m.position")
    .bind(id)
    .all<{ user_id: number; game_name: string }>();
  notify(
    c,
    added.map((friend) => ({
      userId: friend.id,
      data: {
        type: "ping_new",
        pingId: id,
        startsAt: String(current.starts_at),
        hostName: me.gameName,
        others: everyone
          .filter((other) => other.user_id !== friend.id)
          .map((other) => other.game_name)
          .join(","),
      },
    })),
  );
  return c.json({ ping: await pingById(db, id, me.id) });
});

pings.delete("/:id", async (c) => {
  const id = pingId(c.req.param("id"));
  const me = c.var.user;
  const db = c.env.DB;
  const result = await db
    .prepare("UPDATE pings SET canceled = 1 WHERE id = ? AND host = ? AND canceled = 0 AND expires_at > ?")
    .bind(id, me.id, Date.now())
    .run();
  if (result.meta.changes === 0) throw new ApiError(404, "not_found");
  const data = { type: "ping_cancel", pingId: id, hostName: me.gameName } as const;
  notify(c, (await memberIds(db, id)).map((userId) => ({ userId, data })));
  return c.body(null, 204);
});

function pingId(value: string): string {
  if (!PING_ID.test(value)) throw new ApiError(404, "not_found");
  return value;
}

function friendList(value: unknown, me: string): string[] {
  if (!Array.isArray(value) || value.length === 0 || value.length > MAX_FRIENDS) throw new ApiError(400, "invalid_body");
  const puuids = value.filter((item): item is string => typeof item === "string" && validate.PUUID.test(item));
  if (puuids.length !== value.length || new Set(puuids).size !== puuids.length || puuids.includes(me)) {
    throw new ApiError(400, "invalid_body");
  }
  return puuids;
}

function startTime(value: unknown, now: number): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value)) throw new ApiError(400, "invalid_body");
  if (value < now - PAST_GRACE_MS || value > now + MAX_LEAD_MS) throw new ApiError(400, "invalid_time");
  return value;
}

// INSERT 조건에 걸린 이유를 다시 읽어 코드로 바꾼다. 하루 한도면 열 번째로 최근에 띄운 것이 하루를 넘길 때 풀린다.
async function refusal(db: D1Database, hostId: number, now: number): Promise<ApiError> {
  const row = await db
    .prepare(
      `SELECT
         EXISTS (SELECT 1 FROM pings WHERE host = ?1 AND canceled = 0 AND expires_at > ?2) AS active,
         (SELECT created_at FROM pings WHERE host = ?1 AND created_at > ?3
          ORDER BY created_at DESC LIMIT 1 OFFSET ?4) AS oldest`,
    )
    .bind(hostId, now, now - DAY_MS, DAILY_LIMIT - 1)
    .first<{ active: number; oldest: number | null }>();
  if (!row || row.active || row.oldest === null) return new ApiError(409, "ping_active");
  const retryAfter = Math.max(1, Math.ceil((row.oldest + DAY_MS - now) / 1000));
  return new ApiError(429, "too_many_requests", { "Retry-After": String(retryAfter) });
}

async function memberIds(db: D1Database, id: string): Promise<number[]> {
  const { results } = await db
    .prepare("SELECT user_id FROM ping_members WHERE ping_id = ?")
    .bind(id)
    .all<{ user_id: number }>();
  return results.map((row) => row.user_id);
}

async function pingById(db: D1Database, id: string, viewerId: number): Promise<Ping> {
  const { results } = await db
    .prepare(`${SELECT_PINGS} WHERE p.id = ?2 ORDER BY m.position`)
    .bind(viewerId, id)
    .all<PingRow>();
  const [ping] = toPings(results, viewerId);
  if (!ping) throw new ApiError(404, "not_found");
  return ping;
}

/**
 * 같이 불린 사람 중 보는 사람과 서로 수락한 친구가 아닌 사람은 PUUID를 `anon-N`으로 바꾸고 태그를 비웁니다. N은 그 사람이
 * 불린 순서라 같은 오발있 안에서는 늘 같고, 다른 오발있의 같은 이름과는 이어지지 않습니다. 이름과 대답은 그대로 둡니다.
 * 호스트와 나 자신은 가리지 않습니다.
 */
function toPings(rows: PingRow[], viewerId: number): Ping[] {
  const byId = new Map<string, Ping>();
  for (const row of rows) {
    let ping = byId.get(row.id);
    if (!ping) {
      ping = {
        id: row.id,
        host: { puuid: row.host_puuid, gameName: row.host_name, tagLine: row.host_tag },
        startsAt: row.starts_at,
        createdAt: row.created_at,
        expiresAt: row.expires_at,
        members: [],
      };
      byId.set(row.id, ping);
    }
    if (row.puuid === null) continue;
    const known = row.user_id === viewerId || row.friend === 1;
    // 친구가 아닌 사람은 이름만 준다(CLAUDE.md 백엔드). 태그까지 주면 이름#태그로 그 사람을 찾을 수 있다.
    ping.members.push({
      puuid: known ? row.puuid : `anon-${row.position}`,
      gameName: row.game_name!,
      tagLine: known ? row.tag_line! : "",
      answer: row.answer!,
      proposedAt: row.proposed_at,
      updatedAt: row.updated_at!,
    });
  }
  return [...byId.values()];
}
