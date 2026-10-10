package com.ovalit.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.model.BuyRecord
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchPlacement
import com.ovalit.core.model.MatchPlayerStats
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.RoundSummary
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.buyRecords
import com.ovalit.core.model.placements
import com.ovalit.core.model.playerStats
import com.ovalit.core.model.roundSummaries
import com.ovalit.core.model.standings
import com.ovalit.core.ui.FailedAction
import com.ovalit.core.ui.FailureNotice
import com.ovalit.core.ui.FailureNotices
import com.ovalit.core.ui.launchNotifying
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState

    /** 저장한 경기를 지워서 그 경기가 더는 없습니다. */
    data object Gone : MatchDetailUiState

    /**
     * @property groups 스코어보드 묶음입니다. 우리 팀이 먼저입니다([scoreboardGroups]).
     * @property rounds 내가 못 뛴 라운드도 들어갑니다.
     */
    data class Success(
        val match: Match,
        val catalog: ContentCatalog,
        val groups: List<ScoreboardGroup>,
        val rounds: List<RoundSummary>,
        val buys: List<BuyRecord>,
        val timeZone: TimeZone,
    ) : MatchDetailUiState {
        val rows: List<ScoreboardRow> get() = groups.flatMap { it.rows }
    }
}

/**
 * 스코어보드 한 묶음입니다.
 *
 * @property rank 건틀릿처럼 작은 팀 여럿이 등수를 다투는 경기에서 이 팀의 등수입니다. 그 밖에는 `null`입니다.
 */
data class ScoreboardGroup(
    val side: ScoreboardSide,
    val rows: List<ScoreboardRow>,
    val rank: Int? = null,
)

enum class ScoreboardSide {
    /** 우리 팀입니다. 건틀릿이면 나와 짝입니다. */
    MY_TEAM,
    ENEMY_TEAM,

    /** 데스매치처럼 모두가 혼자인 경기의 전체 순위입니다. */
    EVERYONE,

    /** 건틀릿처럼 여러 팀이 등수를 다투는 경기의 다른 팀입니다. */
    OTHER_TEAM,
}

/**
 * 경기 모양에 맞춰 스코어보드를 묶습니다. 두 팀 모드는 우리 팀과 상대 팀이고 라운드제면 라운드당 전투점수, 아니면 킬 순입니다.
 * 데스매치는 모두를 등수 순으로 한 묶음에, 건틀릿은 팀마다 등수 순으로 묶습니다. 펼친 줄의 첫 킬과 멀티킬은 라운드 기록으로 세서
 * 라운드제에만 담습니다.
 */
internal fun Match.scoreboardGroups(relation: (PlayerId) -> PlayerRelation): List<ScoreboardGroup> {
    val placements = placements()
    val stats = if (format == MatchFormat.ROUNDS) playerStats() else emptyMap()
    fun row(line: Scoreline) = ScoreboardRow(line, relation(line.player), placements[line.player], stats[line.player])
    // 라운드제는 자리(라운드당 전투점수) 순이다. 합계로 세우면 튕겨서 덜 뛴 사람이 밀린다. 라운드가 없는 팀 모드는 자리가 없어
    // 킬 순이다.
    val order = compareBy<Scoreline> { placements[it.player]?.rank ?: Int.MAX_VALUE }
        .thenByDescending { it.kills }
        .thenBy { it.deaths }
    return when (format) {
        MatchFormat.ROUNDS, MatchFormat.TEAM_POINTS -> listOf(true, false).map { mine ->
            ScoreboardGroup(
                side = if (mine) ScoreboardSide.MY_TEAM else ScoreboardSide.ENEMY_TEAM,
                rows = players.filter { it.onMyTeam == mine }.sortedWith(order).map(::row),
            )
        }
        MatchFormat.FREE_FOR_ALL -> listOf(ScoreboardGroup(ScoreboardSide.EVERYONE, standings().flatMap { it.members }.map(::row)))
        MatchFormat.TEAM_PLACEMENT -> standings().map { team ->
            ScoreboardGroup(
                side = if (team.members.any { it.player == me }) ScoreboardSide.MY_TEAM else ScoreboardSide.OTHER_TEAM,
                rows = team.members.map(::row),
                rank = team.rank,
            )
        }
    }
}

/**
 * @property placement 그 판에서의 자리(MVP, 팀 MVP, 등수)입니다.
 * @property stats 줄을 펼치면 보이는 첫 킬, 첫 데스, 멀티킬입니다.
 */
data class ScoreboardRow(
    val line: Scoreline,
    val relation: PlayerRelation,
    val placement: MatchPlacement? = null,
    val stats: MatchPlayerStats? = null,
)

/**
 * 스코어보드에서 그 사람과 나의 관계입니다. 프로필은 서로 수락한 친구만 열 수 있고, 친구가 아닌 사람을
 * 누르면 친구 요청이나 초대 링크를 권하는 시트가 뜹니다.
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

    /** 앱을 쓰는지 서버에 묻는 중이거나 묻지 못했습니다. 모르는 사람을 [NOT_APP_USER]로 보이지 않으려고 따로 둡니다. */
    UNKNOWN,
}

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailViewModel(
    private val matchId: MatchId,
    private val friendRepository: FriendRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    private val timeZone: TimeZone,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {

    private val match = matchRepository.observeMatches()
        .map { matches -> matches.firstOrNull { it.id == matchId } }
        .distinctUntilChanged()

    // 앱을 쓰는지는 경기를 불러올 때 서버에 한 번 묻는다. 답을 기다리는 동안과 실패했을 때는 `null`로 두고 스코어보드부터 그린다.
    private val appUsers: Flow<Set<PlayerId>?> = match.flatMapLatest { match ->
        if (match == null) {
            flowOf(emptySet())
        } else {
            flow<Set<PlayerId>?> { emit(friendRepository.appUsersAmong(match.players.map { it.player })) }
                .onStart { emit(null) }
                .catch { emit(null) }
        }
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
        MatchDetailUiState.Success(
            match = match,
            catalog = catalog,
            groups = match.scoreboardGroups { relations.of(it, me = match.me) },
            rounds = match.roundSummaries(),
            buys = match.buyRecords(),
            timeZone = timeZone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchDetailUiState.Loading,
    )

    private val failures = FailureNotices()

    /** 사용자가 한 일이 실패했을 때 화면 아래에 띄울 안내입니다. */
    val notices: Flow<FailureNotice> = failures.flow

    fun sendRequest(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.FRIEND_REQUEST) {
            friendRepository.sendRequest(id)
            analytics.log(AnalyticsEvents.FRIEND_REQUEST, mapOf("action" to "send", "source" to "scoreboard"))
        }
    }

    fun accept(id: PlayerId) {
        viewModelScope.launchNotifying(failures, FailedAction.ACCEPT_FRIEND) {
            friendRepository.accept(id)
            analytics.log(AnalyticsEvents.FRIEND_REQUEST, mapOf("action" to "accept", "source" to "scoreboard"))
        }
    }
}

private class Relations(
    val friends: Set<PlayerId>,
    val requestedMe: Set<PlayerId>,
    val sent: Set<PlayerId>,
    val appUsers: Set<PlayerId>?,
) {
    fun of(player: PlayerId, me: PlayerId): PlayerRelation = when (player) {
        me -> PlayerRelation.ME
        in friends -> PlayerRelation.FRIEND
        in requestedMe -> PlayerRelation.REQUESTED_ME
        in sent -> PlayerRelation.REQUEST_SENT
        else -> when {
            appUsers == null -> PlayerRelation.UNKNOWN
            player in appUsers -> PlayerRelation.APP_USER
            else -> PlayerRelation.NOT_APP_USER
        }
    }
}
