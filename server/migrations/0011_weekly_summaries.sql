-- 친구 비교와 라이벌 대결에 쓰는 주간 요약이다(docs/backend.md 친구 비교 요약).
-- 앱이 자기 경기로 주(월요일 0시, 기기 시간대)마다 고정 칸 합계를 세어 올리고 친구는 요약만 받는다.
-- 비율이 아니라 합계라 기간을 넓힐 때 더한 뒤 나눈다.
-- 한 주가 액트 경계에 걸치거나 경쟁전과 일반전을 같이 뛰면 줄이 나뉜다.
-- 올릴 때 그 주의 줄을 통째로 바꾼다.
-- 연동을 해제하면 CASCADE로 지워진다.
-- 친구를 끊어도 지우지 않고 내주지만 않는다.
CREATE TABLE weekly_summaries (
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  -- 그 주 월요일(YYYY-MM-DD)
  week TEXT NOT NULL,
  act TEXT NOT NULL,
  queue TEXT NOT NULL CHECK (queue IN ('competitive', 'unrated')),
  matches INTEGER NOT NULL,
  rounds INTEGER NOT NULL,
  kills INTEGER NOT NULL,
  deaths INTEGER NOT NULL,
  assists INTEGER NOT NULL,
  score INTEGER NOT NULL,
  damage INTEGER NOT NULL,
  head INTEGER NOT NULL,
  body INTEGER NOT NULL,
  leg INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, week, act, queue)
) WITHOUT ROWID;

-- 크론이 오래된 주를 지울 때 쓴다
CREATE INDEX weekly_summaries_week ON weekly_summaries (week);
