# 오발있 서버

앱과 Riot 사이에 서는 Cloudflare Worker입니다. 세 가지 일을 합니다.

- RSO 로그인을 대신 마칩니다. `client_secret`과 RGAPI 키는 앱에 넣을 수 없어서 여기서만 씁니다.
  앱에는 우리 세션 토큰만 내려갑니다.
- Riot API를 대신 부릅니다. 끝난 경기는 결과가 바뀌지 않으니 한 번 받으면 담아 둡니다.
- 친구 관계를 들고 있습니다. 서로 수락한 친구끼리만 전적을 보여주려면 서버가 알아야 합니다.

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
| GET | `/auth/rso/start?challenge=` | | Riot 로그인으로 보냅니다 |
| GET | `/auth/rso/callback` | | `ovalit://auth?code=`로 돌려보냅니다 |
| POST | `/auth/session` | | `{code, verifier}` → `{token, expiresAt}` |
| POST | `/auth/dev` | | 로컬 전용. `{puuid, gameName, tagLine}` → `{token, expiresAt}` |
| POST | `/auth/logout` | 필요 | 이 세션을 끊습니다 |
| GET | `/me` | 필요 | `{puuid, gameName, tagLine, statsPublic}` |
| PATCH | `/me` | 필요 | `{statsPublic}` |
| DELETE | `/me` | 필요 | 연동 해제. 세션, 친구, 요청, 초대가 같이 지워집니다 |
| GET | `/content` | 필요 | VAL-CONTENT(ko-KR). 6시간 담아 둡니다 |
| GET | `/status` | 필요 | VAL-STATUS. 60초 담아 둡니다 |
| GET | `/riot/matchlist` | 필요 | 내 경기 ID 목록 |
| GET | `/riot/matches/:matchId` | 필요 | 내가 뛴 경기만. 30일 담아 둡니다 |
| GET | `/riot/matches/:matchId/app-users` | 필요 | 그 경기의 앱 사용자와 나의 관계. 적어 둔 경기면 Riot을 부르지 않습니다 |
| GET | `/friends` | 필요 | `[{puuid, gameName, tagLine, statsPublic, since}]` |
| GET | `/friends/requests` | 필요 | `{received: [...], sent: [puuid]}` |
| POST | `/friends/requests` | 필요 | `{puuid, matchId}`. 같이 뛴 경기가 있어야 합니다 |
| POST | `/friends/requests/:puuid/accept` | 필요 | 받은 요청 수락 |
| POST | `/friends/requests/:puuid/decline` | 필요 | 받은 요청 거절 |
| DELETE | `/friends/:puuid` | 필요 | 친구 끊기 |
| GET | `/friends/:puuid/matchlist` | 필요 | 친구가 전적을 공개했을 때만 |
| GET | `/friends/:puuid/matches/:matchId` | 필요 | 내가 안 뛴 경기면 친구와 나 말고는 가립니다 |
| POST | `/invites` | 필요 | 7일짜리 초대 링크 `{code, url, expiresAt}` |
| POST | `/invites/:code/redeem` | 필요 | 초대한 사람에게 친구 요청을 보냅니다 |

`/cards/{uuid}_small.png`와 `/cards/{uuid}_wide.png`는 Worker를 거치지 않는 정적 파일입니다.
`public/cards/`는 저장소에 없고 아래 스크립트로 채웁니다.

### 지키는 선

Riot 프로덕션 키의 승인 조건이라 서버가 직접 막습니다. 자세한 건 `CLAUDE.md`의 "지켜야 할 선"을 봐 주세요.

- 전적에 닿는 경로는 모두 세션이 있어야 열립니다. RSO 전에는 어떤 전적도 부르지 않습니다.
  `/content`와 `/status`도 우리 Riot 키를 쓰니 세션 뒤에 둡니다. 세션 없이 열리는 건 `/health`,
  티어·역할 표, 초대 링크 페이지, 로그인 입구뿐입니다.
- Riot ID로 사람을 찾는 경로가 없습니다. 친구 요청은 같이 뛴 경기나 초대 링크로만 보냅니다.
  같이 뛰었는지는 서버가 경기를 직접 받아 확인합니다. 한 번 받은 경기는 참가자를 적어 두고 거기서 봅니다.
- 다른 사람의 경기는 서로 수락한 친구이고 그 친구가 전적을 공개했을 때만 엽니다.
- 내가 안 뛴 친구 경기에서는 친구와 나 말고 모두의 PUUID를 `anon-N`으로 바꾸고 이름, 태그,
  카드, 칭호를 지웁니다. 파티 ID도 그 경기 안에서만 통하는 이름으로 바꿉니다.
- Riot access token은 계정을 한 번 읽고 버립니다. 세션 토큰과 로그인 코드는 해시만 저장합니다.

### 에러 코드

Riot이 돌려준 실패는 이렇게 바꿔 보냅니다.

| Riot | 우리 응답 |
| --- | --- |
| 429 | 503 `riot_rate_limited`. `Retry-After`를 그대로 붙입니다 |
| 401, 403, 5xx, 연결 실패 | 502 `riot_unavailable` |
| 404 | 404 `not_found` |
| 키 없음 | 503 `riot_key_missing` |

## 무료 한도

카드 등록 없이 쓰는 Workers 무료 플랜 안에서 돌도록 짰습니다. 한도를 넘으면 과금되지 않고 막힙니다.
숫자는 2026-09-25에 Cloudflare 문서에서 확인했습니다.

| 항목 | 한도 | 이 서버에서 |
| --- | --- | --- |
| Worker 요청 | 하루 100,000 | 첫 수집 한 번이 약 51건(경기 목록 1 + 경기 50) |
| 요청당 CPU | 10ms | 가장 무거운 건 친구 경기 가리기. 800KB 경기로 재 보니 이 맥에서 약 3ms |
| 요청당 하위 요청 | 50 | 많아야 2건(RSO 콜백) |
| 요청당 D1 쿼리 | 50 | 많아야 5건(스코어보드 친구 요청) |
| D1 크기 | DB당 500MB | 사용자, 세션, 친구 관계와 경기 참가자. 경기 원문은 두지 않습니다. 참가자는 경기당 약 1.3KB라 38만 경기쯤 들어갑니다 |
| D1 쓰기 | 하루 10만 행 | 새 경기 하나에 약 10행(참가자 수). 다른 쓰기가 없으면 하루 새 경기 약 1만 개까지 적습니다 |
| D1 읽기 | 하루 500만 행 | 참가자 확인 한 번에 약 10행 |
| D1 행 크기 | 2MB | 가장 큰 행도 수백 바이트입니다 |
| 정적 에셋 | 파일 20,000개, 파일당 25MiB | 카드 2,032개. 정적 에셋 요청은 무료이고 요청 한도에 세지 않습니다 |

D1 한도는 00:00 UTC에 초기화됩니다. KV는 하루 쓰기가 1,000번뿐이라 쓰지 않습니다. R2는 카드 등록이
필요해서 쓰지 않습니다.

### 캐시

Riot 응답은 isolate 메모리, Cache API, Riot 순서로 찾습니다.

- Cache API는 사용자 정의 도메인에 붙인 Worker에서만 실제로 담깁니다. `*.workers.dev`에서는 아무것도
  담기지 않습니다. 도메인은 돈이 들어서 붙이지 않았습니다.
- 그래서 앞에 isolate 메모리 캐시를 둡니다. 콘텐츠 1개(6시간), 점검 안내 1개(60초), 끝난 경기는 합쳐서
  8MB까지 오래 안 꺼낸 것부터 버립니다. isolate가 내려가거나 다른 데이터센터로 가면 비어서 있으면 쓰는
  정도입니다.
- 끝난 경기를 Riot에서 처음 받으면 참가자 PUUID를 D1 `match_players`에 적습니다. 스코어보드의 앱 사용자와
  친구 요청은 참가자만 알면 되니 적어 둔 경기면 Riot을 부르지 않습니다. 경기 원문이 필요한 경로도 적어 둔
  참가자로 먼저 걸러, 남의 경기를 Riot에 묻지 않습니다.
- `match_players`는 사용자 계정이 아니라 Riot 경기 기록이라 연동을 해제해도 지우지 않습니다. PUUID 말고는
  적지 않습니다.
- 경기 원문이 필요한 요청은 캐시가 비면 Riot에 다시 갑니다. 앱이 받은 경기를 기기에 저장하니 크게 새지는
  않습니다.

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
npx wrangler d1 migrations apply ovalit --remote
npx wrangler deploy
```

RSO 앱 설정의 redirect URI에는 `https://<배포 주소>/auth/rso/callback`을 등록합니다. 서버는 요청이
들어온 주소로 이 값을 만듭니다.
