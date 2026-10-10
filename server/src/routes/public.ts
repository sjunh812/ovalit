import { Hono } from "hono";
import { html } from "hono/html";
import { INVITE_CODE } from "../crypto";
import type { AppEnv } from "../env";
import { ANDROID_PACKAGE, appIntent, appPage } from "../page";

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
  return appPage(c, {
    title: "오발있 친구 초대",
    main: html`<p>친구가 오발있에서 같이 기록을 보자고 초대했어요.</p>
<a href="${appIntent(new URL(c.req.url), `/i/${code}`)}">앱에서 열기</a>`,
  });
});
