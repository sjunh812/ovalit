package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface ReportUiState {
    data object Loading : ReportUiState

    data class Success(
        val queueFilter: QueueFilter,
        val report: WeeklyReport,
    ) : ReportUiState
}

class ReportViewModel(
    matchRepository: MatchRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    private val queueFilter = MutableStateFlow(QueueFilter.COMPETITIVE_AND_UNRATED)

    val uiState: StateFlow<ReportUiState> = combine(matchRepository.observeMatches(), queueFilter) { matches, filter ->
        ReportUiState.Success(
            queueFilter = filter,
            report = matches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = filter),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState.Loading,
    )

    fun selectQueue(filter: QueueFilter) {
        queueFilter.value = filter
    }
}
