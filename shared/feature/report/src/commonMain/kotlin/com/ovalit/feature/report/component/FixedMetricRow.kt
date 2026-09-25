package com.ovalit.feature.report.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitRollingText
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_average
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.sheet_open
import com.ovalit.feature.report.resources.summary_per_match
import org.jetbrains.compose.resources.stringResource

private val CellGap = 10.dp


@Composable
internal fun FixedMetricRow(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    onOpenMetric: (FixedMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = OvalitSpacing.gutter),
    ) {
        // 간격은 구분선 양옆에만 준다. 칸 폭 안에 간격을 넣으면 가운데 칸만 좁아진다.
        fixedMetrics.forEachIndexed { index, metric ->
            if (index > 0) {
                Spacer(Modifier.width(CellGap))
                VerticalLine()
                Spacer(Modifier.width(CellGap))
            }
            FixedMetricCell(
                metric = metric,
                metrics = metrics,
                baseline = baseline,
                onClick = { onOpenMetric(metric) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FixedMetricCell(
    metric: FixedMetric,
    metrics: MatchMetrics,
    baseline: Baseline?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }
    val label = stringResource(metric.label)

    Column(
        modifier = modifier.clickable(
            onClickLabel = stringResource(Res.string.sheet_open, label),
            role = Role.Button,
            onClick = onClick,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
                maxLines = 1,
                // 화살표까지 넣으면 작은 화면에서 글자를 키웠을 때 9sp로도 "전투점수"가 안 들어간다
                autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize, min = 7.sp),
            )
            Spacer(Modifier.width(3.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 10.dp)
        }
        OvalitRollingText(
            text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            style = OvalitTheme.typography.metricM,
            autoSize = shrinkToFit(OvalitTheme.typography.metricM.fontSize),
        )
        if (current != null && usual != null) {
            OvalitRollingText(
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
