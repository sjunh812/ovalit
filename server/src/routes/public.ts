import { Hono } from "hono";
import { html } from "hono/html";
import { INVITE_CODE } from "../crypto";
import type { AppEnv } from "../env";
import { ANDROID_PACKAGE, appIntent, appPage } from "../page";

// keytool이 찍는 모양 그대로다. 콜론으로 이은 16진수 32바이트.
const CERT_FINGERPRINT = /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/;
// public/og/의 정적 파일이다.
// 카카오톡은 미리보기 그림을 주소로 기억하니, 그림을 바꾸면 scripts/build-og-image.mjs의 VERSION과 같이 올려 새 주소로 낸다.
const OG_IMAGE_PATH = "/og/invite-v1.png";
const INVITE_TITLE = "오발있 친구 초대";
const INVITE_TEXT = "친구가 오발있에서 같이 기록을 보자고 초대했어요";

/** 전적에도 Riot에도 닿지 않는 것만 둡니다. */
export const publicRoutes = new Hono<AppEnv>();

publicRoutes.get("/health", (c) => c.json({ ok: true }));

/**
 * 안드로이드가 App Link를 검증할 때 읽는 파일입니다. 여기 적힌 서명의 `com.ovalit`만 `/auth/done`을 엽니다.
 * 지문은 비밀이 아니지만 키마다 달라서 저장소에 두지 않고 `ANDROID_CERT_SHA256`으로 받습니다.
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
 * 앱이 없는 기기에서 초대 링크를 열면 보이는 페이지입니다.
 * DB를 읽지 않으니 코드가 살아 있는지는 알려주지 않습니다. 밖에서 불러오는 것도 없습니다.
 *
 * 단톡방에 올리면 카카오톡이 이 페이지의 Open Graph 태그로 미리보기 카드를 만듭니다.
 * 그림도 우리 서버의 정적 파일이고, 카드에는 누가 초대했는지 적지 않습니다.
 */
publicRoutes.get("/i/:code", (c) => {
  const code = c.req.param("code").toUpperCase();
  if (!INVITE_CODE.test(code)) return c.notFound();
  const url = new URL(c.req.url);
  return appPage(c, {
    title: INVITE_TITLE,
    head: html`<meta name="description" content="${INVITE_TEXT}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="오발있">
<meta property="og:locale" content="ko_KR">
<meta property="og:title" content="${INVITE_TITLE}">
<meta property="og:description" content="${INVITE_TEXT}">
<meta property="og:url" content="${url.origin}/i/${code}">
<meta property="og:image" content="${url.origin}${OG_IMAGE_PATH}">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="오발있 로고">
<meta name="twitter:card" content="summary_large_image">`,
    main: html`<p>${INVITE_TEXT}.</p>
<a href="${appIntent(url, `/i/${code}`)}">앱에서 열기</a>`,
  });
});
