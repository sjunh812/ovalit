package com.ovalit.core.model

/**
 * 홈 위쪽 큐 칩입니다. 기본은 [COMPETITIVE_AND_UNRATED]입니다.
 *
 * 경쟁과 일반은 13라운드 선취에 이코노미 규칙이 같아 합쳐도 통계가 깨지지 않습니다. 나머지
 * 모드는 라운드 수와 크레드 규칙이 달라 [OTHER]로 따로 봅니다.
 */
enum class QueueFilter(val queues: Set<Queue>) {
    COMPETITIVE_AND_UNRATED(setOf(Queue.COMPETITIVE, Queue.UNRATED)),
    COMPETITIVE(setOf(Queue.COMPETITIVE)),
    UNRATED(setOf(Queue.UNRATED)),
    OTHER(setOf(Queue.SPIKE_RUSH, Queue.SWIFTPLAY, Queue.OTHER)),
    ;

    /**
     * 기타 모드는 K/D와 헤드샷만 봅니다. 데스매치처럼 라운드가 없는 모드가 섞여 있어 라운드당 값인 전투점수와 피해량은
     * 뺍니다.
     */
    val fixedMetrics: List<FixedMetric>
        get() = if (this == OTHER) listOf(FixedMetric.KD, FixedMetric.HEADSHOT_RATE) else FixedMetric.entries

    val hasDynamicMetrics: Boolean get() = this != OTHER
}
