package com.ovalit.feature.profile

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.currentActMatches
import com.ovalit.core.testing.SameThread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModelTest {

    private val myMatches = FakeMatchRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `내 기록은 내 경기로 세고 제목에 이름을 붙이지 않는다`() = runTest {
        val viewModel = viewModel(RecordsOwner.Me, EditableFriends())
        collect(viewModel)

        val state = assertIs<RecordsUiState.Success>(viewModel.uiState.value)
        val mine = myMatches.observeMatches().first().currentActMatches(QueueFilter.COMPETITIVE_AND_UNRATED)
        assertNull(state.ownerName)
        assertEquals(mine.size, state.agents.matches)
        assertEquals(mine.size, state.weapons.matches)
    }

    // 친구 경기는 기기에 저장된 것을 그대로 쓴다. 내 경기와 섞이면 친구 화면에 내 숫자가 뜬다.
    @Test
    fun `친구 기록은 그 친구의 경기로 세고 제목에 이름을 붙인다`() = runTest {
        val friends = EditableFriends()
        val friend = friends.public()
        val viewModel = viewModel(RecordsOwner.Friend(friend.id), friends)
        collect(viewModel)

        val state = assertIs<RecordsUiState.Success>(viewModel.uiState.value)
        val theirs = friend.matches.currentActMatches(QueueFilter.COMPETITIVE_AND_UNRATED)
        assertEquals(friend.riotId.substringBefore('#'), state.ownerName)
        assertTrue(theirs.isNotEmpty())
        assertEquals(theirs.size, state.agents.matches)
        assertEquals(theirs.size, state.weapons.matches)
    }

    // 보는 중에 친구가 전적을 비공개로 바꾸거나 친구가 끊기면 숫자를 거둔다
    @Test
    fun `친구가 전적을 비공개로 바꾸거나 친구가 끊기면 기록을 감춘다`() = runTest {
        val friends = EditableFriends()
        val friend = friends.public()
        val viewModel = viewModel(RecordsOwner.Friend(friend.id), friends)
        collect(viewModel)
        assertIs<RecordsUiState.Success>(viewModel.uiState.value)

        friends.replace(friend.copy(statsPublic = false))
        assertEquals(RecordsUiState.Hidden, viewModel.uiState.value)

        friends.replace(friend)
        assertIs<RecordsUiState.Success>(viewModel.uiState.value)
        friends.remove(friend)
        assertEquals(RecordsUiState.Hidden, viewModel.uiState.value)
    }

    // 메인 스레드에서 세면 화면이 밀려 들어오는 동안 멈춘다
    @Test
    fun `기록은 넘겨받은 곳에서 센다`() = runTest {
        val viewModel = RecordsViewModel(
            RecordsOwner.Me,
            myMatches,
            FakeFriendRepository(),
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
            computation = StandardTestDispatcher(testScheduler),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(RecordsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertIs<RecordsUiState.Success>(viewModel.uiState.value)
    }

    private fun viewModel(owner: RecordsOwner, friends: FriendRepository) =
        RecordsViewModel(owner, myMatches, friends, FakeContentRepository(), Clock.System, TimeZone.of("Asia/Seoul"), computation = SameThread)

    private fun TestScope.collect(viewModel: RecordsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }
}

// 가짜 저장소의 친구 목록을 테스트에서 바꿀 수 있게 감쌌다
private class EditableFriends(
    private val delegate: FakeFriendRepository = FakeFriendRepository(),
) : FriendRepository by delegate {
    private val list = MutableStateFlow<List<Friend>>(emptyList())

    override val friends: Flow<List<Friend>> = list

    suspend fun public(): Friend {
        list.value = delegate.friends.first()
        return list.value.first { it.statsPublic && it.matches.isNotEmpty() }
    }

    fun replace(friend: Friend) {
        list.value = list.value.filterNot { it.id == friend.id } + friend
    }

    fun remove(friend: Friend) {
        list.value = list.value.filterNot { it.id == friend.id }
    }
}
