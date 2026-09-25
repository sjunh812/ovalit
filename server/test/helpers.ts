import { env } from "cloudflare:workers";
import { createApp } from "../src/app";
import { base64url } from "../src/crypto";
import type { Env } from "../src/env";

// 실제 키는 쓰지 않는다. 테스트는 .dev.vars를 읽지 않도록 값을 여기서 덮어쓴다.
const TEST_SECRETS = {
  RIOT_API_KEY: "test-riot-key",
  RSO_CLIENT_ID: "test-client",
  RSO_CLIENT_SECRET: "test-secret",
  DEV_LOGIN: "true",
} satisfies Partial<Env>;

export function makePuuid(): string {
  // 58바이트를 base64url로 바꾸면 PUUID와 같은 78자가 된다.
  return base64url(crypto.getRandomValues(new Uint8Array(58)));
}

type Handler = (req: Request) => Response | Promise<Response>;

/** Riot 대신 응답하고 어떤 요청이 나갔는지 적어 둡니다. 등록하지 않은 주소는 500을 돌려줍니다. */
export class FakeUpstream {
  readonly calls: Request[] = [];
  private readonly routes = new Map<string, Handler>();

  on(url: string, handler: Handler): this {
    this.routes.set(url, handler);
    return this;
  }

  json(url: string, body: unknown, status = 200): this {
    return this.on(url, () => Response.json(body, { status }));
  }

  callsTo(url: string): Request[] {
    return this.calls.filter((req) => req.url === url);
  }

  readonly fetch: typeof fetch = async (input, init) => {
    const req = new Request(input, init);
    this.calls.push(req.clone());
    const handler = this.routes.get(req.url);
    return handler ? handler(req) : new Response("unexpected upstream call", { status: 500 });
  };
}

export interface TestUser {
  puuid: string;
  gameName: string;
  tagLine: string;
  token: string;
}

export function setup(overrides: Partial<Env> = {}) {
  const upstream = new FakeUpstream();
  const app = createApp({ fetch: upstream.fetch });
  const testEnv: Env = { ...env, ...TEST_SECRETS, ...overrides };

  function call(method: string, path: string, token?: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    return Promise.resolve(
      app.request(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) }, testEnv),
    );
  }

  async function login(gameName = "tester", puuid = makePuuid()): Promise<TestUser> {
    const res = await call("POST", "/auth/dev", undefined, { puuid, gameName, tagLine: "KR1" });
    if (res.status !== 200) throw new Error(`dev login failed: ${res.status}`);
    const { token } = await res.json<{ token: string }>();
    return { puuid, gameName, tagLine: "KR1", token };
  }

  return { upstream, app, env: testEnv, call, login };
}

export async function userId(puuid: string): Promise<number> {
  const row = await env.DB.prepare("SELECT id FROM users WHERE puuid = ?").bind(puuid).first<{ id: number }>();
  if (!row) throw new Error("no such user");
  return row.id;
}
