package com.ovalit.feature.match

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.model.Queue
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val ThursdayClock = object : Clock {
    override fun now(): Instant = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `경기는 날짜별로 묶고 최근 날짜와 최근 경기가 위다`() = runTest {
        val state = collect(viewModel())

        val days = state.days.map { it.date }
        assertEquals(days.sortedDescending(), days)
        state.days.forEach { day -> assertEquals(day.matches.sortedByDescending { it.startedAt }, day.matches) }
    }

    @Test
    fun `칩을 고르기 전에는 설정의 기본 큐로 연다`() = runTest {
        val state = collect(viewModel(UserPreferences.Default.copy(defaultQueue = QueueFilter.COMPETITIVE)))

        assertEquals(QueueFilter.COMPETITIVE, state.queueFilter)
        assertTrue(state.days.flatMap { it.matches }.all { it.queue == Queue.COMPETITIVE })
    }

    @Test
    fun `요원과 맵을 고르면 둘 다 맞는 경기만 남긴다`() = runTest {
        val viewModel = viewModel()
        val all = collect(viewModel)
        val agent = all.agents.first()
        val map = all.maps.first()

        viewModel.setFilter(MatchFilter(agent = agent, map = map))

        val filtered = assertIs<MatchesUiState.Success>(viewModel.uiState.value).days.flatMap { it.matches }
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.myAgent == agent && it.map == map })
    }

    // 고를 수 있는 요원은 많이 한 순이다. 필터 시트 첫 줄에 주로 하는 요원이 온다.
    @Test
    fun `필터의 요원은 그 큐에서 많이 한 순이다`() = runTest {
        val state = collect(viewModel())
        val counts = state.days.flatMap { it.matches }.groupingBy { it.myAgent }.eachCount()

        assertEquals(state.agents.map { counts.getValue(it) }, state.agents.map { counts.getValue(it) }.sortedDescending())
    }

    @Test
    fun `당겨서 새로고침하면 방금 끝난 경기를 맨 위에 올린다`() = runTest {
        val viewModel = viewModel()
        val before = collect(viewModel).days.sumOf { it.matches.size }

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        val state = assertIs<MatchesUiState.Success>(viewModel.uiState.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(before + 1, state.days.sumOf { it.matches.size })
        assertEquals("fresh-1", state.days.first().matches.first().id.value)
    }

    // 받는 중에 또 당기면 같은 경기를 두 번 받는다. 레이트 리밋도 두 배로 쓴다.
    @Test
    fun `받는 중에 또 당겨도 한 번만 받는다`() = runTest {
        val viewModel = viewModel()
        val before = collect(viewModel).days.sumOf { it.matches.size }

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(before + 1, assertIs<MatchesUiState.Success>(viewModel.uiState.value).days.sumOf { it.matches.size })
    }

    private fun viewModel(preferences: UserPreferences = UserPreferences.Default) = MatchesViewModel(
        FakeMatchRepository(ThursdayClock),
        StubPreferences(preferences),
        FakeContentRepository(),
        ThursdayClock,
        Seoul,
    )

    private suspend fun TestScope.collect(viewModel: MatchesViewModel): MatchesUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return assertIs(viewModel.uiState.value)
    }
}

internal class StubPreferences(initial: UserPreferences = UserPreferences.Default) : UserPreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun setTheme(theme: ThemePreference) = Unit

    override suspend fun setDefaultQueue(queue: QueueFilter) = Unit

    override suspend fun setStatsPublic(public: Boolean) = Unit

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = Unit

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = Unit

    override suspend fun setFocus(focus: Focus) = Unit
}
