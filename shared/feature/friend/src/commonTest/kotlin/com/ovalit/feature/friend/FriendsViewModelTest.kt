package com.ovalit.feature.friend

import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FakePingRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.PingRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.PlayerId
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

    // 친구가 수백 명이면 다시 같이 할 사람을 찾기 어렵다. 라이벌은 가장 자주 보는 친구라 맨 앞이다.
    @Test
    fun `라이벌을 맨 앞에 두고 나머지는 최근에 같이 뛴 친구부터 세운다`() = runTest {
        val now = Clock.System.now()
        val friends = ListedFriends(
            listOf(
                friend("old", lastPlayed = now - 7.days),
                friend("never", lastPlayed = null),
                friend("recent", lastPlayed = now - 1.hours),
                friend("rival", lastPlayed = null),
            ),
            rival = PlayerId("rival"),
        )
        val viewModel = viewModel(friends, CountingPings(fakePings()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<FriendsUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("rival", "recent", "old", "never"), state.friends.map { it.friend.id.value })
    }

    private fun TestScope.fakePings() = FakePingRepository(FakeFriendRepository(), scope = backgroundScope)

    private fun viewModel(friends: FriendRepository, pings: PingRepository) = FriendsViewModel(
        friendRepository = friends,
        pingRepository = pings,
        accountRepository = FakeAccountRepository(FakeMatchRepository()),
        clock = Clock.System,
        timeZone = TimeZone.of("Asia/Seoul"),
        computation = SameThread,
    )
}

// 앱은 Dispatchers.Default에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서 센다
private val SameThread = EmptyCoroutineContext

private fun friend(id: String, lastPlayed: Instant?) =
    Friend(PlayerId(id), "$id#KR1", playerCard = null, statsPublic = false, matches = emptyList(), lastPlayedTogether = lastPlayed)

private class ListedFriends(list: List<Friend>, rival: PlayerId?) : FriendRepository by FakeFriendRepository() {
    override val friends: Flow<List<Friend>> = flowOf(list)
    override val rival: Flow<PlayerId?> = flowOf(rival)
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
