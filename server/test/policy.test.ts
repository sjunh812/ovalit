import { describe, expect, it } from "vitest";
import { makePuuid, setup } from "./helpers";

// 세션 없이 열리는 경로는 이것뿐이다. 늘리려면 전적에 닿지 않는지 먼저 따지고 여기를 고친다.
const PUBLIC = new Set([
  "GET /health",
  "GET /content",
  "GET /status",
  "GET /content/tiers",
  "GET /content/roles",
  "GET /i/:code",
  "GET /auth/rso/start",
  "GET /auth/rso/callback",
  "POST /auth/session",
  "POST /auth/dev",
]);

function concrete(path: string): string {
  return path
    .replace(":puuid", makePuuid())
    .replace(":matchId", crypto.randomUUID())
    .replace(":code", "ABCDEFGHJKMN");
}

describe("지켜야 할 선", () => {
  const t = setup();
  const routes = [
    ...new Set(t.app.routes.filter((route) => route.method !== "ALL").map((route) => `${route.method} ${route.path}`)),
  ];

  it("세션 없이 열리는 경로는 정해 둔 것뿐이다", () => {
    const open = routes.filter((route) => PUBLIC.has(route));
    expect(new Set(open)).toEqual(PUBLIC);
  });

  it.each(routes.filter((route) => !PUBLIC.has(route)))("세션 없이 %s 요청하면 Riot을 부르지 않고 401이다", async (route) => {
    const [method, path] = route.split(" ") as [string, string];
    const fresh = setup();
    const res = await fresh.call(method, concrete(path), undefined, method === "GET" || method === "DELETE" ? undefined : {});
    expect(res.status).toBe(401);
    expect(fresh.upstream.calls).toHaveLength(0);
  });

  it("Riot ID로 사람을 찾는 경로가 없다", () => {
    for (const route of routes) expect(route).not.toMatch(/search|riot-?id|game-?name|by-name/i);
  });
});
