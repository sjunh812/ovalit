package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.insight_gap_percent
import com.ovalit.feature.report.resources.note_agent
import com.ovalit.feature.report.resources.note_agents_label
import com.ovalit.feature.report.resources.note_down_combat_score
import com.ovalit.feature.report.resources.note_down_damage
import com.ovalit.feature.report.resources.note_down_headshot
import com.ovalit.feature.report.resources.note_down_kd
import com.ovalit.feature.report.resources.note_title
import com.ovalit.feature.report.resources.note_up_combat_score
import com.ovalit.feature.report.resources.note_up_damage
import com.ovalit.feature.report.resources.note_up_headshot
import com.ovalit.feature.report.resources.note_up_kd
import com.ovalit.feature.report.resources.note_usual
import com.ovalit.feature.report.resources.note_weapon_down_label
import com.ovalit.feature.report.resources.note_weapon_up_label
import com.ovalit.feature.report.resources.note_weapon_value
import kotlin.math.abs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 홈 "이번 주 짚을 점"입니다. 고정 칸과 달라진 점 사이에 둡니다. 크게 움직인 고정 지표 하나, 같은 쪽으로 가장 크게 움직인
 * 무기, 잘 풀린 요원 둘 이상을 숫자로만 적습니다. "쓰세요"나 "추천"은 쓰지 않습니다(CLAUDE.md 지켜야 할 선).
 */
@Composable
internal fun WeekNoteSection(note: WeekNote, report: WeeklyReport.Ready, catalog: ContentCatalog, modifier: Modifier = Modifier) {
    // 화면에 보이는 자릿수로 뺀 차이가 0이면 "0 올랐어요"가 되니 지표 줄을 두지 않는다
    val moved = note.moved?.takeIf { it.metric.format.steps(it.current) != it.metric.format.steps(it.usual) }
    if (moved == null && note.agents.isEmpty()) return
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography

    Column(modifier = modifier) {
        HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
        Spacer(Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            OvalitText(
                text = stringResource(Res.string.note_title, periodLabel(report.period)),
                style = typography.bodyStrong,
            )
            if (moved != null) {
                Spacer(Modifier.height(10.dp))
                val format = moved.metric.format
                OvalitText(text = movedHeadline(moved), style = typography.titleM)
                Spacer(Modifier.height(OvalitSpacing.xs))
                OvalitText(
                    text = stringResource(
                        Res.string.note_usual,
                        report.baseline?.weeks ?: 0,
                        format.valueText(moved.usual),
                        periodLabel(report.period),
                        format.valueText(moved.current),
                    ),
                    style = typography.caption,
                    color = colors.t3,
                )
            }
            val weapon = note.weapon?.takeIf { moved != null }
            val rows = listOfNotNull(
                weapon?.let {
                    val format = moved!!.metric.format
                    NoteRow(
                        label = stringResource(if (moved.rose) Res.string.note_weapon_up_label else Res.string.note_weapon_down_label),
                        value = stringResource(
                            Res.string.note_weapon_value,
                            catalog.weaponName(it.weapon),
                            stringResource(moved.metric.label),
                            format.valueText(it.usual),
                            format.valueText(it.current),
                        ),
                    )
                },
                note.agents.takeIf { it.isNotEmpty() }?.let { agents ->
                    NoteRow(
                        label = stringResource(Res.string.note_agents_label),
                        value = agents.map { agent ->
                            stringResource(Res.string.note_agent, catalog.agentName(agent.agent), agent.wins, agent.decided - agent.wins)
                        }.joinToString(SEPARATOR),
                    )
                },
            )
            if (rows.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                            OvalitText(text = row.label, style = typography.caption, color = colors.t3)
                            Spacer(Modifier.height(2.dp))
                            OvalitText(text = row.value, style = typography.body, color = colors.t1)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private class NoteRow(val label: String, val value: String)

// 차이는 보이는 자릿수로 반올림한 값끼리 뺀다. 74%와 69%를 띄워 놓고 6%p라고 하면 틀려 보인다.
@Composable
private fun movedHeadline(moved: MovedMetric): String {
    val format = moved.metric.format
    val gap = abs(format.steps(moved.current) - format.steps(moved.usual))
    val gapText = if (format == MetricFormat.PERCENT) {
        stringResource(Res.string.insight_gap_percent, gap)
    } else {
        format.formatChange(moved.current, moved.usual).trimStart('+', '−')
    }
    return stringResource(moved.metric.headline(moved.rose), gapText)
}

private fun FixedMetric.headline(rose: Boolean): StringResource = when (this) {
    FixedMetric.COMBAT_SCORE -> if (rose) Res.string.note_up_combat_score else Res.string.note_down_combat_score
    FixedMetric.KD -> if (rose) Res.string.note_up_kd else Res.string.note_down_kd
    FixedMetric.DAMAGE -> if (rose) Res.string.note_up_damage else Res.string.note_down_damage
    FixedMetric.HEADSHOT_RATE -> if (rose) Res.string.note_up_headshot else Res.string.note_down_headshot
}
