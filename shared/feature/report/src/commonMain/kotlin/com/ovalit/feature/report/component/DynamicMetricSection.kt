package com.ovalit.feature.report.component

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitRollingText
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.hasGoodDirection
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_average
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.dynamic_caption
import com.ovalit.feature.report.resources.dynamic_caption_with_role
import com.ovalit.feature.report.resources.dynamic_title_moved
import com.ovalit.feature.report.resources.dynamic_title_steady
import com.ovalit.feature.report.resources.dynamic_title_unknown
import com.ovalit.feature.report.resources.dynamic_unknown_hint
import com.ovalit.feature.report.sampleText
import org.jetbrains.compose.resources.stringResource

// 한 줄에 세 칸씩 화면 폭을 나눠 갖고, 넷이나 다섯이면 다음 줄로 넘긴다. 목업처럼 옆으로 밀면 세 번째 칸이 화면
// 끝에서 잘려 숫자가 끊겨 버그처럼 보였다. 간격은 구분선 양옆에만 준다. 칸 폭 안에 간격을 넣으면 첫 칸만 내용이
// 넓어진다.
private const val COLUMNS_PER_ROW = 3
private val ColumnGap = 14.dp
private val RowGap = 20.dp

@Composable
internal fun DynamicMetricSection(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val typography = OvalitTheme.typography
    val columns = report.dynamic.map { slot -> dynamicColumn(slot, report.metrics, report.baseline) }

    Column(modifier = modifier) {
        HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
        Spacer(Modifier.height(18.dp))
        DynamicSectionTitle(report)
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 모든 칸의 이름, 숫자, 설명을 줄마다 한 크기로 맞춘다. 칸마다 따로 줄이면 긴 이름만 작아지고 그 칸의
            // 숫자와 설명만 다른 높이에 놓인다. 둘째 줄도 첫 줄과 칸 폭이 같다.
            val perRow = columns.size.coerceAtMost(COLUMNS_PER_ROW)
            val gaps = (ColumnGap * 2 + 1.dp) * (perRow - 1)
            val columnWidth = (maxWidth - OvalitSpacing.gutter * 2 - gaps) / perRow
            val valueStyle = rememberFittingStyle(columns.map { it.value }, typography.metricM, columnWidth, min = 14.sp)
            val changeStyle = typography.metricS
            val styles = DynamicColumnStyles(
                label = rememberFittingStyle(columns.map { it.label }, typography.caption, columnWidth),
                value = valueStyle,
                change = changeStyle,
                caption = rememberFittingStyle(columns.flatMap { it.lines }, typography.caption, columnWidth),
                stacked = needsStacking(columns, valueStyle, changeStyle, columnWidth),
            )
            Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
                columns.chunked(perRow).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = OvalitSpacing.gutter),
                    ) {
                        repeat(perRow) { index ->
                            val column = row.getOrNull(index)
                            if (index > 0) {
                                Spacer(Modifier.width(ColumnGap))
                                // 덜 찬 줄의 빈자리에는 구분선을 긋지 않는다
                                if (column != null) VerticalLine() else Spacer(Modifier.width(1.dp))
                                Spacer(Modifier.width(ColumnGap))
                            }
                            if (column == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                // 큐를 바꾸면 칸의 지표가 아예 바뀌기도 한다. 그때 숫자를 굴리면 같은 지표가 변한 것처럼
                                // 보여서 지표마다 따로 그린다. 같은 지표일 때만 숫자가 구른다.
                                key(column.slot.metric) {
                                    DynamicMetricColumn(column = column, styles = styles, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 숫자 옆에 변화량이 한 칸이라도 안 들어가면 모든 칸의 변화량을 숫자 아래로 내린다
@Composable
private fun needsStacking(columns: List<DynamicColumn>, valueStyle: TextStyle, changeStyle: TextStyle, width: Dp): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(columns, valueStyle, changeStyle, width, density, measurer) {
        val available = with(density) { width.toPx() }
        val gap = with(density) { ValueChangeGap.toPx() }
        columns.any { column ->
            val change = column.change ?: return@any false
            val valueWidth = measurer.measure(column.value, valueStyle, softWrap = false, maxLines = 1).size.width
            val changeWidth = measurer.measure(change, changeStyle, softWrap = false, maxLines = 1).size.width
            valueWidth + gap + changeWidth > available
        }
    }
}

@Composable
private fun DynamicSectionTitle(report: WeeklyReport.Ready) {
    val movements = report.dynamic.map { it.movement }

    // 판단을 보류한 칸만 있을 때 "큰 변화 없음"이라고 하면 안 된다. 변화가 없는 게 아니라 모르는 것이다.
    val title = when {
        Movement.MOVED in movements -> stringResource(Res.string.dynamic_title_moved)
        Movement.STEADY in movements -> stringResource(Res.string.dynamic_title_steady, periodLabel(report.period))
        else -> stringResource(Res.string.dynamic_title_unknown)
    }
    val caption = report.baseline?.let { baseline ->
        report.mainRole?.let { role ->
            stringResource(Res.string.dynamic_caption_with_role, stringResource(role.label), baseline.weeks)
        } ?: stringResource(Res.string.dynamic_caption, baseline.weeks)
    }

    Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = title,
            titleStyle = OvalitTheme.typography.bodyStrong,
            caption = caption?.let(::AnnotatedString),
        )
        if (movements.all { it == Movement.UNKNOWN }) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.dynamic_unknown_hint),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
            )
        }
    }
}

private class DynamicColumn(
    val slot: DynamicSlot,
    val label: String,
    val value: String,
    val valueColor: Color,
    val change: String?,
    val changeColor: Color,
    val lines: List<String>,
)

private class DynamicColumnStyles(
    val label: TextStyle,
    val value: TextStyle,
    val change: TextStyle,
    val caption: TextStyle,
    val stacked: Boolean,
)

@Composable
private fun dynamicColumn(slot: DynamicSlot, metrics: MatchMetrics, baseline: Baseline?): DynamicColumn {
    val colors = OvalitTheme.colors
    val metric = slot.metric
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }
    val judged = slot.movement != Movement.UNKNOWN && baseline != null && current != null && usual != null
    return DynamicColumn(
        slot = slot,
        label = stringResource(metric.label),
        value = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
        valueColor = if (judged) colors.t1 else colors.t2,
        change = if (judged) metric.format.formatChange(current, usual) else null,
        changeColor = if (judged) changeColor(slot, current, usual) else colors.t3,
        lines = listOf(
            if (judged) {
                stringResource(Res.string.baseline_average, baseline.weeks, metric.format.valueText(usual))
            } else {
                stringResource(Res.string.baseline_missing)
            },
            metric.sampleText(metrics),
        ),
    )
}

@Composable
private fun DynamicMetricColumn(column: DynamicColumn, styles: DynamicColumnStyles, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        // 이름과 설명은 한 줄로 둔다. 좁은 칸에서 한 칸만 두 줄로 꺾이면 그 칸 숫자만 한 줄 아래로 내려간다.
        OvalitText(
            text = column.label,
            style = styles.label,
            color = colors.t2,
            maxLines = 1,
            autoSize = shrinkToFit(styles.label.fontSize, min = 7.sp),
        )
        Spacer(Modifier.height(6.dp))
        ValueWithChange(
            value = {
                OvalitRollingText(
                    text = column.value,
                    style = styles.value,
                    color = column.valueColor,
                    autoSize = shrinkToFit(styles.value.fontSize, min = 14.sp),
                )
            },
            change = column.change?.let { change ->
                {
                    OvalitText(
                        text = change,
                        style = styles.change,
                        color = column.changeColor,
                        maxLines = 1,
                    )
                }
            },
            stacked = styles.stacked,
        )
        Spacer(Modifier.height(6.dp))
        column.lines.forEach { line ->
            OvalitText(
                text = line,
                style = styles.caption,
                color = colors.t3,
                maxLines = 1,
                autoSize = shrinkToFit(styles.caption.fontSize, min = 7.sp),
            )
        }
    }
}

// 움직였다고 판단한 칸만 색을 칠한다. 평소 범위 안의 변화에 색을 칠하면 흔들림이 경고처럼 읽힌다.
@Composable
private fun changeColor(slot: DynamicSlot, current: Double, usual: Double): Color = when {
    slot.movement != Movement.MOVED -> OvalitTheme.colors.t3
    !slot.metric.hasGoodDirection -> OvalitTheme.colors.t1
    else -> directionColor(slot.metric.format, current, usual)
}
