# Implementation Plan: 오발있 기준 스펙

**Branch**: `001-ovalit-baseline` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-ovalit-baseline/spec.md`

**Note**: 이 계획은 새로 만들 것을 고르는 계획이 아니다. 이미 만든 앱과 서버가 쓰는 기술과 구조를 스펙에 맞춰 옮겨 적은 기준
계획이다. 결정의 원본은 `CLAUDE.md`, `docs/backend.md`, `docs/CONVENTIONS.md`이다. 여기와 어긋나면 어느 쪽이 맞는지 정하고 같은
커밋에서 다른 쪽을 고친다(헌법 "문서와 결정"). 아직 정하지 않은 것은 [research.md](research.md)의 "열린 결정"에 정하는 조건을
붙여 둔다.

## Summary

발로란트 경기를 Riot 계정 연동(RSO)으로 불러와 주마다 고정 지표 다섯과 움직인 지표를 지난 4주의 나와 견주어 보여 주고, 서로
수락한 친구와 견주고 불러 같이 게임하게 하는 안드로이드 앱이다. 앱은 Compose Multiplatform 한 벌로 안드로이드와 iOS 시뮬레이터를
같이 그리고, 지표는 모두 기기에서 센다. 서버는 Cloudflare Worker 하나로 RSO 비밀값 교환, Riot API 대리 호출과 레이트 리밋,
친구·초대·파티 모집(ㅇㅂㅇ)과 푸시를 맡는다. 앱은 서버가 준 세션 토큰만 든다. 지금 앱은 가짜 서버와 메모리 저장으로 돌고,
프로덕션 키가 나오면 두 인터페이스(`MatchRemoteSource`, `MatchStore`)의 구현만 바꾼다.

## Technical Context

**Language/Version**: Kotlin 2.4.20, Compose Multiplatform 1.12.0(material3 1.9.0), JVM 17 / 서버 TypeScript 7(Cloudflare
Workers 런타임, 도구는 Node 22 이상)

**Primary Dependencies**:

- 앱: Koin 4.2.2(DI. Hilt는 KMP를 지원하지 않는다), Navigation 3(런타임 androidx 1.2.0, `NavDisplay`는 JetBrains
  1.2.0-beta01), Lifecycle ViewModel 2.11.0, kotlinx.serialization 1.11.0, kotlinx.coroutines 1.11.0, kotlinx-datetime 0.8.0,
  DataStore 1.2.1. HTTP 클라이언트는 Ktor 3.6.0으로 버전 카탈로그에만 올려 두었고 실제 서버를 붙일 때 `core/network`에 넣는다
- 안드로이드에만: AndroidX Browser 1.10.0(RSO Custom Tabs), WorkManager 2.10.1, Firebase BOM 34.19.0(Analytics, Crashlytics,
  Cloud Messaging), Play Services Ads 25.5.0(AdMob 네이티브)
- 서버: Hono 4.13, Cloudflare D1, Cache API, wrangler 4
- 글꼴: Pretendard Variable(SIL OFL) 한 벌

**Storage**:

- 기기: 기기 설정은 DataStore(`UserPreferencesRepository`). 경기는 지금 메모리(`InMemoryMatchStore`)이고 로컬 DB는 열린 결정이다.
  FR-036(인터넷 없이 저장된 경기로 리포트, 경기 목록, 내 프로필 보기)은 로컬 DB가 붙어야 앱을 다시 켠 뒤에도 채워진다.
- 서버: D1(사용자, 세션, 로그인 코드, 친구 요청, 친구 관계, 초대, 경기 참가자, ㅇㅂㅇ, 기기 토큰, Riot 차단). 경기 상세는 Cache
  API와 isolate 메모리에 30일 담아 두되 거기에 기대지 않는다. 경기 원문은 D1에 두지 않는다. 친구 비교용 주간 요약 표는 아직
  없고 올리는 때, 지우는 대상, D1 쓰기 예산은 열린 결정이다([research.md](research.md)).

**Testing**: `kotlin.test` + Turbine + Compose UI Test. 로직과 ViewModel은 `commonTest`(안드로이드 JVM과 iOS 둘 다), Compose UI는
`iosTest`(iOS 시뮬레이터). 서버는 Vitest + `@cloudflare/vitest-plugin`. 공유 테스트 대역은 `shared/core/testing`.

**Target Platform**: 안드로이드 minSdk 26, compile·targetSdk 37(출시). iOS는 시뮬레이터(arm64)에서 도는 데까지. 서버는
Cloudflare Workers 무료 플랜.

**Project Type**: 모바일 앱(KMP/CMP) + API 서버

**Performance Goals**:

- 첫 수집 50경기를 Riot이 붐비지 않을 때(받는 동안 429가 없을 때) 1분 안팎에 마친다(SC-001). 상한은 실제 계정으로 잰 뒤 정한다.
- 경기를 모아 세는 계산은 `Dispatchers.Default`에서 한다. 첫 수집 뒤 홈으로 넘어갈 때 메인 스레드에서 세면 한 프레임이 250ms까지 걸렸다.
- 긴 화면은 묶음마다 한 프레임씩 나눠 그린다(`OvalitStaged`). 홈을 한 프레임에 다 그리면 릴리스에서 54ms였다. 끊김은 릴리스나
  `benchmark` 빌드로 잰다.
- 서버는 요청당 CPU 10ms 안. 가장 무거운 친구 경기 가리기가 800KB 경기에서 약 3ms다.

**Constraints**:

- Riot 프로덕션 키 승인 조건(헌법 I~V). 어기면 키가 회수된다.
- 비용 0원(헌법 VI). Workers 무료 한도: 요청 하루 10만, 요청당 CPU 10ms, 하위 요청 50, D1 쓰기 하루 10만 행, DB 500MB. 카드
  등록 없이 쓰고 넘으면 막힌다. 피할 수 없는 비용은 Google Play 등록 25달러와 iOS를 낼 때의 Apple Developer Program 연 99달러다.
- Riot 레이트 리밋은 앱 전체가 나눠 쓴다. 끝난 경기는 한 번 받아 저장하면 다시 요청하지 않는다(SC-006).
- 데이터는 한국 샤드(`kr.api.riotgames.com`, account-v1은 `asia`)만 쓴다. 화면은 한국어(기본)와 일본어다.
- 앱은 Riot 비밀번호를 만지지 않는다. RSO는 Custom Tabs로 띄우고 WebView를 쓰지 않는다.

**Scale/Scope**: 첫 수집 최근 50경기·8주, 친구 200명, 새 친구 요청 하루 30번, 초대 링크 하나로 요청 20개(모두 시작 기준선).
화면은 S0-1~S7, 내 프로필, 친구 탭, ㅇㅂㅇ 초대, 친구 비교 전체 순위로 스무 개 남짓이다. Gradle 모듈은 14개다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 이 계획에서 지키는 자리 |
| --- | --- | --- |
| I. 본인의 과거와만 견준다 | 통과 | 지표는 기기에서 내 경기로만 센다(`WeeklyReportCalculator`). 서버에는 분포나 순위를 만드는 경로가 없다. 친구 비교와 라이벌은 서로 수락한 친구의 값만 쓴다. KDA·승률 구간 색은 사용자 결정으로 둔 예외다. |
| II. 게임 결정을 대신하지 않는다 | 통과 | 짚을 점, 돌아보기 문구는 사실만 적는 문자열 리소스로 두고 "추천"이 없다. 짚을 점 근거 줄은 요원이 하나여도 내 기록이 어떻게 움직였는지 적는 줄이라 "요원은 둘 이상일 때만"에 걸리지 않는다(헌법 1.0.3). 상점·스카우팅·오버레이·닉변 이력 경로가 없다. |
| III. 서로 수락한 친구에게만 보인다 | 통과 | 서버에 Riot ID 검색 경로가 없다. 친구 요청은 같이 뛴 경기 확인(`match_players`) 뒤에만 받는다. 남의 경기는 친구이고 공개했을 때만, 내가 안 뛴 경기는 `anon-N`으로 가린다(허용 목록). |
| IV. Riot 데이터와 비밀을 내보내지 않는다 | 통과 | RSO 전에는 전적 경로가 세션 없이 열리지 않는다. 비밀값은 서버에만, 세션·로그인 코드는 해시만. 응답 본문은 문자열로 받아 직접 읽어(`decodeMatch`) 예외 문구에 원문이 담기지 않게 하고, 통계 화면 이름은 고정 표(`screenName`)로 보낸다. 비공식 고지는 인트로와 S4 하단. |
| V. 무료로 다 쓰고, 수익은 승인 뒤에 | 통과 | 광고 단위 ID가 없으면 광고를 요청하지 않는다. 보상형은 "24시간 광고 없이 보기"뿐이고 Riot 데이터를 잠그지 않는다. 심사 중에는 신청서와 제품 설명을 고치지 않고, 승인 직후 `docs/RELEASE.md` "신청서와 다른 것" 표를 제품 설명에 한 번에 적은 뒤 실제 광고 단위 ID를 넣는다(T081). |
| VI. 돈이 드는 것을 쓰지 않는다 | 통과 | Workers·D1 무료 플랜, GitHub Actions 표준 러너(공개 저장소). 라이선스는 Apache-2.0·MIT·SIL OFL과 사용자 결정으로 둔 Google 서비스 셋. |
| VII. 숫자는 정직하게 센다 | 통과 | 총량을 더한 뒤 나누고 분모 0은 비운다. 변화량은 보이는 자릿수로 뺀다. `OfflineFirstMatchRepository`가 끝난 경기를 다시 받지 않는다. 각 규칙은 `core/model`·`core/data` 테스트가 지킨다. |
| 문서와 결정 | 통과 | 이 계획은 규칙 문서를 옮겨 적는다. 어긋나면 어느 쪽이 맞는지 정하고 같은 커밋에서 다른 쪽을 고친다. 심사 목업·신청서는 고치지 않는다. |

Phase 1 설계 뒤 다시 확인: [data-model.md](data-model.md)와 [contracts/](contracts/)는 위 자리를 그대로 옮겼고 새 경로나 새
저장을 더하지 않았다. 통과.

## Project Structure

### Documentation (this feature)

```text
specs/001-ovalit-baseline/
├── spec.md              # 기준 스펙
├── plan.md              # 이 문서
├── research.md          # 기술 결정과 열린 결정
├── data-model.md        # 앱 도메인과 서버 저장
├── quickstart.md        # 사용자 이야기마다 돌려 보는 법
├── contracts/
│   ├── server-api.md    # 앱 ↔ 서버 HTTP 계약
│   └── push.md          # FCM 데이터 메시지 계약
├── checklists/
└── tasks.md             # /speckit-tasks가 만든다
```

디자인은 저장소 밖 Figma 파일 "오발있"에 있다. 무료 플랜은 파일 하나에 페이지가 셋까지라 페이지는 기획 개요, 기초(토큰·글자·
컴포넌트), 화면 셋뿐이다. "화면" 페이지 하나에 화면마다 한 줄씩 아래로 쌓고, 줄마다 라이트·화면 설명·다크를 담은 화면 섹션과
상태 섹션을 나란히 둔다(`docs/DECISIONS.md` 2026-10-30). 값의 원본은 `docs/design.md`와 `OvalitColors`·`OvalitTypography`다.

### Source Code (repository root)

```text
composeApp/                      # 안드로이드 앱 셸. 광고(AdMob), 푸시(FCM), WorkManager, Firebase는 여기에만
shared/
├── app/                         # Navigation 3 뼈대, Koin 모듈 묶기, iOS 실행 파일과 Ovalit.app 번들
├── core/
│   ├── model/                   # 도메인(Match, WeeklyReport, Ping…)과 지표 계산. 플랫폼 의존 없음
│   ├── network/                 # Riot DTO와 MatchMapper(응답 → Match). HTTP 클라이언트는 실제 서버를 붙일 때
│   ├── data/                    # Repository 인터페이스와 구현(OfflineFirstMatchRepository, Fake…)
│   ├── designsystem/            # OvalitTheme 토큰, 글자, 컴포넌트, 아이콘
│   ├── ui/                      # 여러 화면이 같이 쓰는 표기·칸·줄(지표 표기, 프로필 칸, 큐 칩 등)
│   └── testing/                 # 공유 테스트 대역(Test…, No…)과 테스트 시계
└── feature/
    ├── onboarding/              # S0-1~S0-4
    ├── report/                  # S1 홈, S1-a·흐름 시트, 친구 비교 전체 순위
    ├── match/                   # S2, S3
    ├── profile/                 # 내 프로필, S6, S7
    ├── friend/                  # 친구 탭, S5, ㅇㅂㅇ 초대
    └── settings/                # S4
build-logic/convention/          # ovalit.android.application, ovalit.kmp.library, ovalit.kmp.feature
server/
├── src/
│   ├── routes/                  # auth, content, friends, invites, me, pings, public, riot
│   ├── data/                    # 티어 이름, 요원 역할 표(valorant-api.com에서 스크립트로)
│   └── *.ts                     # rso, session, riot(레이트 리밋), redact(가리기), push, scheduled(크론)
├── migrations/                  # D1 0001~0010
└── test/                        # Vitest
site/                            # ovalit.netlify.app. 목업 index.html·riot.txt는 고치지 않는다
tools/                           # bundle_assets.py, check_content.py
```

**Structure Decision**: 모바일 + API 구조다. 앱은 nowinandroid에서 UDF, `sealed interface UiState`,
`stateIn(WhileSubscribed(5_000))`, Repository 분리, convention plugin, `core/testing`을 가져오고 20여 개 모듈 구조와 SyncWorker,
FTS는 버렸다. 기능 모듈은 ViewModel을 받아 그리는 진입 컴포저블까지만 내놓고 화면 사이 이동은 `shared/app`이 정한다. 플랫폼마다
다른 일(뒤로 두 번 종료, 알림 권한, 공유 창, 피드백 메일)은 `OvalitPlatform`으로 받는다.

## Complexity Tracking

헌법을 어기는 것이 없어 비워 둔다. 헌법이 미리 허락한 사용자 결정(KDA·승률 구간 색, Google 서비스 셋, 전투점수 예외)은 위반이
아니라 [research.md](research.md)에 까닭과 함께 적었다.
