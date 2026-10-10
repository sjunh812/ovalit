package com.ovalit.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * 홈의 주간 리포트입니다. 한 액트 안에서 고른 큐의 경기만 담습니다.
 *
 * 액트가 바뀌면 랭크가 초기화되고 매칭 난이도가 달라져서 가장 최근 경기의 액트만 봅니다.
 * [Ready.trend]만 앞 액트 주도 그리고 경계를 표시합니다.
 */
sealed interface WeeklyReport {

    /**
     * @property baseline "지난 4주 평균"에 쓰는 비교 기준입니다. 그 기간 경기가 5경기에 못 미치면 `null`입니다.
     * @property mainRole 기간 중 라운드를 가장 많이 뛴 역할입니다. 역할을 아는 경기가 없으면 `null`입니다.
     * @property mainRoleShare 역할을 아는 라운드 중 [mainRole]로 뛴 라운드의 비중입니다. 화면에는 "타격대 78%"로 띄웁니다.
     * @property dynamic 동적 칸(3~5개)입니다.
     *   [Movement.MOVED]가 하나도 없고 [Movement.STEADY]가 있을 때만 "큰 변화 없음"을 띄웁니다.
     *   모두 [Movement.UNKNOWN]이면 판단을 보류했다고 적습니다.
     *   [QueueFilter.OTHER]면 빈 목록입니다.
     * @property insight 개선 포인트 문장입니다. 기준을 넘는 격차가 없거나 [QueueFilter.OTHER]면 `null`입니다.
     * @property trend 지표 설명 시트의 주별 막대입니다.
     *   기간 마지막 주에서 끝나는 [TREND_WEEKS]주이고 오래된 주가 앞에 옵니다.
     * @property results 기간 경기의 승패입니다. 오래된 경기가 앞에 오고, 비겼거나 결과를 모르면 `null`입니다.
     * @property agents 기간에 많이 뛴 요원 순서입니다. 홈의 요원 칸에 씁니다.
     * @property weapons 기간에 킬을 많이 낸 무기 순서입니다. 홈의 무기 칸에 씁니다.
     * @property note 홈 "이번 주 짚을 점"입니다. 짚을 게 없거나 [QueueFilter.OTHER]면 `null`입니다.
     */
    data class Ready(
        val act: ActId,
        val period: ReportPeriod,
        val metrics: MatchMetrics,
        val baseline: Baseline?,
        val mainRole: Role?,
        val mainRoleShare: Double?,
        val dynamic: List<DynamicSlot>,
        val insight: Insight?,
        val trend: List<TrendWeek>,
        val results: List<Boolean?> = emptyList(),
        val agents: List<AgentStats> = emptyList(),
        val weapons: List<WeaponStats> = emptyList(),
        val note: WeekNote? = null,
    ) : WeeklyReport {
        val wins: Int get() = results.count { it == true }
        val losses: Int get() = results.count { it == false }

        /** 비긴 경기는 분모에서 뺍니다. */
        val winRate: Double? get() = wins over (wins + losses)
    }

    /**
     * 최대 기간까지 넓혀도 경기가 모자랍니다. [played]는 그 기간에 이번 액트에서 뛴 경기 수입니다.
     *
     * @property notCounted 최근 [MAX_REPORT_WEEKS]주 동안 고른 큐에서 뛰었지만 규칙이 달라 리포트에 넣지 않는 모드(데스매치 등)의 경기 수입니다.
     *   기타 칩에서 데스매치만 뛴 사람에게 "뛴 경기가 없어요"라고 하지 않으려고 셉니다.
     */
    data class NotEnoughMatches(val played: Int, val notCounted: Int = 0) : WeeklyReport
}

/**
 * @property firstDay 늘 월요일입니다. 여기서부터 [weeks]주를 덮습니다.
 * @property includesThisWeek 이번 주에 뛴 경기가 없으면 `false`이고 기간이 지난주에서 끝납니다.
 *   화면에는 `true`면 "이번 주"와 "최근 N주", `false`면 "지난주"와 "지난 N주"로 띄웁니다.
 */
data class ReportPeriod(
    val firstDay: LocalDate,
    val weeks: Int,
    val includesThisWeek: Boolean,
) {
    val lastDay: LocalDate get() = firstDay.plus(weeks * 7 - 1, DateTimeUnit.DAY)
}

/**
 * @property weeks 보통 [BASELINE_WEEKS]입니다.
 *   그 사이에 액트가 시작됐거나 모아 둔 경기가 그보다 짧으면 줄어듭니다.
 *   화면에는 "지난 N주 평균"으로 띄웁니다.
 */
data class Baseline(
    val metrics: MatchMetrics,
    val weeks: Int,
)

/**
 * 지표 설명 시트의 주별 막대 하나입니다.
 *
 * @property act 그 주에 뛴 가장 최근 액트입니다. 한 주에 액트가 둘이면 새 액트 경기만 셉니다.
 *   그 주에 경기가 없으면 `null`입니다.
 * @property metrics 그 주 합계입니다. 그 주에 경기가 없으면 `null`입니다.
 * @property startsNewAct 앞 주와 액트가 다르면 `true`입니다. 화면은 이 막대 앞에 세로선을 긋습니다.
 */
data class TrendWeek(
    val firstDay: LocalDate,
    val act: ActId?,
    val metrics: MatchMetrics?,
    val startsNewAct: Boolean,
    val inPeriod: Boolean,
) {
    /**
     * 라운드가 [MIN_TREND_ROUNDS]에 못 미치는 주입니다.
     * 값이 크게 흔들려서 평소 범위에서는 빼고, 막대는 다른 주와 같게 그립니다.
     */
    val sparse: Boolean get() = metrics != null && metrics.rounds < MIN_TREND_ROUNDS
}

/**
 * 지표 설명 시트의 "평소에는 어느 정도였나요?"에 쓰는 범위입니다.
 * 비교 대상은 본인의 과거뿐이라 남의 평균 대신 내 주간 값의 범위를 보여줍니다.
 *
 * @property weeks 가장 오래된 주부터 기간 직전까지의 주 수입니다. 화면에는 "지난 N주 동안"으로 띄웁니다.
 */
data class UsualRange(
    val min: Double,
    val max: Double,
    val weeks: Int,
)

/**
 * 기간 앞 막대 가운데 이번 액트이면서 라운드를 [MIN_TREND_ROUNDS]번 넘게 뛴 주만 씁니다.
 * 그런 주가 [MIN_VOLATILITY_WEEKS]주가 안 되면 `null`입니다.
 * 두세 주만 보고 평소라고 하면 어쩌다 잘 풀린 주가 평소가 됩니다.
 */
fun WeeklyReport.Ready.usualRange(value: (MatchMetrics) -> Double?): UsualRange? {
    val weeks = trend
        .filter { !it.inPeriod && it.act == act && !it.sparse }
        .mapNotNull { week -> week.metrics?.let(value)?.let { week.firstDay to it } }
    if (weeks.size < MIN_VOLATILITY_WEEKS) return null

    val values = weeks.map { it.second }
    return UsualRange(
        min = values.min(),
        max = values.max(),
        weeks = weeks.first().first.daysUntil(period.firstDay) / 7,
    )
}
