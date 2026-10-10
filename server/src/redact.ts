import { PUUID } from "./validate";

// 가린 사람의 플레이어 줄과 코치 줄에서 남기는 필드다. 앱이 스코어보드를 그리는 데 쓰는 것만 둔다. Riot이 필드를 더하면
// 여기 넣기 전까지는 빠진다. 이름과 태그는 앱이 문자열로 읽으니 빈 값으로 남긴다.
const PLAYER_FIELDS = new Set([
  "puuid",
  "gameName",
  "tagLine",
  "teamId",
  "partyId",
  "characterId",
  "stats",
  "competitiveTier",
  "isObserver",
]);
const COACH_FIELDS = new Set(["puuid", "teamId"]);
// 프리미어 팀 ID와 이름은 여러 경기에서 같은 값이라 다른 경기의 같은 팀을 찾는 실마리가 된다.
const PREMIER = /premier|roster/i;

/**
 * 요청한 사람이 뛰지 않은 친구 경기에서 `keep` 밖의 사람을 가립니다. 앱을 안 쓰는 사람은 내가 뛴 경기 안의 기록까지만
 * 보여줄 수 있습니다(CLAUDE.md 지켜야 할 선).
 *
 * 가린 사람의 플레이어 줄과 코치 줄은 정해 둔 필드만 남깁니다. 이름과 태그는 비우고 플레이어 카드, 칭호, 계정 레벨처럼
 * 사람을 가리키는 값은 남기지 않습니다. Riot이 새 필드를 더해도 저절로 새지 않게 빼는 목록이 아니라 남기는 목록입니다.
 * 계정 레벨과 파티 ID는 여러 경기에서 같은 값이라, 남겨 두면 다른 경기의 같은 사람을 찾을 수 있습니다. 그래서 파티 ID는
 * 모두 이 경기 안에서만 통하는 이름으로 바꿉니다. 티어는 스코어보드에 뜨는 값이고 같은 티어인 사람이 많아 그대로 둡니다.
 * 커스텀 게임 이름은 비우고 프리미어 팀을 가리키는 값은 뺍니다.
 *
 * 응답 모양을 다 알지 못해서 그 밖의 자리에서도 PUUID처럼 생긴 문자열은 값이든 객체 키든 `anon-1`처럼 바꿉니다. 같은
 * PUUID는 같은 이름이 되니 킬, 어시스트, 피해량이 누구 것인지는 경기 안에서 그대로 이어집니다.
 *
 * 받은 객체를 그 자리에서 고칩니다. 캐시 원문을 새로 파싱한 객체만 넘깁니다.
 */
export function redactMatch<T>(match: T, keep: ReadonlySet<string>): T {
  const people = new Map<string, string>();
  const parties = new Map<string, string>();

  const allowOnly = (list: unknown, fields: ReadonlySet<string>) => {
    if (!Array.isArray(list)) return;
    for (let i = 0; i < list.length; i++) {
      const entry: unknown = list[i];
      if (!isRecord(entry) || (typeof entry.puuid === "string" && keep.has(entry.puuid))) continue;
      const kept: Record<string, unknown> = {};
      for (const key of Object.keys(entry)) if (fields.has(key)) kept[key] = entry[key];
      if ("gameName" in kept) kept.gameName = "";
      if ("tagLine" in kept) kept.tagLine = "";
      list[i] = kept;
    }
  };

  if (isRecord(match)) {
    allowOnly(match.players, PLAYER_FIELDS);
    allowOnly(match.coaches, COACH_FIELDS);
    if (isRecord(match.matchInfo)) {
      if ("customGameName" in match.matchInfo) match.matchInfo.customGameName = "";
      dropPremier(match.matchInfo);
    }
    if (Array.isArray(match.teams)) for (const team of match.teams) if (isRecord(team)) dropPremier(team);
  }

  const alias = (value: string): string => {
    if (value.length !== 78 || keep.has(value) || !PUUID.test(value)) return value;
    let name = people.get(value);
    if (!name) {
      name = `anon-${people.size + 1}`;
      people.set(value, name);
    }
    return name;
  };

  const walk = (node: unknown): unknown => {
    if (typeof node === "string") return alias(node);
    if (Array.isArray(node)) {
      for (let i = 0; i < node.length; i++) node[i] = walk(node[i]);
      return node;
    }
    if (typeof node === "object" && node !== null) {
      const record = node as Record<string, unknown>;
      if (typeof record.puuid === "string" && !keep.has(record.puuid)) {
        if ("gameName" in record) record.gameName = "";
        if ("tagLine" in record) record.tagLine = "";
        delete record.playerCard;
        delete record.playerTitle;
        delete record.accountLevel;
      }
      if (typeof record.partyId === "string") {
        let party = parties.get(record.partyId);
        if (!party) {
          party = `party-${parties.size + 1}`;
          parties.set(record.partyId, party);
        }
        record.partyId = party;
      }
      for (const key of Object.keys(record)) {
        const renamed = alias(key);
        const value = walk(record[key]);
        if (renamed !== key) delete record[key];
        record[renamed] = value;
      }
    }
    return node;
  };

  return walk(match) as T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function dropPremier(record: Record<string, unknown>): void {
  for (const key of Object.keys(record)) if (PREMIER.test(key)) delete record[key];
}
