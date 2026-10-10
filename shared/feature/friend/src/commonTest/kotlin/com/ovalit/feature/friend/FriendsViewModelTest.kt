package com.ovalit.feature.friend

import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FakePingRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.PingRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.PlayerId
import com.ovalit.core.testing.SameThread
import com.ovalit.core.testing.TestFriendRepository
import com.ovalit.core.testing.TestPingRepository
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
        val pings = TestPingRepository(fakePings())
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
        val pings = TestPingRepository(fakePings())
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
        val friends = TestFriendRepository(
            listOf(
                friend("old", lastPlayed = now - 7.days),
                friend("never", lastPlayed = null),
                friend("recent", lastPlayed = now - 1.hours),
                friend("rival", lastPlayed = null),
            ),
            rival = PlayerId("rival"),
        )
        val viewModel = viewModel(friends, TestPingRepository(fakePings()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<FriendsUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("rival", "recent", "old", "never"), state.friends.map { it.friend.id.value })
    }

    // 앱 모듈이 이때 알림을 켜 달라고 묻는다. 수락하지 못했으면 친구가 부를 일도 없다.
    @Test
    fun `친구 요청을 수락하면 다 끝난 뒤에 알린다`() = runTest {
        val friends = FakeFriendRepository()
        val viewModel = viewModel(friends, TestPingRepository(fakePings()))
        val request = friends.requests.first().first()
        var accepted = 0

        viewModel.accept(request.id, onAccepted = { accepted++ })

        assertEquals(1, accepted)
        assertTrue(friends.friends.first().any { it.id == request.id })
    }

    @Test
    fun `친구 요청을 수락하지 못하면 알리지 않는다`() = runTest {
        val failing = object : FriendRepository by FakeFriendRepository() {
            override suspend fun accept(id: PlayerId) = throw OvalitException(OvalitError.Offline)
        }
        val viewModel = viewModel(failing, TestPingRepository(fakePings()))
        var accepted = 0

        viewModel.accept(PlayerId("someone"), onAccepted = { accepted++ })

        assertEquals(0, accepted)
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

private fun friend(id: String, lastPlayed: Instant?) =
    Friend(PlayerId(id), "$id#KR1", playerCard = null, statsPublic = false, matches = emptyList(), lastPlayedTogether = lastPlayed)

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
