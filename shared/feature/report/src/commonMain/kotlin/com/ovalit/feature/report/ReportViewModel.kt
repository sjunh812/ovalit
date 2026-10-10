package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.countNewMatches
import com.ovalit.core.data.logRefresh
import com.ovalit.core.data.settledMatches
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Focus
import com.ovalit.core.model.Friend
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
import com.ovalit.core.ui.RefreshResults
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
     * @property rival 고른 라이벌입니다. 고르지 않았거나 라이벌이 전적을 공개하지 않았거나 리포트가 없으면 `null`입니다.
     * @property friends 전적을 공개한 친구 전부입니다. 리포트 기간에 경기가 없는 친구도 들어 있고, 라이벌은 이 안에서
     * 고릅니다. 순서는 [standingsIn]을 따릅니다. 리포트가 없으면 빈 목록입니다.
     * @property nudge 라이벌 칸 자리에 두는 유도 칸입니다([homeNudge]).
     * @property waitingForNewMatches 새 경기를 여러 판 받는 중이라 [report]의 기간이 바뀔 수 있는지입니다. 그동안 리포트
     * 자리에 스켈레톤을 둡니다. "지난주"를 보던 사람에게 받기 전 숫자를 띄우면 틀린 말이 됩니다.
     * @property needsImport 설정에서 저장된 데이터를 지워 기기에 경기가 없는지입니다. 그러면 리포트 대신 다시 불러오기를
     * 권합니다. 안 뛴 게 아니라 지운 거라 "최근 4주 동안 뛴 경기가 없어요"를 띄우면 안 됩니다.
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
 * 홈에 둘 유도 칸을 고릅니다. 리포트, 친구, 라이벌 순서로 봅니다. 리포트가 없으면 경기 수 안내가 먼저라 아무것도 권하지
 * 않고, 친구가 없으면 초대부터 권합니다. 기타 모드에는 친구 칸이 없어 권하지 않습니다.
 *
 * @param hasFriends 전적 공개와 상관없이 친구가 한 명이라도 있는지입니다.
 * @param rivalCandidates 라이벌로 고를 수 있는 친구입니다. 전적을 공개한 친구뿐이라, 모두 비공개면 비어 있고 라이벌을
 * 권하지 않습니다.
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

/**
 * @property metrics 내 리포트와 같은 기간의 합계입니다. 그 기간에 경기가 없으면 `null`입니다.
 * @property lastPlayedTogether [Friend.lastPlayedTogether]입니다.
 */
data class FriendStanding(
    val id: PlayerId,
    val riotId: String,
    val metrics: MatchMetrics?,
    val lastPlayedTogether: Instant? = null,
)

/**
 * 전적을 공개한 친구마다 내 리포트와 같은 기간의 합계를 셉니다. 최근에 같이 뛴 친구부터 세우고, 같이 뛴 기록이 없으면 그 기간
 * 경기가 많은 친구부터 둡니다. 라이벌 고르기 시트와 유도 칸의 얼굴이 이 순서를 씁니다.
 */
internal fun List<Friend>.standingsIn(report: WeeklyReport.Ready, queueFilter: QueueFilter, timeZone: TimeZone): List<FriendStanding> =
    filter { it.statsPublic }
        .map { FriendStanding(it.id, it.riotId, it.metricsIn(report, queueFilter, timeZone), it.lastPlayedTogether) }
        .sortedWith(compareByDescending<FriendStanding> { it.lastPlayedTogether }.thenByDescending { it.metrics?.matches ?: 0 })

/**
 * @param weekChanges 흐를 때마다 리포트를 다시 셉니다. 앱은 [weekStarts]를 넘겨 월요일 0시에 "이번 주"를 바꾸고,
 * 테스트는 시계를 멈춰 두니 한 번만 흐르는 기본값을 씁니다.
 * @param computation 리포트를 세는 곳입니다. 메인 스레드에서 세면 첫 수집 뒤 홈으로 넘어가는 전환이 멈춰서 기본이
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
    private val pingRepository: PingRepository? = null,
    private val analytics: Analytics = NoAnalytics,
    minuteChanges: Flow<Unit> = flowOf(Unit),
) : ViewModel() {

    /** 홈 맨 위에 띄울 ㅇㅂㅇ입니다. 리포트 계산과 따로 둬서 답이 바뀔 때 리포트를 다시 세지 않습니다. */
    // 분마다 다시 내보낸다. 자정을 넘기면 "내일 09:00"이 "09:00"이 되어야 한다.
    val homePing: StateFlow<HomePing?> = combine(
        pingRepository?.pings ?: flowOf(emptyList()),
        accountRepository.account,
        minuteChanges,
    ) { pings, account, _ ->
        account?.let { mine -> pings.forHome(mine.id)?.let { HomePing(it, mine.id, clock.now()) } }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 칩으로 고르기 전까지는 설정의 기본 큐를 따른다. 고른 칩은 저장하지 않아 앱을 새로 열면 기본 큐로 돌아간다.
    private val selectedQueue = MutableStateFlow<QueueFilter?>(null)

    private val refreshing = MutableStateFlow(false)

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    private val refreshResults = RefreshResults()

    /** 당겨서 새 경기를 다 받으면 몇 판을 받았는지입니다. 화면 아래에 한 줄 띄웁니다. 받는 동안 진행 줄이 떴어도 다 받으면 보냅니다. */
    val refreshed: Flow<Int> = refreshResults.flow

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
        // 리포트를 세는 데 쓰는 두 값만 본다. 테마나 광고 끝 시각이 바뀔 때마다 리포트를 처음부터 다시 세지 않는다.
        preferencesRepository.preferences.map { ReportPreferences(it.defaultQueue, it.focus) }.distinctUntilChanged(),
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
        val standings = if (report is WeeklyReport.Ready) friends.standingsIn(report, filter, timeZone) else emptyList()
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
     * 오른쪽 위에 "내 프로필은 여기서 볼 수 있어요"를 띄울지입니다. 한 번 띄우면 [markProfileHintSeen]으로 적어 두고 다시
     * 띄우지 않습니다.
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
     * 새로 끝난 경기를 받습니다. 당김 표시나 진행 줄이 떠 있으면 이미 받는 중이라 무시합니다. 실패하면 저장해 둔 경기는
     * 그대로 두고 안내만 띄웁니다. 홈 맨 위 ㅇㅂㅇ도 같이 다시 받습니다. 당겼는데 그사이 취소된 초대가 남아 있으면 안 됩니다.
     */
    fun refresh() {
        // ㅇㅂㅇ은 곁다리라 받지 못해도 알리지 않는다. 친구 탭을 당기면 실패를 알린다. 새 경기를 받는 중에 당겨도 ㅇㅂㅇ은 받는다.
        viewModelScope.launch { runCatching { pingRepository?.refresh() } }
        if (refreshing.value || newMatches.value != null) return
        refreshing.value = true
        viewModelScope.launchNotifying(failures, FailedAction.REFRESH) {
            val untilLineShows = launch {
                matchRepository.newMatchesProgress.first { it?.isShown == true }
                refreshing.value = false
            }
            val received = try {
                matchRepository.countNewMatches { logRefresh(analytics, source = "home") { matchRepository.refresh() } }
            } finally {
                untilLineShows.cancel()
                refreshing.value = false
            }
            received?.let(refreshResults::send)
        }
    }

    /** 유도 칸에서 고른 친구를 라이벌로 정합니다. S5의 라이벌 지정과 같은 값을 바꿉니다. */
    fun selectRival(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.RIVAL) { friendRepository.setRival(id) }
    }
}

private data class ReportPreferences(val defaultQueue: QueueFilter, val focus: Focus)

private class ReportInputs(
    val matches: List<Match>,
    val weaponCategories: Map<WeaponId, WeaponCategory>,
    val waitingForNewMatches: Boolean,
    val needsImport: Boolean,
)
