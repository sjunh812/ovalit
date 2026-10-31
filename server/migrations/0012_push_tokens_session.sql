-- 기기 토큰을 등록한 세션에 묶는다.
-- 로그아웃하거나 세션이 끝나면 그 기기의 토큰도 같이 지워진다.
-- 안 지우면 로그아웃한 기기에 친구 이름이 담긴 알림이 계속 간다.
-- 이 마이그레이션 전에 등록한 토큰은 세션이 없어 NULL로 남는다.
-- 그 토큰은 연동을 해제하거나 FCM이 없는 토큰이라고 답할 때 지워진다.
-- 앱은 켤 때마다 토큰을 다시 맡겨서 그때 세션이 붙는다.
ALTER TABLE push_tokens ADD COLUMN session_hash TEXT REFERENCES sessions (token_hash) ON DELETE CASCADE;

-- 세션을 지울 때마다 CASCADE가 이 열로 토큰을 찾는다.
-- 색인이 없으면 세션 하나를 지울 때마다 표를 끝까지 읽는다.
CREATE INDEX push_tokens_session_hash ON push_tokens (session_hash);
