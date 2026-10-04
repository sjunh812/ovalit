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
import com.ovalit.core.model.pingSlots
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.launchNotifying
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface FriendsUiState {
    data object Loading : FriendsUiState

    /**
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

class FriendsViewModel(
    private val friendRepository: FriendRepository,
    private val pingRepository: PingRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
    val timeZone: TimeZone,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    /** 친구 탭을 당겨 새로 받는 중인지입니다. */
    val isRefreshing: StateFlow<Boolean> = refreshing

    val uiState: StateFlow<FriendsUiState> = combine(
        friendRepository.friends,
        friendRepository.requests,
        friendRepository.rival,
        pingRepository.pings,
        accountRepository.account,
    ) { friends, requests, rival, pings, account ->
        FriendsUiState.Success(
            requests = requests,
            friends = friends.map { friend ->
                FriendRow(
                    friend = friend,
                    report = friend.takeIf { it.statsPublic }?.matches
                        ?.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = QUEUE),
                )
            },
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

    fun accept(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.ACCEPT_FRIEND) {
            friendRepository.accept(id)
            analytics.log(AnalyticsEvents.FRIEND_REQUEST, mapOf("action" to "accept", "source" to "friends_tab"))
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

    fun now(): Instant = clock.now()

    /** 보냈거나 정해 둔 까닭으로 막혔으면 [onResult]를 부릅니다. 보내다 실패하면 부르지 않고 안내를 띄워 시트를 그대로 둡니다. */
    fun sendPing(friends: List<PlayerId>, startsAt: Instant, onResult: (PingSendResult) -> Unit) {
        viewModelScope.launchNotifying(failures, FailedAction.PING_SEND) {
            val result = pingRepository.send(friends, startsAt)
            if (result == PingSendResult.SENT) analytics.log(AnalyticsEvents.PING_SEND, mapOf("friend_count" to friends.size.toString()))
            // 보낸 것이 끝나기 전에는 "부르기"를 두지 않으니, 이 결과는 다른 기기에서 그사이 보낸 경우다
            if (result == PingSendResult.ALREADY_ACTIVE) failures.send(FailureNotice(FailedAction.PING_ALREADY_ACTIVE))
            onResult(result)
        }
    }

}

internal val QUEUE = QueueFilter.COMPETITIVE_AND_UNRATED
