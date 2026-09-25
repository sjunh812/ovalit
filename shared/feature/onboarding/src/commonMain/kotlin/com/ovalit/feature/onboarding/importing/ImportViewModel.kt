package com.ovalit.feature.onboarding.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /** @property progress 수집을 아직 시작하지 않았으면 `null`입니다. */
    data class Success(val progress: ImportProgress?, val focus: Focus) : ImportUiState
}

/** S0-4입니다. 첫 수집이 도는 동안 관심사를 고르게 합니다. 수집 자체는 앱 모듈이 시작합니다. */
class ImportViewModel(
    matchRepository: MatchRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ImportUiState> = combine(
        matchRepository.importProgress,
        preferencesRepository.preferences,
    ) { progress, preferences ->
        ImportUiState.Success(progress, preferences.focus)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ImportUiState.Loading,
    )

    fun selectFocus(focus: Focus) {
        viewModelScope.launch { preferencesRepository.setFocus(focus) }
    }
}
