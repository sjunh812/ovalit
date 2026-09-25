import { type Context, Hono } from "hono";
import { randomToken, sha256 } from "../crypto";
import type { AppEnv, Env } from "../env";
import { ApiError } from "../errors";
import { authorizeUrl, readAccount } from "../rso";
import { createSession, requireSession } from "../session";
import { upsertUser } from "../users";
import * as validate from "../validate";

const STATE_TTL_MS = 10 * 60 * 1000;
const LOGIN_CODE_TTL_MS = 2 * 60 * 1000;
const APP_CALLBACK = "ovalit://auth";
// 에뮬레이터는 호스트 PC를 10.0.2.2로 본다.
const DEV_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]", "10.0.2.2"]);
const CALLBACK_ERRORS = new Set(["access_denied", "invalid_state", "riot_rate_limited", "rso_not_configured"]);

export const auth = new Hono<AppEnv>();

function rsoClient(env: Env): { id: string; secret: string } {
  if (!env.RSO_CLIENT_ID || !env.RSO_CLIENT_SECRET) throw new ApiError(503, "rso_not_configured");
  return { id: env.RSO_CLIENT_ID, secret: env.RSO_CLIENT_SECRET };
}

function callbackUrl(c: Context): string {
  return `${new URL(c.req.url).origin}/auth/rso/callback`;
}

/**
 * 앱이 만든 verifier의 해시(challenge)를 받아 두고 Riot 로그인으로 보냅니다. 콜백이 앱으로 넘기는
 * 일회용 코드를 누가 가로채도 verifier가 없으면 세션으로 바꿀 수 없습니다.
 */
auth.get("/rso/start", async (c) => {
  const client = rsoClient(c.env);
  const challenge = validate.token(c.req.query("challenge"), "invalid_challenge");
  const state = randomToken();
  const now = Date.now();
  await c.env.DB.batch([
    c.env.DB.prepare("DELETE FROM auth_states WHERE created_at <= ?").bind(now - STATE_TTL_MS),
    c.env.DB.prepare("INSERT INTO auth_states (state, challenge, created_at) VALUES (?, ?, ?)").bind(state, challenge, now),
  ]);
  return c.redirect(authorizeUrl(client.id, callbackUrl(c), state), 302);
});

// Custom Tabs 안에서 열리는 곳이라 실패도 JSON 대신 앱 딥링크로 돌려보낸다. 그래야 앱이 탭을 닫고 안내한다.
auth.get("/rso/callback", async (c) => {
  try {
    const client = rsoClient(c.env);
    if (c.req.query("error") !== undefined) throw new ApiError(400, "access_denied");
    const state = validate.token(c.req.query("state"), "invalid_state");
    const code = validate.text(c.req.query("code"), 1024, "rso_failed");
    const now = Date.now();
    const pending = await c.env.DB.prepare("DELETE FROM auth_states WHERE state = ? RETURNING challenge, created_at")
      .bind(state)
      .first<{ challenge: string; created_at: number }>();
    if (!pending || pending.created_at <= now - STATE_TTL_MS) throw new ApiError(400, "invalid_state");

    const account = await readAccount(c.var.upstream, client, code, callbackUrl(c));
    const userId = await upsertUser(c.env.DB, account.puuid, account.gameName, account.tagLine);
    const loginCode = randomToken();
    await c.env.DB.batch([
      c.env.DB.prepare("DELETE FROM login_codes WHERE expires_at <= ?").bind(now),
      c.env.DB.prepare("INSERT INTO login_codes (code_hash, user_id, challenge, expires_at) VALUES (?, ?, ?, ?)").bind(
        await sha256(loginCode),
        userId,
        pending.challenge,
        now + LOGIN_CODE_TTL_MS,
      ),
    ]);
    return c.redirect(`${APP_CALLBACK}?code=${loginCode}`, 302);
  } catch (err) {
    const code = err instanceof ApiError && CALLBACK_ERRORS.has(err.code) ? err.code : "rso_failed";
    return c.redirect(`${APP_CALLBACK}?error=${code}`, 302);
  }
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
  return c.json(await createSession(c.env.DB, row.user_id));
});

/**
 * RSO 없이 친구 흐름을 로컬에서 돌려 보는 문입니다. `DEV_LOGIN`이 `"true"`이고 로컬 주소로 들어왔을 때만
 * 열립니다. 아무 PUUID로나 로그인할 수 있어서 배포 환경에서 열리면 남의 전적을 볼 수 있게 됩니다.
 */
auth.post("/dev", async (c) => {
  if (c.env.DEV_LOGIN !== "true" || !DEV_HOSTS.has(new URL(c.req.url).hostname)) throw new ApiError(404, "not_found");
  const body = await validate.jsonBody(c);
  const puuid = validate.puuid(body.puuid);
  const gameName = validate.text(body.gameName, 16, "invalid_game_name");
  const tagLine = validate.text(body.tagLine, 5, "invalid_tag_line");
  const userId = await upsertUser(c.env.DB, puuid, gameName, tagLine);
  return c.json(await createSession(c.env.DB, userId));
});

auth.post("/logout", requireSession, async (c) => {
  await c.env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(c.var.sessionHash).run();
  return c.body(null, 204);
});
