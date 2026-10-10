-- 친구 요청을 하루(UTC)에 몇 번 새로 보냈는지 센다. 수락하거나 거절하면 friend_requests 줄이 지워져서 그 줄로는 셀 수 없다.
-- requests_day는 1970-01-01부터 며칠째인지이고, 날이 바뀐 뒤 처음 보내면 requests_today를 1부터 다시 센다.
ALTER TABLE users ADD COLUMN requests_day INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN requests_today INTEGER NOT NULL DEFAULT 0;

-- 초대 링크 하나로 만든 친구 요청 수다. 단톡방 밖으로 퍼진 링크가 끝없이 요청을 만들지 못하게 한다.
ALTER TABLE invites ADD COLUMN uses INTEGER NOT NULL DEFAULT 0;
