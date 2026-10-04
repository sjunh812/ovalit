package com.ovalit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.launchNotifying
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val account: Account?,
        val preferences: UserPreferences,
        val storedMatches: Int,
    ) : SettingsUiState
}

class SettingsViewModel(
    private val accountRepository: AccountRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val matchRepository: MatchRepository,
    private val importScheduler: ImportScheduler,
) : ViewModel() {

    // 연동을 해제하는 동안에는 화면을 그대로 둔다. 지우는 대로 따라 그리면 인트로로 밀려나는 설정 화면에 "연동되지
    // 않았어요"와 0경기가 잠깐 보인다.
    private var unlinking = false

    val uiState: StateFlow<SettingsUiState> = combine(
        accountRepository.account,
        preferencesRepository.preferences,
        matchRepository.observeMatches().map { it.size },
    ) { account, preferences, storedMatches ->
        SettingsUiState.Success(account, preferences, storedMatches)
    }.filter { !unlinking }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    fun setStatsPublic(public: Boolean) = launch { preferencesRepository.setStatsPublic(public) }

    fun setNotifyAnalysisDone(enabled: Boolean) = launch { preferencesRepository.setNotifyAnalysisDone(enabled) }

    fun setNotifyWeeklyReport(enabled: Boolean) = launch { preferencesRepository.setNotifyWeeklyReport(enabled) }

    fun setNotifyPing(enabled: Boolean) = launch { preferencesRepository.setNotifyPing(enabled) }

    /** 실제 저장소가 붙으면 서버(`PATCH /me`의 `remindBefore`)에도 맡깁니다. 미리 알림은 서버가 이 시간에 맞춰 보냅니다. */
    fun setPingReminder(reminder: PingReminder) = launch { preferencesRepository.setPingReminder(reminder) }

    fun setShareUsageStats(enabled: Boolean) = launch { preferencesRepository.setShareUsageStats(enabled) }

    fun setTheme(theme: ThemePreference) = launch { preferencesRepository.setTheme(theme) }

    fun setFocus(focus: Focus) = launch { preferencesRepository.setFocus(focus) }

    fun setDefaultQueue(queue: QueueFilter) = launch { preferencesRepository.setDefaultQueue(queue) }

    // 기다리던 새 경기 이어 받기도 멈춘다. 지운 뒤에 "새 경기를 다 받았어요" 알림이 가면 안 된다.
    fun deleteData() = launch(FailedAction.DELETE_DATA) {
        importScheduler.cancel()
        matchRepository.deleteAll()
    }

    /**
     * 첫 수집을 멈추고, 연동을 해제해 경기와 친구를 다 지운 뒤 [onUnlinked]를 부릅니다.
     *
     * 수집을 먼저 멈추지 않으면 해제한 뒤에 경기를 다시 채우고 "분석을 마쳤어요" 알림까지 보냅니다. 다 지우기 전에 화면을
     * 옮기면 설정 화면이 스택에서 빠지면서 이 ViewModel이 정리돼, 지우던 작업이 중간에 끊길 수 있습니다.
     */
    fun unlink(onUnlinked: () -> Unit) = launch(FailedAction.UNLINK, onFailure = { unlinking = false }) {
        unlinking = true
        importScheduler.cancel()
        accountRepository.unlink()
        onUnlinked()
    }

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    private fun launch(action: FailedAction = FailedAction.SETTING, onFailure: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launchNotifying(failures, action, onFailure) { block() }
    }
}
