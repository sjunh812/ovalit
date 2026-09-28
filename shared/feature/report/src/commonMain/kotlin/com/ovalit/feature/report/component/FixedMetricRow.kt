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
import androidx.compose.ui.text.SpanStyle
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
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaRatioText
import com.ovalit.core.ui.label
import com.ovalit.core.ui.perMatchKda
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.sheet_open
import com.ovalit.feature.report.resources.summary_compared
import com.ovalit.feature.report.resources.summary_kda
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
        // 이름, 숫자, 변화량마다 모든 칸에 한 크기를 쓴다. 칸마다 따로 줄이면 "전투점수"만 작아지고 그 칸 숫자만
        // 위로 올라가 줄이 어긋난다.
        val gaps = (CellGap * 2 + 1.dp) * (cells.size - 1)
        val cellWidth = (currentMaxWidth - OvalitSpacing.gutter * 2 - gaps) / cells.size
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
                        styles = styles,
                        onClick = { onOpenMetric(cell.metric) },
                        modifier = Modifier.weight(1f),
                    )
                }
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
                autoSize = shrinkToFit(styles.label.fontSize),
            )
            Spacer(Modifier.width(3.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 10.dp)
        }
        OvalitRollingText(
            text = cell.value,
            style = styles.value,
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

/** 고정 칸 밑 두 줄입니다. 첫 줄에 KDA와 판당 K/D/A를, 둘째 줄에 변화량을 무엇과 견줬는지 적습니다. */
@Composable
internal fun FixedMetricSummary(
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption

    Column(
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        // 사용자 결정(2026-09-27): 어시스트가 킬만큼 중요해져서 KDA를 먼저, 한 단계 크게 둔다. KDA만으로는 몇 킬
        // 몇 데스인지 몰라 판당 K/D/A를 옆에 붙인다. 좁으면 판당 K/D/A가 통째로 다음 줄로 내려간다.
        // 사용자 결정(2026-09-27): KDA에도 고정 칸처럼 지난 평균과의 변화량을 붙인다.
        val perMatchText = metrics.perMatchKda()?.let { (kills, deaths, assists) ->
            stringResource(Res.string.summary_kda, kills, deaths, assists)
        }
        val label = OvalitTheme.typography.label
        val kda = metrics.kda
        val usualKda = baseline?.metrics?.kda
        val ratio = kda?.let {
            kdaRatioText(
                kda = it,
                below = colors.t1,
                label = SpanStyle(fontSize = label.fontSize, fontWeight = label.fontWeight, color = colors.t2),
            )
        }
        SeparatedRow(
            items = listOfNotNull<@Composable () -> Unit>(
                ratio?.let {
                    {
                        Row {
                            OvalitText(text = it, modifier = Modifier.alignByBaseline(), style = kdaValueStyle(), color = colors.t1)
                            // 고정 칸과 같이 보이는 두 자리끼리 빼고 오르면 초록, 내리면 빨강이다
                            if (kda != null && usualKda != null) {
                                // KDA 숫자에 딸린 값이라 붙이고, 숫자보다 눈에 덜 띄게 한 단계 작게 둔다
                                Spacer(Modifier.width(3.dp))
                                OvalitText(
                                    text = MetricFormat.TWO_DECIMALS.formatChange(kda, usualKda),
                                    modifier = Modifier.alignByBaseline(),
                                    style = OvalitTheme.typography.metricS.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                    color = directionColor(MetricFormat.TWO_DECIMALS, kda, usualKda),
                                )
                            }
                        }
                    }
                },
                perMatchText?.let { { OvalitText(text = it, style = caption, color = colors.t3) } },
            ),
            separator = { Spacer(Modifier.width(OvalitSpacing.sm)) },
            alignBaseline = true,
        )
        // 사용자 결정(2026-09-27): 무엇과 견준 변화량인지만 적는다. 네 칸의 평균을 한 줄에 늘어놓으면 어느 숫자가
        // 어느 칸 것인지 읽히지 않았다. 칸마다 평균은 S1-a 시트에, 경기 수는 기간 줄에 있다.
        OvalitText(
            text = if (baseline != null) {
                stringResource(Res.string.summary_compared, baseline.weeks)
            } else {
                stringResource(Res.string.baseline_missing)
            },
            style = caption,
            color = colors.t3,
        )
    }
}

// 제목 글꼴에는 tnum이 없어서 붙인다. 자릿수가 바뀌어도 KDA 줄이 흔들리지 않는다.
@Composable
private fun kdaValueStyle(): TextStyle = OvalitTheme.typography.titleM.copy(fontFeatureSettings = "tnum")
