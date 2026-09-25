import { Hono } from "hono";
import { html } from "hono/html";
import { INVITE_CODE } from "../crypto";
import type { AppEnv } from "../env";

/** 전적에도 Riot에도 닿지 않는 것만 둡니다. */
export const publicRoutes = new Hono<AppEnv>();

publicRoutes.get("/health", (c) => c.json({ ok: true }));

/**
 * 앱이 없는 곳에서 초대 링크를 열었을 때 보이는 쪽입니다. DB를 읽지 않아서 코드가 살아 있는지 알려주지
 * 않고, 밖에서 불러오는 것도 없습니다.
 */
publicRoutes.get("/i/:code", (c) => {
  const code = c.req.param("code").toUpperCase();
  if (!INVITE_CODE.test(code)) return c.notFound();
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
:root { --bg: #100E0C; --t1: #F5F2ED; --t2: #9A9289; --accent: #E0B252; --on-accent: #100E0C; }
@media (prefers-color-scheme: light) { :root { --bg: #FDFCFA; --t1: #191510; --t2: #6E675E; } }
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
<a href="ovalit://invite/${code}">앱에서 열기</a>
</main>
</body>
</html>`);
});
