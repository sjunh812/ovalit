import { Hono } from "hono";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";
import { auth } from "./routes/auth";
import { content } from "./routes/content";
import { friends } from "./routes/friends";
import { invites } from "./routes/invites";
import { me } from "./routes/me";
import { publicRoutes } from "./routes/public";
import { riot } from "./routes/riot";

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

  app.route("/", publicRoutes);
  app.route("/", content);
  app.route("/auth", auth);
  app.route("/me", me);
  app.route("/riot", riot);
  app.route("/friends", friends);
  app.route("/invites", invites);

  app.notFound((c) => c.json({ error: "not_found" }, 404));

  app.onError((err, c) => {
    if (err instanceof ApiError) return c.json({ error: err.code }, err.status, err.headers);
    // 요청 본문과 헤더에 토큰이 실리므로 에러 객체를 통째로 찍지 않는다.
    console.error("unhandled", err.name);
    return c.json({ error: "internal" }, 500);
  });

  return app;
}
