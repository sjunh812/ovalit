package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface FriendsUiState {
    data object Loading : FriendsUiState

    data class Success(
        val requests: List<FriendRequest>,
        val friends: List<FriendRow>,
        val rivalId: PlayerId?,
    ) : FriendsUiState
}

/** @property report 친구 본인의 주간 리포트입니다. 전적을 공개하지 않았으면 `null`입니다. */
data class FriendRow(
    val friend: Friend,
    val report: WeeklyReport?,
)

class FriendsViewModel(
    private val friendRepository: FriendRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    val uiState: StateFlow<FriendsUiState> = combine(
        friendRepository.friends,
        friendRepository.requests,
        friendRepository.rival,
    ) { friends, requests, rival ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FriendsUiState.Loading,
    )

    fun accept(id: PlayerId) {
        viewModelScope.launch { friendRepository.accept(id) }
    }

    fun decline(id: PlayerId) {
        viewModelScope.launch { friendRepository.decline(id) }
    }

    fun inviteLink(): String = friendRepository.inviteLink()
}

internal val QUEUE = QueueFilter.COMPETITIVE_AND_UNRATED
