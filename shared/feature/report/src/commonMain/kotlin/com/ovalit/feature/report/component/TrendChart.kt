package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MIN_TREND_ROUNDS
import com.ovalit.core.model.TREND_WEEKS
import com.ovalit.core.model.TrendWeek
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.SEPARATOR
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.period_last_week
import com.ovalit.core.ui.resources.period_this_week
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.period_date_range
import com.ovalit.feature.report.resources.sheet_trend_act_marker
import com.ovalit.feature.report.resources.sheet_trend_description
import com.ovalit.feature.report.resources.sheet_trend_start
import com.ovalit.feature.report.resources.trend_baseline_note
import com.ovalit.feature.report.resources.trend_sparse_note
import com.ovalit.feature.report.resources.trend_week_empty
import com.ovalit.feature.report.resources.trend_week_sample
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

internal val BarGap = 5.dp

// 8주 중 가장 낮은 값이 차지하는 막대 높이 비율이다. 0부터 그리면 피해량 140과 165가 거의 같은 높이라 추이가 안 보인다.
private const val LOWEST_BAR = 0.35f

// 한 주를 고르면 나머지 막대는 이만큼만 남겨 고른 막대가 먼저 보이게 한다
private const val DIMMED_BAR = 0.35f

/**
 * S1-a, 8주 흐름 시트, 홈 흐름 입구가 같이 쓰는 주별 막대입니다. 기간에 든 주만 `--accent`로 칠하고, 경기가 없는 주는 바닥
 * 선만 남기며, 라운드가 모자란 주는 테두리만 그립니다. 액트가 바뀐 곳에는 세로선을 긋습니다. 낭독기는 주마다 값을 읽습니다.
 *
 * @param selected 고른 주의 자리입니다. 고르면 그 막대만 밝게 두고 나머지는 흐리게 합니다.
 * @param onSelect 막대를 누르거나 옆으로 끌면 고른 주를 알립니다. 고른 막대를 다시 누르면 `null`입니다. 없으면 누를 수 없습니다.
 * @param baseline 지난 4주 평균입니다. 있으면 그 높이에 점선을 긋습니다.
 * @param restColor 기간 밖 막대 색입니다. 면 위에 그릴 때는 면과 갈리게 진하게 넘깁니다.
 */
@Composable
internal fun TrendBarRow(
    metric: FixedMetric,
    report: WeeklyReport.Ready,
    modifier: Modifier = Modifier,
    gap: Dp = BarGap,
    selected: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    baseline: Double? = null,
    restColor: Color = OvalitTheme.colors.t5,
) {
    val colors = OvalitTheme.colors
    val values = report.trend.map { week -> week.metrics?.let(metric.value) }
    // 점선도 같은 눈금에 올린다. 데스가 없어 K/D 막대가 빠진 주가 있으면 평균이 막대 범위를 벗어날 수 있다.
    val scale = values.filterNotNull() + listOfNotNull(baseline)
    val low = scale.minOrNull() ?: 0.0
    val high = scale.maxOrNull() ?: 0.0
    val description = stringResource(
        Res.string.sheet_trend_description,
        stringResource(metric.label),
        values.map { value -> value?.let { metric.format.valueText(it) } ?: NO_VALUE }.joinToString(SEPARATOR),
    )
    val lineColor = colors.t3
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val count = report.trend.size

    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .then(
                if (onSelect == null) {
                    Modifier
                } else {
                    Modifier
                        .pointerInput(count) {
                            detectTapGestures { offset ->
                                val index = weekAt(offset.x, size.width, count)
                                currentOnSelect?.invoke(if (index == currentSelected) null else index)
                            }
                        }
                        // 옆으로 끌면 손가락 밑 주를 따라 고른다
                        .pointerInput(count) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset -> currentOnSelect?.invoke(weekAt(offset.x, size.width, count)) },
                            ) { change, _ -> currentOnSelect?.invoke(weekAt(change.position.x, size.width, count)) }
                        }
                },
            )
            .then(
                if (baseline == null) {
                    Modifier
                } else {
                    Modifier.drawWithContent {
                        drawContent()
                        val y = size.height * (1 - barFraction(baseline, low, high))
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        )
                    }
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom,
    ) {
        report.trend.forEachIndexed { index, week ->
            if (week.startsNewAct) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.t4))
            }
            Bar(
                fraction = values[index]?.let { barFraction(it, low, high) },
                color = barColor(week, index, selected, restColor),
                sparse = week.sparse,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// 위의 큰 숫자는 기간에 든 주들의 합계로 낸 값이라 그 주들만 --accent다. 나머지는 --bar로 두니 카드와 시트 위에서 거의 안
// 보여 금색만 떠 보였다(사용자 요청, 2026-10-03).
@Composable
private fun barColor(week: TrendWeek, index: Int, selected: Int?, rest: Color): Color {
    val colors = OvalitTheme.colors
    // 라운드가 모자란 주는 테두리만 그려서 면을 칠한 막대보다 한 단계 진하게 둔다
    val base = when {
        week.inPeriod -> colors.accent
        week.sparse -> colors.t4
        else -> rest
    }
    return when (selected) {
        null -> base
        index -> if (week.inPeriod) colors.accent else colors.t3
        else -> base.copy(alpha = base.alpha * DIMMED_BAR)
    }
}

private fun weekAt(x: Float, width: Int, count: Int): Int =
    (x / width * count).toInt().coerceIn(0, count - 1)

private fun barFraction(value: Double, low: Double, high: Double): Float =
    if (high <= low) 1f else LOWEST_BAR + (1 - LOWEST_BAR) * ((value - low) / (high - low)).toFloat().coerceIn(0f, 1f)

// 경기가 없는 주도 바닥 선을 남긴다. 칸이 빠지면 몇 주 전인지 셀 수 없다.
@Composable
private fun Bar(fraction: Float?, color: Color, sparse: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(2.dp)
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        fraction == null -> Modifier.height(2.dp).background(OvalitTheme.colors.fill, shape)
                        sparse -> Modifier.fillMaxHeight(fraction).border(1.dp, color, shape)
                        else -> Modifier.fillMaxHeight(fraction).background(color, shape)
                    },
                ),
        )
    }
}

/**
 * 막대 위 한 줄입니다. 고른 주가 없으면 리포트 기간을, 고르면 그 주를 적습니다. [metric]을 주면 그 지표 값도 붙입니다.
 * 좁으면 표본을 다음 줄로 내립니다.
 */
@Composable
internal fun TrendReadout(
    report: WeeklyReport.Ready,
    selected: Int?,
    modifier: Modifier = Modifier,
    metric: FixedMetric? = null,
) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    val week = selected?.let { report.trend.getOrNull(it) }
    val first = week?.firstDay ?: report.period.firstDay
    val last = week?.firstDay?.plus(6, DateTimeUnit.DAY) ?: report.period.lastDay
    val metrics = if (week == null) report.metrics else week.metrics
    val value = metrics?.let { metric?.value?.invoke(it) }

    FlowRow(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = if (week == null) periodLabel(report.period) else weekLabel(report, selected),
                style = if (metric == null) typography.bodyStrong else typography.label,
                color = colors.t1,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            OvalitText(
                text = stringResource(Res.string.period_date_range, first.month.number, first.day, last.month.number, last.day),
                style = typography.caption,
                color = colors.t3,
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (metric != null) {
                OvalitText(
                    text = value?.let { metric.format.valueText(it) } ?: NO_VALUE,
                    style = typography.metricS,
                    color = colors.t1,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
            }
            OvalitText(
                text = metrics?.let { stringResource(Res.string.trend_week_sample, it.matches, it.rounds) }
                    ?: stringResource(Res.string.trend_week_empty),
                style = typography.caption,
                color = colors.t3,
                maxLines = 1,
            )
        }
    }
}

// 막대는 이번 주나 지난주에서 끝난다
@Composable
private fun weekLabel(report: WeeklyReport.Ready, index: Int): String {
    val weeksAgo = report.trend.size - 1 - index + if (report.period.includesThisWeek) 0 else 1
    return when (weeksAgo) {
        0 -> stringResource(CoreUiRes.string.period_this_week)
        1 -> stringResource(CoreUiRes.string.period_last_week)
        else -> stringResource(Res.string.sheet_trend_start, weeksAgo)
    }
}

/** 막대 밑 "7주 전 · 이번 주"입니다. */
@Composable
internal fun TrendAxis(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        OvalitText(
            text = stringResource(Res.string.sheet_trend_start, firstBarWeeksAgo(report)),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
        OvalitText(
            text = stringResource(
                if (report.period.includesThisWeek) CoreUiRes.string.period_this_week else CoreUiRes.string.period_last_week,
            ),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

/**
 * 막대 밑 안내입니다. 액트가 바뀐 곳, 테두리만 그린 막대, 점선이 있을 때만 그 뜻을 적습니다.
 *
 * @param baselineWeeks 점선을 그었으면 그 평균의 주 수입니다.
 */
@Composable
internal fun TrendNotes(report: WeeklyReport.Ready, modifier: Modifier = Modifier, baselineWeeks: Int? = null) {
    val notes = buildList {
        if (report.trend.any { it.startsNewAct }) add(stringResource(Res.string.sheet_trend_act_marker))
        if (report.trend.any { it.sparse }) add(stringResource(Res.string.trend_sparse_note, MIN_TREND_ROUNDS))
        if (baselineWeeks != null) add(stringResource(Res.string.trend_baseline_note, baselineWeeks))
    }
    if (notes.isEmpty()) return
    Column(modifier = modifier) {
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = notes.joinToString(" "),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

// 이번 주에서 끝나면 첫 막대는 7주 전이다
private fun firstBarWeeksAgo(report: WeeklyReport.Ready): Int =
    if (report.period.includesThisWeek) TREND_WEEKS - 1 else TREND_WEEKS
