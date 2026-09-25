package com.ovalit.core.model

/**
 * VAL-CONTENT에서 받은 요원·무기 이름입니다. ID와 이름을 잇는 표를 코드에 직접 적지 않습니다.
 *
 * 카탈로그는 패치 뒤에 사람이 손으로 갱신해서 새 요원이나 무기가 한동안 빠져 있을 수 있습니다.
 * 그럴 때 화면에는 "알 수 없는 요원"이 뜹니다.
 */
data class ContentCatalog(
    val agents: Map<AgentId, String>,
    val weapons: Map<WeaponId, WeaponInfo>,
) {
    companion object {
        val Empty = ContentCatalog(agents = emptyMap(), weapons = emptyMap())
    }
}

/** 계열은 VAL-CONTENT에 없어서 어디서 받을지 아직 정하지 못했습니다. */
data class WeaponInfo(
    val name: String,
    val category: WeaponCategory,
)

enum class WeaponCategory {
    RIFLE,
    PISTOL,
    SMG,
    SNIPER,
    SHOTGUN,
    MACHINE_GUN,
}
