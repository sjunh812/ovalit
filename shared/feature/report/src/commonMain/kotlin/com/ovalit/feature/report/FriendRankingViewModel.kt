package com.ovalit.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.settledMatches
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface FriendRankingUiState {
    data object Loading : FriendRankingUiState

    /** 보는 중에 저장된 경기를 지워 리포트가 없어졌거나, 친구 비교가 없는 큐입니다. 화면을 닫습니다. */
    data object Gone : FriendRankingUiState

    /** @property friends 전적을 공개한 친구 전부입니다. 그 기간에 경기가 없는 친구는 줄을 세울 때 빠집니다. */
    data class Success(
        val period: ReportPeriod,
        val mine: MatchMetrics,
        val friends: List<FriendStanding>,
    ) : FriendRankingUiState
}

/**
 * 홈 친구 비교의 "전체 보기"로 여는 전체 순위입니다. 홈과 같은 큐, 같은 기간으로 셉니다. 홈에서 고른 큐 칩은 저장하지 않아서
 * 열 때 [queueFilter]로 받습니다.
 *
 * @param computation 리포트와 친구 합계를 세는 곳입니다. 친구가 수백 명이면 메인 스레드에서 세는 동안 화면이 멈춥니다.
 */
class FriendRankingViewModel(
    queueFilter: QueueFilter,
    matchRepository: MatchRepository,
    friendRepository: FriendRepository,
    clock: Clock,
    timeZone: TimeZone,
    computation: CoroutineContext = Dispatchers.Default,
    weekChanges: Flow<Unit> = flowOf(Unit),
) : ViewModel() {

    // 화면을 켜 둔 채 월요일 0시를 넘기면 홈처럼 기간을 다시 잡는다
    val uiState: StateFlow<FriendRankingUiState> = combine(
        matchRepository.settledMatches(),
        friendRepository.friends,
        weekChanges,
    ) { matches, friends, _ ->
        val report = matches.weeklyReport(now = clock.now(), timeZone = timeZone, queueFilter = queueFilter)
        if (report !is WeeklyReport.Ready || !queueFilter.hasDynamicMetrics) {
            FriendRankingUiState.Gone
        } else {
            FriendRankingUiState.Success(report.period, report.metrics, friends.standingsIn(report, queueFilter, timeZone))
        }
    }.flowOn(computation).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendRankingUiState.Loading)
}
