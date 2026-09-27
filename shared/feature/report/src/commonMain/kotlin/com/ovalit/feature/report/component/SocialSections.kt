package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.friends_choose_metric
import com.ovalit.feature.report.resources.friends_me
import com.ovalit.feature.report.resources.friends_metric_button
import com.ovalit.feature.report.resources.friends_title
import com.ovalit.feature.report.resources.rival_lead
import com.ovalit.feature.report.resources.rival_no_matches
import com.ovalit.feature.report.resources.rival_title
import org.jetbrains.compose.resources.stringResource

// 목업대로 역할이 달라도 나란히 놓을 수 있는 세 지표만 겨룬다
private val RivalMetrics = listOf(FixedMetric.KD, FixedMetric.DAMAGE, FixedMetric.HEADSHOT_RATE)
private val NameWidth = 72.dp

private class RankWidths(val rank: Dp, val name: Dp, val value: Dp)

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

private val RankableMetrics = listOf(FixedMetric.COMBAT_SCORE, FixedMetric.KD, FixedMetric.DAMAGE, FixedMetric.HEADSHOT_RATE)

private class Ranked(val name: String, val isMe: Boolean, val value: Double, val rank: Int)

/**
 * 서로 수락한 친구끼리만 줄을 세웁니다. 앱 전체나 모르는 사람과는 순위를 만들지 않습니다.
 * 그 기간에 경기가 없는 친구는 빠집니다.
 */
@Composable
internal fun FriendRankingSection(mine: MatchMetrics, friends: List<FriendStanding>, modifier: Modifier = Modifier) {
    var metricName by rememberSaveable { mutableStateOf(FixedMetric.DAMAGE.name) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val metric = FixedMetric.valueOf(metricName)
    val me = stringResource(Res.string.friends_me)

    val entries = (listOf(Triple(me, true, metric.value(mine))) +
        friends.map { Triple(it.riotId.substringBefore('#'), false, it.metrics?.let(metric.value)) })
        .mapNotNull { (name, isMe, value) -> value?.let { Triple(name, isMe, it) } }
        .sortedByDescending { it.third }
    // 보이는 값이 같으면 같은 등수다
    val ranked = entries.map { (name, isMe, value) ->
        val shown = metric.format.format(value)
        Ranked(name, isMe, value, rank = entries.indexOfFirst { metric.format.format(it.third) == shown } + 1)
    }
    val top = entries.firstOrNull()?.third?.takeIf { it > 0 } ?: 1.0

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        val metricLabel = stringResource(metric.label)
        OvalitPickerTitle(
            title = { OvalitText(text = stringResource(Res.string.friends_title), style = OvalitTheme.typography.bodyStrong) },
            picker = {
                OvalitPickerButton(
                    text = metricLabel,
                    onClickLabel = stringResource(Res.string.friends_metric_button, metricLabel),
                    onClick = { choosing = true },
                )
            },
        )
        Spacer(Modifier.height(SectionTitleGap))
        // 순위와 값 칸은 가장 긴 글자에 맞추고, 이름은 넘치면 말줄임표로 자른다. 순위와 값 폭을 박아 두면 글씨를
        // 키웠을 때 순위 "10"이 꺾이고 값이 잘렸다.
        val typography = OvalitTheme.typography
        val widths = RankWidths(
            rank = rememberWidestWidth(ranked.map { it.rank.toString() }, typography.metricS),
            // 짧은 이름만 있으면 이름 칸을 줄여 막대가 이름 가까이에서 시작한다. 길면 [NameWidth]에서 자른다.
            name = rememberWidestWidth(ranked.map { it.name }, typography.caption.copy(fontWeight = FontWeight.SemiBold))
                .coerceAtMost(NameWidth),
            value = rememberWidestWidth(ranked.map { metric.format.valueText(it.value) }, typography.metricS.copy(fontWeight = FontWeight.Bold)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BarRowGap)) {
            ranked.forEach { RankRow(it, metric, top, widths) }
        }
    }

    if (choosing) {
        OvalitBottomSheet(title = stringResource(Res.string.friends_choose_metric), onDismiss = { choosing = false }) {
            Column(modifier = Modifier.selectableGroup()) {
                RankableMetrics.forEach { option ->
                    OvalitSheetOption(
                        text = stringResource(option.label),
                        selected = option == metric,
                        onClick = {
                            metricName = option.name
                            choosing = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RankRow(entry: Ranked, metric: FixedMetric, top: Double, widths: RankWidths) {
    val colors = OvalitTheme.colors
    val strong = if (entry.isMe) colors.t1 else colors.t2

    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
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
            // 사용자 요청(2026-09-27): 라이벌·나와 비교 줄처럼 작은 글자로 둔다. 본문 크기로 두면 이 칸만 줄이 크고 넓었다.
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
