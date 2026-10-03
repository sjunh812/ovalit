-- 오발있("오늘 발로란트 할 사람 있어?")이다. 호스트가 서로 수락한 친구를 넷까지 불러 시작 시각을 묻는다.
-- expires_at은 늘 starts_at보다 한 시간 뒤다. 그래서 크론은 starts_at 색인 하나로 곧 시작할 것과 지울 것을 함께 찾는다.
-- 취소해도 줄을 지우지 않는다. 하루 열 번 한도를 이 줄로 센다. 끝나고 하루가 지난 줄은 크론이 지운다.
CREATE TABLE pings (
  id TEXT PRIMARY KEY,
  host INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  starts_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  canceled INTEGER NOT NULL DEFAULT 0 CHECK (canceled IN (0, 1)),
  reminded INTEGER NOT NULL DEFAULT 0 CHECK (reminded IN (0, 1))
);

CREATE INDEX pings_host_expires_at ON pings (host, expires_at);
CREATE INDEX pings_starts_at ON pings (starts_at);

-- 제안한 시각은 "다른 시간"에만 있다. 시간을 바꾸면 모두에게 다시 물으니 그때 같이 비운다.
-- position은 호스트가 고른 순서로 1부터 센다. 나와 친구가 아닌 사람의 PUUID를 anon-N으로 가릴 때 N으로 쓰니,
-- 앞사람이 빠져도 고치지 않는다. 그래야 같은 사람이 그 오발있 안에서 늘 같은 이름으로 보인다.
CREATE TABLE ping_members (
  ping_id TEXT NOT NULL REFERENCES pings (id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  answer TEXT NOT NULL DEFAULT 'pending' CHECK (answer IN ('pending', 'yes', 'other_time', 'no')),
  proposed_at INTEGER,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (ping_id, user_id),
  CHECK ((answer = 'other_time') = (proposed_at IS NOT NULL))
);

CREATE INDEX ping_members_user_id ON ping_members (user_id);

-- FCM 기기 토큰이다. 한 기기에서 다른 계정으로 연동하면 토큰이 새 계정으로 옮겨 간다.
CREATE TABLE push_tokens (
  token TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  updated_at INTEGER NOT NULL
);

CREATE INDEX push_tokens_user_id ON push_tokens (user_id);
