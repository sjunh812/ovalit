# 영어(en) 현지화

화면 문구는 모듈마다 `composeResources/values/strings.xml`(한국어, 기본) 옆의 `values-en/strings.xml`이다. 안드로이드에만 있는
문구(알림, 광고, 공유 창 제목)는 `composeApp/src/main/res/values-en/strings.xml`이다. 한국어 문구를 바꾸면 같은 커밋에서 일본어와
영어도 고친다.

일본 다음 출시 국가인 필리핀, 말레이시아, 싱가포르, 호주를 위한 화면이다(사용자 결정, 2026-11-01, `docs/RELEASE.md` 배포 국가).
네 나라 모두 일본과 같은 AP 샤드라 서버 일은 일본을 낼 때 붙이는 AP 샤드로 끝나고 번역은 영어 하나면 된다. 지금은 화면 언어만
있고 데이터는 한국 샤드다. AP 샤드는 Riot 승인 뒤 제품 설명을 고칠 때 붙인다.

## 언제 영어가 뜨나

- 기기 언어가 영어면 영어다. 한국어, 일본어, 영어가 아닌 언어는 기본인 한국어로 뜬다.
- 안드로이드 13부터는 설정 > 앱 > 오발있 > 언어에서 앱만 영어로 고를 수 있다(`generateLocaleConfig`).
- 개발 중에는 `adb shell cmd locale set-app-locales com.ovalit --locales en-US`로 바꾸고 `--locales ""`로 되돌린다.
- 요원·맵·무기·티어 이름은 서버가 `locale=en-US`로 내려준다(`AppLanguage`).

화면에 뜨는 사람 이름(Riot ID, 친구 이름)은 데이터라 번역하지 않는다.

## 지키는 것

- 말투는 친구에게 말하듯 쉬운 영어다. 2인칭(you)으로 쓰고 줄임말(you're, can't)을 써도 된다. 느낌표와 과장은 쓰지 않는다.
- 제목, 버튼, 칩은 첫 글자만 대문자로 쓴다(Sentence case, "Start with Riot account"). 고유 명사(VALORANT, Riot, 요원·맵·무기 이름,
  큐 이름)는 원래 표기대로 둔다.
- 마침표는 한국어 규칙과 같다. 제목, 버튼, 칩, 한 줄 설명, 숫자 밑 안내, 토스트, 한 줄 상태에는 찍지 않고 카드 본문 문장, 시트와
  설정 설명, 표 밑 안내, 확인 창 본문에는 찍는다.
- "recommend", "you should play", "try using"처럼 결정을 대신하는 말은 쓰지 않는다(CLAUDE.md 지켜야 할 선). 내 기록이 어떻게
  움직였는지만 적는다.
- 지표를 "rating"이나 "score"로 묶어 부르지 않는다. KDA는 "KDA"다. 전투 점수만 Riot 이름대로 "Combat score"다.
- 자리 표시(`%1$s`, `%2$d`)와 XML 이스케이프는 그대로 둔다. 어순이 바뀌면 번호로 자리를 옮긴다.
- 이름 뒤 소유격은 "'s"를 붙인다("Minseok's invite"). 한국어처럼 조사가 받침에 따라 바뀌지 않는다.
- Riot 고지(`riot_disclaimer`)는 기본 문구가 이미 영문 원문이라 `values-en`에 넣지 않는다.
- `text_locale`은 `en-US`, `josa_enabled`는 `false`, `list_separator`는 `, `다.
- 날짜 순서는 나라 관습을 따른다(사용자 결정). `values-en`은 호주, 싱가포르, 말레이시아처럼 일/월("22/9")이고 필리핀과 미국은
  `values-en-rPH`, `values-en-rUS`에서 날짜 문구만 월/일("9/22")로 덮는다. 기기 지역이 영어권의 다른 나라면 `values-en`을 따른다.
- 홈 "What changed" 칸 이름은 짧은 이름(`metric_*_cell`)을 쓴다(사용자 결정). "Opening duels", "Opening win %",
  "Assists/round", "Force win %", "Multi-kill %"이고 칸을 누르면 뜨는 시트 제목은 전체 이름이다. 줄이지 않으면 한 줄에 넣으려고 그
  카드의 칸 이름이 모두 고정 칸보다 작아진다. 칸 이름 자리는 412dp 폰에서 약 100dp, 360dp 폰에서 약 84dp라 "Combat score"(13sp에서
  78dp) 길이 안에 둔다.
- 좁은 자리(칩, 표 열, 세 칸 격자)는 한국어보다 글자가 길어지기 쉬워서 줄임말을 먼저 쓴다(Comp, HS%, Avg).

## 단수와 복수

영어는 숫자에 따라 낱말이 바뀐다("1 match", "2 matches"). 지금 문구는 모두 `stringResource` 하나로 불러서 단수와 복수를 가를 수
없다. 그래서 먼저 단수·복수가 갈리지 않는 표현을 쓴다.

- 승패는 "5W 2L", 걸린 시간은 "12 min", "3 h", 지난 시간은 "51m ago", "3h ago"처럼 줄임말로 쓴다. 경기 줄처럼 좁은 자리에 들어가야 한다.
- 이름표가 앞에 오는 꼴("Matches 50")로 쓸 수 있으면 그렇게 쓴다.
- 그래도 갈려야 하는 문구는 아래 "복수형이 필요한 문구"에 적고 복수형으로 쓴다(1일 때 어색함). 이 문구들은 출시 전에
  `<plurals>`와 `pluralStringResource`로 바꾼다. 한국어와 일본어는 `other` 하나만 둔다.

## 용어

| 한국어 | 영어 | 메모 |
| --- | --- | --- |
| 오발있(앱 이름) | Ovalit | 로고 ㅇㅂㅇ은 기호로 둔다 |
| 오늘 발로란트 할 사람 있어? | Anyone up for VALORANT today? | 이름 풀이 슬로건. 제품 이름에는 넣지 않는다 |
| 오발있?(카드 제목·기능) | Playing today? | 일본어 「今日やる？」와 같은 뜻. 버튼과 알림 채널은 "Party up" |
| 민석의 초대 / 내 초대 | Minseok's invite / My invite | |
| 갈게요 / 다른 시간 / 못 가요 | I'm in / Another time / Can't make it | 상태는 Going / Not going / No reply |
| 요원 | Agent | |
| 타격대·척후대·전략가·감시자 | Duelist · Initiator · Controller · Sentinel | Riot 공식 이름 |
| 역할 | Role | |
| 경기, 판 | Match | 5 matches, per match |
| 라운드 | Round | |
| 이코·포스바이·풀바이·세이브 | Eco · Force buy · Full buy · Save | 피스톨 라운드는 Pistol round |
| 첫 킬·첫 데스 | First kill · First death | |
| 첫 교전(관여율·승률) | Opening duel(Opening duel rate · Opening duel win %) | |
| 첫 킬 승률 | First kill win % | 내 첫 킬이 난 라운드를 이긴 비율 |
| 전투 점수(ACS) | Combat score, 시트는 Average combat score (ACS) | 경기 줄의 "ACS 243"은 그대로 |
| 피해량(ADR) | Damage, 시트는 Average damage per round (ADR) | |
| 관여율(KAST) | KAST | 영어권 사용자는 약어를 그대로 쓴다 |
| 헤드샷 | Headshot %(칸), headshot rate(문장) | 표 열처럼 좁은 자리는 HS% |
| K/D, KDA | K/D, KDA | "rating"으로 부르지 않는다 |
| 라운드 생존율 / 라운드당 어시스트 | Survival rate / Assists per round | |
| 멀티킬 라운드 비율 / 트레이드 받은 데스 비율 | Multi-kill rounds / Traded deaths | |
| 이코·포스바이·풀바이 승률 | Eco win % · Force buy win % · Full buy win % | |
| 클러치 / 에이스 | Clutch / Ace | 1v3 clutch |
| MVP / 팀 MVP / 3등 | MVP / Team MVP / 3rd | |
| 경쟁 + 일반 / 경쟁 / 일반 / 기타 (칩) | Comp + Unrated / Comp / Unrated / Other | 시트와 설명의 한 판 모드는 Competitive / Unrated |
| 스파이크 돌격 / 신속 플레이 | Spike Rush / Swiftplay | |
| 프리미어 / 복제 / 에스컬레이션 | Premier / Replication / Escalation | |
| 데스매치 / 팀 데스매치 / 눈싸움 | Deathmatch / Team Deathmatch / Snowball Fight | |
| 공격 / 수비 | Attack / Defense | |
| 티어 | Rank | |
| 액트 / 에피소드 | Act / Episode | 이번 액트는 "this act" |
| 라이벌 / 친구 | Rival / Friend | 친구 요청은 friend request, 끊기는 Remove friend |
| 주간 리포트 | Weekly report | |
| 이번 주 / 지난주 / 지난 2주 / 최근 2주 | This week / Last week / Last 2 weeks / Past 2 weeks | |
| 달라진 점 | What changed | |
| 이번 액트 돌아보기 | This act in review | |
| 크레드 / 스킬 | Creds / Ability | |
| 장비 가치(한 사람당 평균) | Loadout value | |
| 무기 계열 | Rifle · Sidearm · SMG · Sniper · Shotgun · Heavy · Melee | |
| 퍼센트 변화량 | % | +12%. 퍼센트포인트지만 한국어처럼 pp를 붙이지 않는다(사용자 결정) |
| 새벽 00:30 | 00:30 (overnight) | 시각은 코드가 `HH:mm`으로 만든다 |
| 지금(ㅇㅂㅇ 시각) | Now | |
| 수락 / 거절 | Accept / Decline | |
| 나(목록 안의 나) | You | 초대 화면 참석자 줄, 스코어보드 |
| 시작 전 알림 | Reminder before it starts | 설정의 Playing today? 밑 줄 |
| 평소 / 지난 4주 평균 | Avg / 4-week average | 칸의 "평소 138"은 "Avg 138" |
| 연달아 뛴 판 | Back-to-back matches | |
| 우리 팀 / 상대 팀 | Your team / Enemy team | 좁은 자리는 Ally / Enemy |
| 준 피해 / 받은 피해 | Damage dealt / Damage taken | |
| 불러오다 | Load | "Loading matches", "Loaded 3 new matches" |

## 복수형이 필요한 문구

지금은 복수형으로 두어 1일 때 "1 matches"처럼 뜬다. 출시 전에 `<plurals>`로 바꾼다(`specs/001-ovalit-baseline/tasks.md` T095).

| 모듈 | 키 | 1이 나오는가 |
| --- | --- | --- |
| core/ui | `refresh_received` | 자주 나온다. 당겨서 한 판 받는 경우가 가장 흔해 먼저 바꾼다 |
| core/ui | `act_matches`, `profile_competitive_matches`, `new_matches_description` | 나온다 |
| onboarding | `import_done`, `import_progress_description` | 나온다 |
| profile | `records_queue_matches` | 나온다 |
| composeApp | `notification_new_matches_body` | 스무 판 이상일 때만 보내 지금은 늘 복수다 |

늘 2 이상이라 복수형이 맞는 문구는 위 표에 넣지 않았다(`highlight_aces`, `highlight_clutches`, 등수의 "of 10", `friend_caption`,
`ping_compose_hint`, 짚을 점의 판 수, 주 수가 고정된 시트 문장).

## 아직 정하지 않은 것

- 철자는 `en-US`에 맞춰 미국식이다("canceled", "Defense"). 호주, 싱가포르, 말레이시아는 보통 영국식을 쓴다.
- 좁은 열의 줄임말("%1$dK", "K/D/A", "Dealt/Taken", "Opening win %")은 실제 기기에서 잘림을 본 뒤 다시 정한다.
- 내 프로필 최근 경기 줄은 경기 탭보다 좁아 "50m ago", "Yesterday"가 말줄임표로 잘린다.

## 아직 한국어인 것

- 서버의 초대 페이지와 로그인 완료 페이지(`server/src/routes/public.ts`, `auth.ts`)
- 스토어 등록 정보. 개인정보처리방침과 이용약관은 이미 한국어와 영어를 한 페이지에 둔다(`site/privacy/`, `site/terms/`).
