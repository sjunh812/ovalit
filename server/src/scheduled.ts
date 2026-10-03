import { type Deps, upstreamFetch } from "./app";
import type { Env } from "./env";
import { type Notice, type Push, pushFrom } from "./push";
import { PING_LENGTH_MS } from "./routes/pings";

/** 월요일 00:00 UTC, 한국 시각으로 월요일 오전 9시입니다. `wrangler.jsonc`의 `triggers`와 같은 문자열이어야 합니다. */
export const WEEKLY_CRON = "0 0 * * 1";
const REMIND_BEFORE_MS = 10 * 60 * 1000;
// 크론이 5분마다 돌고 10분 앞부터 찾으니, 몫이 모자라 이번에 못 보낸 것은 시작 전에 한 번 더 잡힌다.
const REMIND_SCAN_LIMIT = 20;
// 하루 열 번 한도를 띄운 줄로 세니 끝나고 하루는 둔다. 그보다 오래된 줄은 한도 계산에 들어가지 않는다.
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

/** 10분 안에 시작하는 오발있을 호스트와 가기로 한 친구에게 한 번만 알립니다. 아무도 가기로 하지 않았으면 알리지 않습니다. */
async function remind(db: D1Database, push: Push, now: number): Promise<void> {
  const { results: due } = await db
    .prepare(
      `SELECT p.id, p.host, p.starts_at, h.game_name AS host_name FROM pings p JOIN users h ON h.id = p.host
       WHERE p.starts_at > ?1 AND p.starts_at <= ?2 AND p.reminded = 0 AND p.canceled = 0
       ORDER BY p.starts_at LIMIT ?3`,
    )
    .bind(now, now + REMIND_BEFORE_MS, REMIND_SCAN_LIMIT)
    .all<{ id: string; host: number; starts_at: number; host_name: string }>();
  if (due.length === 0) return;

  const { results: going } = await db
    .prepare(
      `SELECT m.ping_id, m.user_id, u.game_name FROM ping_members m JOIN users u ON u.id = m.user_id
       WHERE m.answer = 'yes' AND m.ping_id IN (SELECT value FROM json_each(?)) ORDER BY m.position`,
    )
    .bind(JSON.stringify(due.map((ping) => ping.id)))
    .all<{ ping_id: string; user_id: number; game_name: string }>();

  const notices: Notice[] = due.flatMap((ping) => {
    const yes = going.filter((member) => member.ping_id === ping.id);
    if (yes.length === 0) return [];
    const data = {
      type: "ping_remind",
      pingId: ping.id,
      startsAt: String(ping.starts_at),
      names: [ping.host_name, ...yes.map((member) => member.game_name)].join(","),
    } as const;
    return [ping.host, ...yes.map((member) => member.user_id)].map((userId) => ({ userId, data }));
  });
  const messages = await push.messagesFor(notices);

  // 몫에 들어가는 데까지만 고른다. 알릴 사람이 없는 오발있도 다시 찾지 않게 같이 표시한다.
  const chosen: string[] = [];
  let count = 0;
  for (const ping of due) {
    const size = messages.filter((message) => message.data.pingId === ping.id).length;
    if (!push.fits(count + size)) break;
    count += size;
    chosen.push(ping.id);
  }
  if (chosen.length === 0) return;

  // 보내기 전에 표시한다. 크론 두 번이 겹쳐도 한 번만 가고, 보내다 실패하면 다시 보내지 않는다.
  const { results: claimed } = await db
    .prepare("UPDATE pings SET reminded = 1 WHERE id IN (SELECT value FROM json_each(?)) AND reminded = 0 RETURNING id")
    .bind(JSON.stringify(chosen))
    .all<{ id: string }>();
  const ids = new Set(claimed.map((row) => row.id));
  await push.send(messages.filter((message) => ids.has(message.data.pingId!)));
}
