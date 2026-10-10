package com.ovalit.feature.friend

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.PingInviteResult
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.PingSendResult
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

// 앱은 Dispatchers.Default에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서 센다
private val SameThread = EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class FriendProfileViewModelTest {

    private val friends = RecordingFriends(FakeFriendRepository())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `라이벌 버튼은 누를 때마다 지정과 해제를 오간다`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        viewModel.toggleRival()
        assertTrue(assertIs<FriendProfileUiState.Success>(viewModel.uiState.value).isRival)

        viewModel.toggleRival()
        assertEquals(false, assertIs<FriendProfileUiState.Success>(viewModel.uiState.value).isRival)
    }

    // 화면은 Gone을 보고 한 번만 닫힌다
    @Test
    fun `친구를 끊으면 끊긴 뒤에 화면이 사라진 상태가 된다`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        viewModel.unfriend()

        assertTrue(friends.unfriended)
        assertEquals(FriendProfileUiState.Gone, viewModel.uiState.value)
    }

    // 서버는 끊을 때 둘 사이의 ㅇㅂㅇ에서 서로를 뺀다. 다시 받지 않으면 끊은 친구의 초대가 홈에 남아 답할 수도 있다.
    @Test
    fun `친구를 끊으면 ㅇㅂㅇ을 다시 받는다`() = runTest {
        val pings = RefreshCountingPings()
        val viewModel = viewModel(pings)
        collect(viewModel)

        viewModel.unfriend()
        advanceUntilIdle()

        assertEquals(1, pings.refreshed)
    }

    // 비공개로 바꾸기 전에 받아 둔 경기가 기기에 남아 있어도 보여주지 않는다
    @Test
    fun `전적을 비공개로 바꾼 친구는 남아 있는 경기와 티어를 가린다`() = runTest {
        val source = FakeFriendRepository()
        val public = source.friends.first().first { it.statsPublic && it.matches.isNotEmpty() }
        val private = PrivateFriends(source, public.id)
        val viewModel = FriendProfileViewModel(public.id, private, RefreshCountingPings(), FakeMatchRepository(), FakeContentRepository(), Clock.System, TimeZone.of("Asia/Seoul"), computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<FriendProfileUiState.Success>(viewModel.uiState.value)
        assertTrue(state.friend.matches.isEmpty())
        assertEquals(null, state.badge.tier)
        assertEquals(null, state.theirProfile)
    }

    // 메인 스레드에서 세면 화면이 밀려 들어오는 동안 멈춘다
    @Test
    fun `친구 프로필은 넘겨받은 곳에서 센다`() = runTest {
        val id = friends.friends.first().first().id
        val viewModel = FriendProfileViewModel(
            id,
            friends,
            RefreshCountingPings(),
            FakeMatchRepository(),
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
            computation = StandardTestDispatcher(testScheduler),
        )
        collect(viewModel)

        assertEquals(FriendProfileUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertIs<FriendProfileUiState.Success>(viewModel.uiState.value)
    }

    private suspend fun viewModel(pings: PingRepository = RefreshCountingPings()): FriendProfileViewModel {
        val id = friends.friends.first().first().id
        return FriendProfileViewModel(id, friends, pings, FakeMatchRepository(), FakeContentRepository(), Clock.System, TimeZone.of("Asia/Seoul"), computation = SameThread)
    }

    private fun TestScope.collect(viewModel: FriendProfileViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }
}

private class RecordingFriends(private val delegate: FakeFriendRepository) : FriendRepository by delegate {
    var unfriended = false
        private set

    override suspend fun unfriend(id: PlayerId) {
        delegate.unfriend(id)
        unfriended = true
    }
}

// 경기는 그대로 둔 채 [id] 친구만 비공개로 바꾼다
private class PrivateFriends(private val delegate: FakeFriendRepository, private val id: PlayerId) : FriendRepository by delegate {
    override val friends = delegate.friends.map { list -> list.map { if (it.id == id) it.copy(statsPublic = false) else it } }
}

private class RefreshCountingPings : PingRepository {
    var refreshed = 0
        private set

    override val pings: Flow<List<Ping>> = flowOf(emptyList())

    override suspend fun send(friends: List<PlayerId>, startsAt: Instant) = PingSendResult.SENT

    override suspend fun reply(id: PingId, answer: PingAnswer, proposedAt: Instant?) = Unit

    override suspend fun moveTo(id: PingId, startsAt: Instant) = Unit

    override suspend fun invite(id: PingId, friends: List<PlayerId>) = PingInviteResult.INVITED

    override suspend fun cancel(id: PingId) = Unit

    override suspend fun refresh() {
        refreshed++
    }
}
