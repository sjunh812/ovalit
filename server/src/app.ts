import { Hono } from "hono";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";

export interface Deps {
  /** 비워 두면 전역 `fetch`로 Riot을 부릅니다. */
  fetch?: typeof fetch;
}

export function createApp(deps: Deps = {}) {
  const upstream: typeof fetch = deps.fetch ?? ((input, init) => fetch(input, init));
  const app = new Hono<AppEnv>();

  app.use(async (c, next) => {
    c.set("upstream", upstream);
    await next();
  });

  app.get("/health", (c) => c.json({ ok: true }));

  app.notFound((c) => c.json({ error: "not_found" }, 404));

  app.onError((err, c) => {
    if (err instanceof ApiError) return c.json({ error: err.code }, err.status, err.headers);
    // 요청 본문과 헤더에 토큰이 실리므로 에러 객체를 통째로 찍지 않는다.
    console.error("unhandled", err.name);
    return c.json({ error: "internal" }, 500);
  });

  return app;
}
