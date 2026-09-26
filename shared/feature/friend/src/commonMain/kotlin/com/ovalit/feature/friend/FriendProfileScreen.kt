package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitIconButton
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.CompetitiveRecord
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.HeadToHeadRow
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.ProfileAgentsSection
import com.ovalit.core.ui.ProfileBanner
import com.ovalit.core.ui.ProfileIdentity
import com.ovalit.core.ui.ProfileSection
import com.ovalit.core.ui.ProfileShotsSection
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
 * S5 친구 프로필입니다. 같이 한 경기를 맨 위에 두고, 전적을 공개한 친구면 그 아래에 내 프로필과 같은 칸(티어 카드, 통계,
 * 맞힌 부위, 요원, 무기)과 나와 비교, 최근 경기를 둡니다. 비공개면 같이 한 경기만 보여줍니다.
 */
@Composable
fun FriendProfileRoute(
    friendId: PlayerId,
    onBack: () -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendProfileViewModel = koinViewModel(key = friendId.value) { parametersOf(friendId.value) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState == FriendProfileUiState.Gone) onBack()
    }
    FriendProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleRival = viewModel::toggleRival,
        onUnfriend = { viewModel.unfriend(onBack) },
        onOpenMatches = onOpenMatches,
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
) {
    val colors = OvalitTheme.colors
    val haptics = rememberOvalitHaptics()
    var confirmUnfriend by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is FriendProfileUiState.Success) return@Box
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
                // 내 프로필처럼 티어를 아래 카드에 크게 두면 이름 줄에서는 뺀다
                ProfileIdentity(
                    badge = if (competitive != null) uiState.badge.copy(tier = null, tierName = null) else uiState.badge,
                    mainRole = profile?.agents?.mainRole,
                    mainRoleShare = profile?.agents?.mainRoleShare,
                    trailing = profile?.let { stringResource(CoreUiRes.string.act_matches, it.agents.matches) },
                    modifier = Modifier.weight(1f),
                )
                if (friend.statsPublic) {
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
            Spacer(Modifier.height(20.dp))

            // 친구 기반 앱만 낼 수 있는 숫자라 맨 위에 둔다
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
                // 내 프로필과 같은 칸을 같은 순서로 쓴다. 다만 요원과 무기는 친구용 상세 화면이 없어 누르지 않는다.
                if (hasActMatches) {
                    competitive?.let { TierCard(it, uiState) }
                    ProfileStatsSection(profile.summary, Modifier.padding(top = 6.dp))
                }
                (uiState.myReport as? WeeklyReport.Ready)?.let { mine -> CompareSection(mine, uiState, name) }
                if (hasActMatches) {
                    ProfileShotsSection(profile.summary.metrics.shots)
                    ProfileAgentsSection(profile.agents, uiState.catalog)
                    ProfileWeaponsSection(profile.weapons, uiState.catalog)
                }
                RecentMatches(uiState, name, onOpenMatches)
            }
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
        ProfileStatusBarScrim(scrollState)
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

@Composable
private fun TierCard(record: CompetitiveRecord, uiState: FriendProfileUiState.Success) {
    ProfileTierCard(record, uiState.catalog, Modifier.padding(horizontal = OvalitSpacing.gutter))
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
                text = stringResource(Res.string.compare_no_matches, periodLabel(mine.period)),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        }
        Spacer(Modifier.height(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            FixedMetric.entries.forEach {
                HeadToHeadRow(it, mine.metrics, uiState.theirMetricsInMyPeriod, rowMetrics = FixedMetric.entries)
            }
        }
    }
}

// 친구 경기에는 내가 안 뛴 경기의 다른 사람 기록이 섞여 있다. 앱을 안 쓰는 사람의 기록은 내가 뛴 경기
// 안에서만 보여줄 수 있어서 줄을 눌러도 열지 않는다.
@Composable
private fun RecentMatches(uiState: FriendProfileUiState.Success, name: String, onOpenMatches: () -> Unit) {
    val matches = uiState.friend.matches.sortedByDescending { it.startedAt }
    if (matches.isEmpty()) return
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = stringResource(Res.string.recent_title, name),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        if (matches.size > RECENT_MATCHES) {
            OvalitTextButton(text = stringResource(Res.string.recent_all), onClick = onOpenMatches)
        }
    }
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

@Composable
private fun TitleRow(title: String, caption: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        OvalitText(
            text = title,
            modifier = Modifier.weight(1f).alignByBaseline(),
            style = OvalitTheme.typography.bodyStrong,
        )
        OvalitText(
            text = caption,
            modifier = Modifier.alignByBaseline(),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}
