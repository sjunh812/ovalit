package com.ovalit.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.BuyRecord
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.RoundSummary
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.buyRecords
import com.ovalit.core.model.roundSummaries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState

    /** 저장한 경기를 지워서 그 경기가 더는 없습니다. */
    data object Gone : MatchDetailUiState

    /**
     * @property myTeam 전투 점수 순입니다. 게임 스코어보드와 같은 순서입니다.
     * @property rounds 내가 못 뛴 라운드도 들어갑니다.
     */
    data class Success(
        val match: Match,
        val catalog: ContentCatalog,
        val myTeam: List<ScoreboardRow>,
        val enemyTeam: List<ScoreboardRow>,
        val rounds: List<RoundSummary>,
        val buys: List<BuyRecord>,
        val timeZone: TimeZone,
    ) : MatchDetailUiState
}

data class ScoreboardRow(val line: Scoreline, val relation: PlayerRelation)

/**
 * 스코어보드에서 그 사람과 나의 관계입니다. 프로필은 서로 수락한 친구만 열 수 있고, 나머지는 눌러도
 * 친구 요청이나 초대 링크를 권하는 시트가 뜹니다.
 */
enum class PlayerRelation {
    ME,
    FRIEND,

    /** 그 사람이 먼저 나에게 요청을 보냈습니다. */
    REQUESTED_ME,

    /** 내가 요청을 보내 놓았습니다. */
    REQUEST_SENT,

    /** 앱을 쓰지만 아직 아무 요청도 없습니다. */
    APP_USER,

    /** 앱을 쓰지 않아서 요청을 받을 수 없습니다. */
    NOT_APP_USER,
}

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailViewModel(
    private val matchId: MatchId,
    private val friendRepository: FriendRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    private val timeZone: TimeZone,
) : ViewModel() {

    private val match = matchRepository.observeMatches()
        .map { matches -> matches.firstOrNull { it.id == matchId } }
        .distinctUntilChanged()

    // 앱을 쓰는지는 서버에 물어야 알 수 있다. 그래서 보고 있는 경기가 바뀔 때만 다시 묻는다.
    private val appUsers = match.flatMapLatest { match ->
        if (match == null) flowOf(emptySet()) else flow { emit(friendRepository.appUsersAmong(match.players.map { it.player })) }
    }

    private val relations = combine(
        friendRepository.friends,
        friendRepository.requests,
        friendRepository.sentRequests,
        appUsers,
    ) { friends, requests, sent, users ->
        Relations(
            friends = friends.map { it.id }.toSet(),
            requestedMe = requests.map { it.id }.toSet(),
            sent = sent,
            appUsers = users,
        )
    }

    val uiState: StateFlow<MatchDetailUiState> = combine(match, relations, contentRepository.catalog) { match, relations, catalog ->
        if (match == null) return@combine MatchDetailUiState.Gone
        fun rows(onMyTeam: Boolean) = match.players
            .filter { it.onMyTeam == onMyTeam }
            .sortedByDescending { it.combatScore }
            .map { ScoreboardRow(it, relations.of(it.player, me = match.me)) }
        MatchDetailUiState.Success(
            match = match,
            catalog = catalog,
            myTeam = rows(onMyTeam = true),
            enemyTeam = rows(onMyTeam = false),
            rounds = match.roundSummaries(),
            buys = match.buyRecords(),
            timeZone = timeZone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchDetailUiState.Loading,
    )

    fun sendRequest(id: PlayerId) {
        viewModelScope.launch { friendRepository.sendRequest(id) }
    }

    fun accept(id: PlayerId) {
        viewModelScope.launch { friendRepository.accept(id) }
    }
}

private class Relations(
    val friends: Set<PlayerId>,
    val requestedMe: Set<PlayerId>,
    val sent: Set<PlayerId>,
    val appUsers: Set<PlayerId>,
) {
    fun of(player: PlayerId, me: PlayerId): PlayerRelation = when (player) {
        me -> PlayerRelation.ME
        in friends -> PlayerRelation.FRIEND
        in requestedMe -> PlayerRelation.REQUESTED_ME
        in sent -> PlayerRelation.REQUEST_SENT
        in appUsers -> PlayerRelation.APP_USER
        else -> PlayerRelation.NOT_APP_USER
    }
}
