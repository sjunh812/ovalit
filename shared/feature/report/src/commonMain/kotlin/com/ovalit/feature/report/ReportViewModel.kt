package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.metricsIn
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.playerBadge
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface ReportUiState {
    data object Loading : ReportUiState

    /**
     * @property rival 고른 라이벌입니다. 고르지 않았거나 전적을 공개하지 않았으면 `null`입니다.
     * @property friends 전적을 공개한 친구 전부입니다. 리포트 기간에 경기가 없는 친구도 들어 있습니다.
     */
    data class Success(
        val queueFilter: QueueFilter,
        val report: WeeklyReport,
        val rival: FriendStanding? = null,
        val friends: List<FriendStanding> = emptyList(),
    ) : ReportUiState
}

/** @property metrics 내 리포트와 같은 기간의 합계입니다. 그 기간에 경기가 없으면 `null`입니다. */
data class FriendStanding(
    val id: PlayerId,
    val riotId: String,
    val metrics: MatchMetrics?,
)

class ReportViewModel(
    matchRepository: MatchRepository,
    accountRepository: AccountRepository,
    preferencesRepository: UserPreferencesRepository,
    friendRepository: FriendRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    // 칩으로 고르기 전까지는 설정의 기본 큐를 따른다. 고른 칩은 이 화면에 있는 동안만 유지한다.
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)

    // 첫 수집이 끝나기 전에는 숫자를 띄우지 않는다. 헤드샷 24%가 잠시 뒤 19%로 바뀌면 그 뒤로 숫자를 믿지 않는다.
    private val importedMatches = combine(matchRepository.observeMatches(), matchRepository.importProgress) { matches, progress ->
        matches.takeIf { progress == null || progress.isDone }
    }

    val uiState: StateFlow<ReportUiState> = combine(
        importedMatches,
        preferencesRepository.preferences,
        selectedQueue,
        friendRepository.friends,
        friendRepository.rival,
    ) { matches, preferences, selected, friends, rivalId ->
        if (matches == null) return@combine ReportUiState.Loading
        val filter = selected ?: preferences.defaultQueue
        val report = matches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = filter, focus = preferences.focus)
        val standings = if (report is WeeklyReport.Ready) {
            friends
                .filter { it.statsPublic }
                .map { FriendStanding(it.id, it.riotId, it.metricsIn(report, filter, timeZone)) }
        } else {
            emptyList()
        }
        ReportUiState.Success(
            queueFilter = filter,
            report = report,
            rival = standings.firstOrNull { it.id == rivalId },
            friends = standings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState.Loading,
    )

    /** 오른쪽 위 티어와 아바타입니다. 연동을 해제했으면 `null`입니다. */
    val badge: StateFlow<PlayerBadge?> = combine(
        accountRepository.account,
        matchRepository.observeMatches(),
        contentRepository.catalog,
    ) { account, matches, catalog ->
        account?.let { playerBadge(it.riotId, matches, catalog) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun selectQueue(filter: QueueFilter) {
        selectedQueue.value = filter
    }
}
