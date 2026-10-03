package com.ovalit.feature.friend

import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FakePingRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.PingRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `당기면 친구와 ㅇㅂㅇ을 같이 다시 받고 다 받으면 표시를 끈다`() = runTest {
        val friends = GatedFriends(FakeFriendRepository())
        val pings = CountingPings(fakePings())
        val viewModel = viewModel(friends, pings)

        viewModel.refresh()

        assertTrue(viewModel.isRefreshing.value)
        assertEquals(1, friends.refreshed)
        assertEquals(1, pings.refreshed)

        friends.gate.complete(Unit)

        assertFalse(viewModel.isRefreshing.value)
    }

    // 레이트 리밋이 앱 전체에 걸려 있다(CLAUDE.md 아키텍처)
    @Test
    fun `받는 중에 또 당기면 다시 받지 않는다`() = runTest {
        val friends = GatedFriends(FakeFriendRepository())
        val pings = CountingPings(fakePings())
        val viewModel = viewModel(friends, pings)

        viewModel.refresh()
        viewModel.refresh()

        assertEquals(1, friends.refreshed)
        assertEquals(1, pings.refreshed)
        friends.gate.complete(Unit)
    }

    private fun TestScope.fakePings() = FakePingRepository(FakeFriendRepository(), scope = backgroundScope)

    private fun viewModel(friends: FriendRepository, pings: PingRepository) = FriendsViewModel(
        friendRepository = friends,
        pingRepository = pings,
        accountRepository = FakeAccountRepository(FakeMatchRepository()),
        clock = Clock.System,
        timeZone = TimeZone.of("Asia/Seoul"),
    )
}

// 친구 서버가 답할 때까지 붙잡아 둔다
private class GatedFriends(private val delegate: FakeFriendRepository) : FriendRepository by delegate {
    val gate = CompletableDeferred<Unit>()
    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
        gate.await()
    }
}

private class CountingPings(private val delegate: PingRepository) : PingRepository by delegate {
    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
    }
}
