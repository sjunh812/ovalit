package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.ProfileSummary
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
import com.ovalit.core.ui.rememberFitsOnOneLine
import com.ovalit.core.ui.rememberFittingStyle
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
import com.ovalit.feature.profile.resources.profile_competitive_matches
import com.ovalit.feature.profile.resources.profile_competitive_record
import com.ovalit.feature.profile.resources.profile_most_kills
import com.ovalit.feature.profile.resources.profile_per_match
import com.ovalit.feature.profile.resources.profile_per_match_value
import com.ovalit.feature.profile.resources.profile_play_time
import com.ovalit.feature.profile.resources.profile_recent
import com.ovalit.feature.profile.resources.profile_recent_all
import com.ovalit.feature.profile.resources.profile_shots_body
import com.ovalit.feature.profile.resources.profile_shots_caption
import com.ovalit.feature.profile.resources.profile_shots_head
import com.ovalit.feature.profile.resources.profile_shots_leg
import com.ovalit.feature.profile.resources.profile_shots_title
import com.ovalit.feature.profile.resources.profile_stats_title
import com.ovalit.feature.profile.resources.profile_weapon_kill_share
import com.ovalit.feature.profile.resources.profile_weapons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val TierEmblemBoxSize = 56.dp
private val TierEmblemSize = 42.dp
private val AgentFaceSize = 48.dp
private const val SHOWN_AGENTS = 3

/**
 * 이번 액트 경쟁전의 지금 티어와 승패입니다. 목업처럼 한 단계 밝은 면에 올려 화면의 첫 덩어리로 둡니다. 이 화면에서
 * 면을 까는 곳은 여기뿐입니다.
 */
@Composable
internal fun TierCard(record: CompetitiveRecord, catalog: ContentCatalog, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val tier = record.currentTier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.raised, RoundedCornerShape(14.dp))
            .padding(OvalitSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tier != null) {
            Box(
                modifier = Modifier.size(TierEmblemBoxSize).background(colors.fill, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                TierEmblem(tier, Modifier.size(TierEmblemSize))
            }
            Spacer(Modifier.width(OvalitSpacing.lg))
        }
        Column(modifier = Modifier.weight(1f)) {
            OvalitText(
                text = tier?.let { catalog.tiers[it] } ?: stringResource(Res.string.profile_competitive),
                style = OvalitTheme.typography.titleM,
            )
            Spacer(Modifier.height(2.dp))
            // 글자를 키워 한 줄에 안 들어가면 승패가 통째로 다음 줄로 내려간다. 점은 줄 끝에 두지 않는다.
            val caption = OvalitTheme.typography.caption
            SeparatedRow(
                items = listOf(
                    { OvalitText(stringResource(Res.string.profile_competitive_matches, record.matches), style = caption, color = colors.t2) },
                    { OvalitText(stringResource(Res.string.profile_competitive_record, record.wins, record.losses), style = caption, color = colors.t2) },
                ),
                separator = { OvalitText(text = " · ", style = caption, color = colors.t2) },
            )
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            OvalitText(text = stringResource(Res.string.column_win_rate), style = OvalitTheme.typography.caption, color = colors.t3)
            // 티어 이름보다 크면 칸의 주인공이 승률로 바뀐다. 같은 크기에 숫자 폭만 고정한다.
            OvalitText(
                text = percentText(record.winRate),
                style = StatValueStyle(),
                color = winRateColor(record.winRate),
            )
        }
    }
}

/**
 * 이번 액트 합계입니다. 위 줄은 피해량, K/D, 전투점수이고 아래 줄은 최다 킬, 판당 K/D/A, 플레이 시간입니다. 좁은 화면에서
 * 글자를 키우면 같은 순서로 두 칸씩 놓습니다.
 */
@Composable
internal fun StatsSection(summary: ProfileSummary, modifier: Modifier = Modifier) {
    val metrics = summary.metrics
    val main = listOf(FixedMetric.DAMAGE, FixedMetric.KD, FixedMetric.COMBAT_SCORE).map { metric ->
        stringResource(metric.label) to metric.value(metrics)?.let { metric.format.valueText(it) }
    }
    val records = listOf(
        stringResource(Res.string.profile_most_kills) to summary.mostKills?.toString(),
        stringResource(Res.string.profile_per_match) to perMatchText(metrics),
        stringResource(Res.string.profile_play_time) to playTimeText(summary.playTimeMillis),
    )

    Section(modifier = modifier, divider = false) {
        SectionTitle(title = stringResource(Res.string.profile_stats_title))
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 여섯 칸의 이름과 숫자를 한 크기로 맞춘다. 칸마다 따로 줄이면 "판당 K/D/A"처럼 긴 칸만 작아진다.
            // 세 칸에 가장 작게 줄여도 안 들어가면 두 칸씩 놓는다. 숫자가 잘리는 것보다 줄이 하나 느는 게 낫다.
            val cells = main + records
            val labels = cells.map { it.first }
            val values = cells.map { it.second ?: NO_VALUE }
            val valueStyle = StatValueStyle()
            val threeWide = maxWidth / STAT_COLUMNS - OvalitSpacing.sm
            val threeValue = rememberFittingStyle(values, valueStyle, threeWide, min = STAT_MIN_SIZE)
            val columns = if (rememberFitsOnOneLine(values.map(::AnnotatedString), threeValue, threeWide)) STAT_COLUMNS else 2
            val cellWidth = maxWidth / columns - OvalitSpacing.sm
            val styles = StatStyles(
                label = rememberFittingStyle(labels, OvalitTheme.typography.caption, cellWidth),
                value = rememberFittingStyle(values, valueStyle, cellWidth, min = STAT_MIN_SIZE),
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cells.chunked(columns).forEach { row -> StatRow(row, styles) }
            }
        }
    }
}

private const val STAT_COLUMNS = 3
private val STAT_MIN_SIZE = 11.sp

private class StatStyles(val label: TextStyle, val value: TextStyle)

// 홈의 "판당 17.2 / 11.5 / 6.3"과 같은 자릿수다. 칸이 좁아서 빗금 양옆 공백만 뺐다.
@Composable
private fun perMatchText(metrics: MatchMetrics): String? {
    if (metrics.matches == 0) return null
    val (kills, deaths, assists) = listOf(metrics.kills, metrics.deaths, metrics.assists)
        .map { MetricFormat.ONE_DECIMAL.format(it.toDouble() / metrics.matches) }
    return stringResource(Res.string.profile_per_match_value, kills, deaths, assists)
}

@Composable
private fun StatRow(cells: List<Pair<String, String?>>, styles: StatStyles) {
    val colors = OvalitTheme.colors
    Row {
        cells.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f).padding(end = OvalitSpacing.sm).semantics(mergeDescendants = true) {}) {
                OvalitText(
                    text = label,
                    style = styles.label,
                    color = colors.t2,
                    maxLines = 1,
                    autoSize = shrinkToFit(styles.label.fontSize, min = 7.sp),
                )
                Spacer(Modifier.height(4.dp))
                OvalitText(
                    text = value ?: NO_VALUE,
                    style = styles.value,
                    color = if (value != null) colors.t1 else colors.t3,
                    maxLines = 1,
                    autoSize = shrinkToFit(styles.value.fontSize, min = STAT_MIN_SIZE),
                )
            }
        }
    }
}

// 목업처럼 티어 이름과 같은 크기다. 홈의 큰 지표 숫자를 그대로 쓰면 여섯 칸이 한꺼번에 소리친다.
@Composable
private fun StatValueStyle(): TextStyle =
    OvalitTheme.typography.metricM.copy(fontSize = OvalitTheme.typography.titleM.fontSize, lineHeight = 24.sp)

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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 한 칸이라도 탄 수가 비율 옆에 안 들어가면 세 칸 모두 탄 수를 아래로 내린다. 한 칸만 내리면 그 칸만 높아진다.
            val typography = OvalitTheme.typography
            val lines = parts.map { part ->
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(percentText(part.count.toDouble() / shots.total)) }
                    append(part.count.withThousands())
                }
            }
            val stacked = !rememberFitsOnOneLine(lines, typography.label, maxWidth / parts.size, extra = ShotGap)
            Row {
                parts.forEach { part -> ShotLegend(part, shots.total, stacked, Modifier.weight(1f)) }
            }
        }
    }
}

private val ShotGap = 4.dp

private class ShotPart(val label: StringResource, val count: Int, val color: Color)

@Composable
private fun ShotLegend(part: ShotPart, total: Int, stacked: Boolean, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        OvalitText(text = stringResource(part.label), style = typography.caption, color = colors.t2)
        Spacer(Modifier.height(3.dp))
        // 비율과 탄 수는 따로 두고 칸이 좁으면 탄 수를 통째로 다음 줄에 내린다. 한 글줄로 두면 "3,737"이 쉼표에서 갈라진다.
        SeparatedRow(
            items = listOf(
                { OvalitText(text = percentText(part.count.toDouble() / total), style = typography.label.copy(fontWeight = FontWeight.SemiBold)) },
                { OvalitText(text = part.count.withThousands(), style = typography.label, color = colors.t3) },
            ),
            separator = { Spacer(Modifier.width(ShotGap)) },
            stacked = stacked,
        )
    }
}

/** 많이 뛴 요원 셋입니다. 누르면 S7로 갑니다. 승률은 S7처럼 5판 이상 뛴 요원만 띄웁니다. */
@Composable
internal fun AgentsSection(report: AgentReport, catalog: ContentCatalog, onOpen: () -> Unit) {
    val shown = report.agents.take(SHOWN_AGENTS)
    if (shown.isEmpty()) return

    // 역할 비중("타격대 78%")은 바로 위 머리에 있어서 여기 다시 적지 않는다
    Section(modifier = Modifier.clickable(role = SemanticsRole.Button, onClick = onOpen)) {
        SectionTitle(title = stringResource(Res.string.profile_agents), chevron = true)
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 한 칸이라도 승률이 판 수 옆에 안 들어가면 모든 칸의 승률을 아래로 내린다
            val lines = shown.map { agent ->
                val matches = stringResource(Res.string.agents_matches, agent.matches)
                AnnotatedString(if (agent.isMeasurable) matches + AGENT_SEPARATOR + percentText(agent.winRate) else matches)
            }
            val tileWidth = (maxWidth - AgentTileGap * (SHOWN_AGENTS - 1)) / SHOWN_AGENTS
            val stacked = !rememberFitsOnOneLine(lines, OvalitTheme.typography.caption, tileWidth)
            Row(horizontalArrangement = Arrangement.spacedBy(AgentTileGap)) {
                shown.forEach { AgentTile(it, catalog, stacked, Modifier.weight(1f)) }
                repeat(SHOWN_AGENTS - shown.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val AgentTileGap = 10.dp
private const val AGENT_SEPARATOR = "\u00a0·\u00a0"

@Composable
private fun AgentTile(agent: AgentStats, catalog: ContentCatalog, stacked: Boolean, modifier: Modifier) {
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
            separator = { OvalitText(text = AGENT_SEPARATOR, style = caption, color = colors.t5) },
            stacked = stacked,
        )
    }
}

/** S6 위쪽 세 줄과 같은 무기입니다. 이번 액트 킬 중 그 무기로 낸 비중을 붙입니다. 누르면 S6으로 갑니다. */
@Composable
internal fun WeaponsSection(report: WeaponReport, catalog: ContentCatalog, onOpen: () -> Unit) {
    val shown = report.highlights.map { it.act }
    if (shown.isEmpty()) return

    Section(modifier = Modifier.clickable(role = SemanticsRole.Button, onClick = onOpen)) {
        SectionTitle(title = stringResource(Res.string.profile_weapons), chevron = true)
        Spacer(Modifier.height(12.dp))
        // 위 요원 칸과 같은 세 칸 격자에 놓는다. 무기가 셋이 안 되면 빈칸을 남겨 요원과 줄을 맞춘다.
        Row(horizontalArrangement = Arrangement.spacedBy(AgentTileGap)) {
            shown.forEach { WeaponTile(it, report.kills, catalog, Modifier.weight(1f)) }
            repeat(SHOWN_AGENTS - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun WeaponTile(weapon: WeaponStats, totalKills: Int, catalog: ContentCatalog, modifier: Modifier) {
    val name = catalog.weaponName(weapon.weapon)
    val share = if (totalKills > 0) weapon.kills.toDouble() / totalKills else null

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        WeaponThumb(weapon.weapon, name, width = 76.dp, height = 30.dp)
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
// 티어 카드 바로 아래 통계는 카드가 경계를 대신해서 선을 긋지 않는다.
@Composable
private fun Section(modifier: Modifier = Modifier, divider: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    if (divider) OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
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
