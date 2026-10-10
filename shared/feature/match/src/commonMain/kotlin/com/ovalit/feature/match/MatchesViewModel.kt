package com.ovalit.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.countNewMatches
import com.ovalit.core.data.logRefresh
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.RefreshResults
import com.ovalit.core.ui.launchNotifying
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
     * @property noStoredMatches 설정에서 저장된 데이터를 지워 기기에 경기가 하나도 없는지입니다.
     *   "이 큐로 뛴 경기가 없어요"는 틀린 말이라 다시 불러올 곳을 알려 줍니다.
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
        val noStoredMatches: Boolean = false,
    ) : MatchesUiState
}

data class MatchFilter(val agent: AgentId? = null, val map: MapId? = null) {
    val isActive: Boolean get() = agent != null || map != null
}

data class MatchDay(val date: LocalDate, val matches: List<Match>) {
    /**
     * 날짜 머리 오른쪽에 적을 그날 승패입니다.
     * [matches]가 이미 고른 큐와 필터로 거른 경기라 보이는 줄만 셉니다.
     */
    val record: DayRecord? = matches.dayRecord()
}

data class DayRecord(val wins: Int, val losses: Int, val draws: Int)

// 데스매치와 건틀릿은 승패가 아니라 등수로 끝나서 세지 않는다. 경기 줄도 그런 판에는 승패를 적지 않는다.
// 이기거나 진 판이 하나도 없으면 "0승 0패"가 되니 적지 않는다.
private fun List<Match>.dayRecord(): DayRecord? {
    val twoTeams = filter { it.format == MatchFormat.ROUNDS || it.format == MatchFormat.TEAM_POINTS }
    val wins = twoTeams.count { it.myTeamWon == true }
    val losses = twoTeams.count { it.myTeamWon == false }
    if (wins + losses == 0) return null
    return DayRecord(wins = wins, losses = losses, draws = twoTeams.size - wins - losses)
}

/** S2 경기 목록입니다. 경기는 값이 바뀌지 않고 늘어나기만 해서 받는 대로 목록에 채웁니다. */
class MatchesViewModel(
    private val matchRepository: MatchRepository,
    preferencesRepository: UserPreferencesRepository,
    contentRepository: ContentRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val analytics: Analytics = NoAnalytics,
    minuteChanges: Flow<Unit> = flowOf(Unit),
) : ViewModel() {

    // 홈과 마찬가지로 칩을 고르기 전까지는 설정의 기본 큐를 따른다
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)
    private val filter = MutableStateFlow(MatchFilter())
    private val refreshing = MutableStateFlow(false)

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    private val refreshResults = RefreshResults()

    /**
     * 당겨서 새 경기를 다 받으면 몇 판을 받았는지입니다. 화면 아래에 한 줄 띄웁니다.
     * 받는 동안 진행 줄이 떴어도 다 받으면 보냅니다.
     */
    val refreshed: Flow<Int> = refreshResults.flow

    /**
     * 새 경기를 여러 판 받는 중이면 몇 판 중 몇 판을 받았는지입니다.
     * 목록 맨 위 진행 줄로 띄우고, 띄울 만큼 많지 않으면 `null`입니다.
     */
    val newMatches: StateFlow<NewMatchesProgress?> = matchRepository.newMatchesProgress
        .map { progress -> progress?.takeIf { it.isShown } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 목록을 당겨 새 경기를 받는 중인지입니다. 진행 줄이 뜨면 당김 표시는 거둡니다. */
    val isRefreshing: StateFlow<Boolean> = refreshing

    val uiState: StateFlow<MatchesUiState> = combine(
        matchRepository.observeMatches(),
        preferencesRepository.preferences,
        selectedQueue,
        filter,
        // "N분 전"과 오늘·어제 머리가 화면을 켜 둔 동안에도 흐르게 분마다 다시 센다
        combine(contentRepository.catalog, minuteChanges) { catalog, _ -> catalog },
    ) { matches, preferences, selected, chosen, catalog ->
        val queueFilter = selected ?: preferences.defaultQueue
        // 경기를 모두 지웠으면 남겨 둔 요원·맵 필터도 소용없다.
        // 그대로 두면 다시 불러온 뒤에도 "조건에 맞는 경기가 없어요"가 뜬다.
        val filter = if (matches.isEmpty()) MatchFilter() else chosen
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
            // 카탈로그에 이름이 없는 요원과 맵(건틀릿의 로봇 등)은 같은 "알 수 없는 요원" 칩이 여럿 생겨 고를 수 없어 뺀다
            agents = inQueue.mostPlayed { it.myAgent }.filter { it in catalog.agents },
            maps = inQueue.mostPlayed { it.map }.filter { it in catalog.maps },
            catalog = catalog,
            now = clock.now(),
            timeZone = timeZone,
            noStoredMatches = matches.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchesUiState.Loading,
    )

    /**
     * 새로 끝난 경기를 받습니다. 받는 중에 또 당기거나 진행 줄이 떠 있으면 무시합니다.
     * 실패해도 저장해 둔 경기는 그대로 둡니다.
     */
    fun refresh() {
        if (refreshing.value || newMatches.value != null) return
        refreshing.value = true
        viewModelScope.launchNotifying(failures, FailedAction.REFRESH) {
            val untilLineShows = launch {
                matchRepository.newMatchesProgress.first { it?.isShown == true }
                refreshing.value = false
            }
            val received = try {
                matchRepository.countNewMatches { logRefresh(analytics, source = "matches") { matchRepository.refresh() } }
            } finally {
                untilLineShows.cancel()
                refreshing.value = false
            }
            received?.let(refreshResults::send)
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
