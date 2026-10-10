package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.metric_combat_score
import com.ovalit.core.ui.resources.metric_damage
import com.ovalit.core.ui.resources.metric_headshot
import com.ovalit.core.ui.resources.metric_headshot_short
import com.ovalit.core.ui.resources.metric_kd
import com.ovalit.core.ui.resources.period_last_week
import com.ovalit.core.ui.resources.profile_kda
import com.ovalit.core.ui.resources.period_past_weeks
import com.ovalit.core.ui.resources.period_recent_weeks
import com.ovalit.core.ui.resources.period_this_week
import com.ovalit.core.ui.resources.value_percent
import com.ovalit.core.ui.resources.value_percent_point
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val FixedMetric.label: StringResource
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> Res.string.metric_combat_score
        FixedMetric.KD -> Res.string.metric_kd
        FixedMetric.DAMAGE -> Res.string.metric_damage
        FixedMetric.HEADSHOT_RATE -> Res.string.metric_headshot
        FixedMetric.KDA -> Res.string.profile_kda
    }

/** 표 열처럼 좁은 자리의 이름입니다. 한국어는 [label]과 같고, 일본어 「ヘッドショット」는 열에 안 들어가 「HS率」로 줄입니다. */
val FixedMetric.columnLabel: StringResource
    get() = if (this == FixedMetric.HEADSHOT_RATE) Res.string.metric_headshot_short else label

val FixedMetric.format: MetricFormat
    get() = when (this) {
        FixedMetric.COMBAT_SCORE, FixedMetric.DAMAGE -> MetricFormat.INTEGER
        FixedMetric.KD, FixedMetric.KDA -> MetricFormat.TWO_DECIMALS
        FixedMetric.HEADSHOT_RATE -> MetricFormat.PERCENT
    }

@Composable
fun periodLabel(period: ReportPeriod): String = when {
    period.includesThisWeek && period.weeks == 1 -> stringResource(Res.string.period_this_week)
    period.includesThisWeek -> stringResource(Res.string.period_recent_weeks, period.weeks)
    period.weeks == 1 -> stringResource(Res.string.period_last_week)
    else -> stringResource(Res.string.period_past_weeks, period.weeks)
}

@Composable
fun MetricFormat.valueText(value: Double): String =
    if (this == MetricFormat.PERCENT) stringResource(Res.string.value_percent, format(value)) else format(value)

/**
 * 화면에 띄우는 변화량입니다.
 * 퍼센트 지표는 "+11%p"처럼 단위를 붙입니다.
 * 옆의 "평소 28%"와 같은 단위인지 한 번 더 생각하지 않게 하고, 짚을 점 헤드라인("9%p 올랐어요")과 맞춥니다.
 */
@Composable
fun MetricFormat.changeText(current: Double, baseline: Double): String {
    val change = formatChange(current, baseline)
    return if (this == MetricFormat.PERCENT) stringResource(Res.string.value_percent_point, change) else change
}
