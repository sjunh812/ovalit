# 첫 세션에 할 일

Claude Code에서 이 저장소를 열고 아래를 시킨다. `CLAUDE.md`를 먼저 읽힌다.

## 1. 뼈대 생성

Compose Multiplatform 프로젝트를 만든다. KMP 위저드(kmp.jetbrains.com) 또는
Android Studio의 Kotlin Multiplatform App 템플릿 출력을 기준으로 삼되,
버전은 생성 시점의 최신 안정판을 쓰고 무엇을 골랐는지 `docs/DECISIONS.md`에 적는다.

목표 구조. 위저드 출력과 다르면 위저드 쪽에 맞추고 이 문서를 고친다.

```
composeApp/              안드로이드 진입점, Compose UI
shared/
  core/model             도메인 모델
  core/network           Ktor client, API 래퍼
  core/data              Repository
  core/designsystem      색 토큰, 타이포, 공통 컴포넌트
  feature/onboarding     S0
  feature/report         S1, S1-a
  feature/match          S2, S3
  feature/friend         S5
  feature/settings       S4
build-logic/             convention plugin
```

고정된 선택:

- DI는 **Koin 4**. Hilt 금지 (KMP 미지원).
- 직렬화는 kotlinx.serialization, 네트워크는 Ktor client.
- `minSdk` 26.
- `build-logic` convention plugin으로 공통 설정을 묶는다.

확인이 필요한 선택:

- 로컬 저장소를 SQLDelight로 갈지 Room KMP로 갈지. Room이 KMP를 지원하기 시작했으니
  현재 안정성을 확인하고 고른 뒤 `docs/DECISIONS.md`에 이유와 함께 적는다.

## 2. 디자인 토큰 심기

`CLAUDE.md`의 색 토큰 표를 `core/designsystem`에 그대로 옮긴다.
다크와 라이트 두 벌, 원시 hex는 이 파일 밖으로 나가지 않는다.
서체는 숫자에 Archivo, 한글에 IBM Plex Sans KR.

## 3. 빌드 확인

```
./gradlew :composeApp:assembleDebug
```

통과할 때까지는 다음으로 넘어가지 않는다. 여기서 막히면 버전 조합 문제다.

## 4. 첫 화면

S0-1 인트로를 만든다. 로고(`CLAUDE.md`의 SVG 좌표), 한 줄 소개,
"Riot 계정으로 시작하기" 버튼. 화면 하나로 토큰과 타이포가 제대로 붙었는지 본다.

## 아직 하지 않는 것

프로덕션 키 승인 전에는 실데이터를 못 쓴다. 대기 중에는 아키텍처와 UI만 잡는다.
API 연동은 키가 나온 뒤에 시작한다.
