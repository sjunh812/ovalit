package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.ProfileAgentsSection
import com.ovalit.core.ui.ProfileBanner
import com.ovalit.core.ui.ProfileIdentity
import com.ovalit.core.ui.ProfileShotsSection
import com.ovalit.core.ui.ProfileStatsSection
import com.ovalit.core.ui.ProfileStatusBarScrim
import com.ovalit.core.ui.ProfileTierCard
import com.ovalit.core.ui.ProfileWeaponsSection
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.act_matches
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.no_matches
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 내 프로필입니다. 홈 오른쪽 위 아바타에서 들어옵니다. */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, onBack, onOpenAgents, onOpenWeapons, onOpenMatch, onOpenMatches, modifier)
}

/**
 * 위에서부터 티어 카드, 통계, 맞힌 부위, 요원과 무기, 최근 경기 순서입니다. 최근 경기 말고는 모두 이번
 * 액트의 내 경기끼리만 셉니다.
 */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is ProfileUiState.Success) return@Box
        val badge = uiState.badge ?: PlayerBadge(riotId = "", tier = null, tierName = null)
        val competitive = uiState.summary.competitive
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            ProfileBanner(badge) {
                OvalitBackTopBar(onBack = onBack)
            }
            Spacer(Modifier.height(10.dp))
            ProfileIdentity(
                badge = badge,
                showTier = competitive == null,
                mainRole = uiState.agents.mainRole,
                mainRoleShare = uiState.agents.mainRoleShare,
                trailing = stringResource(CoreUiRes.string.act_matches, uiState.agents.matches),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
            )

            if (uiState.agents.matches == 0) {
                Spacer(Modifier.height(20.dp))
                OvalitText(
                    text = stringResource(Res.string.no_matches),
                    modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                    style = OvalitTheme.typography.body,
                    color = colors.t2,
                )
                Spacer(Modifier.height(20.dp))
            } else {
                if (competitive != null) {
                    Spacer(Modifier.height(20.dp))
                    ProfileTierCard(competitive, uiState.catalog, Modifier.padding(horizontal = OvalitSpacing.gutter))
                }
                ProfileStatsSection(uiState.summary, Modifier.padding(top = 6.dp))
                ProfileShotsSection(uiState.summary.metrics.shots)
                ProfileAgentsSection(uiState.agents, uiState.catalog, onOpenAgents)
                ProfileWeaponsSection(uiState.weapons, uiState.catalog, onOpenWeapons)
            }
            RecentMatchesSection(uiState, onOpenMatch, onOpenMatches)
            Spacer(Modifier.height(OvalitSpacing.xxl))
            // 탭바 밖 화면이라 시스템 내비게이션 바 높이만큼 더 띄운다. 안 그러면 마지막 줄이 내비게이션 바에 덮인다.
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        ProfileStatusBarScrim(scrollState)
    }
}
