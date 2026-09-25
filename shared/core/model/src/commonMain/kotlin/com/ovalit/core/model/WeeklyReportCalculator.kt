package com.ovalit.core.model

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

const val MIN_MATCHES_PER_REPORT = 5
const val MAX_REPORT_WEEKS = 4
const val BASELINE_WEEKS = 4
const val TREND_WEEKS = 8
const val MIN_TREND_ROUNDS = 40

/**
 * 주는 [timeZone] 기준 월요일 0시에 바뀝니다. 이번 주는 [now]가 속한, 아직 끝나지 않은 주입니다.
 *
 * 이번 주에 뛴 경기가 없으면 지난주에서 끝나는 기간을 봅니다. 그 기간이
 * [MIN_MATCHES_PER_REPORT]경기에 못 미치면 한 주씩 넓혀 [MAX_REPORT_WEEKS]주까지 봅니다.
 */
fun Iterable<Match>.weeklyReport(
    now: Instant,
    timeZone: TimeZone,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
): WeeklyReport {
    val counted = filter { it.queue in queueFilter.queues }
    val act = counted.maxByOrNull { it.startedAt }?.act
        ?: return WeeklyReport.NotEnoughMatches(played = 0)
    val matchesByWeek = counted
        .filter { it.act == act }
        .groupBy { it.startedAt.weekStart(timeZone) }

    // 월요일 아침에 "최근 2주"로 넓히면 전부 지난주 경기인데 이번 주가 섞인 것처럼 읽힌다.
    val thisWeek = now.weekStart(timeZone)
    val includesThisWeek = thisWeek in matchesByWeek
    val end = if (includesThisWeek) thisWeek.plusWeeks(1) else thisWeek
    val periods = (1..MAX_REPORT_WEEKS).map { weeks ->
        ReportPeriod(firstDay = end.minusWeeks(weeks), weeks = weeks, includesThisWeek = includesThisWeek)
    }
    val period = periods.firstOrNull { period ->
        matchesByWeek.between(period.firstDay, end).size >= MIN_MATCHES_PER_REPORT
    } ?: return WeeklyReport.NotEnoughMatches(
        played = matchesByWeek.between(periods.last().firstDay, end).size,
    )

    val periodMatches = matchesByWeek.between(period.firstDay, end)
    val metrics = periodMatches.totalMetrics()
    val baseline = matchesByWeek.baselineBefore(period.firstDay)
    val mainRole = periodMatches.mainRole()
    val history = (1..VOLATILITY_WEEKS).mapNotNull { weeksBefore ->
        matchesByWeek[period.firstDay.minusWeeks(weeksBefore)]?.totalMetrics()
    }

    return WeeklyReport.Ready(
        act = act,
        period = period,
        metrics = metrics,
        baseline = baseline,
        mainRole = mainRole,
        dynamic = if (queueFilter.hasDynamicMetrics) {
            selectDynamicMetrics(metrics, baseline?.metrics, history, mainRole)
        } else {
            emptyList()
        },
        insight = if (queueFilter.hasDynamicMetrics) periodMatches.sideInsight(mainRole) else null,
        trend = counted.trendWeeks(end = end, period = period, timeZone = timeZone),
    )
}

// 첫 수집 범위가 8주라 막대도 8주다. 지난 액트 주도 그리되 경계에 전환 표시를 한다.
private fun List<Match>.trendWeeks(end: LocalDate, period: ReportPeriod, timeZone: TimeZone): List<TrendWeek> {
    val byWeek = groupBy { it.startedAt.weekStart(timeZone) }
    var previousAct: ActId? = null

    return (TREND_WEEKS downTo 1).map { weeksBefore ->
        val firstDay = end.minusWeeks(weeksBefore)
        val week = byWeek[firstDay].orEmpty()
        val act = week.maxByOrNull { it.startedAt }?.act
        val metrics = week.filter { it.act == act }.totalMetrics()
        TrendWeek(
            firstDay = firstDay,
            act = act,
            metrics = metrics.takeIf { it.rounds >= MIN_TREND_ROUNDS },
            startsNewAct = act != null && previousAct != null && act != previousAct,
            inPeriod = firstDay >= period.firstDay,
        ).also { if (act != null) previousAct = act }
    }
}

private fun List<Match>.mainRole(): Role? = this
    .mapNotNull { match -> match.myRole?.let { it to match.rounds.size } }
    .groupBy({ it.first }, { it.second })
    .maxByOrNull { (_, rounds) -> rounds.sum() }
    ?.key

private fun Map<LocalDate, List<Match>>.baselineBefore(periodStart: LocalDate): Baseline? {
    // 이번 액트 첫 경기가 4주 안쪽이면 거기서부터 센다. 안 그러면 2주치 경기에 "지난 4주 평균"이 붙는다.
    val start = maxOf(periodStart.minusWeeks(BASELINE_WEEKS), keys.min())
    val matches = between(start, periodStart)
    if (matches.size < MIN_MATCHES_PER_REPORT) return null

    return Baseline(metrics = matches.totalMetrics(), weeks = start.daysUntil(periodStart) / 7)
}

private fun Instant.weekStart(timeZone: TimeZone): LocalDate {
    val date = toLocalDateTime(timeZone).date
    return date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
}

private fun Map<LocalDate, List<Match>>.between(from: LocalDate, until: LocalDate): List<Match> =
    filterKeys { it >= from && it < until }.values.flatten()

private fun List<Match>.totalMetrics(): MatchMetrics = map { it.metrics() }.sum()

private fun LocalDate.plusWeeks(weeks: Int) = plus(weeks, DateTimeUnit.WEEK)

private fun LocalDate.minusWeeks(weeks: Int) = minus(weeks, DateTimeUnit.WEEK)
