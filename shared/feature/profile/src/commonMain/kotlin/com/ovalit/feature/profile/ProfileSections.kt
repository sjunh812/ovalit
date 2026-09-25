package com.ovalit.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.CompetitiveRecord
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.HIGHLIGHTED_WEAPONS
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.ProfileSummary
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.TierEmblem
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.recentMatchTimeLabel
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.agents_matches
import com.ovalit.feature.profile.resources.column_win_rate
import com.ovalit.feature.profile.resources.duration_hours
import com.ovalit.feature.profile.resources.duration_hours_minutes
import com.ovalit.feature.profile.resources.duration_minutes
import com.ovalit.feature.profile.resources.profile_agents
import com.ovalit.feature.profile.resources.profile_competitive
import com.ovalit.feature.profile.resources.profile_competitive_record
import com.ovalit.feature.profile.resources.profile_kills_per_match
import com.ovalit.feature.profile.resources.profile_most_kills
import com.ovalit.feature.profile.resources.profile_play_time
import com.ovalit.feature.profile.resources.profile_recent
import com.ovalit.feature.profile.resources.profile_recent_all
import com.ovalit.feature.profile.resources.profile_role_share
import com.ovalit.feature.profile.resources.profile_shots_body
import com.ovalit.feature.profile.resources.profile_shots_caption
import com.ovalit.feature.profile.resources.profile_shots_head
import com.ovalit.feature.profile.resources.profile_shots_leg
import com.ovalit.feature.profile.resources.profile_shots_title
import com.ovalit.feature.profile.resources.profile_stats_caption
import com.ovalit.feature.profile.resources.profile_stats_title
import com.ovalit.feature.profile.resources.profile_tier_best
import com.ovalit.feature.profile.resources.profile_tier_first
import com.ovalit.feature.profile.resources.profile_tier_flow_description
import com.ovalit.feature.profile.resources.profile_weapon_kill_share
import com.ovalit.feature.profile.resources.profile_weapons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val TierEmblemSize = 52.dp
private val TierFlowHeight = 56.dp
private val AgentFaceSize = 48.dp
private const val SHOWN_AGENTS = 3

/**
 * 이번 액트 경쟁전의 지금 티어와 승패, 판마다의 티어 흐름입니다. 보여줄 수 없는 건 앱을 안 쓰는 사람의 티어
 * 추이이고, 내 티어 추이는 보여줘도 됩니다.
 */
@Composable
internal fun TierSection(record: CompetitiveRecord, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val tier = record.currentTier

    Section {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tier != null) {
                TierEmblem(tier, Modifier.size(TierEmblemSize))
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                OvalitText(
                    text = tier?.let { catalog.tiers[it] } ?: stringResource(Res.string.profile_competitive),
                    style = OvalitTheme.typography.titleM,
                )
                Spacer(Modifier.height(2.dp))
                OvalitText(
                    text = stringResource(Res.string.profile_competitive_record, record.matches, record.wins, record.losses),
                    style = OvalitTheme.typography.caption,
                    color = colors.t2,
                )
            }
            Spacer(Modifier.width(OvalitSpacing.md))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                OvalitText(text = stringResource(Res.string.column_win_rate), style = OvalitTheme.typography.caption, color = colors.t3)
                // 티어 이름보다 크면 칸의 주인공이 승률로 바뀐다. 같은 크기에 숫자 폭만 고정한다.
                OvalitText(
                    text = percentText(record.winRate),
                    style = OvalitTheme.typography.metricM.copy(fontSize = OvalitTheme.typography.titleM.fontSize),
                    color = winRateColor(record.winRate),
                )
            }
        }
        if (record.tiers.distinct().size > 1) {
            Spacer(Modifier.height(18.dp))
            TierFlow(record.tiers, catalog)
        }
    }
}

// RR은 API에 없어서 티어가 바뀐 판에서만 선이 꺾인다. 그래서 막대가 아니라 계단으로 긋는다.
@Composable
private fun TierFlow(tiers: List<Int>, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val first = catalog.tiers[tiers.first()]
    val best = catalog.tiers[tiers.max()]
    val current = catalog.tiers[tiers.last()]
    val description = if (first != null && best != null && current != null) {
        stringResource(Res.string.profile_tier_flow_description, first, best, current)
    } else {
        null
    }
    val guide = colors.lineWeak
    val line = colors.t1

    Column(modifier = Modifier.clearAndSetSemantics { description?.let { contentDescription = it } }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(TierFlowHeight)) {
            val dot = 3.5.dp.toPx()
            val low = tiers.min()
            val high = tiers.max()
            val top = dot
            val bottom = size.height - dot
            val left = dot
            val right = size.width - dot
            fun x(index: Int) = left + (right - left) * index / tiers.lastIndex
            fun y(tier: Int) = bottom - (bottom - top) * (tier - low) / (high - low)

            tiers.distinct().forEach { level ->
                drawLine(guide, Offset(0f, y(level)), Offset(size.width, y(level)), strokeWidth = 1.dp.toPx())
            }
            val path = Path().apply {
                moveTo(x(0), y(tiers.first()))
                for (index in 1..tiers.lastIndex) {
                    lineTo(x(index), y(tiers[index - 1]))
                    lineTo(x(index), y(tiers[index]))
                }
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(line, radius = dot, center = Offset(x(tiers.lastIndex), y(tiers.last())))
        }
        if (first != null) {
            Spacer(Modifier.height(8.dp))
            Row {
                OvalitText(
                    text = stringResource(Res.string.profile_tier_first, first),
                    modifier = Modifier.weight(1f),
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                )
                // 지금이 가장 높으면 위 칸의 티어 이름과 같은 말이라 적지 않는다
                if (best != null && tiers.max() != tiers.last()) {
                    OvalitText(
                        text = stringResource(Res.string.profile_tier_best, best),
                        style = OvalitTheme.typography.caption,
                        color = colors.t3,
                    )
                }
            }
        }
    }
}

/** 홈 고정 칸과 같은 순서로 전투점수, K/D, 피해량을 두고 아래 줄에 경기 기록을 둡니다. */
@Composable
internal fun StatsSection(summary: ProfileSummary, matches: Int) {
    val metrics = summary.metrics
    val fixed = listOf(FixedMetric.COMBAT_SCORE, FixedMetric.KD, FixedMetric.DAMAGE).map { metric ->
        stringResource(metric.label) to metric.value(metrics)?.let { metric.format.valueText(it) }
    }
    val killsPerMatch = if (metrics.matches > 0) metrics.kills.toDouble() / metrics.matches else null
    val records = listOf(
        stringResource(Res.string.profile_most_kills) to summary.mostKills?.toString(),
        stringResource(Res.string.profile_kills_per_match) to killsPerMatch?.let { MetricFormat.ONE_DECIMAL.format(it) },
        stringResource(Res.string.profile_play_time) to playTimeText(summary.playTimeMillis),
    )
    val queue = stringResource(QueueFilter.COMPETITIVE_AND_UNRATED.label)

    Section {
        SectionTitle(
            title = stringResource(Res.string.profile_stats_title),
            caption = stringResource(Res.string.profile_stats_caption, queue, matches),
        )
        Spacer(Modifier.height(14.dp))
        StatRow(fixed)
        Spacer(Modifier.height(16.dp))
        StatRow(records)
    }
}

@Composable
private fun StatRow(cells: List<Pair<String, String?>>) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    Row {
        cells.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f).padding(end = OvalitSpacing.sm).semantics(mergeDescendants = true) {}) {
                OvalitText(
                    text = label,
                    style = typography.caption,
                    color = colors.t2,
                    maxLines = 1,
                    autoSize = shrinkToFit(typography.caption.fontSize, min = 7.sp),
                )
                Spacer(Modifier.height(4.dp))
                OvalitText(
                    text = value ?: NO_VALUE,
                    style = typography.metricM,
                    color = if (value != null) colors.t1 else colors.t3,
                    maxLines = 1,
                    autoSize = shrinkToFit(typography.metricM.fontSize, min = 12.sp),
                )
            }
        }
    }
}

// 열 시간이 넘으면 분은 버린다. 칸이 좁고, 그만큼 뛰었으면 분까지 볼 일이 없다.
@Composable
private fun playTimeText(millis: Long): String {
    val minutes = (millis / 60_000).toInt()
    val hours = minutes / 60
    return when {
        hours >= 10 -> stringResource(Res.string.duration_hours, hours)
        hours > 0 -> stringResource(Res.string.duration_hours_minutes, hours, minutes % 60)
        else -> stringResource(Res.string.duration_minutes, minutes)
    }
}

/**
 * 맞힌 탄이 머리, 몸, 다리에 어떻게 나뉘었는지입니다. 홈의 헤드샷과 같은 맞힌 탄 기준입니다. 강조는 색이 아니라
 * 밝기로 해서 머리가 가장 밝습니다.
 */
@Composable
internal fun ShotsSection(shots: Shots) {
    if (shots.total == 0) return
    val colors = OvalitTheme.colors
    val parts = listOf(
        ShotPart(Res.string.profile_shots_head, shots.head, colors.t1),
        ShotPart(Res.string.profile_shots_body, shots.body, colors.t4),
        ShotPart(Res.string.profile_shots_leg, shots.leg, colors.bar),
    )

    Section {
        SectionTitle(
            title = stringResource(Res.string.profile_shots_title),
            caption = stringResource(Res.string.profile_shots_caption, shots.total.withThousands()),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            parts.filter { it.count > 0 }.forEach { part ->
                Box(Modifier.weight(part.count.toFloat()).fillMaxHeight().background(part.color))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            parts.forEach { part -> ShotLegend(part, shots.total, Modifier.weight(1f)) }
        }
    }
}

private class ShotPart(val label: StringResource, val count: Int, val color: Color)

@Composable
private fun ShotLegend(part: ShotPart, total: Int, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val percent = percentText(part.count.toDouble() / total)
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(part.color))
            Spacer(Modifier.width(6.dp))
            OvalitText(text = stringResource(part.label), style = OvalitTheme.typography.caption, color = colors.t2)
        }
        Spacer(Modifier.height(3.dp))
        // 비율과 탄 수를 한 줄에 두면 좁은 칸에서 "3,737"이 쉼표에서 갈라진다. 그래서 줄을 나눈다.
        OvalitText(
            text = percent,
            style = OvalitTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
        )
        OvalitText(
            text = part.count.withThousands(),
            style = OvalitTheme.typography.caption,
            color = colors.t3,
            maxLines = 1,
            autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize),
        )
    }
}

/** 많이 뛴 요원 셋입니다. 누르면 S7로 갑니다. 승률은 S7처럼 5판 이상 뛴 요원만 띄웁니다. */
@Composable
internal fun AgentsSection(report: AgentReport, catalog: ContentCatalog, onOpen: () -> Unit) {
    val shown = report.agents.take(SHOWN_AGENTS)
    if (shown.isEmpty()) return
    val main = report.roles.firstOrNull()
    val share = main?.let {
        val rate = it.rounds.toDouble() / report.roles.sumOf { role -> role.rounds }
        stringResource(Res.string.profile_role_share, stringResource(it.role.label), percentText(rate))
    }

    Section(modifier = Modifier.clickable(role = SemanticsRole.Button, onClick = onOpen)) {
        SectionTitle(title = stringResource(Res.string.profile_agents), caption = share, chevron = true)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            shown.forEach { AgentTile(it, catalog, Modifier.weight(1f)) }
            repeat(SHOWN_AGENTS - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AgentTile(agent: AgentStats, catalog: ContentCatalog, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val name = catalog.agentName(agent.agent)
    val matches = stringResource(Res.string.agents_matches, agent.matches)
    val winRate = if (agent.isMeasurable) percentText(agent.winRate) else null
    val winColor = winRateColor(agent.winRate)

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        AgentImage(agent.agent, name, Modifier.size(AgentFaceSize).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.height(8.dp))
        OvalitText(text = name, style = OvalitTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        // 좁은 칸에서 승률이 다음 줄로 내려가도 줄 끝에 점이 남지 않게 SeparatedRow로 잇는다
        val caption = OvalitTheme.typography.caption
        SeparatedRow(
            items = listOfNotNull<@Composable () -> Unit>(
                { OvalitText(text = matches, style = caption, color = colors.t3) },
                winRate?.let { rate -> { OvalitText(text = rate, style = caption, color = winColor) } },
            ),
            separator = { OvalitText(text = "\u00a0·\u00a0", style = caption, color = colors.t5) },
        )
    }
}

/** S6 위쪽 두 줄과 같은 무기입니다. 이번 액트 킬 중 그 무기로 낸 비중을 붙입니다. 누르면 S6으로 갑니다. */
@Composable
internal fun WeaponsSection(report: WeaponReport, catalog: ContentCatalog, onOpen: () -> Unit) {
    val shown = report.highlights.map { it.act }
    if (shown.isEmpty()) return

    Section(modifier = Modifier.clickable(role = SemanticsRole.Button, onClick = onOpen)) {
        SectionTitle(title = stringResource(Res.string.profile_weapons), chevron = true)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            shown.forEach { WeaponTile(it, report.kills, catalog, Modifier.weight(1f)) }
            repeat(HIGHLIGHTED_WEAPONS - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun WeaponTile(weapon: WeaponStats, totalKills: Int, catalog: ContentCatalog, modifier: Modifier) {
    val name = catalog.weaponName(weapon.weapon)
    val share = if (totalKills > 0) weapon.kills.toDouble() / totalKills else null

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        WeaponThumb(weapon.weapon, name, width = 76.dp, height = 42.dp)
        Spacer(Modifier.height(8.dp))
        OvalitText(text = name, style = OvalitTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        OvalitText(
            text = stringResource(Res.string.profile_weapon_kill_share, percentText(share)),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

/** S5와 같은 짧은 줄 세 개입니다. 내 경기라 누르면 S3가 열리고, "전체 보기"는 경기 탭으로 갑니다. */
@Composable
internal fun RecentMatchesSection(
    uiState: ProfileUiState.Success,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
) {
    if (uiState.recentMatches.isEmpty()) return
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = stringResource(Res.string.profile_recent),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        if (uiState.hasMoreMatches) {
            OvalitTextButton(text = stringResource(Res.string.profile_recent_all), onClick = onOpenMatches)
        }
    }
    uiState.recentMatches.forEachIndexed { index, match ->
        if (index > 0) OvalitDivider(Modifier.padding(start = 67.dp), color = OvalitTheme.colors.lineWeak)
        MatchRow(
            match = match,
            catalog = uiState.catalog,
            timeLabel = recentMatchTimeLabel(match.startedAt, uiState.now, uiState.timeZone),
            style = MatchRowStyle.COMPACT,
            onClick = { onOpenMatch(match.id) },
        )
    }
}

// 섹션마다 위에 선을 긋는다. 누를 수 있는 섹션은 [modifier]로 clickable을 넘긴다. 그러면 선 아래 전체가 눌린다.
@Composable
private fun Section(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 18.dp, bottom = 20.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(title: String, caption: String? = null, chevron: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OvalitText(text = title, modifier = Modifier.weight(1f), style = OvalitTheme.typography.bodyStrong)
        if (caption != null) {
            OvalitText(text = caption, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t3)
        }
        if (chevron) {
            Spacer(Modifier.width(OvalitSpacing.xs))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 16.dp)
        }
    }
}
