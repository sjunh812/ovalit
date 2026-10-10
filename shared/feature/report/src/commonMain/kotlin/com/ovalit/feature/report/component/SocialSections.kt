package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitPickerButton
import com.ovalit.core.designsystem.component.OvalitPickerTitle
import com.ovalit.core.designsystem.component.OvalitSheetOption
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.HeadToHeadRow
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.leadDirection
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.rememberWidestWidth
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.FriendStanding
import com.ovalit.feature.report.RankedEntry
import com.ovalit.feature.report.preview
import com.ovalit.feature.report.rankFriends
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.friends_all
import com.ovalit.feature.report.resources.friends_choose_metric
import com.ovalit.feature.report.resources.friends_me
import com.ovalit.feature.report.resources.friends_metric_button
import com.ovalit.feature.report.resources.friends_title
import com.ovalit.feature.report.resources.rival_lead
import com.ovalit.feature.report.resources.rival_no_matches
import com.ovalit.feature.report.resources.rival_title
import org.jetbrains.compose.resources.stringResource

// 홈 고정 칸을 모두 같은 순서로 겨룬다. 고정 칸에 지표가 늘면 여기에도 같이 붙는다.
private val RivalMetrics = FixedMetric.entries
private val NameWidth = 72.dp

internal class RankWidths(val rank: Dp, val name: Dp, val value: Dp)

@Composable
internal fun RivalSection(
    report: WeeklyReport.Ready,
    mine: MatchMetrics,
    rival: FriendStanding,
    modifier: Modifier = Modifier,
) {
    val theirs = rival.metrics
    // 화면에 보이는 자릿수로 반올림한 값끼리 겨룬다. 1.42와 1.42를 띄워 놓고 한쪽이 앞섰다고 하면 틀려 보인다.
    val leads = if (theirs == null) 0 else RivalMetrics.count { metric -> leadDirection(metric, mine, theirs) > 0 }

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = stringResource(Res.string.rival_title, rival.riotId),
            titleStyle = OvalitTheme.typography.bodyStrong,
            caption = AnnotatedString(
                if (theirs == null) {
                    stringResource(Res.string.rival_no_matches, periodLabel(report.period))
                } else {
                    stringResource(Res.string.rival_lead, RivalMetrics.size, leads)
                },
            ),
        )
        Spacer(Modifier.height(SectionTitleGap))
        Column(verticalArrangement = Arrangement.spacedBy(BarRowGap)) {
            RivalMetrics.forEach { metric -> HeadToHeadRow(metric, mine, theirs, rowMetrics = RivalMetrics) }
        }
    }
}

// 홈 고정 칸과 같은 순서다
private val RankableMetrics = FixedMetric.entries

/**
 * 서로 수락한 친구끼리만 줄을 세웁니다. 앱 전체나 모르는 사람과는 순위를 만들지 않습니다. 그 기간에 경기가 없는 친구는
 * 빠집니다.
 *
 * 친구가 수백 명이어도 위 다섯 줄만 둡니다. 내가 그 밖이면 내 줄을 진짜 등수로 한 칸 띄워 붙이고, 남은 사람이 있으면 전체 순위
 * 화면으로 가는 입구를 둡니다(사용자 결정).
 *
 * @param onOpenAll 지금 고른 지표를 받아 전체 순위 화면을 엽니다.
 */
@Composable
internal fun FriendRankingSection(
    mine: MatchMetrics,
    friends: List<FriendStanding>,
    modifier: Modifier = Modifier,
    onOpenAll: (FixedMetric) -> Unit = {},
) {
    var metricName by rememberSaveable { mutableStateOf(FixedMetric.DAMAGE.name) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val metric = FixedMetric.valueOf(metricName)
    val me = stringResource(Res.string.friends_me)
    val ranked = remember(mine, friends, metric, me) { rankFriends(me, mine, friends, metric) }
    val preview = ranked.preview()
    val top = ranked.firstOrNull()?.value?.takeIf { it > 0 } ?: 1.0

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        RankingTitle(
            title = { OvalitText(text = stringResource(Res.string.friends_title), style = OvalitTheme.typography.bodyStrong) },
            metric = metric,
            onChoose = { choosing = true },
        )
        Spacer(Modifier.height(SectionTitleGap))
        val widths = rememberRankWidths(preview.top + listOfNotNull(preview.mine), metric)
        Column(verticalArrangement = Arrangement.spacedBy(BarRowGap)) {
            preview.top.forEach { RankRow(it, metric, top, widths) }
            preview.mine?.let {
                if (preview.skipsRows) RankGap(widths)
                RankRow(it, metric, top, widths)
            }
        }
        if (preview.hasMore) {
            Spacer(Modifier.height(OvalitSpacing.lg))
            RankingAllEntry(total = preview.total, onClick = { onOpenAll(metric) })
        }
    }

    if (choosing) {
        RankMetricSheet(
            selected = metric,
            onPick = {
                metricName = it.name
                choosing = false
            },
            onDismiss = { choosing = false },
        )
    }
}

/** 친구 비교 제목 줄입니다. 오른쪽에 지금 고른 지표를 적은 버튼을 둡니다. */
@Composable
internal fun RankingTitle(title: @Composable () -> Unit, metric: FixedMetric, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    val metricLabel = stringResource(metric.label)
    OvalitPickerTitle(
        title = title,
        picker = {
            OvalitPickerButton(
                text = metricLabel,
                onClickLabel = stringResource(Res.string.friends_metric_button, metricLabel),
                onClick = onChoose,
            )
        },
        modifier = modifier,
    )
}

@Composable
internal fun RankMetricSheet(selected: FixedMetric, onPick: (FixedMetric) -> Unit, onDismiss: () -> Unit) {
    OvalitBottomSheet(title = stringResource(Res.string.friends_choose_metric), onDismiss = onDismiss) {
        Column(modifier = Modifier.selectableGroup()) {
            RankableMetrics.forEach { option ->
                OvalitSheetOption(
                    text = stringResource(option.label),
                    selected = option == selected,
                    onClick = { onPick(option) },
                )
            }
        }
    }
}

// 홈 첫 카드의 8주 흐름 입구와 같은 면이다. 카드 안에서 다른 화면으로 넘어가는 칸은 이 모양으로 둔다.
@Composable
private fun RankingAllEntry(total: Int, onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 면까지 같이 줄도록 누름 효과를 면보다 앞에 단다(docs/design.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.fill)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = stringResource(Res.string.friends_all, total),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.label,
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 14.dp)
    }
}

/**
 * 순위와 값 칸은 가장 긴 글자에 맞추고 이름은 넘치면 말줄임표로 자릅니다. 폭을 고정하면 글씨를 키웠을 때 순위 "10"이 꺾이고 값이
 * 잘립니다. 숫자는 폭이 고정된 글꼴(tnum)이라 자릿수가 가장 많은 것만 잽니다. 친구가 수백 명이어도 줄마다 재지 않습니다.
 */
@Composable
internal fun rememberRankWidths(entries: List<RankedEntry>, metric: FixedMetric): RankWidths {
    val typography = OvalitTheme.typography
    val format = metric.format
    val longestValues = entries.map { format.format(it.value) }.distinct().let { values ->
        val longest = values.maxOfOrNull { it.length } ?: 0
        values.filter { it.length == longest }
    }
    val valueTexts = longestValues.map { shown -> entries.first { format.format(it.value) == shown } }
        .map { format.valueText(it.value) }
    return RankWidths(
        rank = rememberWidestWidth(listOfNotNull(entries.maxOfOrNull { it.rank }?.toString()), typography.metricS),
        name = rememberNameWidth(entries.map { it.name }, typography.caption.copy(fontWeight = FontWeight.SemiBold)),
        value = rememberWidestWidth(valueTexts, typography.metricS.copy(fontWeight = FontWeight.Bold)),
    )
}

// 짧은 이름만 있으면 이름 칸을 줄여 막대가 이름 가까이에서 시작한다. 길면 [NameWidth]에서 자른다. 친구가 수백 명이면 글자가 많은
// 이름부터 재다가 상한에 닿으면 멈춘다.
@Composable
private fun rememberNameWidth(names: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(names, style, density, measurer) {
        val cap = with(density) { NameWidth.roundToPx() }
        var widest = 0
        for (name in names.distinct().sortedByDescending { it.length }) {
            widest = maxOf(widest, measurer.measure(name, style, softWrap = false, maxLines = 1).size.width)
            if (widest >= cap) break
        }
        with(density) { widest.toDp() }.coerceAtMost(NameWidth)
    }
}

// 위 다섯 줄 밖에 있는 내 줄을 띄운다. 바로 붙이면 내 줄이 6등처럼 읽힌다. 순위 칸 가운데에 점 셋을 세로로 둔다.
@Composable
internal fun RankGap(widths: RankWidths) {
    val dot = OvalitTheme.colors.t4
    Column(
        modifier = Modifier.width(widths.rank),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RankGapDotSpacing),
    ) {
        repeat(3) { Box(Modifier.size(RankGapDotSize).background(dot, CircleShape)) }
    }
}

private val RankGapDotSize = 2.dp
private val RankGapDotSpacing = 3.dp

@Composable
internal fun RankRow(entry: RankedEntry, metric: FixedMetric, top: Double, widths: RankWidths, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val strong = if (entry.isMe) colors.t1 else colors.t2

    Row(modifier = modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = entry.rank.toString(),
            modifier = Modifier.width(widths.rank),
            style = OvalitTheme.typography.metricS,
            color = colors.t3,
            maxLines = 1,
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitText(
            text = entry.name,
            modifier = Modifier.width(widths.name),
            // 막대 줄은 라이벌·나와 비교처럼 작은 글자로 맞춘다(docs/design.md)
            style = OvalitTheme.typography.caption.copy(fontWeight = if (entry.isMe) FontWeight.SemiBold else FontWeight.Normal),
            color = strong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 이름이 말줄임으로 칸을 다 채워도 막대에 붙지 않게 띄운다
        Spacer(Modifier.width(OvalitSpacing.sm))
        Box(modifier = Modifier.weight(1f).height(3.dp).background(colors.fill)) {
            // 목업대로 나만 --accent다
            Box(
                Modifier
                    .fillMaxWidth((entry.value / top).toFloat().coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(if (entry.isMe) colors.accent else colors.t4),
            )
        }
        // 값 칸은 가장 긴 숫자에 맞춘 폭이라 띄우지 않으면 1위 막대 끝이 숫자에 닿는다
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(
            text = metric.format.valueText(entry.value),
            modifier = Modifier.width(widths.value),
            style = OvalitTheme.typography.metricS.copy(fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Medium),
            color = strong,
            textAlign = TextAlign.End,
        )
    }
}
