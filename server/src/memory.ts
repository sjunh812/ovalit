/**
 * isolate 하나의 메모리에 두는 캐시입니다.
 * isolate가 내려가거나 요청이 다른 isolate로 가면 비어 있으니, 없을 수 있다고 보고 씁니다.
 *
 * 오래 안 꺼낸 것부터 버립니다. 크기는 글자당 2바이트로 셉니다.
 * V8은 Latin-1 밖의 글자가 하나라도 섞인 문자열을 통째로 글자당 2바이트로 들고 있고, 한국 서버 경기에는 대개 한글 이름이 섞여 있습니다.
 */
export class MemoryCache {
  private readonly entries = new Map<string, { body: string; expiresAt: number; bytes: number }>();
  private totalBytes = 0;

  constructor(private readonly limits: { maxEntries: number; maxBytes: number }) {}

  get(key: string): string | undefined {
    const entry = this.entries.get(key);
    if (!entry) return undefined;
    this.entries.delete(key);
    if (entry.expiresAt <= Date.now()) {
      this.totalBytes -= entry.bytes;
      return undefined;
    }
    // Map은 넣은 순서를 지키므로 다시 넣으면 가장 최근에 쓴 자리로 간다.
    this.entries.set(key, entry);
    return entry.body;
  }

  set(key: string, body: string, ttlSeconds: number): void {
    const bytes = body.length * 2;
    if (bytes > this.limits.maxBytes) return;
    this.delete(key);
    this.entries.set(key, { body, expiresAt: Date.now() + ttlSeconds * 1000, bytes });
    this.totalBytes += bytes;
    while (this.entries.size > this.limits.maxEntries || this.totalBytes > this.limits.maxBytes) {
      const oldest = this.entries.keys().next().value;
      if (oldest === undefined) break;
      this.delete(oldest);
    }
  }

  clear(): void {
    this.entries.clear();
    this.totalBytes = 0;
  }

  get size(): number {
    return this.entries.size;
  }

  delete(key: string): void {
    const entry = this.entries.get(key);
    if (!entry) return;
    this.entries.delete(key);
    this.totalBytes -= entry.bytes;
  }
}

/**
 * 키마다 창 하나에 `limit`번까지 받습니다.
 * 창은 그 키의 첫 요청에서 시작해 `windowMs` 뒤에 끝나므로, `limit`이 1이면 최소 간격이 됩니다.
 * isolate마다 따로 세니 여러 isolate로 나뉘어 들어온 요청은 한도를 넘길 수 있습니다.
 */
export class Quota {
  private readonly windows = new Map<string, { endsAt: number; used: number }>();

  constructor(
    private readonly limit: number,
    private readonly windowMs: number,
    private readonly maxKeys = 10_000,
  ) {}

  /** 한 번 씁니다. 쓸 수 있으면 `undefined`이고, 다 썼으면 창이 끝날 때까지 남은 초(1 이상)입니다. */
  take(key: string): number | undefined {
    const now = Date.now();
    let window = this.windows.get(key);
    if (!window || window.endsAt <= now) {
      this.windows.delete(key);
      window = { endsAt: now + this.windowMs, used: 0 };
      this.windows.set(key, window);
      // 창을 연 순서대로 들어 있으니 맨 앞이 가장 먼저 끝나는 창이다.
      if (this.windows.size > this.maxKeys) this.windows.delete(this.windows.keys().next().value!);
    }
    if (window.used >= this.limit) return Math.ceil((window.endsAt - now) / 1000);
    window.used++;
    return undefined;
  }

  clear(): void {
    this.windows.clear();
  }
}
