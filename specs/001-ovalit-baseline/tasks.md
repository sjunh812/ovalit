---

description: "오발있 기준 스펙의 작업 목록. 만든 것과 남은 일"
---

# Tasks: 오발있 기준 스펙

**Input**: Design documents from `/specs/001-ovalit-baseline/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: 스펙은 테스트 우선을 요구하지 않는다. 다만 `docs/CONVENTIONS.md`가 지표 계산, 지켜야 할 선, 디자인 토큰 제약은 테스트가
지키게 하라고 해서 남은 일 중 그 셋에 닿는 것에만 테스트 작업을 둔다.

**Organization**: 사용자 이야기(P1~P6)마다 한 단계다. `[x]`는 2026-10-30 기준 `main`(eab00ad)과 Figma 파일 "오발있"
(`3pMWI4a1FQS7slMyUlLMeH`)에 이미 있는 것이고 `[ ]`는 남은 일이다. 남은 일 대부분은 [research.md](research.md)의 열린 결정을
닫는 일이고 그중 실제 서버에 닿는 것은 Riot 프로덕션 키가 승인돼야 시작할 수 있다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일을 고치고 앞선 미완료 작업에 기대지 않아 같이 할 수 있다
- **[Story]**: 사용자 이야기(US1~US6)
- **Figma**: 파일 "오발있"의 "화면" 페이지 안 줄(섹션) 이름을 경로로 쓴다. 무료 플랜이라 페이지는 "표지 · 흐름", "디자인 시스템", "화면" 셋뿐이고
  새 페이지를 만들지 않는다(`docs/DECISIONS.md` 2026-10-30). 화면마다 한 줄씩 아래로 쌓고 줄마다 화면 섹션과 상태 섹션을 나란히
  둔다. 새 화면은 "홈 (S1)" 줄과 같은 틀(라이트 + 파란 번호, 화면 설명 패널, 다크, 옆 상태 섹션에 상태별 화면과 "언제 뜨는지")로
  그리고, 그리기 전에 심사 목업의 그 화면과 `docs/screens.md`의 그 절을 본다

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 저장소, 빌드, 서버 프로젝트의 뼈대

- [x] T001 Gradle 모듈 14개와 convention plugin(`ovalit.android.application`, `ovalit.kmp.library`, `ovalit.kmp.feature`) in settings.gradle.kts, build-logic/convention/
- [x] T002 [P] 버전 카탈로그(Kotlin 2.4.20, CMP 1.12.0, Koin 4.2.2, Navigation 3) in gradle/libs.versions.toml
- [x] T003 [P] CI(Android, iOS, Server)와 새 게임 콘텐츠 확인 in .github/workflows/ci.yml, .github/workflows/content-watch.yml
- [x] T004 [P] 서버 프로젝트(Hono, wrangler, Vitest)와 D1 마이그레이션 0001~0010 in server/package.json, server/wrangler.jsonc, server/migrations/
- [x] T005 [P] Spec Kit 헌법(v1.0.0으로 만들고 지금 v1.0.3)과 기준 스펙 in .specify/memory/constitution.md, specs/001-ovalit-baseline/spec.md
- [x] T006 [P] 기준 계획과 설계 문서 in specs/001-ovalit-baseline/plan.md, research.md, data-model.md, contracts/, quickstart.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 이야기가 기대는 모델, 디자인 시스템, 내비게이션, 서버 공통

- [x] T007 도메인 식별자와 경기 모델(`MatchId`, `PlayerId`, `Match`, `Round`, `Queue`, `QueueFilter`, `Role`) in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/Ids.kt, Match.kt, Queue.kt, QueueFilter.kt, Role.kt
- [x] T008 [P] 색 토큰, 글자 단계, 간격, 컴포넌트 in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/theme/, component/
- [x] T009 [P] 아이콘(목업 SVG 경로) in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/icon/OvalitIcons.kt
- [x] T010 Navigation 3 뼈대, Koin 모듈 묶기, 화면 전환, 탭 상태 보존 in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt, OvalitModules.kt, OvalitTransitions.kt, TabStateDecorator.kt
- [x] T011 [P] 플랫폼 경계(뒤로 두 번 종료, 알림 권한, 공유 창, 피드백 메일) in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitPlatform.kt, composeApp/src/main/kotlin/com/ovalit/AndroidOvalitPlatform.kt
- [x] T012 [P] 실패 안내(`OvalitError`, `launchNotifying`, `FailureNotices`, 토스트) in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/OvalitError.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/FailureNotice.kt, 토스트는 shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/component/OvalitToast.kt
- [x] T013 [P] 사용 통계·오류 보고(구간 값, 고정 화면 이름) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/Analytics.kt, composeApp/src/main/kotlin/com/ovalit/telemetry/OvalitFirebase.kt, 고정 화면 이름 표(`screenName`)는 shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt
- [x] T014 서버 공통(라우팅, 세션, 에러, Riot 레이트 리밋, 가리기) in server/src/app.ts, session.ts, errors.ts, riot.ts, redact.ts
- [x] T015 서버 인증(RSO + PKCE + App Link, 서명한 state) in server/src/rso.ts, state.ts, routes/auth.ts, routes/public.ts
- [x] T016 [P] 콘텐츠 표(티어 이름, 요원 역할)와 VAL-CONTENT·VAL-STATUS 대리 in server/src/data/, server/src/routes/content.ts
- [x] T017 [P] Figma 기초: 색 변수(라이트·다크 두 컬렉션 28개씩), 간격 변수, 글자 스타일 12개, 로고·아이콘 21개, 홈 컴포넌트 in Figma "디자인 시스템" 페이지(2026-10-30 "기초"에서 이름을 바꿈)
- [x] T018 [P] Figma 기획 개요(스펙·헌법·화면 흐름·성공 기준·용어) in Figma "표지 · 흐름" 페이지(2026-10-30 표지, 화면 흐름 지도, 진행 현황, 지켜야 할 선, 용어로 다시 꾸밈)
- [x] T087 [P] AdMob 네이티브 광고 자리(광고 단위 ID가 없으면 요청하지 않고 자리도 없다, 숫자를 읽는 중간에 두지 않는다, 24시간 광고 없이 보기) in shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/AdSlot.kt, composeApp/src/main/kotlin/com/ovalit/ads/AdMobRenderer.kt, NativeAdRow.kt, AdFreeSheet.kt
- [x] T088 [P] 일본어 화면 문구(모듈마다 `values-ja/strings.xml` 10개, 용어는 `docs/i18n-ja.md`) in shared/**/composeResources/values-ja/strings.xml, composeApp/src/main/res/values-ja/strings.xml
- [ ] T019 [P] 숫자 글자 스타일 다섯(metricXl, metricL, metricM, metricS, delta)에 Tabular figures 켜기(API로는 못 켜서 사용자가 Figma 글자 패널에서) in Figma "디자인 시스템" 페이지

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
- [x] T028 [P] [US1] Figma 홈 라이트·다크, 번호 13개와 화면 설명, 상태 8장(새 경기 받는 중, 처음 홈, 남은 경기, 기타 칩, 저장된 경기 없음, 라이벌 대결, S1-a, 떨어진 주) in Figma "화면" 페이지의 "홈 (S1)"·"홈 상태" 섹션
- [x] T089 [P] [US1] 짚을 점과 돌아보기가 "추천"·"쓰세요"를 쓰지 않는지 보는 화면 테스트(FR-015)와 짚을 점 고르기 테스트(FR-038) in shared/feature/report/src/iosTest/kotlin/com/ovalit/feature/report/ReportScreenTest.kt, shared/core/model/src/commonTest/kotlin/com/ovalit/core/model/WeekNoteTest.kt

### 남은 일: 문서와 디자인

- [x] T029 [US1] 홈 순서 어긋남 정리: `docs/screens.md` 홈 (S1)은 "큐 칩, ㅇㅂㅇ 카드, 새 경기 진행 줄"인데 `ReportScreen.kt`는 진행 줄을 칩 바로 밑, ㅇㅂㅇ 카드를 카드 목록 맨 위에 둔다. 사용자에게 어느 쪽이 맞는지 묻고 다른 쪽을 고친다(2026-10-30 앱 순서로 정해 문서를 고쳤다) in docs/screens.md, shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/ReportScreen.kt, research.md 열린 결정 표
- [x] T030 [P] [US1] Figma 온보딩 S0-1 인트로(로고, "오늘 발로란트 할 사람 있어?", 영문 비공식 고지), S0-2 연동 동의, S0-4 불러오는 중(관심사 고르기, 50칸, 멈춤과 "이어서 불러오기", 알림 약속 유무)을 라이트·다크와 화면 설명으로 in Figma "화면" 페이지의 "온보딩 (S0)" 줄 (2026-11-01: 본 화면 라이트·다크와 상태 5장(막 시작, 다 불러옴, 받다 멈춤, 8주 경기 없음, 알림 권한 없음)을 벡터로 그렸다)
- [x] T031 [P] [US1] Figma 홈 나머지 상태: 기타 칩 리포트(K/D, KDA, 헤드샷만), 달라진 점 시트, 8주 흐름 시트(막대를 끈 상태 포함), "이번 주는 큰 변화 없음", 짚을 점의 비중 줄 in Figma "화면" 페이지의 "홈 상태" 섹션 (2026-11-01: 8주 흐름 시트 두 장, 기타 칩 리포트, 큰 변화 없음, 달라진 점 시트, 비중 줄을 "홈 상태" 둘째 줄에 벡터로 그렸다)
- [x] T085 [US1] 로그인 풀림 화면과 점검 안내 줄의 자리와 문구를 정한다. 심사 목업과 `docs/screens.md`에 없는 화면이라 사용자에게 먼저 묻고 `docs/screens.md`에 절을 더한 뒤 Figma에 그린다. 2026-10-30에 칩 밑 한 줄로 정해 `docs/screens.md` 절을 더했고 Figma가 남았다. T040이 이 결과를 따른다 in docs/screens.md, docs/DECISIONS.md, Figma "화면" 페이지의 "로그인 풀림과 점검 안내" 줄 (2026-11-01: "홈 상태" 셋째 줄에 로그인 풀림 줄과 점검 안내 줄을 그렸다)

### 남은 일: 실제 연동과 저장(프로덕션 키 승인 뒤)

- [ ] T032 [US1] 로컬 DB 고르기: KMP를 지원하고 상업 이용이 무료인 라이선스(Apache-2.0·MIT·SIL OFL)인 후보를 실제 응답 크기로 견줘 사용자에게 묻고 결정을 남긴다 in docs/DECISIONS.md, CLAUDE.md 아키텍처, specs/001-ovalit-baseline/research.md
- [ ] T033 [US1] `MatchStore`를 고른 로컬 DB로 구현해 경기, 첫 수집 진행도와 멈춘 까닭(`ImportProgress.stoppedBy`), 마지막 확인 시각, 받다 남은 새 경기를 기기에 남긴다. "앱을 강제로 닫아도 남은 것만 이어 받는다", FR-036 "인터넷이 없어도 기기에 저장된 경기로 주간 리포트, 경기 목록, 내 프로필을 보여 줘야" 한다 in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/MatchStore.kt와 새 구현 파일
- [ ] T034 [US1] `MatchStore` 구현 테스트(다시 켜면 이어 받기, 지우기 뒤 더 저장하지 않기, 인터넷 없이 다시 켜도 저장된 경기로 리포트가 나오고 당기면 인터넷 끊김 까닭을 던지기) in shared/core/data/src/commonTest/kotlin/com/ovalit/core/data/
- [x] T035 [US1] HTTP 클라이언트(Ktor)를 붙이고 본문은 문자열로 받아 직접 읽는다(`ContentNegotiation` 금지, 실패 예외에 원문을 담지 않기), 세션은 `Authorization: Bearer`, 에러는 [contracts/server-api.md](contracts/server-api.md)대로 `OvalitError`로 옮긴다 in shared/core/network/
- [x] T082 [US1] 실패 문구 유출 테스트: HTTP 실패와 읽기 실패를 `OvalitError`로 옮긴 뒤 예외 문구와 원인 예외에 응답 본문(Ktor `Text:` 등)과 요청 URL의 PUUID·경기 ID(`/friends/{puuid}/…`, `/riot/matches/{matchId}`)가 남지 않는지 본다. 이 예외가 Crashlytics로 나가면 Riot이 준 값이 함께 나간다(헌법 IV, FR-032, SC-007) in shared/core/network/src/commonTest/kotlin/com/ovalit/core/network/
- [ ] T036 [US1] `MatchRemoteSource` 실제 구현(`GET /riot/matchlist`, `GET /riot/matches/:matchId`, 사용자 한도 429는 새 경기가 없는 것처럼) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/
- [ ] T037 [US1] RSO 연결: S0-3을 Custom Tabs로 띄우고 PKCE verifier를 만들어 challenge만 넘기며, App Link `/auth/done?code=`를 받아 `POST /auth/session`으로 세션을 받는다. WebView 금지, 커스텀 스킴 금지 in shared/feature/onboarding/, composeApp/src/main/AndroidManifest.xml(호스트는 `local.properties`의 `ovalit.server.host`)
- [ ] T038 [US1] 실제 `AccountRepository`: 계정 값은 기기에서 읽은 뒤 처음 내보낸다(읽기 전에 `null`을 먼저 내보내지 않기), 로그인이 풀리면 다시 시도하지 않고 멈춘다 in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/AccountRepository.kt와 새 구현 파일
- [ ] T039 [US1] 시작 화면을 연동과 첫 수집 상태로 고른다(연동 전 인트로, 첫 수집 중 S0-4에서 `start()`로 이어 받기, 나머지 홈) in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt
- [ ] T040 [US1] 로그인 풀림 줄과 점검 안내 줄(`GET /status`). 자리와 문구는 T085에서 정한 `docs/screens.md` 절을 따른다 in shared/feature/report/, shared/core/ui/
- [ ] T041 [US1] "아직 확인하지 못한 것"(맵 ID 형태, `roundNum` 시작, 항복 라운드, 튕긴 사람, 라운드 없는 모드, 건틀릿 큐 ID, 킬의 `damageType`)을 실제 응답으로 확인하고 옮겨 담기와 테스트를 고친다 in shared/core/network/src/commonMain/kotlin/com/ovalit/core/network/MatchMapper.kt, shared/core/network/src/commonTest/, CLAUDE.md 데이터
- [ ] T042 [US1] 전투 점수 칸 결정: 실제 경기 응답에 `players[].stats.score`와 Performance Score가 어떻게 오는지 보고 사용자에게 묻는다. 바뀌면 고정 칸(`FixedMetric`), 문서, Figma를 같은 커밋에서 고친다 in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/FixedMetric.kt, CLAUDE.md 지켜야 할 선, .specify/memory/constitution.md, docs/DECISIONS.md

**Checkpoint**: 실제 계정으로 P1 독립 검사가 통과하고 SC-001, SC-005, SC-006을 잰다.

---

## Phase 4: User Story 2 - 지난 경기를 돌아본다 (Priority: P2)

**Goal**: 날짜별 경기 목록과 경기 상세(스코어보드, 라운드, 내 기록)로 리포트 숫자의 출처를 확인한다.

**Independent Test**: 경기 탭에서 한 경기를 열어 세 탭의 숫자가 그 경기 결과와 맞는지 본다([quickstart.md](quickstart.md) P2).

### 만든 것

- [x] T043 [P] [US2] 경기 목록 S2(날짜 묶음과 그날 승패, 요원·맵 거르기, 당겨서 새 경기와 결과 토스트) in shared/feature/match/src/commonMain/kotlin/com/ovalit/feature/match/MatchesScreen.kt, MatchesViewModel.kt, shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/NewMatchCount.kt
- [x] T044 [P] [US2] 경기 상세 S3(스코어보드, 라운드 탭 구매 유형별 승패, 내 기록) in shared/feature/match/src/commonMain/kotlin/com/ovalit/feature/match/MatchDetailScreen.kt, MatchDetailParts.kt, MatchRounds.kt, MatchReport.kt

### 남은 일

- [x] T045 [P] [US2] Figma S2 경기 목록(날짜 머리, 경기 줄 칩 MVP·팀 MVP·에이스·클러치 톤온톤, 등수로 끝나는 경기 줄, 거르기 시트, 진행 줄 상태)을 라이트·다크와 화면 설명으로 in Figma "화면" 페이지의 "경기 (S2)" 줄
- [x] T046 [P] [US2] Figma S3 경기 상세 세 탭(스코어보드 친구 표시, 라운드 막대는 이긴 라운드만 pos, 내 기록)과 상태(라운드 없는 모드, 스켈레톤) in Figma "화면" 페이지의 "경기 상세 (S3)" 줄 (2026-11-01: 세 탭 라이트·다크와 상태 6장(펼친 줄, 친구 아닌 사람 시트, 친구 요청 시트, 데스매치, 건틀릿, 스켈레톤)을 벡터로 그렸다)
- [ ] T090 [US2] S3 스코어보드에서 사람을 눌러 펼치는 기록(지금은 피해량, 헤드샷, 첫 킬, 첫 데스, 멀티킬)을 실제 경기 응답을 본 뒤 늘리고 순서를 다시 정한다(사용자 요청 2026-10-31). 후보는 트레이드(해 준 것과 받은 것), 관여율, 클러치·에이스, 응답에 오면 Performance Score다. 순서 초안은 게임 스코어보드와 전적 앱(tracker.gg 열 순서, Blitz 내 줄)을 따라 점수와 K/D/A → 피해량·헤드샷 → 첫 킬·첫 데스 → 트레이드·관여율 → 멀티킬·클러치다. op.gg OP Score, tracker TRS 같은 종합 점수와 분포 등수는 가져오지 않는다. T041·T042 뒤 in shared/feature/match/src/commonMain/kotlin/com/ovalit/feature/match/MatchDetailParts.kt, docs/screens.md S3

**Checkpoint**: P2 독립 검사가 통과한다.

---

## Phase 5: User Story 3 - 내 기록을 모아 본다 (Priority: P3)

**Goal**: 내 프로필에서 이번 액트 티어, 승률, 통계, 맞힌 부위, 요원, 무기, 최근 경기를 보고 S6·S7로 들어간다.

**Independent Test**: 홈 아바타로 내 프로필을 열고 요원·무기 화면의 값을 이번 액트 경기로 센 값과 견준다([quickstart.md](quickstart.md) P3).

### 만든 것

- [x] T047 [P] [US3] 내 프로필(티어 카드, 통계, 맞힌 부위, 요원, 무기, 최근 경기) in shared/feature/profile/src/commonMain/kotlin/com/ovalit/feature/profile/ProfileScreen.kt, ProfileViewModel.kt, ProfileSections.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/ProfileSections.kt
- [x] T048 [P] [US3] S6 무기, S7 요원 in shared/feature/profile/src/commonMain/kotlin/com/ovalit/feature/profile/WeaponsScreen.kt, AgentsScreen.kt, RecordsViewModel.kt
- [x] T086 [P] [US3] KDA·승률 구간 색(FR-020: KDA 2~3 `--stat-high`, 3 이상 `--stat-top`, 승률 55~60%·60% 이상, 승패가 갈린 판이 5판보다 적으면 칠하지 않기)과 테스트 in shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/ProfileParts.kt(`kdaColor`, `winRateColor`), shared/core/ui/src/commonTest/kotlin/com/ovalit/core/ui/KdaTierTest.kt

### 남은 일

- [x] T049 [P] [US3] Figma 내 프로필, S6, S7(목업에 없는 화면이라 `docs/screens.md`만 근거로, 배너는 카드 그림 전까지 fill 면) in Figma "화면" 페이지의 "프로필 (내 프로필, S6, S7)" 줄. 내 프로필 라이트·다크와 번호 9개, 상태 3장(경쟁 칩, 고른 큐에 경기 없음, S5 큐 칩)은 2026-10-31에 "내 프로필"·"내 프로필 상태" 섹션에 그렸고 S6, S7이 남았다. S6, S7은 프로필 칩으로 고른 큐를 제목 줄에 적은 모양("경쟁 · 이번 액트 34경기")도 그린다
- [ ] T050 [US3] 실제 서버 연결 뒤 플레이어 카드를 서버 정적 에셋(`/cards/{uuid}_small|wide.png`)에서 받아 아바타와 프로필 배너에 쓴다. 받기 전에는 Riot ID 첫 글자 in shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/
- [x] T091 [US3] 내 프로필과 S5에 큐 칩(경쟁 + 일반 · 경쟁 · 일반, 기본 경쟁 + 일반)을 두고 통계, 맞힌 부위, 요원, 무기(S5는 나와 비교도)가 칩을 따르게 한다. 그 카드로 연 S7, S6도 고른 큐로 세고 제목 줄에 큐를 적는다. 홈 칩도 S7, S6에 넘긴다. 프로토타입은 Figma "프로토타입 · S6·S7 큐 이어받기"다. 티어 카드, 같이 뛴 경기, 최근 경기는 따르지 않는다(사용자 결정 2026-10-31). 칩 자리는 티어 카드 밑(B안, 사용자 결정 2026-10-31)이고 프로토타입은 Figma "화면" 페이지 "프로토타입 · 프로필 큐 칩"(node 54:1890), 디자인은 "내 프로필"·"내 프로필 상태" 섹션이다 in shared/feature/profile/src/commonMain/kotlin/com/ovalit/feature/profile/ProfileScreen.kt, ProfileViewModel.kt, shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/FriendProfileScreen.kt, docs/screens.md 내 프로필·S5

**Checkpoint**: P3 독립 검사가 통과한다.

---

## Phase 6: User Story 4 - 같이 뛴 사람과 친구를 맺고 견준다 (Priority: P4)

**Goal**: 스코어보드나 초대 링크로 친구를 맺고 서로 수락한 친구끼리만 프로필, 라이벌 대결, 친구 비교를 본다.

**Independent Test**: 두 계정이 같이 뛴 경기에서 요청과 수락 뒤 서로의 프로필과 라이벌 칸이 보이는지 확인한다([quickstart.md](quickstart.md) P4).

### 만든 것

- [x] T051 [P] [US4] 서버 친구·초대(같이 뛴 경기 확인, 200명, 하루 30번, 링크당 20개, 내가 안 뛴 경기 가리기) in server/src/routes/friends.ts, routes/invites.ts, relations.ts, redact.ts
- [x] T052 [P] [US4] 친구 탭, S5 플레이어 프로필, 친구 경기 목록 in shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/FriendsScreen.kt, FriendProfileScreen.kt, FriendMatchesScreen.kt, FriendsViewModel.kt, FriendProfileViewModel.kt
- [x] T053 [P] [US4] 홈 라이벌 대결, 유도 칸, 친구 비교와 전체 순위 in shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/component/SocialSections.kt, NudgeSection.kt, shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/FriendRankingScreen.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/HeadToHeadRow.kt

### 남은 일

- [x] T054 [US4] 주간 요약 서버: 표(주, 액트, 큐, 합계: 경기·라운드 수, 킬·데스·어시, 전투 점수, 피해량, 맞힌 부위별 탄)와 올리기·받기 경로. "세션이 있어야 받고, 서로 수락한 친구가 전적을 공개했을 때만 내려준다". 사람마다 주당 한 줄을 덮어쓰고, 친구를 끊으면 지우지 않고 내주지만 않으며, 연동을 해제하면 지운다([research.md](research.md) 친구 비교 요약, 2026-10-30) in server/migrations/0011_weekly_summaries.sql, server/src/routes/, contracts/server-api.md, docs/backend.md 친구 비교 요약
- [x] T055 [US4] 주간 요약 서버 테스트(세션 없음 401, 친구 아님·비공개 차단, 끊기·해제 뒤 삭제, 비율이 아니라 합계만 받기) in server/test/summaries.test.ts
- [ ] T056 [US4] 실제 `FriendRepository`(`/friends`, `/friends/requests`, 수락·거절·끊기, 초대 링크 `POST /invites`와 `redeem`) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/FriendRepository.kt와 새 구현 파일
- [ ] T057 [US4] 친구 경기를 기기에 저장하고 S5를 열면 저장된 것을 바로 보여 준 뒤 10분이 지났을 때만 새 경기를 받는다. "마지막 갱신 3분 전", 같은 친구 당기기는 1분에 한 번 in shared/core/data/, shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/FriendProfileScreen.kt
- [ ] T058 [US4] 앱이 새 경기를 받을 때마다 그 경기가 든 주의 고정 칸 합계를 올리고(첫 수집 뒤에는 받은 주마다), 친구 비교와 라이벌 대결을 친구 경기 대신 요약으로 센다(기간을 넓힐 때 더한 뒤 나누기) in shared/core/data/, shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/ReportViewModel.kt
- [x] T059 [US4] 친구 요청·수락 푸시 서버: 요청을 새로 만들 때(스코어보드, 초대 링크) 받는 사람에게, 수락할 때 보낸 사람에게 알린다. 다른 푸시처럼 종류와 이름만 담고, 이미 보내 둔 요청을 다시 보내거나 이미 친구인 경우는 알리지 않는다. 신청서에 푸시 알림이 없어서 같은 커밋에서 `docs/RELEASE.md` "신청서와 다른 것" 표에 친구 요청 푸시를 한 줄 더한다 in server/src/routes/friends.ts, server/src/routes/invites.ts, server/src/push.ts, server/README.md, docs/backend.md ㅇㅂㅇ과 푸시, specs/001-ovalit-baseline/contracts/push.md, docs/RELEASE.md
- [x] T060 [US4] 친구 요청·수락 푸시 테스트(요청·수락마다 한 번, 글 없이 종류와 이름만, 다시 보낸 요청과 이미 친구는 알리지 않기) in server/test/friends.test.ts, server/test/invites.test.ts
- [x] T061 [US4] 앱의 친구 요청 알림: 설정에 "친구 요청 알림" 스위치(기본 켬, 기기 설정 `UserPreferences`에 `notifyPing`과 같은 방식), 알림 채널과 문구(한국어·일본어), 누르면 친구 탭. 스위치 자리는 [research.md](research.md) 열린 결정 "친구 요청 알림 스위치 자리"를 따른다. T059가 `docs/RELEASE.md` 표에 더한 줄에 설정 스위치를 같이 적는다 in shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/UserPreferences.kt, shared/feature/settings/src/commonMain/kotlin/com/ovalit/feature/settings/SettingsScreen.kt, composeApp/src/main/kotlin/com/ovalit/push/, docs/screens.md 설정 (S4), docs/RELEASE.md
- [x] T062 [P] [US4] Figma 친구 탭(친구 목록 슬라이스, 받은 요청, 친구 요청 알림 모양), S5(전적 비공개 상태 포함), 친구 요청 시트와 초대 링크 시트, 라이벌 고르기 시트, 친구 비교 전체 순위 in Figma "화면" 페이지의 "친구 (탭, S5)" 줄 (2026-11-01: 친구 탭, S5, 상태 4장(비공개, 친구 끊기, 친구 경기 목록, 라이벌 고르기), 친구 비교 전체 순위, S3의 친구 요청 시트를 벡터로 그렸다. 초대 링크는 시스템 공유 창이라 그리지 않는다)

**Checkpoint**: P4 독립 검사가 실제 계정 둘로 통과한다.

---

## Phase 7: User Story 5 - 친구를 불러 같이 한다 (Priority: P5)

**Goal**: 친구를 4명까지 골라 시각을 정해 부르고 정해 둔 답만 오가며 시작 전에 알림을 받는다.

**Independent Test**: 친구 둘을 불러 받은 쪽이 다른 시간으로 답하고 부른 쪽이 받아들이는지 확인한다([quickstart.md](quickstart.md) P5).

### 만든 것

- [x] T063 [P] [US5] 서버 ㅇㅂㅇ(4명, 24시간 안, 하루 10번, 시간 바꾸면 다시 묻기)과 푸시·크론 in server/src/routes/pings.ts, push.ts, scheduled.ts
- [x] T064 [P] [US5] ㅇㅂㅇ 화면과 홈·친구 탭 카드 in shared/feature/friend/src/commonMain/kotlin/com/ovalit/feature/friend/PingDetailScreen.kt, PingDetailViewModel.kt, PingSection.kt, shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/PingRow.kt
- [x] T065 [P] [US5] 푸시 받기와 알림(문구는 앱이 만든다) in composeApp/src/main/kotlin/com/ovalit/push/OvalitMessagingService.kt, PingNotifications.kt, PingActionReceiver.kt, OvalitPush.kt

### 남은 일

- [ ] T066 [US5] 실제 `PingRepository`와 `PushRepository`(`/pings` 경로, `PUT /me/push-token`, 로그아웃 전 `DELETE /me/push-token`) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/PingRepository.kt, PushRepository.kt와 새 구현 파일
- [x] T067 [P] [US5] Figma 오발있? 카드(친구 탭), 부르기 시트(고른 칸 accent 테두리와 체크, 옆으로 미는 친구 줄), 초대 화면(받은 쪽·보낸 쪽, 다른 시간 제안을 맨 위에), 30분 단위 휠, 알림 모양 in Figma "화면" 페이지의 "오발있? (파티 모집)" 줄 (2026-11-01: 받은 초대 라이트·다크와 상태 5장(부르기 시트, 보낸 초대, 다른 시간, 답한 뒤, 카드에 초대 둘), 알림 모양을 벡터로 그렸다)

**Checkpoint**: P5 독립 검사가 실제 계정 둘로 통과한다.

---

## Phase 8: User Story 6 - 내 데이터와 공개 범위를 관리한다 (Priority: P6)

**Goal**: 전적 공개, 알림, 관심사, 기본 큐를 바꾸고 저장된 데이터를 지우거나 연동을 해제한다.

**Independent Test**: 전적 공개를 끈 뒤 친구 쪽에서 내 경기가 안 보이는지, 연동 해제 뒤 서버에 계정·세션·친구·요청·초대·ㅇㅂㅇ·기기 토큰이 남지 않는지 본다. 경기 참가자 기록(10주)과 경기 상세 캐시(30일)는 FR-030의 기간을 따른다([quickstart.md](quickstart.md) P6).

### 만든 것

- [x] T068 [P] [US6] 설정 S4(전적 공개, 알림, 관심사, 기본 큐, 저장된 데이터 지우기, 연동 해제, 광고 없이 보기) in shared/feature/settings/src/commonMain/kotlin/com/ovalit/feature/settings/SettingsScreen.kt, SettingsSheets.kt, SettingsViewModel.kt, NotificationBlocks.kt
- [x] T069 [P] [US6] 서버 계정(`GET/PATCH/DELETE /me`, 해제 때 같이 지우기) in server/src/routes/me.ts, users.ts
- [x] T070 [P] [US6] Riot 비공식 고지(영문 원문) in shared/core/designsystem/src/commonMain/kotlin/com/ovalit/core/designsystem/component/OvalitDisclaimer.kt
- [x] T071 [P] [US6] 앱을 지운 사람의 삭제 요청(메일)과 이용약관 in site/privacy/index.html, site/terms/index.html

### 남은 일

- [ ] T072 [US6] 실제 `AccountSettingsRepository`(`PATCH /me {statsPublic?, remindBefore?}`, 서버 값을 따른다)와 연동 해제(`DELETE /me` 뒤 기기 기록과 보내지 않은 보고 지우기) in shared/core/data/src/commonMain/kotlin/com/ovalit/core/data/AccountSettingsRepository.kt, AccountSettingsSync.kt와 새 구현 파일
- [x] T073 [P] [US6] Figma S4 설정(묶음 카드, 오발있? 카드의 파티 모집 알림과 시작 전 알림, 친구 요청 알림 스위치, 연동 해제 위험 버튼, 맨 아래 영문 고지). 친구 요청 알림 스위치는 알림 카드에 둔다([research.md](research.md) 친구 요청 알림 스위치) in Figma "화면" 페이지의 "설정 (S4)" 줄 (2026-11-01: 시트 5장(테마, 관심사, 시작 전 알림, 데이터 삭제, 연동 해제)도 벡터로 바꿨다)

**Checkpoint**: 모든 이야기가 따로 돌아간다.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 출시 전 공통 일

- [ ] T081 승인 직후 `docs/RELEASE.md`의 "신청서와 다른 것" 표를 개발자 포털 제품 설명에 한 번에 적고 그 뒤에 실제 광고 단위 ID를 넣는다. 승인 전에는 신청서와 제품 설명을 고치지 않는다(헌법 V, "문서와 결정") in docs/RELEASE.md, local.properties(`admob.appId`, `admob.nativeUnitId`, `admob.rewardedUnitId`)
- [ ] T074 광고 동의 창(UMP)을 붙인다. 출시 전에 하고 T081 뒤에 둔다 in composeApp/src/main/kotlin/com/ovalit/ads/, docs/RELEASE.md
- [ ] T075 [P] 새로 넣는 문구는 같은 커밋에서 일본어도 고친다 in shared/**/composeResources/values-ja/strings.xml, docs/i18n-ja.md
- [x] T083 [P] 서버 콘텐츠의 언어: `GET /content`와 `GET /content/tiers`가 `locale`(`ko-KR`, `ja-JP`)을 받아 언어마다 따로 담아 두고 티어 표에 일본어 이름을 더한다. 앱은 `AppLanguage.current`를 따라 다시 받는다(FR-035). [contracts/server-api.md](contracts/server-api.md)의 계획을 계약으로 옮기고 `docs/i18n-ja.md` "아직 한국어인 것"에서 뺀다 in server/src/routes/content.ts, server/src/riot.ts, server/src/data/tiers.json, server/scripts/build-tables.mjs, server/test/content.test.ts, server/README.md, specs/001-ovalit-baseline/contracts/server-api.md, docs/i18n-ja.md
- [x] T084 [P] 고정 화면 이름 표 테스트: 경로 키에 친구 PUUID나 경기 ID가 들어간 화면도 보내는 화면 이름은 고정 표의 값뿐인지 본다(`screenName`, 헌법 IV, SC-007). 이 테스트가 없으면 SC-007은 출시 뒤 T078에서야 잰다 in shared/app/src/commonMain/kotlin/com/ovalit/app/OvalitApp.kt, shared/app/src/commonTest/kotlin/com/ovalit/app/
- [ ] T076 [P] 접근성 점검(FR-037): 가장 큰 글꼴과 좁은 폭(320dp)에서 글자가 겹치거나 잘리지 않는지, 승패와 증감이 색만이 아니라 글자로도 보이는지, TalkBack이 지표 값과 승패를 읽는지 본다. Figma 화면 줄마다 큰 글꼴 상태를 한 장씩 더한다 in shared/feature/, Figma "화면" 페이지의 각 화면 줄. 앱의 2배 글꼴·320dp 점검은 2026-10-31에 했다(S3 스코어보드 숫자 열 내리기, 설정·광고 줄의 값 내리기, 동적 칸 이름 줄 수, 칩 잘림, 초대 제목 두 줄). TalkBack 점검과 Figma 큰 글꼴 상태가 남았다
- [ ] T077 [P] Figma 상태 화면 다크판(지금은 라이트만) in Figma "화면" 페이지의 각 화면 줄의 상태 섹션
- [ ] T078 실제 계정으로 [quickstart.md](quickstart.md) 전체를 돌고 SC-001(첫 수집 1분 안팎, Riot 붐빔 없이 받은 경우), SC-007(통계·오류 보고에 Riot 값 0건), SC-008(Cloudflare 사용량이 무료 한도 안이고 카드 등록이 없다)을 잰다. 잰 값으로 [research.md](research.md) 열린 결정 "첫 수집 시간 상한"을 닫는다 in specs/001-ovalit-baseline/quickstart.md, specs/001-ovalit-baseline/spec.md, research.md
- [ ] T079 열린 결정을 닫을 때마다 결정을 남기고 스펙과 규칙 문서를 같은 커밋에서 맞춘다 in docs/DECISIONS.md, specs/001-ovalit-baseline/research.md, spec.md
- [x] T092 [P] 시작 시간과 홈 프레임을 재는 Macrobenchmark 모듈과 앱 Baseline Profile(시작 프로필 포함)을 더한다. 플러그인이 만드는 `benchmarkRelease`로 `benchmark` 빌드 타입을 대신한다(2026-11-01) in benchmarks/, composeApp/build.gradle.kts, composeApp/src/main/generated/baselineProfiles/, build-logic/convention/src/main/kotlin/OvalitAndroidTestConventionPlugin.kt, docs/CONVENTIONS.md
- [x] T093 처음 홈의 프로필 말풍선을 칩 위에 띄우지 않고 머리 줄 밑에 끼운다(Figma "프로토타입 · 프로필 말풍선 자리" A안, 2026-11-01) in shared/feature/report/src/commonMain/kotlin/com/ovalit/feature/report/component/ReportHeader.kt, ProfileHint.kt, ReportScreen.kt, shared/feature/report/src/iosTest/kotlin/com/ovalit/feature/report/ReportScreenTest.kt, docs/screens.md
- [ ] T080 Figma에서 화면이 확정되면 바로 앱에 반영한다. 지금 앱과 달라진 곳은 그 화면 모듈과 `docs/screens.md`·`docs/design.md`를 같은 커밋에서 고치고, 심사 목업에서 벗어나는 부분은 반영 전에 사용자에게 묻고 `docs/DECISIONS.md`에 남긴다(spec.md Clarifications 2026-10-30). 신청서와 달라지는 기능이면 `docs/RELEASE.md` "신청서와 다른 것" 표에 한 줄 더한다 in shared/feature/, docs/screens.md, docs/design.md, docs/RELEASE.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup, Foundational**: 끝났다(T019만 남음, 다른 일을 막지 않는다).
- **Figma 작업**(T030, T031, T045, T046, T049, T062, T067, T077): 지금 할 수 있고 서로 기대지 않는다. 모두 "화면" 페이지에 줄을 더하는 일이고 새 페이지를 만들지 않는다. T085(로그인 풀림과 점검 안내)는 자리와 문구를 정했고 그리기만 남았다.
- **실제 서버 연결**: Riot 프로덕션 키 승인이 먼저다. 그 안에서 T035(HTTP 클라이언트)가 T082, T036, T037, T038, T056, T066, T072보다 먼저다. T040은 T085 뒤다.
- **로컬 DB**: T032(결정) → T033 → T034. T039(시작 화면)는 T033과 T038 뒤다.
- **주간 요약**: T054 → T055, T058. T058은 T056 뒤다.
- **전투 점수 칸**(T042)과 응답 확인(T041)은 실제 응답을 받은 뒤다.
- **제품 설명과 광고**: 승인 전에는 신청서와 제품 설명을 고치지 않는다. 승인 직후 T081 → T074. T059와 T080은 신청서와 달라지는 기능을 `docs/RELEASE.md` 표에 쌓아 두고 T081이 그 표를 한 번에 옮긴다.
- **서버 콘텐츠 언어**(T083)는 로컬 서버로 지금 할 수 있고 실제 서버를 붙이기 전에 끝낸다.

### User Story Dependencies

- **US1**: 다른 이야기에 기대지 않는다. 실제 연동(T035~T040)이 US3~US6의 실제 저장소 작업보다 먼저다.
- **US2, US3**: US1의 실제 경기 받기(T036)만 있으면 된다.
- **US4**: 실제 서버 연결(T035) 뒤. 주간 요약(T054)은 US4 안에서 닫힌다. 친구 요청 푸시 서버(T059, T060)는 로컬 서버로 지금 할 수 있고 앱 쪽(T061)은 실제 친구 저장소(T056) 뒤다.
- **US5**: 실제 친구 저장소(T056) 뒤.
- **US6**: 실제 계정 저장소(T038) 뒤.

### Parallel Opportunities

- Figma 화면 작업은 모두 [P]다. 한 세션에서 한 화면씩 그리고 사용자에게 보여 준 뒤 다음으로 간다.
- 실제 서버 연결 뒤 T056(친구), T066(ㅇㅂㅇ·푸시), T072(설정)는 서로 다른 저장소라 같이 할 수 있다.

---

## Parallel Example: 프로덕션 키 승인 전에 할 수 있는 것

```text
Task: "T029 홈 순서 어긋남 정리(사용자에게 묻기)"
Task: "T030 Figma '화면' 페이지의 '온보딩 (S0)' 줄"
Task: "T045 Figma '화면' 페이지의 '경기 (S2)' 줄"
Task: "T049 Figma '화면' 페이지의 '프로필 (내 프로필, S6, S7)' 줄"
Task: "T054 주간 요약 서버 표와 경로(열린 결정을 닫은 뒤, 서버는 로컬 D1로 돈다)"
Task: "T083 서버 콘텐츠 언어(로컬 서버)"
```

## Parallel Example: 승인 뒤 저장소 붙이기

```text
Task: "T056 실제 FriendRepository"
Task: "T066 실제 PingRepository와 PushRepository"
Task: "T072 실제 AccountSettingsRepository와 연동 해제"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. 승인 전: T029(문서·코드 맞추기), T030·T031(Figma 온보딩과 홈 나머지), T085(로그인 풀림과 점검 안내 묻기), T032 후보 정리.
2. 승인 뒤: T035 → T082 → T036 → T037 → T038 → T033 → T039 → T040 순서로 실제 연동을 붙인다.
3. **멈추고 확인**: 실제 계정으로 P1 독립 검사, SC-001·SC-005·SC-006.
4. T041·T042로 응답 모양과 전투 점수 칸을 닫는다.

### Incremental Delivery

1. US1 실제 연동 → 출시 가능한 최소 앱(혼자 쓰는 리포트).
2. US2·US3은 저장소 교체만으로 따라온다.
3. US4(친구와 주간 요약) → US5(ㅇㅂㅇ) → US6(설정의 서버 연결).
4. 승인 직후 T081(제품 설명과 광고 단위 ID), 출시 전 T074(UMP)와 T078(실측).

---

## Notes

- `[x]` 작업은 기준선 기록이다. 다시 하지 않는다.
- T081~T089는 2026-10-30 분석 뒤에 더해서 번호가 단계 순서와 다르다. T081~T085는 남은 일이고 T086~T089는 이미 있던 것을 기록한 `[x]`다.
- 테스트 작업이 없는 요구사항: FR-014(종합 점수·남과의 순위 없음)는 지표를 기기에서 내 경기로만 세고 서버에 분포·순위 경로가 없는 것으로 지키고 plan.md의 Constitution Check가 확인한다. SC-008(서버 비용 0원)은 T078에서 잰다.
- 각 작업이 규칙을 바꾸면 `CLAUDE.md`, `docs/screens.md`, `docs/design.md`, `docs/backend.md` 중 그 규칙이 있는 문서를 같은 커밋에서 고친다.
- 심사 목업(아티팩트, `site/index.html`, `site/riot.txt`)은 고치지 않는다. 새 디자인이 목업에서 벗어나면 먼저 묻고 `docs/DECISIONS.md`에 남긴다.
