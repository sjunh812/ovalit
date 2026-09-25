package com.ovalit.feature.report.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_average
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.sheet_open
import com.ovalit.feature.report.resources.summary_kda
import com.ovalit.feature.report.resources.summary_sample
import org.jetbrains.compose.resources.stringResource

private val CellGap = 10.dp
private val ChevronSpace = 13.dp

@Composable
internal fun FixedMetricRow(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    onOpenMetric: (FixedMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = OvalitTheme.typography
    val cells = fixedMetrics.map { metric ->
        val current = metric.value(metrics)
        val usual = baseline?.let { metric.value(it.metrics) }
        FixedCell(
            metric = metric,
            label = stringResource(metric.label),
            value = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            change = if (current != null && usual != null) metric.format.formatChange(current, usual) else null,
            changeColor = if (current != null && usual != null) directionColor(metric.format, current, usual) else null,
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 네 칸의 이름, 숫자, 변화량을 줄마다 한 크기로 맞춘다. 칸마다 따로 줄이면 "전투점수"만 작아지고
        // 그 칸 숫자만 위로 올라가 줄이 어긋난다.
        val gaps = (CellGap * 2 + 1.dp) * (cells.size - 1)
        val cellWidth = (maxWidth - OvalitSpacing.gutter * 2 - gaps) / cells.size
        val styles = FixedCellStyles(
            label = rememberFittingStyle(cells.map { it.label }, typography.caption, cellWidth - ChevronSpace),
            value = rememberFittingStyle(cells.map { it.value }, typography.metricM, cellWidth, min = 14.sp),
            change = rememberFittingStyle(cells.mapNotNull { it.change }, typography.metricS, cellWidth),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = OvalitSpacing.gutter),
        ) {
            // 간격은 구분선 양옆에만 준다. 칸 폭 안에 간격을 넣으면 가운데 칸만 좁아진다.
            cells.forEachIndexed { index, cell ->
                if (index > 0) {
                    Spacer(Modifier.width(CellGap))
                    VerticalLine()
                    Spacer(Modifier.width(CellGap))
                }
                FixedMetricCell(
                    cell = cell,
                    styles = styles,
                    onClick = { onOpenMetric(cell.metric) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private class FixedCell(
    val metric: FixedMetric,
    val label: String,
    val value: String,
    val change: String?,
    val changeColor: Color?,
)

private class FixedCellStyles(val label: TextStyle, val value: TextStyle, val change: TextStyle)

@Composable
private fun FixedMetricCell(
    cell: FixedCell,
    styles: FixedCellStyles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(
            onClickLabel = stringResource(Res.string.sheet_open, cell.label),
            role = Role.Button,
            onClick = onClick,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = cell.label,
                modifier = Modifier.weight(1f, fill = false),
                style = styles.label,
                color = OvalitTheme.colors.t2,
                maxLines = 1,
                autoSize = shrinkToFit(styles.label.fontSize, min = 7.sp),
            )
            Spacer(Modifier.width(3.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 10.dp)
        }
        OvalitRollingText(
            text = cell.value,
            style = styles.value,
            autoSize = shrinkToFit(styles.value.fontSize),
        )
        if (cell.change != null && cell.changeColor != null) {
            OvalitText(
                text = cell.change,
                style = styles.change,
                color = cell.changeColor,
                maxLines = 1,
                autoSize = shrinkToFit(styles.change.fontSize, min = 7.sp),
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
        // 글자를 키워 한 줄에 안 들어가면 표본이 통째로 다음 줄로 내려간다. 점은 줄 끝이나 맨 앞에 두지 않는다.
        val caption = OvalitTheme.typography.caption
        val kda = stringResource(
            Res.string.summary_kda,
            perMatch.format(metrics.kills / matches),
            perMatch.format(metrics.deaths / matches),
            perMatch.format(metrics.assists / matches),
        )
        val sample = stringResource(Res.string.summary_sample, metrics.matches, metrics.rounds)
        SeparatedRow(
            items = listOf(
                { OvalitText(text = kda, style = caption, color = OvalitTheme.colors.t3) },
                { OvalitText(text = sample, style = caption, color = OvalitTheme.colors.t3) },
            ),
            separator = { OvalitText(text = SEPARATOR, style = caption, color = OvalitTheme.colors.t3) },
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
