-- 로그인을 시작할 때마다 10분 지난 state를 지운다. 이 색인이 없으면 지울 게 없어도 그때마다 표를 끝까지 읽는다.
-- 이 경로는 세션 없이 열려 있어서 누구나 D1 읽기 한도를 쓰게 할 수 있었다.
CREATE INDEX auth_states_created_at ON auth_states (created_at);
