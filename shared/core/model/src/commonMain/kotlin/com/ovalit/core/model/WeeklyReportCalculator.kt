package com.ovalit.core.model

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
 *
 * @param weaponCategories 개선 포인트가 같은 계열의 무기끼리 견줄 때 씁니다. 비어 있으면 무기끼리는 견주지 않습니다.
 */
fun Iterable<Match>.weeklyReport(
    now: Instant,
    timeZone: TimeZone,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
    focus: Focus = Focus.NONE,
    weaponCategories: Map<WeaponId, WeaponCategory> = emptyMap(),
): WeeklyReport {
    val counted = filter { it.queue in queueFilter.queues }
    val act = counted.maxByOrNull { it.startedAt }?.act
        ?: return WeeklyReport.NotEnoughMatches(played = 0)
    val matchesByWeek = counted
        .filter { it.act == act }
        .groupBy { it.startedAt.weekStart(timeZone) }

    val choice = matchesByWeek.choosePeriod(now, timeZone)
    val period = choice.period ?: return WeeklyReport.NotEnoughMatches(played = choice.played)
    val end = period.end

    val periodMatches = matchesByWeek.between(period.firstDay, end)
    val metrics = periodMatches.totalMetrics()
    val baseline = matchesByWeek.baselineBefore(period.firstDay)
    val roleRounds = periodMatches.roleRounds()
    val mainRole = roleRounds.maxByOrNull { it.value }?.key
    val history = matchesByWeek.weeksBefore(period.firstDay).map { it.totalMetrics() }

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
        insight = if (queueFilter.hasDynamicMetrics) matchesByWeek.actInsight(periodMatches, mainRole, focus, weaponCategories) else null,
        trend = counted.trendWeeks(end = end, period = period, timeZone = timeZone),
        results = periodMatches.sortedBy { it.startedAt }.map { it.myTeamWon },
        agents = periodMatches.agentReport().agents,
        weapons = periodMatches.weaponStats(),
        note = if (queueFilter.hasDynamicMetrics) {
            matchesByWeek.weekNote(periodMatches, period, metrics, baseline, history, mainRole)
        } else {
            null
        },
    )
}

// 한 주 경기를 둘로 나누면 표본이 작아 우연한 차이가 대부분이다. 이번 액트 경기로 견주고, 그 차이가 이번 기간에도
// 이어졌는지 기간 값을 붙인다. 기간이 액트 경기를 모두 담으면 위 숫자와 같아서 붙이지 않는다.
private fun Map<LocalDate, List<Match>>.actInsight(
    periodMatches: List<Match>,
    role: Role?,
    focus: Focus,
    categories: Map<WeaponId, WeaponCategory>,
): Insight? {
    val actMatches = values.flatten()
    val insight = actMatches.insight(role, focus, categories) ?: return null
    return if (periodMatches.size < actMatches.size) insight.during(periodMatches, categories) else insight
}

// 무기와 요원은 기간 성적을 바로 앞 비교 기준과 견준다. 무기는 S6 위쪽 표와 같은 창이다.
private fun Map<LocalDate, List<Match>>.weekNote(
    periodMatches: List<Match>,
    period: ReportPeriod,
    metrics: MatchMetrics,
    baseline: Baseline?,
    history: List<MatchMetrics>,
    role: Role?,
): WeekNote? {
    val usualMatches = between(baselineStart(period.firstDay), period.firstDay)
    val usualWeapons = usualMatches.weaponStats().associateBy { it.weapon }
    val weapons = periodMatches.weaponStats().map { stats ->
        WeaponTrend(weapon = stats.weapon, current = stats, baseline = usualWeapons[stats.weapon])
    }
    val usualAgents = usualMatches.groupBy { it.myAgent }
    val agentTrends = periodMatches.groupBy { it.myAgent }.map { (agent, matches) ->
        AgentTrend(agent = agent, current = matches.totalMetrics(), baseline = usualAgents[agent]?.totalMetrics())
    }
    return chooseWeekNote(
        current = metrics,
        baseline = baseline?.metrics,
        history = history,
        role = role,
        weapons = weapons,
        agentTrends = agentTrends,
        mixes = listOf(
            mixSlices(periodMatches, usualMatches) { match, round -> round.buyType(match.queue)?.let { MixGroup.Buy(it) } },
            mixSlices(periodMatches, usualMatches) { _, round -> round.economy?.myWeapon?.let { MixGroup.Weapon(it) } },
            agentSlices(periodMatches, usualMatches),
        ),
        // 8주 변동폭과 달리 이번 액트 앞 주를 모두 본다. 액트가 길면 8주 앞에 더 높은 주가 있을 수 있다.
        actWeeks = if (period.weeks == 1) filterKeys { it < period.firstDay }.values.map { it.totalMetrics() } else emptyList(),
    )
}

// 경기 안에서 라운드를 묶음으로 가른다. 어느 묶음에도 안 드는 라운드(장비 가치를 모르는 라운드)는 빠진다.
private fun mixSlices(current: List<Match>, usual: List<Match>, group: (Match, Round) -> MixGroup?): List<MixSlice> {
    fun List<Match>.split(): Map<MixGroup, MatchMetrics> = this
        .flatMap { match -> match.rounds.mapNotNull { group(match, it) }.distinct().map { key -> key to match.roundMetrics { group(match, it) == key } } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, metrics) -> metrics.sum() }
    val now = current.split()
    val before = usual.split()
    return (now.keys + before.keys).map { key -> MixSlice(key, now[key] ?: MatchMetrics.Empty, before[key] ?: MatchMetrics.Empty) }
}

// 요원은 판 전체로 가른다. 전투점수도 요원으로는 가를 수 있다.
private fun agentSlices(current: List<Match>, usual: List<Match>): List<MixSlice> {
    val now = current.groupBy { it.myAgent }.mapValues { (_, matches) -> matches.totalMetrics() }
    val before = usual.groupBy { it.myAgent }.mapValues { (_, matches) -> matches.totalMetrics() }
    return (now.keys + before.keys).map { agent ->
        MixSlice(MixGroup.Agent(agent), now[agent] ?: MatchMetrics.Empty, before[agent] ?: MatchMetrics.Empty)
    }
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

// 역할을 모르는 경기는 뺀다
private fun List<Match>.roleRounds(): Map<Role, Int> = this
    .mapNotNull { match -> match.myRole?.let { it to match.rounds.size } }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, rounds) -> rounds.sum() }

private fun Map<LocalDate, List<Match>>.baselineBefore(periodStart: LocalDate): Baseline? {
    val start = baselineStart(periodStart)
    val matches = between(start, periodStart)
    if (matches.size < MIN_MATCHES_PER_REPORT) return null

    return Baseline(metrics = matches.totalMetrics(), weeks = start.daysUntil(periodStart) / 7)
}

/**
 * 기간을 못 정했으면 [period]가 `null`이고 [played]에 최대 [MAX_REPORT_WEEKS]주 동안 뛴 경기 수를 담습니다.
 */
private class PeriodChoice(val period: ReportPeriod?, val played: Int)

private fun Map<LocalDate, List<Match>>.choosePeriod(now: Instant, timeZone: TimeZone): PeriodChoice {
    // 월요일 아침에 "최근 2주"로 넓히면 전부 지난주 경기인데 이번 주가 섞인 것처럼 읽힌다
    val thisWeek = now.weekStart(timeZone)
    val includesThisWeek = thisWeek in this
    val end = if (includesThisWeek) thisWeek.plusWeeks(1) else thisWeek
    val periods = (1..MAX_REPORT_WEEKS).map { weeks ->
        ReportPeriod(firstDay = end.minusWeeks(weeks), weeks = weeks, includesThisWeek = includesThisWeek)
    }
    return PeriodChoice(
        period = periods.firstOrNull { between(it.firstDay, end).size >= MIN_MATCHES_PER_REPORT },
        played = between(periods.last().firstDay, end).size,
    )
}

private val ReportPeriod.end: LocalDate get() = firstDay.plusWeeks(weeks)

// 비교 기준은 기간 바로 앞 4주다. 이번 액트 첫 경기가 4주 안쪽이면 거기서부터 센다. 안 그러면 2주치 경기에 "지난 4주
// 평균"이 붙는다. 홈 리포트, 짚을 점의 무기, S6이 같은 창을 써야 숫자가 갈리지 않는다.
private fun Map<LocalDate, List<Match>>.baselineStart(periodStart: LocalDate): LocalDate =
    maxOf(periodStart.minusWeeks(BASELINE_WEEKS), keys.min())

// 평소 변동폭을 재는 기간 앞 주들이다. 경기가 없는 주는 빠진다.
private fun Map<LocalDate, List<Match>>.weeksBefore(periodStart: LocalDate): List<List<Match>> =
    (1..VOLATILITY_WEEKS).mapNotNull { weeksBefore -> this[periodStart.minusWeeks(weeksBefore)] }

private fun Instant.weekStart(timeZone: TimeZone): LocalDate {
    val date = toLocalDateTime(timeZone).date
    return date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
}

/** 이 시각 다음에 오는 [timeZone] 기준 월요일 0시입니다. 그때 리포트의 "이번 주"가 바뀝니다. */
fun Instant.nextWeekStart(timeZone: TimeZone): Instant = weekStart(timeZone).plus(1, DateTimeUnit.WEEK).atStartOfDayIn(timeZone)

private fun Map<LocalDate, List<Match>>.between(from: LocalDate, until: LocalDate): List<Match> =
    filterKeys { it >= from && it < until }.values.flatten()

private fun List<Match>.totalMetrics(): MatchMetrics = map { it.metrics() }.sum()

private fun LocalDate.plusWeeks(weeks: Int) = plus(weeks, DateTimeUnit.WEEK)

private fun LocalDate.minusWeeks(weeks: Int) = minus(weeks, DateTimeUnit.WEEK)

/** S6·S7과 프로필이 보는 경기입니다. 고른 큐에서 가장 최근 경기의 액트만 남깁니다. */
fun Iterable<Match>.currentActMatches(queueFilter: QueueFilter): List<Match> {
    val counted = filter { it.queue in queueFilter.queues }
    val act = counted.maxByOrNull { it.startedAt }?.act ?: return emptyList()
    return counted.filter { it.act == act }
}

/**
 * S6 무기 화면에 쓰는 집계입니다. 목록은 이번 액트 전체를 보고, 위쪽 세 무기는 기간과 비교 기준을
 * 홈 리포트와 똑같이 잡습니다.
 */
fun Iterable<Match>.weaponReport(
    now: Instant,
    timeZone: TimeZone,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
): WeaponReport {
    val matches = currentActMatches(queueFilter)
    val weapons = matches.weaponStats()
    val byWeek = matches.groupBy { it.startedAt.weekStart(timeZone) }
    val period = byWeek.choosePeriod(now, timeZone).period

    return WeaponReport(
        matches = matches.size,
        kills = weapons.sumOf { it.kills },
        weapons = weapons,
        highlights = weapons.take(HIGHLIGHTED_WEAPONS).map { byWeek.highlight(it, period) },
        period = period,
    )
}

private fun Map<LocalDate, List<Match>>.highlight(act: WeaponStats, period: ReportPeriod?): WeaponHighlight {
    if (period == null) return WeaponHighlight(act, null, null, baselineWeeks = 0, movements = emptyMap())
    fun List<Match>.stats() = weaponStats().firstOrNull { it.weapon == act.weapon }

    // 헤드샷과 K/D·피해량은 표본이 다르다. 하나라도 모자라면 줄 전체를 이번 액트로 띄운다.
    val current = between(period.firstDay, period.end).stats()?.takeIf { it.isMeasurable && it.isCarriedMeasurable }
    val start = baselineStart(period.firstDay)
    val baseline = between(start, period.firstDay).stats()
    val weeks = weeksBefore(period.firstDay).mapNotNull { it.stats() }

    val movements = WeaponMetric.entries.associateWith { metric ->
        val now = current?.value(metric)
        val usual = baseline?.value(metric)
        if (now != null && usual != null) {
            assessMovement(now, usual, weekly = weeks.mapNotNull { it.value(metric) }).movement
        } else {
            Movement.UNKNOWN
        }
    }

    return WeaponHighlight(
        act = act,
        current = current,
        baseline = baseline,
        baselineWeeks = start.daysUntil(period.firstDay) / 7,
        movements = movements,
    )
}
