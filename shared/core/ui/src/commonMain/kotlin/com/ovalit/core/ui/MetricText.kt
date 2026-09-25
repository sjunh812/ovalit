package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.metric_combat_score
import com.ovalit.core.ui.resources.metric_damage
import com.ovalit.core.ui.resources.metric_headshot
import com.ovalit.core.ui.resources.metric_kd
import com.ovalit.core.ui.resources.period_last_week
import com.ovalit.core.ui.resources.period_past_weeks
import com.ovalit.core.ui.resources.period_recent_weeks
import com.ovalit.core.ui.resources.period_this_week
import com.ovalit.core.ui.resources.value_percent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val FixedMetric.label: StringResource
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> Res.string.metric_combat_score
        FixedMetric.KD -> Res.string.metric_kd
        FixedMetric.DAMAGE -> Res.string.metric_damage
        FixedMetric.HEADSHOT_RATE -> Res.string.metric_headshot
    }

val FixedMetric.format: MetricFormat
    get() = when (this) {
        FixedMetric.COMBAT_SCORE, FixedMetric.DAMAGE -> MetricFormat.INTEGER
        FixedMetric.KD -> MetricFormat.TWO_DECIMALS
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
