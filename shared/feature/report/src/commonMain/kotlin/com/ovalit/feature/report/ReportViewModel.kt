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
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface ReportUiState {
    data object Loading : ReportUiState

    /**
     * @property rival 고른 라이벌입니다. 고르지 않았거나 전적을 공개하지 않았으면 `null`입니다.
     * @property friends 전적을 공개한 친구 전부입니다. 리포트 기간에 경기가 없는 친구도 들어 있습니다. 라이벌은
     * 이 안에서만 고릅니다.
     * @property nudge 라이벌 칸 자리에 두는 유도 칸입니다. [homeNudge]가 정합니다.
     */
    data class Success(
        val queueFilter: QueueFilter,
        val report: WeeklyReport,
        val rival: FriendStanding? = null,
        val friends: List<FriendStanding> = emptyList(),
        val nudge: HomeNudge? = null,
    ) : ReportUiState
}

/** 친구나 라이벌이 없을 때 빈자리 대신 두는 칸입니다. 한 번에 하나만 둡니다. */
enum class HomeNudge {
    INVITE_FRIEND,
    PICK_RIVAL,
}

/**
 * 리포트, 친구, 라이벌 순서로 봅니다. 리포트를 만들 기록이 없으면 그 안내가 먼저라 아무것도 권하지 않고, 친구가
 * 없으면 라이벌을 고를 수 없으니 초대부터 권합니다. 기타 모드에는 친구 칸이 없어서 권하지 않습니다.
 *
 * @param hasFriends 전적 공개와 상관없이 친구가 한 명이라도 있는지입니다.
 * @param rivalCandidates 라이벌로 고를 수 있는 친구입니다. 전적을 공개한 친구뿐이라, 친구가 모두 비공개면 권하지 않습니다.
 */
internal fun homeNudge(
    report: WeeklyReport,
    queueFilter: QueueFilter,
    hasFriends: Boolean,
    rivalCandidates: List<FriendStanding>,
    rival: FriendStanding?,
): HomeNudge? = when {
    report !is WeeklyReport.Ready || !queueFilter.hasDynamicMetrics -> null
    !hasFriends -> HomeNudge.INVITE_FRIEND
    rival == null && rivalCandidates.isNotEmpty() -> HomeNudge.PICK_RIVAL
    else -> null
}

/** @property metrics 내 리포트와 같은 기간의 합계입니다. 그 기간에 경기가 없으면 `null`입니다. */
data class FriendStanding(
    val id: PlayerId,
    val riotId: String,
    val metrics: MatchMetrics?,
)

class ReportViewModel(
    private val matchRepository: MatchRepository,
    accountRepository: AccountRepository,
    preferencesRepository: UserPreferencesRepository,
    private val friendRepository: FriendRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    // 칩으로 고르기 전까지는 설정의 기본 큐를 따른다. 고른 칩은 이 화면에 있는 동안만 유지한다.
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)

    private val refreshing = MutableStateFlow(false)

    /** 홈을 당겨 새 경기를 받는 중인지입니다. */
    val isRefreshing: StateFlow<Boolean> = refreshing

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
        val rival = standings.firstOrNull { it.id == rivalId }
        ReportUiState.Success(
            queueFilter = filter,
            report = report,
            rival = rival,
            friends = standings,
            nudge = homeNudge(report, filter, hasFriends = friends.isNotEmpty(), rivalCandidates = standings, rival = rival),
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

    /** 홈의 유도 칸에서 고른 라이벌입니다. S5의 라이벌 지정과 같은 값을 바꿉니다. */
    fun selectRival(id: PlayerId) {
        viewModelScope.launch { friendRepository.setRival(id) }
    }
}
