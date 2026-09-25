package com.ovalit.feature.report

import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.Match
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)
private val ThursdayClock = object : Clock {
    override fun now(): Instant = Thursday
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `경기를 받기 전에는 불러오는 중이다`() {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), ThursdayClock, Seoul)

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `받은 경기로 주간 리포트를 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(QueueFilter.COMPETITIVE_AND_UNRATED, state.queueFilter)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    @Test
    fun `경기가 새로 들어오면 리포트를 다시 만든다`() = runTest {
        val matches = MutableStateFlow<List<Match>>(emptyList())
        val viewModel = ReportViewModel(StubRepository(matches), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(
            ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, WeeklyReport.NotEnoughMatches(played = 0)),
            viewModel.uiState.value,
        )

        matches.value = FakeMatchRepository(ThursdayClock).observeMatches().first()

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    // 가짜 경기는 경쟁과 일반뿐이라 기타로 바꾸면 한 경기도 없다
    @Test
    fun `큐를 바꾸면 그 큐 경기로 리포트를 다시 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectQueue(QueueFilter.OTHER)

        assertEquals(
            ReportUiState.Success(QueueFilter.OTHER, WeeklyReport.NotEnoughMatches(played = 0)),
            viewModel.uiState.value,
        )
    }
}

private class StubRepository(private val matches: Flow<List<Match>>) : MatchRepository {
    override fun observeMatches(): Flow<List<Match>> = matches
}
