import { applyD1Migrations } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { beforeEach } from "vitest";

await applyD1Migrations(env.DB, env.TEST_MIGRATIONS);

// 테스트끼리 D1을 같이 쓴다. 한 테스트가 받은 Riot 429가 다음 테스트의 Riot 호출을 막지 않게 비운다.
beforeEach(async () => {
  await env.DB.prepare("DELETE FROM riot_blocks").run();
});
