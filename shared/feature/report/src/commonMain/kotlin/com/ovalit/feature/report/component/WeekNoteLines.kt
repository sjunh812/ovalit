package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MixGroup
import com.ovalit.core.model.MixShift
import com.ovalit.core.model.MovedMetric
import com.ovalit.core.model.SteadyPart
import com.ovalit.core.model.WeekNote
import com.ovalit.core.ui.Josa
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withJosa
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.note_agent_down_label
import com.ovalit.feature.report.resources.note_agent_up_label
import com.ovalit.feature.report.resources.note_case_matches
import com.ovalit.feature.report.resources.note_case_rounds
import com.ovalit.feature.report.resources.note_case_value
import com.ovalit.feature.report.resources.note_down_combat_score
import com.ovalit.feature.report.resources.note_down_damage
import com.ovalit.feature.report.resources.note_down_headshot
import com.ovalit.feature.report.resources.note_down_kd
import com.ovalit.feature.report.resources.note_mix_agent_down
import com.ovalit.feature.report.resources.note_mix_agent_up
import com.ovalit.feature.report.resources.note_mix_buy_down
import com.ovalit.feature.report.resources.note_mix_buy_up
import com.ovalit.feature.report.resources.note_mix_value
import com.ovalit.feature.report.resources.note_mix_weapon_down
import com.ovalit.feature.report.resources.note_mix_weapon_up
import com.ovalit.feature.report.resources.note_steady_agent
import com.ovalit.feature.report.resources.note_steady_buy
import com.ovalit.feature.report.resources.note_steady_value
import com.ovalit.feature.report.resources.note_steady_weapon
import com.ovalit.feature.report.resources.note_up_combat_score
import com.ovalit.feature.report.resources.note_up_damage
import com.ovalit.feature.report.resources.note_up_headshot
import com.ovalit.feature.report.resources.note_up_kd
import com.ovalit.feature.report.resources.note_weapon_down_label
import com.ovalit.feature.report.resources.note_weapon_up_label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 고정 칸 바로 밑에 붙는 "이번 주 짚을 점"입니다. 크게 움직인 고정 지표 하나를 문장으로 풀고, 그 변화를 가장 크게
 * 끌어간 무기와 요원을 숫자로 붙입니다. 변화의 절반 이상이 이코 라운드나 오퍼레이터처럼 비중이 바뀐 데서 왔으면 무기와
 * 요원 대신 그 비중과, 비중에 휘둘리지 않은 묶음의 성적을 붙입니다. "쓰세요"나 "추천"은 쓰지 않습니다(CLAUDE.md 지켜야 할
 * 선).
 */
@Composable
internal fun WeekNoteLines(note: WeekNote, catalog: ContentCatalog, modifier: Modifier = Modifier) {
    val moved = note.moved
    val format = moved.metric.format
    // 보이는 자릿수로 뺀 차이가 0이면 "0 올랐어요"가 되니 칸을 두지 않는다. 무기와 요원 줄도 "140 → 140"이면 뺀다.
    if (format.steps(moved.current) == format.steps(moved.usual)) return
    fun visible(current: Double, usual: Double) = format.steps(current) != format.steps(usual)
    val weapon = note.weapon?.takeIf { visible(it.current, it.usual) }
    val agent = note.agent?.takeIf { visible(it.current, it.usual) }
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        // 사용자 요청(2026-09-27): 평균이 얼마였는지는 위 고정 칸에 이미 있다. 그 변화를 무엇이 끌었는지를 적는다.
        OvalitText(text = movedHeadline(moved), style = typography.bodyStrong)
        val rows = listOfNotNull(
            note.mix?.let { mixRow(it, catalog) },
            note.mix?.steady?.let { steadyRow(moved, it, catalog) },
            weapon?.let {
                NoteRow(
                    label = stringResource(if (moved.rose) Res.string.note_weapon_up_label else Res.string.note_weapon_down_label),
                    value = stringResource(
                        Res.string.note_case_value,
                        catalog.weaponName(it.weapon),
                        stringResource(moved.metric.label),
                        format.valueText(it.usual),
                        format.valueText(it.current),
                        stringResource(Res.string.note_case_rounds, it.rounds),
                    ),
                )
            },
            agent?.let {
                NoteRow(
                    label = stringResource(if (moved.rose) Res.string.note_agent_up_label else Res.string.note_agent_down_label),
                    value = stringResource(
                        Res.string.note_case_value,
                        catalog.agentName(it.agent),
                        stringResource(moved.metric.label),
                        format.valueText(it.usual),
                        format.valueText(it.current),
                        stringResource(Res.string.note_case_matches, it.matches),
                    ),
                )
            },
        )
        rows.forEach { row ->
            // 개선 포인트 문장처럼 헤드라인과 첫 줄 사이를 6dp 띄운다. 더 붙이면 두 줄이 한 덩어리로 뭉개진다.
            Spacer(Modifier.height(6.dp))
            // 좁으면 값이 통째로 이름 밑으로 내려간다
            SeparatedRow(
                items = listOf<@Composable () -> Unit>(
                    { OvalitText(text = row.label, style = typography.caption, color = colors.t3) },
                    { OvalitText(text = row.value, style = typography.caption, color = colors.t2) },
                ),
                separator = { Spacer(Modifier.width(OvalitSpacing.sm)) },
                alignBaseline = true,
            )
        }
    }
}

private class NoteRow(val label: String, val value: String)

// "비중이 늘어난 라운드 · 이코 16% → 29%"
@Composable
private fun mixRow(mix: MixShift, catalog: ContentCatalog): NoteRow {
    val rose = mix.share > mix.usualShare
    val label = when (mix.group) {
        is MixGroup.Buy -> if (rose) Res.string.note_mix_buy_up else Res.string.note_mix_buy_down
        is MixGroup.Weapon -> if (rose) Res.string.note_mix_weapon_up else Res.string.note_mix_weapon_down
        is MixGroup.Agent -> if (rose) Res.string.note_mix_agent_up else Res.string.note_mix_agent_down
    }
    return NoteRow(
        label = stringResource(label),
        value = stringResource(
            Res.string.note_mix_value,
            mix.group.name(catalog),
            MetricFormat.PERCENT.valueText(mix.usualShare),
            MetricFormat.PERCENT.valueText(mix.share),
        ),
    )
}

// "풀바이 라운드만 보면 · 피해량 152 → 150". 차이가 없어도 적는다. 평소와 같았다는 게 이 줄이 하는 말이다.
@Composable
private fun steadyRow(moved: MovedMetric, steady: SteadyPart, catalog: ContentCatalog): NoteRow {
    val name = steady.group.name(catalog)
    val label = when (steady.group) {
        is MixGroup.Buy -> stringResource(Res.string.note_steady_buy, name)
        is MixGroup.Weapon -> stringResource(Res.string.note_steady_weapon, name.withJosa(Josa.EUL_REUL))
        is MixGroup.Agent -> stringResource(Res.string.note_steady_agent, name.withJosa(Josa.EURO_RO))
    }
    val format = moved.metric.format
    return NoteRow(
        label = label,
        value = stringResource(
            Res.string.note_steady_value,
            stringResource(moved.metric.label),
            format.valueText(steady.usual),
            format.valueText(steady.current),
        ),
    )
}

@Composable
private fun MixGroup.name(catalog: ContentCatalog): String = when (this) {
    is MixGroup.Buy -> stringResource(type.label)
    is MixGroup.Weapon -> catalog.weaponName(weapon)
    is MixGroup.Agent -> catalog.agentName(agent)
}

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
