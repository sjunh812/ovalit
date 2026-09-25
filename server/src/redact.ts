import { PUUID } from "./validate";

/**
 * 요청한 사람이 뛰지 않은 친구 경기에서 `keep` 밖의 사람을 가립니다. 앱을 안 쓰는 사람은 내가 뛴 경기
 * 안의 기록까지만 보여줄 수 있습니다.
 *
 * 응답 모양을 다 알지 못해서 PUUID처럼 생긴 문자열은 어느 자리에 있든 `anon-1`처럼 바꿉니다. 같은
 * PUUID는 같은 이름으로 바뀌어 킬, 어시스트, 피해량이 누구 것인지는 경기 안에서 그대로 이어집니다.
 * 이름과 태그는 비우고 플레이어 카드와 칭호는 뺍니다. 파티 ID는 여러 경기에 걸쳐 같은 값이라 그걸로
 * 다른 경기의 같은 사람을 찾을 수 있어서, 모두의 파티 ID를 이 경기 안에서만 통하는 이름으로 바꿉니다.
 *
 * 받은 객체를 그 자리에서 고칩니다. 캐시 원문을 새로 파싱한 객체만 넘깁니다.
 */
export function redactMatch<T>(match: T, keep: ReadonlySet<string>): T {
  const people = new Map<string, string>();
  const parties = new Map<string, string>();

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
      }
      if (typeof record.partyId === "string") {
        let party = parties.get(record.partyId);
        if (!party) {
          party = `party-${parties.size + 1}`;
          parties.set(record.partyId, party);
        }
        record.partyId = party;
      }
      for (const key of Object.keys(record)) record[key] = walk(record[key]);
    }
    return node;
  };

  return walk(match) as T;
}
