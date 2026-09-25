package com.ovalit.feature.report.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * 지표 숫자와 변화량입니다. 한 줄에 들어가면 글자 기준선을 맞춰 나란히 두고, 안 들어가면 변화량을 숫자 아래로
 * 내립니다. 좁은 칸에서 둘을 한 줄에 억지로 넣으면 숫자가 잘리거나 너무 작아집니다.
 */
@Composable
internal fun ValueWithChange(
    value: @Composable () -> Unit,
    change: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp,
) {
    Layout(contents = listOf(value, change ?: {}), modifier = modifier) { (valueMeasurables, changeMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val changePlaceable = changeMeasurables.firstOrNull()?.measure(loose)
        val valueMeasurable = valueMeasurables.first()
        val gapPx = gap.roundToPx()
        val besideWidth = changePlaceable?.let { it.width + gapPx } ?: 0
        // 숫자를 줄이지 않은 폭으로 재서 변화량과 같이 들어가는지 본다
        val fitsBeside = changePlaceable == null ||
            valueMeasurable.maxIntrinsicWidth(constraints.maxHeight) + besideWidth <= constraints.maxWidth

        if (fitsBeside) {
            val valuePlaceable = valueMeasurable.measure(loose.copy(maxWidth = (constraints.maxWidth - besideWidth).coerceAtLeast(0)))
            val valueBaseline = valuePlaceable.baselineOr(valuePlaceable.height)
            val changeBaseline = changePlaceable?.baselineOr(changePlaceable.height) ?: 0
            val baseline = max(valueBaseline, changeBaseline)
            val height = max(
                valuePlaceable.height + baseline - valueBaseline,
                (changePlaceable?.height ?: 0) + baseline - changeBaseline,
            )
            layout(valuePlaceable.width + besideWidth, height) {
                valuePlaceable.place(0, baseline - valueBaseline)
                changePlaceable?.place(valuePlaceable.width + gapPx, baseline - changeBaseline)
            }
        } else {
            val valuePlaceable = valueMeasurable.measure(loose)
            val changeHeight = changePlaceable?.height ?: 0
            layout(max(valuePlaceable.width, changePlaceable?.width ?: 0), valuePlaceable.height + changeHeight) {
                valuePlaceable.place(0, 0)
                changePlaceable?.place(0, valuePlaceable.height)
            }
        }
    }
}

private fun Placeable.baselineOr(fallback: Int): Int = get(FirstBaseline).takeIf { it != AlignmentLine.Unspecified } ?: fallback
