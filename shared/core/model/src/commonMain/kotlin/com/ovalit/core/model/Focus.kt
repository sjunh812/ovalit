package com.ovalit.core.model

/**
 * S0-4와 설정에서 고르는 관심사입니다. 달라진 점의 맨 앞에 이 지표들을 늘 둡니다. 움직이지 않았어도, 역할이
 * 크게 띄우지 않는 지표여도 넣습니다.
 */
enum class Focus(internal val metrics: List<DynamicMetric>) {
    AIM(listOf(DynamicMetric.FIRST_DUEL_WIN_RATE)),
    ROUND_PLAY(listOf(DynamicMetric.FORCE_BUY_WIN_RATE, DynamicMetric.ECO_WIN_RATE, DynamicMetric.FULL_BUY_WIN_RATE)),
    CONSISTENCY(listOf(DynamicMetric.KAST, DynamicMetric.SURVIVAL_RATE)),

    /** 그 주에 많이 움직인 순서만 봅니다. */
    NONE(emptyList()),
}
