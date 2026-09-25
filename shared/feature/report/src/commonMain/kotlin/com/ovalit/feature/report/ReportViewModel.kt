package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface ReportUiState {
    data object Loading : ReportUiState

    data class Success(val report: WeeklyReport) : ReportUiState
}

class ReportViewModel(
    matchRepository: MatchRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    val uiState: StateFlow<ReportUiState> = matchRepository.observeMatches()
        .map { matches -> ReportUiState.Success(matches.weeklyReport(now = clock.now(), timeZone = timeZone)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportUiState.Loading,
        )
}
