package com.ovalit.feature.settings

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
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
import kotlin.time.Instant
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
    private val importScheduler = RecordingScheduler()
    private val viewModel by lazy { SettingsViewModel(account, preferences, matches, importScheduler) }

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
        viewModel.setTheme(ThemePreference.LIGHT)
        viewModel.setDefaultQueue(QueueFilter.COMPETITIVE)
        viewModel.setFocus(Focus.AIM)

        assertEquals(
            UserPreferences(
                theme = ThemePreference.LIGHT,
                defaultQueue = QueueFilter.COMPETITIVE,
                statsPublic = false,
                notifyAnalysisDone = false,
                notifyWeeklyReport = UserPreferences.Default.notifyWeeklyReport,
                focus = Focus.AIM,
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

    // 지우기 전에 인트로로 보내면 이 ViewModel이 정리되면서 지우던 작업이 끊길 수 있다
    @Test
    fun `연동을 해제하면 경기까지 다 지운 뒤에 알린다`() = runTest {
        collectUiState()
        var unlinkedWhenNotified: Boolean? = null

        viewModel.unlink { unlinkedWhenNotified = account.unlinked }

        assertEquals(true, unlinkedWhenNotified)
        // 수집을 멈추지 않으면 해제한 뒤에 경기를 다시 채우고 알림까지 보낸다
        assertTrue(importScheduler.cancelled)
        assertNull(account.account.first())
        assertTrue(matches.observeMatches().first().isEmpty())
    }

    // 인트로로 밀려나는 동안 "연동되지 않았어요"와 0경기가 잠깐 보이면 안 된다
    @Test
    fun `연동을 해제하는 동안에는 화면을 그대로 둔다`() = runTest {
        collectUiState()
        val before = success()

        viewModel.unlink {}

        assertEquals(before, success())
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun success() = assertIs<SettingsUiState.Success>(viewModel.uiState.value)
}

private class RecordingScheduler : ImportScheduler {
    var cancelled = false
        private set

    override fun start() = Unit

    override fun continueNewMatches(total: Int) = Unit

    override fun cancel() {
        cancelled = true
    }
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

    override suspend fun setNotifyPing(enabled: Boolean) = preferences.update { it.copy(notifyPing = enabled) }

    override suspend fun setFocus(focus: Focus) = preferences.update { it.copy(focus = focus) }

    override suspend fun setSeenProfileHint() = preferences.update { it.copy(seenProfileHint = true) }

    override suspend fun setAdFreeUntil(until: Instant) = preferences.update { it.copy(adFreeUntil = until) }
}
