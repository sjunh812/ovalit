import { afterEach, describe, expect, it, vi } from "vitest";
import { cacheKey } from "../src/riot";
import { forgetCaches, namedError, RIOT, setup } from "./helpers";

const CONTENT_PATH = "/val/content/v1/contents?locale=ko-KR";
const CONTENT_JA_PATH = "/val/content/v1/contents?locale=ja-JP";
const STATUS_PATH = "/val/status/v1/platform-data";
const CONTENT_URL = `${RIOT}${CONTENT_PATH}`;
const CONTENT_JA_URL = `${RIOT}${CONTENT_JA_PATH}`;
const STATUS_URL = `${RIOT}${STATUS_PATH}`;

afterEach(() => {
  vi.restoreAllMocks();
});

describe("콘텐츠", () => {
  it("티어 표는 번호에서 한글 이름으로 가고 쓰지 않는 번호는 없다", async () => {
    const t = setup();
    const tiers = await (await t.call("GET", "/content/tiers")).json<Record<string, string>>();
    expect(tiers["3"]).toBe("아이언 1");
    expect(tiers["27"]).toBe("레디언트");
    expect(Object.keys(tiers)).not.toContain("0");
    expect(Object.values(tiers).some((name) => name.startsWith("미사용"))).toBe(false);
  });

  // 앱 언어를 일본어로 바꾸면 티어 이름도 일본어여야 한다(FR-035)
  it("티어 표는 locale을 받아 그 언어의 이름을 같은 번호로 내려준다", async () => {
    const t = setup();
    const ko = await (await t.call("GET", "/content/tiers?locale=ko-KR")).json<Record<string, string>>();
    const ja = await (await t.call("GET", "/content/tiers?locale=ja-JP")).json<Record<string, string>>();
    expect(ja["3"]).toBe("アイアン 1");
    expect(ja["27"]).toBe("レディアント");
    expect(Object.keys(ja)).toEqual(Object.keys(ko));
  });

  // 영어 화면(필리핀, 말레이시아, 싱가포르, 호주)도 같은 번호로 영어 이름을 받는다
  it("티어 표는 영어 이름을 첫 글자만 대문자로 내려준다", async () => {
    const t = setup();
    const ko = await (await t.call("GET", "/content/tiers?locale=ko-KR")).json<Record<string, string>>();
    const en = await (await t.call("GET", "/content/tiers?locale=en-US")).json<Record<string, string>>();
    expect(en["3"]).toBe("Iron 1");
    expect(en["21"]).toBe("Ascendant 1");
    expect(en["27"]).toBe("Radiant");
    expect(Object.keys(en)).toEqual(Object.keys(ko));
  });

  // 모르는 값을 받아 주면 값마다 Riot 콘텐츠를 따로 담아 캐시가 끝없이 는다
  it("모르는 locale은 Riot을 부르지 않고 400이다", async () => {
    const t = setup();
    const me = await t.login();
    for (const path of ["/content/tiers?locale=fr-FR", "/content?locale=xx"]) {
      const res = await t.call("GET", path, me.token);
      expect(res.status).toBe(400);
      expect(await res.json()).toEqual({ error: "invalid_locale" });
    }
    expect(t.upstream.calls).toHaveLength(0);
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

  it("VAL-CONTENT는 언어마다 따로 받아 담아 둔다", async () => {
    await forgetCaches(CONTENT_PATH);
    await forgetCaches(CONTENT_JA_PATH);
    const t = setup();
    const me = await t.login();
    t.upstream.json(CONTENT_URL, { version: "13.06", characters: [{ name: "제트" }] });
    t.upstream.json(CONTENT_JA_URL, { version: "13.06", characters: [{ name: "ジェット" }] });
    const ko = await t.call("GET", "/content", me.token);
    const ja = await t.call("GET", "/content?locale=ja-JP", me.token);
    expect(await ko.json()).toEqual({ version: "13.06", characters: [{ name: "제트" }] });
    expect(await ja.json()).toEqual({ version: "13.06", characters: [{ name: "ジェット" }] });
    await t.call("GET", "/content", me.token);
    await t.call("GET", "/content?locale=ja-JP", me.token);
    expect(t.upstream.callsTo(CONTENT_URL)).toHaveLength(1);
    expect(t.upstream.callsTo(CONTENT_JA_URL)).toHaveLength(1);
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

  it("Cache API가 실패해도 Riot에서 받은 값을 내려보내고 에러 이름만 남긴다", async () => {
    await forgetCaches(STATUS_PATH);
    const t = setup();
    const me = await t.login();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(caches.default, "match").mockRejectedValue(namedError("CacheDown", "match"));
    vi.spyOn(caches.default, "put").mockRejectedValue(namedError("CacheDown", "put"));
    t.upstream.json(STATUS_URL, { id: "KR", maintenances: [], incidents: [] });
    const res = await t.call("GET", "/status", me.token);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ id: "KR", maintenances: [], incidents: [] });
    expect(errors.mock.calls).toEqual([
      ["riot_cache", "CacheDown"],
      ["riot_cache", "CacheDown"],
    ]);
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
