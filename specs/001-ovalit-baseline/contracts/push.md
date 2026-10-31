# Contract: 푸시(FCM 데이터 메시지)

서버가 보내고 앱이 문구를 만드는 알림의 모양이다. 필드 표의 원본은 `server/README.md`의 "오발있"이다.

## 공통

- FCM HTTP v1 데이터 메시지다. 값은 모두 문자열이고 시각도 epoch ms를 문자열로 보낸다.
- 글은 보내지 않는다. 종류(`type`), 이름, 시각만 보내고 문구는 앱이 언어에 맞춰 만든다(FR-027, FR-035).
- 이름 목록은 쉼표로 잇고 빈칸을 두지 않는다.
- ㅇㅂㅇ 알림은 `HIGH`에 TTL 1시간, 주간 리포트는 `NORMAL`이다.
- 알림이 실패해도 API 요청은 성공한다. FCM이 없는 토큰이라고 답하면(`UNREGISTERED`, 토큰을 짚은 `INVALID_ARGUMENT`,
  `SENDER_ID_MISMATCH`) 서버가 지운다. 로그아웃하거나 세션이 끝나면 그 세션으로 맡긴 토큰도 지운다.
- 같은 사람에게 너무 자주 가는 알림은 서버가 뺀다. ㅇㅂㅇ 하나에서 한 사람에게 가는 `ping_new`·`ping_reply`·`ping_time`은 한 시간에
  20번, 같은 사람이 보낸 `friend_request`는 24시간에 한 번이다(시작 기준선). API는 그대로 하니 앱은 알림이 오지 않아도 다시 보일 때
  받아 맞춘다. `ping_cancel`은 세지 않는다.

## 종류

| `type` | 받는 사람 | 필드 |
| --- | --- | --- |
| `ping_new` | 불려 간 친구 | `pingId`, `startsAt`, `hostName`, `others` |
| `ping_reply` | 호스트 | `pingId`, `startsAt`, `memberName`, `answer`, `proposedAt` |
| `ping_time` | 불려 간 친구 | `pingId`, `startsAt`, `hostName` |
| `ping_cancel` | 불려 간 친구 | `pingId`, `hostName` |
| `ping_remind` | 호스트와 `yes`인 친구 | `pingId`, `startsAt`, `names` |
| `weekly_report` | `weekly_report` 주제 구독 기기 | 없음 |
| `friend_request` | 친구 요청을 받은 사람(새로 들어간 요청만) | `name` |
| `friend_accept` | 친구 요청을 보낸 사람(상대가 수락했을 때) | `name` |

친구 요청 알림(FR-027, 2026-10-30 사용자 결정, T059)은 다른 푸시처럼 종류와 이름만 담는다. 이미 보내 둔 요청을 다시
보내거나 이미 친구인 경우에는 보내지 않는다. 거절당한 뒤 24시간 안에 다시 보낸 요청도 알리지 않는다. ㅇㅂㅇ처럼 `HIGH`에 TTL
1시간이다.

## 앱이 지키는 것

- 알림을 받고 새로 받기를 실패하면 조용히 넘기고 다음에 다시 한다.
- 시작 전 알림 시각은 서버 설정(`remindBefore`)을 따르고 주간 리포트는 설정 스위치를 따라 주제를 구독한다.
- 알림은 종류마다 끌 수 있다. 친구 요청 알림은 스위치를 따로 두고 기본은 켠다(FR-027).
- 알림 문구에 Riot이 준 값을 넣어도 그 문구를 사용 통계나 오류 보고로 보내지 않는다(FR-032).
