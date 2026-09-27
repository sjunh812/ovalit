-- 로그인과 초대 링크를 만들 때 만료된 세션과 초대를 누구 것이든 같이 지운다.
-- 이 색인이 없으면 지울 게 없어도 그때마다 표를 끝까지 읽어 D1 읽기 한도를 쓴다.
CREATE INDEX sessions_expires_at ON sessions (expires_at);
CREATE INDEX invites_expires_at ON invites (expires_at);
