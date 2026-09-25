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
    focus: Focus = Focus.NONE,
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
    val roleRounds = periodMatches.roleRounds()
    val mainRole = roleRounds.maxByOrNull { it.value }?.key
    val history = (1..VOLATILITY_WEEKS).mapNotNull { weeksBefore ->
        matchesByWeek[period.firstDay.minusWeeks(weeksBefore)]?.totalMetrics()
    }

    return WeeklyReport.Ready(
        act = act,
        period = period,
        metrics = metrics,
        baseline = baseline,
        mainRole = mainRole,
        mainRoleShare = mainRole?.let { roleRounds.getValue(it) over roleRounds.values.sum() },
        dynamic = if (queueFilter.hasDynamicMetrics) {
            selectDynamicMetrics(metrics, baseline?.metrics, history, mainRole, focus)
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

// 역할마다 뛴 라운드 수다. 역할을 모르는 경기는 뺀다.
private fun List<Match>.roleRounds(): Map<Role, Int> = this
    .mapNotNull { match -> match.myRole?.let { it to match.rounds.size } }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, rounds) -> rounds.sum() }

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

/** 요원·무기 화면이 보는 경기입니다. 고른 큐에서 가장 최근 경기의 액트만 남깁니다. */
fun Iterable<Match>.currentActMatches(queueFilter: QueueFilter): List<Match> {
    val counted = filter { it.queue in queueFilter.queues }
    val act = counted.maxByOrNull { it.startedAt }?.act ?: return emptyList()
    return counted.filter { it.act == act }
}

/**
 * S6 무기 화면에 쓰는 집계입니다. 목록은 이번 액트 전체를 보고, 위쪽 세 무기는 홈 리포트와 같은
 * 기간을 그 앞 4주와 비교합니다.
 */
fun Iterable<Match>.weaponReport(
    now: Instant,
    timeZone: TimeZone,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
): WeaponReport {
    val matches = currentActMatches(queueFilter)
    val weapons = matches.weaponStats()
    val period = (weeklyReport(now, timeZone, queueFilter) as? WeeklyReport.Ready)?.period
    val byWeek = matches.groupBy { it.startedAt.weekStart(timeZone) }

    return WeaponReport(
        matches = matches.size,
        kills = weapons.sumOf { it.kills },
        weapons = weapons,
        highlights = weapons.take(HIGHLIGHTED_WEAPONS).map { byWeek.highlight(it, period) },
    )
}

private fun Map<LocalDate, List<Match>>.highlight(act: WeaponStats, period: ReportPeriod?): WeaponHighlight {
    if (period == null) return WeaponHighlight(act, null, null, baselineWeeks = 0, movement = Movement.UNKNOWN)
    fun List<Match>.stats() = weaponStats().firstOrNull { it.weapon == act.weapon }

    val end = period.firstDay.plusWeeks(period.weeks)
    val current = between(period.firstDay, end).stats()
    val start = maxOf(period.firstDay.minusWeeks(BASELINE_WEEKS), keys.min())
    val baseline = between(start, period.firstDay).stats()
    val weekly = (1..VOLATILITY_WEEKS)
        .mapNotNull { weeksBefore -> this[period.firstDay.minusWeeks(weeksBefore)]?.stats() }
        .filter { it.isMeasurable }
        .mapNotNull { it.headshotRate }

    val now = current?.takeIf { it.isMeasurable }?.headshotRate
    val usual = baseline?.takeIf { it.isMeasurable }?.headshotRate
    val movement = if (now != null && usual != null) assessMovement(now, usual, weekly).movement else Movement.UNKNOWN

    return WeaponHighlight(
        act = act,
        current = current,
        baseline = baseline,
        baselineWeeks = start.daysUntil(period.firstDay) / 7,
        movement = movement,
    )
}
