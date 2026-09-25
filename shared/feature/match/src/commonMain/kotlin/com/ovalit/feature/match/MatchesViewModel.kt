package com.ovalit.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.QueueFilter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface MatchesUiState {
    data object Loading : MatchesUiState

    /**
     * @property days 고른 큐와 필터에 맞는 경기를 날짜별로 묶었습니다. 최근 날짜가 위입니다.
     * @property agents 필터에서 고를 수 있는 요원입니다. 고른 큐에서 많이 한 순입니다.
     */
    data class Success(
        val queueFilter: QueueFilter,
        val filter: MatchFilter,
        val days: List<MatchDay>,
        val agents: List<AgentId>,
        val maps: List<MapId>,
        val catalog: ContentCatalog,
        val now: Instant,
        val timeZone: TimeZone,
    ) : MatchesUiState
}

data class MatchFilter(val agent: AgentId? = null, val map: MapId? = null) {
    val isActive: Boolean get() = agent != null || map != null
}

data class MatchDay(val date: LocalDate, val matches: List<Match>)

/** S2 경기 목록입니다. 경기는 값이 바뀌지 않고 늘어나기만 해서, 받는 대로 목록에 채웁니다. */
class MatchesViewModel(
    private val matchRepository: MatchRepository,
    preferencesRepository: UserPreferencesRepository,
    contentRepository: ContentRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {

    // 홈과 마찬가지로 칩을 고르기 전까지는 설정의 기본 큐를 따른다
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)
    private val filter = MutableStateFlow(MatchFilter())
    private val refreshing = MutableStateFlow(false)

    /** 목록을 당겨 새 경기를 받는 중인지입니다. */
    val isRefreshing: StateFlow<Boolean> = refreshing

    val uiState: StateFlow<MatchesUiState> = combine(
        matchRepository.observeMatches(),
        preferencesRepository.preferences,
        selectedQueue,
        filter,
        contentRepository.catalog,
    ) { matches, preferences, selected, filter, catalog ->
        val queueFilter = selected ?: preferences.defaultQueue
        val inQueue = matches.filter { it.queue in queueFilter.queues }.sortedByDescending { it.startedAt }
        val shown = inQueue.filter { match ->
            (filter.agent == null || match.myAgent == filter.agent) && (filter.map == null || match.map == filter.map)
        }
        MatchesUiState.Success(
            queueFilter = queueFilter,
            filter = filter,
            days = shown
                .groupBy { it.startedAt.toLocalDateTime(timeZone).date }
                .map { (date, dayMatches) -> MatchDay(date, dayMatches) },
            agents = inQueue.mostPlayed { it.myAgent },
            maps = inQueue.mostPlayed { it.map },
            catalog = catalog,
            now = clock.now(),
            timeZone = timeZone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchesUiState.Loading,
    )

    /** 새로 끝난 경기를 받습니다. 받는 중에 또 당기면 무시합니다. 실패해도 저장해 둔 경기는 그대로 둡니다. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                runCatching { matchRepository.refresh() }
            } finally {
                refreshing.value = false
            }
        }
    }

    fun selectQueue(queueFilter: QueueFilter) {
        selectedQueue.value = queueFilter
    }

    fun setFilter(value: MatchFilter) {
        filter.value = value
    }
}

private fun <T> List<Match>.mostPlayed(key: (Match) -> T): List<T> =
    groupingBy(key).eachCount().entries.sortedByDescending { it.value }.map { it.key }
