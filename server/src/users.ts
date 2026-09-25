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
