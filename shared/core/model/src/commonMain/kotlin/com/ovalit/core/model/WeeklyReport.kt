package com.ovalit.core.model

import kotlinx.datetime.LocalDate

/**
 * 홈의 주간 리포트입니다. 한 액트 안의 경쟁전과 일반전만 담습니다.
 *
 * 액트가 바뀌면 랭크가 초기화되고 매칭 난이도가 달라집니다. 경계를 넘겨 합치면 비교가 의미를
 * 잃으니, 가장 최근 경기의 액트만 봅니다.
 */
sealed interface WeeklyReport {

    /**
     * @property baseline "지난 4주 평균"에 쓰는 비교 기준입니다. 그 기간 경기가 모자라면 없습니다.
     * @property mainRole 기간 중 라운드를 가장 많이 뛴 역할입니다. 역할을 아는 경기가 없으면 없습니다.
     * @property dynamic 동적 3칸입니다. 하나도 [Movement.MOVED]가 아닐 때만 "큰 변화 없음"을 띄웁니다.
     */
    data class Ready(
        val act: ActId,
        val period: ReportPeriod,
        val metrics: MatchMetrics,
        val baseline: Baseline?,
        val mainRole: Role?,
        val dynamic: List<DynamicSlot>,
    ) : WeeklyReport

    /** 최대 기간까지 넓혀도 경기가 모자랍니다. [played]는 그 기간에 이번 액트에서 뛴 경기 수입니다. */
    data class NotEnoughMatches(val played: Int) : WeeklyReport
}

/**
 * @property firstDay 늘 월요일입니다. 여기서부터 이번 주까지 [weeks]주를 덮습니다.
 * 1주면 "이번 주", 그보다 길면 "최근 N주"로 띄웁니다.
 */
data class ReportPeriod(
    val firstDay: LocalDate,
    val weeks: Int,
)

/**
 * @property weeks 보통 [BASELINE_WEEKS]입니다. 그 사이에 액트가 시작됐거나 모아 둔 경기가 그보다
 * 짧으면 줄어듭니다. 화면에는 "지난 N주 평균"으로 띄웁니다.
 */
data class Baseline(
    val metrics: MatchMetrics,
    val weeks: Int,
)
