import { Hono } from "hono";
import { html } from "hono/html";
import { INVITE_CODE } from "../crypto";
import type { AppEnv } from "../env";
import { ANDROID_PACKAGE } from "./auth";

// keytool이 찍는 모양 그대로다. 콜론으로 이은 16진수 32바이트.
const CERT_FINGERPRINT = /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/;

/** 전적에도 Riot에도 닿지 않는 것만 둡니다. */
export const publicRoutes = new Hono<AppEnv>();

publicRoutes.get("/health", (c) => c.json({ ok: true }));

/**
 * 안드로이드가 App Link를 검증할 때 읽는 파일입니다. 여기 적힌 서명의 `com.ovalit`만 `/auth/done`을 엽니다. 지문은
 * 비밀이 아니지만 키마다 달라서 저장소에 두지 않고 `ANDROID_CERT_SHA256`으로 받습니다.
 */
publicRoutes.get("/.well-known/assetlinks.json", (c) => {
  const fingerprints = (c.env.ANDROID_CERT_SHA256 ?? "")
    .split(",")
    .map((value) => value.trim().toUpperCase())
    .filter((value) => CERT_FINGERPRINT.test(value));
  if (fingerprints.length === 0) return c.notFound();
  return c.json([
    {
      relation: ["delegate_permission/common.handle_all_urls"],
      target: { namespace: "android_app", package_name: ANDROID_PACKAGE, sha256_cert_fingerprints: fingerprints },
    },
  ]);
});

/**
 * 앱이 없는 기기에서 초대 링크를 열면 보이는 페이지입니다. DB를 읽지 않으니 코드가 살아 있는지는 알려주지
 * 않습니다. 밖에서 불러오는 것도 없습니다.
 */
publicRoutes.get("/i/:code", (c) => {
  const code = c.req.param("code").toUpperCase();
  if (!INVITE_CODE.test(code)) return c.notFound();
  // 커스텀 스킴(ovalit://)은 다른 앱이 같은 이름을 등록해 가로챌 수 있어 쓰지 않는다(docs/backend.md). /auth/done처럼 이 주소를
  // com.ovalit에만 넘기는 intent로 연다.
  const url = new URL(c.req.url);
  const intent = `intent://${url.host}/i/${code}#Intent;scheme=${url.protocol.replace(":", "")};package=${ANDROID_PACKAGE};end`;
  c.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'");
  c.header("Referrer-Policy", "no-referrer");
  c.header("X-Robots-Tag", "noindex");
  return c.html(html`<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>오발있 친구 초대</title>
<style>
:root { --bg: #101012; --t1: #F2F2F4; --t2: #9E9EA6; --accent: #FF4655; --on-accent: #FFFFFF; }
@media (prefers-color-scheme: light) { :root { --bg: #FFFFFF; --t1: #191F28; --t2: #5F6873; } }
body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg); color: var(--t1); font-family: system-ui, sans-serif; }
main { padding: 24px 16px; text-align: center; }
h1 { margin: 0 0 8px; font-size: 28px; }
p { margin: 0 0 24px; color: var(--t2); font-size: 15px; line-height: 1.5; }
a { display: inline-block; padding: 14px 28px; border-radius: 12px; background: var(--accent);
  color: var(--on-accent); font-weight: 600; text-decoration: none; }
</style>
</head>
<body>
<main>
<h1>오발있?</h1>
<p>친구가 오발있에서 같이 기록을 보자고 초대했어요.</p>
<a href="${intent}">앱에서 열기</a>
</main>
</body>
</html>`);
});
