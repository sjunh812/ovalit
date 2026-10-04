package com.ovalit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.settledMatches
import com.ovalit.core.model.Account
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.ProfileSummary
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.agentReport
import com.ovalit.core.model.currentActMatches
import com.ovalit.core.model.profileSummary
import com.ovalit.core.model.weaponReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.playerBadge
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /**
     * @property recentMatches 큐와 액트를 가리지 않은 가장 최근 경기입니다. 내 경기라 누르면 S3가 열립니다.
     */
    data class Success(
        val account: Account?,
        val badge: PlayerBadge?,
        val summary: ProfileSummary,
        val agents: AgentReport,
        val weapons: WeaponReport,
        val recentMatches: List<Match>,
        val hasMoreMatches: Boolean,
        val catalog: ContentCatalog,
        val now: Instant,
        val timeZone: TimeZone,
    ) : ProfileUiState
}

/**
 * 내 프로필 상태를 만듭니다. 최근 경기 말고는 모두 이번 액트의 경쟁 + 일반 경기만 셉니다. S6 무기와 S7 요원은
 * [RecordsViewModel]이 맡습니다.
 *
 * @param computation 경기를 모아 세는 곳입니다. 메인 스레드에서 세면 홈에서 넘어오는 전환이 멈춰서 기본은
 * [Dispatchers.Default]입니다.
 */
class ProfileViewModel(
    accountRepository: AccountRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
    computation: CoroutineContext = Dispatchers.Default,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        accountRepository.account,
        matchRepository.settledMatches(),
        contentRepository.catalog,
    ) { account, matches, catalog ->
        val actMatches = matches.currentActMatches(QueueFilter.PROFILE)
        val latest = matches.sortedByDescending { it.startedAt }
        ProfileUiState.Success(
            account = account,
            badge = account?.let { playerBadge(it.riotId, matches, catalog) },
            summary = actMatches.profileSummary(),
            agents = actMatches.agentReport(),
            weapons = matches.weaponReport(now = clock.now(), timeZone = timeZone, queueFilter = QueueFilter.PROFILE),
            recentMatches = latest.take(RECENT_MATCHES),
            hasMoreMatches = latest.size > RECENT_MATCHES,
            catalog = catalog,
            now = clock.now(),
            timeZone = timeZone,
        )
    }.flowOn(computation).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading,
    )

    private companion object {
        const val RECENT_MATCHES = 3
    }
}
