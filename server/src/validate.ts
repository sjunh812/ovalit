import type { Context } from "hono";
import { ApiError } from "./errors";

// Riot 문서가 PUUID를 78자로 못박아 둔다. 경로에 그대로 붙이므로 `/`나 `?`가 섞이지 않게 먼저 거른다.
export const PUUID = /^[A-Za-z0-9_-]{78}$/;
const MATCH_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const TOKEN = /^[A-Za-z0-9_-]{43}$/;
// RFC 7636의 code_verifier 규칙이다.
const VERIFIER = /^[A-Za-z0-9._~-]{43,128}$/;

export function puuid(value: unknown): string {
  if (typeof value !== "string" || !PUUID.test(value)) throw new ApiError(400, "invalid_puuid");
  return value;
}

/** 캐시 키가 갈리지 않게 소문자로 맞춰 돌려줍니다. */
export function matchId(value: unknown): string {
  if (typeof value !== "string" || !MATCH_ID.test(value)) throw new ApiError(400, "invalid_match_id");
  return value.toLowerCase();
}

export function token(value: unknown, code: string): string {
  if (typeof value !== "string" || !TOKEN.test(value)) throw new ApiError(400, code);
  return value;
}

export function verifier(value: unknown): string {
  if (typeof value !== "string" || !VERIFIER.test(value)) throw new ApiError(400, "invalid_verifier");
  return value;
}

export function text(value: unknown, maxLength: number, code: string): string {
  if (typeof value !== "string" || value.length === 0 || value.length > maxLength) throw new ApiError(400, code);
  return value;
}

export async function jsonBody(c: Context): Promise<Record<string, unknown>> {
  const body: unknown = await c.req.json().catch(() => null);
  if (typeof body !== "object" || body === null || Array.isArray(body)) throw new ApiError(400, "invalid_body");
  return body as Record<string, unknown>;
}
