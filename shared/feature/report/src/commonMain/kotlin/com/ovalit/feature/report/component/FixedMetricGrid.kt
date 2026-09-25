package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.feature.report.changeText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.fixed_same_as_baseline
import com.ovalit.feature.report.resources.fixed_versus_baseline
import com.ovalit.feature.report.valueText
import org.jetbrains.compose.resources.stringResource

internal val BlockShape = RoundedCornerShape(16.dp)

// 칸 사이를 선으로 긋지 않고 바탕을 비워서 나눈다. 테두리 대신 배경 단계로 영역을 가른다.
internal val BlockGap = 2.dp

internal const val NO_VALUE = "–"

@Composable
internal fun FixedMetricGrid(
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(BlockShape),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        FixedMetric.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(BlockGap),
            ) {
                row.forEach { metric ->
                    FixedMetricCell(
                        metric = metric,
                        metrics = metrics,
                        baseline = baseline,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
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
    val colors = OvalitTheme.colors
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }

    Column(
        modifier = modifier
            .background(colors.raised)
            .padding(OvalitSpacing.lg)
            .semantics(mergeDescendants = true) {},
    ) {
        OvalitText(
            text = stringResource(metric.label),
            style = OvalitTheme.typography.label,
            color = colors.t2,
        )
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            style = OvalitTheme.typography.metricL,
        )
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = when {
                baseline == null || current == null || usual == null ->
                    stringResource(Res.string.baseline_missing)
                metric.format.direction(current, usual) == 0 ->
                    stringResource(Res.string.fixed_same_as_baseline, baseline.weeks)
                else -> stringResource(
                    Res.string.fixed_versus_baseline,
                    baseline.weeks,
                    metric.format.changeText(current, usual),
                )
            },
            style = OvalitTheme.typography.caption,
            color = colors.t3,
        )
    }
}
