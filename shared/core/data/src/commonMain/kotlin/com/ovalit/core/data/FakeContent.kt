package com.ovalit.core.data

import com.ovalit.core.model.AgentId
import com.ovalit.core.model.MapId
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Role
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponId

// 가짜 경기에 나오는 요원, 맵, 무기, 플레이어입니다. ID는 콘텐츠 카탈로그의 실제 UUID라서 번들한
// 이미지가 그대로 붙습니다. 이미지를 찾을 때 대문자로 맞추므로 ID의 대소문자는 상관없습니다.

internal class FakeAgent(val id: AgentId, val name: String, val role: Role)

private fun agent(id: String, name: String, role: Role) = FakeAgent(AgentId(id), name, role)

internal val AgentPool = listOf(
    agent("add6443a-41bd-e414-f6ad-e58d267f4e95", "제트", Role.DUELIST),
    agent("f94c3b30-42be-e959-889c-5aa313dba261", "레이즈", Role.DUELIST),
    agent("a3bfb853-43b2-7238-a4f1-ad90e9e46bcc", "레이나", Role.DUELIST),
    agent("eb93336a-449b-9c1b-0a54-a891f7921d69", "피닉스", Role.DUELIST),
    agent("bb2a4828-46eb-8cd1-e765-15848195d751", "네온", Role.DUELIST),
    agent("7f94d92c-4234-0a36-9646-3a87eb8b5c89", "요루", Role.DUELIST),
    agent("0e38b510-41a8-5780-5e8f-568b2a4f2d6c", "아이소", Role.DUELIST),
    agent("320b2a48-4d9b-a075-30f1-1f93a9b638fa", "소바", Role.INITIATOR),
    agent("5f8d3a7f-467b-97f3-062c-13acf203c006", "브리치", Role.INITIATOR),
    agent("6f2a04ca-43e0-be17-7f36-b3908627744d", "스카이", Role.INITIATOR),
    agent("601dbbe7-43ce-be57-2a40-4abd24953621", "케이/오", Role.INITIATOR),
    agent("dade69b4-4f5a-8528-247b-219e5a1facd6", "페이드", Role.INITIATOR),
    agent("e370fa57-4757-3604-3648-499e1f642d3f", "게코", Role.INITIATOR),
    agent("8e253930-4c05-31dd-1b6c-968525494517", "오멘", Role.CONTROLLER),
    agent("9f0d8ba9-4140-b941-57d3-a7ad57c6b417", "브림스톤", Role.CONTROLLER),
    agent("707eab51-4836-f488-046a-cda6bf494859", "바이퍼", Role.CONTROLLER),
    agent("41fb69c1-4189-7b37-f117-bcaf1e96f1bf", "아스트라", Role.CONTROLLER),
    agent("95b78ed7-4637-86d9-7e41-71ba8c293152", "하버", Role.CONTROLLER),
    agent("1dbf2edd-4729-0984-3115-daa5eed44993", "클로브", Role.CONTROLLER),
    agent("1e58de9c-4950-5125-93e9-a0aee9f98746", "킬조이", Role.SENTINEL),
    agent("117ed9e3-49f3-6512-3ccf-0cada7e3823b", "사이퍼", Role.SENTINEL),
    agent("569fdd95-4d10-43ab-ca70-79becc718b46", "세이지", Role.SENTINEL),
    agent("22697a3d-45bf-8dd7-4fec-84a9e28c69d7", "체임버", Role.SENTINEL),
    agent("cc8b64c8-4b25-4ff9-6e7f-37b4da43d235", "데드록", Role.SENTINEL),
    agent("efba5359-4016-a1e5-7626-b1ae76895940", "바이스", Role.SENTINEL),
)

internal class WeightedAgent(val agent: FakeAgent, val weight: Double)

/** 내가 고르는 요원입니다. 타격대를 주로 하고 가끔 다른 역할을 합니다. */
internal val MyAgents = listOf(
    WeightedAgent(AgentPool[0], 0.45),
    WeightedAgent(AgentPool[1], 0.30),
    WeightedAgent(AgentPool[7], 0.15),
    WeightedAgent(AgentPool[13], 0.07),
    WeightedAgent(AgentPool[19], 0.03),
)

internal class FakeMap(val id: MapId, val name: String)

private fun map(id: String, name: String) = FakeMap(MapId(id), name)

internal val FakeMaps = listOf(
    map("7eaecc1b-4337-bbf6-6ab9-04b8f06b3319", "어센트"),
    map("2bee0dc9-4ffe-519b-1cbd-7fbe763a6047", "헤이븐"),
    map("2c9d57ec-4431-9c5e-2939-8f9ef6dd5cba", "바인드"),
    map("2fe4ed3a-450a-948b-6d6b-e89a78e680a9", "로터스"),
    map("d960549e-485c-e861-8d71-aa9d1aed12a2", "스플릿"),
    map("e2ad5c54-4114-a870-9641-8ea21279579a", "아이스박스"),
    map("fd267378-4d1d-484f-ff52-77821ed10dc2", "펄"),
    map("92584fbe-486a-b1b2-9faa-39b0f486b498", "선셋"),
    map("224b0a95-48b9-f703-1bd8-67aca101a61f", "어비스"),
    map("1c18ab1f-420d-0d8b-71d0-77ad3c439115", "코로드"),
)

internal class FakeWeapon(
    val id: WeaponId,
    val name: String,
    val category: WeaponCategory,
    val weight: Double,
    val headshotRate: Double,
)

private fun weapon(id: String, name: String, category: WeaponCategory, weight: Double, headshotRate: Double) =
    FakeWeapon(WeaponId(id), name, category, weight, headshotRate)

internal val FakeRifles = listOf(
    weapon("EE8E8D15-496B-07AC-E5F6-8FAE5D4C7B1A", "팬텀", WeaponCategory.RIFLE, 0.45, 0.24),
    weapon("9C82E19D-4575-0200-1A81-3EACF00CF872", "밴달", WeaponCategory.RIFLE, 0.30, 0.21),
    weapon("4ADE7FAA-4CF1-8376-95EF-39884480959B", "가디언", WeaponCategory.RIFLE, 0.06, 0.30),
    weapon("AE3DE142-4D85-2547-DD26-4E90BED35CF7", "불독", WeaponCategory.RIFLE, 0.04, 0.18),
    weapon("462080D1-4035-2937-7C09-27AA2A5C27A7", "스펙터", WeaponCategory.SMG, 0.07, 0.16),
    weapon("A03B24D3-4319-996D-0F8C-94BBFBA1DFC7", "오퍼레이터", WeaponCategory.SNIPER, 0.04, 0.10),
    weapon("EC845BF4-4F79-DDDA-A3DA-0DB3774B2794", "저지", WeaponCategory.SHOTGUN, 0.02, 0.08),
    weapon("63E6C2B6-4A8E-869C-3D4C-E38355226584", "오딘", WeaponCategory.MACHINE_GUN, 0.02, 0.12),
)

internal val FakePistols = listOf(
    weapon("1BAA85B4-4C70-1284-64BB-6481DFC3BB4E", "고스트", WeaponCategory.PISTOL, 0.5, 0.28),
    weapon("E336C6B8-418D-9340-D77F-7A9E4CFE0702", "셰리프", WeaponCategory.PISTOL, 0.3, 0.35),
    weapon("29A0CFAB-485B-F5D5-779A-B59F85E204A8", "클래식", WeaponCategory.PISTOL, 0.2, 0.22),
)

internal val FakeCards = listOf(
    "52995983-424C-9517-2FE8-3795E092EDB8",
    "867E85C2-4F4E-797B-7C94-A0B403FE22F3",
    "1FB0BEE0-49DB-FB51-B090-BC834BABDB2B",
    "89FDD50E-439B-EBEB-0EF2-AF8271550943",
    "970C5624-42B9-8808-387E-BD9BACB2EF13",
    "BFBC000C-4121-3227-E7F5-A3ABA576FA3C",
    "CABD47C0-44B9-A3E0-F100-EA87B692DC86",
    "E9368C54-41BA-5D07-F9AA-7F98650E9885",
    "D84BFA7E-4445-F315-F988-7FAC509E4122",
    "F823AD0B-408B-A2CB-FB6D-0D8F32061566",
    "2630C24B-424D-974F-98B4-A1ADF1B49C72",
    "13B0954B-4347-6698-1141-4589E6EF726D",
    "9FB348BC-41A0-91AD-8A3E-818035C4E561",
).map(::PlayerCardId)

internal class FakePlayer(val id: PlayerId, val riotId: String, val card: PlayerCardId)

/** 같이 매칭되는 모르는 사람들입니다. 앞의 다섯 이름은 목업 스코어보드에서 가져왔습니다. */
internal val Strangers = listOf(
    "Hwan#KR2", "bloom#1004", "난나야#KR1", "Ash#KR1", "rev#9922", "하늘#KR1", "moonlight#KR3", "도윤#0412",
    "pixel#KR1", "새벽#KR2", "Ryu#KR5", "보라#KR1", "kite#7777", "태오#KR4", "Nova#KR2", "은하#KR9",
    "blink#KR1", "소금#KR3", "zed#1212", "유나#KR6", "hush#KR1", "달빛#KR8", "Echo#KR3", "민트#KR2",
).mapIndexed { index, riotId ->
    FakePlayer(PlayerId("fake-player-$index"), riotId, FakeCards[index % FakeCards.size])
}

/** 목업의 친구들입니다. 준호, 민석, 재현은 가끔 내 경기에 같은 편으로 들어옵니다. */
internal val FakeFriendPlayers = listOf(
    FakePlayer(PlayerId("fake-junho"), "준호#KR1", FakeCards[3]),
    FakePlayer(PlayerId("fake-minseok"), "민석#KR3", FakeCards[5]),
    FakePlayer(PlayerId("fake-jaehyun"), "재현#KR2", FakeCards[8]),
)

internal val FakeFriendIds = FakeFriendPlayers.map { it.id }

internal val FakeFriendProfiles = FakeFriendPlayers.associateBy { it.id }

/** 친구는 아니지만 앱을 쓰는 사람들입니다. 스코어보드에서 누르면 친구 요청을 보낼 수 있습니다. */
internal val StrangersUsingApp = setOf(Strangers[1].id, Strangers[3].id, Strangers[5].id)

/** 서버가 내려줄 티어 이름을 대신합니다. 번호는 경기 응답의 `competitiveTier`입니다. */
internal val FakeTiers: Map<Int, String> = buildMap {
    val ranks = listOf("아이언", "브론즈", "실버", "골드", "플래티넘", "다이아몬드", "초월자", "불멸")
    ranks.forEachIndexed { rank, name ->
        (1..3).forEach { division -> put(3 + rank * 3 + division - 1, "$name $division") }
    }
    put(27, "레디언트")
}
