package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.feature.report.changeText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.hasGoodDirection
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.dynamic_baseline
import com.ovalit.feature.report.resources.dynamic_baseline_with_sample
import com.ovalit.feature.report.resources.dynamic_steady
import com.ovalit.feature.report.sampleText
import com.ovalit.feature.report.valueText
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DynamicMetricList(
    slots: List<DynamicSlot>,
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(BlockShape),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        slots.forEach { slot ->
            DynamicMetricRow(slot = slot, metrics = metrics, baseline = baseline)
        }
    }
}

@Composable
private fun DynamicMetricRow(
    slot: DynamicSlot,
    metrics: MatchMetrics,
    baseline: Baseline?,
) {
    val colors = OvalitTheme.colors
    val metric = slot.metric
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }
    val judged = slot.movement != Movement.UNKNOWN && baseline != null && current != null && usual != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.raised)
            .padding(OvalitSpacing.lg)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OvalitText(
                text = stringResource(metric.label),
                style = OvalitTheme.typography.bodyStrong,
            )
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = if (judged) {
                    evidenceText(slot, metrics, baseline, usual)
                } else {
                    stringResource(Res.string.baseline_missing)
                },
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }

        Spacer(Modifier.width(OvalitSpacing.md))

        Column(horizontalAlignment = Alignment.End) {
            OvalitText(
                text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                style = OvalitTheme.typography.metricM,
                color = if (judged) colors.t1 else colors.t2,
            )
            if (judged) {
                Spacer(Modifier.height(OvalitSpacing.xs))
                when (slot.movement) {
                    Movement.MOVED -> OvalitText(
                        text = metric.format.changeText(current, usual),
                        style = OvalitTheme.typography.label,
                        color = changeColor(slot, current, usual),
                    )
                    else -> OvalitText(
                        text = stringResource(Res.string.dynamic_steady),
                        style = OvalitTheme.typography.caption,
                        color = colors.t3,
                    )
                }
            }
        }
    }
}

@Composable
private fun evidenceText(slot: DynamicSlot, metrics: MatchMetrics, baseline: Baseline, usual: Double): String {
    val usualText = slot.metric.format.valueText(usual)
    val sample = slot.metric.sampleText(metrics)
        ?: return stringResource(Res.string.dynamic_baseline, baseline.weeks, usualText)
    return stringResource(Res.string.dynamic_baseline_with_sample, baseline.weeks, usualText, sample)
}

@Composable
private fun changeColor(slot: DynamicSlot, current: Double, usual: Double): Color {
    val colors = OvalitTheme.colors
    if (!slot.metric.hasGoodDirection) return colors.t1
    return when (slot.metric.format.direction(current, usual)) {
        1 -> colors.pos
        -1 -> colors.neg
        else -> colors.t1
    }
}
