import { ApiError } from "./errors";
import { send } from "./upstream";
import { PUUID } from "./validate";

const AUTHORIZE_URL = "https://auth.riotgames.com/authorize";
const TOKEN_URL = "https://auth.riotgames.com/token";
const ACCOUNT_ME_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/me";

export function authorizeUrl(clientId: string, redirectUri: string, state: string): string {
  const query = new URLSearchParams({
    redirect_uri: redirectUri,
    client_id: clientId,
    response_type: "code",
    scope: "openid",
    state,
  });
  return `${AUTHORIZE_URL}?${query}`;
}

/** access token은 계정을 한 번 읽는 데만 쓰고 버립니다. 같이 오는 refresh token도 저장하지 않습니다. */
export async function readAccount(
  upstream: typeof fetch,
  client: { id: string; secret: string },
  code: string,
  redirectUri: string,
): Promise<{ puuid: string; gameName: string; tagLine: string }> {
  const tokenRes = await send(upstream, TOKEN_URL, {
    method: "POST",
    headers: {
      Authorization: `Basic ${btoa(`${client.id}:${client.secret}`)}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({ grant_type: "authorization_code", code, redirect_uri: redirectUri }).toString(),
  });
  const token: unknown = await tokenRes.json().catch(() => null);
  const accessToken = (token as { access_token?: unknown } | null)?.access_token;
  if (typeof accessToken !== "string" || accessToken.length === 0) throw new ApiError(502, "rso_failed");

  const accountRes = await send(upstream, ACCOUNT_ME_URL, { headers: { Authorization: `Bearer ${accessToken}` } });
  const account = (await accountRes.json().catch(() => null)) as Record<string, unknown> | null;
  const { puuid, gameName, tagLine } = account ?? {};
  if (typeof puuid !== "string" || !PUUID.test(puuid) || typeof gameName !== "string" || typeof tagLine !== "string") {
    throw new ApiError(502, "rso_failed");
  }
  return { puuid, gameName, tagLine };
}
