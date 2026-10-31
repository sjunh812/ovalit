import { expect } from "vitest";
import { randomToken } from "../src/crypto";
import type { setup, TestUser } from "./helpers";

type T = ReturnType<typeof setup>;

export const FCM_PROJECT = "ovalit-test";
export const FCM_CLIENT_EMAIL = "push@ovalit-test.iam.gserviceaccount.com";
export const FCM_TOKEN_URL = "https://oauth2.googleapis.com/token";
export const FCM_SEND_URL = `https://fcm.googleapis.com/v1/projects/${FCM_PROJECT}/messages:send`;

/** FCM에 보낸 메시지 하나입니다. */
export interface Sent {
  token?: string;
  topic?: string;
  data: Record<string, string>;
  android: unknown;
}

export interface FcmAccount {
  keys: CryptoKeyPair;
  /** `FCM_SERVICE_ACCOUNT`에 넣을 JSON입니다. */
  serviceAccount: string;
}

let account: Promise<FcmAccount> | undefined;

/** 테스트용 서비스 계정입니다. RSA 키는 만드는 데 시간이 걸려 파일마다 한 번만 만듭니다. */
export function fcmServiceAccount(): Promise<FcmAccount> {
  account ??= makeAccount();
  return account;
}

async function makeAccount(): Promise<FcmAccount> {
  const keys = (await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  )) as CryptoKeyPair;
  const der = new Uint8Array((await crypto.subtle.exportKey("pkcs8", keys.privateKey)) as ArrayBuffer);
  const lines = btoa(String.fromCharCode(...der)).match(/.{1,64}/g)!.join("\n");
  const serviceAccount = JSON.stringify({
    type: "service_account",
    project_id: FCM_PROJECT,
    client_email: FCM_CLIENT_EMAIL,
    private_key: `-----BEGIN PRIVATE KEY-----\n${lines}\n-----END PRIVATE KEY-----\n`,
  });
  return { keys, serviceAccount };
}

/** FCM 대신 답하고 보낸 메시지를 모읍니다. `answer`로 토큰마다 응답을 바꿉니다. */
export function fakeFcm(t: T, answer: (token: string) => Response = () => Response.json({ name: "projects/x/messages/1" })): Sent[] {
  const sent: Sent[] = [];
  t.upstream.json(FCM_TOKEN_URL, { access_token: "fcm-access", expires_in: 3599, token_type: "Bearer" });
  t.upstream.on(FCM_SEND_URL, async (req) => {
    const { message } = await req.json<{ message: Sent }>();
    sent.push(message);
    return message.token ? answer(message.token) : Response.json({ name: "projects/x/messages/2" });
  });
  return sent;
}

export async function registerToken(t: T, user: TestUser, token = `device-${randomToken(8)}`): Promise<string> {
  expect((await t.call("PUT", "/me/push-token", user.token, { token })).status).toBe(204);
  return token;
}
