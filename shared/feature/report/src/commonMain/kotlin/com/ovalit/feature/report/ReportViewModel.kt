package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    accountRepository: AccountRepository,
    preferencesRepository: UserPreferencesRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    // 칩으로 고르기 전까지는 설정의 기본 큐를 따른다. 고른 칩은 이 화면에 있는 동안만 유지한다.
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)

    val uiState: StateFlow<ReportUiState> = combine(
        matchRepository.observeMatches(),
        preferencesRepository.preferences,
        selectedQueue,
    ) { matches, preferences, selected ->
        val filter = selected ?: preferences.defaultQueue
        ReportUiState.Success(
            queueFilter = filter,
            report = matches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = filter),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState.Loading,
    )

    /** 오른쪽 위 아바타에 쓰는 Riot ID입니다. 연동을 해제했으면 `null`입니다. */
    val riotId: StateFlow<String?> = accountRepository.account
        .map { it?.riotId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun selectQueue(filter: QueueFilter) {
        selectedQueue.value = filter
    }
}
