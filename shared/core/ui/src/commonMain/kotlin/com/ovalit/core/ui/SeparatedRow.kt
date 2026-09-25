package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 항목을 한 줄에 늘어놓다가 안 들어가면 다음 줄로 넘깁니다. [separator]는 같은 줄에 있는 항목 사이에만
 * 둡니다. FlowRow에 점을 항목처럼 넣으면 줄이 바뀔 때 "· 이번 액트 66경기"처럼 점이 줄 맨 앞에 옵니다.
 */
@Composable
fun SeparatedRow(
    items: List<@Composable () -> Unit>,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 2.dp,
    alignEnd: Boolean = false,
) {
    Layout(
        contents = listOf({ items.forEach { it() } }, { repeat((items.size - 1).coerceAtLeast(0)) { separator() } }),
        modifier = modifier,
    ) { (itemMeasurables, separatorMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = itemMeasurables.map { it.measure(loose) }
        val separators = separatorMeasurables.map { it.measure(loose) }

        val lines = mutableListOf(mutableListOf<Pair<Placeable, Int>>())
        var x = 0
        placeables.forEachIndexed { index, item ->
            val separator = separators.getOrNull(index - 1)
            val needed = (separator?.width ?: 0) + item.width
            if (index > 0 && x + needed <= constraints.maxWidth) {
                lines.last() += separator!! to x
                lines.last() += item to x + separator.width
                x += needed
            } else {
                if (index > 0) lines += mutableListOf<Pair<Placeable, Int>>()
                lines.last() += item to 0
                x = item.width
            }
        }

        val gap = lineSpacing.roundToPx()
        val heights = lines.map { line -> line.maxOfOrNull { it.first.height } ?: 0 }
        val width = lines.maxOf { line -> line.maxOfOrNull { (p, left) -> left + p.width } ?: 0 }
        val height = heights.sum() + gap * (lines.size - 1).coerceAtLeast(0)
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var top = 0
            lines.forEachIndexed { index, line ->
                val lineWidth = line.maxOfOrNull { (p, left) -> left + p.width } ?: 0
                val shift = if (alignEnd) width - lineWidth else 0
                line.forEach { (placeable, left) -> placeable.place(shift + left, top + (heights[index] - placeable.height) / 2) }
                top += heights[index] + gap
            }
        }
    }
}
