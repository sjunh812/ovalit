/**
 * isolate 하나의 메모리에 두는 캐시입니다. `*.workers.dev`에서는 Cache API가 담지 않아서 그 대신
 * 앞에 둡니다. isolate가 내려가거나 다른 데이터센터로 가면 비어 있으니 있으면 쓰는 정도로만 믿습니다.
 *
 * 오래 안 꺼낸 것부터 버립니다. 크기는 문자열 길이의 두 배로 셉니다. V8이 한글이 섞인 문자열을 글자당
 * 2바이트로 들고 있어서 실제보다 작게 세지 않으려는 것입니다.
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

  private delete(key: string): void {
    const entry = this.entries.get(key);
    if (!entry) return;
    this.entries.delete(key);
    this.totalBytes -= entry.bytes;
  }
}
