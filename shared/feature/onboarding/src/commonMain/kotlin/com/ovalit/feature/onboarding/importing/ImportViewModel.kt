package com.ovalit.feature.onboarding.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.model.ImportProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    data object Loading : ImportUiState

    /**
     * @property progress 수집을 아직 시작하지 않았으면 `null`입니다.
     * @property notifyWhenDone 설정에서 분석 완료 알림을 켜 두었는지입니다. 껐으면 다 불러와도 알림을 보내지 않습니다.
     */
    data class Success(val progress: ImportProgress?, val focus: Focus, val notifyWhenDone: Boolean = true) : ImportUiState
}

/** S0-4입니다. 첫 수집이 도는 동안 관심사를 고르게 합니다. 수집 자체는 앱 모듈이 시작합니다. */
class ImportViewModel(
    matchRepository: MatchRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val importScheduler: ImportScheduler,
) : ViewModel() {

    val uiState: StateFlow<ImportUiState> = combine(
        matchRepository.importProgress,
        preferencesRepository.preferences,
    ) { progress, preferences ->
        ImportUiState.Success(progress, preferences.focus, notifyWhenDone = preferences.notifyAnalysisDone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ImportUiState.Loading,
    )

    fun selectFocus(focus: Focus) {
        viewModelScope.launch { runCatching { preferencesRepository.setFocus(focus) } }
    }

    /** 멈춘 첫 수집을 지금 이어 받습니다. 기다리면 WorkManager가 저절로 다시 띄우지만 사용자가 바로 다시 할 수 있게 둡니다. */
    fun retry() = importScheduler.retry()
}
