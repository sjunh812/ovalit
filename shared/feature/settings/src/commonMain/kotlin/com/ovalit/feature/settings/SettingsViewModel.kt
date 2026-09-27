package com.ovalit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val uiState: StateFlow<SettingsUiState> = combine(
        accountRepository.account,
        preferencesRepository.preferences,
        matchRepository.observeMatches().map { it.size },
    ) { account, preferences, storedMatches ->
        SettingsUiState.Success(account, preferences, storedMatches)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    fun setStatsPublic(public: Boolean) = launch { preferencesRepository.setStatsPublic(public) }

    fun setNotifyAnalysisDone(enabled: Boolean) = launch { preferencesRepository.setNotifyAnalysisDone(enabled) }

    fun setNotifyWeeklyReport(enabled: Boolean) = launch { preferencesRepository.setNotifyWeeklyReport(enabled) }

    fun setTheme(theme: ThemePreference) = launch { preferencesRepository.setTheme(theme) }

    fun setFocus(focus: Focus) = launch { preferencesRepository.setFocus(focus) }

    fun setDefaultQueue(queue: QueueFilter) = launch { preferencesRepository.setDefaultQueue(queue) }

    fun deleteData() = launch { matchRepository.deleteAll() }

    /**
     * 첫 수집을 멈추고, 연동을 해제해 경기와 친구를 다 지운 뒤 [onUnlinked]를 부릅니다.
     *
     * 수집을 먼저 멈추지 않으면 해제한 뒤에 경기를 다시 채우고 "분석을 마쳤어요" 알림까지 보냅니다. 다 지우기 전에 화면을
     * 옮기면 홈이 지워지는 경기를 잠깐 그립니다.
     */
    fun unlink(onUnlinked: () -> Unit) = launch {
        importScheduler.cancel()
        accountRepository.unlink()
        onUnlinked()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
