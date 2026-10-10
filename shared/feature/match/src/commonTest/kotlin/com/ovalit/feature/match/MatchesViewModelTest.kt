package com.ovalit.feature.match

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.Queue
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
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

    // 필터 시트 첫 줄에 주로 하는 요원이 온다
    @Test
    fun `필터의 요원은 그 큐에서 많이 한 순이다`() = runTest {
        val state = collect(viewModel())
        val counts = state.days.flatMap { it.matches }.groupingBy { it.myAgent }.eachCount()

        assertEquals(state.agents.map { counts.getValue(it) }, state.agents.map { counts.getValue(it) }.sortedDescending())
    }

    // 데스매치와 건틀릿은 등수로 끝나 승패가 없다. 경기 줄도 그런 판에는 승패를 적지 않는다.
    @Test
    fun `날짜 머리 승패는 두 팀이 겨룬 경기만 세고 비긴 판은 따로 센다`() {
        val won = MatchPreviewData.detailMatch
        val day = MatchDay(
            date = LocalDate(2026, 9, 24),
            matches = listOf(
                won,
                won.copy(myTeamWon = false),
                won.copy(myTeamWon = null),
                MatchPreviewData.deathmatch,
                MatchPreviewData.gauntlet,
            ),
        )

        assertEquals(DayRecord(wins = 1, losses = 1, draws = 1), day.record)
    }

    @Test
    fun `이기거나 진 판이 없는 날은 승패를 적지 않는다`() {
        val date = LocalDate(2026, 9, 24)

        assertNull(MatchDay(date, listOf(MatchPreviewData.deathmatch, MatchPreviewData.gauntlet)).record)
        assertNull(MatchDay(date, listOf(MatchPreviewData.detailMatch.copy(myTeamWon = null))).record)
    }

    @Test
    fun `날짜 머리 승패는 고른 큐와 필터에 맞는 경기만 센다`() = runTest {
        val viewModel = viewModel()
        val all = collect(viewModel)
        val agent = all.agents.last()

        viewModel.setFilter(MatchFilter(agent = agent))

        val days = assertIs<MatchesUiState.Success>(viewModel.uiState.value).days
        val decided = days.sumOf { (it.record?.wins ?: 0) + (it.record?.losses ?: 0) }
        assertTrue(days.isNotEmpty() && days.all { day -> day.matches.all { it.myAgent == agent } })
        assertEquals(days.sumOf { day -> day.matches.count { it.myTeamWon != null } }, decided)
        assertTrue(decided < all.days.sumOf { day -> day.matches.count { it.myTeamWon != null } })
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
        assertEquals("fresh-1-0", state.days.first().matches.first().id.value)
    }

    // 받는 중에 또 받으면 레이트 리밋을 두 배로 쓴다
    @Test
    fun `받는 중에 또 당겨도 한 번만 받는다`() = runTest {
        val viewModel = viewModel()
        val before = collect(viewModel).days.sumOf { it.matches.size }

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(before + 1, assertIs<MatchesUiState.Success>(viewModel.uiState.value).days.sumOf { it.matches.size })
    }

    // 안내를 넘기고 당김 표시도 거둔다
    @Test
    fun `새 경기를 받지 못하면 까닭을 안내로 넘긴다`() = runTest {
        val failing = object : MatchRepository by FakeMatchRepository(ThursdayClock, scope = this) {
            override suspend fun refresh(): Int = throw OvalitException(OvalitError.Offline)
        }
        val viewModel = MatchesViewModel(failing, StubPreferences(UserPreferences.Default), FakeContentRepository(), ThursdayClock, Seoul)
        val notices = mutableListOf<FailureNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.notices.toList(notices) }

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(FailureNotice(FailedAction.REFRESH, OvalitError.Offline)), notices)
        assertFalse(viewModel.isRefreshing.value)
    }

    private fun TestScope.viewModel(preferences: UserPreferences = UserPreferences.Default) = MatchesViewModel(
        FakeMatchRepository(ThursdayClock, scope = this),
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

    override suspend fun setNotifyPing(enabled: Boolean) = Unit

    override suspend fun setPingReminder(reminder: PingReminder) = Unit


    override suspend fun setFocus(focus: Focus) = Unit

    override suspend fun setSeenProfileHint() = Unit

    override suspend fun setAdFreeUntil(until: Instant) = Unit
}
