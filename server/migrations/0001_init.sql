-- 시각은 모두 epoch ms다. 토큰과 로그인 코드는 SHA-256 해시만 둔다. DB가 새도 세션을 훔쳐 쓸 수 없게 한다.
-- 연동 해제는 users 한 줄을 지우는 것으로 끝난다. 나머지는 ON DELETE CASCADE가 따라 지운다.

CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  puuid TEXT NOT NULL UNIQUE,
  game_name TEXT NOT NULL,
  tag_line TEXT NOT NULL,
  stats_public INTEGER NOT NULL DEFAULT 1 CHECK (stats_public IN (0, 1)),
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX sessions_user_id ON sessions (user_id);

-- RSO를 띄울 때 받은 앱의 challenge를 콜백까지 들고 간다. 아직 누구인지 모르는 단계라 사용자와 잇지 않는다.
CREATE TABLE auth_states (
  state TEXT PRIMARY KEY,
  challenge TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE login_codes (
  code_hash TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  challenge TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX login_codes_user_id ON login_codes (user_id);

CREATE TABLE friend_requests (
  from_user INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  to_user INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  source TEXT NOT NULL CHECK (source IN ('scoreboard', 'invite_link')),
  created_at INTEGER NOT NULL,
  PRIMARY KEY (from_user, to_user),
  CHECK (from_user <> to_user)
);

CREATE INDEX friend_requests_to_user ON friend_requests (to_user);

-- 한 쌍을 한 줄로만 두려고 작은 id를 user_a에 넣는다.
CREATE TABLE friendships (
  user_a INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  user_b INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (user_a, user_b),
  CHECK (user_a < user_b)
);

CREATE INDEX friendships_user_b ON friendships (user_b);

CREATE TABLE invites (
  code TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX invites_user_id ON invites (user_id);
