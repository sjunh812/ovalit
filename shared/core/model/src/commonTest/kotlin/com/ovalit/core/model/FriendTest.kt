package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
private val Act = ActId("act")
private val Junho = PlayerId("junho")

class FriendTest {

    @Test
    fun `같이 한 경기는 그 친구가 우리 팀이었던 내 경기만 센다`() {
        val matches = listOf(
            game(allies = setOf(Junho), won = true),
            game(allies = setOf(Junho), won = false),
            game(allies = setOf(Junho), won = null),
            game(allies = emptySet(), won = true),
        )

        assertEquals(SharedRecord(matches = 3, wins = 1, losses = 1), matches.sharedWith(Junho))
    }

    // 라이벌 대결은 같은 달력 구간을 봐야 둘을 나란히 놓을 수 있다
    @Test
    fun `친구 값은 내 리포트와 같은 기간으로 센다`() {
        val friend = friend(games(weeksAgo = 0, count = 2, damage = 200) + games(weeksAgo = 1, count = 3, damage = 100))

        val metrics = friend.metricsIn(myReport(), QueueFilter.COMPETITIVE_AND_UNRATED, Seoul)

        assertEquals(2, metrics?.matches)
        assertEquals(200.0, metrics?.adr)
    }

    @Test
    fun `다른 액트나 다른 큐 경기는 친구 값에 넣지 않는다`() {
        val friend = friend(
            games(weeksAgo = 0, count = 1) +
                games(weeksAgo = 0, count = 1, act = ActId("old")) +
                games(weeksAgo = 0, count = 1, queue = Queue.SPIKE_RUSH),
        )

        assertEquals(1, friend.metricsIn(myReport(), QueueFilter.COMPETITIVE_AND_UNRATED, Seoul)?.matches)
    }

    @Test
    fun `전적을 공개하지 않은 친구는 값을 내놓지 않는다`() {
        val hidden = friend(games(weeksAgo = 0, count = 5)).copy(statsPublic = false)

        assertNull(hidden.metricsIn(myReport(), QueueFilter.COMPETITIVE_AND_UNRATED, Seoul))
    }

    @Test
    fun `그 기간에 경기가 없으면 값이 없다`() {
        val friend = friend(games(weeksAgo = 2, count = 5))

        assertNull(friend.metricsIn(myReport(), QueueFilter.COMPETITIVE_AND_UNRATED, Seoul))
    }
}

private fun myReport(): WeeklyReport.Ready =
    assertIs(games(weeksAgo = 0, count = 5).weeklyReport(now = Now, timeZone = Seoul))

private fun friend(matches: List<Match>) = Friend(Junho, "준호#KR1", statsPublic = true, matches = matches)

private fun game(allies: Set<PlayerId>, won: Boolean?) =
    match(quietRound(), won = won).copy(allies = allies)

private fun games(
    weeksAgo: Int,
    count: Int,
    damage: Int = 150,
    act: ActId = Act,
    queue: Queue = Queue.COMPETITIVE,
): List<Match> {
    val date = ThisMonday.minus(weeksAgo, DateTimeUnit.WEEK).plus(1, DateTimeUnit.DAY)
    return List(count) {
        match(round(damage = damage), act = act, queue = queue, startedAt = date.atTime(21, 0).toInstant(Seoul))
    }
}
