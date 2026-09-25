export function base64url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

/** 32바이트면 base64url 43자입니다. 세션 토큰, 로그인 코드, RSO state에 씁니다. */
export function randomToken(bytes = 32): string {
  return base64url(crypto.getRandomValues(new Uint8Array(bytes)));
}

export async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return base64url(new Uint8Array(digest));
}

// 0, O, 1, I를 뺐다. 링크가 잘려 사람이 옮겨 적을 때 헷갈리지 않게 한다.
// 글자가 32개라 바이트를 32로 나눈 나머지를 써도 어느 글자로 치우치지 않는다.
const INVITE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
export const INVITE_CODE = /^[2-9A-HJ-NP-Z]{12}$/;

export function inviteCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(12));
  return Array.from(bytes, (byte) => INVITE_ALPHABET[byte % 32]).join("");
}
