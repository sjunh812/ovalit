# 릴리스

Play에 올릴 때마다 하는 일이다. 서버 배포는 `server/README.md`에 있다.

## local.properties

저장소에 올리지 않는 값은 `local.properties`와 `composeApp/google-services.json`에 둔다. 없으면 그 기능 없이 빌드된다. 키만 적고
값을 비워 둔 것도 없는 것으로 친다.

| 키 | 쓰는 곳 | 없으면 |
| --- | --- | --- |
| `composeApp/google-services.json` (Firebase 콘솔에서 받는 파일) | 푸시, 사용 통계, 비정상 종료 보고 | 셋 다 없이 돈다 |
| `admob.appId`, `admob.nativeUnitId`, `admob.rewardedUnitId` | 광고 | 광고 자리가 비고, 보상형이 없으면 광고 줄의 ×가 바로 닫는다 |
| `ovalit.feedback.email` | 설정의 "피드백 보내기" | 그 줄이 없다 |
| `ovalit.server.host` | RSO App Link 호스트 | 아무 데도 이어지지 않는 예약 도메인 |

실제 광고 단위 ID는 Riot 개발자 포털에서 제품이 승인된 뒤에 넣는다(CLAUDE.md 지켜야 할 선).

## Crashlytics 매핑 파일

난독화를 풀 매핑 파일은 릴리스 빌드(`./gradlew :composeApp:bundleRelease`) 때 Crashlytics 플러그인이 올린다. 인터넷이 있어야 하고,
안 올라가면 Crashlytics 콘솔의 스택이 `a.b.c` 같은 이름으로 남는다. 개발 기기용 `benchmark` 빌드는 올리지 않는다. Play Console의
Android vitals도 AAB에 든 매핑으로 비정상 종료를 풀어 준다.

## Play 데이터 보안

사용 통계와 비정상 종료 보고를 켰으니 다음을 적는다. 설정의 "사용 통계 보내기"로 끌 수 있어 "사용자가 선택 가능"이다.

- 앱 활동: 앱 상호작용 (Firebase Analytics)
- 앱 정보 및 성능: 비정상 종료 로그, 진단 (Crashlytics)
- 기기 또는 기타 ID: 앱 인스턴스 ID, 광고 ID(AdMob)
- 목적은 분석과 광고, 전송 중 암호화, 서비스 제공자가 처리하므로 공유가 아니다

출시 전에 AdMob 동의 창(UMP)을 붙이고, 처음에는 배포 국가를 한국으로 둔다.
