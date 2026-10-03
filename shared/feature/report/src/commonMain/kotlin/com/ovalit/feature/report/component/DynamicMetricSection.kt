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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.rememberFitsOnOneLine
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.format
import com.ovalit.feature.report.hasGoodDirection
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.dynamic_caption
import com.ovalit.feature.report.resources.dynamic_caption_with_role
import com.ovalit.feature.report.resources.dynamic_title_moved
import com.ovalit.feature.report.resources.dynamic_title_steady
import com.ovalit.feature.report.resources.dynamic_title_unknown
import com.ovalit.feature.report.resources.dynamic_unknown_hint
import com.ovalit.feature.report.resources.dynamic_usual
import com.ovalit.feature.report.resources.sheet_open
import org.jetbrains.compose.resources.stringResource

// 목업은 옆으로 밀지만 세 번째 칸이 화면 끝에서 잘려 숫자가 끊긴다. 한 줄에 세 칸씩 폭을 나누고 넘치면 다음 줄로
// 넘긴다(DECISIONS 2026-09-25).
private const val COLUMNS_PER_ROW = 3
// 이름 뒤 간격 3dp와 화살표 10dp를 더한 폭이다. 고정 칸도 같은 값을 쓴다.
private val ChevronSpace = 13.dp
private val ColumnGap = 14.dp
private val RowGap = 20.dp

/** @param onOpenMetric 칸을 누르면 그 지표의 표본과 판단 근거를 시트로 엽니다. */
@Composable
internal fun DynamicMetricSection(
    report: WeeklyReport.Ready,
    modifier: Modifier = Modifier,
    onOpenMetric: (DynamicMetric) -> Unit = {},
) {
    val typography = OvalitTheme.typography
    val columns = report.dynamic.map { slot -> dynamicColumn(slot, report.metrics, report.baseline) }
    if (columns.isEmpty()) return

    Column(modifier = modifier) {
        DynamicSectionTitle(report)
        Spacer(Modifier.height(SectionTitleGap))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 이름, 숫자, 설명마다 모든 칸에 한 크기를 쓴다. 칸마다 따로 줄이면 긴 이름만 작아지고 그 칸의 숫자와
            // 설명만 다른 높이에 놓인다. 둘째 줄도 첫 줄과 칸 폭이 같다.
            val perRow = columns.size.coerceAtMost(COLUMNS_PER_ROW)
            val gaps = (ColumnGap * 2 + 1.dp) * (perRow - 1)
            val columnWidth = (currentMaxWidth - OvalitSpacing.gutter * 2 - gaps) / perRow
            val valueStyle = rememberFittingStyle(columns.map { it.value }, typography.metricM, columnWidth, min = 14.sp)
            val changeStyle = typography.metricS
            // 가장 작은 글자로도 한 줄에 안 들어가는 이름이 있으면 모든 칸 이름을 두 줄로 꺾는다. 그때는 가장 긴 어절이
            // 들어가는 크기를 쓴다.
            val labels = columns.map { it.label }
            val labelWidth = columnWidth - ChevronSpace
            val oneLineLabel = rememberFittingStyle(labels, typography.caption, labelWidth)
            val wordLabel = rememberFittingStyle(labels.flatMap { it.split(' ') }, typography.caption, labelWidth)
            val wrapLabels = !rememberFitsOnOneLine(labels.map { AnnotatedString(it) }, oneLineLabel, labelWidth)
            val styles = DynamicColumnStyles(
                label = if (wrapLabels) wordLabel else oneLineLabel,
                wrapLabels = wrapLabels,
                value = valueStyle,
                change = changeStyle,
                caption = rememberFittingStyle(columns.map { it.usual }, typography.caption, columnWidth),
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
                                // 간격은 구분선 양옆에만 준다. 칸 폭 안에 넣으면 칸마다 내용 폭이 달라진다.
                                Spacer(Modifier.width(ColumnGap))
                                // 덜 찬 줄의 빈자리에는 구분선을 긋지 않는다
                                if (column != null) VerticalLine() else Spacer(Modifier.width(1.dp))
                                Spacer(Modifier.width(ColumnGap))
                            }
                            if (column == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                // 큐를 바꾸면 칸의 지표가 아예 바뀌기도 한다. 그때 숫자를 굴리면 같은 지표가 변한 것처럼
                                // 보여서 칸을 자리가 아니라 지표로 묶는다. 같은 지표일 때만 숫자가 구른다.
                                key(column.slot.metric) {
                                    DynamicMetricColumn(
                                        column = column,
                                        styles = styles,
                                        onClick = { onOpenMetric(column.slot.metric) },
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
    val usual: String,
)

private class DynamicColumnStyles(
    val label: TextStyle,
    val wrapLabels: Boolean,
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
        // 칸 밑에는 평소 값만 둔다. 표본은 시트에 있다. 판단을 보류한 칸도 평균이 있으면 적고, 없으면 대시만 둔다.
        // "비교할 기록이 모자라요"를 칸마다 쓰면 좁은 칸에서 잘리고 제목과 같은 말이 다섯 번 뜬다.
        usual = stringResource(Res.string.dynamic_usual, usual?.let { metric.format.valueText(it) } ?: NO_VALUE),
    )
}

@Composable
private fun DynamicMetricColumn(
    column: DynamicColumn,
    styles: DynamicColumnStyles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    Column(
        modifier = modifier.clickable(
            onClickLabel = stringResource(Res.string.sheet_open, column.label),
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        // 이름과 설명은 칸마다 따로 꺾지 않는다. 한 칸만 두 줄이 되면 그 칸 숫자만 한 줄 아래로 내려간다. 이름이 꺾일 때는
        // 모든 칸이 두 줄을 차지한다.
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = column.label,
                modifier = Modifier.weight(1f, fill = false),
                style = styles.label,
                color = colors.t2,
                maxLines = if (styles.wrapLabels) 2 else 1,
                minLines = if (styles.wrapLabels) 2 else 1,
                autoSize = if (styles.wrapLabels) null else shrinkToFit(styles.label.fontSize),
            )
            Spacer(Modifier.width(3.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 10.dp)
        }
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
        OvalitText(
            text = column.usual,
            style = styles.caption,
            color = colors.t3,
            maxLines = 1,
            autoSize = shrinkToFit(styles.caption.fontSize),
        )
    }
}

// 움직였다고 판단한 칸만 오르내림 색을 칠한다(CLAUDE.md 디자인). 달라진 점 시트도 이 색을 쓴다.
@Composable
internal fun changeColor(slot: DynamicSlot, current: Double, usual: Double): Color = when {
    slot.movement != Movement.MOVED -> OvalitTheme.colors.t3
    !slot.metric.hasGoodDirection -> OvalitTheme.colors.t1
    else -> directionColor(slot.metric.format, current, usual)
}
