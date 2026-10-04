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
import com.ovalit.core.designsystem.component.OvalitStage
import com.ovalit.core.designsystem.component.OvalitStaged
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.ProfileAgentsSection
import com.ovalit.core.ui.ProfileBanner
import com.ovalit.core.ui.ProfileIdentity
import com.ovalit.core.ui.ProfileSection
import com.ovalit.core.ui.ProfileShotsSection
import com.ovalit.core.ui.ProfileSkeleton
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

/** 내 프로필입니다. 홈 오른쪽 위 아바타, S3 스코어보드의 내 줄, S4 계정 줄에서 들어옵니다. */
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

// 칸 순서는 docs/screens.md의 내 프로필을 따른다. 최근 경기 말고는 모두 이번 액트의 경쟁 + 일반 경기로 센 숫자다.
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
    // 밀려 들어오는 중에 내용이 도착하면 다 들어올 때까지 스켈레톤을 두고, 묶음마다 한 프레임씩 그린 뒤 서서히 바꾼다.
    val shown = rememberContentShown(loaded = uiState is ProfileUiState.Success)

    OvalitStaged(
        ready = uiState is ProfileUiState.Success && shown,
        modifier = modifier.fillMaxSize().background(colors.canvas),
        contentBackground = colors.canvas,
        placeholder = { ProfileSkeleton { OvalitBackTopBar(onBack = onBack) } },
    ) {
        if (uiState !is ProfileUiState.Success) return@OvalitStaged
        val badge = uiState.badge ?: PlayerBadge(riotId = "", tier = null, tierName = null)
        val competitive = uiState.summary.competitive
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                OvalitStage {
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
                }

                Spacer(Modifier.height(OvalitSpacing.sm))
                if (uiState.agents.matches == 0) {
                    ProfileSection {
                        OvalitText(text = stringResource(Res.string.no_matches), style = OvalitTheme.typography.body, color = colors.t2)
                    }
                } else {
                    OvalitStage {
                        competitive?.let { ProfileTierCard(it, uiState.catalog) }
                        ProfileStatsSection(uiState.summary)
                    }
                    OvalitStage { ProfileShotsSection(uiState.summary.metrics.shots) }
                    OvalitStage { ProfileAgentsSection(uiState.agents, uiState.catalog, onOpenAgents) }
                    OvalitStage { ProfileWeaponsSection(uiState.weapons, uiState.catalog, onOpenWeapons) }
                }
                OvalitStage { RecentMatchesSection(uiState, onOpenMatch, onOpenMatches) }
                Spacer(Modifier.height(OvalitSpacing.xxl))
                // 탭바가 없는 화면이라 마지막 줄이 내비게이션 바에 덮이지 않게 그 높이만큼 띄운다.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            ProfileStatusBarScrim(scrollState)
        }
    }
}
