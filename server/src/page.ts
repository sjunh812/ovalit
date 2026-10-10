import type { Context } from "hono";
import { html } from "hono/html";
import type { HtmlEscapedString } from "hono/utils/html";

type Html = HtmlEscapedString | Promise<HtmlEscapedString>;

export const ANDROID_PACKAGE = "com.ovalit";

/**
 * 같은 주소를 우리 패키지에만 넘기는 intent 주소입니다. 앱이 없으면 아무 데도 열리지 않습니다. 커스텀 스킴(`ovalit://`)은
 * 다른 앱이 같은 이름을 등록해 가로챌 수 있어 쓰지 않습니다(docs/backend.md).
 *
 * `pathAndQuery`는 `/`로 시작하고 이미 모양을 확인한 값만 넘깁니다.
 */
export function appIntent(pageUrl: URL, pathAndQuery: string): string {
  const scheme = pageUrl.protocol.replace(":", "");
  return `intent://${pageUrl.host}${pathAndQuery}#Intent;scheme=${scheme};package=${ANDROID_PACKAGE};end`;
}

/**
 * 앱이 안 열린 기기에서 브라우저에 뜨는 작은 페이지입니다(`/auth/done`, `/i/:code`). 밖에서 불러오는 것이 없고 스크립트도
 * 없습니다. `main`은 제목 밑에 들어갈 본문이고, `head`에는 미리보기 태그처럼 머리에 더할 것을 넣습니다.
 */
export function appPage(c: Context, page: { title: string; head?: Html; main: Html }): Response | Promise<Response> {
  c.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'");
  c.header("Referrer-Policy", "no-referrer");
  c.header("X-Robots-Tag", "noindex");
  return c.html(html`<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${page.title}</title>
${page.head ?? ""}
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
${page.main}
</main>
</body>
</html>`);
}
