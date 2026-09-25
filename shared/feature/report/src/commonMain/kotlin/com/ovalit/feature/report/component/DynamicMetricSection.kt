package com.ovalit.feature.report.component

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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

// 칸이 셋뿐이라 옆으로 밀지 않고 화면 폭을 나눠 갖는다. 목업처럼 세 번째 칸을 화면 끝에서 자르면 숫자가 끊겨
// 버그처럼 보였다. 간격은 구분선 양옆에만 준다. 칸 폭 안에 간격을 넣으면 첫 칸만 내용이 넓어진다.
private val ColumnGap = 14.dp

@Composable
internal fun DynamicMetricSection(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
        Spacer(Modifier.height(18.dp))
        DynamicSectionTitle(report)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = OvalitSpacing.gutter),
        ) {
            report.dynamic.forEachIndexed { index, slot ->
                if (index > 0) {
                    Spacer(Modifier.width(ColumnGap))
                    VerticalLine()
                    Spacer(Modifier.width(ColumnGap))
                }
                // 큐를 바꾸면 칸의 지표가 아예 바뀌기도 한다. 그때 숫자를 굴리면 같은 지표가 변한 것처럼 보여서
                // 지표마다 따로 그린다. 같은 지표일 때만 숫자가 구른다.
                key(slot.metric) {
                    DynamicMetricColumn(
                        slot = slot,
                        metrics = report.metrics,
                        baseline = report.baseline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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

@Composable
private fun DynamicMetricColumn(
    slot: DynamicSlot,
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    val metric = slot.metric
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }
    val judged = slot.movement != Movement.UNKNOWN && baseline != null && current != null && usual != null

    val caption = OvalitTheme.typography.caption
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        // 이름과 설명은 한 줄로 둔다. 좁은 칸에서 한 칸만 두 줄로 꺾이면 그 칸 숫자만 한 줄 아래로 내려간다.
        OvalitText(
            text = stringResource(metric.label),
            style = caption,
            color = colors.t2,
            maxLines = 1,
            autoSize = shrinkToFit(caption.fontSize, min = 7.sp),
        )
        Spacer(Modifier.height(6.dp))
        ValueWithChange(
            value = {
                OvalitRollingText(
                    text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                    style = OvalitTheme.typography.metricM,
                    color = if (judged) colors.t1 else colors.t2,
                    autoSize = shrinkToFit(OvalitTheme.typography.metricM.fontSize, min = 14.sp),
                )
            },
            change = if (judged) {
                {
                    OvalitText(
                        text = metric.format.formatChange(current, usual),
                        style = OvalitTheme.typography.metricS,
                        color = changeColor(slot, current, usual),
                        maxLines = 1,
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(6.dp))
        val lines = listOf(
            if (judged) {
                stringResource(Res.string.baseline_average, baseline.weeks, metric.format.valueText(usual))
            } else {
                stringResource(Res.string.baseline_missing)
            },
            metric.sampleText(metrics),
        )
        // 두 줄을 따로 줄이면 한 칸 안에서 글자 크기가 달라진다. 더 긴 줄에 맞춰 둘을 같은 크기로 줄인다.
        FittingLines(lines = lines, style = caption.merge(color = colors.t3))
    }
}

/**
 * 여러 줄을 한 크기로 그립니다. 가장 긴 줄이 칸에 안 들어가면 모든 줄을 같은 비율로 줄입니다.
 *
 * 칸 폭을 알아야 크기를 정할 수 있는데, 이 칸은 세로 구분선 높이를 맞추려고 크기를 미리 묻는 줄 안에 있습니다.
 * BoxWithConstraints는 그 질문을 받으면 앱이 죽어서, 글자를 직접 재고 그리는 레이아웃으로 짰습니다.
 */
@Composable
private fun FittingLines(lines: List<String>, style: TextStyle, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val drawn = remember { DrawnLines() }
    Layout(
        modifier = modifier
            .semantics { this[SemanticsProperties.Text] = lines.map { AnnotatedString(it) } }
            .drawBehind {
                var top = 0f
                drawn.layouts.forEach { line ->
                    drawText(line, topLeft = Offset(0f, top))
                    top += line.size.height
                }
            },
    ) { _, constraints ->
        fun measureAll(lineStyle: TextStyle) = lines.map { measurer.measure(it, lineStyle, maxLines = 1, softWrap = false) }
        val natural = measureAll(style)
        val widest = natural.maxOf { it.size.width }
        val layouts = if (constraints.hasBoundedWidth && widest > constraints.maxWidth) {
            val scale = (constraints.maxWidth.toFloat() / widest).coerceAtLeast(MIN_LINE_SCALE)
            measureAll(style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale))
        } else {
            natural
        }
        drawn.layouts = layouts
        val width = layouts.maxOf { it.size.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, layouts.sumOf { it.size.height }) {}
    }
}

private class DrawnLines(var layouts: List<TextLayoutResult> = emptyList())

// 이보다 작아지면 못 읽는다. 320dp 화면에 글꼴 1.5배에서도 여기까지 줄일 일은 없었다.
private const val MIN_LINE_SCALE = 0.6f

// 움직였다고 판단한 칸만 색을 칠한다. 평소 범위 안의 변화에 색을 칠하면 흔들림이 경고처럼 읽힌다.
@Composable
private fun changeColor(slot: DynamicSlot, current: Double, usual: Double): Color = when {
    slot.movement != Movement.MOVED -> OvalitTheme.colors.t3
    !slot.metric.hasGoodDirection -> OvalitTheme.colors.t1
    else -> directionColor(slot.metric.format, current, usual)
}
