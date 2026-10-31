import { createMiddleware } from "hono/factory";
import type { AppEnv } from "./env";
import { ApiError } from "./errors";
import { Quota } from "./memory";

const registered: Quota[] = [];

/**
 * 한도를 한 번 씁니다.
 * 다 썼으면 창이 끝날 때까지 남은 초(`Retry-After`)를 붙여 429 `too_many_requests`를 던집니다.
 */
export function spend(quota: Quota, key: string): void {
  const retryAfter = quota.take(key);
  if (retryAfter !== undefined) throw new ApiError(429, "too_many_requests", { "Retry-After": String(retryAfter) });
}

/**
 * 사람마다 `windowMs`에 `limit`번까지 받는 미들웨어입니다.
 * `requireSession` 뒤에 겁니다.
 * 넘으면 본문을 읽기 전에 429 `too_many_requests`와 `Retry-After`를 줍니다.
 * isolate 메모리로 세니 막는다기보다 줄이는 장치입니다.
 */
export function perUser(limit: number, windowMs: number) {
  const quota = new Quota(limit, windowMs);
  registered.push(quota);
  return createMiddleware<AppEnv>(async (c, next) => {
    spend(quota, String(c.var.user.id));
    await next();
  });
}

/** 테스트끼리 사람마다 센 횟수가 이어지지 않게 비울 때 씁니다. */
export function clearUserLimits(): void {
  for (const quota of registered) quota.clear();
}
