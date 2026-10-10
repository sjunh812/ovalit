-- RSO state는 이제 STATE_SECRET으로 서명한 값이라 D1에 두지 않는다.
-- 로그인 시작은 세션 없이 열려 있어서, 시작마다 이 표에 쓰면 누구나 되풀이해 불러 하루 쓰기 한도를 다 쓰게 할 수 있었다.
-- 색인 auth_states_created_at도 같이 지워진다.
DROP TABLE auth_states;
