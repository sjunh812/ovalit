package com.ovalit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.settledMatches
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.agentReport
import com.ovalit.core.model.currentActMatches
import com.ovalit.core.model.weaponReport
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

/** S6 무기와 S7 요원이 누구의 기록을 보는지입니다. */
sealed interface RecordsOwner {
    data object Me : RecordsOwner

    /** 서로 수락한 친구입니다. 친구 경기는 기기에 저장된 것을 그대로 써서 새로 요청하지 않습니다. */
    data class Friend(val id: PlayerId) : RecordsOwner
}

sealed interface RecordsUiState {
    data object Loading : RecordsUiState

    /** 친구를 끊었거나 친구가 전적을 비공개로 바꿨을 때입니다. */
    data object Hidden : RecordsUiState

    /** @property ownerName 친구 기록일 때 제목에 붙이는 이름입니다. 내 기록이면 `null`입니다. */
    data class Success(
        val ownerName: String?,
        val agents: AgentReport,
        val weapons: WeaponReport,
        val catalog: ContentCatalog,
    ) : RecordsUiState
}

/**
 * S6 무기와 S7 요원이 같이 씁니다. 내 기록이든 친구 기록이든 이번 액트의 경쟁 + 일반 경기만 셉니다.
 *
 * @param computation 경기를 모아 세는 곳입니다. 메인 스레드에서 세면 화면이 밀려 들어오는 동안 멈춰서 기본은
 * [Dispatchers.Default]입니다.
 */
class RecordsViewModel(
    owner: RecordsOwner,
    matchRepository: MatchRepository,
    friendRepository: FriendRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
    computation: CoroutineContext = Dispatchers.Default,
) : ViewModel() {

    // 제목에 붙일 이름과 셀 경기다. 내 기록이면 이름이 null이고, 친구를 끊었거나 친구가 전적을 비공개로 바꿨으면 통째로
    // null이다.
    private val source: Flow<Pair<String?, List<Match>>?> = when (owner) {
        RecordsOwner.Me -> matchRepository.settledMatches().map { null to it }
        is RecordsOwner.Friend -> friendRepository.friends.map { friends ->
            friends.firstOrNull { it.id == owner.id }
                ?.takeIf { it.statsPublic }
                ?.let { it.riotId.substringBefore('#') to it.matches }
        }
    }

    val uiState: StateFlow<RecordsUiState> = combine(source, contentRepository.catalog) { source, catalog ->
        val (ownerName, matches) = source ?: return@combine RecordsUiState.Hidden
        RecordsUiState.Success(
            ownerName = ownerName,
            agents = matches.currentActMatches(QueueFilter.PROFILE).agentReport(),
            weapons = matches.weaponReport(now = clock.now(), timeZone = timeZone, queueFilter = QueueFilter.PROFILE),
            catalog = catalog,
        )
    }.flowOn(computation).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState.Loading,
    )
}
