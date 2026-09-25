#!/usr/bin/env node
/**
 * valorant-api.com에서 받은 요원·티어 JSON(ko-KR)으로 서버가 내려줄 표 두 개를 만듭니다.
 * Riot 공식 자료에는 요원 역할과 티어 이름이 없습니다. 패치로 요원이나 티어가 바뀌면 다시 돌립니다.
 *
 *     node scripts/build-tables.mjs <agents.json> <competitivetiers.json>
 *
 * 결과는 src/data/roles.json(요원 UUID → 역할)과 src/data/tiers.json(티어 번호 → 한글 이름)입니다.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

// 역할 이름은 로케일마다 바뀌어서 에셋 경로로 가른다. 코드는 앱의 Role enum과 같은 영어 소문자다.
const ROLE_BY_ASSET = {
  Assault: "duelist",
  Breaker: "initiator",
  Strategist: "controller",
  Sentinel: "sentinel",
};

function roleOf(agent) {
  const asset = /\/Roles\/(\w+)_PrimaryDataAsset$/.exec(agent.role?.assetPath ?? "")?.[1];
  const role = ROLE_BY_ASSET[asset];
  // 새 역할이 생겼는데 조용히 빠지면 그 요원만 역할 없이 뜬다. 여기서 멈추고 표를 고치게 한다.
  if (!role) throw new Error(`역할을 모르는 요원: ${agent.displayName} (${agent.role?.assetPath})`);
  return role;
}

function buildRoles(agents) {
  const playable = agents.data.filter((agent) => agent.isPlayableCharacter);
  const entries = playable.map((agent) => [agent.uuid.toLowerCase(), roleOf(agent)]);
  return Object.fromEntries(entries.sort(([a], [b]) => a.localeCompare(b)));
}

function buildTiers(competitiveTiers) {
  // 에피소드마다 표가 하나씩 쌓이고 마지막이 지금 쓰는 표다.
  const current = competitiveTiers.data.at(-1);
  // 0은 랭크가 없는 상태다. 앱에 번들한 엠블럼도 3부터라 이름도 3부터 둔다. 1, 2는 게임에서 쓰지 않는 자리다.
  const tiers = current.tiers.filter((tier) => tier.tier > 0 && !/^(미사용|Unused)/.test(tier.tierName));
  return Object.fromEntries(tiers.map((tier) => [String(tier.tier), tier.tierName]));
}

function write(name, table) {
  const path = fileURLToPath(new URL(`../src/data/${name}`, import.meta.url));
  writeFileSync(path, `${JSON.stringify(table, null, 2)}\n`);
  console.log(`${name}: ${Object.keys(table).length}개`);
}

const [agentsPath, tiersPath] = process.argv.slice(2);
if (!agentsPath || !tiersPath) {
  console.error("사용법: node scripts/build-tables.mjs <agents.json> <competitivetiers.json>");
  process.exit(1);
}
write("roles.json", buildRoles(JSON.parse(readFileSync(agentsPath, "utf8"))));
write("tiers.json", buildTiers(JSON.parse(readFileSync(tiersPath, "utf8"))));
