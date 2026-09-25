package com.ovalit.core.model

/**
 * 우리 팀 한 사람당 평균 장비 가치가 이 값보다 적으면 이코, [FULL_BUY_MIN_LOADOUT] 이상이면 풀바이, 그
 * 사이는 포스바이로 칩니다. 라이플과 중형 방어구를 합치면 3,900이라 풀바이 기준을 거기 뒀습니다. 공식 기준이
 * 없어서 시작 기준선이고 실데이터를 보고 조정합니다.
 */
const val ECO_MAX_LOADOUT = 2_000
const val FULL_BUY_MIN_LOADOUT = 3_900

enum class BuyType {
    /** 전반과 후반 첫 라운드입니다. 모두 같은 크레드로 시작해서 이코·포스바이로 가르지 않습니다. */
    PISTOL,
    ECO,
    FORCE_BUY,
    FULL_BUY,
}

/** 장비 가치를 모르거나 이코노미 규칙이 다른 모드면 `null`입니다. */
fun Round.buyType(queue: Queue): BuyType? {
    val half = queue.halfRounds ?: return null
    if (!queue.hasEconomy) return null
    if (number == 1 || number == half + 1) return BuyType.PISTOL
    val loadout = economy?.teamLoadout ?: return null
    return when {
        loadout < ECO_MAX_LOADOUT -> BuyType.ECO
        loadout < FULL_BUY_MIN_LOADOUT -> BuyType.FORCE_BUY
        else -> BuyType.FULL_BUY
    }
}
