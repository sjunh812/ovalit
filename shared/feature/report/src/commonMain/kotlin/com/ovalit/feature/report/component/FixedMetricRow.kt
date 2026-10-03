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
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.ui.NO_VALUE
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
 * 홈 위쪽 고정 칸입니다. 전투점수, K/D, 피해량 / 헤드샷, KDA를 한 줄에 세 칸씩 놓고, 칸마다 이름, 숫자, 그 밑에
 * "+75 · 평소 177"을 둡니다. 달라진 점과 같은 격자, 같은 칸 모양이라 두 카드의 칸 경계가 위아래로 맞습니다.
 *
 * 사용자 결정(2026-10-03): 목업의 네 칸 한 줄 밑에 KDA 넓은 칸을 두니 KDA만 따로 놀아 보였다. 다섯 칸을 같은 모양으로
 * 둔다. 판당 킬·데스·어시는 K/D에도 똑같이 필요한 풀이라 칸에서 빼고 K/D와 KDA 시트에 둔다. 칸 밖에 "변화량은 지난 4주
 * 평균과 비교했어요"를 따로 두니 무엇의 설명인지 붕 떠 보여서, 칸마다 평소 값을 붙여 변화량이 무엇과 견준 것인지 보인다.
 */
@Composable
internal fun FixedMetricRow(
    metrics: MatchMetrics,
    baseline: Baseline?,
    fixedMetrics: List<FixedMetric>,
    onOpenMetric: (FixedMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = OvalitTheme.typography
    val colors = OvalitTheme.colors

    val cells = fixedMetrics.map { metric ->
        val current = metric.value(metrics)
        val usual = baseline?.let { metric.value(it.metrics) }
        FixedCell(
            metric = metric,
            label = stringResource(metric.label),
            value = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            valueColor = if (metric == FixedMetric.KDA && current != null) kdaColor(current, below = colors.t1) else colors.t1,
            change = if (current != null && usual != null) metric.format.formatChange(current, usual) else null,
            changeColor = if (current != null && usual != null) directionColor(metric.format, current, usual) else colors.t3,
            usual = stringResource(Res.string.dynamic_usual, usual?.let { metric.format.valueText(it) } ?: NO_VALUE),
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 이름, 숫자, 변화량마다 모든 칸에 한 크기를 쓴다. 칸마다 따로 줄이면 "전투점수"만 작아지고 그 칸 숫자만
        // 위로 올라가 줄이 어긋난다.
        val perRow = cells.size.coerceAtMost(MetricColumns)
        val gaps = (MetricColumnGap * 2 + 1.dp) * (perRow - 1)
        val cellWidth = (currentMaxWidth - OvalitSpacing.gutter * 2 - gaps) / perRow
        val styles = FixedCellStyles(
            label = rememberFittingStyle(cells.map { it.label }, typography.caption, cellWidth - ChevronSpace),
            value = rememberFittingStyle(cells.map { it.value }, typography.metricM, cellWidth, min = 14.sp),
            subLine = rememberSubLineStyle(cells.map { it.change to it.usual }, cellWidth),
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
                            // 덜 찬 줄의 빈자리에는 구분선을 긋지 않는다
                            if (cell != null) VerticalLine() else Spacer(Modifier.width(1.dp))
                            Spacer(Modifier.width(MetricColumnGap))
                        }
                        if (cell == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            // 기타 모드로 바꾸면 첫 칸이 전투점수에서 K/D로 바뀐다. 자리로 묶으면 186이 1.34로 굴러가서 지표로 묶는다.
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
        modifier = modifier.clickable(
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
