# 릴리스

Play에 앱을 올릴 때 보는 문서입니다. 처음 한 번 해 둘 것, 올릴 때마다 할 것, Play Console 양식에 적을 것 순서입니다. 서버
배포는 `server/README.md`에 있습니다.

출시는 Riot 프로덕션 키가 나오고 개발자 포털에서 제품이 승인된 뒤입니다. 실제 광고 단위 ID도 승인 뒤 제품 설명에 광고를
적고 나서 넣습니다(CLAUDE.md 지켜야 할 선).

## 처음 한 번

### 저장소에 올리지 않는 값

공개 저장소라 아래 값은 올리지 않고 내 컴퓨터에만 둡니다. 없으면 그 기능 없이 빌드됩니다. `local.properties`에 키만 적고 값을
비워 둔 것도 없는 것으로 칩니다.

| 어디에 | 무엇 | 쓰는 곳 | 없으면 |
| --- | --- | --- | --- |
| `composeApp/google-services.json` | Firebase 콘솔 > 프로젝트 설정 > 내 앱에서 받은 파일 | 푸시, 사용 통계, 비정상 종료 보고 | 셋 다 없이 돕니다 |
| `local.properties` | `admob.appId`, `admob.nativeUnitId`, `admob.rewardedUnitId` | 광고 | 광고 자리가 비고, 보상형이 없으면 광고 줄의 ×가 그 광고만 바로 닫습니다 |
| `local.properties` | `admob.test` | `true`면 디버그 빌드가 Google 테스트 광고를 받습니다. 빌드할 때 `-Povalit.ads.test=true`로도 됩니다 | 디버그도 위 ID를 씁니다 |
| `local.properties` | `ovalit.feedback.email` | 설정의 "피드백 보내기"가 여는 메일의 받는 주소 | 그 줄이 없습니다 |
| `local.properties` | `ovalit.server.host` | RSO 로그인과 초대 링크의 App Link 호스트 | 아무 데도 이어지지 않는 예약 도메인을 씁니다 |
| `server/.dev.vars`, Cloudflare 비밀값 | Riot 키, RSO `client_secret`, `FCM_SERVICE_ACCOUNT` 등 | 서버 | `server/README.md`를 봅니다 |

### 릴리스 서명

아직 없습니다. 지금 `bundleRelease`는 서명하지 않은 AAB를 만들어서 Play에 올릴 수 없습니다. 출시 전에 이렇게 붙입니다.

1. 업로드 키를 만듭니다. 키 파일과 비밀번호는 저장소 밖에 두고 잃어버리지 않게 따로 보관합니다.

   ```bash
   keytool -genkeypair -v -keystore ~/ovalit-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```

2. `composeApp/build.gradle.kts`의 `release`에 서명 설정을 더하고, 키 경로와 비밀번호는 `local.properties`에서 읽게 합니다.
3. Play Console에서 Play 앱 서명을 켭니다. 배포용 키는 Google이 들고, 우리는 업로드 키로만 서명합니다.
4. Play 앱 서명 키의 SHA-256 지문을 서버의 `ANDROID_CERT_SHA256`에 넣습니다. App Link(`/.well-known/assetlinks.json`)가 이
   지문으로 검증합니다. 디버그 키 지문과 다릅니다.

## 올릴 때마다

1. `composeApp/build.gradle.kts`의 `versionCode`를 하나 올리고 `versionName`을 고칩니다.
2. 릴리스를 빌드합니다. 인터넷이 있어야 합니다.

   ```bash
   ./gradlew :composeApp:bundleRelease
   ```

   - 빌드하면서 Crashlytics 플러그인이 난독화를 풀 매핑 파일을 Firebase에 올립니다. 안 올라가면 Crashlytics 콘솔의 스택이
     `a.b.c` 같은 이름으로 남습니다.
   - 개발 기기용 `benchmark` 빌드는 매핑을 올리지 않습니다.
   - Play Console의 Android vitals도 AAB에 든 매핑으로 비정상 종료를 풀어 줍니다.
3. `composeApp/build/outputs/bundle/release/`의 AAB를 Play Console에 올립니다.

## Play Console 양식

### 데이터 보안

앱이 Google로 보내는 것과 우리 서버가 저장하는 것을 모두 적습니다. 모두 전송 중에 암호화합니다. Firebase가 처리하는 항목은
Firebase의 [Play 데이터 보안 안내](https://firebase.google.com/docs/android/play-data-disclosure)와 맞춰 봅니다.

| 항목 | 무엇 | 어디로 | 목적 | 필수 여부 |
| --- | --- | --- | --- | --- |
| 개인 정보 > 사용자 ID | Riot PUUID와 Riot ID | 우리 서버 | 앱 기능(친구, ㅇㅂㅇ) | 필수. 연동해야 쓸 수 있습니다 |
| 앱 활동 > 기타 작업 | 친구 관계와 요청, ㅇㅂㅇ에 고른 시각과 답, 같이 뛴 경기의 참가자 | 우리 서버 | 앱 기능 | 필수 |
| 앱 활동 > 앱 상호작용 | 연 화면, 누른 기능 | Firebase Analytics | 분석 | 필수. 끄는 스위치가 없습니다 |
| 앱 정보 및 성능 | 비정상 종료 로그, 진단 | Crashlytics | 분석 | 필수 |
| 기기 또는 기타 ID | FCM 토큰 | 우리 서버, Firebase | 앱 기능(알림) | 필수 |
| 기기 또는 기타 ID | 앱 인스턴스 ID, 광고 ID | Firebase, AdMob | 분석, 광고 | 광고 ID는 기기 설정에서 끌 수 있습니다 |

- 경기 기록은 Riot이 준 것을 서버가 30일까지 담아 두었다가 앱으로 넘기고, 앱은 기기에 저장합니다. Riot 데이터는 Firebase로
  보내지 않습니다.
- 서비스 제공자(Google, Cloudflare)가 처리하는 것이라 "공유"가 아닙니다.
- 세션 토큰과 로그인 코드는 해시만 저장합니다.

### 데이터 삭제

- 앱 안에서는 설정의 "Riot 계정 연동 해제"가 서버의 사용자 줄을 지우고, 거기 걸린 세션, 친구, 요청, 초대, ㅇㅂㅇ, 기기 토큰이
  같이 지워집니다.
- Play는 앱을 지운 사람도 삭제를 요청할 수 있는 웹 주소를 따로 요구합니다. 아직 없어서 출시 전에 서버에 페이지를 둡니다.

### 그 밖에

- **개인정보처리방침 URL**: 위 데이터 보안 내용과 같은 것을 적은 공개 페이지가 있어야 합니다. 아직 없습니다.
- **배포 국가**: 처음에는 한국만 둡니다. 데이터는 한국 샤드뿐이라 일본 계정은 경기가 보이지 않습니다. 일본어 화면은 들어
  있어서 AP 샤드를 붙이면 일본을 더합니다.
- **광고 동의 창(UMP)**: AdMob은 유럽 경제 지역, 영국, 스위스에 배포하면 동의 창을 요구합니다. 한국만 배포하면 필요 없지만, 배포
  국가를 넓히기 전에 붙입니다.
