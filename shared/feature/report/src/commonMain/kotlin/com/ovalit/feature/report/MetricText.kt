package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.ui.MetricFormat
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.metric_assists_per_round
import com.ovalit.feature.report.resources.metric_assists_per_round_cell
import com.ovalit.feature.report.resources.metric_eco_win
import com.ovalit.feature.report.resources.metric_first_duel_involvement
import com.ovalit.feature.report.resources.metric_first_duel_involvement_cell
import com.ovalit.feature.report.resources.metric_first_duel_win
import com.ovalit.feature.report.resources.metric_first_duel_win_cell
import com.ovalit.feature.report.resources.metric_first_kill_win
import com.ovalit.feature.report.resources.metric_force_buy_win
import com.ovalit.feature.report.resources.metric_force_buy_win_cell
import com.ovalit.feature.report.resources.metric_full_buy_win
import com.ovalit.feature.report.resources.metric_kast
import com.ovalit.feature.report.resources.metric_multi_kill
import com.ovalit.feature.report.resources.metric_multi_kill_cell
import com.ovalit.feature.report.resources.metric_survival
import com.ovalit.feature.report.resources.metric_traded_death
import com.ovalit.feature.report.resources.sample_deaths
import com.ovalit.feature.report.resources.sample_eco_rounds
import com.ovalit.feature.report.resources.sample_first_duels
import com.ovalit.feature.report.resources.sample_first_kills
import com.ovalit.feature.report.resources.sample_force_buy_rounds
import com.ovalit.feature.report.resources.sample_full_buy_rounds
import com.ovalit.feature.report.resources.sample_rounds
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
        DynamicMetric.ECO_WIN_RATE -> Res.string.metric_eco_win
        DynamicMetric.FORCE_BUY_WIN_RATE -> Res.string.metric_force_buy_win
        DynamicMetric.FULL_BUY_WIN_RATE -> Res.string.metric_full_buy_win
        DynamicMetric.MULTI_KILL_RATE -> Res.string.metric_multi_kill
        DynamicMetric.TRADED_DEATH_RATE -> Res.string.metric_traded_death
    }

/**
 * 달라진 점 칸에 쓰는 이름입니다.
 * 한국어와 일본어는 [label]과 같고 영어는 긴 이름이 칸에 안 들어가 줄입니다.
 * 줄이지 않으면 한 줄에 넣으려고 그 카드의 칸 이름이 모두 작아집니다.
 */
internal val DynamicMetric.cellLabel: StringResource
    get() = when (this) {
        DynamicMetric.FIRST_DUEL_INVOLVEMENT -> Res.string.metric_first_duel_involvement_cell
        DynamicMetric.FIRST_DUEL_WIN_RATE -> Res.string.metric_first_duel_win_cell
        DynamicMetric.ASSISTS_PER_ROUND -> Res.string.metric_assists_per_round_cell
        DynamicMetric.FORCE_BUY_WIN_RATE -> Res.string.metric_force_buy_win_cell
        DynamicMetric.MULTI_KILL_RATE -> Res.string.metric_multi_kill_cell
        else -> label
    }

internal val DynamicMetric.format: MetricFormat
    get() = when (this) {
        DynamicMetric.ASSISTS_PER_ROUND -> MetricFormat.TWO_DECIMALS
        else -> MetricFormat.PERCENT
    }

/** 달라진 점 시트에 적는 표본입니다. 비율의 분모를 보여줍니다. */
@Composable
internal fun DynamicMetric.sampleText(metrics: MatchMetrics): String = when (this) {
    DynamicMetric.FIRST_KILL_WIN_RATE -> stringResource(Res.string.sample_first_kills, metrics.firstKills)
    DynamicMetric.FIRST_DUEL_WIN_RATE ->
        stringResource(Res.string.sample_first_duels, metrics.firstKills + metrics.firstDeaths)
    DynamicMetric.ECO_WIN_RATE -> stringResource(Res.string.sample_eco_rounds, metrics.ecoRounds)
    DynamicMetric.FORCE_BUY_WIN_RATE -> stringResource(Res.string.sample_force_buy_rounds, metrics.forceBuyRounds)
    DynamicMetric.FULL_BUY_WIN_RATE -> stringResource(Res.string.sample_full_buy_rounds, metrics.fullBuyRounds)
    DynamicMetric.TRADED_DEATH_RATE -> stringResource(Res.string.sample_deaths, metrics.deaths)
    else -> stringResource(Res.string.sample_rounds, metrics.rounds)
}
