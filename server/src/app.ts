import { Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";
import { logFailure } from "./log";
import { auth } from "./routes/auth";
import { content } from "./routes/content";
import { friends } from "./routes/friends";
import { invites } from "./routes/invites";
import { me } from "./routes/me";
import { pings } from "./routes/pings";
import { publicRoutes } from "./routes/public";
import { riot } from "./routes/riot";

// Riot, RSO, FCM이 답하지 않을 때 기다리는 시간이다.
// 끊지 않으면 같은 경로를 같이 기다리는 요청(riot.ts inflight)이 모두 Riot이 답할 때까지 묶인다.
// 본문을 받는 시간도 여기에 든다.
const UPSTREAM_TIMEOUT_MS = 8_000;
// 가장 큰 본문은 FCM 토큰(4096자까지)과 주간 요약(1KB 안팎)이다.
// 그보다 훨씬 큰 본문은 JSON으로 읽기 전에 끊는다.
// 읽고 파싱하는 것만으로 요청당 CPU 10ms를 넘길 수 있다.
const MAX_BODY_BYTES = 16 * 1024;

export interface Deps {
  /** 비워 두면 전역 `fetch`로 Riot과 FCM을 부릅니다. */
  fetch?: typeof fetch;
  /**
   * 밖으로 나가는 요청을 끊는 시간(ms)입니다.
   * 비워 두면 8초입니다.
   * 테스트가 답하지 않는 Riot을 흉내 낼 때 줄입니다.
   */
  timeoutMs?: number;
}

/**
 * 밖으로 나가는 요청은 모두 이 `fetch`를 거칩니다.
 * 정한 시간 안에 답하지 않으면 `TimeoutError`로 끊습니다.
 */
export function upstreamFetch(deps: Deps): typeof fetch {
  const base = deps.fetch ?? ((input, init) => fetch(input, init));
  const timeoutMs = deps.timeoutMs ?? UPSTREAM_TIMEOUT_MS;
  return (input, init) => {
    const timeout = AbortSignal.timeout(timeoutMs);
    return base(input, { ...init, signal: init?.signal ? AbortSignal.any([init.signal, timeout]) : timeout });
  };
}

export function createApp(deps: Deps = {}) {
  const upstream = upstreamFetch(deps);
  const app = new Hono<AppEnv>();

  app.use(bodyLimit({ maxSize: MAX_BODY_BYTES, onError: (c) => c.json({ error: "payload_too_large" }, 413) }));
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
  app.route("/pings", pings);

  app.notFound((c) => c.json({ error: "not_found" }, 404));

  app.onError((err, c) => {
    if (err instanceof ApiError) return c.json({ error: err.code }, err.status, err.headers);
    logFailure("unhandled", err);
    return c.json({ error: "internal" }, 500);
  });

  return app;
}
