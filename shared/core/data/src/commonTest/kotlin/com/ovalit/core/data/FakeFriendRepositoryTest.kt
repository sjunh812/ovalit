package com.ovalit.core.data

import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.lastPlayedWith
import com.ovalit.core.testing.StepClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FakeFriendRepositoryTest {

    @Test
    fun `요청을 수락하면 친구가 되고 요청 목록에서 빠진다`() = runTest {
        val repository = FakeFriendRepository()
        val request = repository.requests.first().first()

        repository.accept(request.id)

        assertTrue(repository.friends.first().any { it.id == request.id })
        assertTrue(repository.requests.first().none { it.id == request.id })
    }

    @Test
    fun `거절하면 친구가 되지 않는다`() = runTest {
        val repository = FakeFriendRepository()
        val request = repository.requests.first().first()

        repository.decline(request.id)

        assertTrue(repository.friends.first().none { it.id == request.id })
        assertTrue(repository.requests.first().none { it.id == request.id })
    }

    @Test
    fun `라이벌과 친구를 끊으면 라이벌도 비운다`() = runTest {
        val repository = FakeFriendRepository()
        val rival = repository.friends.first().first().id
        repository.setRival(rival)

        repository.unfriend(rival)

        assertNull(repository.rival.first())
    }

    @Test
    fun `앱을 쓰는 사람만 골라낸다`() = runTest {
        val repository = FakeFriendRepository()
        val friend = repository.friends.first().first().id
        val stranger = PlayerId("not-using-app")

        assertEquals(setOf(friend), repository.appUsersAmong(listOf(friend, stranger)))
    }

    @Test
    fun `보낸 요청은 연동을 해제하면 지운다`() = runTest {
        val friends = FakeFriendRepository()
        val account = FakeAccountRepository(FakeMatchRepository(), friendRepository = friends)
        friends.sendRequest(PlayerId("someone"))

        account.unlink()

        assertEquals(emptySet(), friends.sentRequests.first())
    }

    // docs/screens.md: 연동을 해제하면 친구 관계도 모두 지운다
    @Test
    fun `연동을 해제하면 친구와 요청과 라이벌을 모두 지운다`() = runTest {
        val friends = FakeFriendRepository()
        val account = FakeAccountRepository(FakeMatchRepository(), friendRepository = friends)
        friends.setRival(friends.friends.first().first().id)

        account.unlink()

        assertEquals(emptyList(), friends.friends.first())
        assertEquals(emptyList(), friends.requests.first())
        assertNull(friends.rival.first())
    }

    // 친구 탭과 부르기 시트가 이 시각으로 줄을 세운다. 가짜 내 경기와 어긋나면 S5의 같이 뛴 경기와 순서가 따로 논다.
    @Test
    fun `가짜 친구와 마지막으로 같이 뛴 시각은 가짜 내 경기에서 찾는다`() = runTest {
        val clock = StepClock(Instant.parse("2026-09-24T12:00:00Z"))
        val friends = FakeFriendRepository(clock).friends.first()
        val mine = FakeMatchRepository(clock).observeMatches().first()

        friends.forEach { assertEquals(mine.lastPlayedWith(it.id), it.lastPlayedTogether, it.riotId) }
        assertTrue(friends.any { it.lastPlayedTogether != null })
    }
}
