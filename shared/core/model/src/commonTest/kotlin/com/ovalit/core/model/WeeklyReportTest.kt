package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val ThisMonday = LocalDate(2026, 9, 21)
private val Now = LocalDateTime(2026, 9, 23, 12, 0).toInstant(Seoul)

private val CurrentAct = ActId("current")
private val PreviousAct = ActId("previous")

class WeeklyReportTest {

    @Test
    fun `이번 주에 5경기를 채웠으면 이번 주만 집계한다`() {
        val report = ready(games(weeksAgo = 0, count = 5) + games(weeksAgo = 1, count = 3))

        assertEquals(ReportPeriod(firstDay = ThisMonday, weeks = 1), report.period)
        assertEquals(5, report.metrics.matches)
    }

    @Test
    fun `이번 주가 5경기에 못 미치면 지난주까지 넓힌다`() {
        val report = ready(games(weeksAgo = 0, count = 2) + games(weeksAgo = 1, count = 3))

        assertEquals(2, report.period.weeks)
        assertEquals(5, report.metrics.matches)
    }

    @Test
    fun `5경기를 채울 때까지 최대 4주까지 넓힌다`() {
        val report = ready(games(0, 1) + games(1, 1) + games(2, 1) + games(3, 2))

        assertEquals(ReportPeriod(firstDay = LocalDate(2026, 8, 31), weeks = 4), report.period)
    }

    @Test
    fun `4주로도 5경기를 못 채우면 리포트 대신 뛴 경기 수를 알려준다`() {
        val result = report(games(0, 1) + games(1, 1) + games(2, 1) + games(3, 1) + games(4, 3))

        assertEquals(WeeklyReport.NotEnoughMatches(played = 4), result)
    }

    @Test
    fun `4주 넘게 쉬었으면 예전 경기로 리포트를 만들지 않는다`() {
        val result = report(games(weeksAgo = 5, count = 10))

        assertEquals(WeeklyReport.NotEnoughMatches(played = 0), result)
    }

    @Test
    fun `경기가 하나도 없으면 리포트를 만들지 않는다`() {
        assertEquals(WeeklyReport.NotEnoughMatches(played = 0), report(emptyList()))
    }

    @Test
    fun `주는 월요일 0시에 바뀐다`() {
        val sundayNight = List(5) { gameAt(LocalDateTime(2026, 9, 20, 23, 59)) }
        val mondayMidnight = List(5) { gameAt(LocalDateTime(2026, 9, 21, 0, 0)) }

        assertEquals(2, ready(sundayNight).period.weeks)
        assertEquals(1, ready(mondayMidnight).period.weeks)
    }

    // 서울의 월요일 0시 30분은 UTC로 아직 일요일이다
    @Test
    fun `주 경계는 넘겨받은 시간대를 따른다`() {
        val matches = List(5) { gameAt(LocalDateTime(2026, 9, 21, 0, 30)) }

        assertEquals(1, ready(matches, timeZone = Seoul).period.weeks)
        assertEquals(2, ready(matches, timeZone = TimeZone.UTC).period.weeks)
    }

    @Test
    fun `경쟁전과 일반전만 센다`() {
        val matches = games(0, 4, queue = Queue.COMPETITIVE) +
            games(0, 1, queue = Queue.UNRATED) +
            games(0, 2, queue = Queue.SPIKE_RUSH) +
            games(0, 1, queue = Queue.SWIFTPLAY) +
            games(0, 1, queue = Queue.OTHER)

        val report = ready(matches)

        assertEquals(1, report.period.weeks)
        assertEquals(5, report.metrics.matches)
    }

    @Test
    fun `비교 기준은 집계 기간 바로 앞 4주다`() {
        val matches = games(0, 5) +
            games(1, 2) + games(2, 2) + games(3, 2) + games(4, 2) +
            games(5, 3)

        val baseline = assertNotNull(ready(matches).baseline)

        assertEquals(4, baseline.weeks)
        assertEquals(8, baseline.metrics.matches)
    }

    @Test
    fun `기간을 넓히면 비교 기준도 그만큼 뒤로 밀린다`() {
        val matches = games(0, 2) + games(1, 3) +
            games(2, 2) + games(3, 2) + games(4, 2) + games(5, 2) +
            games(6, 3)

        val report = ready(matches)
        val baseline = assertNotNull(report.baseline)

        assertEquals(2, report.period.weeks)
        assertEquals(4, baseline.weeks)
        assertEquals(8, baseline.metrics.matches)
    }

    @Test
    fun `비교 기준이 5경기에 못 미치면 비교하지 않는다`() {
        val report = ready(games(0, 5) + games(1, 2) + games(3, 2))

        assertNull(report.baseline)
    }

    // 이번 주 월요일까지 이전 액트였고 화요일에 새 액트가 열렸다
    @Test
    fun `이전 액트 경기는 집계 기간에 넣지 않는다`() {
        val matches = games(0, 3, act = PreviousAct, day = DayOfWeek.MONDAY) +
            games(0, 3, act = CurrentAct, day = DayOfWeek.TUESDAY) +
            games(1, 5, act = PreviousAct)

        assertEquals(WeeklyReport.NotEnoughMatches(played = 3), report(matches))
    }

    @Test
    fun `비교 기준도 이번 액트가 시작된 주부터 센다`() {
        val matches = games(0, 5) +
            games(1, 5) +
            games(2, 4, act = PreviousAct) + games(3, 4, act = PreviousAct)

        val report = ready(matches)
        val baseline = assertNotNull(report.baseline)

        assertEquals(CurrentAct, report.act)
        assertEquals(1, baseline.weeks)
        assertEquals(5, baseline.metrics.matches)
    }

    @Test
    fun `모아 둔 경기가 4주보다 짧으면 비교 기준 기간도 그만큼 짧다`() {
        val matches = games(0, 5) + games(1, 3) + games(2, 3)

        val baseline = assertNotNull(ready(matches).baseline)

        assertEquals(2, baseline.weeks)
        assertEquals(6, baseline.metrics.matches)
    }

    @Test
    fun `주로 뛴 역할은 경기 수가 아니라 라운드 수로 정한다`() {
        val tuesday = LocalDateTime(2026, 9, 22, 21, 0)
        val longDuelist = List(2) { gameAt(tuesday, role = Role.DUELIST, rounds = List(3) { quietRound() }) }
        val shortController = List(3) { gameAt(tuesday, role = Role.CONTROLLER) }

        assertEquals(Role.DUELIST, ready(longDuelist + shortController).mainRole)
    }

    @Test
    fun `역할을 모르는 경기는 역할을 정할 때 뺀다`() {
        val tuesday = LocalDateTime(2026, 9, 22, 21, 0)
        val newAgent = List(3) { gameAt(tuesday, role = null, rounds = List(5) { quietRound() }) }
        val sentinel = List(2) { gameAt(tuesday, role = Role.SENTINEL) }

        assertEquals(Role.SENTINEL, ready(newAgent + sentinel).mainRole)
    }

    // 역할을 모르면 빈칸이 기본 3개(관여율, 생존율, 퍼블 승률)로 채워진다
    @Test
    fun `동적 칸은 주로 뛴 역할에 맞춰 고른다`() {
        val report = ready(List(5) { gameAt(LocalDateTime(2026, 9, 22, 21, 0), role = Role.CONTROLLER) })

        assertEquals(
            listOf(DynamicMetric.KAST, DynamicMetric.SURVIVAL_RATE, DynamicMetric.ASSISTS_PER_ROUND),
            report.dynamic.map { it.metric },
        )
    }

    // 8주 동안 관여율이 0.675와 0.725를 오갔고, 9주 전에 한 번 크게 무너졌다.
    // 9주 전까지 넣으면 변동폭이 커져서 이번 주 0.10 상승이 묻힌다.
    @Test
    fun `평소 변동폭은 집계 기간 앞 8주로 잰다`() {
        val usual = (1..8).flatMap { kastWeek(weeksAgo = it, kastRounds = if (it % 2 == 0) 27 else 29) }
        val matches = kastWeek(weeksAgo = 0, kastRounds = 32) + usual + kastWeek(weeksAgo = 9, kastRounds = 0)

        val kast = ready(matches).dynamic.first { it.metric == DynamicMetric.KAST }

        assertEquals(Movement.MOVED, kast.movement)
    }

    // 이번 주 0.747까지 변동폭에 넣으면 기준선이 0.0433에서 0.0490으로 올라가 0.047 상승을 놓친다
    @Test
    fun `평소 변동폭에 이번 기간은 넣지 않는다`() {
        val usual = (1..4).flatMap { kastWeek(weeksAgo = it, kastRounds = if (it % 2 == 0) 27 else 29) }
        val matches = kastWeek(weeksAgo = 0, kastRounds = 747, rounds = 1000) + usual

        val kast = ready(matches).dynamic.first { it.metric == DynamicMetric.KAST }

        assertEquals(Movement.MOVED, kast.movement)
    }
}

private fun games(
    weeksAgo: Int,
    count: Int,
    act: ActId = CurrentAct,
    queue: Queue = Queue.COMPETITIVE,
    day: DayOfWeek = DayOfWeek.TUESDAY,
): List<Match> {
    val date = ThisMonday
        .minus(weeksAgo, DateTimeUnit.WEEK)
        .plus(day.isoDayNumber - 1, DateTimeUnit.DAY)
    return List(count) { gameAt(date.atTime(21, 0), act, queue) }
}

private fun gameAt(
    time: LocalDateTime,
    act: ActId = CurrentAct,
    queue: Queue = Queue.COMPETITIVE,
    role: Role? = null,
    rounds: List<Round> = listOf(quietRound()),
) = match(*rounds.toTypedArray(), queue = queue, act = act, startedAt = time.toInstant(Seoul), role = role)

/** [rounds]라운드를 5경기로 나눠 담는다. 앞의 [kastRounds]라운드는 살아남고 나머지는 죽는다. */
private fun kastWeek(weeksAgo: Int, kastRounds: Int, rounds: Int = 40): List<Match> {
    val date = ThisMonday.minus(weeksAgo, DateTimeUnit.WEEK).plus(1, DateTimeUnit.DAY)
    return List(rounds) { if (it < kastRounds) quietRound() else round(kill(10.0, Enemy, Me)) }
        .chunked(rounds / 5)
        .map { gameAt(date.atTime(21, 0), rounds = it) }
}

private fun report(matches: List<Match>, timeZone: TimeZone = Seoul) =
    matches.weeklyReport(now = Now, timeZone = timeZone)

private fun ready(matches: List<Match>, timeZone: TimeZone = Seoul): WeeklyReport.Ready =
    assertIs(report(matches, timeZone))
