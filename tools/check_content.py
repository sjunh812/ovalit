#!/usr/bin/env python3
"""새 요원, 맵, 무기, 모드, 티어가 나왔는지 valorant-api.com과 저장소를 견줍니다.

GitHub Actions(.github/workflows/content-watch.yml)가 매일 돌리고, 저장소가 모르는 것이 있으면 이슈를 열거나
고칩니다. 공개 저장소의 표준 러너와 valorant-api.com은 무료입니다(CLAUDE.md 비용). Riot 키는 개발용이
24시간마다 바뀌어서 쓰지 않습니다. 표준 라이브러리만 씁니다.

    tools/check_content.py [--out report.md]

견주는 곳은 넷입니다.

- 앱에 넣은 그림: GameAssetIndex.kt의 요원, 맵, 무기, 티어
- 서버가 내려주는 표: server/src/data/roles.json(요원 역할), tiers.json(티어 이름)
- 큐 ID 표: core/model의 Queue.fromRiot
- 일부러 넘기는 것: tools/content-known.json

새 것이 있으면 GITHUB_OUTPUT에 changed=true를 적고, 보고서를 --out 파일과 표준 출력에 씁니다.
"""
import json
import os
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
INDEX = ROOT / "shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/GameAssetIndex.kt"
QUEUE = ROOT / "shared/core/model/src/commonMain/kotlin/com/ovalit/core/model/Queue.kt"
ROLES = ROOT / "server/src/data/roles.json"
TIERS = ROOT / "server/src/data/tiers.json"
KNOWN = ROOT / "tools/content-known.json"
API = "https://valorant-api.com/v1"


def fetch(path):
    request = urllib.request.Request(f"{API}/{path}", headers={"User-Agent": "ovalit-content-watch"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)["data"]


def index_keys(name):
    """GameAssetIndex.kt에서 `val name = mapOf(...)`의 키를 소문자로 꺼냅니다."""
    text = INDEX.read_text(encoding="utf-8")
    block = re.search(rf"val {name}\b[^=]*=\s*mapOf\((.*?)\n\s*\)", text, re.S)
    if block is None:
        sys.exit(f"GameAssetIndex.kt에서 {name} 표를 찾지 못했습니다")
    return {key.lower() for key in re.findall(r'"?([^"\s]+?)"?\s+to\s', block.group(1))}


def known_queues():
    """Queue.fromRiot의 when 분기에 적힌 큐 ID입니다. 빈 값과 custom도 넣습니다."""
    text = QUEUE.read_text(encoding="utf-8")
    body = re.search(r"fun fromRiot\(.*?\n        }\n", text, re.S)
    if body is None:
        sys.exit("Queue.kt에서 fromRiot을 찾지 못했습니다")
    return set(re.findall(r'"([a-z0-9_]*)"\s*(?:,|->)', body.group(0)))


def main():
    out = None
    if "--out" in sys.argv:
        out = pathlib.Path(sys.argv[sys.argv.index("--out") + 1])
    known = json.loads(KNOWN.read_text(encoding="utf-8"))
    skip_maps = {key.lower() for key in known["maps"]}
    skip_queues = set(known["queues"])
    sections = []

    version = fetch("version")
    agents = fetch("agents?isPlayableCharacter=true&language=ko-KR")
    weapons = fetch("weapons?language=ko-KR")
    maps = fetch("maps?language=ko-KR")
    queues = fetch("gamemodes/queues?language=ko-KR")
    tiers = fetch("competitivetiers?language=ko-KR")

    agent_images = index_keys("agents")
    roles = {key.lower() for key in json.loads(ROLES.read_text(encoding="utf-8"))}
    rows = []
    for agent in agents:
        uuid = agent["uuid"].lower()
        missing = [what for what, have in (("그림", agent_images), ("역할", roles)) if uuid not in have]
        if missing:
            rows.append(f"| {agent['displayName']} | `{uuid}` | {', '.join(missing)} |")
    if rows:
        sections.append(("요원", "| 이름 | UUID | 없는 것 |\n| --- | --- | --- |\n" + "\n".join(rows),
                         "카탈로그를 새로 받아 `tools/bundle_assets.py`로 그림을 넣고, `server`에서 "
                         "`npm run build:tables`로 역할표를 다시 만듭니다(`server/README.md`)."))

    weapon_images = index_keys("weapons")
    rows = [f"| {w['displayName']} | `{w['uuid'].lower()}` | {w['category'].split('::')[-1]} |"
            for w in weapons if w["uuid"].lower() not in weapon_images]
    if rows:
        sections.append(("무기", "| 이름 | UUID | 계열 |\n| --- | --- | --- |\n" + "\n".join(rows),
                         "카탈로그를 새로 받아 `tools/bundle_assets.py`로 그림을 넣습니다. 이름과 계열은 카탈로그에서 옵니다."))

    map_images = index_keys("maps")
    rows = []
    for game_map in maps:
        keys = {game_map["uuid"].lower(), game_map["mapUrl"].lower()}
        if keys & map_images or keys & skip_maps:
            continue
        rows.append(f"| {game_map['displayName']} | `{game_map['uuid'].lower()}` | `{game_map['mapUrl']}` |")
    if rows:
        sections.append(("맵", "| 이름 | UUID | 경로 |\n| --- | --- | --- |\n" + "\n".join(rows),
                         "카탈로그를 새로 받아 `tools/bundle_assets.py`로 그림을 넣습니다. 그림이 없으면 경기 줄에 맵 이름만 뜹니다. "
                         "경기 기록에 나오지 않는 맵이면 `tools/content-known.json`의 `maps`에 까닭과 함께 적습니다."))

    ours = known_queues()
    rows = [f"| `{q['queueId']}` | {q['displayName']} |" for q in queues
            if q["queueId"] not in ours and q["queueId"] not in skip_queues and not q["queueId"].startswith("console_")]
    if rows:
        sections.append(("모드(큐 ID)", "| 큐 ID | 이름 |\n| --- | --- |\n" + "\n".join(rows),
                         "모르는 큐 ID는 앱이 기타 목록에만 둬서 죽지는 않습니다. 실제 경기 응답으로 규칙을 확인하면 "
                         "`Queue.fromRiot`과 CLAUDE.md의 큐 표에 넣고, 그 전까지 지켜보기만 할 거면 "
                         "`tools/content-known.json`의 `queues`에 까닭과 함께 적습니다."))

    # 가장 최근 에피소드의 티어 표만 본다. 쓰지 않는 번호(0~2)는 이름이 Unused다.
    latest = tiers[-1]["tiers"]
    tier_names = json.loads(TIERS.read_text(encoding="utf-8"))
    tier_images = index_keys("tiers")
    rows = []
    for tier in latest:
        number = str(tier["tier"])
        if number in ("0", "1", "2"):
            continue
        problems = []
        if number not in tier_names:
            problems.append("이름 없음")
        elif tier_names[number] != tier["tierName"]:
            problems.append(f"이름이 다름(우리 표: {tier_names[number]})")
        if number not in tier_images:
            problems.append("엠블럼 없음")
        if problems:
            rows.append(f"| {number} | {tier['tierName']} | {', '.join(problems)} |")
    if rows:
        sections.append(("티어", "| 번호 | 이름 | 문제 |\n| --- | --- | --- |\n" + "\n".join(rows),
                         "`server`에서 `npm run build:tables`로 티어 표를 다시 만들고, `tools/bundle_assets.py`로 엠블럼을 넣습니다."))

    header = f"게임 버전 `{version['version']}`(빌드 {version['buildDate'][:10]}) 기준으로 저장소가 모르는 것입니다."
    if not sections:
        report = header.replace("모르는 것입니다.", "모르는 것이 없습니다.")
    else:
        parts = [header, "", "자료는 valorant-api.com(커뮤니티 API)입니다. Riot 응답과 다를 수 있어 실제 경기로 다시 확인합니다."]
        for title, table, todo in sections:
            parts += ["", f"## {title}", "", table, "", todo]
        parts += ["", "다 처리하면 이 이슈를 닫습니다. 다음 확인에서도 남아 있으면 이슈 본문을 새로 고칩니다."]
        report = "\n".join(parts)

    print(report)
    if out is not None:
        out.write_text(report + "\n", encoding="utf-8")
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"changed={'true' if sections else 'false'}\n")


if __name__ == "__main__":
    main()
