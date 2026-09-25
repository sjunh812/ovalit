package com.ovalit.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Friend
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.SharedRecord
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.latestAgent
import com.ovalit.core.model.latestTier
import com.ovalit.core.model.metricsIn
import com.ovalit.core.model.sharedWith
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.ui.PlayerBadge
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface FriendProfileUiState {
    data object Loading : FriendProfileUiState

    /** 친구를 끊었거나 목록에 없습니다. */
    data object Gone : FriendProfileUiState

    /**
     * @property theirReport 친구 본인의 주간 리포트입니다. 전적 비공개면 `null`입니다.
     * @property theirMetricsInMyPeriod "나와 비교"에 쓰는 값입니다. 내 리포트와 같은 기간으로 셉니다.
     */
    data class Success(
        val friend: Friend,
        val badge: PlayerBadge,
        val catalog: ContentCatalog,
        val isRival: Boolean,
        val shared: SharedRecord,
        val theirReport: WeeklyReport?,
        val myReport: WeeklyReport,
        val theirMetricsInMyPeriod: MatchMetrics?,
        val now: Instant,
        val timeZone: TimeZone,
    ) : FriendProfileUiState
}

class FriendProfileViewModel(
    private val friendId: PlayerId,
    private val friendRepository: FriendRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    val uiState: StateFlow<FriendProfileUiState> = combine(
        friendRepository.friends,
        friendRepository.rival,
        matchRepository.observeMatches(),
        contentRepository.catalog,
    ) { friends, rival, myMatches, catalog ->
        val friend = friends.firstOrNull { it.id == friendId } ?: return@combine FriendProfileUiState.Gone
        val myReport = myMatches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = QUEUE)
        val tier = friend.matches.latestTier()
        FriendProfileUiState.Success(
            friend = friend,
            badge = PlayerBadge(friend.riotId, friend.matches.latestAgent(), tier, tier?.let { catalog.tiers[it] }),
            catalog = catalog,
            isRival = rival == friendId,
            shared = myMatches.sharedWith(friendId),
            theirReport = friend.takeIf { it.statsPublic }?.matches
                ?.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = QUEUE),
            myReport = myReport,
            theirMetricsInMyPeriod = (myReport as? WeeklyReport.Ready)?.let { friend.metricsIn(it, QUEUE, timeZone) },
            now = clock.now(),
            timeZone = timeZone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FriendProfileUiState.Loading,
    )

    fun toggleRival() {
        viewModelScope.launch {
            val isRival = friendRepository.rival.first() == friendId
            friendRepository.setRival(if (isRival) null else friendId)
        }
    }

    /** [onDone]은 끊은 뒤에 부릅니다. 화면을 먼저 닫으면 방금 끊은 친구가 목록에 잠깐 남습니다. */
    fun unfriend(onDone: () -> Unit) {
        viewModelScope.launch {
            friendRepository.unfriend(friendId)
            onDone()
        }
    }
}
