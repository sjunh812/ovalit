package com.ovalit.core.model

/**
 * S0-4와 설정에서 고르는 관심사입니다. 달라진 점을 고를 때 역할의 우선 지표 다음으로 이 지표들을 앞에
 * 둡니다. 빈칸을 채울 때도 기본 지표보다 먼저 씁니다.
 */
enum class Focus(internal val metrics: List<DynamicMetric>) {
    AIM(listOf(DynamicMetric.FIRST_DUEL_WIN_RATE)),
    ROUND_PLAY(listOf(DynamicMetric.FORCE_BUY_WIN_RATE, DynamicMetric.ECO_WIN_RATE, DynamicMetric.FULL_BUY_WIN_RATE)),
    CONSISTENCY(listOf(DynamicMetric.KAST, DynamicMetric.SURVIVAL_RATE)),

    /** 그 주에 많이 움직인 순서만 봅니다. */
    NONE(emptyList()),
}
