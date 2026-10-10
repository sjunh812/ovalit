#!/usr/bin/env node
/**
 * 초대 링크 미리보기 그림(1200×630 PNG)을 만들어 public/og/에 둡니다. 결과는 저장소에 올립니다.
 *
 *     node scripts/build-og-image.mjs
 *
 * 앱 아이콘처럼 `--accent` 바탕에 흰 말풍선 ㅇㅂㅇ입니다.
 * 카카오톡은 미리보기 그림을 PNG나 JPG로만 받아서 SVG를 그대로 두지 않고 그림으로 굽습니다.
 * 글자는 넣지 않습니다. 미리보기 카드가 제목과 설명을 따로 적고, 서버에는 Pretendard를 그릴 도구가 없습니다.
 *
 * 그림을 바꾸면 파일 이름의 버전을 올리고 src/routes/public.ts의 `OG_IMAGE_PATH`도 같이 고칩니다.
 * 카카오톡은 그림을 주소로 기억해서 같은 이름으로 덮어쓰면 한동안 옛 그림이 뜹니다.
 *
 * sharp는 wrangler가 끌어오는 빌드 도구입니다(LGPL). 배포물에는 PNG만 들어갑니다.
 */
import { mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

const VERSION = 1;
const WIDTH = 1200;
const HEIGHT = 630;
const ACCENT = "#FF4655";
const ON_ACCENT = "#FFFFFF";

// docs/design.md 로고 항목의 경로다. 1024 정사각 앱 아이콘 그림의 픽셀이고 evenOdd로 칠해 글자 자리를 비운다.
const LOGO_PATH =
  "M272,280H752A120,120 0,0 1,872,400V598A120,120 0,0 1,752,718H432L322.5,813.8Q312,823 312,809V718H272A120,120 0,0 1,152,598V400A120,120 0,0 1,272,280ZM228,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM272,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0ZM430,422A8,8 0,0 1,438,414H466A8,8 0,0 1,474,422V463H550V422A8,8 0,0 1,558,414H586A8,8 0,0 1,594,422V573A10,10 0,0 1,584,583H440A10,10 0,0 1,430,573ZM474,499H550V548H474ZM620,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM664,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0Z";

// 말풍선 몸통(x 152~872, y 280~718)의 가운데를 그림 가운데보다 15px 위에 둔다.
// 아래로 꼬리가 나와서 그만큼 올려야 위아래가 맞아 보인다. 카카오톡이 가운데를 정사각으로 잘라도 말풍선이 다 남는 크기다.
const SCALE = 0.6;
const BODY_CENTER = { x: (152 + 872) / 2, y: (280 + 718) / 2 };
const dx = WIDTH / 2 - BODY_CENTER.x * SCALE;
const dy = HEIGHT / 2 - 15 - BODY_CENTER.y * SCALE;

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}">
<rect width="${WIDTH}" height="${HEIGHT}" fill="${ACCENT}"/>
<path transform="translate(${dx} ${dy}) scale(${SCALE})" fill="${ON_ACCENT}" fill-rule="evenodd" d="${LOGO_PATH}"/>
</svg>`;

let sharp;
try {
  ({ default: sharp } = await import("sharp"));
} catch {
  console.error("sharp가 없습니다. server에서 npm install을 먼저 돌려 주세요.");
  process.exit(1);
}

const dir = fileURLToPath(new URL("../public/og/", import.meta.url));
mkdirSync(dir, { recursive: true });
const out = `${dir}invite-v${VERSION}.png`;
await sharp(Buffer.from(svg)).png({ palette: true, compressionLevel: 9 }).toFile(out);
console.log(out);
