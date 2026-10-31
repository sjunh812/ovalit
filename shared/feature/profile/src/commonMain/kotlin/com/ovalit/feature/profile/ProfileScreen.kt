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
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.ProfileAdSlot
import com.ovalit.core.ui.ProfileAgentsSection
import com.ovalit.core.ui.ProfileBanner
import com.ovalit.core.ui.ProfileHeaderInset
import com.ovalit.core.ui.ProfileIdentity
import com.ovalit.core.ui.ProfileNoActMatchesSection
import com.ovalit.core.ui.ProfileQueueChips
import com.ovalit.core.ui.ProfileQueueEmptySection
import com.ovalit.core.ui.ProfileShotsSection
import com.ovalit.core.ui.ProfileSkeleton
import com.ovalit.core.ui.ProfileStatsSection
import com.ovalit.core.ui.ProfileStatusBarScrim
import com.ovalit.core.ui.ProfileTierCard
import com.ovalit.core.ui.ProfileWeaponsSection
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.act_matches
import com.ovalit.core.ui.resources.profile_queue_matches
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 내 프로필입니다. 홈 오른쪽 위 아바타, S3 스코어보드의 내 줄, S4 계정 줄에서 들어옵니다. */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenAgents: (QueueFilter) -> Unit,
    onOpenWeapons: (QueueFilter) -> Unit,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, onBack, onOpenAgents, onOpenWeapons, onOpenMatch, onOpenMatches, modifier, onSelectQueue = viewModel::selectQueue)
}

// 칸 순서는 docs/screens.md의 내 프로필을 따른다.
// 머리와 티어 카드는 이번 액트의 경쟁 + 일반 경기로, 통계부터 무기까지는 큐 칩이 고른 큐로 센 숫자다.
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onOpenAgents: (QueueFilter) -> Unit,
    onOpenWeapons: (QueueFilter) -> Unit,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectQueue: (QueueFilter) -> Unit = {},
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
        val report = uiState.report
        val competitive = report.competitive
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
                        mainRole = report.mainRole,
                        mainRoleShare = report.mainRoleShare,
                        trailing = stringResource(CoreUiRes.string.act_matches, report.actMatches),
                        modifier = Modifier.padding(horizontal = ProfileHeaderInset),
                    )
                }

                Spacer(Modifier.height(OvalitSpacing.sm))
                if (report.actMatches == 0) {
                    ProfileNoActMatchesSection()
                } else {
                    OvalitStage {
                        competitive?.let { ProfileTierCard(it, uiState.catalog) }
                        ProfileQueueChips(selected = report.queueFilter, onSelect = onSelectQueue)
                        if (report.isQueueEmpty) {
                            ProfileQueueEmptySection(report.queueFilter)
                        } else {
                            ProfileStatsSection(
                                summary = report.summary,
                                caption = stringResource(CoreUiRes.string.profile_queue_matches, report.agents.matches),
                            )
                        }
                    }
                    if (!report.isQueueEmpty) {
                        OvalitStage { ProfileShotsSection(report.summary.metrics.shots) }
                        // S7, S6도 칩이 고른 큐로 세어 카드에서 본 숫자와 같게 연다
                        OvalitStage { ProfileAgentsSection(report.agents, uiState.catalog) { onOpenAgents(report.queueFilter) } }
                        OvalitStage { ProfileWeaponsSection(report.weapons, uiState.catalog) { onOpenWeapons(report.queueFilter) } }
                        ProfileAdSlot(key = "profile-me")
                    }
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
