package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Role
import com.ovalit.core.model.SideInsight
import com.ovalit.core.model.SideMetric
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.label
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.metric_damage
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.gap_percent
import com.ovalit.feature.report.resources.insight_headline
import com.ovalit.feature.report.resources.insight_headline_multi_kill
import com.ovalit.feature.report.resources.insight_side_attack
import com.ovalit.feature.report.resources.insight_side_defense
import com.ovalit.feature.report.resources.insight_values
import com.ovalit.feature.report.resources.insight_values_damage
import com.ovalit.feature.report.resources.insight_values_damage_with_focus
import com.ovalit.feature.report.resources.insight_values_damage_with_role
import com.ovalit.feature.report.resources.insight_values_with_focus
import com.ovalit.feature.report.resources.insight_values_with_role
import com.ovalit.feature.report.resources.metric_eco_win
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_force_buy_win
import com.ovalit.feature.report.resources.metric_full_buy_win
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_multi_kill
import com.ovalit.feature.report.resources.metric_survival
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 개선 포인트 문장입니다. [insight]에 담긴 지표의 공격과 수비 차이를 사실로만 적습니다. 어느 지표를 적을지는 모델이
 * 관심사 지표, 역할의 우선 지표, 가장 벌어진 지표 순으로 고릅니다. "수비에서 더 버티세요"처럼 게임 결정을 대신하는
 * 말은 쓰지 않습니다(CLAUDE.md 지켜야 할 선).
 */
@Composable
internal fun InsightSection(insight: SideInsight, role: Role?, modifier: Modifier = Modifier) {
    val format = insight.metric.format
    val attack = insight.metric.value(insight.attack) ?: return
    val defense = insight.metric.value(insight.defense) ?: return
    // 화면에 보이는 자릿수로 반올림한 값끼리 견주고 뺀다
    val attackLower = format.steps(attack) < format.steps(defense)
    val gap = format.formatGap(attack, defense)
    val metricLabel = stringResource(insight.metric.label)
    val attackLabel = stringResource(Res.string.insight_side_attack)
    val defenseLabel = stringResource(Res.string.insight_side_defense)

    val lower = if (attackLower) attackLabel else defenseLabel
    val higher = if (attackLower) defenseLabel else attackLabel
    val gapText = if (format == MetricFormat.PERCENT) stringResource(Res.string.gap_percent, gap) else gap
    // "멀티킬 라운드"는 받침이 없어 "이"가 붙지 않고, "수비 라운드 멀티킬 라운드"처럼 라운드가 겹쳐서 문장을 따로 쓴다
    val headline = if (insight.metric == SideMetric.MULTI_KILL_RATE) {
        stringResource(Res.string.insight_headline_multi_kill, lower, higher, gapText)
    } else {
        stringResource(Res.string.insight_headline, lower, metricLabel, higher, gapText)
    }
    val attackValue = format.valueText(attack)
    val defenseValue = format.valueText(defense)
    val focus = insight.focus
    // 피해량은 정수라 "121예요"처럼 숫자 뒤 조사가 틀린다. 숫자 뒤에 조사가 오지 않는 문장을 따로 쓴다.
    val damage = insight.metric == SideMetric.DAMAGE
    val body = when {
        // 관심사 지표면 관심사를 까닭으로 든다
        focus != null -> stringResource(
            if (damage) Res.string.insight_values_damage_with_focus else Res.string.insight_values_with_focus,
            attackLabel,
            attackValue,
            defenseLabel,
            defenseValue,
            stringResource(focus.label),
        )
        insight.isRolePriority && role != null -> stringResource(
            if (damage) Res.string.insight_values_damage_with_role else Res.string.insight_values_with_role,
            attackLabel,
            attackValue,
            defenseLabel,
            defenseValue,
            stringResource(role.label),
            metricLabel,
        )
        else -> stringResource(
            if (damage) Res.string.insight_values_damage else Res.string.insight_values,
            attackLabel,
            attackValue,
            defenseLabel,
            defenseValue,
        )
    }

    Column(
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OvalitText(text = headline, style = OvalitTheme.typography.bodyStrong)
        OvalitText(text = body, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
    }
}

private val SideMetric.label: StringResource
    get() = when (this) {
        SideMetric.SURVIVAL_RATE -> Res.string.metric_survival
        SideMetric.KAST -> Res.string.metric_kast
        SideMetric.FIRST_DUEL_WIN_RATE -> Res.string.metric_first_duel_win
        SideMetric.DAMAGE -> CoreUiRes.string.metric_damage
        SideMetric.MULTI_KILL_RATE -> Res.string.metric_multi_kill
        SideMetric.FORCE_BUY_WIN_RATE -> Res.string.metric_force_buy_win
        SideMetric.ECO_WIN_RATE -> Res.string.metric_eco_win
        SideMetric.FULL_BUY_WIN_RATE -> Res.string.metric_full_buy_win
    }

private val SideMetric.format: MetricFormat
    get() = if (this == SideMetric.DAMAGE) MetricFormat.INTEGER else MetricFormat.PERCENT
