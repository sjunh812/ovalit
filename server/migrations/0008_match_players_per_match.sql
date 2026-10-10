-- 참가자를 사람마다 한 줄에서 경기마다 한 줄로 바꾼다.
-- 사람마다 한 줄이면 새 경기 하나에 열 줄을 써서 첫 수집 한 번(50경기)이 D1 하루 쓰기 10만 행 중 500행을 쓴다.
-- 로그인도 같은 몫을 쓰니 다 쓰면 다음 0시(UTC)까지 아무도 로그인하지 못한다.
--
-- puuids는 참가자 PUUID의 JSON 배열이다. 이름, 요원, 성적은 여기 두지 않는다.
-- 사용자 계정이 아니라 Riot 경기 기록이라 users와 잇지 않고 연동을 해제해도 지우지 않는다.
--
-- recorded_at은 적은 시각(epoch ms)이다. 앱은 8주 안의 경기만 보니 크론이 적은 지 10주 지난 줄을 지운다.
-- 경기는 끝난 뒤에 적으니 앱이 볼 수 있는 경기는 늘 남아 있다. 이 색인이 없으면 지울 게 없어도 크론마다 표를 끝까지 읽는다.
CREATE TABLE match_players_by_match (
  match_id TEXT PRIMARY KEY,
  puuids TEXT NOT NULL,
  recorded_at INTEGER NOT NULL
) WITHOUT ROWID;

-- 옮기는 줄은 언제 적었는지 몰라 지금 적은 것으로 친다. 길어야 10주 더 남는다.
INSERT INTO match_players_by_match (match_id, puuids, recorded_at)
SELECT match_id, json_group_array(puuid), CAST(strftime('%s', 'now') AS INTEGER) * 1000
FROM match_players
GROUP BY match_id;

DROP TABLE match_players;
ALTER TABLE match_players_by_match RENAME TO match_players;

CREATE INDEX match_players_recorded_at ON match_players (recorded_at);
