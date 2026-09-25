package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Role
import com.ovalit.core.model.SideInsight
import com.ovalit.core.model.SideMetric
import com.ovalit.core.ui.MetricFormat
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.insight_gap_percent
import com.ovalit.feature.report.resources.insight_headline
import com.ovalit.feature.report.resources.insight_side_attack
import com.ovalit.feature.report.resources.insight_side_defense
import com.ovalit.feature.report.resources.insight_values
import com.ovalit.feature.report.resources.insight_values_with_role
import com.ovalit.core.ui.resources.metric_damage
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_survival
import com.ovalit.core.ui.valueText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.ovalit.core.ui.resources.Res as CoreUiRes

/**
 * 개선 포인트 문장입니다. 공격과 수비로 나눠 가장 크게 벌어진 지표를 사실로만 적습니다.
 * "수비에서 더 버티세요"처럼 게임 결정을 대신하는 말은 쓰지 않습니다.
 */
@Composable
internal fun InsightSection(insight: SideInsight, role: Role?, modifier: Modifier = Modifier) {
    val format = insight.metric.format
    val attack = insight.metric.value(insight.attack) ?: return
    val defense = insight.metric.value(insight.defense) ?: return
    // 화면에 보이는 자릿수로 반올림한 값끼리 뺀다
    val attackLower = format.steps(attack) < format.steps(defense)
    val gap = abs(format.steps(attack) - format.steps(defense))
    val metricLabel = stringResource(insight.metric.label)
    val attackLabel = stringResource(Res.string.insight_side_attack)
    val defenseLabel = stringResource(Res.string.insight_side_defense)

    val headline = stringResource(
        Res.string.insight_headline,
        if (attackLower) attackLabel else defenseLabel,
        metricLabel,
        if (attackLower) defenseLabel else attackLabel,
        if (format == MetricFormat.PERCENT) stringResource(Res.string.insight_gap_percent, gap) else gap.toString(),
    )
    val attackValue = format.valueText(attack)
    val defenseValue = format.valueText(defense)
    val body = if (insight.isRolePriority && role != null) {
        stringResource(
            Res.string.insight_values_with_role,
            attackLabel,
            attackValue,
            defenseLabel,
            defenseValue,
            stringResource(role.label),
            metricLabel,
        )
    } else {
        stringResource(Res.string.insight_values, attackLabel, attackValue, defenseLabel, defenseValue)
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
    }

private val SideMetric.format: MetricFormat
    get() = if (this == SideMetric.DAMAGE) MetricFormat.INTEGER else MetricFormat.PERCENT
