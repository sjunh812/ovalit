package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MovedMetric
import com.ovalit.core.model.WeekNote
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.format
import com.ovalit.core.ui.joinKeepingParts
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.rememberWidestWidth
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.note_agent
import com.ovalit.feature.report.resources.note_agents_label
import com.ovalit.feature.report.resources.note_down_combat_score
import com.ovalit.feature.report.resources.note_down_damage
import com.ovalit.feature.report.resources.note_down_headshot
import com.ovalit.feature.report.resources.note_down_kd
import com.ovalit.feature.report.resources.note_up_combat_score
import com.ovalit.feature.report.resources.note_up_damage
import com.ovalit.feature.report.resources.note_up_headshot
import com.ovalit.feature.report.resources.note_up_kd
import com.ovalit.feature.report.resources.note_usual
import com.ovalit.feature.report.resources.note_weapon_down_label
import com.ovalit.feature.report.resources.note_weapon_up_label
import com.ovalit.feature.report.resources.note_weapon_value
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 고정 칸 바로 밑에 붙는 "이번 주 짚을 점"입니다. 크게 움직인 고정 지표 하나를 문장으로 풀고, 같은 쪽으로 가장 크게
 * 움직인 무기와 이긴 판이 더 많았던 요원 둘 이상을 숫자로 붙입니다. "쓰세요"나 "추천"은 쓰지 않습니다(CLAUDE.md
 * 지켜야 할 선).
 */
@Composable
internal fun WeekNoteLines(note: WeekNote, report: WeeklyReport.Ready, catalog: ContentCatalog, modifier: Modifier = Modifier) {
    // 보이는 자릿수로 뺀 차이가 0이면 "0 올랐어요"가 되니 지표 줄을 두지 않는다. 무기 줄도 "140 → 140"이면 뺀다.
    val moved = note.moved?.takeIf { it.metric.format.steps(it.current) != it.metric.format.steps(it.usual) }
    val weapon = moved?.let { note.weapon }?.takeIf { moved.metric.format.steps(it.current) != moved.metric.format.steps(it.usual) }
    if (moved == null && note.agents.isEmpty()) return
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        if (moved != null) {
            val format = moved.metric.format
            OvalitText(text = movedHeadline(moved), style = typography.bodyStrong)
            report.baseline?.let { baseline ->
                Spacer(Modifier.height(OvalitSpacing.xs))
                OvalitText(
                    text = stringResource(
                        Res.string.note_usual,
                        baseline.weeks,
                        format.valueText(moved.usual),
                        periodLabel(report.period),
                        format.valueText(moved.current),
                    ),
                    style = typography.caption,
                    color = colors.t3,
                )
            }
        }
        val rows = listOfNotNull(
            if (moved != null && weapon != null) {
                val format = moved.metric.format
                NoteRow(
                    label = stringResource(if (moved.rose) Res.string.note_weapon_up_label else Res.string.note_weapon_down_label),
                    value = stringResource(
                        Res.string.note_weapon_value,
                        catalog.weaponName(weapon.weapon),
                        stringResource(moved.metric.label),
                        format.valueText(weapon.usual),
                        format.valueText(weapon.current),
                    ),
                )
            } else {
                null
            },
            note.agents.takeIf { it.isNotEmpty() }?.let { agents ->
                NoteRow(
                    label = stringResource(Res.string.note_agents_label),
                    value = joinKeepingParts(
                        agents.map { agent ->
                            stringResource(Res.string.note_agent, catalog.agentName(agent.agent), agent.wins, agent.decided - agent.wins)
                        },
                    ),
                )
            },
        )
        if (rows.isEmpty()) return@Column
        // 헤드라인과 그 풀이는 한 덩어리로 붙이고, 무기와 요원 줄은 한 칸 띄워 덧붙인 줄로 읽히게 한다
        if (moved != null) Spacer(Modifier.height(OvalitSpacing.md))
        NoteTable(rows)
    }
}

// 이름을 한 열로 세워 값이 줄마다 같은 자리에서 시작하게 한다. 한 줄이라도 옆에 안 들어가면 모든 줄에서 값을 이름
// 밑으로 내린다. 한 줄만 내리면 두 줄의 모양이 달라 표가 흐트러진다.
@Composable
private fun NoteTable(rows: List<NoteRow>) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelWidth = rememberWidestWidth(rows.map { it.label }, caption)
        val valueWidth = rememberWidestWidth(rows.map { it.value }, caption)
        val sideBySide = labelWidth + NoteColumnGap + valueWidth <= maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(if (sideBySide) 6.dp else OvalitSpacing.sm)) {
            rows.forEach { row ->
                val merged = Modifier.semantics(mergeDescendants = true) {}
                if (sideBySide) {
                    Row(modifier = merged) {
                        OvalitText(text = row.label, modifier = Modifier.width(labelWidth), style = caption, color = colors.t3)
                        Spacer(Modifier.width(NoteColumnGap))
                        OvalitText(text = row.value, style = caption, color = colors.t2)
                    }
                } else {
                    Column(modifier = merged, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OvalitText(text = row.label, style = caption, color = colors.t3)
                        OvalitText(text = row.value, style = caption, color = colors.t2)
                    }
                }
            }
        }
    }
}

private val NoteColumnGap = 12.dp

private class NoteRow(val label: String, val value: String)

// 차이는 보이는 자릿수로 반올림한 값끼리 뺀다(CLAUDE.md 디자인)
@Composable
private fun movedHeadline(moved: MovedMetric): String {
    val format = moved.metric.format
    val gap = format.formatGap(moved.current, moved.usual)
    val gapText = if (format == MetricFormat.PERCENT) stringResource(Res.string.gap_percent, gap) else gap
    return stringResource(moved.metric.headline(moved.rose), gapText)
}

private fun FixedMetric.headline(rose: Boolean): StringResource = when (this) {
    FixedMetric.COMBAT_SCORE -> if (rose) Res.string.note_up_combat_score else Res.string.note_down_combat_score
    FixedMetric.KD -> if (rose) Res.string.note_up_kd else Res.string.note_down_kd
    FixedMetric.DAMAGE -> if (rose) Res.string.note_up_damage else Res.string.note_down_damage
    FixedMetric.HEADSHOT_RATE -> if (rose) Res.string.note_up_headshot else Res.string.note_down_headshot
}
