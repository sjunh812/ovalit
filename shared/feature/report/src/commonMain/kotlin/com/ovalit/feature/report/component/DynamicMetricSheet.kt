package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MOVEMENT_THRESHOLD
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.VOLATILITY_WEEKS
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.dynamic_sheet_assists_body
import com.ovalit.feature.report.resources.dynamic_sheet_eco_body
import com.ovalit.feature.report.resources.dynamic_sheet_first_duel_involvement_body
import com.ovalit.feature.report.resources.dynamic_sheet_first_duel_win_body
import com.ovalit.feature.report.resources.dynamic_sheet_first_kill_win_body
import com.ovalit.feature.report.resources.dynamic_sheet_force_buy_body
import com.ovalit.feature.report.resources.dynamic_sheet_full_buy_body
import com.ovalit.feature.report.resources.dynamic_sheet_kast_body
import com.ovalit.feature.report.resources.dynamic_sheet_kast_name
import com.ovalit.feature.report.resources.dynamic_sheet_moved
import com.ovalit.feature.report.resources.dynamic_sheet_movement_title
import com.ovalit.feature.report.resources.dynamic_sheet_sample_body
import com.ovalit.feature.report.resources.dynamic_sheet_sample_title
import com.ovalit.feature.report.resources.dynamic_sheet_steady
import com.ovalit.feature.report.resources.dynamic_sheet_survival_body
import com.ovalit.feature.report.resources.dynamic_sheet_unknown
import com.ovalit.feature.report.resources.period_label_matches
import com.ovalit.feature.report.resources.sheet_usual_average
import com.ovalit.feature.report.sampleText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 달라진 점 칸을 누르면 뜨는 시트입니다. 칸 밑에는 "평소 27%"만 있어서 표본과 최소 표본, 달라졌다고 본 근거를
 * 여기에 적습니다.
 */
@Composable
internal fun DynamicMetricSheet(metric: DynamicMetric, report: WeeklyReport.Ready, onDismiss: () -> Unit) {
    OvalitBottomSheet(
        title = stringResource(metric.label),
        titleNote = metric.sheetName?.let { stringResource(it) },
        body = stringResource(metric.sheetBody),
        onDismiss = onDismiss,
    ) {
        DynamicMetricSheetBody(metric, report)
    }
}

// 시트는 따로 창을 띄워서 프리뷰에 안 나온다. 프리뷰와 테스트는 이 본문만 그린다.
@Composable
internal fun DynamicMetricSheetBody(metric: DynamicMetric, report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val slot = report.dynamic.firstOrNull { it.metric == metric } ?: DynamicSlot(metric, Movement.UNKNOWN)
    val movement = slot.movement
    Column(modifier = modifier) {
        CurrentValue(slot, report)
        Spacer(Modifier.height(22.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        SheetBlock(
            title = stringResource(Res.string.dynamic_sheet_sample_title),
            body = stringResource(Res.string.dynamic_sheet_sample_body, metric.minSampleText()),
        )
        Spacer(Modifier.height(18.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        SheetBlock(
            title = stringResource(Res.string.dynamic_sheet_movement_title),
            body = when (movement) {
                Movement.MOVED -> stringResource(
                    Res.string.dynamic_sheet_moved,
                    VOLATILITY_WEEKS,
                    MetricFormat.ONE_DECIMAL.format(MOVEMENT_THRESHOLD),
                )
                Movement.STEADY -> stringResource(Res.string.dynamic_sheet_steady)
                Movement.UNKNOWN -> stringResource(Res.string.dynamic_sheet_unknown)
            },
        )
    }
}

@Composable
private fun CurrentValue(slot: DynamicSlot, report: WeeklyReport.Ready) {
    val metric = slot.metric
    val judged = slot.movement != Movement.UNKNOWN
    val current = metric.value(report.metrics)
    val baseline = report.baseline
    val usual = baseline?.let { metric.value(it.metrics) }

    // 좁은 화면에서 오른쪽 표본이 먼저 자리를 잡으면 큰 숫자와 변화량이 글자 단위로 꺾인다. 모자라면 표본을 다음 줄로 내린다.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.sm),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Row {
                OvalitText(
                    text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.metricXl,
                )
                if (judged && current != null && usual != null) {
                    Spacer(Modifier.width(OvalitSpacing.sm))
                    OvalitText(
                        text = metric.format.formatChange(current, usual),
                        modifier = Modifier.alignByBaseline(),
                        style = OvalitTheme.typography.metricS,
                        color = changeColor(slot, current, usual),
                    )
                }
            }
            if (baseline != null && usual != null) {
                Spacer(Modifier.height(OvalitSpacing.xs))
                OvalitText(
                    text = stringResource(Res.string.sheet_usual_average, baseline.weeks, metric.format.valueText(usual)),
                    style = OvalitTheme.typography.caption,
                    color = OvalitTheme.colors.t2,
                )
            }
        }
        // 이 비율을 몇 번으로 셌는지 적는다. 분모가 작으면 숫자가 크게 흔들린다.
        Column {
            OvalitText(
                text = stringResource(Res.string.period_label_matches, periodLabel(report.period), report.metrics.matches),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
            OvalitText(
                text = metric.sampleText(report.metrics),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun SheetBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OvalitText(text = title, style = OvalitTheme.typography.bodyStrong)
        OvalitText(text = body, style = OvalitTheme.typography.body, color = OvalitTheme.colors.t2)
    }
}

// 최소 표본을 위에 적은 표본과 같은 꼴("40라운드", "퍼블 10번")로 적는다
@Composable
private fun DynamicMetric.minSampleText(): String {
    val sample = minSample
    val metrics = MatchMetrics.Empty.copy(
        rounds = sample,
        firstKills = sample,
        ecoRounds = sample,
        forceBuyRounds = sample,
        fullBuyRounds = sample,
    )
    return sampleText(metrics)
}

private val DynamicMetric.sheetName: StringResource?
    get() = if (this == DynamicMetric.KAST) Res.string.dynamic_sheet_kast_name else null

private val DynamicMetric.sheetBody: StringResource
    get() = when (this) {
        DynamicMetric.KAST -> Res.string.dynamic_sheet_kast_body
        DynamicMetric.SURVIVAL_RATE -> Res.string.dynamic_sheet_survival_body
        DynamicMetric.FIRST_KILL_WIN_RATE -> Res.string.dynamic_sheet_first_kill_win_body
        DynamicMetric.FIRST_DUEL_INVOLVEMENT -> Res.string.dynamic_sheet_first_duel_involvement_body
        DynamicMetric.FIRST_DUEL_WIN_RATE -> Res.string.dynamic_sheet_first_duel_win_body
        DynamicMetric.ASSISTS_PER_ROUND -> Res.string.dynamic_sheet_assists_body
        DynamicMetric.ECO_WIN_RATE -> Res.string.dynamic_sheet_eco_body
        DynamicMetric.FORCE_BUY_WIN_RATE -> Res.string.dynamic_sheet_force_buy_body
        DynamicMetric.FULL_BUY_WIN_RATE -> Res.string.dynamic_sheet_full_buy_body
    }
