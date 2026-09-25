import { Hono } from "hono";
import roles from "../data/roles.json";
import tiers from "../data/tiers.json";
import type { AppEnv } from "../env";
import { rawJson, riotFor } from "../riot";
import { requireSession } from "../session";

export const content = new Hono<AppEnv>();

// 전적은 아니지만 우리 Riot 키로 부른다. 여러 isolate가 같이 쓰는 캐시가 없는 동안에는 모르는 사람이
// 이 두 경로로 레이트 리밋을 태우지 못하게 세션 뒤에 둔다.
content.get("/content", requireSession, async (c) => rawJson(c, await riotFor(c).content(), "private, max-age=21600"));

content.get("/status", requireSession, async (c) => rawJson(c, await riotFor(c).status(), "private, max-age=60"));

// VAL-CONTENT에는 티어 이름도 요원 역할도 없다. valorant-api.com에서 받아 scripts/build-tables.mjs로 만든 표다.
// Riot을 부르지 않으니 세션 없이 연다.
content.get("/content/tiers", (c) => c.json(tiers, 200, { "Cache-Control": "public, max-age=86400" }));

content.get("/content/roles", (c) => c.json(roles, 200, { "Cache-Control": "public, max-age=86400" }));
