package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.data.PingInviteResult
import com.ovalit.core.data.PingRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.pingSlots
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.InFlight
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface PingDetailUiState {
    data object Loading : PingDetailUiState

    /** 부른 사람이 취소했거나 끝나서 더는 없는 초대입니다. 알림을 늦게 누르면 이렇게 들어옵니다. */
    data object Gone : PingDetailUiState

    /**
     * @property now 시각 글자("21:00", "새벽 01:00")를 정할 때 쓰는 지금입니다.
     * @property invitable 내가 보낸 것이면 더 부를 수 있는 친구입니다.
     *   이미 부른 친구는 빠집니다.
     */
    data class Success(
        val ping: Ping,
        val me: PlayerId,
        val now: Instant,
        val invitable: List<Friend> = emptyList(),
    ) : PingDetailUiState
}

/** 초대 하나의 화면입니다. 받은 초대면 답하고, 보낸 초대면 시각을 옮기거나 취소합니다. */
class PingDetailViewModel(
    private val pingId: PingId,
    private val pingRepository: PingRepository,
    friendRepository: FriendRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
    val timeZone: TimeZone,
    private val analytics: Analytics = NoAnalytics,
    minuteChanges: Flow<Unit> = flowOf(Unit),
) : ViewModel() {

    private val inFlight = InFlight()

    /** 보내는 중인 동작입니다. 화면은 그 버튼을 흐리게 둡니다. */
    val busy: StateFlow<Set<Any>> = inFlight.running

    private val sending = MutableStateFlow<PingAnswer?>(null)

    /**
     * 보내는 중인 내 답입니다.
     * 화면은 누르는 즉시 이 답으로 버튼 줄을 접고, 보내지 못하면 `null`로 돌아와 버튼 줄을 다시 펼칩니다.
     */
    val sendingAnswer: StateFlow<PingAnswer?> = sending.asStateFlow()

    // 처음 받기를 마쳤는지와 내가 취소해 닫히는 중인지다
    private val phase = MutableStateFlow(Phase())

    // 분마다 다시 내보낸다.
    // "31분 뒤"가 줄고, 시작 시각이 지나면 "시작했어요"로 바뀌어야 한다.
    val uiState: StateFlow<PingDetailUiState> = combine(
        pingRepository.pings,
        friendRepository.friends,
        accountRepository.account,
        minuteChanges,
        phase,
    ) { pings, friends, account, _, phase ->
        val ping = pings.firstOrNull { it.id == pingId }
        when {
            account != null && ping != null -> PingDetailUiState.Success(
                ping = ping,
                me = account.id,
                now = clock.now(),
                invitable = if (ping.isHostedBy(account.id)) friends.filter { ping.memberOf(it.id) == null } else emptyList(),
            ).also { lastShown = it }
            // 내가 취소해 닫히는 동안에는 밀려나는 화면에 "끝났거나 취소된 초대예요"를 띄우지 않고 마지막 모습을 둔다
            phase.closing -> lastShown ?: PingDetailUiState.Gone
            // 기기에 없는 초대(알림을 눌러 들어온 새 초대)는 처음 받기가 끝날 때까지 기다린다
            account != null && !phase.loaded -> PingDetailUiState.Loading
            else -> PingDetailUiState.Gone
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PingDetailUiState.Loading,
    )

    private var lastShown: PingDetailUiState.Success? = null

    init {
        // 알림을 눌러 들어오면 푸시보다 서버가 앞서 있을 수 있어 열 때 한 번 받는다.
        // 저절로 한 일이라 받지 못해도 알리지 않는다.
        viewModelScope.launch {
            runCatching { pingRepository.refresh() }
            phase.update { it.copy(loaded = true) }
        }
    }

    /** 지금부터 고를 수 있는 시각입니다. 시트를 열 때마다 다시 셉니다. */
    fun slots(): List<Instant> = pingSlots(clock.now(), timeZone)

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    fun reply(answer: PingAnswer, proposedAt: Instant? = null) {
        inFlight.launch(viewModelScope, Action.REPLY, failures, FailedAction.PING_REPLY, onFailure = { sending.value = null }) {
            sending.value = answer
            pingRepository.reply(pingId, answer, proposedAt)
            sending.value = null
            analytics.log(AnalyticsEvents.PING_REPLY, mapOf("answer" to answer.name.lowercase(), "via" to "app"))
        }
    }

    fun moveTo(startsAt: Instant) {
        inFlight.launch(viewModelScope, Action.MOVE, failures, FailedAction.PING_TIME) { pingRepository.moveTo(pingId, startsAt) }
    }

    fun invite(friends: List<PlayerId>) {
        inFlight.launch(viewModelScope, Action.INVITE, failures, FailedAction.PING_INVITE) {
            // 그사이 다른 기기에서 더 불러 자리가 찼으면 아무도 더하지 않는다
            if (pingRepository.invite(pingId, friends) == PingInviteResult.FULL) failures.send(FailureNotice(FailedAction.PING_FULL))
        }
    }

    /** 취소하면 [onCancelled]를 부릅니다. 실패하면 부르지 않고 안내만 띄워 화면을 그대로 둡니다. */
    fun cancel(onCancelled: () -> Unit = {}) {
        inFlight.launch(viewModelScope, Action.CANCEL, failures, FailedAction.PING_CANCEL, onFailure = { phase.update { it.copy(closing = false) } }) {
            phase.update { it.copy(closing = true) }
            pingRepository.cancel(pingId)
            onCancelled()
        }
    }

    private data class Phase(val loaded: Boolean = false, val closing: Boolean = false)

    /** 화면이 흐리게 둘 버튼을 가르는 키입니다. */
    enum class Action { REPLY, MOVE, INVITE, CANCEL }
}
