import { describe, expect, it } from "vitest";
import { cacheKey } from "../src/riot";
import { forgetCaches, RIOT, setup } from "./helpers";

const CONTENT_PATH = "/val/content/v1/contents?locale=ko-KR";
const STATUS_PATH = "/val/status/v1/platform-data";
const CONTENT_URL = `${RIOT}${CONTENT_PATH}`;
const STATUS_URL = `${RIOT}${STATUS_PATH}`;

describe("콘텐츠", () => {
  it("티어 표는 번호에서 한글 이름으로 가고 쓰지 않는 번호는 없다", async () => {
    const t = setup();
    const tiers = await (await t.call("GET", "/content/tiers")).json<Record<string, string>>();
    expect(tiers["3"]).toBe("아이언 1");
    expect(tiers["27"]).toBe("레디언트");
    expect(Object.keys(tiers)).not.toContain("0");
    expect(Object.values(tiers).some((name) => name.startsWith("미사용"))).toBe(false);
  });

  it("역할 표는 요원 UUID에서 앱의 역할 코드로 간다", async () => {
    const t = setup();
    const roles = await (await t.call("GET", "/content/roles")).json<Record<string, string>>();
    // 제트, 킬조이, 오멘, 소바
    expect(roles["add6443a-41bd-e414-f6ad-e58d267f4e95"]).toBe("duelist");
    expect(roles["1e58de9c-4950-5125-93e9-a0aee9f98746"]).toBe("sentinel");
    expect(roles["8e253930-4c05-31dd-1b6c-968525494517"]).toBe("controller");
    expect(roles["320b2a48-4d9b-a075-30f1-1f93a9b638fa"]).toBe("initiator");
    expect(new Set(Object.values(roles))).toEqual(new Set(["duelist", "initiator", "controller", "sentinel"]));
  });

  it("VAL-CONTENT와 점검 안내는 세션이 없으면 Riot을 부르지 않고 401이다", async () => {
    const t = setup();
    for (const path of ["/content", "/status"]) {
      const res = await t.call("GET", path);
      expect(res.status).toBe(401);
    }
    expect(t.upstream.calls).toHaveLength(0);
  });

  // 캐시는 테스트 파일끼리도 이어질 수 있어서 VAL-CONTENT는 이 파일에서만 부른다.
  it("VAL-CONTENT는 한 번 받으면 담아 둔다", async () => {
    await forgetCaches(CONTENT_PATH);
    const t = setup();
    const me = await t.login();
    t.upstream.json(CONTENT_URL, { version: "13.06", characters: [] });
    const first = await t.call("GET", "/content", me.token);
    expect(first.status).toBe(200);
    expect(await first.json()).toEqual({ version: "13.06", characters: [] });
    await t.call("GET", "/content", me.token);
    expect(t.upstream.callsTo(CONTENT_URL)).toHaveLength(1);
  });

  it("Cache API가 담지 못해도 isolate 메모리에서 꺼낸다", async () => {
    await forgetCaches(CONTENT_PATH);
    const t = setup();
    const me = await t.login();
    t.upstream.json(CONTENT_URL, { version: "13.06" });
    await t.call("GET", "/content", me.token);
    // workers.dev처럼 Cache API에 아무것도 남지 않은 상태를 만든다.
    await caches.default.delete(cacheKey("http://localhost", CONTENT_PATH));
    await t.call("GET", "/content", me.token);
    expect(t.upstream.callsTo(CONTENT_URL)).toHaveLength(1);
  });

  it("점검 안내도 담아 둔다", async () => {
    await forgetCaches(STATUS_PATH);
    const t = setup();
    const me = await t.login();
    t.upstream.json(STATUS_URL, { id: "KR", maintenances: [], incidents: [] });
    await t.call("GET", "/status", me.token);
    await t.call("GET", "/status", me.token);
    expect(t.upstream.callsTo(STATUS_URL)).toHaveLength(1);
  });
});
