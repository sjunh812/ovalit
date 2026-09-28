package com.ovalit.feature.report.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import com.ovalit.core.ui.perMatchKda
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.kda_assists
import com.ovalit.feature.report.resources.kda_deaths
import com.ovalit.feature.report.resources.kda_kills
import com.ovalit.feature.report.resources.kda_per_match
import com.ovalit.feature.report.resources.sheet_open
import com.ovalit.feature.report.resources.summary_compared
import org.jetbrains.compose.resources.stringResource

private val CellGap = 10.dp
private val ChevronSpace = 13.dp

// KDA 칸 왼쪽 숫자와 오른쪽 판당 표 사이. 이보다 좁아지면 표를 숫자 밑으로 내린다.
private val KdaTableGap = 16.dp
private val PerMatchColumnGap = 12.dp

/**
 * 홈 위쪽 고정 칸입니다. 전투점수, K/D, 피해량, 헤드샷을 한 줄 칸에 두고, KDA는 그 밑에 한 줄을 다 쓰는 넓은 칸으로
 * 둡니다. 넓은 칸 오른쪽에는 판당 킬·데스·어시를 풀어 KDA가 어디서 나왔는지 같이 보입니다.
 *
 * 사용자 결정(2026-09-29): 어시스트가 킬만큼 중요해져 KDA를 칸 밑 작은 줄에서 고정 칸과 같은 숫자 크기로 올렸다. 목업의 네
 * 칸 한 줄은 그대로 둔다.
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

    @Composable
    fun cellOf(metric: FixedMetric): FixedCell {
        val current = metric.value(metrics)
        val usual = baseline?.let { metric.value(it.metrics) }
        return FixedCell(
            metric = metric,
            value = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
            valueColor = if (metric == FixedMetric.KDA && current != null) kdaColor(current, below = colors.t1) else colors.t1,
            change = if (current != null && usual != null) metric.format.formatChange(current, usual) else null,
            changeColor = if (current != null && usual != null) directionColor(metric.format, current, usual) else null,
        )
    }
    val cells = fixedMetrics.filter { it != FixedMetric.KDA }.map { cellOf(it) }
    val kda = fixedMetrics.firstOrNull { it == FixedMetric.KDA }?.let { cellOf(it) }
    val labels = cells.map { stringResource(it.metric.label) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 이름, 숫자, 변화량마다 모든 칸에 한 크기를 쓴다. 칸마다 따로 줄이면 "전투점수"만 작아지고 그 칸 숫자만
        // 위로 올라가 줄이 어긋난다. KDA 칸도 같은 크기로 둬야 다섯 숫자가 한 무리로 읽힌다.
        val gaps = (CellGap * 2 + 1.dp) * (cells.size - 1)
        val cellWidth = (currentMaxWidth - OvalitSpacing.gutter * 2 - gaps) / cells.size
        val styles = FixedCellStyles(
            label = rememberFittingStyle(labels, typography.caption, cellWidth - ChevronSpace),
            value = rememberFittingStyle(cells.map { it.value }, typography.metricM, cellWidth, min = 14.sp),
            change = rememberFittingStyle(cells.mapNotNull { it.change }, typography.metricS, cellWidth),
        )
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = OvalitSpacing.gutter),
            ) {
                // 간격은 구분선 양옆에만 준다. 칸 폭 안에 넣으면 칸마다 내용 폭이 달라진다.
                cells.forEachIndexed { index, cell ->
                    if (index > 0) {
                        Spacer(Modifier.width(CellGap))
                        VerticalLine()
                        Spacer(Modifier.width(CellGap))
                    }
                    // 기타 모드로 바꾸면 첫 칸이 전투점수에서 K/D로 바뀐다. 자리로 묶으면 186이 1.34로 굴러가서 지표로 묶는다.
                    key(cell.metric) {
                        FixedMetricCell(
                            cell = cell,
                            label = labels[index],
                            styles = styles,
                            onClick = { onOpenMetric(cell.metric) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            kda?.let {
                // 위 네 칸의 둘째 줄처럼 읽히지 않게 옅은 선으로 떼어 따로 한 칸임을 보인다
                Spacer(Modifier.height(14.dp))
                Spacer(
                    Modifier
                        .padding(horizontal = OvalitSpacing.gutter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.lineWeak),
                )
                Spacer(Modifier.height(12.dp))
                KdaCell(
                    cell = it,
                    perMatch = metrics.perMatchKda(),
                    styles = styles,
                    onClick = { onOpenMetric(FixedMetric.KDA) },
                    modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                )
            }
        }
    }
}

private class FixedCell(
    val metric: FixedMetric,
    val value: String,
    val valueColor: Color,
    val change: String?,
    val changeColor: Color?,
)

private class FixedCellStyles(val label: TextStyle, val value: TextStyle, val change: TextStyle)

@Composable
private fun FixedMetricCell(
    cell: FixedCell,
    label: String,
    styles: FixedCellStyles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(
            onClickLabel = stringResource(Res.string.sheet_open, label),
            role = Role.Button,
            onClick = onClick,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CellLabel(label, styles.label)
        OvalitRollingText(
            text = cell.value,
            style = styles.value,
            color = cell.valueColor,
            autoSize = shrinkToFit(styles.value.fontSize, min = 14.sp),
        )
        if (cell.change != null && cell.changeColor != null) {
            OvalitText(
                text = cell.change,
                style = styles.change,
                color = cell.changeColor,
                maxLines = 1,
                autoSize = shrinkToFit(styles.change.fontSize),
            )
        }
    }
}

@Composable
private fun CellLabel(label: String, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = style,
            color = OvalitTheme.colors.t2,
            maxLines = 1,
            autoSize = shrinkToFit(style.fontSize),
        )
        Spacer(Modifier.width(3.dp))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 10.dp)
    }
}

// 왼쪽은 다른 칸처럼 이름, 숫자와 변화량이다. 변화량은 넓은 칸이라 숫자 옆에 붙인다.
@Composable
private fun KdaCell(
    cell: FixedCell,
    perMatch: List<String>?,
    styles: FixedCellStyles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(cell.metric.label)
    SideOrBelow(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(Res.string.sheet_open, label), role = Role.Button, onClick = onClick),
        gap = KdaTableGap,
        start = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CellLabel(label, styles.label)
                Row {
                    OvalitRollingText(
                        text = cell.value,
                        modifier = Modifier.alignByBaseline(),
                        style = styles.value,
                        color = cell.valueColor,
                    )
                    if (cell.change != null && cell.changeColor != null) {
                        Spacer(Modifier.width(6.dp))
                        OvalitText(
                            text = cell.change,
                            modifier = Modifier.alignByBaseline(),
                            style = styles.change,
                            color = cell.changeColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        end = perMatch?.let { { PerMatchTable(it) } },
    )
}

// "판당"을 줄 머리로, 킬·데스·어시를 열 머리로 둔 한 줄 표다. 숫자는 KDA보다 한참 작게 둬 KDA가 먼저 읽힌다.
@Composable
private fun PerMatchTable(values: List<String>) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    val valueStyle = OvalitTheme.typography.metricS.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val headers = listOf(Res.string.kda_kills, Res.string.kda_deaths, Res.string.kda_assists).map { stringResource(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(PerMatchColumnGap)) {
        OvalitText(
            text = stringResource(Res.string.kda_per_match),
            modifier = Modifier.alignBy(LastBaseline),
            style = caption,
            color = colors.t3,
        )
        headers.zip(values).forEach { (header, value) ->
            // 낭독기는 한 열을 "킬 15.4"로 읽는다
            Column(
                modifier = Modifier.alignBy(LastBaseline).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.End,
            ) {
                OvalitText(text = header, style = caption, color = colors.t3)
                OvalitText(text = value, style = valueStyle, color = colors.t1)
            }
        }
    }
}

/**
 * [start]를 왼쪽, [end]를 오른쪽 끝에 두고 두 쪽의 마지막 글자 기준선을 맞춥니다. 사이가 [gap]보다 좁아지면 [end]를
 * [start] 밑으로 내립니다. 좁은 화면이나 큰 글꼴에서 판당 표가 KDA 숫자를 덮지 않게 합니다.
 */
@Composable
private fun SideOrBelow(
    gap: Dp,
    start: @Composable () -> Unit,
    end: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(start, end ?: {}), modifier = modifier) { (starts, ends), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val left = starts.first().measure(loose)
        val right = ends.firstOrNull()?.measure(loose)
        val width = constraints.maxWidth
        if (right == null) return@Layout layout(width, left.height) { left.place(0, 0) }
        val fits = left.width + gap.roundToPx() + right.width <= width
        if (!fits) {
            val below = 10.dp.roundToPx()
            return@Layout layout(width, left.height + below + right.height) {
                left.place(0, 0)
                right.place(0, left.height + below)
            }
        }
        val leftBaseline = left[LastBaseline].takeIf { it != AlignmentLine.Unspecified } ?: left.height
        val rightBaseline = right[LastBaseline].takeIf { it != AlignmentLine.Unspecified } ?: right.height
        val top = maxOf(leftBaseline, rightBaseline)
        val height = maxOf(top - leftBaseline + left.height, top - rightBaseline + right.height)
        layout(width, height) {
            left.place(0, top - leftBaseline)
            right.place(width - right.width, top - rightBaseline)
        }
    }
}

/** 고정 칸 밑 한 줄입니다. 변화량을 무엇과 견줬는지 적습니다. */
@Composable
internal fun FixedMetricSummary(baseline: Baseline?, modifier: Modifier = Modifier) {
    // 사용자 결정(2026-09-27): 무엇과 견준 변화량인지만 적는다. 칸마다 평균을 한 줄에 늘어놓으면 어느 숫자가 어느 칸
    // 것인지 읽히지 않았다. 칸마다 평균은 S1-a 시트에, 경기 수는 기간 줄에 있다.
    OvalitText(
        text = if (baseline != null) {
            stringResource(Res.string.summary_compared, baseline.weeks)
        } else {
            stringResource(Res.string.baseline_missing)
        },
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
    )
}
