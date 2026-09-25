-- 끝난 경기에 누가 뛰었는지만 둔다. 친구 요청과 스코어보드의 앱 사용자 확인은 참가자만 알면 되는데,
-- workers.dev에서는 Cache API가 담지 않아서 이 표가 없으면 그때마다 Riot에 경기를 다시 묻는다.
-- 경기 결과는 바뀌지 않으니 한 번 적으면 고치지 않는다.
--
-- 사용자 계정이 아니라 Riot 경기 기록이라 users와 잇지 않는다. 연동을 해제해도 지우지 않는다.
-- PUUID 말고는 아무것도 적지 않는다. 이름, 요원, 성적은 여기 두지 않는다.
--
-- puuid 쪽 색인은 두지 않는다. 지금은 경기로만 찾고, 색인을 두면 새 경기마다 쓰는 행이 두 배가 된다.
CREATE TABLE match_players (
  match_id TEXT NOT NULL,
  puuid TEXT NOT NULL,
  PRIMARY KEY (match_id, puuid)
) WITHOUT ROWID;
