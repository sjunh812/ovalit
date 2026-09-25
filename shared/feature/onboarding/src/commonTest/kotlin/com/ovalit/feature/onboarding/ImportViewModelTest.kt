package com.ovalit.feature.onboarding

import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.feature.onboarding.importing.ImportUiState
import com.ovalit.feature.onboarding.importing.ImportViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 수집이 진행되는 대로 받은 경기 수가 늘어난다`() = runTest {
        val matches = FakeMatchRepository(importDelay = Duration.ZERO)
        val viewModel = ImportViewModel(matches, InMemoryPreferences())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertNull(assertIs<ImportUiState.Success>(viewModel.uiState.value).progress)

        matches.importRecent()

        val progress = assertIs<ImportUiState.Success>(viewModel.uiState.value).progress
        assertTrue(progress!!.isDone)
        assertEquals(progress.total, progress.loaded)
    }

    @Test
    fun `고른 관심사를 설정에 저장한다`() = runTest {
        val preferences = InMemoryPreferences()
        val viewModel = ImportViewModel(FakeMatchRepository(), preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectFocus(Focus.CONSISTENCY)

        assertEquals(Focus.CONSISTENCY, preferences.preferences.value.focus)
        assertEquals(Focus.CONSISTENCY, assertIs<ImportUiState.Success>(viewModel.uiState.value).focus)
    }
}

private class InMemoryPreferences : UserPreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences.Default)

    override suspend fun setTheme(theme: ThemePreference) = Unit

    override suspend fun setDefaultQueue(queue: QueueFilter) = Unit

    override suspend fun setStatsPublic(public: Boolean) = Unit

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = Unit

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = Unit

    override suspend fun setFocus(focus: Focus) = preferences.update { it.copy(focus = focus) }
}
