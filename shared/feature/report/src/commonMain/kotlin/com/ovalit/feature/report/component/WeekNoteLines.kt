package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MixGroup
import com.ovalit.core.model.MixShift
import com.ovalit.core.model.MovedMetric
import com.ovalit.core.model.SteadyPart
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeekNote
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.Josa
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.WeaponImage
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.format
import com.ovalit.core.ui.keepTogether
import com.ovalit.core.ui.label
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withLocalJosa
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.note_best_combat_score
import com.ovalit.feature.report.resources.note_best_damage
import com.ovalit.feature.report.resources.note_best_headshot
import com.ovalit.feature.report.resources.note_best_kd
import com.ovalit.feature.report.resources.note_best_kda
import com.ovalit.feature.report.resources.note_case_change
import com.ovalit.feature.report.resources.note_down_combat_score
import com.ovalit.feature.report.resources.note_down_damage
import com.ovalit.feature.report.resources.note_down_headshot
import com.ovalit.feature.report.resources.note_down_kd
import com.ovalit.feature.report.resources.note_down_kda
import com.ovalit.feature.report.resources.note_mix_buy
import com.ovalit.feature.report.resources.note_mix_share
import com.ovalit.feature.report.resources.note_steady_agent
import com.ovalit.feature.report.resources.note_steady_buy
import com.ovalit.feature.report.resources.note_steady_weapon
import com.ovalit.feature.report.resources.note_up_combat_score
import com.ovalit.feature.report.resources.note_up_damage
import com.ovalit.feature.report.resources.note_up_headshot
import com.ovalit.feature.report.resources.note_up_kd
import com.ovalit.feature.report.resources.note_up_kda
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// 무기 실루엣과 요원 얼굴은 글자 높이에 맞춘 작은 타일에 담는다. 무기는 가로로 길어 타일도 가로로 길게 두고 양옆을 띄워 눕힌다.
private val LeadHeight = 24.dp
private val WeaponLeadWidth = 36.dp
private val LeadShape = RoundedCornerShape(7.dp)
private val WeaponInset = 4.dp
private val LeadGap = 6.dp

// 이름과 숫자가 붙어 보이지 않을 만큼만 띄운다
private val ValueGap = 6.dp

// 두 칸을 나란히 둘 때 칸 사이다. 한 칸의 숫자와 다음 칸의 그림이 붙어 보이지 않게 띄운다.
private val ColumnGap = 16.dp

/**
 * "이번 주 짚을 점"의 헤드라인입니다. 기간과 승패 칸 바로 밑, 고정 칸 위에 둡니다. 크게 움직인 고정 지표 하나를 문장으로 풀고, 오른
 * 값이 이번 액트 어느 주보다 높으면 "이번 액트 최고"를 넣습니다. 근거가 되는 무기와 요원 줄은 바로 밑의 [WeekNoteLines]가
 * 맡습니다. "쓰세요"나 "추천"은 쓰지 않습니다(CLAUDE.md 지켜야 할 선).
 */
@Composable
internal fun WeekNoteHeadline(note: WeekNote, modifier: Modifier = Modifier) {
    if (!note.isShown) return
    val moved = note.moved
    val format = moved.metric.format
    // 보이는 자릿수로 이전 최고와 같으면 최고라고 할 수 없다
    val best = note.previousBest?.let { format.steps(moved.current) > format.steps(it) } == true
    // 평균이 얼마에서 얼마가 됐는지는 바로 밑 고정 칸에 있어 헤드라인에 다시 적지 않는다
    OvalitText(text = movedHeadline(moved, best), modifier = modifier.padding(horizontal = OvalitSpacing.gutter), style = headlineStyle())
}

/**
 * 헤드라인 바로 밑에 붙는 짚을 점의 근거 줄입니다. 헤드라인의 변화를 가장 크게 끌어간 무기와 요원을 숫자로 붙입니다. 둘이면
 * 한 줄에 두 칸으로 나란히 두어 고정 칸이 덜 내려가게 하고, 한 칸이라도 반쪽에 안 들어가면 한 줄에 하나씩 둡니다. 변화의 절반
 * 이상이 이코 라운드나 오퍼레이터처럼 비중이 바뀐 데서 왔으면 무기와 요원 대신 그 비중과, 비중에 휘둘리지 않은 묶음의 성적을
 * 붙입니다. 헤드라인([WeekNoteHeadline])이 없으면 이 줄도 두지 않습니다.
 *
 * 줄의 숫자에는 색을 입히지 않습니다. 오르내림은 헤드라인이 말하고, 비중 줄은 늘어난 게 좋은지 나쁜지 정해져 있지 않으며,
 * 평소와 같았다는 줄에 빨강이 붙으면 틀린 말이 됩니다.
 */
@Composable
internal fun WeekNoteLines(note: WeekNote, catalog: ContentCatalog, modifier: Modifier = Modifier) {
    if (!note.isShown) return
    val format = note.moved.metric.format
    // 무기와 요원 줄도 "140 → 140"이면 뺀다
    fun visible(current: Double, usual: Double) = format.steps(current) != format.steps(usual)
    val weapon = note.weapon?.takeIf { visible(it.current, it.usual) }
    val agent = note.agent?.takeIf { visible(it.current, it.usual) }
    val rows = listOfNotNull(
        note.mix?.let { mixRow(it, catalog) },
        note.mix?.steady?.let { steadyRow(note.moved, it, catalog) },
        weapon?.let {
            val name = catalog.weaponName(it.weapon)
            NoteRow(
                lead = NoteLead.Weapon(it.weapon, name),
                name = name,
                usual = format.valueText(it.usual),
                current = format.valueText(it.current),
            )
        },
        agent?.let {
            val name = catalog.agentName(it.agent)
            NoteRow(
                lead = NoteLead.Agent(it.agent, name),
                name = name,
                usual = format.valueText(it.usual),
                current = format.valueText(it.current),
            )
        },
    )
    if (rows.isEmpty()) return
    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) { NoteRows(rows) }
}

// 보이는 자릿수로 뺀 차이가 0이면 "0 올랐어요"가 되니 헤드라인도 근거 줄도 두지 않는다
private val WeekNote.isShown: Boolean
    get() = moved.metric.format.let { it.steps(moved.current) != it.steps(moved.usual) }

/** 짚을 점 한 칸입니다. 앞에 무기나 요원 그림, 이름, "평소 → 이번" 숫자 순서입니다. 표본(라운드, 판)은 S6·S7에서 봅니다. */
private class NoteRow(
    val lead: NoteLead?,
    val name: String,
    val usual: String,
    val current: String,
)

private sealed interface NoteLead {
    val width: Dp

    class Weapon(val weapon: WeaponId, val name: String) : NoteLead {
        override val width = WeaponLeadWidth
    }

    class Agent(val agent: AgentId, val name: String) : NoteLead {
        override val width = LeadHeight
    }
}

private enum class NoteLayout {
    /** 두 칸을 한 줄에 나란히 둡니다. */
    SIDE_BY_SIDE,

    /** 한 줄에 한 칸씩, 이름 옆에 숫자를 둡니다. */
    ONE_PER_LINE,

    /** 한 줄에 한 칸씩, 숫자를 이름 밑으로 내립니다. 글꼴이 크거나 폭이 좁을 때입니다. */
    STACKED,
}

@Composable
private fun NoteRows(rows: List<NoteRow>) {
    val typography = OvalitTheme.typography
    val nameStyle = typography.label
    val valueStyle = typography.label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")
    val names = rows.map { it.nameText(nameStyle) }
    val values = rows.map { changeText(it.usual, it.current) }
    // 이코 라운드처럼 그림이 없는 묶음은 글자를 왼쪽 끝에 붙인다. 한 번에 뜨는 칸은 모두 같은 종류라 섞이지 않는다.
    val hasLead = rows.any { it.lead != null }
    // 한 줄에 하나씩 둘 때는 무기와 요원의 이름이 같은 자리에서 시작하게 그림 자리를 가장 넓은 그림에 맞춘다
    val leadSlot = rows.mapNotNull { it.lead?.width }.maxOrNull() ?: 0.dp
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layout = with(LocalDensity.current) {
            fun textWidth(index: Int) =
                measurer.measure(names[index], nameStyle, maxLines = 1).size.width +
                    ValueGap.roundToPx() +
                    measurer.measure(values[index], valueStyle, maxLines = 1).size.width
            val half = (constraints.maxWidth - ColumnGap.roundToPx()) / 2
            val lineLead = if (hasLead) (leadSlot + LeadGap).roundToPx() else 0
            when {
                rows.size == 2 && rows.indices.all { index ->
                    (rows[index].lead?.let { (it.width + LeadGap).roundToPx() } ?: 0) + textWidth(index) <= half
                } -> NoteLayout.SIDE_BY_SIDE
                // 한 칸이라도 이름 옆에 숫자가 안 들어가면 모든 칸의 숫자를 이름 밑으로 내린다. 한 칸만 내리면 숫자 열이 어긋난다.
                rows.indices.all { lineLead + textWidth(it) <= constraints.maxWidth } -> NoteLayout.ONE_PER_LINE
                else -> NoteLayout.STACKED
            }
        }
        if (layout == NoteLayout.SIDE_BY_SIDE) {
            Row(horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                rows.forEachIndexed { index, row ->
                    NoteCell(row, names[index], values[index], nameStyle, valueStyle, leadSlot = row.lead?.width, stacked = false, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { index, row ->
                    NoteCell(
                        row = row,
                        name = names[index],
                        value = values[index],
                        nameStyle = nameStyle,
                        valueStyle = valueStyle,
                        leadSlot = leadSlot.takeIf { hasLead },
                        stacked = layout == NoteLayout.STACKED,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** @param leadSlot 그림 자리의 폭입니다. 그림이 없는 묶음이면 `null`이고 자리를 두지 않습니다. */
@Composable
private fun NoteCell(
    row: NoteRow,
    name: AnnotatedString,
    value: AnnotatedString,
    nameStyle: TextStyle,
    valueStyle: TextStyle,
    leadSlot: Dp?,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    // 화면 읽기 프로그램이 한 칸을 "팬텀, 131 → 217"로 한 번에 읽게 한다
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = if (stacked) Alignment.Top else Alignment.CenterVertically,
    ) {
        if (leadSlot != null) {
            Box(modifier = Modifier.width(leadSlot).height(LeadHeight)) {
                row.lead?.let { NoteLeadImage(it) }
            }
            Spacer(Modifier.width(LeadGap))
        }
        if (stacked) {
            Column {
                OvalitText(text = name, style = nameStyle)
                OvalitText(text = value, style = valueStyle)
            }
        } else {
            OvalitText(text = name, style = nameStyle, maxLines = 1)
            Spacer(Modifier.width(ValueGap))
            OvalitText(text = value, style = valueStyle, maxLines = 1)
        }
    }
}

// 그림은 이름 옆에 붙는 장식이라 화면 읽기 프로그램이 읽지 않는다. 이름은 바로 옆에 있다.
@Composable
private fun NoteLeadImage(lead: NoteLead) {
    val tile = Modifier.size(width = lead.width, height = LeadHeight).clip(LeadShape)
    when (lead) {
        is NoteLead.Weapon -> Box(modifier = tile.background(OvalitTheme.colors.fill), contentAlignment = Alignment.Center) {
            WeaponImage(
                weapon = lead.weapon,
                name = lead.name,
                modifier = Modifier.padding(horizontal = WeaponInset).fillMaxSize(),
                tint = OvalitTheme.colors.t2,
            )
        }
        // 요원 얼굴은 타일을 꽉 채운다. 그림을 받기 전에는 같은 --fill 면이 보인다.
        is NoteLead.Agent -> AgentImage(lead.agent, lead.name, tile)
    }
}

// 이름은 가장 진하게 둔다
@Composable
private fun NoteRow.nameText(nameStyle: TextStyle): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = OvalitTheme.colors.t1, fontWeight = nameStyle.fontWeight)) { append(name.keepTogether()) }
}

// "131 → 217". 평소 값과 화살표는 옅게, 이번 값은 밝고 굵게 둔다. 꼴은 문자열 리소스를 따른다.
@Composable
private fun changeText(usual: String, current: String): AnnotatedString {
    val colors = OvalitTheme.colors
    val pattern = stringResource(Res.string.note_case_change, USUAL_MARK, CURRENT_MARK)
    val usualAt = pattern.indexOf(USUAL_MARK)
    val currentAt = pattern.indexOf(CURRENT_MARK)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = colors.t3)) {
            append(pattern.substring(0, usualAt))
            append(usual)
            append(pattern.substring(usualAt + USUAL_MARK.length, currentAt).keepTogether())
        }
        withStyle(SpanStyle(color = colors.t1, fontWeight = FontWeight.SemiBold)) { append(current) }
        withStyle(SpanStyle(color = colors.t3)) { append(pattern.substring(currentAt + CURRENT_MARK.length)) }
    }
}

private const val USUAL_MARK = "\u0001"
private const val CURRENT_MARK = "\u0002"

// "이코 라운드 비중 16% → 31%"
@Composable
private fun mixRow(mix: MixShift, catalog: ContentCatalog): NoteRow {
    val name = mix.group.name(catalog)
    return NoteRow(
        lead = mix.group.lead(catalog),
        name = stringResource(if (mix.group is MixGroup.Buy) Res.string.note_mix_buy else Res.string.note_mix_share, name),
        usual = MetricFormat.PERCENT.valueText(mix.usualShare),
        current = MetricFormat.PERCENT.valueText(mix.share),
    )
}

// "풀바이 라운드만 보면 158 → 156". 차이가 없어도 적는다. 평소와 같았다는 게 이 줄이 하는 말이다.
@Composable
private fun steadyRow(moved: MovedMetric, steady: SteadyPart, catalog: ContentCatalog): NoteRow {
    val name = steady.group.name(catalog)
    val format = moved.metric.format
    return NoteRow(
        lead = steady.group.lead(catalog),
        name = when (steady.group) {
            is MixGroup.Buy -> stringResource(Res.string.note_steady_buy, name)
            is MixGroup.Weapon -> stringResource(Res.string.note_steady_weapon, name.withLocalJosa(Josa.EUL_REUL))
            is MixGroup.Agent -> stringResource(Res.string.note_steady_agent, name.withLocalJosa(Josa.EURO_RO))
        },
        usual = format.valueText(steady.usual),
        current = format.valueText(steady.current),
    )
}

@Composable
private fun MixGroup.name(catalog: ContentCatalog): String = when (this) {
    is MixGroup.Buy -> stringResource(type.label)
    is MixGroup.Weapon -> catalog.weaponName(weapon)
    is MixGroup.Agent -> catalog.agentName(agent)
}

// 이코·포스바이·풀바이에는 그림이 없다
@Composable
private fun MixGroup.lead(catalog: ContentCatalog): NoteLead? = when (this) {
    is MixGroup.Buy -> null
    is MixGroup.Weapon -> NoteLead.Weapon(weapon, catalog.weaponName(weapon))
    is MixGroup.Agent -> NoteLead.Agent(agent, catalog.agentName(agent))
}

// 차이는 보이는 자릿수로 반올림한 값끼리 뺀다(CLAUDE.md 지표 규칙)
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
        FixedMetric.KDA -> Triple(Res.string.note_up_kda, Res.string.note_down_kda, Res.string.note_best_kda)
    }
    return when {
        best -> record
        rose -> up
        else -> down
    }
}
