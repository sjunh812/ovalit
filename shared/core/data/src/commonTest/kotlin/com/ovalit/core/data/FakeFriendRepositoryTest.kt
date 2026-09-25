package com.ovalit.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    // 연동을 해제하면 Riot 계정과 이어진 관계도 남기지 않는다
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
}
