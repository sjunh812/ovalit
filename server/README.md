# 오발있 서버

앱과 Riot 사이에 서는 Cloudflare Worker입니다. 네 가지 일을 합니다.

- RSO 로그인을 대신 마칩니다. `client_secret`과 RGAPI 키는 앱에 넣을 수 없어서 여기서만 씁니다.
  앱에는 우리 세션 토큰만 내려갑니다.
- Riot API를 대신 부릅니다. 끝난 경기는 결과가 바뀌지 않으니 한 번 받으면 담아 둡니다.
- 친구 관계를 들고 있습니다. 서로 수락한 친구끼리만 전적을 보여주려면 서버가 알아야 합니다.
- 알림을 보냅니다. 오발있("오늘 발로란트 할 사람 있어?")을 띄우거나 대답하면 FCM으로 친구에게 알리고, 월요일
  아침에는 지난주 리포트가 나왔다고 알립니다.

지금은 로컬에서만 돕니다. Cloudflare 계정이 없어도 됩니다.

## 로컬에서 돌리기

Node 22 이상이 필요합니다. wrangler 4가 그렇게 요구합니다.

```bash
cd server
npm install
cp .dev.vars.example .dev.vars
npm run db:migrate:local
npm run dev
```

`http://localhost:8787`에서 뜹니다. `.dev.vars`는 저장소에 올리지 않습니다. Riot 키를 넣으면
`/content`와 `/status`가 실제 값을 받아옵니다. 비워 두면 Riot을 부르는 경로는
`riot_key_missing`을 돌려줍니다.

RSO는 프로덕션 키가 나와야 붙일 수 있습니다. 그 전에는 `/auth/dev`로 아무 PUUID나 넣어 세션을
받습니다. `.dev.vars`의 `DEV_LOGIN=true`이고 로컬 주소로 들어올 때만 열립니다. 에뮬레이터에서는
`http://10.0.2.2:8787`로 부르거나 `adb reverse tcp:8787 tcp:8787`을 걸고 `localhost`로 부릅니다.

로컬 주소인지는 요청의 Host 헤더로 가립니다. 이 헤더는 보내는 쪽이 마음대로 적을 수 있어서 실제로 막는 건
`DEV_LOGIN` 하나입니다. `wrangler dev`는 기본으로 localhost에만 붙습니다. `--ip 0.0.0.0`이나 `dev.ip` 설정으로
바깥에 열지 마세요. 같은 네트워크의 누구든 Host 헤더만 바꿔 아무 PUUID로 로그인할 수 있게 됩니다. 기기에서 닿지
않으면 바깥에 여는 대신 `adb reverse`를 씁니다.

```bash
curl -X POST localhost:8787/auth/dev -H 'Content-Type: application/json' \
  -d '{"puuid":"<78자>","gameName":"제트장인","tagLine":"KR1"}'
```

테스트와 타입 검사는 이렇게 돌립니다. 테스트는 Workers 런타임 안에서 돌고 Riot은 가짜로 바꿔 끼웁니다.

```bash
npm test
npm run typecheck
```

## 엔드포인트

응답은 JSON입니다. 실패하면 `{ "error": "<코드>" }`가 옵니다. 세션은
`Authorization: Bearer <token>` 헤더로 보냅니다.

| 메서드 | 경로 | 세션 | 하는 일 |
| --- | --- | --- | --- |
| GET | `/health` | | 살아 있는지 |
| GET | `/content/tiers` | | 티어 번호 → 한글 이름 |
| GET | `/content/roles` | | 요원 UUID(소문자) → `duelist` · `initiator` · `controller` · `sentinel` |
| GET | `/i/:code` | | 초대 링크를 브라우저에서 열었을 때 보이는 쪽 |
| GET | `/auth/rso/start?challenge=` | | Riot 로그인으로 보냅니다. D1에 쓰지 않습니다 |
| GET | `/auth/rso/callback` | | `/auth/done?code=`(App Link)로 돌려보냅니다. 실패하면 `/auth/done?error=` |
| GET | `/auth/done` | | 앱이 안 열렸을 때 보이는 쪽. 우리 패키지를 지정한 intent 버튼을 둡니다 |
| GET | `/.well-known/assetlinks.json` | | App Link 검증 파일. `ANDROID_CERT_SHA256`이 없으면 404 |
| POST | `/auth/session` | | `{code, verifier}` → `{token, expiresAt}` |
| POST | `/auth/dev` | | 로컬 전용. `{puuid, gameName, tagLine}` → `{token, expiresAt}` |
| POST | `/auth/logout` | 필요 | 이 세션을 끊습니다 |
| GET | `/me` | 필요 | `{puuid, gameName, tagLine, statsPublic}` |
| PATCH | `/me` | 필요 | `{statsPublic}` |
| DELETE | `/me` | 필요 | 연동 해제. 세션, 친구, 요청, 초대, 오발있, 기기 토큰이 같이 지워집니다 |
| PUT | `/me/push-token` | 필요 | `{token}`. FCM 기기 토큰을 등록합니다. 다른 계정이 쓰던 토큰이면 옮겨 오고, 사람마다 최근 셋만 둡니다 |
| DELETE | `/me/push-token` | 필요 | `{token}`. 내 토큰일 때만 지우고 없어도 204입니다. 로그아웃하기 전에 부릅니다 |
| GET | `/content` | 필요 | VAL-CONTENT(ko-KR). 6시간 담아 둡니다 |
| GET | `/status` | 필요 | VAL-STATUS. 60초 담아 둡니다 |
| GET | `/riot/matchlist` | 필요 | 내 경기 ID 목록. 담아 두지 않고 10초에 한 번만 Riot에 갑니다 |
| GET | `/riot/matches/:matchId` | 필요 | 내가 뛴 경기만. 30일 담아 둡니다 |
| GET | `/riot/matches/:matchId/app-users` | 필요 | 그 경기의 앱 사용자와 나의 관계. 적어 둔 경기면 Riot을 부르지 않습니다 |
| GET | `/friends` | 필요 | `[{puuid, gameName, tagLine, statsPublic, since}]`. 최근에 친구가 된 순서로 200명까지 |
| GET | `/friends/requests` | 필요 | `{received: [...], sent: [puuid]}`. 둘 다 최근 것부터 200개까지 |
| POST | `/friends/requests` | 필요 | `{puuid, matchId}`. 같이 뛴 경기가 있어야 합니다 |
| POST | `/friends/requests/:puuid/accept` | 필요 | 받은 요청 수락. 한도에 걸리면 요청은 남습니다 |
| POST | `/friends/requests/:puuid/decline` | 필요 | 받은 요청 거절 |
| DELETE | `/friends/:puuid` | 필요 | 친구 끊기. 서로 띄운 오발있에서도 빠집니다 |
| GET | `/friends/:puuid/matchlist` | 필요 | 친구가 전적을 공개했을 때만. 같은 친구는 1분에 한 번만 Riot에 갑니다 |
| GET | `/friends/:puuid/matches/:matchId` | 필요 | 내가 안 뛴 경기면 친구와 나 말고는 가립니다 |
| POST | `/invites` | 필요 | 7일짜리 초대 링크 `{code, url, expiresAt}`. 하루 넘게 남고 다 쓰지 않은 링크가 있으면 그 링크를 200으로 줍니다 |
| POST | `/invites/:code/redeem` | 필요 | 초대한 사람에게 친구 요청을 보냅니다 |
| GET | `/pings` | 필요 | `{pings: [Ping]}`. 내가 띄웠거나 불려 간 것 중 취소하지 않았고 끝나지 않은 것. 최근에 띄운 것부터 |
| POST | `/pings` | 필요 | `{friends: [puuid], startsAt}` → 201 `{ping}`. 서로 수락한 친구를 1~4명 부릅니다 |
| POST | `/pings/:id/reply` | 필요 | `{answer, proposedAt?}` → `{ping}`. 불려 간 친구만 |
| POST | `/pings/:id/time` | 필요 | `{startsAt}` → `{ping}`. 호스트만 |
| POST | `/pings/:id/members` | 필요 | `{friends: [puuid]}` → `{ping}`. 호스트만. 못 간다고 한 사람을 뺀 인원이 넷까지. 넘으면 409 `ping_full`, 이미 부른 친구면 409 `already_invited`. 더한 친구에게만 `ping_new` |
| DELETE | `/pings/:id` | 필요 | 호스트만. 취소하면 아무에게도 보이지 않습니다 |

이미 친구인 사람에게 요청하면 스코어보드 쪽(`POST /friends/requests`)은 409 `already_friends`, 초대 링크 쪽은
200 `{status: "already_friends"}`입니다. 초대 링크는 단톡방에 올려 여럿이 누르는 것이라 이미 친구인 사람이 다시
눌러도 에러가 나지 않게 했습니다. 스코어보드는 친구가 아닌 사람에게만 요청 버튼이 뜨니, 거기서 409가 오면 앱이
들고 있는 관계가 낡은 것입니다.

친구 요청과 수락에는 한도가 있습니다. 숫자는 모두 시작 기준선입니다. 새로 요청을 만들 때만 세고, 이미 보내 둔 요청을
다시 보내거나 이미 친구인 사람이 초대 링크를 누르는 건 세지 않습니다.

| 상태 | 코드 | 언제 |
| --- | --- | --- |
| 409 | `friends_full` | 내 친구가 200명일 때. 요청을 보낼 때와 수락할 때 모두 |
| 409 | `their_friends_full` | 상대 친구가 200명일 때. 초대 링크면 링크를 만든 사람 |
| 429 | `too_many_friend_requests` | 오늘(UTC) 요청을 30번 보냈을 때. `Retry-After`(초)는 다음 날 0시(UTC)까지 |
| 410 | `invite_used_up` | 그 초대 링크로 요청을 20개 만들었을 때. `POST /invites`가 새 링크를 줍니다 |

### 오발있

호스트가 서로 수락한 친구를 넷까지 불러 언제 할지 묻습니다. 발로란트 파티가 다섯 명까지라 넷입니다.

```
Ping = {
  id, host: {puuid, gameName, tagLine}, startsAt, createdAt, expiresAt,
  members: [{puuid, gameName, tagLine, answer, proposedAt, updatedAt}]
}
```

- 시각은 모두 epoch ms입니다. `startsAt`은 지금부터 1분 전과 24시간 뒤 사이여야 하고, `expiresAt`은 늘
  `startsAt` 한 시간 뒤입니다. 그때가 지나면 목록에서 빠집니다.
- `answer`는 `pending` · `yes` · `no` · `other_time`입니다. `other_time`이면 `proposedAt`을 같은 범위로 같이 보내고,
  `yes`나 `no`면 `proposedAt`은 보내도 버리고 `null`로 둡니다. 같은 대답을 다시 보내면 호스트에게 알리지 않습니다.
- 호스트가 시간을 바꾸면 그 시각을 제안한 친구는 `yes`가 되고, 나머지는 `yes`였던 친구까지 `pending`으로 돌아가
  다시 대답합니다. 같은 시각으로 다시 보내면 아무것도 바꾸지 않습니다. 버튼을 두 번 눌러 방금 `yes`가 된 친구가
  `pending`으로 돌아가지 않게 하려는 것입니다.
- `members`는 호스트가 고른 순서입니다. 친구를 끊거나 친구가 연동을 해제하면 그 자리가 빠집니다.
- 보는 사람과 친구가 아닌 `members`의 `puuid`는 `anon-N`입니다. N은 그 사람이 불린 순서라 앞사람이 빠져도 바뀌지 않고,
  다른 오발있의 같은 번호와는 상관이 없습니다. 호스트와 나 자신은 늘 진짜 PUUID입니다.
- 한 사람이 살아 있는 오발있은 하나만 띄웁니다. 24시간 동안 띄울 수 있는 건 취소한 것까지 열 번입니다.

| 상태 | 코드 | 언제 |
| --- | --- | --- |
| 400 | `invalid_body` | 본문 모양이 틀렸을 때. 친구 목록이 비었거나, 넷을 넘거나, 겹치거나, 나를 넣었을 때도 |
| 400 | `invalid_time` | `startsAt`이나 `proposedAt`이 1분 전보다 이르거나 24시간 뒤보다 늦을 때 |
| 400 | `invalid_push_token` | 토큰이 문자열이 아니거나 1~4096자가 아닐 때 |
| 403 | `not_friend` | 부른 사람 중에 서로 수락한 친구가 아닌 사람이 있을 때 |
| 404 | `not_found` | 없거나, 취소했거나, 끝났거나, 내가 대답하거나 고칠 수 있는 오발있이 아닐 때 |
| 409 | `ping_active` | 띄운 오발있이 아직 끝나지 않았을 때 |
| 429 | `too_many_requests` | 24시간 동안 열 번 띄웠을 때. `Retry-After`(초)가 지나면 한 번 더 띄울 수 있습니다 |

알림은 FCM data 메시지라 값이 모두 문자열입니다. 시각도 epoch ms를 문자열로 보냅니다. 오발있 알림은 `HIGH`에
TTL 1시간이고, 주간 리포트는 `NORMAL`입니다. 이름 목록은 쉼표로 잇고 빈칸을 두지 않습니다.

| `type` | 받는 사람 | 나머지 필드 |
| --- | --- | --- |
| `ping_new` | 불려 간 친구 | `pingId`, `startsAt`, `hostName`, `others`(같이 불린 친구 이름. 없으면 빈 문자열) |
| `ping_reply` | 호스트 | `pingId`, `startsAt`, `memberName`, `answer`, `proposedAt`(없으면 빈 문자열) |
| `ping_time` | 불려 간 친구 | `pingId`, `startsAt`(새 시각), `hostName` |
| `ping_cancel` | 불려 간 친구 | `pingId`, `hostName` |
| `ping_remind` | 호스트와 `yes`인 친구 | `pingId`, `startsAt`, `names`(호스트, `yes`인 친구 순서) |
| `weekly_report` | `weekly_report` 주제를 구독한 기기 | 없음 |

`ping_remind`는 시작 10분 전 안쪽에서 한 번만 갑니다. 아무도 `yes`가 아니면 보내지 않습니다. 시간을 바꾸면 새
시각으로 다시 한 번 갑니다. 알림이 실패해도 API 요청은 성공하고, 알림은 응답을 보낸 뒤에 나갑니다. FCM이 받을 수
없다고 한 토큰(404, `UNREGISTERED`, 토큰 형식이 틀린 `INVALID_ARGUMENT`)은 지웁니다.

`/cards/{uuid}_small.png`와 `/cards/{uuid}_wide.png`는 Worker를 거치지 않는 정적 파일입니다.
`public/cards/`는 저장소에 없고 아래 스크립트로 채웁니다.

초대 링크 페이지(`/i/:code`)에는 카카오톡 미리보기 카드용 Open Graph 태그가 있습니다. 그림은 정적 파일
`/og/invite-v1.png`(1200×630)이고 저장소에 들어 있습니다. `robots.txt`는 미리보기 수집기가 막히지 않게 `/i/`와
`/og/`만 엽니다.

### 지키는 선

Riot 프로덕션 키의 승인 조건이라 서버가 직접 막습니다. 자세한 건 `CLAUDE.md`의 "지켜야 할 선"을 봐 주세요.

- 전적에 닿는 경로는 모두 세션이 있어야 열립니다. RSO 전에는 어떤 전적도 부르지 않습니다.
  `/content`와 `/status`도 우리 Riot 키를 쓰니 세션 뒤에 둡니다. 세션 없이 열리는 건 `/health`,
  티어·역할 표, 초대 링크 페이지, 로그인 입구뿐입니다.
- Riot ID로 사람을 찾는 경로가 없습니다. 친구 요청은 같이 뛴 경기나 초대 링크로만 보냅니다.
  같이 뛰었는지는 서버가 경기를 직접 받아 확인합니다. 한 번 받은 경기는 참가자를 적어 두고 거기서 봅니다.
- 다른 사람의 경기는 서로 수락한 친구이고 그 친구가 전적을 공개했을 때만 엽니다.
- 내가 안 뛴 친구 경기에서는 친구와 나 말고 모두의 PUUID를 `anon-N`으로 바꾸고 이름, 태그,
  카드, 칭호, 계정 레벨을 지웁니다. PUUID가 객체 키로 와도 바꿉니다. 파티 ID도 그 경기 안에서만 통하는
  이름으로 바꿉니다.
- 오발있에 같이 불린 사람 중 나와 서로 수락한 친구가 아닌 사람은 PUUID를 그 오발있 안에서만 통하는 `anon-N`으로
  바꿉니다. 이름, 태그, 대답은 그대로 보여 줍니다(사용자 결정, 2026-10-03).
- Riot access token은 계정을 한 번 읽고 버립니다. 세션 토큰과 로그인 코드는 해시만 저장합니다. 세션 토큰을
  주는 응답에는 `Cache-Control: no-store`를 붙입니다.

### 에러 코드

Riot이 돌려준 실패는 이렇게 바꿔 보냅니다.

| Riot | 우리 응답 |
| --- | --- |
| 429 | 503 `riot_rate_limited`. Riot이 준 `Retry-After`를 붙이고, 없으면 10초(`service`는 5초)를 붙입니다 |
| 401, 403, 5xx, 연결 실패 | 502 `riot_unavailable` |
| 404 | 404 `not_found` |
| 키 없음 | 503 `riot_key_missing` |

429를 받은 뒤에는 `Retry-After`가 지날 때까지 막힌 범위로 가는 요청을 Riot에 보내지 않고 바로 같은 503을
돌려줍니다. 막힌 동안 계속 부르면 앱 전체의 몫이 더 깎이고, 되풀이하면 키가 막힐 수 있습니다. 범위는 Riot이 준
`X-Rate-Limit-Type`으로 정합니다.

| 종류 | 막는 범위 | 나누는 곳 |
| --- | --- | --- |
| `application` | 그 호스트 전체(`kr.api.riotgames.com`) | D1 `riot_blocks` |
| `method` | 그 호스트의 같은 경로 틀(`/val/match/v1/matches/{id}`) | D1 `riot_blocks` |
| `service`, 없음 | 그 경로 틀. Riot 쪽 서비스가 바쁜 것이라 우리 몫과 상관없습니다 | 이 isolate만 |

D1에는 429를 받을 때만 한 줄 씁니다. isolate는 Riot을 부르기 전에 5초에 한 번까지만 읽어서, 다른 isolate가 막힌 걸
알기까지 5초가 걸릴 수 있습니다. 풀린 줄은 5분마다 크론이 지워서 평소에는 빈 표를 읽습니다.

### 로그인

RSO state는 D1에 두지 않고 `STATE_SECRET`으로 서명해 Riot에 실어 보냅니다. 로그인 시작은 세션 없이 열려 있어서
시작마다 D1에 쓰면 누구나 되풀이해 불러 하루 쓰기 한도를 다 쓰게 할 수 있기 때문입니다. 콜백은 서명과 10분 기한을
보고, 같은 state를 두 번 받지 않습니다. 이건 isolate 메모리로 막는데, 다른 isolate로 가도 Riot 인가 코드가 한 번만
쓰여 로그인 코드가 두 번 나오지 않습니다.

로그인 시작과 콜백은 IP마다 1분에 20번까지입니다. 시작은 넘기면 429 `too_many_requests`에 `Retry-After`를 주고,
콜백은 Custom Tabs 안이라 `/auth/done?error=too_many_requests`로 돌려보냅니다. 콜백이 앱에 넘기는 `error`는
`access_denied`, `invalid_state`, `riot_rate_limited`, `rso_not_configured`, `too_many_requests`, `rso_failed`입니다.

### 호출 한도

Riot 레이트 리밋은 앱 전체에 걸려서 한 사람이 몰아 부르면 모두의 몫이 줄어듭니다. 그래서 사용자마다 Riot에
가는 횟수를 셉니다. 넘기면 Riot을 부르지 않고 429 `too_many_requests`에 `Retry-After`(초)를 붙여 돌려줍니다.

| 무엇 | 한도 |
| --- | --- |
| 내 경기 ID 목록 | 10초에 한 번 |
| 친구 경기 ID 목록 | 같은 친구는 1분에 한 번. 앱의 규칙과 같습니다 |
| 캐시에 없어 Riot에 가는 경기 상세 | 1분에 120번. 첫 수집 50경기는 걸리지 않습니다 |

Riot이 404를 준 경기 ID는 10분 동안 기억해 두고 다시 묻지 않습니다. 아무 ID나 넣은 요청이 그때마다 Riot 몫을
쓰지 않게 하려는 것입니다.

같은 경로를 동시에 부르면 Riot에는 한 번만 가고 결과를 나눠 받습니다. 같이 기다린 쪽은 위 한도를 쓰지 않습니다.

이 한도와 404 기억은 isolate 메모리에 둡니다. isolate끼리 나누지 않으니 요청이 여러 isolate로 나뉘면 한도를
넘길 수 있습니다. 막는 장치가 아니라 줄이는 장치로 봐 주세요. 위의 429 차단 중 `application`과 `method`만 D1로
나눕니다.

## 무료 한도

카드 등록 없이 쓰는 Workers 무료 플랜 안에서 돌도록 짰습니다. 한도를 넘으면 과금되지 않고 막힙니다.
숫자는 2026-09-25에 Cloudflare 문서에서 확인했습니다.

| 항목 | 한도 | 이 서버에서 |
| --- | --- | --- |
| Worker 요청 | 하루 100,000 | 첫 수집 한 번이 약 51건(경기 목록 1 + 경기 50) |
| 요청당 CPU | 10ms | 가장 무거운 건 친구 경기 가리기. 800KB 경기로 재 보니 이 맥에서 약 3ms |
| 요청당 하위 요청 | 50 | 많아야 2건(RSO 콜백) |
| 요청당 D1 쿼리 | 50 | 많아야 8건(스코어보드 친구 요청에서 Riot까지 갈 때) |
| D1 크기 | DB당 500MB | 사용자, 세션, 친구 관계와 경기 참가자. 경기 원문은 두지 않습니다. 참가자는 경기당 약 1KB이고 10주 지나면 지웁니다. 10주 동안 50만 경기쯤 들어갑니다 |
| D1 쓰기 | 하루 10만 행 | 새 경기 하나에 2행(경기 한 줄과 색인), 10주 뒤 지울 때 1행. 첫 수집 한 번이 100행입니다(2026-10-10, 로컬 D1의 `rows_written`) |
| D1 읽기 | 하루 500만 행 | 참가자 확인 한 번에 1행. Riot을 부르는 isolate마다 5초에 한 번 `riot_blocks`를 읽습니다(평소 빈 표) |
| D1 행 크기 | 2MB | 가장 큰 행도 수백 바이트입니다 |
| 정적 에셋 | 파일 20,000개, 파일당 25MiB | 카드 2,032개와 미리보기 그림 1개. 정적 에셋 요청은 무료이고 요청 한도에 세지 않습니다 |

D1 한도는 00:00 UTC에 초기화됩니다. KV는 하루 쓰기가 1,000번뿐이라 쓰지 않습니다. R2는 카드 등록이
필요해서 쓰지 않습니다.

만료된 세션과 초대는 누가 로그인하거나 링크를 만들 때 한 번에 50줄까지 같이 지웁니다.
`expires_at` 색인이 있어 지울 게 없으면 거의 읽지 않습니다. 색인 때문에 세션과 초대를 한 줄 넣을 때 쓰는 행이
하나씩 늘어납니다.

### 크론과 알림

`wrangler.jsonc`의 `triggers`에 크론 둘을 둡니다.

- `*/5 * * * *`: 10분 안에 시작하는 오발있을 알리고, 끝나고 하루가 지난 오발있을 한 번에 100개까지 지웁니다. 하루
  288번 돕니다. 하루 열 번 한도를 띄운 줄로 세서 끝나자마자 지우지 않습니다. 적은 지 10주가 지난 경기 참가자도 한 번에
  100경기까지 지우고, 풀린 Riot 429 차단도 지웁니다.
- `0 0 * * 1`: 월요일 00:00 UTC, 한국 시각 월요일 오전 9시에 지난주 리포트 알림을 보냅니다. 주제 메시지 하나라 사용자
  수와 상관없이 일주일에 FCM 요청 한 번입니다.

크론이 요청 한도에 같이 세어져도 하루 289건입니다. 오발있 하나를 넷에게 띄우고 넷이 대답한 뒤 알림까지 가면 이렇게
씁니다. 2026-10-03에 로컬 D1의 `rows_written`으로 쟀습니다.

| 무엇 | Worker 요청 | D1 행 쓰기(색인 포함) | FCM 하위 요청 |
| --- | --- | --- | --- |
| 띄우기 | 1 | 16 | 부른 친구의 기기 수(사람마다 셋까지) |
| 대답 | 1 | 1 | 호스트의 기기 수 |
| 시간 바꾸기 | 1 | 7 | 부른 친구의 기기 수 |
| 취소 | 1 | 1 | 부른 친구의 기기 수 |
| 시작 전 알림 | 크론 | 1 | 호스트와 `yes`인 친구의 기기 수 |
| 지우기 | 크론 | 5 | 0 |

기기가 사람마다 하나면 띄우고 대답하고 알리기까지 D1 쓰기 26행, FCM 하위 요청 13번 안팎입니다. 액세스 토큰을 새로
받을 때 한 번이 더 듭니다. 액세스 토큰은 isolate 메모리에 만료 5분 전까지 두고, 서비스 계정 키로 JWT에 서명하는 것도
그때만 합니다. 한 번 불릴 때 하위 요청은 50번까지라 알림은 40번 안에서만 보냅니다. 시작 전 알림이 그보다 많으면
남은 것은 5분 뒤 크론이 보냅니다.

### 캐시

Riot 응답은 isolate 메모리, Cache API, Riot 순서로 찾습니다.

- Cache API는 사용자 정의 도메인에 붙인 Worker에서만 실제로 담깁니다. `*.workers.dev`에서는 아무것도
  담기지 않습니다. 도메인은 돈이 들어서 붙이지 않았습니다.
- 그래서 앞에 isolate 메모리 캐시를 둡니다. 콘텐츠 1개(6시간), 점검 안내 1개(60초), 끝난 경기는 합쳐서
  24MB까지 오래 안 꺼낸 것부터 버립니다. 글자당 2바이트로 세서 800KB 경기가 15판쯤 들어갑니다. isolate가
  내려가거나 요청이 다른 데이터센터로 가면 비므로, 없을 수 있다고 보고 씁니다.
- 끝난 경기를 Riot에서 처음 받으면 참가자 PUUID를 D1 `match_players`에 경기마다 한 줄로 적습니다. 스코어보드의
  앱 사용자와 친구 요청은 참가자만 알면 되니 적어 둔 경기면 Riot을 부르지 않습니다. 경기 원문이 필요한 경로도
  적어 둔 참가자로 먼저 걸러, 남의 경기를 Riot에 묻지 않습니다.
- `match_players`는 사용자 계정이 아니라 Riot 경기 기록이라 연동을 해제해도 지우지 않습니다. PUUID 말고는
  적지 않습니다. 적은 지 10주가 지나면 크론이 지웁니다. 앱은 8주 안의 경기만 봅니다.
- 경기 원문이 필요한 요청은 캐시가 비면 Riot에 다시 갑니다. 앱이 받은 경기를 기기에 저장하니 크게 새지는
  않습니다.
- Cache API를 읽거나 담다 실패해도 요청은 실패하지 않습니다. 못 읽으면 없는 것으로 치고 Riot에서 받고, 로그에는
  `riot_cache`와 에러 이름만 남깁니다.

## 표와 카드 다시 만들기

티어 이름과 요원 역할은 Riot 공식 자료에 없어서 valorant-api.com의 ko-KR 덤프로 만듭니다. 새 요원이
나오거나 티어가 바뀌면 다시 돌리고 결과를 커밋합니다.

```bash
npm run build:tables -- <agents.json> <competitivetiers.json>
```

플레이어 카드는 콘텐츠 카탈로그 압축 파일에서 꺼냅니다. 시스템 `unzip`을 씁니다.

```bash
npm run prepare:assets -- <catalog.zip>
```

초대 링크 미리보기 그림은 로고 경로로 굽습니다. wrangler가 같이 설치하는 sharp를 씁니다. 그림을 바꾸면 스크립트의
`VERSION`과 `src/routes/public.ts`의 `OG_IMAGE_PATH`를 같이 올립니다. 카카오톡은 그림을 주소로 기억해서 같은
이름으로 덮어쓰면 한동안 옛 그림이 뜹니다.

```bash
node scripts/build-og-image.mjs
```

## 나중에 배포하기

Cloudflare 계정을 만든 뒤에 한 번만 하면 됩니다. `wrangler.jsonc`에 계정 ID는 넣지 않습니다.
배포 전에 `npm run prepare:assets`로 카드를 채워 둡니다.

```bash
npx wrangler login
npx wrangler d1 create ovalit
```

`d1 create`가 알려준 `database_id`를 `wrangler.jsonc`의 `d1_databases`에 넣습니다. 비밀값은 저장소나
`wrangler.jsonc`가 아니라 `secret put`으로만 넣습니다. `DEV_LOGIN`은 배포 환경에 절대 넣지 않습니다.

```bash
npx wrangler secret put RIOT_API_KEY
npx wrangler secret put RSO_CLIENT_ID
npx wrangler secret put RSO_CLIENT_SECRET
openssl rand -base64 32 | npx wrangler secret put STATE_SECRET
npx wrangler secret put FCM_SERVICE_ACCOUNT
npx wrangler d1 migrations apply ovalit --remote
npx wrangler deploy
```

`STATE_SECRET`은 RSO state에 서명하는 키라 32자 이상의 아무 값이면 됩니다. 없으면 RSO를 띄우지 않습니다. 바꾸면
그때 Riot 로그인 화면에 있던 사람만 처음부터 다시 로그인합니다.

`FCM_SERVICE_ACCOUNT`에는 Firebase 콘솔의 프로젝트 설정 > 서비스 계정에서 받은 JSON 파일 내용을 통째로 붙여 넣습니다.
`project_id`, `client_email`, `private_key`를 씁니다. 개인 키가 들어 있어서 저장소, `wrangler.jsonc`, `.dev.vars.example`
어디에도 두지 않습니다. 넣지 않으면 서버는 알림만 보내지 않고 나머지는 그대로 돕니다. 크론은 `wrangler deploy`가
`triggers`를 읽어 같이 등록합니다.

RSO 앱 설정의 redirect URI에는 `https://<배포 주소>/auth/rso/callback`을 등록합니다. 서버는 요청이
들어온 주소로 이 값을 만듭니다.

로그인을 마치면 서버는 `https://<배포 주소>/auth/done?code=`로 돌려보냅니다. `ovalit://` 같은 커스텀 스킴은 다른
앱도 등록할 수 있어서, 남이 시작한 로그인의 코드를 가로채 그 사람 계정의 세션을 받을 수 있습니다. 안드로이드가
이 주소를 우리 앱에 잇게 하려면 둘을 맞춥니다.

- 서버: 앱 서명 인증서의 SHA-256 지문을 `ANDROID_CERT_SHA256`에 넣습니다. 여럿이면 쉼표로 잇습니다. 디버그 키는
  `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`로, 출시 키는 Play
  Console의 앱 무결성 화면에서 봅니다. 비밀값은 아니지만 키마다 달라서 저장소에 두지 않습니다.
- 앱: `local.properties`에 `ovalit.server.host=<배포 주소의 호스트>`를 적습니다. 매니페스트의 App Link 호스트가
  됩니다. 적지 않으면 아무 데도 이어지지 않는 `ovalit.invalid`입니다.

```bash
npx wrangler secret put ANDROID_CERT_SHA256
```

검증이 안 된 기기에서는 `/auth/done`이 브라우저에 뜹니다. 그 페이지의 버튼은 `package=com.ovalit`을 지정한 intent
주소라 우리 앱만 받습니다.
