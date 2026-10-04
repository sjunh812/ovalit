import type { Context } from "hono";
import { base64url } from "./crypto";
import type { AppEnv, Env } from "./env";
import { MemoryCache } from "./memory";

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const JWT_TTL_SECONDS = 3600;
// 받은 액세스 토큰은 만료 5분 전까지 쓴다. 보내는 도중에 만료되지 않게 여유를 둔다.
const REFRESH_MARGIN_SECONDS = 5 * 60;
// 무료 플랜은 한 번 불릴 때 하위 요청이 50번까지다. 같은 요청에서 다른 데를 부를 몫을 남긴다.
const SUBREQUEST_BUDGET = 40;

// 서비스 계정마다 하나다. isolate가 바뀌면 비어 있어 한 번 더 받는다.
const accessTokens = new MemoryCache({ maxEntries: 4, maxBytes: 64 * 1024 });

/** 테스트끼리 액세스 토큰이 이어지지 않게 비울 때 씁니다. */
export function clearAccessTokens(): void {
  accessTokens.clear();
}

export type PushType = "ping_new" | "ping_reply" | "ping_time" | "ping_cancel" | "ping_remind" | "weekly_report";

/** FCM data 메시지는 값이 모두 문자열이어야 합니다. 숫자는 `String()`으로 바꿔 넣습니다. */
export type PushData = { type: PushType } & Record<string, string>;

export interface Notice {
  userId: number;
  data: PushData;
}

export interface Message {
  token: string;
  data: PushData;
  /** 받는 사람입니다. 보내기 전에 누구 몫인지 가를 때만 씁니다. */
  userId?: number;
}

interface ServiceAccount {
  projectId: string;
  clientEmail: string;
  privateKey: string;
}

interface FcmError {
  error?: {
    details?: { errorCode?: string; fieldViolations?: { field?: string }[] }[];
  };
}

/** `FCM_SERVICE_ACCOUNT`가 없거나 읽을 수 없으면 `undefined`입니다. 부르는 쪽은 그때 아무것도 보내지 않습니다. */
export function pushFrom(env: Env, upstream: typeof fetch): Push | undefined {
  const account = serviceAccount(env.FCM_SERVICE_ACCOUNT);
  return account && new Push(env.DB, account, upstream);
}

/**
 * 응답을 기다리게 하지 않고 뒤에서 보냅니다. 알림이 실패해도 요청은 그대로 성공합니다. `FCM_SERVICE_ACCOUNT`가 없으면
 * 토큰도 읽지 않습니다.
 */
export function notify(c: Context<AppEnv>, notices: Notice[]): void {
  if (notices.length === 0) return;
  const push = pushFrom(c.env, c.var.upstream);
  if (push) c.executionCtx.waitUntil(push.notify(notices));
}

/** 요청 하나나 크론 한 번 동안 씁니다. 하위 요청 몫을 그 안에서 셉니다. */
export class Push {
  private remaining = SUBREQUEST_BUDGET;

  constructor(
    private readonly db: D1Database,
    private readonly account: ServiceAccount,
    private readonly upstream: typeof fetch,
  ) {}

  /** 실패해도 던지지 않습니다. */
  async notify(notices: Notice[]): Promise<void> {
    try {
      await this.send(await this.messagesFor(notices));
    } catch (err) {
      logFailure("push", err);
    }
  }

  /** 받는 사람이 등록한 기기마다 메시지를 하나씩 만듭니다. 토큰이 없는 사람은 빠집니다. */
  async messagesFor(notices: Notice[]): Promise<Message[]> {
    if (notices.length === 0) return [];
    const userIds = [...new Set(notices.map((notice) => notice.userId))];
    const { results } = await this.db
      .prepare("SELECT token, user_id FROM push_tokens WHERE user_id IN (SELECT value FROM json_each(?))")
      .bind(JSON.stringify(userIds))
      .all<{ token: string; user_id: number }>();
    return notices.flatMap((notice) =>
      results
        .filter((row) => row.user_id === notice.userId)
        .map((row) => ({ token: row.token, data: notice.data, userId: notice.userId })),
    );
  }

  /** 남은 하위 요청 몫으로 메시지 `count`개를 다 보낼 수 있는지 봅니다. 액세스 토큰을 새로 받을 한 번은 남겨 둡니다. */
  fits(count: number): boolean {
    return count + 1 <= this.remaining;
  }

  /** 실패해도 던지지 않습니다. 몫을 넘는 메시지는 보내지 않습니다. */
  async send(messages: Message[]): Promise<void> {
    if (messages.length === 0) return;
    try {
      const accessToken = await this.accessToken();
      if (!accessToken) return;
      const dead: string[] = [];
      await Promise.all(
        messages.map(async ({ token, data }) => {
          const res = await this.post({ token, data, android: { priority: "HIGH", ttl: "3600s" } }, accessToken);
          if (res && (await isDeadToken(res))) dead.push(token);
        }),
      );
      if (dead.length > 0) {
        await this.db.batch(dead.map((token) => this.db.prepare("DELETE FROM push_tokens WHERE token = ?").bind(token)));
      }
    } catch (err) {
      logFailure("push", err);
    }
  }

  /** 실패해도 던지지 않습니다. 주제를 구독한 기기 모두에 갑니다. 사용자 수와 상관없이 요청 하나입니다. */
  async toTopic(topic: string, data: PushData): Promise<void> {
    try {
      const accessToken = await this.accessToken();
      if (!accessToken) return;
      const res = await this.post({ topic, data, android: { priority: "NORMAL" } }, accessToken);
      if (res && !res.ok) console.error("push_topic", res.status);
      await res?.body?.cancel();
    } catch (err) {
      logFailure("push_topic", err);
    }
  }

  private take(): boolean {
    if (this.remaining <= 0) {
      console.error("push_budget");
      return false;
    }
    this.remaining--;
    return true;
  }

  private async post(message: Record<string, unknown>, accessToken: string): Promise<Response | undefined> {
    if (!this.take()) return undefined;
    const url = `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(this.account.projectId)}/messages:send`;
    try {
      const res = await this.upstream(url, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ message }),
      });
      // 액세스 토큰이 먼저 끊긴 것이다. 다음 알림은 새로 받아 보낸다.
      if (res.status === 401) accessTokens.delete(this.account.clientEmail);
      if (!res.ok && res.status !== 404) console.error("push_send", res.status);
      return res;
    } catch (err) {
      logFailure("push_send", err);
      return undefined;
    }
  }

  private async accessToken(): Promise<string | undefined> {
    const key = this.account.clientEmail;
    const cached = accessTokens.get(key);
    if (cached !== undefined) return cached;
    if (!this.take()) return undefined;
    const assertion = await signJwt(this.account, Math.floor(Date.now() / 1000));
    const res = await this.upstream(TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }).toString(),
    });
    if (!res.ok) {
      console.error("push_token", res.status);
      await res.body?.cancel();
      return undefined;
    }
    const body = await res.json<{ access_token?: unknown; expires_in?: unknown }>();
    if (typeof body.access_token !== "string") return undefined;
    const ttl = (typeof body.expires_in === "number" ? body.expires_in : 0) - REFRESH_MARGIN_SECONDS;
    if (ttl > 0) accessTokens.set(key, body.access_token, ttl);
    return body.access_token;
  }
}

function serviceAccount(raw: string | undefined): ServiceAccount | undefined {
  if (!raw) return undefined;
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    parsed = null;
  }
  const json = (typeof parsed === "object" && parsed !== null ? parsed : {}) as Record<string, unknown>;
  const { project_id: projectId, client_email: clientEmail, private_key: privateKey } = json;
  if (typeof projectId !== "string" || typeof clientEmail !== "string" || typeof privateKey !== "string") {
    console.error("push_service_account_invalid");
    return undefined;
  }
  return { projectId, clientEmail, privateKey };
}

async function signJwt(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const header = encodeJson({ alg: "RS256", typ: "JWT" });
  const claims = encodeJson({
    iss: account.clientEmail,
    scope: SCOPE,
    aud: TOKEN_URL,
    iat: nowSeconds,
    exp: nowSeconds + JWT_TTL_SECONDS,
  });
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(account.privateKey),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));
  return `${header}.${claims}.${base64url(new Uint8Array(signature))}`;
}

function encodeJson(value: unknown): string {
  return base64url(new TextEncoder().encode(JSON.stringify(value)));
}

// 비밀값을 붙여 넣다 줄바꿈이 `\n` 두 글자로 남는 일이 있어 그것도 걷어낸다.
function pemToDer(pem: string): Uint8Array {
  const base64 = pem
    .replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "")
    .replaceAll("\\n", "")
    .replace(/\s+/g, "");
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
}

/**
 * 다시 보내도 안 닿는 토큰인지 봅니다. 앱을 지웠거나 토큰이 바뀌면 404 `UNREGISTERED`가 옵니다. 형식이 틀린 토큰은
 * 400 `INVALID_ARGUMENT`인데, 우리가 보낸 내용이 틀려도 같은 코드가 와서 그때는 BadRequest가 토큰 말고 다른 필드를
 * 짚습니다. 그런 응답에는 토큰을 지우지 않습니다. 잘못 보낸 알림 하나에 모두의 토큰이 지워지면 안 됩니다.
 */
async function isDeadToken(res: Response): Promise<boolean> {
  if (res.ok) {
    await res.body?.cancel();
    return false;
  }
  if (res.status === 404) {
    await res.body?.cancel();
    return true;
  }
  const body = (await res.json().catch(() => null)) as FcmError | null;
  const details = body?.error?.details ?? [];
  const codes = details.map((detail) => detail.errorCode);
  if (codes.includes("UNREGISTERED")) return true;
  if (!codes.includes("INVALID_ARGUMENT")) return false;
  const fields = details.flatMap((detail) => detail.fieldViolations ?? []).map((violation) => violation.field);
  return fields.every((field) => field === "message.token");
}

// D1 에러 메시지에는 SQL이 섞이고 FCM 에러에는 토큰이 섞일 수 있어 이름만 남긴다.
function logFailure(label: string, err: unknown): void {
  console.error(label, err instanceof Error ? err.name : typeof err);
}
