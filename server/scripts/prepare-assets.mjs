#!/usr/bin/env node
/**
 * 콘텐츠 카탈로그 압축 파일에서 플레이어 카드를 꺼내 public/cards/에 둡니다. 정적 에셋으로 나가서
 * Worker를 거치지 않습니다. 카탈로그가 바뀌면 다시 돌립니다. 결과는 저장소에 올리지 않습니다.
 *
 *     node scripts/prepare-assets.mjs <catalog.zip>
 *
 * 카탈로그 파일 이름은 대문자 UUID지만 여기서는 소문자로 맞춥니다(`/cards/{uuid}_small.png`,
 * `/cards/{uuid}_wide.png`). 경기 응답의 `players[].playerCard`가 어느 쪽으로 오든 앱이 소문자로 바꿔 부릅니다.
 */
import { execFileSync } from "node:child_process";
import { mkdirSync, readdirSync, renameSync, rmSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const CARD = /^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}_(small|wide)\.png$/;

const zip = process.argv[2];
if (!zip) {
  console.error("사용법: node scripts/prepare-assets.mjs <catalog.zip>");
  process.exit(1);
}

const out = fileURLToPath(new URL("../public/cards/", import.meta.url));
// 카탈로그에서 빠진 카드가 남지 않게 매번 비우고 채운다.
rmSync(out, { recursive: true, force: true });
mkdirSync(out, { recursive: true });

// 새 의존성을 들이지 않으려고 시스템 unzip을 쓴다. 2GB 압축을 다 풀지 않고 카드 두 크기만 꺼낸다.
execFileSync("unzip", ["-q", "-j", "-o", zip, "PlayerCards/*_small.png", "PlayerCards/*_wide.png", "-d", out], {
  stdio: "inherit",
});

let count = 0;
for (const name of readdirSync(out)) {
  if (!CARD.test(name)) {
    rmSync(join(out, name));
    continue;
  }
  // 대소문자를 가리지 않는 macOS 파일 시스템에서도 rename은 이름의 대소문자만 바꿀 수 있다.
  if (name !== name.toLowerCase()) renameSync(join(out, name), join(out, name.toLowerCase()));
  count++;
}
console.log(`public/cards: ${count}개`);
