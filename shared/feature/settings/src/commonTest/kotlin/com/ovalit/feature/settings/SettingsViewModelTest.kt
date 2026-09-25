package com.ovalit.feature.settings

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val matches = FakeMatchRepository()
    private val account = RecordingAccount(FakeAccountRepository(matches))
    private val preferences = InMemoryPreferences()
    private val viewModel by lazy { SettingsViewModel(account, preferences, matches) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `구독하기 전에는 불러오는 중이다`() {
        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `계정과 설정과 저장된 경기 수를 보여준다`() = runTest {
        collectUiState()

        val state = success()
        assertNotNull(state.account)
        assertEquals(UserPreferences.Default, state.preferences)
        assertEquals(matches.observeMatches().first().size, state.storedMatches)
        assertTrue(state.storedMatches > 0)
    }

    @Test
    fun `토글과 선택지를 바꾸면 설정에 저장한다`() = runTest {
        collectUiState()

        viewModel.setStatsPublic(false)
        viewModel.setNotifyAnalysisDone(false)
        viewModel.setNotifyWeeklyReport(false)
        viewModel.setTheme(ThemePreference.LIGHT)
        viewModel.setDefaultQueue(QueueFilter.COMPETITIVE)

        assertEquals(
            UserPreferences(
                theme = ThemePreference.LIGHT,
                defaultQueue = QueueFilter.COMPETITIVE,
                statsPublic = false,
                notifyAnalysisDone = false,
                notifyWeeklyReport = false,
            ),
            success().preferences,
        )
    }

    @Test
    fun `저장된 데이터를 지우면 경기만 비우고 연동은 그대로 둔다`() = runTest {
        collectUiState()

        viewModel.deleteData()

        assertEquals(0, success().storedMatches)
        assertNotNull(success().account)
    }

    // 지우기 전에 인트로로 보내면 홈이 지워질 경기를 잠깐 그린다
    @Test
    fun `연동을 해제하면 경기까지 다 지운 뒤에 알린다`() = runTest {
        collectUiState()
        var unlinkedWhenNotified: Boolean? = null

        viewModel.unlink { unlinkedWhenNotified = account.unlinked }

        assertEquals(true, unlinkedWhenNotified)
        assertNull(success().account)
        assertEquals(0, success().storedMatches)
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun success() = assertIs<SettingsUiState.Success>(viewModel.uiState.value)
}

private class RecordingAccount(private val delegate: FakeAccountRepository) : AccountRepository by delegate {
    var unlinked = false
        private set

    override suspend fun unlink() {
        delegate.unlink()
        unlinked = true
    }
}

private class InMemoryPreferences : UserPreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences.Default)

    override suspend fun setTheme(theme: ThemePreference) = preferences.update { it.copy(theme = theme) }

    override suspend fun setDefaultQueue(queue: QueueFilter) = preferences.update { it.copy(defaultQueue = queue) }

    override suspend fun setStatsPublic(public: Boolean) = preferences.update { it.copy(statsPublic = public) }

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) =
        preferences.update { it.copy(notifyAnalysisDone = enabled) }

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) =
        preferences.update { it.copy(notifyWeeklyReport = enabled) }
}
