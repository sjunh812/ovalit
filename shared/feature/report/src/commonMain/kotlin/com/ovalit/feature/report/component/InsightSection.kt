package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
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
import com.ovalit.core.model.Insight
import com.ovalit.core.model.InsightMetric
import com.ovalit.core.model.InsightPart
import com.ovalit.core.model.InsightSubject
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Role
import com.ovalit.core.model.Side
import com.ovalit.core.ui.Josa
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.WRAPPING_SEPARATOR
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.keepTogether
import com.ovalit.core.ui.label
import com.ovalit.core.ui.mapName
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.column_win_rate
import com.ovalit.core.ui.resources.metric_damage
import com.ovalit.core.ui.resources.metric_headshot
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withJosa
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.insight_higher
import com.ovalit.feature.report.resources.insight_lower
import com.ovalit.feature.report.resources.insight_map
import com.ovalit.feature.report.resources.insight_matches
import com.ovalit.feature.report.resources.insight_metric_multi_kill
import com.ovalit.feature.report.resources.insight_other_agents
import com.ovalit.feature.report.resources.insight_other_maps
import com.ovalit.feature.report.resources.insight_other_roles
import com.ovalit.feature.report.resources.insight_other_weapons
import com.ovalit.feature.report.resources.insight_title
import com.ovalit.feature.report.resources.insight_title_matches
import com.ovalit.feature.report.resources.insight_played_as
import com.ovalit.feature.report.resources.insight_reason_focus
import com.ovalit.feature.report.resources.insight_reason_role
import com.ovalit.feature.report.resources.insight_rounds
import com.ovalit.feature.report.resources.insight_session
import com.ovalit.feature.report.resources.insight_session_early
import com.ovalit.feature.report.resources.insight_session_late
import com.ovalit.feature.report.resources.insight_side
import com.ovalit.feature.report.resources.insight_side_attack
import com.ovalit.feature.report.resources.insight_side_defense
import com.ovalit.feature.report.resources.insight_weapon
import com.ovalit.feature.report.resources.metric_eco_win
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_force_buy_win
import com.ovalit.feature.report.resources.metric_full_buy_win
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_survival
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 개선 포인트 문장입니다. 이번 액트 경기를 둘로 나눠 [insight]의 주어 쪽을 견준 쪽과 비교해 사실로만 적습니다. 두 쪽 모두 이름이 있으면 높은 쪽이
 * 주어라 "제트로 뛴 판은 승률이 레이즈보다 높아요"가 되고, 한쪽이 "다른 맵"처럼 묶음이면 이름 있는 쪽이 주어라 낮으면
 * "낮아요"가 됩니다. 무엇과 견줄지와 어느 지표를 적을지는 모델이 나에게 영향이 큰 순으로 고릅니다. "제트를 쓰세요"처럼
 * 게임 결정을 대신하는 말은 쓰지 않습니다(CLAUDE.md 지켜야 할 선).
 */
@Composable
internal fun InsightSection(
    insight: Insight,
    role: Role?,
    period: ReportPeriod,
    catalog: ContentCatalog,
    modifier: Modifier = Modifier,
) {
    val format = if (insight.metric.isPercent) MetricFormat.PERCENT else MetricFormat.INTEGER
    val lead = insight.lead
    val other = insight.other
    // 화면에 보이는 자릿수로 반올림한 값끼리 뺀다
    val gap = format.formatGap(lead.value, other.value)
    val gapText = if (format == MetricFormat.PERCENT) stringResource(Res.string.gap_percent, gap) else gap
    val metric = stringResource(insight.metric.label)
    val leadName = lead.subject.name(catalog)
    val otherName = other.subject.name(catalog)

    val headline = stringResource(
        when (lead.subject) {
            is InsightSubject.OnAgent, is InsightSubject.OnRole -> Res.string.insight_played_as
            is InsightSubject.OnMap -> Res.string.insight_map
            is InsightSubject.WithWeapon -> Res.string.insight_weapon
            InsightSubject.LateInSession -> Res.string.insight_session
            else -> Res.string.insight_side
        },
        when (lead.subject) {
            is InsightSubject.OnAgent, is InsightSubject.OnRole -> leadName.withJosa(Josa.EURO_RO)
            is InsightSubject.WithWeapon -> leadName.withJosa(Josa.EUL_REUL)
            else -> leadName
        },
        metric.withJosa(Josa.I_GA),
        otherName,
        gapText,
        stringResource(if (insight.leadIsHigher) Res.string.insight_higher else Res.string.insight_lower),
    )
    // 몇 판, 몇 라운드로 센 숫자인지 같이 적는다. 적게 뛴 쪽의 숫자는 크게 흔들린다. 위 칸들과 달리 이번 액트 경기로 견줘서
    // 기간을 맨 앞에 적는다.
    // 묶음 제목에 "이번 액트"가 있어 이 줄에는 기간을 다시 적지 않는다
    val act = comparisonLine(
        period = null,
        parts = listOf(
            LinePart(leadName, format.valueText(lead.value), lead.sampleText()),
            LinePart(otherName, format.valueText(other.value), other.sampleText()),
        ),
    )
    // 사용자 요청(2026-09-27): 액트 동안의 차이가 이번 주에도 이어졌는지 숫자만 붙인다. 한 주 표본이라 판단은 하지 않는다.
    // 액트 줄과 같은 꼴로 두어 두 줄을 위아래로 견줘 읽게 한다.
    val recent = insight.recent?.let {
        comparisonLine(
            period = periodLabel(period),
            parts = listOf(LinePart(leadName, format.valueText(it.lead)), LinePart(otherName, format.valueText(it.other))),
        )
    }
    val focus = insight.focus
    val reason = when {
        focus != null -> stringResource(Res.string.insight_reason_focus, stringResource(focus.label).withJosa(Josa.EUL_REUL))
        insight.isRolePriority && role != null -> stringResource(
            Res.string.insight_reason_role,
            stringResource(role.label),
            metric.withJosa(Josa.EUN_NEUN),
        )
        else -> null
    }

    // 위 칸들은 모두 이번 주 숫자라 선과 제목으로 떼어 이번 액트 이야기임을 밝힌다
    Spacer(Modifier.height(20.dp))
    HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Spacer(Modifier.height(18.dp))
    Column(
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TitleWithCaption(
            title = stringResource(Res.string.insight_title),
            titleStyle = OvalitTheme.typography.bodyStrong,
            caption = insight.matches.takeIf { it > 0 }?.let { AnnotatedString(stringResource(Res.string.insight_title_matches, it)) },
        )
        Spacer(Modifier.height(6.dp))
        OvalitText(text = headline, style = headlineStyle())
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OvalitText(text = act, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
            recent?.let { OvalitText(text = it, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2) }
        }
        // 왜 이 지표를 먼저 봤는지는 숫자 줄과 다른 이야기라 숫자 줄에 붙이지 않고 한 칸 띄운다
        reason?.let { OvalitText(text = it, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t3) }
    }
}

@Composable
private fun InsightSubject.name(catalog: ContentCatalog): String = when (this) {
    is InsightSubject.OnSide -> stringResource(if (side == Side.ATTACK) Res.string.insight_side_attack else Res.string.insight_side_defense)
    is InsightSubject.OnAgent -> catalog.agentName(agent)
    is InsightSubject.OtherAgents -> agents.singleOrNull()?.let { catalog.agentName(it) }
        ?: stringResource(Res.string.insight_other_agents, stringResource(role.label))
    is InsightSubject.OnRole -> stringResource(role.label)
    is InsightSubject.OtherRoles -> roles.singleOrNull()?.let { stringResource(it.label) }
        ?: stringResource(Res.string.insight_other_roles)
    is InsightSubject.OnMap -> catalog.mapName(map)
    is InsightSubject.OtherMaps -> maps.singleOrNull()?.let { catalog.mapName(it) } ?: stringResource(Res.string.insight_other_maps)
    is InsightSubject.WithWeapon -> catalog.weaponName(weapon)
    is InsightSubject.OtherWeapons -> weapons.singleOrNull()?.let { catalog.weaponName(it) }
        ?: stringResource(Res.string.insight_other_weapons, stringResource(category.label))
    // 모델의 LATE_SESSION_GAME(3)에 맞춘 이름이다
    InsightSubject.LateInSession -> stringResource(Res.string.insight_session_late)
    InsightSubject.EarlyInSession -> stringResource(Res.string.insight_session_early)
}

// 공수와 무기는 라운드로, 요원과 역할과 맵은 판으로 센다
@Composable
private fun InsightPart.sampleText(): String = when (subject) {
    is InsightSubject.OnSide, is InsightSubject.WithWeapon, is InsightSubject.OtherWeapons ->
        stringResource(Res.string.insight_rounds, rounds)
    else -> stringResource(Res.string.insight_matches, matches)
}

/** 견주는 한쪽입니다. "공격 58% 388라운드"처럼 이름, 값, 표본 순서입니다. */
private class LinePart(val name: String, val value: String, val sample: String? = null)

// 사용자 요청(2026-09-27): 짚을 점 줄처럼 이름을 가장 진하게, 값을 그다음, 기간과 표본을 가장 옅게 칠한다. 값을 이름 바로
// 뒤에 두어 "공격 58%"로 읽힌다. 줄은 두 쪽 사이에서만 바뀐다.
@Composable
private fun comparisonLine(period: String?, parts: List<LinePart>): AnnotatedString {
    val colors = OvalitTheme.colors
    val faint = SpanStyle(color = colors.t3)
    return buildAnnotatedString {
        period?.let { withStyle(faint) { append(it.keepTogether() + NBSP) } }
        parts.forEachIndexed { index, part ->
            if (index > 0) withStyle(faint) { append(WRAPPING_SEPARATOR) }
            withStyle(SpanStyle(color = colors.t1, fontWeight = FontWeight.Medium)) { append(part.name.keepTogether()) }
            append(NBSP + part.value)
            part.sample?.let { withStyle(faint) { append(NBSP + it.keepTogether()) } }
        }
    }
}

private const val NBSP = "\u00a0"

private val InsightMetric.label: StringResource
    get() = when (this) {
        InsightMetric.SURVIVAL_RATE -> Res.string.metric_survival
        InsightMetric.KAST -> Res.string.metric_kast
        InsightMetric.FIRST_DUEL_WIN_RATE -> Res.string.metric_first_duel_win
        InsightMetric.DAMAGE -> CoreUiRes.string.metric_damage
        InsightMetric.MULTI_KILL_RATE -> Res.string.insight_metric_multi_kill
        InsightMetric.WIN_RATE -> CoreUiRes.string.column_win_rate
        InsightMetric.HEADSHOT_RATE -> CoreUiRes.string.metric_headshot
        InsightMetric.FORCE_BUY_WIN_RATE -> Res.string.metric_force_buy_win
        InsightMetric.ECO_WIN_RATE -> Res.string.metric_eco_win
        InsightMetric.FULL_BUY_WIN_RATE -> Res.string.metric_full_buy_win
    }
