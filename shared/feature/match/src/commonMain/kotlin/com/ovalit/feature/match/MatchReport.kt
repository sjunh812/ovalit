package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.currentMaxWidth
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Duel
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.model.duels
import com.ovalit.core.model.myWeapons
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.ProfileSectionTitle
import com.ovalit.core.ui.ShotsBreakdown
import com.ovalit.core.ui.WeaponThumb
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withThousands
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.report_assists
import com.ovalit.feature.match.resources.report_damage
import com.ovalit.feature.match.resources.report_damage_dealt
import com.ovalit.feature.match.resources.report_damage_taken
import com.ovalit.feature.match.resources.report_deaths
import com.ovalit.feature.match.resources.report_duels_title
import com.ovalit.feature.match.resources.report_headshot
import com.ovalit.feature.match.resources.report_kills
import com.ovalit.feature.match.resources.report_weapons_note
import com.ovalit.feature.match.resources.report_weapons_title
import org.jetbrains.compose.resources.stringResource

private val FaceSize = 28.dp
private val MinNameWidth = 40.dp
private val BaseCountColumn = 30.dp
private val BaseDamageColumn = 60.dp

// 큰 글씨에서 열 제목끼리 붙지 않게 열 폭도 글자 크기만큼 넓힌다. 이름 칸은 남은 폭을 쓴다.
private val CountColumn: Dp
    @Composable get() = BaseCountColumn * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
private val DamageColumn: Dp
    @Composable get() = BaseDamageColumn * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)

/**
 * S3 내 기록 탭입니다. 맞힌 부위, 상대마다 잡고 잡힌 수와 주고받은 피해, 이 판에서 쓴 무기를 둡니다. 내가 뛴 라운드만 셉니다.
 */
@Composable
internal fun MatchReport(uiState: MatchDetailUiState.Success) {
    val match = uiState.match
    val duels = remember(match) { match.duels() }
    // 사서 들기만 하고 킬을 못 낸 무기까지 두면 줄만 길어진다
    val weapons = remember(match) { match.myWeapons().filter { it.kills > 0 } }
    val shots = remember(match) { match.rounds.fold(Shots.None) { total, round -> total + round.myShots } }

    Column {
        if (shots.total > 0) {
            Box(Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg)) { ShotsBreakdown(shots) }
            Spacer(Modifier.height(OvalitSpacing.lg))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
        }
        if (duels.isNotEmpty()) Duels(duels, uiState)
        if (weapons.isNotEmpty()) {
            Spacer(Modifier.height(OvalitSpacing.sm))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
            Weapons(weapons, uiState)
        }
    }
}

// 상대마다 한 줄이다. 킬·데스·어시는 숫자로 적고, 준 피해와 받은 피해는 숫자 밑에 가장 큰 값에 견준 막대를 긋는다.
@Composable
private fun Duels(duels: List<Duel>, uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    val lines = uiState.match.players.associateBy { it.player }
    val most = duels.maxOf { maxOf(it.damageDealt, it.damageTaken) }.coerceAtLeast(1)

    ReportTitle(stringResource(Res.string.report_duels_title))
    val countColumn = CountColumn
    val damageColumn = DamageColumn
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 이름 칸이 너무 좁아지면 "…"만 남기지 않고 이름을 빼고 얼굴만 둔다. 상대 팀 안에서는 요원이 겹치지 않아 얼굴로 가릴 수 있다.
        val nameRoom = currentMaxWidth - OvalitSpacing.gutter * 2 - FaceSize - OvalitSpacing.sm - countColumn * 3 - damageColumn * 2
        val showNames = nameRoom >= MinNameWidth
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xs)) {
                Spacer(Modifier.weight(1f))
                HeaderCell(stringResource(Res.string.report_kills), countColumn)
                HeaderCell(stringResource(Res.string.report_deaths), countColumn)
                HeaderCell(stringResource(Res.string.report_assists), countColumn)
                HeaderCell(stringResource(Res.string.report_damage_dealt), damageColumn)
                HeaderCell(stringResource(Res.string.report_damage_taken), damageColumn)
            }
            duels.forEach { duel ->
                val line = lines[duel.opponent] ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .semantics(mergeDescendants = true) {}
                        .padding(horizontal = OvalitSpacing.gutter, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AgentImage(line.agent, uiState.catalog.agentName(line.agent), Modifier.size(FaceSize).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(OvalitSpacing.sm))
                    if (showNames) {
                        OvalitText(
                            text = line.riotId.substringBefore('#'),
                            modifier = Modifier.weight(1f),
                            style = OvalitTheme.typography.label,
                            color = colors.t2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    CountCell(duel.kills, emphasized = duel.kills > duel.deaths)
                    CountCell(duel.deaths, emphasized = duel.deaths > duel.kills)
                    CountCell(duel.assists, emphasized = false)
                    DamageCell(duel.damageDealt, most, colors.t1)
                    DamageCell(duel.damageTaken, most, colors.t4)
                }
            }
        }
    }
}

// 맞힌 부위와 같은 묶음 제목이다. 밑 열 제목 줄의 위 여백 4dp와 합쳐 제목과 내용 사이가 12dp다.
@Composable
private fun ReportTitle(text: String) {
    Box(Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.sm)) {
        ProfileSectionTitle(text)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    OvalitText(
        text = text,
        modifier = Modifier.width(width),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun CountCell(count: Int, emphasized: Boolean) {
    OvalitText(
        text = count.toString(),
        modifier = Modifier.width(CountColumn),
        style = OvalitTheme.typography.metricS.copy(fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium),
        color = if (emphasized) OvalitTheme.colors.t1 else OvalitTheme.colors.t3,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun DamageCell(damage: Int, most: Int, color: Color) {
    Column(modifier = Modifier.width(DamageColumn).padding(start = OvalitSpacing.sm), horizontalAlignment = Alignment.End) {
        OvalitText(text = damage.withThousands(), style = OvalitTheme.typography.metricS, color = OvalitTheme.colors.t2, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(OvalitTheme.colors.fill)) {
            Box(Modifier.fillMaxWidth(damage.toFloat() / most).fillMaxHeight().background(color))
        }
    }
}

// 이 판에서 쓴 무기다. 킬은 쓰러뜨린 무기로, 피해량은 그 무기를 들고 시작한 라운드로, 헤드샷은 그 무기로만 킬을 낸 라운드로 센다.
@Composable
private fun Weapons(weapons: List<WeaponStats>, uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    ReportTitle(stringResource(Res.string.report_weapons_title))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xs)) {
        Spacer(Modifier.weight(1f))
        HeaderCell(stringResource(Res.string.report_kills), CountColumn)
        HeaderCell(stringResource(Res.string.report_damage), DamageColumn)
        HeaderCell(stringResource(Res.string.report_headshot), DamageColumn)
    }
    weapons.forEach { weapon ->
        val name = uiState.catalog.weaponName(weapon.weapon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = OvalitSpacing.gutter, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeaponThumb(weapon.weapon, name, width = 44.dp, height = 22.dp)
            Spacer(Modifier.width(OvalitSpacing.md))
            // 글자를 키워 이름이 안 들어가면 자르지 않고 조금 줄인다. "오퍼레…"로는 무슨 무기인지 모른다.
            OvalitText(
                text = name,
                modifier = Modifier.weight(1f),
                style = OvalitTheme.typography.body,
                maxLines = 1,
                autoSize = shrinkToFit(OvalitTheme.typography.body.fontSize),
            )
            CountCell(weapon.kills, emphasized = true)
            ValueCell(weapon.damagePerRound?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE)
            ValueCell(weapon.headshotRate?.let { percentText(it) } ?: NO_VALUE)
        }
    }
    OvalitText(
        text = stringResource(Res.string.report_weapons_note),
        modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.sm),
        style = OvalitTheme.typography.caption,
        color = colors.t3,
    )
}

@Composable
private fun ValueCell(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.width(DamageColumn),
        style = OvalitTheme.typography.metricS,
        color = OvalitTheme.colors.t2,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}
