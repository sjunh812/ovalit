# Data Model: 오발있 기준 스펙

앱의 도메인 모델(`shared/core/model`)과 서버 저장(`server/migrations`)을 옮겨 적는다. 필드 이름은 코드와 같다. 계산 규칙의 원본은
`CLAUDE.md`의 데이터와 지표 규칙이고 `core/model` 테스트가 지킨다.

## 식별자

모두 문자열을 감싼 값 클래스다(`Ids.kt`). `MatchId`, `PlayerId`(PUUID), `AgentId`, `ActId`, `WeaponId`, `MapId`, `PlayerCardId`,
`PingId`. 요원·무기·맵·액트·카드 UUID는 소문자로 옮긴다. 서버가 가린 사람은 `anon-N`이고 Riot ID가 빈 문자열이다.

## 앱 도메인

### Account (계정)

| 필드 | 뜻 |
| --- | --- |
| `id` | 내 PUUID |
| `riotId` | Riot ID(이름#태그) |
| `linkedOn` | 연동한 날 |

- 연동 전에는 어떤 경기도 요청하지 않는다(FR-002).

### AccountSettings (서버가 따르는 설정)

| 필드 | 뜻 |
| --- | --- |
| `statsPublic` | 전적 공개. 끄면 친구가 내 경기와 통계를 못 본다 |
| `pingReminder` | 시작 전 알림. 10분·30분·1시간·받지 않기(기본 10분) |

- 서버 값을 따른다(`PATCH /me`). 기기에만 두면 공개를 꺼도 서버가 몰라 친구에게 그대로 보인다(FR-029).

### UserPreferences (기기 설정, DataStore)

`theme`(시스템·다크·라이트), `defaultQueue`(`QueueFilter`), `focus`(관심사), 알림 스위치(`notifyAnalysisDone`,
`notifyWeeklyReport`, `notifyPing`), 한 번만 띄우는 안내(`seenProfileHint`, `seenNotificationPrimer`,
`askedNotificationPermission`), `adFreeUntil`.

- 친구 요청 알림 스위치(기본 켬)는 FR-027로 정했고 아직 필드가 없다. 구현할 때 같은 자리에 더한다.

- `Focus`: 에임 올리기(첫 교전 승률, 멀티킬 라운드 비율), 라운드 운영 다듬기(포스바이·이코·풀바이 승률), 기복 줄이기(관여율,
  생존율), 특별히 없어요.
- `QueueFilter`: 경쟁 + 일반, 경쟁, 일반, 기타. 홈과 경기 탭에서 고른 칩은 저장하지 않는다.

### Match (경기)

| 필드 | 뜻 |
| --- | --- |
| `id`, `queue`, `act`, `map`, `startedAt`, `lengthMillis` | 경기 머리 |
| `me`, `myAgent`, `myRole` | 나와 내 요원. 역할은 서버 역할 표로 알아내고 모르면 비운다 |
| `allies` | 우리 팀. 팀킬과 자기 스킬 사망을 킬로 세지 않는 데 쓴다 |
| `myCombatScore`, `myTeamWon` | 내 점수와 승패(비기면 `null`) |
| `roundOutcomes` | 스코어용 라운드 결과. 못 뛴 라운드도 넣는다 |
| `rounds` | 내가 뛴 라운드(`Round`) |
| `players` | 스코어보드(`Scoreline`). 관전자와 코치는 넣지 않는다 |
| `teams` | `MatchTeam`(구성원, 승패, 점수, 등수). 셋 이상이면 등수로 끝나는 경기 |

- 커스텀 게임, 끝나지 않은 경기, 내가 없는 경기는 옮기지 않는다. 경기 ID와 시작 시각이 빠지면 던진다.
- 경기 모양(`MatchFormat`): `ROUNDS`, `TEAM_POINTS`, `FREE_FOR_ALL`, `TEAM_PLACEMENT`. 라운드가 없는 모드는 라운드를 0으로 두어
  라운드 값(전투 점수, 피해량, 관여율)을 비운다.

### Round (라운드)

`number`(1부터), `won`, `kills`(시각순 `KillEvent`, 겹친 킬은 하나), `myDamage`, `myShots`(머리·몸·다리), `mySide`(공격·수비,
모르면 비움), `ending`, `economy`(팀 장비 가치 → 이코·포스바이·풀바이), `myDamageTo`, `myDamageFrom`.

- 피해량과 맞힌 부위는 상대에게 준 것만 센다.
- 첫 킬과 첫 데스는 `timeSinceRoundStartMillis`로 가린 라운드의 처음 적 처치다. 자기 스킬 사망과 팀킬은 건너뛴다.
- 트레이드는 내가 죽은 뒤 5초 안에 우리 팀이 내 킬러를 잡은 경우다(시작 기준선).

### WeeklyReport (주간 리포트)

`sealed interface`다.

- `Ready`: `act`, `period`(`ReportPeriod`: 첫날, 주 수, 이번 주를 담는지), `metrics`(`MatchMetrics` 합계), `baseline`(지난 4주,
  모자라면 줄어든 주 수), `mainRole`과 `mainRoleShare`, `dynamic`(`DynamicSlot` 3~5개), `note`(`WeekNote`, 짚을 점), `insight`
  (`Insight`, 이번 액트 돌아보기), `trend`(`TrendWeek` 8주), `results`(승패 칸), `agents`, `weapons`.
- `NotEnoughMatches`: `played`, `notCounted`(리포트에 넣지 않는 모드 경기 수).

검증 규칙:

- 기간은 월요일 0시(기기 시간대)에 시작하는 주. 이번 주에 한 판도 없으면 지난주에서 끝나는 기간. 5경기를 못 채우면 최대 4주까지
  넓히고 그래도 못 채우면 `NotEnoughMatches`(FR-007).
- 리포트는 가장 최근 경기의 액트만 센다. 비교 기준도 그 액트 첫 경기가 있는 주부터라 줄어들 수 있다(FR-013).
- 비율은 총량을 더한 뒤 나누고 분모가 0이면 `null`(FR-012). 변화량은 보이는 자릿수로 반올림한 값끼리 뺀다(FR-009).
- 동적 칸 후보는 최소 표본을 넘기고 `|이번 − 지난 4주 평균| > 1.5 × 지난 8주 주간 표준편차`인 지표다. 표본을 넘긴 주가 4주 미만이면
  `Movement.UNKNOWN`이다. 관심사 지표와 움직인 지표를 앞에 두고 3개가 안 되면 `DynamicMetric` 선언 순서로 채운다. "이번 주는 큰
  변화 없음"은 하나도 움직이지 않았을 때만이고 `UNKNOWN`은 움직이지 않은 것으로 치지 않는다(FR-010).
- 짚을 점(`WeekNote.moved`)은 고정 지표 중 같은 1.5배 기준을 넘은 것 가운데 변화량을 변동폭으로 나눈 값이 가장 큰 하나다.
  오르내림은 가리지 않고 역할이 크게 띄우지 않는 지표는 고르지 않는다. `previousBest`는 오른 값이 이번 액트 앞 주들보다 높고
  리포트 기간이 한 주일 때만 채운다. 떨어진 쪽의 최저 기록은 담지 않는다(사용자 결정). 움직인 고정 지표가 없거나 기타 모드면
  `note`가 `null`이다(FR-038).
- 그 주의 역할(`mainRole`)은 라운드를 가장 많이 뛴 역할이다. 역할의 우선 지표와 크게 띄우지 않는 지표가 동적 칸, 짚을 점,
  돌아보기를 바꾼다(FR-011).

### Friend, FriendRequest

| 엔티티 | 필드 |
| --- | --- |
| `Friend` | `id`, `riotId`, `playerCard`, `statsPublic`, `matches`, `lastPlayedTogether` |
| `FriendRequest` | `id`, `riotId`, `playerCard`, `source`(`SCOREBOARD`, `INVITE_LINK`) |

상태 변화:

```text
(없음) ──요청(같이 뛴 경기 확인 또는 초대 링크)──▶ 요청 중 ──수락──▶ 친구 ──끊기──▶ (없음)
                                                    └──거절──▶ (없음)
```

- 수락할 때 둘 중 한쪽이 200명이면 409로 막고 요청은 남긴다. 새 요청은 하루(UTC) 30번, 초대 링크 하나로 20개(시작 기준선).
- 라이벌은 친구 한 명이고 기기에만 둔다.

### Ping (ㅇㅂㅇ, 파티 모집)

| 엔티티 | 필드 |
| --- | --- |
| `Ping` | `id`, `host`(`PingPerson`), `startsAt`, `createdAt`, `members` |
| `PingMember` | `person`, `answer`, `proposedAt` |

`PingAnswer` 상태 변화:

```text
PENDING ──갈게요──▶ YES
        ──못 가요──▶ NO
        ──다른 시간(proposedAt)──▶ OTHER_TIME
호스트가 시간을 바꾸면: 그 시각을 제안한 친구는 YES, 나머지는 PENDING으로 돌아간다
```

- 서로 수락한 친구만 최대 4명(못 간다고 한 친구는 세지 않는다). 시각은 지금부터 24시간 안이고 시작 한 시간 뒤 사라진다.
- 한 사람이 살아 있는 ㅇㅂㅇ은 하나, 24시간에 열 번까지. 글을 쓰는 칸은 없다(FR-026).

### ImportProgress (첫 수집)

`total`, `results`(받은 경기마다 승·패·무), `stoppedBy`(`OvalitError`).

```text
시작 ──한 판씩 저장──▶ 받는 중 ──다 받음──▶ 끝(isDone)
                         └──끊김·429·응답 없음──▶ 멈춤(stoppedBy) ──이어서 불러오기──▶ 받는 중
저장된 경기 지우기·연동 해제 ──▶ 더 저장하지 않음
```

- 멈춘 수집은 끝난 것으로 보지 않는다. 받은 경기는 남긴다(FR-006).

### NewMatchesProgress (새 경기 받기)

`total`, `received`. 다섯 판 이상일 때 홈과 경기 탭에 진행 줄을 둔다(`isShown`). 스무 판 이상이면 앱을 닫아도 이어 받는다.

## 서버 저장(D1)

| 표 | 담는 것 | 지우는 때 |
| --- | --- | --- |
| `users` | PUUID, 이름, 태그, `stats_public`, `remind_before`, 하루 요청 수 | 연동 해제 |
| `sessions` | 세션 토큰 해시, 기한(90일). 사람마다 최근 5개 | 로그아웃, 만료(로그인 때 50줄씩), 여섯 번째 로그인 |
| `login_codes` | 일회용 로그인 코드 해시, PKCE challenge | 교환, 만료 |
| `friend_requests` | 보낸 사람, 받는 사람, 출처 | 수락, 거절, 연동 해제 |
| `friendships` | 두 사람(`user_a < user_b`) | 끊기, 연동 해제 |
| `invites` | 초대 코드, 기한 7일, 쓴 횟수 | 만료, 연동 해제 |
| `match_players` | 경기마다 PUUID JSON 배열 한 줄 | 10주 뒤 크론(한 번에 100줄) |
| `pings`, `ping_members` | ㅇㅂㅇ과 불린 친구(순서, 대답, 알림 여부) | 끝나면 크론 |
| `push_tokens` | FCM 기기 토큰(사람마다 최근 3개)과 등록한 세션 | FCM이 없다고 답할 때, 그 세션이 끝날 때(로그아웃, 만료, 밀려남), 연동 해제 |
| `riot_blocks` | 막힌 Riot 범위와 풀리는 시각 | 풀리면 크론 |
| `weekly_summaries` | 친구 비교와 라이벌 대결에 쓰는 주·액트·큐마다의 고정 칸 합계 | 10주 뒤 크론, 연동 해제 |

- 경기 원문은 D1에 두지 않는다. 연동을 해제하면 `users` 한 줄을 지우고 세션, 친구, 요청, 초대, ㅇㅂㅇ, 기기 토큰이
  `ON DELETE CASCADE`로 같이 지워진다. 경기 참가자 기록은 10주 뒤 크론이 지우고 담아 둔 경기 상세는 30일 안에 사라진다(FR-030).
