import type { Context } from "hono";
import type { AppEnv, User } from "./env";
import { logFailure } from "./log";
import { Quota } from "./memory";
import { riotFor } from "./riot";

// Riot ID를 다시 읽는 간격이다.
// 세션이 90일이라 로그인할 때만 읽으면 닉네임을 바꾼 사람의 옛 이름이 친구 목록, 요청, ㅇㅂㅇ과 알림에 그만큼 남는다.
// 옛 Riot ID를 보여주는 건 정책 위반이다(CLAUDE.md 아직 확인하지 못한 것).
const RIOT_ID_REFRESH_MS = 24 * 60 * 60 * 1000;
// 한 isolate에서 한 사람에게 하루 한 번만 account-v1을 부른다.
// 이름이 그대로면 D1에 쓰지 않으니 언제 확인했는지는 메모리에만 둔다.
const riotIdChecks = new Quota(1, RIOT_ID_REFRESH_MS);

/** 테스트끼리 Riot ID를 확인한 기록이 이어지지 않게 비울 때 씁니다. */
export function clearRiotIdChecks(): void {
  riotIdChecks.clear();
}

/** Riot ID는 바꿀 수 있어서 로그인할 때마다 새 값으로 덮어씁니다. 옛 이름은 남기지 않습니다(닉변 이력 없음). */
export async function upsertUser(db: D1Database, puuid: string, gameName: string, tagLine: string): Promise<number> {
  const now = Date.now();
  const row = await db
    .prepare(
      `INSERT INTO users (puuid, game_name, tag_line, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, ?4)
       ON CONFLICT (puuid) DO UPDATE SET game_name = excluded.game_name, tag_line = excluded.tag_line, updated_at = excluded.updated_at
       RETURNING id`,
    )
    .bind(puuid, gameName, tagLine, now)
    .first<{ id: number }>();
  return row!.id;
}

/**
 * 세션으로 들어온 사람의 Riot ID를 하루에 한 번 account-v1로 다시 읽어 바뀌었을 때만 덮어씁니다.
 * 응답을 기다리게 하지 않고 뒤에서 읽으며, 실패해도 요청은 그대로 성공합니다.
 * `updatedAt`(users.updated_at)이 하루가 안 됐으면 방금 로그인해 새 이름을 받은 것이라 부르지 않습니다.
 * 이번 요청의 `c.var.user`는 옛 이름 그대로이고 다음 요청부터 새 이름입니다.
 */
export function refreshRiotId(c: Context<AppEnv>, updatedAt: number): void {
  if (Date.now() - updatedAt < RIOT_ID_REFRESH_MS || !c.env.RIOT_API_KEY) return;
  if (riotIdChecks.take(String(c.var.user.id)) !== undefined) return;
  // 에러에 Riot 응답이나 PUUID가 섞일 수 있어 이름만 남긴다
  c.executionCtx.waitUntil(rereadRiotId(c, c.var.user).catch((err) => logFailure("riot_id", err)));
}

async function rereadRiotId(c: Context<AppEnv>, user: User): Promise<void> {
  const account = JSON.parse(await riotFor(c).account()) as Record<string, unknown> | null;
  const { puuid, gameName, tagLine } = account ?? {};
  if (puuid !== user.puuid || !isName(gameName) || !isName(tagLine)) return;
  if (gameName === user.gameName && tagLine === user.tagLine) return;
  await c.env.DB.prepare("UPDATE users SET game_name = ?2, tag_line = ?3, updated_at = ?4 WHERE id = ?1")
    .bind(user.id, gameName, tagLine, Date.now())
    .run();
}

// 빈 이름으로 덮어쓰지 않는다.
// Riot이 모양이 틀린 응답을 주면 지금 이름을 그대로 둔다.
function isName(value: unknown): value is string {
  return typeof value === "string" && value.length > 0;
}
