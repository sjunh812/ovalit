package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.settledMatches
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.forHome
import com.ovalit.core.model.metricsIn
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.launchNotifying
import com.ovalit.core.ui.playerBadge
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface ReportUiState {
    data object Loading : ReportUiState

    /**
     * @property rival 고른 라이벌입니다. 고르지 않았거나, 라이벌이 전적을 공개하지 않았거나, 리포트를 만들지 못했으면
     * `null`입니다.
     * @property friends 전적을 공개한 친구 전부입니다. 리포트 기간에 경기가 없는 친구도 들어 있습니다. 라이벌은
     * 이 안에서만 고릅니다. 리포트를 만들지 못했으면 빈 목록입니다.
     * @property nudge 라이벌 칸 자리에 두는 유도 칸입니다. [homeNudge]가 정합니다.
     * @property waitingForNewMatches 새 경기를 여러 판 받는 중이라 [report]의 기간이 바뀔 수 있는지입니다. 그러면 리포트
     * 자리에 스켈레톤을 둡니다. 오래 쉬어 "지난주"를 보던 사람에게 받기 전 숫자를 띄우면 틀린 말이 됩니다.
     * @property needsImport 설정에서 저장된 데이터를 지워 기기에 경기가 없는지입니다. 그러면 리포트 대신 다시 불러오기를
     * 권합니다. 뛴 경기가 없는 것이 아니라 지운 것이라 "최근 4주 동안 뛴 경기가 없어요"는 틀린 말입니다.
     */
    data class Success(
        val queueFilter: QueueFilter,
        val report: WeeklyReport,
        val rival: FriendStanding? = null,
        val friends: List<FriendStanding> = emptyList(),
        val nudge: HomeNudge? = null,
        val waitingForNewMatches: Boolean = false,
        val needsImport: Boolean = false,
    ) : ReportUiState
}

/** 홈 맨 위 ㅇㅂㅇ 한 줄입니다. [now]는 시각 글자("21:00", "내일 01:00")를 정할 때 씁니다. */
data class HomePing(val ping: Ping, val me: PlayerId, val now: Instant)

/** 친구나 라이벌이 없을 때 빈자리 대신 두는 칸입니다. 한 번에 하나만 둡니다. */
enum class HomeNudge {
    INVITE_FRIEND,
    PICK_RIVAL,
}

/**
 * 리포트, 친구, 라이벌 순서로 봅니다. 리포트를 만들 기록이 없으면 그 안내가 먼저라 아무것도 권하지 않고, 친구가
 * 없으면 라이벌을 고를 수 없으니 초대부터 권합니다. 기타 모드에는 친구 칸이 없어서 권하지 않습니다. 권할 게 없으면
 * `null`입니다.
 *
 * @param hasFriends 전적 공개와 상관없이 친구가 한 명이라도 있는지입니다.
 * @param rivalCandidates 라이벌로 고를 수 있는 친구입니다. 전적을 공개한 친구뿐이라, 친구가 모두 비공개면 빈 목록이고
 * 라이벌을 권하지 않습니다.
 */
internal fun homeNudge(
    report: WeeklyReport,
    queueFilter: QueueFilter,
    hasFriends: Boolean,
    rivalCandidates: List<FriendStanding>,
    rival: FriendStanding?,
    hasRival: Boolean = rival != null,
): HomeNudge? = when {
    report !is WeeklyReport.Ready || !queueFilter.hasDynamicMetrics -> null
    !hasFriends -> HomeNudge.INVITE_FRIEND
    // 라이벌로 둔 친구가 전적을 비공개로 바꾸면 라이벌 칸은 없지만 라이벌은 그대로다. 그때 또 고르라고 하지 않는다.
    !hasRival && rivalCandidates.isNotEmpty() -> HomeNudge.PICK_RIVAL
    else -> null
}

/** @property metrics 내 리포트와 같은 기간의 합계입니다. 그 기간에 경기가 없으면 `null`입니다. */
data class FriendStanding(
    val id: PlayerId,
    val riotId: String,
    val metrics: MatchMetrics?,
)

/**
 * @param weekChanges 흐를 때마다 리포트를 다시 셉니다. 앱에서는 [weekStarts]를 넘겨 월요일 0시에 "이번 주"를 바꿉니다.
 * 테스트는 시계를 멈춰 두므로 한 번만 흐르는 기본값을 씁니다.
 * @param computation 리포트를 세는 곳입니다. 메인 스레드에서 세면 첫 수집 뒤 홈으로 넘어가는 전환이 멈춰서 기본은
 * [Dispatchers.Default]입니다. 테스트는 값을 바로 읽으려고 부르는 쪽에서 셉니다.
 */
class ReportViewModel(
    private val matchRepository: MatchRepository,
    accountRepository: AccountRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val friendRepository: FriendRepository,
    contentRepository: ContentRepository,
    private val clock: Clock,
    val timeZone: TimeZone,
    weekChanges: Flow<Unit> = flowOf(Unit),
    computation: CoroutineContext = Dispatchers.Default,
    pingRepository: PingRepository? = null,
) : ViewModel() {

    /** 홈 맨 위에 띄울 ㅇㅂㅇ입니다. 리포트 계산과 따로 둬서 답이 바뀔 때 리포트를 다시 세지 않습니다. */
    val homePing: StateFlow<HomePing?> = (pingRepository?.pings ?: flowOf(emptyList()))
        .combine(accountRepository.account) { pings, account ->
            account?.let { mine -> pings.forHome(mine.id)?.let { HomePing(it, mine.id, clock.now()) } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 칩으로 고르기 전까지는 설정의 기본 큐를 따른다. 고른 칩은 저장하지 않아 앱을 새로 열면 기본 큐로 돌아간다.
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)

    private val refreshing = MutableStateFlow(false)

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    /** 새 경기를 여러 판 받는 중이면 몇 판 중 몇 판을 받았는지입니다. 홈 맨 위 진행 줄로 띄웁니다. 다섯 판보다 적으면 `null`입니다. */
    val newMatches: StateFlow<NewMatchesProgress?> = matchRepository.newMatchesProgress
        .map { progress -> progress?.takeIf { it.isShown } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 홈을 당겨 새 경기를 받는 중인지입니다. 진행 줄이 뜨면 당김 표시는 거둡니다. 둘 다 돌면 같은 일을 두 번 알립니다. */
    val isRefreshing: StateFlow<Boolean> = refreshing

    private val waitingForNewMatches = matchRepository.newMatchesProgress.map { it?.isShown == true }.distinctUntilChanged()

    // 첫 수집이 끝나기 전에는 숫자를 띄우지 않는다. 헤드샷 24%가 잠시 뒤 19%로 바뀌면 유저는 그 뒤로 숫자를 믿지 않는다.
    // 무기 계열은 개선 포인트가 같은 계열의 무기끼리 견줄 때 쓴다.
    private val importedMatches = combine(
        matchRepository.settledMatches(),
        matchRepository.importProgress,
        weekChanges,
        contentRepository.catalog,
        waitingForNewMatches,
    ) { matches, progress, _, catalog, waiting ->
        matches.takeIf { progress == null || progress.isDone }?.let {
            ReportInputs(it, catalog.weapons.mapValues { (_, info) -> info.category }, waiting, needsImport = progress == null && it.isEmpty())
        }
    }

    val uiState: StateFlow<ReportUiState> = combine(
        importedMatches,
        preferencesRepository.preferences,
        selectedQueue,
        friendRepository.friends,
        friendRepository.rival,
    ) { inputs, preferences, selected, friends, rivalId ->
        if (inputs == null) return@combine ReportUiState.Loading
        val filter = selected ?: preferences.defaultQueue
        val report = inputs.matches.weeklyReport(
            now = clock.now(),
            timeZone = timeZone,
            queueFilter = filter,
            focus = preferences.focus,
            weaponCategories = inputs.weaponCategories,
        )
        val standings = if (report is WeeklyReport.Ready) {
            friends
                .filter { it.statsPublic }
                .map { FriendStanding(it.id, it.riotId, it.metricsIn(report, filter, timeZone)) }
        } else {
            emptyList()
        }
        val rival = standings.firstOrNull { it.id == rivalId }
        // 이번 주를 보고 있었으면 새 경기도 이번 주라 기간은 그대로다. 숫자만 다 받은 뒤 바뀐다.
        val periodMayChange = !(report is WeeklyReport.Ready && report.period.includesThisWeek)
        ReportUiState.Success(
            queueFilter = filter,
            report = report,
            rival = rival,
            friends = standings,
            nudge = homeNudge(
                report,
                filter,
                hasFriends = friends.isNotEmpty(),
                rivalCandidates = standings,
                rival = rival,
                hasRival = rivalId != null,
            ),
            waitingForNewMatches = inputs.waitingForNewMatches && periodMayChange,
            needsImport = inputs.needsImport,
        )
    }.flowOn(computation).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState.Loading,
    )

    /**
     * 오른쪽 위에 "내 프로필은 여기서 볼 수 있어요"를 띄울지입니다. 한 번 띄우면 [markProfileHintSeen]으로 적어 두고 다시 띄우지
     * 않습니다. 오른쪽 위 아바타 하나로는 내 프로필을 찾기 어려웠습니다(사용자 요청, 2026-10-03).
     */
    val profileHint: StateFlow<Boolean> = preferencesRepository.preferences
        .map { !it.seenProfileHint }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun markProfileHintSeen() {
        // 적지 못하면 다음에 한 번 더 뜰 뿐이라 안내하지 않는다
        viewModelScope.launch { runCatching { preferencesRepository.setSeenProfileHint() } }
    }

    /** 요원과 무기 이름을 찾는 카탈로그입니다. 받기 전에는 비어 있어 "알 수 없는 요원", "알 수 없는 무기"가 뜹니다. */
    val catalog: StateFlow<ContentCatalog> =
        contentRepository.catalog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = ContentCatalog.Empty)

    /** 오른쪽 위 티어와 아바타입니다. 연동을 해제했으면 `null`입니다. */
    val badge: StateFlow<PlayerBadge?> = combine(
        accountRepository.account,
        matchRepository.observeMatches(),
        contentRepository.catalog,
    ) { account, matches, catalog ->
        account?.let { playerBadge(it.riotId, matches, catalog) }
    }.flowOn(computation).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun selectQueue(filter: QueueFilter) {
        selectedQueue.value = filter
    }

    /**
     * 새로 끝난 경기를 받습니다. 받는 중에 또 당기면 무시합니다. 실패해도 저장해 둔 경기는 그대로 둡니다. 진행 줄이 떠 있으면
     * 이미 받는 중이라 당겨도 새로 받지 않습니다.
     */
    fun refresh() {
        if (refreshing.value || newMatches.value != null) return
        refreshing.value = true
        // 받지 못해도 저장해 둔 경기와 그 숫자는 그대로 두고 안내만 띄운다
        viewModelScope.launchNotifying(failures, FailedAction.REFRESH) {
            val untilLineShows = launch {
                matchRepository.newMatchesProgress.first { it?.isShown == true }
                refreshing.value = false
            }
            try {
                matchRepository.refresh()
            } finally {
                untilLineShows.cancel()
                refreshing.value = false
            }
        }
    }

    /** 유도 칸에서 고른 친구를 라이벌로 정합니다. S5의 라이벌 지정과 같은 값을 바꿉니다. */
    fun selectRival(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.RIVAL) { friendRepository.setRival(id) }
    }
}

private class ReportInputs(
    val matches: List<Match>,
    val weaponCategories: Map<WeaponId, WeaponCategory>,
    val waitingForNewMatches: Boolean,
    val needsImport: Boolean,
)
