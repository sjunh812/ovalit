package com.ovalit.core.model

/**
 * 홈과 경기 탭 위쪽 큐 칩입니다. 기본은 [COMPETITIVE_AND_UNRATED]입니다.
 *
 * 경쟁과 일반은 13라운드 선취에 이코노미 규칙이 같아 합쳐도 통계가 깨지지 않습니다.
 * 나머지 모드는 라운드 수와 크레드 규칙이 달라 [OTHER]로 따로 봅니다.
 *
 * @property queues 경기 탭 목록에 띄우는 큐입니다. 리포트와 평균은 [countedQueues]만 셉니다.
 */
enum class QueueFilter(val queues: Set<Queue>) {
    COMPETITIVE_AND_UNRATED(setOf(Queue.COMPETITIVE, Queue.UNRATED)),
    COMPETITIVE(setOf(Queue.COMPETITIVE)),
    UNRATED(setOf(Queue.UNRATED)),
    OTHER(Queue.entries.toSet() - Queue.COMPETITIVE - Queue.UNRATED),
    ;

    /**
     * 리포트와 평균에 넣는 큐입니다. 데스매치처럼 규칙이 달라 목록에만 두는 모드([Queue.countsInReports])를 뺐습니다.
     * 기타는 스파이크 돌격, 신속 플레이, 프리미어, 복제만 셉니다.
     */
    val countedQueues: Set<Queue> = queues.filterTo(mutableSetOf()) { it.countsInReports }

    /**
     * 기타 모드는 K/D, KDA, 헤드샷만 봅니다.
     * 모드마다 라운드 수와 무기, 크레드 규칙이 달라 라운드당 값인 전투점수와 피해량은 경쟁·일반과 같은 잣대로 볼 수 없습니다.
     */
    val fixedMetrics: List<FixedMetric>
        get() = if (this == OTHER) listOf(FixedMetric.KD, FixedMetric.KDA, FixedMetric.HEADSHOT_RATE) else FixedMetric.entries

    val hasDynamicMetrics: Boolean get() = this != OTHER

    companion object {
        /**
         * 큐 칩이 없는 화면(프로필, S5, S6, S7, 친구 목록)이 세는 큐입니다.
         * 기타 모드는 라운드 수와 크레드 규칙이 달라 섞으면 비율이 틀어집니다.
         */
        val PROFILE = COMPETITIVE_AND_UNRATED
    }
}
