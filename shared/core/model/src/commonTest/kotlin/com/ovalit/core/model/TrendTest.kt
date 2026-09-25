package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val ThisMonday = LocalDate(2026, 9, 21)
private val Now = LocalDateTime(2026, 9, 23, 12, 0).toInstant(Seoul)

private val CurrentAct = ActId("current")
private val PreviousAct = ActId("previous")

class TrendTest {

    @Test
    fun `막대는 기간 마지막 주에서 끝나는 8주다`() {
        val trend = ready(week(weeksAgo = 0) + week(weeksAgo = 3)).trend

        assertEquals(TREND_WEEKS, trend.size)
        assertEquals(ThisMonday.minusWeeks(7), trend.first().firstDay)
        assertEquals(ThisMonday, trend.last().firstDay)
        assertEquals(listOf(false, false, false, false, false, false, false, true), trend.map { it.inPeriod })
    }

    @Test
    fun `이번 주에 뛴 경기가 없으면 막대도 지난주에서 끝난다`() {
        val trend = ready(week(weeksAgo = 1)).trend

        assertEquals(ThisMonday.minusWeeks(1), trend.last().firstDay)
    }

    @Test
    fun `기간을 넓혔으면 넓힌 주가 모두 기간에 든다`() {
        val trend = ready(week(weeksAgo = 0, matches = 2) + week(weeksAgo = 1, matches = 3)).trend

        assertEquals(listOf(true, true), trend.takeLast(2).map { it.inPeriod })
        assertEquals(false, trend[5].inPeriod)
    }

    // 한두 판짜리 주는 막대 하나가 추이를 흔든다
    @Test
    fun `라운드가 40에 못 미치는 주는 막대를 비운다`() {
        val trend = ready(week(weeksAgo = 0) + week(weeksAgo = 1, rounds = 39)).trend

        assertNull(trend[6].metrics)
        assertNotNull(trend[7].metrics)
    }

    @Test
    fun `액트가 바뀐 주에 전환 표시를 한다`() {
        val matches = week(weeksAgo = 0) + week(weeksAgo = 2) + week(weeksAgo = 4, act = PreviousAct)

        val trend = ready(matches).trend

        assertEquals(listOf(5), trend.indices.filter { trend[it].startsNewAct })
    }

    // CLAUDE.md: 액트 경계를 넘는 평균은 만들지 않는다
    @Test
    fun `한 주에 액트가 둘이면 새 액트 경기만 센다`() {
        val matches = week(weeksAgo = 0, act = PreviousAct, damagePerRound = 100, day = 1) +
            week(weeksAgo = 0, act = CurrentAct, damagePerRound = 150, day = 3)

        val week = ready(matches).trend.last()

        assertEquals(CurrentAct, week.act)
        assertEquals(150.0, week.metrics?.adr)
    }

    @Test
    fun `평소 범위는 기간 앞 주들의 가장 낮은 값과 높은 값이다`() {
        val matches = week(weeksAgo = 0, damagePerRound = 200) +
            listOf(140, 165, 150, 155).mapIndexed { index, damage ->
                week(weeksAgo = index + 1, damagePerRound = damage)
            }.flatten()

        val range = ready(matches).usualRange { it.adr }

        assertEquals(UsualRange(min = 140.0, max = 165.0, weeks = 4), range)
    }

    @Test
    fun `평소 범위의 주 수는 경기가 없던 주까지 센다`() {
        val matches = week(weeksAgo = 0) + listOf(1, 2, 3, 6).flatMap { week(weeksAgo = it) }

        assertEquals(6, ready(matches).usualRange { it.adr }?.weeks)
    }

    @Test
    fun `앞선 주가 4주에 못 미치면 평소 범위를 말하지 않는다`() {
        val matches = week(weeksAgo = 0) + (1..3).flatMap { week(weeksAgo = it) }

        assertNull(ready(matches).usualRange { it.adr })
    }

    @Test
    fun `지난 액트 주는 평소 범위에 넣지 않는다`() {
        val matches = week(weeksAgo = 0) +
            (1..3).flatMap { week(weeksAgo = it) } +
            week(weeksAgo = 4, act = PreviousAct, damagePerRound = 300)

        assertNull(ready(matches).usualRange { it.adr })
    }
}

/** [rounds]라운드를 [matches]경기로 나눠 담는다. 라운드마다 [damagePerRound]만큼 피해를 준다. */
private fun week(
    weeksAgo: Int,
    rounds: Int = 40,
    matches: Int = 5,
    act: ActId = CurrentAct,
    damagePerRound: Int = 150,
    day: Int = 1,
): List<Match> {
    val date = ThisMonday.minus(weeksAgo, DateTimeUnit.WEEK).plus(day, DateTimeUnit.DAY)
    return List(rounds) { round(damage = damagePerRound) }
        .chunked((rounds + matches - 1) / matches)
        .map { match(*it.toTypedArray(), act = act, startedAt = date.atTime(21, 0).toInstant(Seoul)) }
}

private fun ready(matches: List<Match>): WeeklyReport.Ready =
    assertIs(matches.weeklyReport(now = Now, timeZone = Seoul))

private fun LocalDate.minusWeeks(weeks: Int) = minus(weeks, DateTimeUnit.WEEK)
