package com.ovalit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.model.Account
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.agentReport
import com.ovalit.core.model.currentActMatches
import com.ovalit.core.model.weaponReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.playerBadge
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Success(
        val account: Account?,
        val badge: PlayerBadge?,
        val agents: AgentReport,
        val weapons: WeaponReport,
        val catalog: ContentCatalog,
    ) : ProfileUiState
}

/**
 * 내 프로필, S6 무기, S7 요원이 같이 씁니다. 세 화면 모두 이번 액트의 경쟁 + 일반 경기를 봅니다.
 * 기타 모드는 라운드 수와 크레드 규칙이 달라서 섞으면 비율이 틀어집니다.
 */
class ProfileViewModel(
    accountRepository: AccountRepository,
    matchRepository: MatchRepository,
    contentRepository: ContentRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        accountRepository.account,
        matchRepository.observeMatches(),
        contentRepository.catalog,
    ) { account, matches, catalog ->
        ProfileUiState.Success(
            account = account,
            badge = account?.let { playerBadge(it.riotId, matches, catalog) },
            agents = matches.currentActMatches(QUEUE).agentReport(),
            weapons = matches.weaponReport(now = clock.now(), timeZone = timeZone, queueFilter = QUEUE),
            catalog = catalog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading,
    )

    private companion object {
        val QUEUE = QueueFilter.COMPETITIVE_AND_UNRATED
    }
}
