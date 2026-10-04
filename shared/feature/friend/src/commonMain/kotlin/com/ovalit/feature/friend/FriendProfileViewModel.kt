package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Friend
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.ProfileSummary
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.SharedRecord
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.agentReport
import com.ovalit.core.model.currentActMatches
import com.ovalit.core.model.latestTier
import com.ovalit.core.model.metricsIn
import com.ovalit.core.model.profileSummary
import com.ovalit.core.model.sharedWith
import com.ovalit.core.model.weaponReport
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.launchNotifying
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * 친구의 이번 액트 경쟁 + 일반 경기 성적입니다. 내 프로필과 같은 기준으로 셉니다.
 *
 * @property weapons 위쪽 세 무기의 이번 액트 값만 씁니다. 누르면 여는 S6은 RecordsViewModel이 기기에 저장된 같은
 * 경기로 다시 셉니다.
 */
data class FriendProfile(
    val summary: ProfileSummary,
    val agents: AgentReport,
    val weapons: WeaponReport,
)

sealed interface FriendProfileUiState {
    data object Loading : FriendProfileUiState

    /** 친구 목록에 없을 때입니다. 내가 끊었든 상대가 끊었든 같습니다. */
    data object Gone : FriendProfileUiState

    /**
     * @property theirProfile 내 프로필과 같은 칸에 넣는 친구의 이번 액트 성적입니다. 전적 비공개면 `null`입니다.
     * @property theirMetricsInMyPeriod "나와 비교"에 쓰는 값입니다. 내 리포트와 같은 기간으로 셉니다. 내 리포트가 없거나,
     * 친구가 전적을 공개하지 않았거나, 그 기간에 친구 경기가 없으면 `null`입니다.
     */
    data class Success(
        val friend: Friend,
        val badge: PlayerBadge,
        val catalog: ContentCatalog,
        val isRival: Boolean,
        val shared: SharedRecord,
        val theirProfile: FriendProfile?,
        val myReport: WeeklyReport,
        val theirMetricsInMyPeriod: MatchMetrics?,
        val now: Instant,
        val timeZone: TimeZone,
    ) : FriendProfileUiState
}

/**
 * S5 친구 프로필입니다.
 *
 * @param computation 경기를 세는 디스패처입니다. 메인 스레드에서 세면 화면 전환이 멈춰서 기본은 [Dispatchers.Default]입니다.
 */
class FriendProfileViewModel(
    private val friendId: PlayerId,
    private val friendRepository: FriendRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
    computation: CoroutineContext = Dispatchers.Default,
) : ViewModel() {

    val uiState: StateFlow<FriendProfileUiState> = combine(
        friendRepository.friends,
        friendRepository.rival,
        matchRepository.observeMatches(),
        contentRepository.catalog,
    ) { friends, rival, myMatches, catalog ->
        // 전적을 비공개로 바꾼 친구는 기기에 경기가 남아 있어도 경기 목록, 티어, 최근 경기까지 모두 가린다
        val found = friends.firstOrNull { it.id == friendId } ?: return@combine FriendProfileUiState.Gone
        val friend = if (found.statsPublic) found else found.copy(matches = emptyList())
        val myReport = myMatches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = QueueFilter.PROFILE)
        val tier = friend.matches.latestTier()
        FriendProfileUiState.Success(
            friend = friend,
            badge = PlayerBadge(friend.riotId, tier, tier?.let { catalog.tiers[it] }),
            catalog = catalog,
            isRival = rival == friendId,
            shared = myMatches.sharedWith(friendId),
            theirProfile = friend.takeIf { it.statsPublic }?.matches?.let { matches ->
                val actMatches = matches.currentActMatches(QueueFilter.PROFILE)
                FriendProfile(
                    summary = actMatches.profileSummary(),
                    agents = actMatches.agentReport(),
                    weapons = matches.weaponReport(now = clock.now(), timeZone = timeZone, queueFilter = QueueFilter.PROFILE),
                )
            },
            myReport = myReport,
            theirMetricsInMyPeriod = (myReport as? WeeklyReport.Ready)?.let { friend.metricsIn(it, QueueFilter.PROFILE, timeZone) },
            now = clock.now(),
            timeZone = timeZone,
        )
    }.flowOn(computation).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FriendProfileUiState.Loading,
    )

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    fun toggleRival() {
        viewModelScope.launchNotifying(failures, FailedAction.RIVAL) {
            val isRival = friendRepository.rival.first() == friendId
            friendRepository.setRival(if (isRival) null else friendId)
        }
    }

    /** 끊고 나면 상태가 [FriendProfileUiState.Gone]이 되고, 화면은 그걸 보고 닫힙니다. */
    fun unfriend() {
        viewModelScope.launchNotifying(failures, FailedAction.UNFRIEND) { friendRepository.unfriend(friendId) }
    }
}
