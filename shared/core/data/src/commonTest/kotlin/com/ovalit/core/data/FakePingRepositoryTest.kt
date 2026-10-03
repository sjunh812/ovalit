package com.ovalit.core.data

import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingLength
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Evening = LocalDateTime(2026, 10, 3, 20, 10).toInstant(Seoul)

class FakePingRepositoryTest {

    @Test
    fun `처음에는 민석이 보낸 ㅇㅂㅇ이 와 있다`() = runTest {
        val repository = repository()

        val ping = repository.pings.first().single()

        assertEquals("민석#KR3", ping.host.riotId)
        assertEquals(PingAnswer.PENDING, ping.memberOf(Me)?.answer)
    }

    @Test
    fun `보내면 가짜 친구들이 차례로 답한다`() = runTest {
        val repository = repository()
        val friends = FakeFriendIds
        val startsAt = Evening + 50.minutes

        assertEquals(PingSendResult.SENT, repository.send(friends, startsAt))
        advanceTimeBy(3.seconds)
        runCurrent()
        assertEquals(listOf(PingAnswer.YES, PingAnswer.PENDING, PingAnswer.PENDING), repository.mine().members.map { it.answer })
        advanceTimeBy(3.seconds)
        runCurrent()

        val second = repository.mine().members[1]
        assertEquals(PingAnswer.OTHER_TIME, second.answer)
        // 21:00에 하자고 하면 다음 칸인 21:30을 낸다
        assertEquals(startsAt + 30.minutes, second.proposedAt)
    }

    @Test
    fun `보낸 ㅇㅂㅇ이 끝나기 전에는 하나 더 보낼 수 없다`() = runTest {
        val repository = repository()
        repository.send(FakeFriendIds.take(1), Evening)

        assertEquals(PingSendResult.ALREADY_ACTIVE, repository.send(FakeFriendIds.take(1), Evening + 1.hours))
    }

    @Test
    fun `받은 ㅇㅂㅇ에 답하면 내 답이 바뀐다`() = runTest {
        val repository = repository()
        val ping = repository.pings.first().single()

        repository.reply(ping.id, PingAnswer.OTHER_TIME, proposedAt = ping.startsAt + 1.hours)

        val mine = repository.pings.first().single().memberOf(Me)
        assertEquals(PingAnswer.OTHER_TIME, mine?.answer)
        assertEquals(ping.startsAt + 1.hours, mine?.proposedAt)
    }

    @Test
    fun `시각을 옮기면 모두에게 다시 묻고 한 친구가 새 시각에 간다고 한다`() = runTest {
        val repository = repository()
        val startsAt = Evening + 50.minutes
        repository.send(FakeFriendIds, startsAt)
        advanceTimeBy(9.seconds)
        runCurrent()

        repository.moveTo(repository.mine().id, startsAt + 30.minutes)
        // 30분 뒤를 냈던 둘째만 참석으로 남는다
        assertEquals(listOf(PingAnswer.PENDING, PingAnswer.YES, PingAnswer.PENDING), repository.mine().members.map { it.answer })
        advanceTimeBy(3.seconds)
        runCurrent()

        assertEquals(PingAnswer.YES, repository.mine().members.first().answer)
    }

    @Test
    fun `친구를 더 부르면 뒤에 붙고 잠시 뒤 간다고 한다`() = runTest {
        val repository = repository()
        repository.send(FakeFriendIds.take(1), Evening + 50.minutes)
        val id = repository.mine().id

        assertEquals(PingInviteResult.INVITED, repository.invite(id, FakeFriendIds.drop(1).take(1)))
        assertEquals(FakeFriendIds.take(2), repository.mine().members.map { it.person.id })
        assertEquals(PingAnswer.PENDING, repository.mine().members[1].answer)
        advanceTimeBy(9.seconds)
        runCurrent()

        assertEquals(PingAnswer.YES, repository.mine().members[1].answer)
    }

    @Test
    fun `자리가 모자라면 더 부르지 않는다`() = runTest {
        val repository = repository()
        repository.send(FakeFriendIds.take(3), Evening + 50.minutes)

        // 셋을 불러 한 자리가 남았는데 둘을 더 부른다
        assertEquals(PingInviteResult.FULL, repository.invite(repository.mine().id, listOf(PlayerId("a"), PlayerId("b"))))
        assertEquals(3, repository.mine().members.size)
    }

    @Test
    fun `취소하면 목록에서 빠진다`() = runTest {
        val repository = repository()
        repository.send(FakeFriendIds.take(1), Evening)

        repository.cancel(repository.mine().id)

        assertTrue(repository.pings.first().none { it.isHostedBy(Me) })
    }

    @Test
    fun `하자고 한 시각에서 한 시간이 지나면 내려주지 않는다`() = runTest {
        val clock = MovableClock(Evening)
        val repository = repository(clock)
        val ping = repository.pings.first().single()

        clock.now = ping.startsAt + PingLength

        assertTrue(repository.pings.first().isEmpty())
    }
}

private class MovableClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

private fun TestScope.repository(clock: Clock = MovableClock(Evening)) = FakePingRepository(
    friendRepository = FakeFriendRepository(clock),
    clock = clock,
    timeZone = Seoul,
    scope = backgroundScope,
)

private suspend fun FakePingRepository.mine() = pings.first().first { it.isHostedBy(Me) }
