package com.ovalit.feature.profile

import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeMatchRepository
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

// 앱은 Dispatchers.Default에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서 센다
private val SameThread = EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `요원과 무기 집계에 콘텐츠 카탈로그 이름표를 붙여 내놓는다`() = runTest {
        val matches = FakeMatchRepository()
        val viewModel = ProfileViewModel(
            FakeAccountRepository(matches),
            matches,
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
            computation = SameThread,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ProfileUiState.Success>(viewModel.uiState.value)
        assertTrue(state.agents.matches > 0)
        assertEquals(state.agents.matches, state.weapons.matches)
        assertTrue(state.weapons.weapons.all { it.weapon in state.catalog.weapons })
        assertTrue(state.agents.agents.all { it.agent in state.catalog.agents })
    }

    // 메인 스레드에서 세면 홈에서 넘어오는 전환이 멈춘다
    @Test
    fun `프로필은 넘겨받은 곳에서 센다`() = runTest {
        val matches = FakeMatchRepository()
        val viewModel = ProfileViewModel(
            FakeAccountRepository(matches),
            matches,
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
            computation = StandardTestDispatcher(testScheduler),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(ProfileUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertIs<ProfileUiState.Success>(viewModel.uiState.value)
    }

    // 요원·무기·통계는 이번 액트만 보지만 최근 경기는 무엇을 뛰었든 가장 최근 판이다
    @Test
    fun `최근 경기는 큐를 가리지 않고 가장 최근 세 판이다`() = runTest {
        val matches = FakeMatchRepository()
        val viewModel = ProfileViewModel(
            FakeAccountRepository(matches),
            matches,
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
            computation = SameThread,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ProfileUiState.Success>(viewModel.uiState.value)
        val all = matches.observeMatches().first().sortedByDescending { it.startedAt }
        assertEquals(all.take(3), state.recentMatches)
        assertTrue(state.hasMoreMatches)
        assertEquals(state.agents.matches, state.summary.metrics.matches)
    }
}
