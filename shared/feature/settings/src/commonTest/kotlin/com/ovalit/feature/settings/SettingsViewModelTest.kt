package com.ovalit.feature.settings

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.AccountSettingsRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeAccountSettingsRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.model.AccountSettings
import com.ovalit.core.model.Focus
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.core.testing.TestImportScheduler
import com.ovalit.core.testing.TestUserPreferencesRepository
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val matches = FakeMatchRepository()
    private val account = RecordingAccount(FakeAccountRepository(matches))
    private val preferences = TestUserPreferencesRepository()
    private val server = FakeAccountSettingsRepository(latency = 1.seconds)
    private val accountSettings = CountingSettings(server)
    private val importScheduler = TestImportScheduler()
    private val viewModel by lazy { SettingsViewModel(account, preferences, accountSettings, matches, importScheduler) }

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
        assertEquals(AccountSettings.Default, state.accountSettings)
        assertEquals(matches.observeMatches().first().size, state.storedMatches)
        assertTrue(state.storedMatches > 0)
    }

    @Test
    fun `토글과 선택지를 바꾸면 기기 설정에 저장한다`() = runTest {
        collectUiState()

        viewModel.setNotifyAnalysisDone(false)
        viewModel.setTheme(ThemePreference.LIGHT)
        viewModel.setDefaultQueue(QueueFilter.COMPETITIVE)
        viewModel.setFocus(Focus.AIM)

        assertEquals(
            UserPreferences(
                theme = ThemePreference.LIGHT,
                defaultQueue = QueueFilter.COMPETITIVE,
                notifyAnalysisDone = false,
                notifyWeeklyReport = UserPreferences.Default.notifyWeeklyReport,
                focus = Focus.AIM,
            ),
            success().preferences,
        )
    }

    // 서버가 이 값을 보고 친구에게 전적을 내준다. 기기에만 두면 꺼도 친구에게 그대로 보인다.
    @Test
    fun `전적 공개와 시작 전 알림은 서버에 두는 설정에 맡긴다`() = runTest {
        collectUiState()
        val notices = notices()

        viewModel.setStatsPublic(false)
        viewModel.setPingReminder(PingReminder.ONE_HOUR)
        advanceUntilIdle()

        assertEquals(AccountSettings(statsPublic = false, pingReminder = PingReminder.ONE_HOUR), success().accountSettings)
        assertEquals(UserPreferences.Default, success().preferences)
        assertEquals(emptyList(), notices)
    }

    @Test
    fun `전적 공개를 끄면 서버 답을 기다리지 않고 스위치가 바로 움직인다`() = runTest {
        collectUiState()

        viewModel.setStatsPublic(false)

        assertEquals(false, success().accountSettings.statsPublic)
    }

    @Test
    fun `전적 공개를 서버에 보내지 못하면 스위치를 되돌리고 까닭을 알린다`() = runTest {
        collectUiState()
        val notices = notices()
        server.failure = OvalitError.Offline

        viewModel.setStatsPublic(false)
        assertEquals(false, success().accountSettings.statsPublic)
        advanceUntilIdle()

        assertEquals(true, success().accountSettings.statsPublic)
        assertEquals(listOf(FailureNotice(FailedAction.SETTING, OvalitError.Offline)), notices)
    }

    @Test
    fun `시작 전 알림을 서버에 보내지 못하면 전에 고른 시간으로 돌아간다`() = runTest {
        collectUiState()
        val notices = notices()
        server.failure = OvalitError.Unknown

        viewModel.setPingReminder(PingReminder.OFF)
        assertEquals(PingReminder.OFF, success().accountSettings.pingReminder)
        advanceUntilIdle()

        assertEquals(PingReminder.TEN_MINUTES, success().accountSettings.pingReminder)
        assertEquals(listOf(FailureNotice(FailedAction.SETTING)), notices)
    }

    // CLAUDE.md 실패 안내: 저절로 한 일은 조용히 넘긴다
    @Test
    fun `설정을 열면 서버 값을 다시 받고 받지 못해도 알리지 않는다`() = runTest {
        server.failure = OvalitError.Offline
        collectUiState()
        val notices = notices()

        advanceUntilIdle()

        assertEquals(1, accountSettings.refreshed)
        assertEquals(AccountSettings.Default, success().accountSettings)
        assertEquals(emptyList(), notices)
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

    private fun TestScope.notices(): List<FailureNotice> {
        val received = mutableListOf<FailureNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.notices.collect { received += it } }
        return received
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

private class CountingSettings(
    private val delegate: FakeAccountSettingsRepository,
) : AccountSettingsRepository by delegate {
    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
        delegate.refresh()
    }
}
