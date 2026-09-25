package com.ovalit.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * 홈의 주간 리포트입니다. 한 액트 안에서 고른 큐의 경기만 담습니다.
 *
 * 액트가 바뀌면 랭크가 초기화되고 매칭 난이도가 달라집니다. 경계를 넘겨 합치면 비교가 의미를
 * 잃으니, 가장 최근 경기의 액트만 봅니다.
 */
sealed interface WeeklyReport {

    /**
     * @property baseline "지난 4주 평균"에 쓰는 비교 기준입니다. 그 기간 경기가 모자라면 없습니다.
     * @property mainRole 기간 중 라운드를 가장 많이 뛴 역할입니다. 역할을 아는 경기가 없으면 없습니다.
     * @property mainRoleShare 역할을 아는 라운드 중 [mainRole]로 뛴 라운드의 비중입니다. 화면에는 "타격대 78%"로
     * 띄웁니다. 역할만 적으면 그 기간에 그 역할만 한 것처럼 읽힙니다.
     * @property dynamic 동적 3칸입니다. 하나도 [Movement.MOVED]가 아닐 때만 "큰 변화 없음"을 띄웁니다.
     * [QueueFilter.OTHER]면 비어 있습니다.
     * @property insight 개선 포인트 문장입니다. 공수 격차가 기준을 넘지 않거나 [QueueFilter.OTHER]면 없습니다.
     * @property trend 지표 설명 시트의 주별 막대입니다. 기간 마지막 주에서 끝나는 [TREND_WEEKS]주이고
     * 오래된 주가 앞에 옵니다.
     */
    data class Ready(
        val act: ActId,
        val period: ReportPeriod,
        val metrics: MatchMetrics,
        val baseline: Baseline?,
        val mainRole: Role?,
        val mainRoleShare: Double?,
        val dynamic: List<DynamicSlot>,
        val insight: SideInsight?,
        val trend: List<TrendWeek>,
    ) : WeeklyReport

    /** 최대 기간까지 넓혀도 경기가 모자랍니다. [played]는 그 기간에 이번 액트에서 뛴 경기 수입니다. */
    data class NotEnoughMatches(val played: Int) : WeeklyReport
}

/**
 * @property firstDay 늘 월요일입니다. 여기서부터 [weeks]주를 덮습니다.
 * @property includesThisWeek 이번 주에 뛴 경기가 없으면 `false`이고 기간이 지난주에서 끝납니다.
 * 화면에는 `true`면 "이번 주"와 "최근 N주", `false`면 "지난주"와 "지난 N주"로 띄웁니다.
 */
data class ReportPeriod(
    val firstDay: LocalDate,
    val weeks: Int,
    val includesThisWeek: Boolean,
) {
    val lastDay: LocalDate get() = firstDay.plus(weeks * 7 - 1, DateTimeUnit.DAY)
}

/**
 * @property weeks 보통 [BASELINE_WEEKS]입니다. 그 사이에 액트가 시작됐거나 모아 둔 경기가 그보다
 * 짧으면 줄어듭니다. 화면에는 "지난 N주 평균"으로 띄웁니다.
 */
data class Baseline(
    val metrics: MatchMetrics,
    val weeks: Int,
)

/**
 * 지표 설명 시트의 주별 막대 하나입니다.
 *
 * @property act 그 주에 뛴 가장 최근 액트입니다. 한 주에 액트가 둘이면 새 액트 경기만 셉니다.
 * 액트 경계를 넘는 평균은 만들지 않습니다.
 * @property metrics 그 주 합계입니다. 라운드가 [MIN_TREND_ROUNDS]에 못 미치면 없습니다. 한두 판만
 * 뛴 주가 섞이면 막대 하나 때문에 추이가 흔들립니다.
 * @property startsNewAct 앞 주와 액트가 다르면 `true`입니다. 화면은 이 막대 앞에 세로선을 긋습니다.
 * @property inPeriod 리포트 기간에 든 주입니다.
 */
data class TrendWeek(
    val firstDay: LocalDate,
    val act: ActId?,
    val metrics: MatchMetrics?,
    val startsNewAct: Boolean,
    val inPeriod: Boolean,
)

/**
 * 지표 설명 시트의 "평소에는 어느 정도였나요?"에 쓰는 범위입니다. 비교 대상은 본인의 과거뿐이라
 * 남의 평균 대신 내 주간 값의 범위를 보여줍니다.
 *
 * @property weeks 가장 오래된 주부터 기간 직전까지의 주 수입니다. 화면에는 "지난 N주 동안"으로 띄웁니다.
 */
data class UsualRange(
    val min: Double,
    val max: Double,
    val weeks: Int,
)

/**
 * 기간 앞 막대 가운데 이번 액트이면서 값이 있는 주만 씁니다. 그런 주가 [MIN_VOLATILITY_WEEKS]주가
 * 안 되면 `null`입니다. 두세 주만 보고 평소라고 하면 어쩌다 잘 풀린 주가 평소가 됩니다.
 */
fun WeeklyReport.Ready.usualRange(value: (MatchMetrics) -> Double?): UsualRange? {
    val weeks = trend
        .filter { !it.inPeriod && it.act == act }
        .mapNotNull { week -> week.metrics?.let(value)?.let { week.firstDay to it } }
    if (weeks.size < MIN_VOLATILITY_WEEKS) return null

    val values = weeks.map { it.second }
    return UsualRange(
        min = values.min(),
        max = values.max(),
        weeks = weeks.first().first.daysUntil(period.firstDay) / 7,
    )
}
