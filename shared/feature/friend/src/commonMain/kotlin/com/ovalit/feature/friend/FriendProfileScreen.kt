package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.HeadToHeadRow
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.ProfileBanner
import com.ovalit.core.ui.ProfileIdentity
import com.ovalit.core.ui.ProfileStatusBarScrim
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.recentMatchTimeLabel
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.cancel
import com.ovalit.feature.friend.resources.compare_caption
import com.ovalit.feature.friend.resources.compare_no_matches
import com.ovalit.feature.friend.resources.compare_title
import com.ovalit.feature.friend.resources.more
import com.ovalit.feature.friend.resources.not_enough
import com.ovalit.feature.friend.resources.period_matches
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

private val TouchSize = 44.dp
private val CellGap = 10.dp
private const val RECENT_MATCHES = 3

/** S5 친구 프로필입니다. 전적 페이지가 아니라 나와의 관계 페이지라 같이 한 경기가 맨 위에 옵니다. */
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
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter),
                verticalAlignment = Alignment.Top,
            ) {
                ProfileIdentity(
                    badge = uiState.badge,
                    mainRole = (uiState.theirReport as? WeeklyReport.Ready)?.mainRole,
                    mainRoleShare = (uiState.theirReport as? WeeklyReport.Ready)?.mainRoleShare,
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

            Section {
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

            if (!friend.statsPublic) {
                Section {
                    OvalitText(text = stringResource(Res.string.private_body), style = OvalitTheme.typography.body, color = colors.t2)
                }
            } else {
                TheirWeek(uiState.theirReport)
                (uiState.myReport as? WeeklyReport.Ready)?.let { mine ->
                    Section {
                        TitleRow(
                            title = stringResource(Res.string.compare_title),
                            caption = stringResource(Res.string.compare_caption, periodLabel(mine.period), name),
                        )
                        if (uiState.theirMetricsInMyPeriod == null) {
                            Spacer(Modifier.height(OvalitSpacing.xs))
                            OvalitText(
                                text = stringResource(Res.string.compare_no_matches, periodLabel(mine.period)),
                                style = OvalitTheme.typography.caption,
                                color = colors.t3,
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

// 친구 경기에는 내가 안 뛴 경기의 다른 사람 기록이 섞여 있다. 앱을 안 쓰는 사람의 기록은 내가 뛴 경기
// 안에서만 보여줄 수 있어서 줄을 눌러도 열지 않는다.
@Composable
private fun RecentMatches(uiState: FriendProfileUiState.Success, name: String, onOpenMatches: () -> Unit) {
    val matches = uiState.friend.matches.sortedByDescending { it.startedAt }
    if (matches.isEmpty()) return
    Spacer(Modifier.height(20.dp))
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm, top = 6.dp),
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
private fun Section(content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Spacer(Modifier.height(18.dp))
    Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) { content() }
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

@Composable
private fun TheirWeek(report: WeeklyReport?) {
    val colors = OvalitTheme.colors
    Section {
        when (report) {
            is WeeklyReport.Ready -> {
                TitleRow(
                    title = periodLabel(report.period),
                    caption = stringResource(Res.string.period_matches, report.metrics.matches),
                )
                Spacer(Modifier.height(14.dp))
                TheirMetrics(report)
            }
            is WeeklyReport.NotEnoughMatches -> OvalitText(
                text = stringResource(Res.string.not_enough, report.played),
                style = OvalitTheme.typography.body,
                color = colors.t2,
            )
            null -> Unit
        }
    }
}


// 네 칸의 이름, 숫자, 변화량을 줄마다 한 크기로 맞춘다. 칸마다 따로 줄이면 "전투점수"만 작아지고 그 칸
// 숫자만 위로 올라가 줄이 어긋난다.
@Composable
private fun TheirMetrics(report: WeeklyReport.Ready) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    val cells = FixedMetric.entries.map { metric ->
        val current = metric.value(report.metrics)
        val before = report.baseline?.metrics?.let(metric.value)
        MetricCellText(
            label = stringResource(metric.label),
            value = current?.let { metric.format.valueText(it) } ?: "–",
            change = if (current != null && before != null) metric.format.formatChange(current, before) else null,
            changeColor = if (current != null && before != null) {
                when (metric.format.direction(current, before)) {
                    1 -> colors.pos
                    -1 -> colors.neg
                    else -> colors.t3
                }
            } else {
                colors.t3
            },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gaps = (CellGap * 2 + 1.dp) * (cells.size - 1)
        val cellWidth = (maxWidth - gaps) / cells.size
        val labelStyle = rememberFittingStyle(cells.map { it.label }, typography.caption, cellWidth)
        val valueStyle = rememberFittingStyle(cells.map { it.value }, typography.metricM, cellWidth, min = 14.sp)
        val changeStyle = rememberFittingStyle(cells.mapNotNull { it.change }, typography.metricS, cellWidth)
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            cells.forEachIndexed { index, cell ->
                if (index > 0) {
                    Spacer(Modifier.width(CellGap))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                    Spacer(Modifier.width(CellGap))
                }
                Column(
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    OvalitText(
                        text = cell.label,
                        style = labelStyle,
                        color = colors.t2,
                        maxLines = 1,
                        autoSize = shrinkToFit(labelStyle.fontSize, min = 7.sp),
                    )
                    OvalitText(
                        text = cell.value,
                        style = valueStyle,
                        maxLines = 1,
                        autoSize = shrinkToFit(valueStyle.fontSize),
                    )
                    if (cell.change != null) {
                        OvalitText(
                            text = cell.change,
                            style = changeStyle,
                            color = cell.changeColor,
                            maxLines = 1,
                            autoSize = shrinkToFit(changeStyle.fontSize, min = 7.sp),
                        )
                    }
                }
            }
        }
    }
}

private class MetricCellText(val label: String, val value: String, val change: String?, val changeColor: Color)
