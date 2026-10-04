-- 오발있 미리 알림을 시작 몇 분 전에 받을지다. 0이면 받지 않는다. 앱 설정에서 10분, 30분, 1시간 중에 고른다.
ALTER TABLE users ADD COLUMN remind_before INTEGER NOT NULL DEFAULT 10 CHECK (remind_before IN (0, 10, 30, 60));

-- 사람마다 고른 시간에 알리니 한 번 보냈는지를 받는 사람마다 적는다. 호스트 몫은 pings.reminded다.
ALTER TABLE ping_members ADD COLUMN reminded INTEGER NOT NULL DEFAULT 0 CHECK (reminded IN (0, 1));
