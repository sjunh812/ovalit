package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.TREND_WEEKS
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.usualRange
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.periodLabel
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.period_last_week
import com.ovalit.feature.report.resources.period_this_week
import com.ovalit.feature.report.resources.sheet_combat_score_body
import com.ovalit.feature.report.resources.sheet_combat_score_formula
import com.ovalit.feature.report.resources.sheet_combat_score_method
import com.ovalit.feature.report.resources.sheet_combat_score_name
import com.ovalit.feature.report.resources.sheet_damage_body
import com.ovalit.feature.report.resources.sheet_damage_formula
import com.ovalit.feature.report.resources.sheet_damage_method
import com.ovalit.feature.report.resources.sheet_damage_name
import com.ovalit.feature.report.resources.sheet_headshot_body
import com.ovalit.feature.report.resources.sheet_headshot_formula
import com.ovalit.feature.report.resources.sheet_headshot_method
import com.ovalit.feature.report.resources.sheet_headshot_name
import com.ovalit.feature.report.resources.sheet_kd_body
import com.ovalit.feature.report.resources.sheet_kd_formula
import com.ovalit.feature.report.resources.sheet_kd_method
import com.ovalit.feature.report.resources.sheet_kd_no_deaths
import com.ovalit.feature.report.resources.sheet_method_title
import com.ovalit.feature.report.resources.sheet_sample_matches
import com.ovalit.feature.report.resources.sheet_sample_rounds
import com.ovalit.feature.report.resources.sheet_trend_act_marker
import com.ovalit.feature.report.resources.sheet_trend_description
import com.ovalit.feature.report.resources.sheet_trend_start
import com.ovalit.feature.report.resources.sheet_usual_missing
import com.ovalit.feature.report.resources.sheet_usual_range
import com.ovalit.feature.report.resources.sheet_usual_same
import com.ovalit.feature.report.resources.sheet_usual_title
import com.ovalit.feature.report.valueText
import com.ovalit.feature.report.withThousands
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val TrendHeight = 62.dp
private val BarGap = 5.dp

// 막대 높이는 0이 아니라 8주 중 가장 낮은 값 근처에서 시작한다. 피해량 140과 165를 0부터 그리면
// 둘 다 거의 같은 높이라 추이가 안 보인다.
private const val LOWEST_BAR = 0.35f

/** S1-a 지표 설명 시트입니다. 고정 지표 칸을 누르면 뜹니다. */
@Composable
internal fun MetricSheet(
    metric: FixedMetric,
    report: WeeklyReport.Ready,
    onDismiss: () -> Unit,
) {
    val text = metric.sheetText
    OvalitBottomSheet(
        title = stringResource(metric.label),
        titleNote = text.name?.let { stringResource(it) },
        body = stringResource(text.body),
        onDismiss = onDismiss,
    ) {
        MetricSheetBody(metric, report)
    }
}

// 시트는 따로 창을 띄워서 프리뷰에 안 나온다. 프리뷰와 테스트는 이 본문만 그린다.
@Composable
internal fun MetricSheetBody(metric: FixedMetric, report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CurrentValue(metric, report)
        Spacer(Modifier.height(OvalitSpacing.xl))
        TrendBars(metric, report)
        Spacer(Modifier.height(22.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        UsualRangeSection(metric, report)
        Spacer(Modifier.height(18.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        MethodSection(metric, report.metrics)
    }
}

private class SheetText(
    val name: StringResource?,
    val body: StringResource,
    val method: StringResource,
    val formula: StringResource,
)

private val FixedMetric.sheetText: SheetText
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> SheetText(
            name = Res.string.sheet_combat_score_name,
            body = Res.string.sheet_combat_score_body,
            method = Res.string.sheet_combat_score_method,
            formula = Res.string.sheet_combat_score_formula,
        )
        // K/D는 화면 라벨이 곧 정식 명칭이다
        FixedMetric.KD -> SheetText(
            name = null,
            body = Res.string.sheet_kd_body,
            method = Res.string.sheet_kd_method,
            formula = Res.string.sheet_kd_formula,
        )
        FixedMetric.DAMAGE -> SheetText(
            name = Res.string.sheet_damage_name,
            body = Res.string.sheet_damage_body,
            method = Res.string.sheet_damage_method,
            formula = Res.string.sheet_damage_formula,
        )
        FixedMetric.HEADSHOT_RATE -> SheetText(
            name = Res.string.sheet_headshot_name,
            body = Res.string.sheet_headshot_body,
            method = Res.string.sheet_headshot_method,
            formula = Res.string.sheet_headshot_formula,
        )
    }

@Composable
private fun CurrentValue(metric: FixedMetric, report: WeeklyReport.Ready) {
    val current = metric.value(report.metrics)
    val usual = report.baseline?.let { metric.value(it.metrics) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        OvalitText(
            text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            modifier = Modifier.alignByBaseline(),
            style = OvalitTheme.typography.metricXl,
        )
        if (current != null && usual != null) {
            Spacer(Modifier.width(OvalitSpacing.sm))
            OvalitText(
                text = metric.format.formatChange(current, usual),
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.metricS,
                color = directionColor(metric.format, current, usual),
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            OvalitText(
                text = stringResource(Res.string.sheet_sample_matches, periodLabel(report.period), report.metrics.matches),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
            OvalitText(
                text = stringResource(Res.string.sheet_sample_rounds, report.metrics.rounds),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun TrendBars(metric: FixedMetric, report: WeeklyReport.Ready) {
    val colors = OvalitTheme.colors
    val values = report.trend.map { week -> week.metrics?.let(metric.value) }
    val present = values.filterNotNull()
    val low = present.minOrNull() ?: 0.0
    val high = present.maxOrNull() ?: 0.0
    val description = stringResource(
        Res.string.sheet_trend_description,
        stringResource(metric.label),
        values.map { value -> value?.let { metric.format.valueText(it) } ?: NO_VALUE }.joinToString(SEPARATOR),
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrendHeight)
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(BarGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            report.trend.forEachIndexed { index, week ->
                if (week.startsNewAct) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.t4))
                }
                val value = values[index]
                // 기간에 든 주만 금색으로 칠한다. 시트 위의 큰 숫자가 그 막대들을 합친 값이다.
                Bar(
                    fraction = value?.let { barFraction(it, low, high) },
                    color = if (week.inPeriod) colors.accent else colors.bar,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(OvalitSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            OvalitText(
                text = stringResource(Res.string.sheet_trend_start, firstBarWeeksAgo(report)),
                modifier = Modifier.weight(1f),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
            OvalitText(
                text = stringResource(
                    if (report.period.includesThisWeek) Res.string.period_this_week else Res.string.period_last_week,
                ),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
        if (report.trend.any { it.startsNewAct }) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.sheet_trend_act_marker),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
    }
}

// 막대는 이번 주나 지난주에서 끝난다. 이번 주에서 끝나면 첫 막대는 7주 전이다.
private fun firstBarWeeksAgo(report: WeeklyReport.Ready): Int =
    if (report.period.includesThisWeek) TREND_WEEKS - 1 else TREND_WEEKS

private fun barFraction(value: Double, low: Double, high: Double): Float =
    if (high <= low) 1f else LOWEST_BAR + (1 - LOWEST_BAR) * ((value - low) / (high - low)).toFloat()

/** 라운드가 모자라 비운 주는 막대 대신 바닥 선만 둔다. 칸이 빠지면 몇 주 전인지 셀 수 없다. */
@Composable
private fun Bar(fraction: Float?, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fraction != null) Modifier.fillMaxHeight(fraction) else Modifier.height(2.dp))
                .background(if (fraction != null) color else OvalitTheme.colors.fill, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun UsualRangeSection(metric: FixedMetric, report: WeeklyReport.Ready) {
    val range = report.usualRange(metric.value)
    val text = when {
        range == null -> stringResource(Res.string.sheet_usual_missing)
        metric.format.format(range.min) == metric.format.format(range.max) ->
            stringResource(Res.string.sheet_usual_same, range.weeks, metric.format.valueText(range.min))
        else -> stringResource(
            Res.string.sheet_usual_range,
            range.weeks,
            metric.format.valueText(range.min),
            metric.format.valueText(range.max),
        )
    }
    SheetSection(title = stringResource(Res.string.sheet_usual_title), body = text)
}

@Composable
private fun MethodSection(metric: FixedMetric, metrics: MatchMetrics) {
    val current = metric.value(metrics)
    val formula = current?.let {
        val result = metric.format.valueText(it)
        when (metric) {
            FixedMetric.COMBAT_SCORE -> stringResource(
                metric.sheetText.formula,
                metrics.combatScore.withThousands(),
                metrics.rounds.withThousands(),
                result,
            )
            FixedMetric.KD -> stringResource(
                metric.sheetText.formula,
                metrics.kills.withThousands(),
                metrics.deaths.withThousands(),
                result,
            )
            FixedMetric.DAMAGE -> stringResource(
                metric.sheetText.formula,
                metrics.damage.withThousands(),
                metrics.rounds.withThousands(),
                result,
            )
            FixedMetric.HEADSHOT_RATE -> stringResource(
                metric.sheetText.formula,
                metrics.shots.head.withThousands(),
                metrics.shots.total.withThousands(),
                result,
            )
        }
    }

    SheetSection(title = stringResource(Res.string.sheet_method_title), body = stringResource(metric.sheetText.method))
    Spacer(Modifier.height(10.dp))
    if (formula != null) {
        OvalitText(
            text = formula,
            modifier = Modifier
                .fillMaxWidth()
                .background(OvalitTheme.colors.fill, RoundedCornerShape(9.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            style = OvalitTheme.typography.metricS,
            color = OvalitTheme.colors.t2,
        )
    } else if (metric == FixedMetric.KD) {
        OvalitText(
            text = stringResource(Res.string.sheet_kd_no_deaths),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

@Composable
private fun SheetSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OvalitText(text = title, style = OvalitTheme.typography.bodyStrong)
        OvalitText(text = body, style = OvalitTheme.typography.body, color = OvalitTheme.colors.t2)
    }
}
