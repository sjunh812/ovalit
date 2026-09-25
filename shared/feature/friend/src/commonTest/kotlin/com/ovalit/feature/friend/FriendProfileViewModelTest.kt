package com.ovalit.feature.friend

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.model.PlayerId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
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

    // 끊기 전에 화면을 닫으면 방금 끊은 친구가 목록에 잠깐 남는다
    @Test
    fun `친구를 끊으면 다 끊은 뒤에 알리고 화면은 사라진 상태가 된다`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)
        var unfriendedWhenNotified: Boolean? = null

        viewModel.unfriend { unfriendedWhenNotified = friends.unfriended }

        assertEquals(true, unfriendedWhenNotified)
        assertEquals(FriendProfileUiState.Gone, viewModel.uiState.value)
    }

    private suspend fun viewModel(): FriendProfileViewModel {
        val id = friends.friends.first().first().id
        return FriendProfileViewModel(id, friends, FakeMatchRepository(), FakeContentRepository(), Clock.System, TimeZone.of("Asia/Seoul"))
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
