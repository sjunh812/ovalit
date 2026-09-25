package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.feature.report.MetricFormat
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_average
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.summary_per_match
import com.ovalit.feature.report.valueText
import org.jetbrains.compose.resources.stringResource

private val CellGap = 10.dp

// 한 줄에 네 칸이라 글자를 키운 사용자에게는 좁다. 줄을 바꾸거나 자르지 않고 글자를 줄인다.
private fun shrinkToFit(size: TextUnit) = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size)

@Composable
internal fun FixedMetricRow(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = OvalitSpacing.gutter),
    ) {
        fixedMetrics.forEachIndexed { index, metric ->
            if (index > 0) VerticalLine()
            FixedMetricCell(
                metric = metric,
                metrics = metrics,
                baseline = baseline,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (index > 0) CellGap else 0.dp,
                        end = if (index < fixedMetrics.lastIndex) CellGap else 0.dp,
                    ),
            )
        }
    }
}

@Composable
private fun FixedMetricCell(
    metric: FixedMetric,
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        OvalitText(
            text = stringResource(metric.label),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t2,
            maxLines = 1,
            autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize),
        )
        OvalitText(
            text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            style = OvalitTheme.typography.metricM,
            maxLines = 1,
            autoSize = shrinkToFit(OvalitTheme.typography.metricM.fontSize),
        )
        if (current != null && usual != null) {
            OvalitText(
                text = metric.format.formatChange(current, usual),
                style = OvalitTheme.typography.metricS,
                color = directionColor(metric.format, current, usual),
            )
        }
    }
}

/** 고정 지표 밑 두 줄입니다. 판당 K/D/A와 표본, 그리고 비교 기준 값을 고정 지표 순서대로 적습니다. */
@Composable
internal fun FixedMetricSummary(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    modifier: Modifier = Modifier,
) {
    val perMatch = MetricFormat.ONE_DECIMAL
    val matches = metrics.matches.toDouble()

    Column(
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        OvalitText(
            text = stringResource(
                Res.string.summary_per_match,
                perMatch.format(metrics.kills / matches),
                perMatch.format(metrics.deaths / matches),
                perMatch.format(metrics.assists / matches),
                metrics.matches,
                metrics.rounds,
            ),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
        OvalitText(
            text = if (baseline != null) {
                val usualValues = fixedMetrics.map { metric ->
                    metric.value(baseline.metrics)?.let { metric.format.valueText(it) } ?: NO_VALUE
                }
                stringResource(Res.string.baseline_average, baseline.weeks, usualValues.joinToString(SEPARATOR))
            } else {
                stringResource(Res.string.baseline_missing)
            },
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}
