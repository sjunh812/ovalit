---
name: "ovalit-flow"
description: "오발있 기능 하나를 Spec Kit으로 설계(스펙) → 계획 문서 → Figma 디자인과 화면 설명 → 개발 → 마무리까지 한 번에 이어서 진행한다. 사용자가 올인원으로, 처음부터 끝까지, 설계부터 개발까지 해 달라고 하거나 /ovalit-flow를 부를 때 쓴다."
argument-hint: "새 기능 설명, 또는 기존 스펙(예: 001)과 시작 단계·범위(예: '001 디자인부터', '001 개발 US4')"
user-invocable: true
disable-model-invocation: false
---

# 오발있 올인원 흐름

설계부터 개발까지를 다섯 단계로 잇는다. 각 단계는 이미 있는 Spec Kit 스킬과 이 저장소의 규칙을 그대로 쓰고 이 스킬은 순서와
멈추는 곳, 오발있에서만 필요한 것을 정한다.

## 입력

```text
$ARGUMENTS
```

- 새 기능 설명이면 1단계부터 한다. 예: `친구 요청 알림`.
- 기존 스펙이면 번호나 폴더(`001`, `specs/001-ovalit-baseline`)를 받는다. 시작 단계(`설계부터`, `계획부터`, `디자인부터`,
  `개발부터`)와 범위(`US4`, `T045`, `홈`)를 같이 받을 수 있다.
- 비어 있으면 0단계로 지금 상태를 보고 어디서 이어 갈지 사용자에게 묻는다.

## 언제나 지키는 것

- 답과 진행 안내는 한국어로 한다.
- 기능을 제안하거나 받기 전에 `CLAUDE.md`의 "지켜야 할 선"과 `.specify/memory/constitution.md`를 본다. 요청이 선을 넘으면 그 자리에서
  멈추고 어느 원칙에 걸리는지 말한 뒤 묻는다(종합 점수, 남과의 순위, 닉네임 검색, "추천" 문구 같은 것).
- 단계가 끝날 때마다 결과를 보여 주고 다음 단계로 간다. 사용자가 "멈추지 말고 끝까지"라고 하면 단계 사이에서는 묻지 않되, 아래
  "꼭 묻는 곳"에서는 묻는다.
- 규칙(지켜야 할 선, 지표, 역할, 용어, 색 토큰, 화면 구조, 서버 규칙)이 바뀌면 그 규칙이 있는 문서(`CLAUDE.md`, `docs/screens.md`,
  `docs/design.md`, `docs/backend.md`)와 헌법, 스펙을 같은 커밋에서 고치고 `docs/DECISIONS.md`에 날짜와 까닭을 남긴다.
- 커밋과 푸시는 사용자가 요청할 때만 한다. 같은 저장소에서 다른 세션이 일하고 있을 수 있으니 커밋 전에 `git status`로 보고 이번에
  고친 파일만 담는다.

꼭 묻는 곳:

- 심사에 낸 목업(claude.ai 아티팩트, `site/index.html`)에서 화면이 벗어날 때
- 열린 결정을 닫을 때(로컬 DB, 전투점수 칸 같은 `research.md`의 열린 결정)
- KDA·승률 구간 경계나 이름처럼 "사용자 결정"이라 적힌 규칙을 건드릴 때
- 유료 구간이 있는 선택지가 나올 때(무료 한도부터 적는다)

## 0단계: 지금 상태 보기

1. `.specify/feature.json`, `specs/` 목록, `git status`, 지금 브랜치를 본다.
2. 대상 스펙 폴더에서 무엇이 있는지로 이어 갈 단계를 정한다.

| 있는 것 | 이어 갈 단계 |
| --- | --- |
| 스펙 없음 | 1단계 |
| `spec.md`만 | 2단계 |
| `plan.md`까지, `tasks.md` 없음 | 2단계의 작업 목록부터 |
| `tasks.md`까지 | 3단계(디자인이 남았으면) 또는 4단계 |

3. 인자로 시작 단계를 줬으면 그걸 따른다. 앞 단계 문서가 없으면 그 사실을 말하고 앞 단계부터 할지 묻는다.

## 1단계: 설계(스펙)

1. 새 기능이면 `/speckit-specify`에 기능 설명을 넘긴다. 브랜치를 새로 만들면 `docs/CONVENTIONS.md`의 브랜치 규칙
   (`<type>/<영문-슬러그>`)을 따른다.
2. `/speckit-clarify`로 애매한 곳을 묻는다. 답은 스펙의 Clarifications에 남기고 규칙이 바뀌는 답이면 규칙 문서와 DECISIONS도 고친다.
3. 필요하면 `/speckit-checklist`로 요구사항 품질을 본다.
4. 스펙 요약(사용자 이야기와 우선순위, 새 요구사항, 지켜야 할 선에 닿는 곳)을 보여 준다.

## 2단계: 계획 문서

1. `/speckit-plan`을 부른다. 새 기술을 고르지 않고 `CLAUDE.md`, `docs/backend.md`, `docs/CONVENTIONS.md`의 결정을 따른다. 정하지
   않은 것은 정하는 조건과 함께 `research.md`의 열린 결정으로 둔다. Constitution Check는 원칙마다 이 계획에서 지키는 자리를 적는다.
2. `/speckit-tasks`를 부른다. 이미 있는 것은 `[x]`, 남은 일은 사용자 이야기 순서다. 실제 파일 경로를 쓰고 Figma 작업은 "화면"
   페이지의 섹션 이름을 경로로 쓴다.
3. `/speckit-analyze`로 스펙, 계획, 작업 목록이 서로 맞는지 보고 어긋난 곳을 고친다.
4. 문서에 적은 파일, 함수, 테스트 이름이 실제로 있는지 `grep`으로 확인한다. 없는 걸 있다고 적지 않는다.

## 3단계: 디자인과 Figma 기획문서

준비:

1. 메모리의 `figma-design-file`을 읽는다. 파일 구조와 이미 겪은 함정이 있다.
2. `figma_get_status`(필요하면 `ToolSearch`로 figma-console 도구를 불러온다)로 Desktop Bridge 연결을 본다. 안 붙어 있으면 사용자에게
   Figma 파일 "오발있"에서 플러그인을 실행하거나 "Try again"을 눌러 달라고 한다.
3. 게임 이미지가 필요하면 이 스킬의 `scripts/asset_server.py`를 백그라운드로 띄운다.

```bash
python3 -I .claude/skills/ovalit-flow/scripts/asset_server.py shared/core/ui/src/commonMain/composeResources/files 9232
```

화면마다:

1. 그리기 전에 심사 목업의 그 화면, `docs/screens.md`의 그 절, 앱 코드의 실제 치수(dp, 글자 스타일, 색 토큰)를 본다. 앱에 이미 있는
   화면이면 에뮬레이터(`ANDROID_SERIAL=emulator-5554`) 스크린샷을 참고로 찍는다.
2. 무료 플랜은 파일 하나에 페이지가 셋(기획 개요, 기초, 화면)까지라 페이지를 새로 만들지 않는다. "화면" 페이지에 화면마다 한
   줄씩 아래로 쌓는다. 줄 왼쪽은 섹션 "<화면 이름> (Sx)"에 [라이트 화면과 왼쪽 여백의 파란 번호 | 화면 설명 패널 | 다크 화면]이고,
   옆 섹션 "<화면> 상태"에 상태별 화면과 밑에 "언제 뜨는지" 설명을 둔다. 설명은 규칙집처럼 단정형이고 근거(스펙 이야기, 문서 절)를 적는다.
3. 색은 "색 · 라이트"·"색 · 다크" 변수, 글자는 글자 스타일, 반복되는 것은 "기초" 페이지 컴포넌트의 인스턴스로 쓴다. 새로 반복되는
   것이 생기면 "기초" 페이지에 컴포넌트로 먼저 만든다.
4. 다 그리면 캡처해서 확인하고(겹침, 잘림, 빈 칸 높이) 사용자에게 보낸 뒤 다음 화면으로 간다.
5. "기획 개요" 페이지의 사용자 이야기 표에서 그 이야기의 Figma 진행 칸을 고친다.

Figma 함정(자세한 건 메모리):

- 노드를 만들기 전에 매번 `setCurrentPageAsync`로 페이지를 정한다. 사용자가 보는 페이지를 따라 바뀐다.
- 변수를 건 칠에는 변수 값도 같이 넣는다. 여러 색이 섞인 글자는 `getStyledTextSegments`로 구간마다 바꾼다.
- 여러 줄 글은 만들 때 어절 안 글자 사이에 U+2060을 넣는다. 그래서 Figma 문구는 복사해 쓰지 않고 `strings.xml`이 원본이다.
- 자동 배치 프레임에 `resize`를 부르면 높이가 고정되니 `primaryAxisSizingMode = 'AUTO'`를 다시 준다.
- 인스턴스 안 사각형 크기는 바뀌지 않는다. 길이가 값에 따라 달라지는 막대는 자동 배치 막대의 `paddingRight`로 정한다.
- 숫자 스타일의 tnum은 API로 못 켠다.

디자인이 지금 앱과 달라지면 먼저 묻고 DECISIONS에 남긴다. 확정된 화면은 4단계에서 바로 앱에 반영한다.

## 4단계: 개발

1. `/speckit-implement`에 범위(이야기, 작업 번호)를 넘긴다. `tasks.md` 순서를 따르고 `[P]` 작업은 서브에이전트로 나눠도 된다.
2. 코드를 쓸 때 `CLAUDE.md`와 `docs/CONVENTIONS.md`를 따른다. 자주 놓치는 것:
   - 화면 문구는 `strings.xml`(한국어)과 `values-ja/strings.xml`(일본어)을 같은 커밋에서. Kotlin 상수와 `\n` 금지
   - 원시 hex와 `MaterialTheme.colorScheme` 금지, `OvalitTheme.colors` 토큰
   - 경기를 모아 세는 계산은 ViewModel이 `Dispatchers.Default`에서
   - Riot이 준 값은 사용 통계, 비정상 종료 보고, 피드백 메일에 넣지 않는다
   - 지표 계산, 지켜야 할 선, 디자인 토큰 제약은 테스트가 지키게 하고 새 규칙은 일부러 지워 테스트가 깨지는지 본다
   - 주석은 자연스러운 한국말로, 문장마다 줄을 바꾼다
3. 확인한다.

```bash
./gradlew testAndroidHostTest
```

```bash
./gradlew :shared:feature:<모듈>:iosSimulatorArm64Test
```

```bash
cd server && npm run typecheck && npm test
```

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :composeApp:installDebug
```

   설치는 에뮬레이터에만 한다(폰은 사용자가 요청할 때만). 화면은 에뮬레이터 스크린샷을 Figma 화면과 나란히 보고 어긋난 곳을 고친다.
4. 끝난 작업은 `tasks.md`에 `[x]`로 표시한다. 실패한 검사는 출력과 함께 그대로 보고한다.

## 5단계: 마무리

1. 문서 동기화를 본다. 바뀐 규칙이 규칙 문서, 헌법, 스펙, DECISIONS에 같은 내용으로 있는지, `tasks.md`와 Figma 진행 칸이 맞는지.
2. 사용자가 커밋을 원하면 `docs/CONVENTIONS.md` 형식으로 한다. 제목은 `<type>(<scope>): <한글 제목>`이고 "추가", "수정", "제거"
   같은 명사로 끝낸다. DECISIONS를 고쳤으면 `Decision:` 트레일러를 단다. 다른 세션의 변경이 섞였으면 무엇이 누구 것인지 말하고
   담을지 묻는다.
3. 보고: 단계마다 한 것, 남은 작업 번호, 열린 결정과 물어볼 것. 띄워 둔 이미지 서버가 있으면 그것도 알린다.
