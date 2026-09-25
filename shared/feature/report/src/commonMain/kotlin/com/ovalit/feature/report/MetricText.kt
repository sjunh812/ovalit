package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Role
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.delta_percent_points
import com.ovalit.feature.report.resources.metric_assists_per_round
import com.ovalit.feature.report.resources.metric_combat_score
import com.ovalit.feature.report.resources.metric_damage
import com.ovalit.feature.report.resources.metric_first_duel_involvement
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_first_kill_win
import com.ovalit.feature.report.resources.metric_headshot
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_kd
import com.ovalit.feature.report.resources.metric_survival
import com.ovalit.feature.report.resources.report_period_recent_weeks
import com.ovalit.feature.report.resources.report_period_this_week
import com.ovalit.feature.report.resources.role_controller
import com.ovalit.feature.report.resources.role_duelist
import com.ovalit.feature.report.resources.role_initiator
import com.ovalit.feature.report.resources.role_sentinel
import com.ovalit.feature.report.resources.sample_first_duels
import com.ovalit.feature.report.resources.sample_first_kills
import com.ovalit.feature.report.resources.value_percent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val FixedMetric.label: StringResource
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> Res.string.metric_combat_score
        FixedMetric.KD -> Res.string.metric_kd
        FixedMetric.DAMAGE -> Res.string.metric_damage
        FixedMetric.HEADSHOT_RATE -> Res.string.metric_headshot
    }

internal val FixedMetric.format: MetricFormat
    get() = when (this) {
        FixedMetric.COMBAT_SCORE, FixedMetric.DAMAGE -> MetricFormat.INTEGER
        FixedMetric.KD -> MetricFormat.DECIMAL
        FixedMetric.HEADSHOT_RATE -> MetricFormat.PERCENT
    }

internal val DynamicMetric.label: StringResource
    get() = when (this) {
        DynamicMetric.KAST -> Res.string.metric_kast
        DynamicMetric.SURVIVAL_RATE -> Res.string.metric_survival
        DynamicMetric.FIRST_KILL_WIN_RATE -> Res.string.metric_first_kill_win
        DynamicMetric.FIRST_DUEL_INVOLVEMENT -> Res.string.metric_first_duel_involvement
        DynamicMetric.FIRST_DUEL_WIN_RATE -> Res.string.metric_first_duel_win
        DynamicMetric.ASSISTS_PER_ROUND -> Res.string.metric_assists_per_round
    }

internal val DynamicMetric.format: MetricFormat
    get() = when (this) {
        DynamicMetric.ASSISTS_PER_ROUND -> MetricFormat.DECIMAL
        else -> MetricFormat.PERCENT
    }

// 퍼블 관여율은 역할에 따라 오르는 게 좋기도 나쁘기도 하다. 올랐다고 초록으로 칠하지 않는다.
internal val DynamicMetric.hasGoodDirection: Boolean
    get() = this != DynamicMetric.FIRST_DUEL_INVOLVEMENT

internal val Role.label: StringResource
    get() = when (this) {
        Role.DUELIST -> Res.string.role_duelist
        Role.INITIATOR -> Res.string.role_initiator
        Role.CONTROLLER -> Res.string.role_controller
        Role.SENTINEL -> Res.string.role_sentinel
    }

@Composable
internal fun periodLabel(period: ReportPeriod): String =
    if (period.weeks == 1) {
        stringResource(Res.string.report_period_this_week)
    } else {
        stringResource(Res.string.report_period_recent_weeks, period.weeks)
    }

@Composable
internal fun MetricFormat.valueText(value: Double): String =
    if (this == MetricFormat.PERCENT) stringResource(Res.string.value_percent, format(value)) else format(value)

@Composable
internal fun MetricFormat.changeText(current: Double, baseline: Double): String {
    val change = formatChange(current, baseline)
    return if (this == MetricFormat.PERCENT) stringResource(Res.string.delta_percent_points, change) else change
}

/**
 * 카드 근거에 붙는 표본입니다. 라운드로 나누는 지표는 표본을 적지 않고 `null`을 돌려줍니다.
 * 경기 수는 헤드라인에 이미 있어서, 칸마다 적으면 같은 숫자가 세 번 되풀이됩니다.
 */
@Composable
internal fun DynamicMetric.sampleText(metrics: MatchMetrics): String? = when (this) {
    DynamicMetric.FIRST_KILL_WIN_RATE -> stringResource(Res.string.sample_first_kills, metrics.firstKills)
    DynamicMetric.FIRST_DUEL_WIN_RATE ->
        stringResource(Res.string.sample_first_duels, metrics.firstKills + metrics.firstDeaths)
    else -> null
}
