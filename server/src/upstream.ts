import { ApiError } from "./errors";

/**
 * Riot으로 요청을 보내고 실패를 앱이 읽을 코드로 바꿉니다. 앱은 레이트 리밋만 기다렸다 다시 부르고,
 * 키가 막힌 경우(401·403)는 우리 쪽 문제라 502로 구분합니다.
 */
export async function send(upstream: typeof fetch, url: string, init?: RequestInit): Promise<Response> {
  let res: Response;
  try {
    res = await upstream(url, init);
  } catch {
    throw new ApiError(502, "riot_unavailable");
  }
  if (res.ok) return res;
  if (res.status === 429) {
    const retryAfter = res.headers.get("Retry-After");
    throw new ApiError(503, "riot_rate_limited", retryAfter ? { "Retry-After": retryAfter } : undefined);
  }
  if (res.status === 404) throw new ApiError(404, "not_found");
  throw new ApiError(502, "riot_unavailable");
}
