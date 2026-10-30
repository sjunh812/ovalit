# Contract: 앱 ↔ 서버 HTTP API

앱이 기대는 서버 경로와 서버가 지키는 약속을 적는다. 요청·응답 본문의 모양, 에러 코드, 호출 한도의 원본은
`server/README.md`의 "엔드포인트"다. 경로를 바꾸면 그 표와 이 문서를 같은 커밋에서 고친다.

## 공통

- 응답은 JSON이고 실패하면 `{ "error": "<코드>" }`다. 세션은 `Authorization: Bearer <token>`으로 보낸다.
- 시각은 모두 epoch ms다.
- Riot 실패는 이렇게 바꾼다: 429 → 503 `riot_rate_limited`(+`Retry-After`), 401·403·5xx·연결 실패 → 502 `riot_unavailable`,
  404 → 404 `not_found`. 앱은 이를 `OvalitError`로 옮긴다.
- 사용자마다 Riot 호출 한도를 넘기면 429 `too_many_requests`(+`Retry-After`)다. 앱은 새로 받을 경기가 없는 것처럼 둔다.

## 세션 없이 열리는 경로

| 경로 | 쓰는 곳 |
| --- | --- |
| `GET /health` | 살아 있는지 |
| `GET /content/tiers`, `GET /content/roles` | 티어 이름, 요원 역할 표 |
| `GET /auth/rso/start?challenge=` | S0-3 로그인 시작(Custom Tabs). D1에 쓰지 않는다 |
| `GET /auth/rso/callback` → `GET /auth/done?code=` | App Link로 일회용 코드 전달 |
| `POST /auth/session` | `{code, verifier}` → `{token, expiresAt}` |
| `GET /i/:code` | 초대 링크를 브라우저에서 열었을 때 |
| `GET /.well-known/assetlinks.json` | App Link 검증 |
| `POST /auth/dev` | 로컬 전용. RSO 비밀값이 있으면 404 |

전적에 닿는 경로는 이 목록에 없다. RSO 전에는 어떤 전적도 받을 수 없다(FR-002).

## 세션이 필요한 경로

| 묶음 | 경로 | 스펙 |
| --- | --- | --- |
| 계정 | `GET /me`, `PATCH /me {statsPublic?, remindBefore?}`, `DELETE /me`(연동 해제), `POST /auth/logout` | FR-029, FR-030 |
| 기기 토큰 | `PUT /me/push-token`, `DELETE /me/push-token` | FR-027 |
| 콘텐츠 | `GET /content`(VAL-CONTENT ko-KR), `GET /status`(점검 안내) | FR-035 |
| 내 경기 | `GET /riot/matchlist`, `GET /riot/matches/:matchId`, `GET /riot/matches/:matchId/app-users` | FR-003, FR-004, FR-024 |
| 친구 | `GET /friends`, `GET /friends/requests`, `POST /friends/requests {puuid, matchId}`, `POST /friends/requests/:puuid/accept`, `POST /friends/requests/:puuid/decline`, `DELETE /friends/:puuid` | FR-021, FR-028 |
| 친구 경기 | `GET /friends/:puuid/matchlist`, `GET /friends/:puuid/matches/:matchId` | FR-022, FR-023 |
| 초대 링크 | `POST /invites`, `POST /invites/:code/redeem` | FR-021 |
| ㅇㅂㅇ | `GET /pings`, `POST /pings {friends, startsAt}`, `POST /pings/:id/reply {answer, proposedAt?}`, `POST /pings/:id/time {startsAt}`, `POST /pings/:id/members {friends}`, `DELETE /pings/:id` | FR-026, FR-027 |

## 서버가 지키는 약속

- Riot ID로 사람을 찾는 경로가 없다. 친구 요청은 같이 뛴 경기가 확인될 때만 받는다(FR-021).
- `GET /friends/:puuid/*`는 서로 수락한 친구가 전적을 공개했을 때만 연다. 내가 안 뛴 경기는 친구와 나 말고 모든 PUUID를 `anon-N`으로
  바꾸고 플레이어 줄을 허용 목록으로 줄인다(FR-022, FR-023).
- `GET /riot/matches/:matchId/app-users`는 그 경기를 같이 뛴 사람에게만 앱 사용 여부를 준다(FR-024).
- `GET /riot/matches/:matchId`는 내가 뛴 경기만 준다. 남의 경기 ID로 참가자 기록을 늘릴 수 없다.
- `DELETE /me`는 세션, 친구, 요청, 초대, ㅇㅂㅇ, 기기 토큰을 바로 지운다. 살아 있던 ㅇㅂㅇ의 불린 친구에게 `ping_cancel`을 보낸다(FR-030).
- 세션 토큰을 주는 응답은 `Cache-Control: no-store`다. 세션과 로그인 코드는 해시만 저장한다.

## 앱이 지키는 약속

- 끝난 경기 상세(`/riot/matches/:matchId`)는 저장한 뒤 다시 부르지 않는다(SC-006).
- 경기 ID 목록은 당길 때, 또는 앱을 다시 열고 마지막 확인 뒤 10분이 지났을 때만 부른다(FR-005).
- 같은 친구의 경기 목록은 1분에 한 번까지 당긴다.
- 응답 본문은 문자열로 받아 직접 읽고, 실패한 예외 문구를 오류 보고에 보내지 않는다(FR-032).
