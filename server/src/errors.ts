import type { ContentfulStatusCode } from "hono/utils/http-status";

/** 던지면 `onError`가 `{ error: code }`로 바꿔 내려보냅니다. */
export class ApiError extends Error {
  constructor(
    readonly status: ContentfulStatusCode,
    readonly code: string,
    readonly headers?: Record<string, string>,
  ) {
    super(code);
  }
}
