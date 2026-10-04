import { type Deps, upstreamFetch } from "./app";
import type { Env } from "./env";
import { type Notice, type Push, pushFrom } from "./push";
import { PING_LENGTH_MS } from "./routes/pings";

/** 월요일 00:00 UTC, 한국 시각으로 월요일 오전 9시입니다. `wrangler.jsonc`의 `triggers`와 같은 문자열이어야 합니다. */
export const WEEKLY_CRON = "0 0 * * 1";
const MINUTE_MS = 60 * 1000;
// 미리 알림으로 고를 수 있는 가장 긴 시간이다. 그보다 뒤에 시작하는 ㅇㅂㅇ은 아직 찾지 않는다.
const MAX_REMIND_BEFORE_MS = 60 * MINUTE_MS;
// 크론이 5분마다 도니, 몫이 모자라 이번에 못 보낸 것은 다음 크론에 다시 잡힌다.
const REMIND_SCAN_LIMIT = 20;
// 하루 열 번 한도를 pings 줄로 세니 끝나고도 하루는 남겨 둔다
const KEEP_AFTER_EXPIRY_MS = 24 * 60 * 60 * 1000;
const CLEANUP_LIMIT = 100;

/** `createApp`과 같은 `deps`를 받습니다. 테스트는 가짜 `fetch`를 넣어 FCM을 부르지 않습니다. */
export function createScheduled(deps: Deps = {}): ExportedHandlerScheduledHandler<Env> {
  const upstream = upstreamFetch(deps);
  return async (controller, env) => {
    const push = pushFrom(env, upstream);
    if (controller.cron === WEEKLY_CRON) {
      await push?.toTopic("weekly_report", { type: "weekly_report" });
      return;
    }
    const now = Date.now();
    await cleanUp(env.DB, now);
    if (push) await remind(env.DB, push, now);
  };
}

async function cleanUp(db: D1Database, now: number): Promise<void> {
  // expires_at은 늘 starts_at보다 한 시간 뒤라 starts_at 색인으로 찾는다. 불려 간 자리는 CASCADE로 같이 지워진다.
  await db
    .prepare("DELETE FROM pings WHERE id IN (SELECT id FROM pings WHERE starts_at < ? LIMIT ?)")
    .bind(now - KEEP_AFTER_EXPIRY_MS - PING_LENGTH_MS, CLEANUP_LIMIT)
    .run();
}

/**
 * 곧 시작하는 ㅇㅂㅇ을 호스트와 가기로 한 친구에게 알립니다. 사람마다 고른 시간(`users.remind_before`) 안에 들어오면 한 번만
 * 보내고, 0을 고른 사람에게는 보내지 않습니다. 아무도 가기로 하지 않았으면 호스트에게도 알리지 않습니다.
 */
async function remind(db: D1Database, push: Push, now: number): Promise<void> {
  const { results: due } = await db
    .prepare(
      `SELECT p.id, p.host, p.starts_at, p.reminded, h.game_name AS host_name, h.remind_before AS host_before
       FROM pings p JOIN users h ON h.id = p.host
       WHERE p.starts_at > ?1 AND p.starts_at <= ?1 + ?2 AND p.canceled = 0
         AND EXISTS (SELECT 1 FROM ping_members m WHERE m.ping_id = p.id AND m.answer = 'yes')
         AND ((p.reminded = 0 AND p.starts_at - ?1 <= h.remind_before * ?3)
           OR EXISTS (SELECT 1 FROM ping_members m JOIN users u ON u.id = m.user_id
                      WHERE m.ping_id = p.id AND m.answer = 'yes' AND m.reminded = 0
                        AND p.starts_at - ?1 <= u.remind_before * ?3))
       ORDER BY p.starts_at LIMIT ?4`,
    )
    .bind(now, MAX_REMIND_BEFORE_MS, MINUTE_MS, REMIND_SCAN_LIMIT)
    .all<{ id: string; host: number; starts_at: number; reminded: number; host_name: string; host_before: number }>();
  if (due.length === 0) return;

  const { results: going } = await db
    .prepare(
      `SELECT m.ping_id, m.user_id, m.reminded, u.game_name, u.remind_before FROM ping_members m JOIN users u ON u.id = m.user_id
       WHERE m.answer = 'yes' AND m.ping_id IN (SELECT value FROM json_each(?)) ORDER BY m.position`,
    )
    .bind(JSON.stringify(due.map((ping) => ping.id)))
    .all<{ ping_id: string; user_id: number; reminded: number; game_name: string; remind_before: number }>();

  const inTime = (ping: { starts_at: number }, before: number) => before > 0 && ping.starts_at - now <= before * MINUTE_MS;
  const notices: Notice[] = due.flatMap((ping) => {
    const yes = going.filter((member) => member.ping_id === ping.id);
    const data = {
      type: "ping_remind",
      pingId: ping.id,
      startsAt: String(ping.starts_at),
      names: [ping.host_name, ...yes.map((member) => member.game_name)].join(","),
    } as const;
    const hostDue = ping.reminded === 0 && inTime(ping, ping.host_before) ? [ping.host] : [];
    const membersDue = yes.filter((member) => member.reminded === 0 && inTime(ping, member.remind_before));
    return [...hostDue, ...membersDue.map((member) => member.user_id)].map((userId) => ({ userId, data }));
  });
  const messages = await push.messagesFor(notices);

  // 몫에 들어가는 ㅇㅂㅇ까지만 고른다. 기기가 없어 보낼 메시지가 없는 사람도 같이 표시해 다시 찾지 않는다.
  const chosen = new Set<string>();
  let count = 0;
  for (const ping of due) {
    const size = messages.filter((message) => message.data.pingId === ping.id).length;
    if (!push.fits(count + size)) break;
    count += size;
    chosen.add(ping.id);
  }
  const hosts = notices.filter((notice) => chosen.has(notice.data.pingId!) && isHost(due, notice)).map((notice) => notice.data.pingId!);
  const members = notices
    .filter((notice) => chosen.has(notice.data.pingId!) && !isHost(due, notice))
    .map((notice) => [notice.data.pingId!, notice.userId]);
  if (hosts.length === 0 && members.length === 0) return;

  // 보내기 전에 표시한다. 크론 두 번이 겹쳐도 한 번만 가고, 보내다 실패하면 다시 보내지 않는다.
  const [claimedHosts, claimedMembers] = await db.batch<{ id?: string; ping_id?: string; user_id?: number }>([
    db
      .prepare("UPDATE pings SET reminded = 1 WHERE id IN (SELECT value FROM json_each(?)) AND reminded = 0 RETURNING id")
      .bind(JSON.stringify(hosts)),
    db
      .prepare(
        `UPDATE ping_members SET reminded = 1 WHERE reminded = 0 AND (ping_id, user_id) IN (
           SELECT json_extract(value, '$[0]'), json_extract(value, '$[1]') FROM json_each(?))
         RETURNING ping_id, user_id`,
      )
      .bind(JSON.stringify(members)),
  ]);
  const claimed = new Set([
    ...claimedHosts!.results.map((row) => `${row.id}:${due.find((ping) => ping.id === row.id)!.host}`),
    ...claimedMembers!.results.map((row) => `${row.ping_id}:${row.user_id}`),
  ]);
  await push.send(messages.filter((message) => claimed.has(`${message.data.pingId}:${message.userId}`)));
}

function isHost(due: { id: string; host: number }[], notice: Notice): boolean {
  return due.find((ping) => ping.id === notice.data.pingId)?.host === notice.userId;
}
