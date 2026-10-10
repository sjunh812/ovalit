package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.PingSendResult
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.byLastPlayedTogether
import com.ovalit.core.model.pingSlots
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.launchNotifying
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface FriendsUiState {
    data object Loading : FriendsUiState

    /**
     * @property friends 라이벌이 맨 앞이고 그 뒤는 최근에 같이 뛴 친구부터입니다([byLastPlayedTogether]).
     * @property pings 아직 끝나지 않은 ㅇㅂㅇ입니다. 받은 것과 내가 보낸 것이 섞여 있습니다.
     * @property me 내 PUUID입니다. 연동하지 않았으면 `null`이고 ㅇㅂㅇ을 띄우지 않습니다.
     * @property now 시각 글자("21:00", "내일 01:00")를 정할 때 쓰는 지금입니다.
     */
    data class Success(
        val requests: List<FriendRequest>,
        val friends: List<FriendRow>,
        val rivalId: PlayerId?,
        val pings: List<Ping> = emptyList(),
        val me: PlayerId? = null,
        val now: Instant = Instant.DISTANT_PAST,
    ) : FriendsUiState
}

/** @property report 친구 본인의 주간 리포트입니다. 전적을 공개하지 않았으면 `null`입니다. */
data class FriendRow(
    val friend: Friend,
    val report: WeeklyReport?,
)

/**
 * @param computation 친구마다 주간 리포트를 세는 곳입니다.
 *   친구가 수백 명이면 메인 스레드에서 세는 동안 탭이 멈춰서 기본이 [Dispatchers.Default]입니다.
 *   테스트는 값을 바로 읽으려고 부르는 쪽에서 셉니다.
 */
class FriendsViewModel(
    private val friendRepository: FriendRepository,
    private val pingRepository: PingRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
    val timeZone: TimeZone,
    private val analytics: Analytics = NoAnalytics,
    computation: CoroutineContext = Dispatchers.Default,
    minuteChanges: Flow<Unit> = flowOf(Unit),
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = refreshing

    // 친구 목록이 바뀔 때만 센다. ㅇㅂㅇ 답이나 라이벌이 바뀔 때마다 친구 수백 명의 리포트를 다시 세지 않는다.
    private val friendRows = friendRepository.friends
        .map { friends ->
            friends.byLastPlayedTogether().map { friend ->
                FriendRow(
                    friend = friend,
                    report = friend.takeIf { it.statsPublic }?.matches
                        ?.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = QueueFilter.PROFILE),
                )
            }
        }
        .flowOn(computation)

    val uiState: StateFlow<FriendsUiState> = combine(
        friendRows,
        friendRepository.requests,
        friendRepository.rival,
        // 초대 줄의 시각 글자가 분마다 흐르게 지금을 같이 받는다. 친구 리포트는 다시 세지 않는다.
        combine(pingRepository.pings, minuteChanges) { pings, _ -> pings },
        accountRepository.account,
    ) { rows, requests, rival, pings, account ->
        FriendsUiState.Success(
            requests = requests,
            // 라이벌은 가장 자주 보는 친구라 맨 앞에 둔다. 나머지는 이미 최근에 같이 뛴 순서다.
            friends = rows.sortedByDescending { it.friend.id == rival },
            rivalId = rival,
            pings = if (account == null) emptyList() else pings,
            me = account?.id,
            now = clock.now(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FriendsUiState.Loading,
    )

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    /** 수락을 마치면 [onAccepted]를 부릅니다. 실패하면 부르지 않고 안내만 띄웁니다. */
    fun accept(id: PlayerId, onAccepted: () -> Unit = {}) {
        viewModelScope.launchNotifying(failures, FailedAction.ACCEPT_FRIEND) {
            friendRepository.accept(id)
            analytics.log(AnalyticsEvents.FRIEND_REQUEST, mapOf("action" to "accept", "source" to "friends_tab"))
            onAccepted()
        }
    }

    fun decline(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.DECLINE_FRIEND) {
            friendRepository.decline(id)
            analytics.log(AnalyticsEvents.FRIEND_REQUEST, mapOf("action" to "decline", "source" to "friends_tab"))
        }
    }

    fun inviteLink(): String = friendRepository.inviteLink()

    /** 친구, 받은 요청, ㅇㅂㅇ을 같이 다시 받습니다. 받는 중에 또 당기면 무시합니다. */
    fun refresh() {
        if (!refreshing.compareAndSet(expect = false, update = true)) return
        viewModelScope.launchNotifying(failures, FailedAction.FRIENDS_REFRESH) {
            try {
                coroutineScope {
                    launch { friendRepository.refresh() }
                    launch { pingRepository.refresh() }
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    /** 지금부터 고를 수 있는 시각입니다. 시트를 열 때마다 다시 셉니다. */
    fun pingSlots(): List<Instant> = pingSlots(clock.now(), timeZone)

    /**
     * 서버가 답하면 막혔든 보냈든 [onResult]를 부릅니다.
     * 보내다 실패하면 부르지 않고 안내만 띄워 시트를 그대로 둡니다.
     *
     * @param startsAt `null`이면 "지금"이라 보내는 순간의 시각을 씁니다.
     */
    fun sendPing(friends: List<PlayerId>, startsAt: Instant?, onResult: (PingSendResult) -> Unit) {
        viewModelScope.launchNotifying(failures, FailedAction.PING_SEND) {
            val result = pingRepository.send(friends, startsAt ?: clock.now())
            if (result == PingSendResult.SENT) analytics.log(AnalyticsEvents.PING_SEND, mapOf("friend_count" to friends.size.toString()))
            // 보낸 것이 끝나기 전에는 부르기 버튼이 없으니 이 결과는 그사이 다른 기기에서 보낸 경우다
            if (result == PingSendResult.ALREADY_ACTIVE) failures.send(FailureNotice(FailedAction.PING_ALREADY_ACTIVE))
            onResult(result)
        }
    }
}
