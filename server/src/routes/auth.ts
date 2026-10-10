import { type Context, Hono } from "hono";
import { html } from "hono/html";
import { randomToken, sha256 } from "../crypto";
import type { AppEnv, Env } from "../env";
import { ApiError } from "../errors";
import { logFailure } from "../log";
import { Quota } from "../memory";
import { appIntent, appPage } from "../page";
import { authorizeUrl, readAccount } from "../rso";
import { createSession, requireSession } from "../session";
import { openState, signState, stateSecret } from "../state";
import { upsertUser } from "../users";
import * as validate from "../validate";

const LOGIN_CODE_TTL_MS = 2 * 60 * 1000;
// 로그인을 마치면 서버 자기 주소의 이 경로로 돌려보낸다. 안드로이드가 App Link로 검증한 주소라 우리 앱만 연다.
// ovalit:// 같은 커스텀 스킴은 다른 앱도 같은 이름을 등록할 수 있어서, 남이 시작한 로그인의 코드를 가로챌 수 있다.
const APP_RETURN_PATH = "/auth/done";
// 에뮬레이터는 호스트 PC를 10.0.2.2로 본다.
const DEV_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]", "10.0.2.2"]);
const CALLBACK_ERRORS = new Set([
  "access_denied",
  "invalid_state",
  "riot_rate_limited",
  "rso_not_configured",
  "too_many_requests",
]);
const RETURN_ERRORS = new Set([...CALLBACK_ERRORS, "rso_failed"]);
// 세션 토큰이 담긴 응답이다. 중간 프록시나 기기 캐시에 남지 않게 한다.
const NO_STORE = { "Cache-Control": "no-store" };

// 로그인 시작과 콜백은 세션 없이 열려 있고 콜백은 Riot을 부른다. 그래서 IP마다 센다. isolate 메모리에서 세니 막는다기보다
// 줄이는 장치다. 휴대폰 통신사는 여럿이 IP 하나를 같이 쓰니 넉넉히 둔다. 시작 기준선이다.
const perIp = new Quota(20, 60_000);

export const auth = new Hono<AppEnv>();

/** 테스트끼리 IP별 한도가 이어지지 않게 비울 때 씁니다. */
export function clearAuthQuotas(): void {
  perIp.clear();
}

function rsoConfig(env: Env): { client: { id: string; secret: string }; stateSecret: string } {
  const secret = stateSecret(env.STATE_SECRET);
  if (!env.RSO_CLIENT_ID || !env.RSO_CLIENT_SECRET || !secret) throw new ApiError(503, "rso_not_configured");
  return { client: { id: env.RSO_CLIENT_ID, secret: env.RSO_CLIENT_SECRET }, stateSecret: secret };
}

function spendIp(c: Context, route: string): void {
  const retryAfter = perIp.take(`${route}:${c.req.header("CF-Connecting-IP") ?? "unknown"}`);
  if (retryAfter !== undefined) throw new ApiError(429, "too_many_requests", { "Retry-After": String(retryAfter) });
}

function callbackUrl(c: Context): string {
  return `${new URL(c.req.url).origin}/auth/rso/callback`;
}

function appReturn(c: Context, query: string): string {
  return `${new URL(c.req.url).origin}${APP_RETURN_PATH}?${query}`;
}

/**
 * 앱이 만든 verifier의 해시(challenge)를 서명한 state에 담아 Riot 로그인으로 보냅니다. 콜백이 앱으로 넘기는
 * 일회용 코드를 누가 가로채도 verifier가 없으면 세션으로 바꿀 수 없습니다. D1에는 아무것도 쓰지 않습니다.
 */
auth.get("/rso/start", async (c) => {
  const config = rsoConfig(c.env);
  spendIp(c, "start");
  const challenge = validate.token(c.req.query("challenge"), "invalid_challenge");
  const state = await signState(config.stateSecret, challenge, Date.now());
  return c.redirect(authorizeUrl(config.client.id, callbackUrl(c), state), 302);
});

// Custom Tabs 안에서 열리는 곳이라 실패도 JSON 대신 앱으로 돌려보낸다. 그래야 앱이 탭을 닫고 안내한다.
auth.get("/rso/callback", async (c) => {
  try {
    const config = rsoConfig(c.env);
    spendIp(c, "callback");
    if (c.req.query("error") !== undefined) throw new ApiError(400, "access_denied");
    const now = Date.now();
    const challenge = await openState(config.stateSecret, c.req.query("state") ?? "", now);
    if (!challenge) throw new ApiError(400, "invalid_state");
    const code = validate.text(c.req.query("code"), 1024, "rso_failed");

    const account = await readAccount(c.var.upstream, config.client, code, callbackUrl(c));
    const userId = await upsertUser(c.env.DB, account.puuid, account.gameName, account.tagLine);
    const loginCode = randomToken();
    await c.env.DB.batch([
      c.env.DB.prepare("DELETE FROM login_codes WHERE expires_at <= ?").bind(now),
      c.env.DB.prepare("INSERT INTO login_codes (code_hash, user_id, challenge, expires_at) VALUES (?, ?, ?, ?)").bind(
        await sha256(loginCode),
        userId,
        challenge,
        now + LOGIN_CODE_TTL_MS,
      ),
    ]);
    return c.redirect(appReturn(c, `code=${loginCode}`), 302);
  } catch (err) {
    // ApiError가 아니면 D1처럼 우리 쪽에서 난 실패다. 앱에는 rso_failed로만 가니 이름이라도 남겨 둔다.
    if (!(err instanceof ApiError)) logFailure("rso_callback", err);
    const code = err instanceof ApiError && CALLBACK_ERRORS.has(err.code) ? err.code : "rso_failed";
    return c.redirect(appReturn(c, `error=${code}`), 302);
  }
});

/**
 * 로그인을 마친 뒤 앱이 열리지 않았을 때 브라우저에 보이는 페이지입니다. 앱이 없거나 App Link 검증이 아직 안 된
 * 기기에서 여기에 머뭅니다. 버튼은 우리 패키지를 지정한 intent 주소라 다른 앱이 받지 못합니다.
 *
 * 코드는 이미 주소창에 있는 값이라 페이지에 한 번 더 적어도 더 새지 않습니다. 모양이 맞을 때만 버튼에 넣습니다.
 */
auth.get("/done", (c) => {
  const code = c.req.query("code");
  const error = c.req.query("error");
  const query = code !== undefined && validate.TOKEN.test(code)
    ? `code=${code}`
    : error !== undefined && RETURN_ERRORS.has(error)
      ? `error=${error}`
      : undefined;
  const intent = query && appIntent(new URL(c.req.url), `${APP_RETURN_PATH}?${query}`);
  c.header("Cache-Control", "no-store");
  return appPage(c, {
    title: "오발있 로그인",
    main: intent
      ? html`<p>오발있 앱에서 이어서 진행해요. 앱이 저절로 열리지 않았다면 아래 버튼을 눌러 주세요.</p>
<a href="${intent}">앱으로 돌아가기</a>`
      : html`<p>오발있 앱에서 다시 로그인해 주세요.</p>`,
  });
});

auth.post("/session", async (c) => {
  const body = await validate.jsonBody(c);
  const code = validate.token(body.code, "invalid_code");
  const verifier = validate.verifier(body.verifier);
  // verifier가 틀려도 코드는 이 자리에서 지운다. 가로챈 코드로 verifier를 여러 번 찍어 볼 수 없게 한다.
  const row = await c.env.DB.prepare("DELETE FROM login_codes WHERE code_hash = ? RETURNING user_id, challenge, expires_at")
    .bind(await sha256(code))
    .first<{ user_id: number; challenge: string; expires_at: number }>();
  if (!row || row.expires_at <= Date.now() || (await sha256(verifier)) !== row.challenge) {
    throw new ApiError(400, "invalid_code");
  }
  return c.json(await createSession(c.env.DB, row.user_id), 200, NO_STORE);
});

/**
 * RSO 없이 친구 흐름을 로컬에서 돌려 보는 문입니다. `DEV_LOGIN`이 `"true"`이고 로컬 주소로 들어왔을 때만
 * 열립니다. 아무 PUUID로나 로그인할 수 있어서 배포 환경에서 열리면 남의 전적을 볼 수 있게 됩니다.
 *
 * 로컬 주소는 요청의 Host 헤더로 가리므로 보내는 쪽이 얼마든지 꾸밀 수 있습니다. 실제로 막는 건 `DEV_LOGIN`
 * 하나이고, `wrangler dev`도 localhost에만 붙여 둬야 합니다.
 */
auth.post("/dev", async (c) => {
  if (c.env.DEV_LOGIN !== "true" || !DEV_HOSTS.has(new URL(c.req.url).hostname)) throw new ApiError(404, "not_found");
  const body = await validate.jsonBody(c);
  const puuid = validate.puuid(body.puuid);
  const gameName = validate.text(body.gameName, 16, "invalid_game_name");
  const tagLine = validate.text(body.tagLine, 5, "invalid_tag_line");
  const userId = await upsertUser(c.env.DB, puuid, gameName, tagLine);
  return c.json(await createSession(c.env.DB, userId), 200, NO_STORE);
});

auth.post("/logout", requireSession, async (c) => {
  await c.env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(c.var.sessionHash).run();
  return c.body(null, 204);
});
