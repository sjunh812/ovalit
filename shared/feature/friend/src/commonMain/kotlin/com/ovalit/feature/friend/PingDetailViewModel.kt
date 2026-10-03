package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.PingRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.pingSlots
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface PingDetailUiState {
    data object Loading : PingDetailUiState

    /** 부른 사람이 취소했거나 끝나서 더는 없는 초대입니다. 알림을 늦게 누르면 이렇게 들어옵니다. */
    data object Gone : PingDetailUiState

    /**
     * @property now 시각 글자("21:00", "새벽 01:00")를 정할 때 쓰는 지금입니다.
     * @property invitable 내가 보낸 것이면 더 부를 수 있는 친구입니다. 이미 부른 친구는 빠집니다.
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
) : ViewModel() {

    val uiState: StateFlow<PingDetailUiState> = combine(
        pingRepository.pings,
        friendRepository.friends,
        accountRepository.account,
    ) { pings, friends, account ->
        val ping = pings.firstOrNull { it.id == pingId }
        if (account == null || ping == null) {
            PingDetailUiState.Gone
        } else {
            PingDetailUiState.Success(
                ping = ping,
                me = account.id,
                now = clock.now(),
                invitable = if (ping.isHostedBy(account.id)) friends.filter { ping.memberOf(it.id) == null } else emptyList(),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PingDetailUiState.Loading,
    )

    /** 지금부터 고를 수 있는 시각입니다. 시트를 열 때마다 다시 셉니다. */
    fun slots(): List<Instant> = pingSlots(clock.now(), timeZone)

    fun reply(answer: PingAnswer, proposedAt: Instant? = null) {
        viewModelScope.launch { pingRepository.reply(pingId, answer, proposedAt) }
    }

    fun moveTo(startsAt: Instant) {
        viewModelScope.launch { pingRepository.moveTo(pingId, startsAt) }
    }

    fun invite(friends: List<PlayerId>) {
        viewModelScope.launch { pingRepository.invite(pingId, friends) }
    }

    fun cancel() {
        viewModelScope.launch { pingRepository.cancel(pingId) }
    }
}
