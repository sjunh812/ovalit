package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Sent = LocalDateTime(2026, 10, 3, 20, 10).toInstant(Seoul)
private val Nine = LocalDateTime(2026, 10, 3, 21, 0).toInstant(Seoul)
private val Ten = LocalDateTime(2026, 10, 3, 22, 0).toInstant(Seoul)

private val Host = PingPerson(PlayerId("host"), "민석#KR3")
private val Junho = PingPerson(PlayerId("junho"), "준호#KR1")
private val Jaehyun = PingPerson(PlayerId("jaehyun"), "재현#KR2")
private val Seoyeon = PingPerson(PlayerId("seoyeon"), "서연#KR7")

class PingTest {

    @Test
    fun `하자고 한 시각에서 한 시간 지나면 끝난다`() {
        val ping = ping()

        assertTrue(ping.isActive(Nine + 59.minutes))
        assertFalse(ping.isActive(Nine + 1.hours))
    }

    @Test
    fun `보낸 시각에 바로 하자고 하면 지금이다`() {
        assertTrue(ping(startsAt = Sent + 2.minutes).isNow)
        assertFalse(ping(startsAt = Sent + 30.minutes).isNow)
    }

    @Test
    fun `다른 시간이 아닌 답에는 낸 시각을 남기지 않는다`() {
        val ping = ping().answered(Junho.id, PingAnswer.YES, proposedAt = Ten)

        assertEquals(PingAnswer.YES, ping.memberOf(Junho.id)?.answer)
        assertNull(ping.memberOf(Junho.id)?.proposedAt)
    }

    // 9시에 간다던 친구가 10시에도 되는지는 모르고, 안 된다던 친구가 10시에는 될 수도 있다
    @Test
    fun `시각을 옮기면 그 시각을 낸 친구만 참석으로 두고 나머지는 다시 묻는다`() {
        val ping = ping()
            .answered(Junho.id, PingAnswer.OTHER_TIME, proposedAt = Ten)
            .answered(Jaehyun.id, PingAnswer.YES)
            .answered(Seoyeon.id, PingAnswer.NO)

        val moved = ping.movedTo(Ten)

        assertEquals(Ten, moved.startsAt)
        assertEquals(
            listOf(PingAnswer.YES, PingAnswer.PENDING, PingAnswer.PENDING),
            moved.members.map { it.answer },
        )
        assertTrue(moved.members.all { it.proposedAt == null })
    }

    @Test
    fun `갈게만 센다`() {
        val ping = ping().answered(Junho.id, PingAnswer.YES).answered(Jaehyun.id, PingAnswer.OTHER_TIME, Ten)

        assertEquals(1, ping.yesCount)
    }

    @Test
    fun `홈에는 답하지 않은 받은 것을 먼저 띄우고 그다음 내가 보낸 것이다`() {
        val me = Junho.id
        val answered = ping().answered(me, PingAnswer.YES).copy(id = PingId("answered"))
        val pending = ping().copy(id = PingId("pending"))
        val mine = ping().copy(id = PingId("mine"), host = Junho, members = listOf(PingMember(Jaehyun, PingAnswer.PENDING)))

        assertEquals(PingId("pending"), listOf(answered, mine, pending).forHome(me)?.id)
        assertEquals(PingId("mine"), listOf(answered, mine).forHome(me)?.id)
        assertEquals(PingId("answered"), listOf(answered).forHome(me)?.id)
        assertNull(emptyList<Ping>().forHome(me))
    }

    @Test
    fun `시간 칸은 다음 30분부터 30분마다 다음 날 같은 시각 전까지다`() {
        val slots = pingSlots(LocalDateTime(2026, 10, 3, 13, 12, 40).toInstant(Seoul), Seoul)

        assertEquals(LocalDateTime(2026, 10, 3, 13, 30).toInstant(Seoul), slots.first())
        // 서버는 24시간 앞까지 받는다
        assertEquals(LocalDateTime(2026, 10, 4, 12, 30).toInstant(Seoul), slots.last())
        assertEquals(47, slots.size)
    }

    // 누가 못 간다고 하면 그 자리에 다른 친구를 부를 수 있다
    @Test
    fun `못 간다고 한 친구는 자리를 비운 것으로 친다`() {
        // 셋을 불렀으니 하나를 더 부르면 넷이라 자리가 없다
        val full = ping().invited(listOf(PingPerson(PlayerId("c"), "c#KR1")))

        assertEquals(0, full.openSeats)
        assertEquals(1, full.answered(PlayerId("c"), PingAnswer.NO).openSeats)
    }

    @Test
    fun `이미 부른 친구는 다시 붙이지 않는다`() {
        val again = ping().invited(listOf(ping().members.first().person))

        assertEquals(ping().members, again.members)
    }

    @Test
    fun `언제 불러도 같은 칸은 같은 시각이다`() {
        val base = LocalDateTime(2026, 10, 3, 13, 12, 40).toInstant(Seoul)
        val later = base + 3.minutes + 123_456.nanoseconds

        assertEquals(pingSlots(base, Seoul), pingSlots(later, Seoul))
        assertEquals(0, pingSlots(later, Seoul).first().nanosecondsOfSecond)
    }

    @Test
    fun `딱 30분이면 다음 칸부터다`() {
        val slots = pingSlots(LocalDateTime(2026, 10, 3, 13, 30).toInstant(Seoul), Seoul)

        assertEquals(LocalDateTime(2026, 10, 3, 14, 0).toInstant(Seoul), slots.first())
    }
}

private fun ping(startsAt: kotlin.time.Instant = Nine) = Ping(
    id = PingId("ping"),
    host = Host,
    startsAt = startsAt,
    createdAt = Sent,
    members = listOf(Junho, Jaehyun, Seoyeon).map { PingMember(it, PingAnswer.PENDING) },
)
