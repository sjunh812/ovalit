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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitRollingText
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.currentMaxWidth
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.changeText
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaColor
import com.ovalit.core.ui.label
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.dynamic_usual
import com.ovalit.feature.report.resources.sheet_open
import org.jetbrains.compose.resources.stringResource

/**
 * 홈 위쪽 고정 칸입니다. 한 줄에 세 칸씩 두고 칸마다 이름, 숫자, 그 밑에 "+75 · 평소 177"을 둡니다. 달라진 점과 같은
 * 격자라 두 카드의 칸 경계가 위아래로 맞습니다.
 */
@Composable
internal fun FixedMetricRow(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    onOpenMetric: (FixedMetric) -> Unit,
    modifier: Modifier = Modifier,
    subLines: List<Pair<String?, String>>? = null,
) {
    val typography = OvalitTheme.typography
    val cells = fixedCells(metrics, baseline, fixedMetrics)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 이름, 숫자, 변화량마다 모든 칸에 한 크기를 쓴다. 칸마다 따로 줄이면 "전투점수"만 작아지고 그 칸 숫자만
        // 위로 올라가 줄이 어긋난다.
        val perRow = cells.size.coerceAtMost(MetricColumns)
        val gaps = (MetricColumnGap * 2 + 1.dp) * (perRow - 1)
        val cellWidth = (currentMaxWidth - OvalitSpacing.gutter * 2 - gaps) / perRow
        val styles = FixedCellStyles(
            label = rememberFittingStyle(cells.map { it.label }, typography.caption, cellWidth - ChevronSpace),
            value = rememberFittingStyle(cells.map { it.value }, typography.metricM, cellWidth, min = 14.sp),
            subLine = rememberSubLineStyle(subLines ?: cells.map { it.change to it.usual }, cellWidth),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MetricRowGap)) {
            cells.chunked(perRow).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = OvalitSpacing.gutter),
                ) {
                    repeat(perRow) { index ->
                        val cell = row.getOrNull(index)
                        if (index > 0) {
                            // 간격은 구분선 양옆에만 준다. 칸 폭 안에 넣으면 칸마다 내용 폭이 달라진다.
                            Spacer(Modifier.width(MetricColumnGap))
                            if (cell != null) VerticalLine() else Spacer(Modifier.width(1.dp))
                            Spacer(Modifier.width(MetricColumnGap))
                        }
                        if (cell == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            // 기타 모드로 바꾸면 자리마다 지표가 바뀐다. 자리로 묶으면 피해량 숫자가 K/D로 굴러가서 지표로 묶는다.
                            key(cell.metric) {
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
            }
        }
    }
}

/** 고정 칸마다 숫자 밑 한 줄의 변화량과 평소 값 글자입니다. 달라진 점과 같은 크기를 쓰려고 홈이 모읍니다([rememberSubLineStyle]). */
@Composable
internal fun fixedSubLines(metrics: MatchMetrics, baseline: Baseline?, fixedMetrics: List<FixedMetric>): List<Pair<String?, String>> =
    fixedCells(metrics, baseline, fixedMetrics).map { it.change to it.usual }

@Composable
private fun fixedCells(metrics: MatchMetrics, baseline: Baseline?, fixedMetrics: List<FixedMetric>): List<FixedCell> {
    val colors = OvalitTheme.colors
    return fixedMetrics.map { metric ->
        val current = metric.value(metrics)
        val usual = baseline?.let { metric.value(it.metrics) }
        FixedCell(
            metric = metric,
            label = stringResource(metric.label),
            value = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            valueColor = if (metric == FixedMetric.KDA && current != null) kdaColor(current, below = colors.t1) else colors.t1,
            change = if (current != null && usual != null) metric.format.changeText(current, usual) else null,
            changeColor = if (current != null && usual != null) directionColor(metric.format, current, usual) else colors.t3,
            usual = stringResource(Res.string.dynamic_usual, usual?.let { metric.format.valueText(it) } ?: NO_VALUE),
        )
    }
}

private class FixedCell(
    val metric: FixedMetric,
    val label: String,
    val value: String,
    val valueColor: Color,
    val change: String?,
    val changeColor: Color,
    val usual: String,
)

private class FixedCellStyles(val label: TextStyle, val value: TextStyle, val subLine: MetricSubLineStyle)

@Composable
private fun FixedMetricCell(cell: FixedCell, styles: FixedCellStyles, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Column(
        // 칸에 여백이 없어서 누른 면만 칸 밖으로 넓힌다
        modifier = modifier.clickable(
            interactionSource = null,
            indication = pressIndication(horizontalOutset = CellPressOutset, verticalOutset = CellPressOutset),
            onClickLabel = stringResource(Res.string.sheet_open, cell.label),
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = cell.label,
                modifier = Modifier.weight(1f, fill = false),
                style = styles.label,
                color = colors.t2,
                maxLines = 1,
                autoSize = shrinkToFit(styles.label.fontSize),
            )
            Spacer(Modifier.width(3.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 10.dp)
        }
        // 달라진 점 칸과 같은 간격이다
        Spacer(Modifier.height(6.dp))
        OvalitRollingText(
            text = cell.value,
            style = styles.value,
            color = cell.valueColor,
            autoSize = shrinkToFit(styles.value.fontSize, min = 14.sp),
        )
        Spacer(Modifier.height(2.dp))
        MetricSubLine(cell.change, cell.changeColor, cell.usual, styles.subLine)
    }
}

/** 비교 기준이 없을 때만 고정 칸 밑에 그 까닭을 적습니다. 있으면 칸마다 "평소 177"이 무엇과 견줬는지 말합니다. */
@Composable
internal fun FixedMetricSummary(baseline: Baseline?, modifier: Modifier = Modifier) {
    if (baseline != null) return
    OvalitText(
        text = stringResource(Res.string.baseline_missing),
        modifier = modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 10.dp),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
    )
}
