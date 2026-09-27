import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryCache, Quota } from "../src/memory";

describe("isolate 메모리 캐시", () => {
  it("기한이 지난 값은 돌려주지 않는다", () => {
    const cache = new MemoryCache({ maxEntries: 10, maxBytes: 1000 });
    cache.set("gone", "x", 0);
    cache.set("kept", "y", 60);
    expect(cache.get("gone")).toBeUndefined();
    expect(cache.get("kept")).toBe("y");
    expect(cache.size).toBe(1);
  });

  it("크기를 넘으면 가장 오래 안 꺼낸 것부터 버린다", () => {
    // 네 글자는 8바이트로 센다. 20바이트면 두 개까지 들어간다.
    const cache = new MemoryCache({ maxEntries: 10, maxBytes: 20 });
    cache.set("a", "aaaa", 60);
    cache.set("b", "bbbb", 60);
    expect(cache.get("a")).toBe("aaaa");
    cache.set("c", "cccc", 60);
    expect(cache.get("b")).toBeUndefined();
    expect(cache.get("a")).toBe("aaaa");
    expect(cache.get("c")).toBe("cccc");
  });

  it("개수를 넘어도 오래된 것부터 버린다", () => {
    const cache = new MemoryCache({ maxEntries: 1, maxBytes: 1000 });
    cache.set("old", "1", 60);
    cache.set("new", "2", 60);
    expect(cache.get("old")).toBeUndefined();
    expect(cache.get("new")).toBe("2");
  });

  it("혼자서 한도를 넘는 값은 담지 않고 있던 것도 버리지 않는다", () => {
    const cache = new MemoryCache({ maxEntries: 10, maxBytes: 20 });
    cache.set("small", "ok", 60);
    cache.set("huge", "x".repeat(11), 60);
    expect(cache.get("huge")).toBeUndefined();
    expect(cache.get("small")).toBe("ok");
  });

  it("같은 키를 다시 넣으면 크기를 두 번 세지 않는다", () => {
    const cache = new MemoryCache({ maxEntries: 10, maxBytes: 20 });
    cache.set("a", "aaaa", 60);
    cache.set("a", "aaaa", 60);
    cache.set("b", "bbbb", 60);
    expect(cache.get("a")).toBe("aaaa");
    expect(cache.get("b")).toBe("bbbb");
  });
});

describe("호출 한도", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("창 하나에 정한 횟수만 받고 남은 초를 알려주며, 창이 끝나면 다시 받는다", () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    const quota = new Quota(2, 10_000);
    expect(quota.take("a")).toBeUndefined();
    vi.advanceTimersByTime(3_000);
    expect(quota.take("a")).toBeUndefined();
    expect(quota.take("a")).toBe(7);
    expect(quota.take("b")).toBeUndefined();
    vi.advanceTimersByTime(7_000);
    expect(quota.take("a")).toBeUndefined();
  });

  it("키가 너무 많아지면 가장 먼저 연 창부터 버린다", () => {
    const quota = new Quota(1, 60_000, 2);
    quota.take("a");
    quota.take("b");
    quota.take("c");
    expect(quota.take("a")).toBeUndefined();
    expect(quota.take("c")).toBe(60);
  });
});
