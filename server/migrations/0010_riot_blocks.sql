-- Riot 429로 막힌 범위와 풀리는 시각(epoch ms)이다.
-- isolate 메모리에만 두면 다른 isolate는 429를 받은 뒤에도 계속 Riot을 불러 앱 전체의 몫을 더 깎는다.
-- 429를 받을 때만 한 줄 쓰고 isolate마다 5초에 한 번까지만 읽는다.
-- scope는 호스트(application 한도)나 호스트와 경로 틀(method 한도)이라 열 줄 안팎이다. 풀린 줄은 크론이 지운다.
CREATE TABLE riot_blocks (
  scope TEXT PRIMARY KEY,
  blocked_until INTEGER NOT NULL
) WITHOUT ROWID;
