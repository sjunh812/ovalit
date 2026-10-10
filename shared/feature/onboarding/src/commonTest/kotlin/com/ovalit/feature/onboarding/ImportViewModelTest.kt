package com.ovalit.feature.onboarding

import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.testing.TestImportScheduler
import com.ovalit.core.testing.TestUserPreferencesRepository
import com.ovalit.feature.onboarding.importing.ImportUiState
import com.ovalit.feature.onboarding.importing.ImportViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
        // 연동하면 저장된 경기와 지난 진행도를 지운 뒤 첫 수집을 시작한다
        matches.deleteAll()
        val viewModel = ImportViewModel(matches, TestUserPreferencesRepository(), TestImportScheduler())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertNull(assertIs<ImportUiState.Success>(viewModel.uiState.value).progress)

        matches.importRecent()

        val progress = assertIs<ImportUiState.Success>(viewModel.uiState.value).progress
        assertTrue(progress!!.isDone)
        assertEquals(progress.total, progress.loaded)
    }

    @Test
    fun `고른 관심사를 설정에 저장한다`() = runTest {
        val preferences = TestUserPreferencesRepository()
        val viewModel = ImportViewModel(FakeMatchRepository(), preferences, TestImportScheduler())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectFocus(Focus.CONSISTENCY)

        assertEquals(Focus.CONSISTENCY, preferences.preferences.value.focus)
        assertEquals(Focus.CONSISTENCY, assertIs<ImportUiState.Success>(viewModel.uiState.value).focus)
    }

    // 설정에서 분석 완료 알림을 껐으면 S0-4에 다 불러오면 알리겠다는 말을 적지 않는다
    @Test
    fun `설정의 분석 완료 알림을 따른다`() = runTest {
        val preferences = TestUserPreferencesRepository()
        val viewModel = ImportViewModel(FakeMatchRepository(), preferences, TestImportScheduler())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertTrue(assertIs<ImportUiState.Success>(viewModel.uiState.value).notifyWhenDone)

        preferences.setNotifyAnalysisDone(false)

        assertFalse(assertIs<ImportUiState.Success>(viewModel.uiState.value).notifyWhenDone)
    }
}
