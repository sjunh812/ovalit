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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.WeeklyReport
import com.ovalit.feature.report.FriendStanding
import com.ovalit.core.ui.HeadToHeadRow
import com.ovalit.core.ui.compare
import com.ovalit.core.ui.format
import com.ovalit.feature.report.format
import com.ovalit.core.ui.label
import com.ovalit.feature.report.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.friends_choose_metric
import com.ovalit.feature.report.resources.friends_me
import com.ovalit.feature.report.resources.friends_metric_button
import com.ovalit.feature.report.resources.friends_title
import com.ovalit.feature.report.resources.rival_lead
import com.ovalit.feature.report.resources.rival_no_matches
import com.ovalit.feature.report.resources.rival_title
import com.ovalit.core.ui.valueText
import org.jetbrains.compose.resources.stringResource

// 목업대로 역할이 달라도 나란히 놓을 수 있는 세 지표만 겨룬다
private val RivalMetrics = listOf(FixedMetric.KD, FixedMetric.DAMAGE, FixedMetric.HEADSHOT_RATE)
private val ValueWidth = 40.dp

@Composable
internal fun RivalSection(
    report: WeeklyReport.Ready,
    mine: MatchMetrics,
    rival: FriendStanding,
    modifier: Modifier = Modifier,
) {
    val theirs = rival.metrics
    // 화면에 보이는 자릿수로 반올림한 값끼리 겨룬다. 1.42와 1.42를 띄워 놓고 한쪽이 앞섰다고 하면 틀려 보인다.
    val leads = if (theirs == null) 0 else RivalMetrics.count { metric -> compare(metric, mine, theirs) > 0 }

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
        Spacer(Modifier.height(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            RivalMetrics.forEach { metric -> HeadToHeadRow(metric, mine, theirs) }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = stringResource(Res.string.friends_title),
                modifier = Modifier.weight(1f),
                style = OvalitTheme.typography.bodyStrong,
            )
            val metricLabel = stringResource(metric.label)
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(
                        onClickLabel = stringResource(Res.string.friends_metric_button, metricLabel),
                        role = Role.Button,
                        onClick = { choosing = true },
                    )
                    .padding(start = OvalitSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OvalitText(text = metricLabel, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
                Spacer(Modifier.width(2.dp))
                OvalitIcon(OvalitIcons.ChevronDown, contentDescription = null, tint = OvalitTheme.colors.t3, size = 12.dp)
            }
        }
        Spacer(Modifier.height(OvalitSpacing.xs))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ranked.forEach { RankRow(it, metric, top) }
        }
    }

    if (choosing) {
        OvalitBottomSheet(title = stringResource(Res.string.friends_choose_metric), onDismiss = { choosing = false }) {
            Column(modifier = Modifier.selectableGroup()) {
                RankableMetrics.forEach { option ->
                    val selected = option == metric
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = {
                                    metricName = option.name
                                    choosing = false
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OvalitText(
                            text = stringResource(option.label),
                            modifier = Modifier.weight(1f),
                            style = if (selected) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body,
                            color = if (selected) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
                        )
                        if (selected) OvalitIcon(OvalitIcons.Check, contentDescription = null, tint = OvalitTheme.colors.t1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankRow(entry: Ranked, metric: FixedMetric, top: Double) {
    val colors = OvalitTheme.colors
    val strong = if (entry.isMe) colors.t1 else colors.t2

    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = entry.rank.toString(),
            modifier = Modifier.width(16.dp),
            style = OvalitTheme.typography.metricS,
            color = colors.t3,
        )
        OvalitText(
            text = entry.name,
            modifier = Modifier.width(60.dp),
            style = if (entry.isMe) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body,
            color = strong,
            maxLines = 1,
        )
        Box(modifier = Modifier.weight(1f).height(3.dp).background(colors.fill)) {
            // 목업대로 나만 금색이다
            Box(
                Modifier
                    .fillMaxWidth((entry.value / top).toFloat().coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(if (entry.isMe) colors.accent else colors.t4),
            )
        }
        OvalitText(
            text = metric.format.valueText(entry.value),
            modifier = Modifier.width(ValueWidth),
            style = OvalitTheme.typography.metricS.copy(fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Medium),
            color = strong,
            textAlign = TextAlign.End,
        )
    }
}
