import { base64url, fromBase64url, randomToken } from "./crypto";
import { MemoryCache } from "./memory";

/** state를 받는 시간입니다. 그 안에 Riot 로그인을 마쳐야 합니다. */
export const STATE_TTL_MS = 10 * 60 * 1000;
// HMAC 키로 쓰기에 짧은 값은 받지 않는다. `openssl rand -base64 32`가 44자를 준다.
const MIN_SECRET_LENGTH = 32;
// 다른 isolate의 시계가 조금 늦어 만든 시각이 지금보다 앞설 수 있다.
const CLOCK_SKEW_MS = 60 * 1000;
// challenge.만든시각.임의값.서명이다. 시각은 epoch ms를 36진수로 적었다. 모두 주소에 그대로 실리는 글자다.
const STATE = /^([A-Za-z0-9_-]{43})\.([0-9a-z]{1,12})\.([A-Za-z0-9_-]{16})\.([A-Za-z0-9_-]{43})$/;

// 같은 state가 다시 오면 막는다.
// isolate 메모리라 다른 isolate로 간 재사용은 못 막지만, 그때도 Riot 인가 코드는 한 번만 쓸 수 있어 로그인 코드가 두 번 나오지 않는다.
const used = new MemoryCache({ maxEntries: 10_000, maxBytes: Number.POSITIVE_INFINITY });
let cachedKey: { secret: string; key: Promise<CryptoKey> } | undefined;

/** 테스트끼리 쓴 state가 이어지지 않게 비울 때 씁니다. */
export function clearUsedStates(): void {
  used.clear();
}

/** `STATE_SECRET`이 없거나 32자보다 짧으면 `undefined`입니다. 그때는 RSO를 띄우지 않습니다. */
export function stateSecret(value: string | undefined): string | undefined {
  return value && value.length >= MIN_SECRET_LENGTH ? value : undefined;
}

/**
 * 앱이 넘긴 challenge를 담아 서명한 state를 만듭니다. D1에 적지 않고 콜백이 서명으로 확인합니다.
 * 로그인 시작은 세션 없이 열려 있어서, 시작마다 D1에 쓰면 누구나 되풀이해 불러 하루 쓰기 한도를 다 쓰게 할 수 있습니다.
 */
export async function signState(secret: string, challenge: string, now: number): Promise<string> {
  const payload = `${challenge}.${now.toString(36)}.${randomToken(12)}`;
  const signature = await crypto.subtle.sign("HMAC", await hmacKey(secret), new TextEncoder().encode(payload));
  return `${payload}.${base64url(new Uint8Array(signature))}`;
}

/**
 * 서명이 맞고 10분이 안 지났고 이 isolate에서 처음 보는 state면 담긴 challenge를, 아니면 `undefined`를 돌려줍니다.
 * 받은 state는 쓴 것으로 적어 둡니다.
 */
export async function openState(secret: string, state: string, now: number): Promise<string | undefined> {
  const match = STATE.exec(state);
  if (!match) return undefined;
  const [, challenge, issued, nonce, signature] = match as unknown as [string, string, string, string, string];
  const issuedAt = Number.parseInt(issued, 36);
  if (issuedAt - now > CLOCK_SKEW_MS || now - issuedAt >= STATE_TTL_MS) return undefined;
  const payload = new TextEncoder().encode(`${challenge}.${issued}.${nonce}`);
  if (!(await crypto.subtle.verify("HMAC", await hmacKey(secret), fromBase64url(signature), payload))) return undefined;
  if (used.get(nonce) !== undefined) return undefined;
  used.set(nonce, "", Math.ceil((issuedAt + STATE_TTL_MS + CLOCK_SKEW_MS - now) / 1000));
  return challenge;
}

function hmacKey(secret: string): Promise<CryptoKey> {
  if (cachedKey?.secret !== secret) {
    const key = crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, [
      "sign",
      "verify",
    ]);
    cachedKey = { secret, key };
  }
  return cachedKey.key;
}
