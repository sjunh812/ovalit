#!/usr/bin/env python3
"""앱에 넣을 게임 이미지를 콘텐츠 카탈로그에서 골라 줄입니다.

서버가 이미지를 내려주기 전까지, 그리고 서버에서 못 받았을 때 쓰는 대체용입니다. 패치로 카탈로그가
바뀌면 다시 돌립니다. macOS의 sips로 줄이므로 맥에서만 돕니다.

    tools/bundle_assets.py <압축을 푼 카탈로그 폴더> <티어 폴더>

티어는 카탈로그에 없어서 valorant-api.com에서 받은 것을 씁니다. 티어 폴더에는 `{번호}.png` 엠블럼과
영문 이름이 든 `competitivetiers.json`을 둡니다.

플레이어 카드는 넣지 않습니다. 천 장이 넘어 앱 용량만 커지고, 서버가 생기면 프로필 배너에만 붙입니다.

파일 이름은 영문 이름으로 짓고(`agents/jett.png`), UUID에서 파일 이름으로 가는 표는
GameAssetIndex.kt로 만들어 둡니다. 그 파일은 손으로 고치지 않습니다.
"""
import json
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "shared/core/ui/src/commonMain/composeResources/files"
INDEX = ROOT / "shared/core/ui/src/commonMain/kotlin/com/ovalit/core/ui/GameAssetIndex.kt"
ROLES = {
    "DBE8757E-9E92-4ED4-B39F-9DFC589691D4": "duelist",
    "1B47567F-8F7B-444B-AAE3-B0C634622D10": "initiator",
    "4EE40330-ECDD-4F2F-98A8-EB1243428373": "controller",
    "5FC02F99-4091-4486-A531-98459A3E95E9": "sentinel",
}


def sips(*args):
    subprocess.run(["sips", *map(str, args)], check=True, capture_output=True)


def size_of(image):
    out = subprocess.run(["sips", "-g", "pixelWidth", "-g", "pixelHeight", str(image)], capture_output=True, text=True).stdout
    return [int(line.split()[-1]) for line in out.strip().splitlines()[1:]]


def fit(src, dst, width):
    """비율을 두고 폭을 맞춥니다. 원본보다 키우지는 않습니다."""
    if size_of(src)[0] > width:
        sips("--resampleWidth", width, src, "--out", dst)
    else:
        shutil.copy(src, dst)


def cover(src, dst, width, height, quality=70):
    """가운데를 잘라 비율을 맞춘 뒤 줄여 JPEG로 저장합니다."""
    w, h = size_of(src)
    crop_w, crop_h = (w, round(w * height / width)) if w / h < width / height else (round(h * width / height), h)
    with tempfile.TemporaryDirectory() as tmp:
        cropped = pathlib.Path(tmp) / "crop.png"
        sips("-c", crop_h, crop_w, src, "--out", cropped)
        sips("-z", height, width, "-s", "format", "jpeg", "-s", "formatOptions", quality, cropped, "--out", dst)


def slug(name, suffix=""):
    name = re.sub(rf"\s*{suffix}$", "", name, flags=re.I) if suffix else name
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def english(entry):
    return entry["name"]["localizedByCulture"]["en-US"] or entry["name"]["defaultText"]


def main(catalog, tiers):
    catalog, tiers = pathlib.Path(catalog), pathlib.Path(tiers)
    data = json.loads((catalog / "PublicContentCatalog.json").read_text())
    shutil.rmtree(OUT, ignore_errors=True)
    for folder in ["agents", "roles", "maps", "weapons", "tiers"]:
        (OUT / folder).mkdir(parents=True)
    index = {"agents": {}, "maps": {}, "weapons": {}, "tiers": {}}

    # 전신 초상이 있는 요원만 실제로 고를 수 있는 요원이다. 케이/오는 ID가 둘인데 하나는 쓰이지 않는다.
    for agent in data["characters"]:
        if (catalog / "Characters" / f"{agent['id']}_full.png").exists():
            name = slug(english(agent))
            fit(catalog / "Characters" / f"{agent['id']}.png", OUT / "agents" / f"{name}.png", 128)
            index["agents"][agent["id"].upper()] = name

    for role_id, name in ROLES.items():
        fit(catalog / "CharacterRoles" / f"{role_id}.png", OUT / "roles" / f"{name}.png", 48)

    # 미니맵이 있는 맵만 실제로 뛰는 맵이다. 사격장, 기초 훈련, 난투 맵은 빠진다.
    for game_map in data["maps"]:
        if (catalog / "Maps" / f"{game_map['id']}.png").exists():
            name = slug(english(game_map))
            splash = catalog / "Maps" / f"{game_map['id']}_splash.png"
            cover(splash, OUT / "maps" / f"{name}_thumb.jpg", 216, 152)
            cover(splash, OUT / "maps" / f"{name}_banner.jpg", 780, 392, 55)
            index["maps"][game_map["id"].upper()] = name

    # 무기는 기본 스킨 그림을 쓴다. 무기 폴더의 그림은 흰 선화라 밝은 바탕에서 안 보인다.
    # 기본 스킨이 없는 건 모드 전용 무기나 스킬 장비라 넣지 않는다.
    weapons = {weapon["id"]: weapon for weapon in data["weapons"]}
    for skin in data["weaponSkins"]:
        standard = skin["name"]["defaultText"].startswith("Standard") or skin["name"]["defaultText"] == "Melee"
        render = catalog / "WeaponSkins" / f"{skin['defaultChromaId']}.png"
        if standard and skin["weaponId"] in weapons and render.exists():
            name = slug(english(weapons[skin["weaponId"]]))
            fit(render, OUT / "weapons" / f"{name}.png", 256)
            index["weapons"][skin["weaponId"].upper()] = name

    names = {t["tier"]: t["tierName"] for t in json.loads((tiers / "competitivetiers.json").read_text())["data"][-1]["tiers"]}
    for emblem in sorted(tiers.glob("*.png"), key=lambda p: int(p.stem)):
        number = int(emblem.stem)
        if number >= 3:
            name = slug(names[number])
            fit(emblem, OUT / "tiers" / f"{name}.png", 96)
            index["tiers"][number] = name

    write_index(index)


def write_index(index):
    def entries(table, quote_key=True):
        key = (lambda k: f'"{k}"') if quote_key else str
        return "\n".join(f'        {key(k)} to "{v}",' for k, v in sorted(table.items(), key=lambda kv: kv[1]))

    INDEX.write_text(f'''package com.ovalit.core.ui

// tools/bundle_assets.py가 만든 파일입니다. 손으로 고치지 말고 스크립트를 다시 돌립니다.
// 카탈로그 UUID(대문자)로 번들 파일 이름을 찾는 표입니다. 화면에 띄우는 이름은 여기서 가져오지 않습니다.
internal object GameAssetIndex {{
    val agents = mapOf(
{entries(index["agents"])}
    )

    val maps = mapOf(
{entries(index["maps"])}
    )

    val weapons = mapOf(
{entries(index["weapons"])}
    )

    /** 경기 응답의 `competitiveTier` 번호입니다. */
    val tiers = mapOf(
{entries(index["tiers"], quote_key=False)}
    )
}}
''')


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
