package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.Role
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.metric_assists_per_round
import com.ovalit.feature.report.resources.metric_first_duel_involvement
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_first_kill_win
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_survival
import com.ovalit.feature.report.resources.queue_competitive
import com.ovalit.feature.report.resources.queue_competitive_and_unrated
import com.ovalit.feature.report.resources.queue_other
import com.ovalit.feature.report.resources.queue_unrated
import com.ovalit.feature.report.resources.role_controller
import com.ovalit.feature.report.resources.role_duelist
import com.ovalit.feature.report.resources.role_initiator
import com.ovalit.feature.report.resources.role_sentinel
import com.ovalit.feature.report.resources.sample_first_duels
import com.ovalit.feature.report.resources.sample_first_kills
import com.ovalit.feature.report.resources.sample_rounds
import com.ovalit.core.ui.MetricFormat
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
        DynamicMetric.ASSISTS_PER_ROUND -> MetricFormat.TWO_DECIMALS
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

internal val QueueFilter.label: StringResource
    get() = when (this) {
        QueueFilter.COMPETITIVE_AND_UNRATED -> Res.string.queue_competitive_and_unrated
        QueueFilter.COMPETITIVE -> Res.string.queue_competitive
        QueueFilter.UNRATED -> Res.string.queue_unrated
        QueueFilter.OTHER -> Res.string.queue_other
    }

/** 카드 근거에 붙는 표본입니다. 비율의 분모를 보여줍니다. */
@Composable
internal fun DynamicMetric.sampleText(metrics: MatchMetrics): String = when (this) {
    DynamicMetric.FIRST_KILL_WIN_RATE -> stringResource(Res.string.sample_first_kills, metrics.firstKills)
    DynamicMetric.FIRST_DUEL_WIN_RATE ->
        stringResource(Res.string.sample_first_duels, metrics.firstKills + metrics.firstDeaths)
    else -> stringResource(Res.string.sample_rounds, metrics.rounds)
}
