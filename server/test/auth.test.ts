import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { base64url, randomToken, sha256 } from "../src/crypto";
import { makePuuid, setup } from "./helpers";

const TOKEN_URL = "https://auth.riotgames.com/token";
const ACCOUNT_ME_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/me";

function pkce() {
  const verifier = base64url(crypto.getRandomValues(new Uint8Array(32)));
  return { verifier, challenge: sha256(verifier) };
}

/** 앱이 Custom Tabs로 RSO를 띄우고 딥링크를 받기까지를 따라갑니다. */
async function rsoLogin(t: ReturnType<typeof setup>, account = { puuid: makePuuid(), gameName: "제트장인", tagLine: "KR1" }) {
  const { verifier, challenge } = pkce();
  t.upstream.json(TOKEN_URL, { access_token: "riot-access", refresh_token: "riot-refresh", token_type: "Bearer" });
  t.upstream.json(ACCOUNT_ME_URL, account);
  const start = await t.call("GET", `/auth/rso/start?challenge=${await challenge}`);
  const authorize = new URL(start.headers.get("Location")!);
  const state = authorize.searchParams.get("state")!;
  const callback = await t.call("GET", `/auth/rso/callback?code=riot-code&state=${state}`);
  const deepLink = new URL(callback.headers.get("Location")!);
  return { start, authorize, callback, deepLink, verifier, account, state };
}

describe("RSO 로그인", () => {
  it("Riot 로그인으로 보내고 콜백에서 앱 딥링크로 일회용 코드를 넘긴다", async () => {
    const t = setup();
    const { start, authorize, callback, deepLink } = await rsoLogin(t);

    expect(start.status).toBe(302);
    expect(authorize.origin + authorize.pathname).toBe("https://auth.riotgames.com/authorize");
    expect(authorize.searchParams.get("client_id")).toBe("test-client");
    expect(authorize.searchParams.get("response_type")).toBe("code");
    expect(authorize.searchParams.get("scope")).toBe("openid");
    expect(authorize.searchParams.get("redirect_uri")).toBe("http://localhost/auth/rso/callback");

    expect(callback.status).toBe(302);
    expect(deepLink.protocol).toBe("ovalit:");
    expect(deepLink.searchParams.get("code")).toMatch(/^[A-Za-z0-9_-]{43}$/);

    const [token] = t.upstream.callsTo(TOKEN_URL);
    expect(token!.method).toBe("POST");
    expect(token!.headers.get("Authorization")).toBe(`Basic ${btoa("test-client:test-secret")}`);
    const form = new URLSearchParams(new TextDecoder().decode(await token!.arrayBuffer()));
    expect(form.get("grant_type")).toBe("authorization_code");
    expect(form.get("code")).toBe("riot-code");
    expect(form.get("redirect_uri")).toBe("http://localhost/auth/rso/callback");
    expect(t.upstream.callsTo(ACCOUNT_ME_URL)[0]!.headers.get("Authorization")).toBe("Bearer riot-access");
  });

  it("코드와 verifier를 맞춰 보내야 세션이 나온다", async () => {
    const t = setup();
    const { deepLink, verifier, account } = await rsoLogin(t);
    const res = await t.call("POST", "/auth/session", undefined, { code: deepLink.searchParams.get("code"), verifier });
    expect(res.status).toBe(200);
    const { token, expiresAt } = await res.json<{ token: string; expiresAt: number }>();
    expect(expiresAt).toBeGreaterThan(Date.now() + 89 * 24 * 60 * 60 * 1000);

    const me = await t.call("GET", "/me", token);
    expect(await me.json()).toEqual({ puuid: account.puuid, gameName: "제트장인", tagLine: "KR1", statsPublic: true });
  });

  it("Riot 토큰은 어디에도 저장하지 않는다", async () => {
    const t = setup();
    await rsoLogin(t);
    const tables = ["users", "sessions", "auth_states", "login_codes"];
    for (const table of tables) {
      const { results } = await env.DB.prepare(`SELECT * FROM ${table}`).all();
      expect(JSON.stringify(results)).not.toContain("riot-access");
      expect(JSON.stringify(results)).not.toContain("riot-refresh");
    }
  });

  it("verifier가 틀리면 세션을 주지 않고 코드도 버린다", async () => {
    const t = setup();
    const { deepLink, verifier } = await rsoLogin(t);
    const code = deepLink.searchParams.get("code");
    const wrong = await t.call("POST", "/auth/session", undefined, { code, verifier: pkce().verifier });
    expect(wrong.status).toBe(400);
    expect(await wrong.json()).toEqual({ error: "invalid_code" });

    const retry = await t.call("POST", "/auth/session", undefined, { code, verifier });
    expect(retry.status).toBe(400);
  });

  it("쓴 코드는 다시 쓸 수 없다", async () => {
    const t = setup();
    const { deepLink, verifier } = await rsoLogin(t);
    const code = deepLink.searchParams.get("code");
    expect((await t.call("POST", "/auth/session", undefined, { code, verifier })).status).toBe(200);
    expect((await t.call("POST", "/auth/session", undefined, { code, verifier })).status).toBe(400);
  });

  it("2분이 지난 코드는 쓸 수 없다", async () => {
    const t = setup();
    const { deepLink, verifier } = await rsoLogin(t);
    const code = deepLink.searchParams.get("code")!;
    await env.DB.prepare("UPDATE login_codes SET expires_at = ? WHERE code_hash = ?")
      .bind(Date.now() - 1, await sha256(code))
      .run();
    expect((await t.call("POST", "/auth/session", undefined, { code, verifier })).status).toBe(400);
  });

  it("모르는 state로 들어온 콜백은 Riot을 부르지 않고 앱에 실패를 알린다", async () => {
    const t = setup();
    const res = await t.call("GET", `/auth/rso/callback?code=riot-code&state=${randomToken()}`);
    expect(res.headers.get("Location")).toBe("ovalit://auth?error=invalid_state");
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("state는 한 번만 쓸 수 있다", async () => {
    const t = setup();
    const { state } = await rsoLogin(t);
    const again = await t.call("GET", `/auth/rso/callback?code=riot-code&state=${state}`);
    expect(again.headers.get("Location")).toBe("ovalit://auth?error=invalid_state");
  });

  it("10분이 지난 state는 쓸 수 없다", async () => {
    const t = setup();
    const start = await t.call("GET", `/auth/rso/start?challenge=${await pkce().challenge}`);
    const state = new URL(start.headers.get("Location")!).searchParams.get("state")!;
    await env.DB.prepare("UPDATE auth_states SET created_at = ? WHERE state = ?")
      .bind(Date.now() - 11 * 60 * 1000, state)
      .run();
    const res = await t.call("GET", `/auth/rso/callback?code=riot-code&state=${state}`);
    expect(res.headers.get("Location")).toBe("ovalit://auth?error=invalid_state");
  });

  it("유저가 Riot 로그인을 취소하면 앱에 알린다", async () => {
    const t = setup();
    const res = await t.call("GET", "/auth/rso/callback?error=access_denied");
    expect(res.headers.get("Location")).toBe("ovalit://auth?error=access_denied");
  });

  it("Riot이 레이트 리밋에 걸리면 앱에 그대로 알린다", async () => {
    const t = setup();
    const start = await t.call("GET", `/auth/rso/start?challenge=${await pkce().challenge}`);
    const state = new URL(start.headers.get("Location")!).searchParams.get("state")!;
    t.upstream.on(TOKEN_URL, () => new Response(null, { status: 429, headers: { "Retry-After": "7" } }));
    const res = await t.call("GET", `/auth/rso/callback?code=riot-code&state=${state}`);
    expect(res.headers.get("Location")).toBe("ovalit://auth?error=riot_rate_limited");
  });

  it("다시 로그인하면 바뀐 Riot ID로 덮어쓴다", async () => {
    const t = setup();
    const puuid = makePuuid();
    await rsoLogin(t, { puuid, gameName: "옛이름", tagLine: "KR1" });
    await rsoLogin(t, { puuid, gameName: "새이름", tagLine: "KR2" });
    const { results } = await env.DB.prepare("SELECT game_name, tag_line FROM users WHERE puuid = ?").bind(puuid).all();
    expect(results).toEqual([{ game_name: "새이름", tag_line: "KR2" }]);
  });

  it("RSO 설정이 없으면 로그인을 띄우지 않는다", async () => {
    const t = setup({ RSO_CLIENT_ID: undefined, RSO_CLIENT_SECRET: undefined });
    const res = await t.call("GET", `/auth/rso/start?challenge=${await pkce().challenge}`);
    expect(res.status).toBe(503);
    expect(await res.json()).toEqual({ error: "rso_not_configured" });
  });

  it("challenge 모양이 틀리면 받지 않는다", async () => {
    const t = setup();
    const res = await t.call("GET", "/auth/rso/start?challenge=short");
    expect(res.status).toBe(400);
  });
});

describe("개발용 로그인", () => {
  const body = () => ({ puuid: makePuuid(), gameName: "dev", tagLine: "KR1" });

  it("DEV_LOGIN이 없으면 없는 경로처럼 군다", async () => {
    const t = setup({ DEV_LOGIN: undefined });
    const res = await t.call("POST", "/auth/dev", undefined, body());
    expect(res.status).toBe(404);
    expect(t.upstream.calls).toHaveLength(0);
  });

  it("DEV_LOGIN이 true가 아니면 열리지 않는다", async () => {
    const t = setup({ DEV_LOGIN: "false" });
    expect((await t.call("POST", "/auth/dev", undefined, body())).status).toBe(404);
  });

  it("DEV_LOGIN을 켜도 로컬 주소가 아니면 열리지 않는다", async () => {
    const t = setup();
    const res = await t.app.request(
      "https://ovalit.example.workers.dev/auth/dev",
      { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body()) },
      t.env,
    );
    expect(res.status).toBe(404);
  });

  it("켜져 있으면 Riot을 부르지 않고 세션을 준다", async () => {
    const t = setup();
    const res = await t.call("POST", "/auth/dev", undefined, body());
    expect(res.status).toBe(200);
    const { token } = await res.json<{ token: string }>();
    expect((await t.call("GET", "/me", token)).status).toBe(200);
    expect(t.upstream.calls).toHaveLength(0);
  });
});

describe("세션", () => {
  it("토큰이 없거나 모르는 토큰이면 401이다", async () => {
    const t = setup();
    expect((await t.call("GET", "/me")).status).toBe(401);
    expect((await t.call("GET", "/me", randomToken())).status).toBe(401);
  });

  it("저장소에는 토큰이 아니라 해시만 남는다", async () => {
    const t = setup();
    const user = await t.login();
    const row = await env.DB.prepare("SELECT token_hash FROM sessions WHERE token_hash = ?")
      .bind(await sha256(user.token))
      .first();
    expect(row).not.toBeNull();
    const plain = await env.DB.prepare("SELECT 1 FROM sessions WHERE token_hash = ?").bind(user.token).first();
    expect(plain).toBeNull();
  });

  it("만료된 세션은 401이고 지워진다", async () => {
    const t = setup();
    const user = await t.login();
    const hash = await sha256(user.token);
    await env.DB.prepare("UPDATE sessions SET expires_at = ? WHERE token_hash = ?").bind(Date.now() - 1, hash).run();
    expect((await t.call("GET", "/me", user.token)).status).toBe(401);
    expect(await env.DB.prepare("SELECT 1 FROM sessions WHERE token_hash = ?").bind(hash).first()).toBeNull();
  });

  it("로그아웃하면 그 토큰은 더 못 쓴다", async () => {
    const t = setup();
    const user = await t.login();
    expect((await t.call("POST", "/auth/logout", user.token)).status).toBe(204);
    expect((await t.call("GET", "/me", user.token)).status).toBe(401);
  });
});
