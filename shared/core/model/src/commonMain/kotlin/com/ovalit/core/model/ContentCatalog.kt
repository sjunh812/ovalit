package com.ovalit.core.model

/**
 * ID를 화면에 띄울 이름으로 바꾸는 표입니다. ID와 이름을 잇는 표를 앱 코드에 직접 적지 않습니다.
 *
 * 요원·무기·맵 이름은 VAL-CONTENT에서, 무기 계열은 콘텐츠 카탈로그에서 옵니다. 티어 이름은 Riot 공식
 * 자료에 없어서 우리 서버가 들고 있다가 내려줍니다. 카탈로그는 패치 뒤에 사람이 손으로 갱신해서 새
 * 요원이나 무기가 한동안 빠져 있을 수 있고, 그럴 때 화면에는 "알 수 없는 요원"이 뜹니다.
 *
 * @property tiers 경기 응답의 `competitiveTier` 번호를 "플래티넘 2" 같은 이름으로 바꿉니다.
 */
data class ContentCatalog(
    val agents: Map<AgentId, String>,
    val weapons: Map<WeaponId, WeaponInfo>,
    val maps: Map<MapId, String>,
    val tiers: Map<Int, String>,
) {
    companion object {
        val Empty = ContentCatalog(agents = emptyMap(), weapons = emptyMap(), maps = emptyMap(), tiers = emptyMap())
    }
}

data class WeaponInfo(
    val name: String,
    val category: WeaponCategory,
)

/** 콘텐츠 카탈로그의 `weapons[].category`입니다. */
enum class WeaponCategory {
    RIFLE,
    PISTOL,
    SMG,
    SNIPER,
    SHOTGUN,
    MACHINE_GUN,
    MELEE,
}
