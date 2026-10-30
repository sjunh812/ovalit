---

description: "오발있 기준 스펙의 작업 목록. 만든 것과 남은 일"
---

# Tasks: 오발있 기준 스펙

**Input**: Design documents from `/specs/001-ovalit-baseline/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: 스펙은 테스트 우선을 요구하지 않는다. 다만 `docs/CONVENTIONS.md`가 지표 계산, 지켜야 할 선, 디자인 토큰 제약은 테스트가
지키게 하라고 해서 남은 일 중 그 셋에 닿는 것에만 테스트 작업을 둔다.

**Organization**: 사용자 이야기(P1~P6)마다 한 단계다. `[x]`는 2026-10-30 기준 `main`(8ab09a3)과 Figma 파일 "오발있"
(`3pMWI4a1FQS7slMyUlLMeH`)에 이미 있는 것이고, `[ ]`는 남은 일이다. 남은 일 대부분은 [research.md](research.md)의 열린 결정을
닫는 일이고, 그중 실제 서버에 닿는 것은 Riot 프로덕션 키가 승인돼야 시작할 수 있다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일을 고치고 앞선 미완료 작업에 기대지 않아 같이 할 수 있다
- **[Story]**: 사용자 이야기(US1~US6)
- **Figma**: 파일 "오발있"의 페이지 이름을 경로로 쓴다. 새 화면은 "홈 (S1)"과 같은 틀(라이트 + 파란 번호, 화면 설명 패널, 다크,
  옆 섹션에 상태별 화면과 "언제 뜨는지")로 그리고, 그리기 전에 심사 목업의 그 화면과 `docs/screens.md`의 그 절을 본다

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 저장소, 빌드, 서버 프로젝트의 뼈대

- [x] T001 Gradle 모듈 14개와 convention plugin(`ovalit.android.application`, `ovalit.kmp.library`, `ovalit.kmp.feature`) in settings.gradle.kts, build-logic/convention/
- [x] T002 [P] 버전 카탈로그(Kotlin 2.4.20, CMP 1.12.0, Koin 4.2.2, Navigation 3) in gradle/libs.versions.toml
- [x] T003 [P] CI(Android, iOS, Server)와 새 게임 콘텐츠 확인 in .github/workflows/ci.yml, .github/workflows/content-watch.yml
- [x] T004 [P] 서버 프로젝트(Hono, wrangler, Vitest)와 D1 마이그레이션 0001~0010 in server/package.json, server/wrangler.jsonc, server/migrations/
- [x] T005 [P] Spec Kit 헌법 v1.0.0과 기준 스펙 in .specify/memory/constitution.md, specs/001-ovalit-baseline/spec.md
- [x] T006 [P] 기준 계획과 설계 문서 in specs/001-ovalit-baseline/plan.md, research.md, data-model.md, contracts/, quickstart.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 이야기가 기대는 모델, 디자인 시스템, 내비게이션, 서버 공통

- [x] T007 도메인 식별자와 경기 모델(`MatchId`, `PlayerId`, `Match`, `Round`, `Queue`, `QueueFilter`, `Role`) in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/Ids.kt, Match.kt, Queue.kt, QueueFilter.kt, Role.kt
- [x] T008 [P] 색 토큰, 글자 단계, 간격, 컴포넌트 in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/theme/, component/
- [x] T009 [P] 아이콘(목업 SVG 경로) in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/icon/OvalitIcons.kt
- [x] T010 Navigation 3 뼈대, Koin 모듈 묶기, 화면 전환, 탭 상태 보존 in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt, OvalitModules.kt, OvalitTransitions.kt, TabStateDecorator.kt
- [x] T011 [P] 플랫폼 경계(뒤로 두 번 종료, 알림 권한, 공유 창, 피드백 메일) in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitPlatform.kt, composeApp/src/main/kotlin/com/ovalit/AndroidOvalitPlatform.kt
- [x] T012 [P] 실패 안내(`OvalitError`, `launchNotifying`, `FailureNotices`, 토스트) in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/OvalitError.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/FailureNotice.kt
- [x] T013 [P] 사용 통계·오류 보고(고정 화면 이름, 구간 값) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/Analytics.kt, composeApp/src/main/kotlin/com/ovalit/telemetry/OvalitFirebase.kt
- [x] T014 서버 공통(라우팅, 세션, 에러, Riot 레이트 리밋, 가리기) in server/src/app.ts, session.ts, errors.ts, riot.ts, redact.ts
- [x] T015 서버 인증(RSO + PKCE + App Link, 서명한 state) in server/src/rso.ts, state.ts, routes/auth.ts, routes/public.ts
- [x] T016 [P] 콘텐츠 표(티어 이름, 요원 역할)와 VAL-CONTENT·VAL-STATUS 대리 in server/src/data/, server/src/routes/content.ts
- [x] T017 [P] Figma 기초: 색 변수(라이트·다크 두 컬렉션 28개씩), 간격 변수, 글자 스타일 12개, 로고·아이콘 21개, 홈 컴포넌트 in Figma "기초" 페이지
- [x] T018 [P] Figma 기획 개요(스펙·헌법·화면 흐름·성공 기준·용어) in Figma "기획 개요" 페이지
- [ ] T019 [P] 숫자 글자 스타일 다섯(metricXl, metricL, metricM, metricS, delta)에 Tabular figures 켜기(API로는 못 켜서 사용자가 Figma 글자 패널에서) in Figma "기초" 페이지

**Checkpoint**: 기초가 갖춰져 이야기마다 따로 진행할 수 있다.

---

## Phase 3: User Story 1 - 연동하고 이번 주 리포트를 본다 (Priority: P1) 🎯 MVP

**Goal**: Riot 연동 뒤 최근 50경기(8주)를 받아 이번 주 지표가 지난 4주의 나와 견줘 어떻게 움직였는지 보여 준다.

**Independent Test**: 새 계정으로 연동해 첫 수집을 마치고 홈 리포트 숫자를 같은 경기로 손으로 센 값과 견준다([quickstart.md](quickstart.md) P1).

### 만든 것

- [x] T020 [P] [US1] 지표 계산(총량 뒤 나누기, 분모 0은 비움, 보이는 자릿수 변화량, 1.5배 기준, 액트 경계) in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/MatchMetricsCalculator.kt, WeeklyReportCalculator.kt, DynamicMetric.kt, WeekNote.kt, Insight.kt
- [x] T021 [P] [US1] Riot 응답 옮겨 담기(문자열로 받아 `decodeMatch`) in shared/core/network/src/commonMain/kotlin/com/ovalit/core/network/MatchMapper.kt, MatchlistMapper.kt, RiotJson.kt
- [x] T022 [US1] 받기 규칙(첫 수집 50경기·8주, 끝난 경기 다시 받지 않기, 10분 재확인, 멈추면 까닭 적기) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/OfflineFirstMatchRepository.kt, NewMatchesWatcher.kt, MatchStore.kt, FakeMatchRemoteSource.kt
- [x] T023 [P] [US1] 첫 수집과 쌓인 새 경기를 앱 밖에서 받기 in composeApp/src/main/kotlin/com/ovalit/importing/FirstImport.kt, shared/app/src/iosMain/kotlin/com/ovalit/app/InProcessImportScheduler.kt
- [x] T024 [P] [US1] 온보딩 S0-1, S0-2, S0-4(관심사, 받은 경기 칸, 멈춤과 이어서 불러오기) in shared/feature/onboarding/src/commonMain/kotlin/com/ovalit/feature/onboarding/intro/IntroScreen.kt, consent/ConsentScreen.kt, importing/ImportScreen.kt, importing/ImportViewModel.kt
- [x] T025 [US1] 홈 S1(머리, 큐 칩, 기간 줄, 승패 칸, 짚을 점, 고정 칸, 달라진 점, 돌아보기, 요원·무기) in shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/ReportScreen.kt, ReportViewModel.kt, component/
- [x] T026 [P] [US1] S1-a 지표 설명 시트, 달라진 점 시트, 8주 흐름 시트 in shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/component/MetricSheet.kt, DynamicMetricSheet.kt, TrendSheet.kt
- [x] T027 [P] [US1] 서버 내 경기 경로(`/riot/matchlist`, `/riot/matches/:matchId`, 참가자 기록) in server/src/routes/riot.ts
- [x] T028 [P] [US1] Figma 홈 라이트·다크, 번호 13개와 화면 설명, 상태 7장(새 경기 받는 중, 처음 홈, 남은 경기, 기타 칩, 저장된 경기 없음, 라이벌 대결, S1-a) in Figma "홈 (S1)" 페이지

### 남은 일: 문서와 디자인

- [ ] T029 [US1] 홈 순서 어긋남 정리: `docs/screens.md` 홈 (S1)은 "큐 칩, ㅇㅂㅇ 카드, 새 경기 진행 줄"인데 `ReportScreen.kt`는 진행 줄을 칩 바로 밑, ㅇㅂㅇ 카드를 카드 목록 맨 위에 둔다. 사용자에게 어느 쪽이 맞는지 묻고 다른 쪽을 고친다 in docs/screens.md, shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/ReportScreen.kt, research.md 열린 결정 표
- [ ] T030 [P] [US1] Figma 온보딩 S0-1 인트로(로고, "오늘 발로란트 할 사람 있어?", 영문 비공식 고지), S0-2 연동 동의, S0-4 불러오는 중(관심사 고르기, 50칸, 멈춤과 "이어서 불러오기", 알림 약속 유무)을 라이트·다크와 화면 설명으로 in Figma "온보딩 (S0)" 페이지
- [ ] T031 [P] [US1] Figma 홈 나머지 상태: 기타 칩 리포트(K/D, KDA, 헤드샷만), 달라진 점 시트, 8주 흐름 시트(막대를 끈 상태 포함), "이번 주는 큰 변화 없음", 짚을 점의 비중 줄 in Figma "홈 (S1)" 페이지의 "홈 상태" 섹션

### 남은 일: 실제 연동과 저장(프로덕션 키 승인 뒤)

- [ ] T032 [US1] 로컬 DB 고르기: KMP를 지원하고 상업 이용이 무료인 라이선스(Apache-2.0·MIT·SIL OFL)인 후보를 실제 응답 크기로 견줘 사용자에게 묻고 결정을 남긴다 in docs/DECISIONS.md, CLAUDE.md 아키텍처, specs/001-ovalit-baseline/research.md
- [ ] T033 [US1] `MatchStore`를 고른 로컬 DB로 구현해 경기, 첫 수집 진행도와 멈춘 까닭(`ImportProgress.stoppedBy`), 마지막 확인 시각, 받다 남은 새 경기를 기기에 남긴다. "앱을 강제로 닫아도 남은 것만 이어 받는다", FR-036 "인터넷이 없어도 기기에 저장된 경기로 주간 리포트, 경기 목록, 내 프로필을 보여 줘야" 한다 in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/MatchStore.kt와 새 구현 파일
- [ ] T034 [US1] `MatchStore` 구현 테스트(다시 켜면 이어 받기, 지우기 뒤 더 저장하지 않기, 인터넷 없이 다시 켜도 저장된 경기로 리포트가 나오고 당기면 인터넷 끊김 까닭을 던지기) in shared/core/data/src/commonTest/kotlin/com/ovalit/core/data/
- [ ] T035 [US1] HTTP 클라이언트(Ktor)를 붙이고 본문은 문자열로 받아 직접 읽는다(`ContentNegotiation` 금지, 실패 예외에 원문을 담지 않기), 세션은 `Authorization: Bearer`, 에러는 [contracts/server-api.md](contracts/server-api.md)대로 `OvalitError`로 옮긴다 in shared/core/network/
- [ ] T036 [US1] `MatchRemoteSource` 실제 구현(`GET /riot/matchlist`, `GET /riot/matches/:matchId`, 사용자 한도 429는 새 경기가 없는 것처럼) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/
- [ ] T037 [US1] RSO 연결: S0-3을 Custom Tabs로 띄우고 PKCE verifier를 만들어 challenge만 넘기며, App Link `/auth/done?code=`를 받아 `POST /auth/session`으로 세션을 받는다. WebView 금지, 커스텀 스킴 금지 in shared/feature/onboarding/, composeApp/src/main/AndroidManifest.xml(호스트는 `local.properties`의 `ovalit.server.host`)
- [ ] T038 [US1] 실제 `AccountRepository`: 계정 값은 기기에서 읽은 뒤 처음 내보낸다(읽기 전에 `null`을 먼저 내보내지 않기), 로그인이 풀리면 다시 시도하지 않고 멈춘다 in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/AccountRepository.kt와 새 구현 파일
- [ ] T039 [US1] 시작 화면을 연동과 첫 수집 상태로 고른다(연동 전 인트로, 첫 수집 중 S0-4에서 `start()`로 이어 받기, 나머지 홈) in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt
- [ ] T040 [US1] 로그인 풀림 화면과 점검 안내 줄(`GET /status`) in shared/feature/report/, shared/core/ui/
- [ ] T041 [US1] "아직 확인하지 못한 것"(맵 ID 형태, `roundNum` 시작, 항복 라운드, 튕긴 사람, 라운드 없는 모드, 건틀릿 큐 ID, 킬의 `damageType`)을 실제 응답으로 확인하고 옮겨 담기와 테스트를 고친다 in shared/core/network/src/commonMain/kotlin/com/ovalit/core/network/MatchMapper.kt, shared/core/network/src/commonTest/, CLAUDE.md 데이터
- [ ] T042 [US1] 전투점수 칸 결정: 실제 경기 응답에 `players[].stats.score`와 Performance Score가 어떻게 오는지 보고 사용자에게 묻는다. 바뀌면 고정 칸(`FixedMetric`), 문서, Figma를 같은 커밋에서 고친다 in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/FixedMetric.kt, CLAUDE.md 지켜야 할 선, .specify/memory/constitution.md, docs/DECISIONS.md

**Checkpoint**: 실제 계정으로 P1 독립 검사가 통과하고 SC-001, SC-005, SC-006을 잰다.

---

## Phase 4: User Story 2 - 지난 경기를 돌아본다 (Priority: P2)

**Goal**: 날짜별 경기 목록과 경기 상세(스코어보드, 라운드, 내 기록)로 리포트 숫자의 출처를 확인한다.

**Independent Test**: 경기 탭에서 한 경기를 열어 세 탭의 숫자가 그 경기 결과와 맞는지 본다([quickstart.md](quickstart.md) P2).

### 만든 것

- [x] T043 [P] [US2] 경기 목록 S2(날짜 묶음과 그날 승패, 요원·맵 거르기, 당겨서 새 경기와 결과 토스트) in shared/feature/match/src/commonMain/kotlin/com/ovalit/feature/match/MatchesScreen.kt, MatchesViewModel.kt, shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/NewMatchCount.kt
- [x] T044 [P] [US2] 경기 상세 S3(스코어보드, 라운드 탭 구매 유형별 승패, 내 기록) in shared/feature/match/src/commonMain/kotlin/com/ovalit/feature/match/MatchDetailScreen.kt, MatchDetailParts.kt, MatchRounds.kt, MatchReport.kt

### 남은 일

- [ ] T045 [P] [US2] Figma S2 경기 목록(날짜 머리, 경기 줄 칩 MVP·팀 MVP·에이스·클러치 톤온톤, 등수로 끝나는 경기 줄, 거르기 시트, 진행 줄 상태)을 라이트·다크와 화면 설명으로 in Figma "경기 (S2)" 페이지
- [ ] T046 [P] [US2] Figma S3 경기 상세 세 탭(스코어보드 친구 표시, 라운드 막대는 이긴 라운드만 pos, 내 기록)과 상태(라운드 없는 모드, 스켈레톤) in Figma "경기 상세 (S3)" 페이지

**Checkpoint**: P2 독립 검사가 통과한다.

---

## Phase 5: User Story 3 - 내 기록을 모아 본다 (Priority: P3)

**Goal**: 내 프로필에서 이번 액트 티어, 승률, 통계, 맞힌 부위, 요원, 무기, 최근 경기를 보고 S6·S7로 들어간다.

**Independent Test**: 홈 아바타로 내 프로필을 열고 요원·무기 화면의 값을 이번 액트 경기로 센 값과 견준다([quickstart.md](quickstart.md) P3).

### 만든 것

- [x] T047 [P] [US3] 내 프로필(티어 카드, 통계, 맞힌 부위, 요원, 무기, 최근 경기) in shared/feature/profile/src/commonMain/kotlin/com/ovalit/feature/profile/ProfileScreen.kt, ProfileViewModel.kt, ProfileSections.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/ProfileSections.kt
- [x] T048 [P] [US3] S6 무기, S7 요원 in shared/feature/profile/src/commonMain/kotlin/com/ovalit/feature/profile/WeaponsScreen.kt, AgentsScreen.kt, RecordsViewModel.kt

### 남은 일

- [ ] T049 [P] [US3] Figma 내 프로필, S6, S7(목업에 없는 화면이라 `docs/screens.md`만 근거로, 배너는 카드 그림 전까지 fill 면) in Figma "프로필 (내 프로필, S6, S7)" 페이지
- [ ] T050 [US3] 실제 서버 연결 뒤 플레이어 카드를 서버 정적 에셋(`/cards/{uuid}_small|wide.png`)에서 받아 아바타와 프로필 배너에 쓴다. 받기 전에는 Riot ID 첫 글자 in shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/

**Checkpoint**: P3 독립 검사가 통과한다.

---

## Phase 6: User Story 4 - 같이 뛴 사람과 친구를 맺고 견준다 (Priority: P4)

**Goal**: 스코어보드나 초대 링크로 친구를 맺고 서로 수락한 친구끼리만 프로필, 라이벌 대결, 친구 비교를 본다.

**Independent Test**: 두 계정이 같이 뛴 경기에서 요청과 수락 뒤 서로의 프로필과 라이벌 칸이 보이는지 확인한다([quickstart.md](quickstart.md) P4).

### 만든 것

- [x] T051 [P] [US4] 서버 친구·초대(같이 뛴 경기 확인, 200명, 하루 30번, 링크당 20개, 내가 안 뛴 경기 가리기) in server/src/routes/friends.ts, routes/invites.ts, relations.ts, redact.ts
- [x] T052 [P] [US4] 친구 탭, S5 플레이어 프로필, 친구 경기 목록 in shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/FriendsScreen.kt, FriendProfileScreen.kt, FriendMatchesScreen.kt, FriendsViewModel.kt, FriendProfileViewModel.kt
- [x] T053 [P] [US4] 홈 라이벌 대결, 유도 칸, 친구 비교와 전체 순위 in shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/component/SocialSections.kt, NudgeSection.kt, FriendRankingScreen.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/HeadToHeadRow.kt

### 남은 일

- [ ] T054 [US4] 주간 요약 서버: 표(주, 액트, 큐, 합계: 경기·라운드 수, 킬·데스·어시, 전투점수, 피해량, 맞힌 부위별 탄)와 올리기·받기 경로. "세션이 있어야 받고, 서로 수락한 친구가 전적을 공개했을 때만 내려준다. 친구를 끊거나 연동을 해제하면 지운다" in server/migrations/0011_weekly_summaries.sql, server/src/routes/, contracts/server-api.md, docs/backend.md 친구 비교 요약
- [ ] T055 [US4] 주간 요약 서버 테스트(세션 없음 401, 친구 아님·비공개 차단, 끊기·해제 뒤 삭제, 비율이 아니라 합계만 받기) in server/test/summaries.test.ts
- [ ] T056 [US4] 실제 `FriendRepository`(`/friends`, `/friends/requests`, 수락·거절·끊기, 초대 링크 `POST /invites`와 `redeem`) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/FriendRepository.kt와 새 구현 파일
- [ ] T057 [US4] 친구 경기를 기기에 저장하고 S5를 열면 저장된 것을 바로 보여 준 뒤 10분이 지났을 때만 새 경기를 받는다. "마지막 갱신 3분 전", 같은 친구 당기기는 1분에 한 번 in shared/core/data/, shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/FriendProfileScreen.kt
- [ ] T058 [US4] 앱이 주(월요일 0시)마다 고정 칸 합계를 올리고, 친구 비교와 라이벌 대결을 친구 경기 대신 요약으로 센다(기간을 넓힐 때 더한 뒤 나누기) in shared/core/data/, shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/ReportViewModel.kt
- [ ] T059 [P] [US4] Figma 친구 탭(친구 목록 슬라이스, 받은 요청), S5(전적 비공개 상태 포함), 친구 요청 시트와 초대 링크 시트, 라이벌 고르기 시트, 친구 비교 전체 순위 in Figma "친구 (탭, S5)" 페이지

**Checkpoint**: P4 독립 검사가 실제 계정 둘로 통과한다.

---

## Phase 7: User Story 5 - 친구를 불러 같이 한다 (Priority: P5)

**Goal**: 친구를 4명까지 골라 시각을 정해 부르고 정해 둔 답만 오가며 시작 전에 알림을 받는다.

**Independent Test**: 친구 둘을 불러 받은 쪽이 다른 시간으로 답하고 부른 쪽이 받아들이는지 확인한다([quickstart.md](quickstart.md) P5).

### 만든 것

- [x] T060 [P] [US5] 서버 ㅇㅂㅇ(4명, 24시간 안, 하루 10번, 시간 바꾸면 다시 묻기)과 푸시·크론 in server/src/routes/pings.ts, push.ts, scheduled.ts
- [x] T061 [P] [US5] ㅇㅂㅇ 화면과 홈·친구 탭 카드 in shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/PingDetailScreen.kt, PingDetailViewModel.kt, PingSection.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/PingRow.kt
- [x] T062 [P] [US5] 푸시 받기와 알림(문구는 앱이 만든다) in composeApp/src/main/kotlin/com/ovalit/push/OvalitMessagingService.kt, PingNotifications.kt, PingActionReceiver.kt, OvalitPush.kt

### 남은 일

- [ ] T063 [US5] 실제 `PingRepository`와 `PushRepository`(`/pings` 경로, `PUT /me/push-token`, 로그아웃 전 `DELETE /me/push-token`) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/PingRepository.kt, PushRepository.kt와 새 구현 파일
- [ ] T064 [P] [US5] Figma 오발있? 카드(친구 탭), 부르기 시트(고른 칸 accent 테두리와 체크, 옆으로 미는 친구 줄), 초대 화면(받은 쪽·보낸 쪽, 다른 시간 제안을 맨 위에), 30분 단위 휠, 알림 모양 in Figma "오발있? (파티 모집)" 페이지

**Checkpoint**: P5 독립 검사가 실제 계정 둘로 통과한다.

---

## Phase 8: User Story 6 - 내 데이터와 공개 범위를 관리한다 (Priority: P6)

**Goal**: 전적 공개, 알림, 관심사, 기본 큐를 바꾸고 저장된 데이터를 지우거나 연동을 해제한다.

**Independent Test**: 전적 공개를 끈 뒤 친구 쪽에서 내 경기가 안 보이는지, 연동 해제 뒤 서버에 정보가 남지 않는지 본다([quickstart.md](quickstart.md) P6).

### 만든 것

- [x] T065 [P] [US6] 설정 S4(전적 공개, 알림, 관심사, 기본 큐, 저장된 데이터 지우기, 연동 해제, 광고 없이 보기) in shared/feature/settings/src/commonMain/kotlin/com/ovalit/feature/settings/SettingsScreen.kt, SettingsSheets.kt, SettingsViewModel.kt, NotificationBlocks.kt
- [x] T066 [P] [US6] 서버 계정(`GET/PATCH/DELETE /me`, 해제 때 같이 지우기) in server/src/routes/me.ts, users.ts
- [x] T067 [P] [US6] Riot 비공식 고지(영문 원문) in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/component/OvalitDisclaimer.kt
- [x] T068 [P] [US6] 앱을 지운 사람의 삭제 요청(메일)과 이용약관 in site/privacy/index.html, site/terms/index.html

### 남은 일

- [ ] T069 [US6] 실제 `AccountSettingsRepository`(`PATCH /me {statsPublic?, remindBefore?}`, 서버 값을 따른다)와 연동 해제(`DELETE /me` 뒤 기기 기록과 보내지 않은 보고 지우기) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/AccountSettingsRepository.kt, AccountSettingsSync.kt와 새 구현 파일
- [ ] T070 [P] [US6] Figma S4 설정(묶음 카드, 오발있? 카드의 파티 모집 알림과 시작 전 알림, 연동 해제 위험 버튼, 맨 아래 영문 고지) in Figma "설정 (S4)" 페이지

**Checkpoint**: 모든 이야기가 따로 돌아간다.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 출시 전 공통 일

- [ ] T071 광고 동의 창(UMP)을 붙인다. 출시 전에, 광고 단위 ID는 Riot 승인 뒤 제품 설명에 광고를 적고 나서 in composeApp/src/main/kotlin/com/ovalit/ads/, docs/RELEASE.md
- [ ] T072 [P] 새로 넣는 문구는 같은 커밋에서 일본어도 고친다 in shared/**/composeResources/values-ja/strings.xml, docs/i18n-ja.md
- [ ] T073 [P] Figma 상태 화면 다크판(지금은 라이트만) in Figma 각 화면 페이지의 상태 섹션
- [ ] T074 실제 계정으로 [quickstart.md](quickstart.md) 전체를 돌고 SC-001(첫 수집 1분 안팎)과 SC-007(통계·오류 보고에 Riot 값 0건)을 잰다 in specs/001-ovalit-baseline/quickstart.md
- [ ] T075 열린 결정을 닫을 때마다 결정을 남기고 스펙과 규칙 문서를 같은 커밋에서 맞춘다 in docs/DECISIONS.md, specs/001-ovalit-baseline/research.md, spec.md
- [ ] T076 Figma에서 화면이 확정되면 바로 앱에 반영한다. 지금 앱과 달라진 곳은 그 화면 모듈과 `docs/screens.md`·`docs/design.md`를 같은 커밋에서 고치고, 심사 목업에서 벗어나는 부분은 반영 전에 사용자에게 묻고 `docs/DECISIONS.md`에 남긴다(spec.md Clarifications 2026-10-30) in shared/feature/, docs/screens.md, docs/design.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup, Foundational**: 끝났다(T019만 남음, 다른 일을 막지 않는다).
- **Figma 작업**(T030, T031, T045, T046, T049, T059, T064, T070, T073): 지금 할 수 있고 서로 기대지 않는다.
- **실제 서버 연결**: Riot 프로덕션 키 승인이 먼저다. 그 안에서 T035(HTTP 클라이언트)가 T036, T037, T038, T056, T063, T069보다 먼저다.
- **로컬 DB**: T032(결정) → T033 → T034. T039(시작 화면)는 T033과 T038 뒤다.
- **주간 요약**: T054 → T055, T058. T058은 T056 뒤다.
- **전투점수 칸**(T042)과 응답 확인(T041)은 실제 응답을 받은 뒤다.

### User Story Dependencies

- **US1**: 다른 이야기에 기대지 않는다. 실제 연동(T035~T040)이 US3~US6의 실제 저장소 작업보다 먼저다.
- **US2, US3**: US1의 실제 경기 받기(T036)만 있으면 된다.
- **US4**: 실제 서버 연결(T035) 뒤. 주간 요약(T054)은 US4 안에서 닫힌다.
- **US5**: 실제 친구 저장소(T056) 뒤.
- **US6**: 실제 계정 저장소(T038) 뒤.

### Parallel Opportunities

- Figma 화면 작업은 모두 [P]다. 한 세션에서 한 화면씩 그리고 사용자에게 보여 준 뒤 다음으로 간다.
- 실제 서버 연결 뒤 T056(친구), T063(ㅇㅂㅇ·푸시), T069(설정)는 서로 다른 저장소라 같이 할 수 있다.

---

## Parallel Example: 프로덕션 키 승인 전에 할 수 있는 것

```text
Task: "T029 홈 순서 어긋남 정리(사용자에게 묻기)"
Task: "T030 Figma 온보딩 (S0)"
Task: "T045 Figma 경기 (S2)"
Task: "T049 Figma 프로필"
Task: "T054 주간 요약 서버 표와 경로(서버는 로컬 D1로 돈다)"
```

## Parallel Example: 승인 뒤 저장소 붙이기

```text
Task: "T056 실제 FriendRepository"
Task: "T063 실제 PingRepository와 PushRepository"
Task: "T069 실제 AccountSettingsRepository와 연동 해제"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. 승인 전: T029(문서·코드 맞추기), T030·T031(Figma 온보딩과 홈 나머지), T032 후보 정리.
2. 승인 뒤: T035 → T036 → T037 → T038 → T033 → T039 → T040 순서로 실제 연동을 붙인다.
3. **멈추고 확인**: 실제 계정으로 P1 독립 검사, SC-001·SC-005·SC-006.
4. T041·T042로 응답 모양과 전투점수 칸을 닫는다.

### Incremental Delivery

1. US1 실제 연동 → 출시 가능한 최소 앱(혼자 쓰는 리포트).
2. US2·US3은 저장소 교체만으로 따라온다.
3. US4(친구와 주간 요약) → US5(ㅇㅂㅇ) → US6(설정의 서버 연결).
4. 출시 전 T071(UMP)과 T074(실측).

---

## Notes

- `[x]` 작업은 기준선 기록이다. 다시 하지 않는다.
- 각 작업이 규칙을 바꾸면 `CLAUDE.md`, `docs/screens.md`, `docs/design.md`, `docs/backend.md` 중 그 규칙이 있는 문서를 같은 커밋에서 고친다.
- 심사 목업(아티팩트, `site/index.html`, `site/riot.txt`)은 고치지 않는다. 새 디자인이 목업에서 벗어나면 먼저 묻고 `docs/DECISIONS.md`에 남긴다.
