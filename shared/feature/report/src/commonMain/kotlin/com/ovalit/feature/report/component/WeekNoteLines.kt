package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.ovalit.core.ui.WRAPPING_SEPARATOR
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.format
import com.ovalit.core.ui.keepTogether
import com.ovalit.core.ui.label
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withJosa
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.note_best_combat_score
import com.ovalit.feature.report.resources.note_best_damage
import com.ovalit.feature.report.resources.note_best_headshot
import com.ovalit.feature.report.resources.note_best_kd
import com.ovalit.feature.report.resources.note_case_change
import com.ovalit.feature.report.resources.note_case_matches
import com.ovalit.feature.report.resources.note_case_rounds
import com.ovalit.feature.report.resources.note_down_combat_score
import com.ovalit.feature.report.resources.note_down_damage
import com.ovalit.feature.report.resources.note_down_headshot
import com.ovalit.feature.report.resources.note_down_kd
import com.ovalit.feature.report.resources.note_mix_buy
import com.ovalit.feature.report.resources.note_mix_share
import com.ovalit.feature.report.resources.note_steady_agent
import com.ovalit.feature.report.resources.note_steady_buy
import com.ovalit.feature.report.resources.note_steady_weapon
import com.ovalit.feature.report.resources.note_up_combat_score
import com.ovalit.feature.report.resources.note_up_damage
import com.ovalit.feature.report.resources.note_up_headshot
import com.ovalit.feature.report.resources.note_up_kd
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 고정 칸 바로 밑에 붙는 "이번 주 짚을 점"입니다. 크게 움직인 고정 지표 하나를 문장으로 풀고, 그 변화를 가장 크게
 * 끌어간 무기와 요원을 숫자로 붙입니다. 변화의 절반 이상이 이코 라운드나 오퍼레이터처럼 비중이 바뀐 데서 왔으면 무기와
 * 요원 대신 그 비중과, 비중에 휘둘리지 않은 묶음의 성적을 붙입니다. 오른 값이 이번 액트 어느 주보다 높으면 헤드라인에
 * "이번 액트 최고"를 넣습니다. "쓰세요"나 "추천"은 쓰지 않습니다(CLAUDE.md 지켜야 할 선).
 *
 * 사용자 요청(2026-09-27): 줄마다 붙던 "가장 크게 끌어올린 무기" 같은 이름표를 뺐다. 이름, 숫자, 표본을 굵기와 밝기로만
 * 가른다. 지표 이름은 헤드라인에 있어 줄마다 되풀이하지 않는다.
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
    // 보이는 자릿수로 이전 최고와 같으면 최고라고 할 수 없다
    val best = note.previousBest?.let { format.steps(moved.current) > format.steps(it) } == true

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        // 사용자 요청(2026-09-27): 평균이 얼마였는지는 위 고정 칸에 이미 있다. 그 변화를 무엇이 끌었는지를 적는다.
        OvalitText(text = movedHeadline(moved, best), style = OvalitTheme.typography.bodyStrong)
        val rows = listOfNotNull(
            note.mix?.let { mixRow(it, catalog) },
            note.mix?.steady?.let { steadyRow(moved, it, catalog) },
            weapon?.let {
                NoteRow(
                    name = catalog.weaponName(it.weapon),
                    change = stringResource(Res.string.note_case_change, format.valueText(it.usual), format.valueText(it.current)),
                    sample = stringResource(Res.string.note_case_rounds, it.rounds),
                )
            },
            agent?.let {
                NoteRow(
                    name = catalog.agentName(it.agent),
                    change = stringResource(Res.string.note_case_change, format.valueText(it.usual), format.valueText(it.current)),
                    sample = stringResource(Res.string.note_case_matches, it.matches),
                )
            },
        )
        rows.forEachIndexed { index, row ->
            // 개선 포인트 문장처럼 헤드라인과 첫 줄 사이를 6dp 띄운다. 더 붙이면 두 줄이 한 덩어리로 뭉개진다. 줄끼리는 한
            // 묶음으로 읽히게 조금 덜 띄운다.
            Spacer(Modifier.height(if (index == 0) 6.dp else 4.dp))
            OvalitText(text = row.text(), style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
        }
    }
}

/** 짚을 점 한 줄입니다. "팬텀 130 → 233 · 60라운드"처럼 이름, 숫자, 표본 순서입니다. */
private class NoteRow(val name: String, val change: String, val sample: String? = null)

// 이름은 가장 진하게, 숫자는 그다음, 표본은 가장 옅게 칠한다. 줄은 이름과 숫자 사이, 표본 앞에서만 바뀐다.
@Composable
private fun NoteRow.text(): AnnotatedString {
    val colors = OvalitTheme.colors
    return buildAnnotatedString {
        withStyle(SpanStyle(color = colors.t1, fontWeight = FontWeight.Medium)) { append(name.keepTogether()) }
        append(" ")
        append(change.keepTogether())
        sample?.let { withStyle(SpanStyle(color = colors.t3)) { append(WRAPPING_SEPARATOR + it.keepTogether()) } }
    }
}

// "이코 라운드 비중 16% → 31%"
@Composable
private fun mixRow(mix: MixShift, catalog: ContentCatalog): NoteRow {
    val name = mix.group.name(catalog)
    return NoteRow(
        name = stringResource(if (mix.group is MixGroup.Buy) Res.string.note_mix_buy else Res.string.note_mix_share, name),
        change = stringResource(
            Res.string.note_case_change,
            MetricFormat.PERCENT.valueText(mix.usualShare),
            MetricFormat.PERCENT.valueText(mix.share),
        ),
    )
}

// "풀바이 라운드만 보면 158 → 156". 차이가 없어도 적는다. 평소와 같았다는 게 이 줄이 하는 말이다.
@Composable
private fun steadyRow(moved: MovedMetric, steady: SteadyPart, catalog: ContentCatalog): NoteRow {
    val name = steady.group.name(catalog)
    val format = moved.metric.format
    return NoteRow(
        name = when (steady.group) {
            is MixGroup.Buy -> stringResource(Res.string.note_steady_buy, name)
            is MixGroup.Weapon -> stringResource(Res.string.note_steady_weapon, name.withJosa(Josa.EUL_REUL))
            is MixGroup.Agent -> stringResource(Res.string.note_steady_agent, name.withJosa(Josa.EURO_RO))
        },
        change = stringResource(Res.string.note_case_change, format.valueText(steady.usual), format.valueText(steady.current)),
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
private fun movedHeadline(moved: MovedMetric, best: Boolean): String {
    val format = moved.metric.format
    val gap = format.formatGap(moved.current, moved.usual)
    val gapText = if (format == MetricFormat.PERCENT) stringResource(Res.string.gap_percent, gap) else gap
    return stringResource(moved.metric.headline(moved.rose, best), gapText)
}

private fun FixedMetric.headline(rose: Boolean, best: Boolean): StringResource {
    val (up, down, record) = when (this) {
        FixedMetric.COMBAT_SCORE ->
            Triple(Res.string.note_up_combat_score, Res.string.note_down_combat_score, Res.string.note_best_combat_score)
        FixedMetric.KD -> Triple(Res.string.note_up_kd, Res.string.note_down_kd, Res.string.note_best_kd)
        FixedMetric.DAMAGE -> Triple(Res.string.note_up_damage, Res.string.note_down_damage, Res.string.note_best_damage)
        FixedMetric.HEADSHOT_RATE -> Triple(Res.string.note_up_headshot, Res.string.note_down_headshot, Res.string.note_best_headshot)
    }
    return when {
        best -> record
        rose -> up
        else -> down
    }
}
