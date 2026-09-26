package com.ovalit.core.ui

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
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.CompetitiveRecord
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.ProfileSummary
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.agents_matches
import com.ovalit.core.ui.resources.agents_record
import com.ovalit.core.ui.resources.column_win_rate
import com.ovalit.core.ui.resources.duration_hours
import com.ovalit.core.ui.resources.duration_hours_minutes
import com.ovalit.core.ui.resources.duration_minutes
import com.ovalit.core.ui.resources.kda_counts
import com.ovalit.core.ui.resources.profile_aces
import com.ovalit.core.ui.resources.profile_agents
import com.ovalit.core.ui.resources.profile_clutch_value
import com.ovalit.core.ui.resources.profile_clutches
import com.ovalit.core.ui.resources.profile_competitive
import com.ovalit.core.ui.resources.profile_competitive_matches
import com.ovalit.core.ui.resources.profile_competitive_record
import com.ovalit.core.ui.resources.profile_count
import com.ovalit.core.ui.resources.profile_kda
import com.ovalit.core.ui.resources.profile_most_kills
import com.ovalit.core.ui.resources.profile_per_match
import com.ovalit.core.ui.resources.profile_play_time
import com.ovalit.core.ui.resources.profile_shots_body
import com.ovalit.core.ui.resources.profile_shots_caption
import com.ovalit.core.ui.resources.profile_shots_head
import com.ovalit.core.ui.resources.profile_shots_leg
import com.ovalit.core.ui.resources.profile_shots_title
import com.ovalit.core.ui.resources.profile_stats_title
import com.ovalit.core.ui.resources.profile_weapon_damage
import com.ovalit.core.ui.resources.profile_weapon_headshot
import com.ovalit.core.ui.resources.profile_weapon_kills
import com.ovalit.core.ui.resources.profile_weapons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// 내 프로필과 S5 친구 프로필이 같이 쓰는 덩어리들이다. 한쪽만 고치면 두 프로필의 같은 칸이 다르게 보인다.

private val TierEmblemBoxSize = 56.dp
private val TierEmblemSize = 42.dp
private val AgentFaceSize = 48.dp
private const val SHOWN_TILES = 3

/**
 * 이번 액트 경쟁전의 지금 티어와 승패입니다. 목업처럼 한 단계 밝은 면에 올려 화면의 첫 덩어리로 둡니다. 이 화면에서
 * 면을 까는 곳은 여기뿐입니다.
 */
@Composable
fun ProfileTierCard(record: CompetitiveRecord, catalog: ContentCatalog, modifier: Modifier = Modifier) {
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
 * 이번 액트 합계입니다. 피해량, K/D, 전투점수, 최다 킬, KDA, 플레이 시간, 에이스, 클러치 순서로 한 줄에 세 칸씩
 * 놓습니다. 좁은 화면에서 글자를 키우면 같은 순서로 두 칸씩 놓습니다. 내 프로필과 S5가 같이 씁니다.
 */
@Composable
fun ProfileStatsSection(summary: ProfileSummary, modifier: Modifier = Modifier) {
    val metrics = summary.metrics
    val main = listOf(FixedMetric.DAMAGE, FixedMetric.KD, FixedMetric.COMBAT_SCORE).map { metric ->
        StatCell(stringResource(metric.label), metric.value(metrics)?.let { metric.format.valueText(it) })
    }
    val records = listOf(
        StatCell(stringResource(Res.string.profile_most_kills), summary.mostKills?.toString()),
        // KDA 숫자만으로는 몇 킬 몇 데스인지 모른다. 판당 K/D/A를 그 밑에 붙인다.
        StatCell(
            label = stringResource(Res.string.profile_kda),
            value = metrics.kda?.let { MetricFormat.TWO_DECIMALS.format(it) },
            detail = perMatchText(metrics)?.let { stringResource(Res.string.profile_per_match, it) },
        ),
        StatCell(stringResource(Res.string.profile_play_time), playTimeText(summary.playTimeMillis)),
    )
    val highlights = summary.highlights
    val scenes = listOf(
        StatCell(stringResource(Res.string.profile_aces), stringResource(Res.string.profile_count, highlights.aces)),
        // 나만 남은 라운드가 한 번도 없었으면 0번 중 0번이 아니라 비워 둔다
        StatCell(
            stringResource(Res.string.profile_clutches),
            highlights.clutchAttempts.takeIf { it > 0 }?.let { stringResource(Res.string.profile_clutch_value, it, highlights.clutches) },
        ),
    )

    ProfileSection(modifier = modifier, divider = false) {
        ProfileSectionTitle(title = stringResource(Res.string.profile_stats_title))
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 모든 칸의 이름과 숫자를 한 크기로 맞춘다. 칸마다 따로 줄이면 "4번 중 2번"처럼 긴 칸만 작아진다.
            // 세 칸에 가장 작게 줄여도 안 들어가면 두 칸씩 놓는다. 숫자가 잘리는 것보다 줄이 하나 느는 게 낫다.
            val cells = main + records + scenes
            val labels = cells.map { it.label }
            val values = cells.map { it.value ?: NO_VALUE }
            val valueStyle = StatValueStyle()
            val threeWide = maxWidth / STAT_COLUMNS - OvalitSpacing.sm
            val threeValue = rememberFittingStyle(values, valueStyle, threeWide, min = STAT_MIN_SIZE)
            val columns = if (rememberFitsOnOneLine(values.map(::AnnotatedString), threeValue, threeWide)) STAT_COLUMNS else 2
            val cellWidth = maxWidth / columns - OvalitSpacing.sm
            val styles = StatStyles(
                label = rememberFittingStyle(labels, OvalitTheme.typography.caption, cellWidth),
                value = rememberFittingStyle(values, valueStyle, cellWidth, min = STAT_MIN_SIZE),
                detail = rememberFittingStyle(cells.mapNotNull { it.detail }, OvalitTheme.typography.caption, cellWidth),
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cells.chunked(columns).forEach { row -> StatRow(row, columns, styles) }
            }
        }
    }
}

private const val STAT_COLUMNS = 3
private val STAT_MIN_SIZE = 11.sp

private class StatStyles(val label: TextStyle, val value: TextStyle, val detail: TextStyle)

/** @property detail 숫자 밑에 작게 붙이는 풀이입니다. KDA 밑의 판당 K/D/A가 그렇습니다. */
private class StatCell(val label: String, val value: String?, val detail: String? = null)

// 홈의 "판당 17.2 / 11.5 / 6.3"과 같은 자릿수다. 칸이 좁아서 빗금 양옆 공백만 뺐다.
@Composable
private fun perMatchText(metrics: MatchMetrics): String? {
    if (metrics.matches == 0) return null
    val (kills, deaths, assists) = listOf(metrics.kills, metrics.deaths, metrics.assists)
        .map { MetricFormat.ONE_DECIMAL.format(it.toDouble() / metrics.matches) }
    return stringResource(Res.string.kda_counts, kills, deaths, assists)
}

@Composable
private fun StatRow(cells: List<StatCell>, columns: Int, styles: StatStyles) {
    val colors = OvalitTheme.colors
    Row {
        cells.forEach { cell ->
            val value = cell.value
            Column(modifier = Modifier.weight(1f).padding(end = OvalitSpacing.sm).semantics(mergeDescendants = true) {}) {
                OvalitText(
                    text = cell.label,
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
                cell.detail?.let { detail ->
                    Spacer(Modifier.height(2.dp))
                    OvalitText(
                        text = detail,
                        style = styles.detail,
                        color = colors.t3,
                        maxLines = 1,
                        autoSize = shrinkToFit(styles.detail.fontSize, min = 7.sp),
                    )
                }
            }
        }
        // 마지막 줄이 덜 차도 칸 폭은 위 줄과 같아야 세로로 줄이 맞는다
        repeat(columns - cells.size) { Spacer(Modifier.weight(1f)) }
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
fun ProfileShotsSection(shots: Shots) {
    if (shots.total == 0) return
    val colors = OvalitTheme.colors
    val parts = listOf(
        ShotPart(Res.string.profile_shots_head, shots.head, colors.t1),
        ShotPart(Res.string.profile_shots_body, shots.body, colors.t4),
        ShotPart(Res.string.profile_shots_leg, shots.leg, colors.bar),
    )

    ProfileSection {
        ProfileSectionTitle(
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

/**
 * 많이 뛴 요원 셋입니다. 판 수와 승률, KDA를 적습니다. 승률과 KDA는 S7처럼 5판 이상 뛴 요원만 띄웁니다.
 * [onOpen]이 있으면 섹션 전체가 눌리고 S7로 갑니다. 친구 프로필은 요원 화면이 없어서 누르지 않습니다.
 */
@Composable
fun ProfileAgentsSection(report: AgentReport, catalog: ContentCatalog, onOpen: (() -> Unit)? = null) {
    val shown = report.agents.take(SHOWN_TILES)
    if (shown.isEmpty()) return

    // 역할 비중("타격대 78%")은 바로 위 머리에 있어서 여기 다시 적지 않는다
    ProfileSection(modifier = Modifier.openable(onOpen)) {
        ProfileSectionTitle(title = stringResource(Res.string.profile_agents), chevron = onOpen != null)
        Spacer(Modifier.height(12.dp))
        AgentTileRow(shown, catalog)
    }
}

/**
 * 요원 세 칸입니다. 내 프로필, S5, 홈이 같이 씁니다.
 *
 * @param showRecord 판 수 대신 "2승 1패"를 적습니다. 홈처럼 기간이 짧아 5판을 못 넘기는 요원이 많은 곳에 씁니다. 그때는
 * KDA도 판 수와 상관없이 적습니다. 그 기간의 합계라서입니다. 승률은 어디서나 5판을 넘길 때만 붙입니다.
 */
@Composable
fun AgentTileRow(agents: List<AgentStats>, catalog: ContentCatalog, showRecord: Boolean = false) {
    val shown = agents.take(SHOWN_TILES)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val caption = OvalitTheme.typography.caption
        val tileWidth = (maxWidth - TileGap * (SHOWN_TILES - 1)) / SHOWN_TILES
        // 한 칸이라도 승률이 판 수 옆에 안 들어가면 모든 칸의 승률을 아래로 내린다
        val lines = shown.map { agent ->
            val first = agentFirstLine(agent, showRecord)
            AnnotatedString(if (agent.isMeasurable) first + TILE_SEPARATOR + percentText(agent.winRate) else first)
        }
        val stacked = !rememberFitsOnOneLine(lines, caption, tileWidth)
        Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            shown.forEach { agent -> AgentTile(agent, catalog, stacked, showRecord, Modifier.weight(1f)) }
            repeat(SHOWN_TILES - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun agentFirstLine(agent: AgentStats, showRecord: Boolean): String = if (showRecord) {
    stringResource(Res.string.agents_record, agent.wins, agent.decided - agent.wins)
} else {
    stringResource(Res.string.agents_matches, agent.matches)
}

private val TileGap = 10.dp
private const val TILE_SEPARATOR = "\u00a0·\u00a0"

@Composable
private fun AgentTile(agent: AgentStats, catalog: ContentCatalog, stacked: Boolean, showRecord: Boolean, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val name = catalog.agentName(agent.agent)
    val matches = agentFirstLine(agent, showRecord)
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
            separator = { OvalitText(text = TILE_SEPARATOR, style = caption, color = colors.t5) },
            stacked = stacked,
        )
        // 합계까지 적으면 칸이 무거워서 KDA만 둔다(사용자 결정)
        agent.metrics.kda?.takeIf { showRecord || agent.isMeasurable }?.let { kda ->
            OvalitText(text = kdaRatioText(kda), style = caption, color = colors.t3, maxLines = 1)
        }
    }
}

/**
 * S6 위쪽 세 줄과 같은 무기입니다. 이번 액트 킬 수, 헤드샷, 라운드당 피해량을 적습니다. 헤드샷과 피해량은 S6처럼 표본을
 * 넘길 때만 띄웁니다. [onOpen]이 있으면 섹션 전체가 눌리고 S6으로 갑니다.
 */
@Composable
fun ProfileWeaponsSection(report: WeaponReport, catalog: ContentCatalog, onOpen: (() -> Unit)? = null) {
    val shown = report.highlights.map { it.act }
    if (shown.isEmpty()) return

    ProfileSection(modifier = Modifier.openable(onOpen)) {
        ProfileSectionTitle(title = stringResource(Res.string.profile_weapons), chevron = onOpen != null)
        Spacer(Modifier.height(12.dp))
        WeaponTileRow(shown, catalog)
    }
}

/** 무기 세 칸입니다. 내 프로필, S5, 홈이 같이 씁니다. 킬 수, 헤드샷, 라운드당 피해량을 적습니다. */
@Composable
fun WeaponTileRow(weapons: List<WeaponStats>, catalog: ContentCatalog) {
    val shown = weapons.take(SHOWN_TILES)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - TileGap * (SHOWN_TILES - 1)) / SHOWN_TILES
        val lines = shown.map { weaponLine(it) }
        // 한 칸이라도 헤드샷과 피해량이 한 줄에 안 들어가면 모든 칸에서 피해량을 아래로 내린다
        val stacked = !rememberFitsOnOneLine(
            lines.map { AnnotatedString(it.headshot + TILE_SEPARATOR + it.damage) },
            OvalitTheme.typography.caption,
            tileWidth,
        )
        // 요원 칸과 같은 세 칸 격자에 놓는다. 무기가 셋이 안 되면 빈칸을 남겨 요원과 줄을 맞춘다.
        Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            shown.forEachIndexed { index, weapon -> WeaponTile(weapon, lines[index], stacked, catalog, Modifier.weight(1f)) }
            repeat(SHOWN_TILES - shown.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private class WeaponLine(val kills: String, val headshot: String, val damage: String)

@Composable
private fun weaponLine(weapon: WeaponStats) = WeaponLine(
    kills = stringResource(Res.string.profile_weapon_kills, weapon.kills.withThousands()),
    headshot = stringResource(Res.string.profile_weapon_headshot, percentText(weapon.headshotRate?.takeIf { weapon.isMeasurable })),
    damage = stringResource(
        Res.string.profile_weapon_damage,
        weapon.damagePerRound?.takeIf { weapon.isCarriedMeasurable }?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
    ),
)

@Composable
private fun WeaponTile(weapon: WeaponStats, line: WeaponLine, stacked: Boolean, catalog: ContentCatalog, modifier: Modifier) {
    val name = catalog.weaponName(weapon.weapon)
    val caption = OvalitTheme.typography.caption
    val colors = OvalitTheme.colors

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        WeaponThumb(weapon.weapon, name, width = 76.dp, height = 30.dp)
        Spacer(Modifier.height(8.dp))
        OvalitText(text = name, style = OvalitTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        OvalitText(text = line.kills, style = caption, color = colors.t2)
        SeparatedRow(
            items = listOf(
                { OvalitText(text = line.headshot, style = caption, color = colors.t3) },
                { OvalitText(text = line.damage, style = caption, color = colors.t3) },
            ),
            separator = { OvalitText(text = TILE_SEPARATOR, style = caption, color = colors.t5) },
            stacked = stacked,
        )
    }
}

private fun Modifier.openable(onOpen: (() -> Unit)?): Modifier =
    if (onOpen != null) clickable(role = SemanticsRole.Button, onClick = onOpen) else this

/**
 * 프로필 화면의 한 덩어리입니다. 위에 선을 긋습니다. 누를 수 있는 섹션은 [modifier]로 clickable을 넘깁니다. 그러면 선
 * 아래 전체가 눌립니다. 티어 카드 바로 아래 통계는 카드가 경계를 대신해서 선을 긋지 않습니다.
 */
@Composable
fun ProfileSection(modifier: Modifier = Modifier, divider: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    if (divider) OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 18.dp, bottom = 20.dp),
        content = content,
    )
}

@Composable
fun ProfileSectionTitle(title: String, caption: String? = null, chevron: Boolean = false) {
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
