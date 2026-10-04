package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.component.OvalitCardGap
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitIconButton
import com.ovalit.core.designsystem.component.OvalitPickerTitle
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitStaged
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.FailureNoticesEffect
import com.ovalit.core.ui.HeadToHeadRow
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
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
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.recentMatchTimeLabel
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.act_matches
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.cancel
import com.ovalit.feature.friend.resources.compare_caption
import com.ovalit.feature.friend.resources.compare_no_matches
import com.ovalit.feature.friend.resources.compare_title
import com.ovalit.feature.friend.resources.more
import com.ovalit.feature.friend.resources.no_act_matches
import com.ovalit.feature.friend.resources.private_body
import com.ovalit.feature.friend.resources.recent_all
import com.ovalit.feature.friend.resources.recent_title
import com.ovalit.feature.friend.resources.set_rival
import com.ovalit.feature.friend.resources.shared_count
import com.ovalit.feature.friend.resources.shared_matches
import com.ovalit.feature.friend.resources.shared_record
import com.ovalit.feature.friend.resources.unfriend
import com.ovalit.feature.friend.resources.unfriend_body
import com.ovalit.feature.friend.resources.unfriend_title
import com.ovalit.feature.friend.resources.unset_rival
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val RECENT_MATCHES = 3

/**
 * S5 친구 프로필입니다. 머리 바로 밑에 같이 뛴 경기를 두고, 전적을 공개한 친구면 그 아래로 티어 카드, 통계, 나와 비교,
 * 맞힌 부위, 요원, 무기, 최근 경기를 둡니다. 비공개면 같이 뛴 경기만 보여줍니다.
 */
@Composable
fun FriendProfileRoute(
    friendId: PlayerId,
    onBack: () -> Unit,
    onOpenMatches: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendProfileViewModel = koinViewModel(key = friendId.value) { parametersOf(friendId.value) },
) {
    FailureNoticesEffect(viewModel.notices)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 화면을 닫는 곳은 여기 하나다. 끊기 버튼에서도 닫으면 뒤로 가기가 두 번 돼 그 앞 화면까지 빠진다.
    LaunchedEffect(uiState) {
        if (uiState == FriendProfileUiState.Gone) onBack()
    }
    // 친구가 끊겨 닫히는 동안에는 마지막 모습을 그린다. 바로 비우면 밀려나는 화면이 빈 바탕으로 나간다.
    val lastShown = remember { LastShown() }
    if (uiState != FriendProfileUiState.Gone) lastShown.state = uiState
    FriendProfileScreen(
        uiState = lastShown.state,
        onBack = onBack,
        onToggleRival = viewModel::toggleRival,
        onUnfriend = viewModel::unfriend,
        onOpenMatches = onOpenMatches,
        onOpenAgents = onOpenAgents,
        onOpenWeapons = onOpenWeapons,
        modifier = modifier,
    )
}

@Composable
internal fun FriendProfileScreen(
    uiState: FriendProfileUiState,
    onBack: () -> Unit,
    onToggleRival: () -> Unit,
    onUnfriend: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMatches: () -> Unit = {},
    onOpenAgents: () -> Unit = {},
    onOpenWeapons: () -> Unit = {},
) {
    val colors = OvalitTheme.colors
    val haptics = rememberOvalitHaptics()
    var confirmUnfriend by rememberSaveable { mutableStateOf(false) }

    // 밀려 들어오는 중에 내용이 도착하면 다 들어올 때까지 스켈레톤을 두었다가 서서히 바꾼다.
    val shown = rememberContentShown(loaded = uiState is FriendProfileUiState.Success)
    OvalitStaged(
        ready = uiState is FriendProfileUiState.Success && shown,
        modifier = modifier.fillMaxSize().background(colors.canvas),
        contentBackground = colors.canvas,
        placeholder = { ProfileSkeleton { OvalitBackTopBar(onBack = onBack) } },
    ) {
        if (uiState !is FriendProfileUiState.Success) return@OvalitStaged
        Box(modifier = Modifier.fillMaxSize()) {
            val friend = uiState.friend
            val name = friend.riotId.substringBefore('#')

            val scrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                ProfileBanner(uiState.badge) {
                    OvalitBackTopBar(onBack = onBack) {
                        OvalitIconButton(OvalitIcons.More, stringResource(Res.string.more), onClick = { confirmUnfriend = true })
                    }
                }
                val profile = uiState.theirProfile
                val competitive = profile?.summary?.competitive
                val hasActMatches = profile != null && profile.agents.matches > 0
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProfileIdentity(
                        badge = uiState.badge,
                        showTier = competitive == null,
                        mainRole = profile?.agents?.mainRole,
                        mainRoleShare = profile?.agents?.mainRoleShare,
                        trailing = profile?.let { stringResource(CoreUiRes.string.act_matches, it.agents.matches) },
                        modifier = Modifier.weight(1f),
                    )
                    // 라이벌로 둔 친구가 전적을 비공개로 바꿔도 해제할 수 있어야 한다
                    if (friend.statsPublic || uiState.isRival) {
                        SmallButton(
                            text = stringResource(if (uiState.isRival) Res.string.unset_rival else Res.string.set_rival),
                            filled = false,
                            onClick = {
                                if (!uiState.isRival) haptics.confirm()
                                onToggleRival()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(OvalitSpacing.sm))

                // 친구 기반 앱만 낼 수 있는 숫자라 머리 바로 밑에 둔다
                ProfileSection {
                    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        OvalitText(
                            text = stringResource(Res.string.shared_matches),
                            modifier = Modifier.weight(1f),
                            style = OvalitTheme.typography.body,
                            color = colors.t2,
                        )
                        OvalitText(
                            text = stringResource(Res.string.shared_count, uiState.shared.matches),
                            style = OvalitTheme.typography.metricS,
                            color = colors.t3,
                        )
                        Spacer(Modifier.width(OvalitSpacing.md))
                        OvalitText(
                            text = stringResource(Res.string.shared_record, uiState.shared.wins, uiState.shared.losses),
                            style = OvalitTheme.typography.bodyStrong,
                        )
                    }
                }

                if (profile == null) {
                    ProfileSection {
                        OvalitText(text = stringResource(Res.string.private_body), style = OvalitTheme.typography.body, color = colors.t2)
                    }
                } else {
                    // 내 프로필과 같은 칸을 같은 순서로 쓰고 통계 다음에만 나와 비교를 끼운다.
                    if (hasActMatches) {
                        competitive?.let { ProfileTierCard(it, uiState.catalog) }
                        ProfileStatsSection(profile.summary)
                    } else {
                        ProfileSection {
                            OvalitText(text = stringResource(Res.string.no_act_matches), style = OvalitTheme.typography.body, color = colors.t2)
                        }
                    }
                    (uiState.myReport as? WeeklyReport.Ready)?.let { mine -> CompareSection(mine, uiState, name) }
                    if (hasActMatches) {
                        ProfileShotsSection(profile.summary.metrics.shots)
                        ProfileAgentsSection(profile.agents, uiState.catalog, onOpen = onOpenAgents)
                        ProfileWeaponsSection(profile.weapons, uiState.catalog, onOpen = onOpenWeapons)
                    }
                    RecentMatches(uiState, name, onOpenMatches)
                }
                Spacer(Modifier.height(OvalitSpacing.xxl))
                // 탭바가 없는 화면이라 마지막 줄이 내비게이션 바에 덮이지 않게 그 높이만큼 띄운다.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            ProfileStatusBarScrim(scrollState)
        }
    }

    if (confirmUnfriend && uiState is FriendProfileUiState.Success) {
        OvalitBottomSheet(
            title = stringResource(Res.string.unfriend_title, uiState.friend.riotId.substringBefore('#')),
            body = stringResource(Res.string.unfriend_body),
            onDismiss = { confirmUnfriend = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OvalitPrimaryButton(
                    text = stringResource(Res.string.unfriend),
                    onClick = {
                        confirmUnfriend = false
                        onUnfriend()
                    },
                )
                OvalitTextButton(text = stringResource(Res.string.cancel), onClick = { confirmUnfriend = false })
            }
        }
    }
}

// 내 리포트 기간으로 센 친구 숫자를 내 숫자와 나란히 둔다
@Composable
private fun CompareSection(mine: WeeklyReport.Ready, uiState: FriendProfileUiState.Success, name: String) {
    ProfileSection {
        TitleRow(
            title = stringResource(Res.string.compare_title),
            caption = stringResource(Res.string.compare_caption, periodLabel(mine.period), name),
        )
        if (uiState.theirMetricsInMyPeriod == null) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.compare_no_matches, periodLabel(mine.period), name),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            FixedMetric.entries.forEach {
                HeadToHeadRow(it, mine.metrics, uiState.theirMetricsInMyPeriod, rowMetrics = FixedMetric.entries)
            }
        }
    }
}

// 친구 경기에는 내가 안 뛴 경기의 다른 사람 기록이 섞여 있다. 앱을 안 쓰는 사람의 기록은 내가 뛴 경기 안에서만
// 보여줄 수 있어서 줄을 눌러도 열지 않는다(CLAUDE.md 지켜야 할 선).
@Composable
private fun RecentMatches(uiState: FriendProfileUiState.Success, name: String, onOpenMatches: () -> Unit) {
    val matches = uiState.friend.matches.sortedByDescending { it.startedAt }
    if (matches.isEmpty()) return
    Spacer(Modifier.height(OvalitCardGap))
    OvalitCard {
        // 버튼 높이로 줄을 늘리지 않아 다른 카드와 제목 자리가 같다
        OvalitPickerTitle(
            title = { OvalitText(text = stringResource(Res.string.recent_title, name), style = OvalitTheme.typography.bodyStrong) },
            picker = {
                if (matches.size > RECENT_MATCHES) {
                    OvalitTextButton(text = stringResource(Res.string.recent_all), onClick = onOpenMatches)
                }
            },
            modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm),
        )
        matches.take(RECENT_MATCHES).forEachIndexed { index, match ->
            if (index > 0) OvalitDivider(Modifier.padding(start = 67.dp), color = OvalitTheme.colors.lineWeak)
            MatchRow(
                match = match,
                catalog = uiState.catalog,
                timeLabel = recentMatchTimeLabel(match.startedAt, uiState.now, uiState.timeZone),
                style = MatchRowStyle.COMPACT,
            )
        }
    }
}

@Composable
private fun TitleRow(title: String, caption: String) {
    // 제목을 먼저 재고 설명은 남은 폭에서 꺾는다. 설명을 먼저 재면 친구 이름이 길 때 "나와 비교"가 밀려 꺾인다.
    Row(verticalAlignment = Alignment.Bottom) {
        OvalitText(
            text = title,
            modifier = Modifier.alignByBaseline(),
            style = OvalitTheme.typography.bodyStrong,
            maxLines = 1,
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitText(
            text = caption,
            modifier = Modifier.weight(1f).alignByBaseline(),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
            textAlign = TextAlign.End,
        )
    }
}

private class LastShown(var state: FriendProfileUiState = FriendProfileUiState.Loading)
