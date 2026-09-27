package com.ovalit.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText

/**
 * 항목을 한 줄에 늘어놓다가 안 들어가면 다음 줄로 넘깁니다. [separator]는 같은 줄에 있는 항목 사이에만
 * 둡니다. FlowRow에 점을 항목처럼 넣으면 줄이 바뀔 때 "· 이번 액트 66경기"처럼 점이 줄 맨 앞에 옵니다.
 *
 * 항목 하나는 레이아웃 하나만 그려야 합니다. 글자 둘을 나란히 두려면 `Row`로 감쌉니다. 둘을 그대로 내보내면 한
 * 항목이 둘로 세어져 구분점이 어긋납니다. 구분점은 낭독기가 읽지 않습니다.
 *
 * @param stacked 들어가든 말든 항목마다 줄을 바꿉니다. 나란한 칸 중 하나라도 넘치면 모든 칸을 같은 모양으로
 *   꺾을 때 씁니다([rememberFitsOnOneLine]). 한 칸만 꺾이면 그 칸만 높아집니다.
 * @param alignBaseline 한 줄 안에서 글자 기준선을 맞춥니다. 크기가 다른 글자를 나란히 둘 때 씁니다. 끄면 가운데를 맞춥니다.
 */
@Composable
fun SeparatedRow(
    items: List<@Composable () -> Unit>,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    stacked: Boolean = false,
    alignBaseline: Boolean = false,
) {
    Layout(
        contents = listOf(
            { items.forEach { it() } },
            { repeat((items.size - 1).coerceAtLeast(0)) { Box(Modifier.clearAndSetSemantics {}) { separator() } } },
        ),
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
            if (index > 0 && !stacked && x + needed <= constraints.maxWidth) {
                lines.last() += separator!! to x
                lines.last() += item to x + separator.width
                x += needed
            } else {
                if (index > 0) lines += mutableListOf<Pair<Placeable, Int>>()
                lines.last() += item to 0
                x = item.width
            }
        }

        val gap = LineSpacing.roundToPx()
        fun Placeable.baseline() = get(FirstBaseline).takeIf { it != AlignmentLine.Unspecified } ?: height
        // 기준선을 맞추면 줄의 위쪽은 가장 높은 기준선, 아래쪽은 기준선 밑으로 가장 긴 글자가 정한다
        val baselines = lines.map { line -> if (alignBaseline) line.maxOfOrNull { it.first.baseline() } ?: 0 else 0 }
        val heights = lines.mapIndexed { index, line ->
            if (alignBaseline) {
                baselines[index] + (line.maxOfOrNull { (p, _) -> p.height - p.baseline() } ?: 0)
            } else {
                line.maxOfOrNull { it.first.height } ?: 0
            }
        }
        val content = lines.maxOf { line -> line.maxOfOrNull { (p, left) -> left + p.width } ?: 0 }
        val width = content.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = heights.sum() + gap * (lines.size - 1).coerceAtLeast(0)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var top = 0
            lines.forEachIndexed { index, line ->
                val lineWidth = line.maxOfOrNull { (p, left) -> left + p.width } ?: 0
                // 부르는 쪽이 폭을 채우라고 했으면 그 폭의 오른쪽 끝에 맞춘다
                val shift = if (alignEnd) width - lineWidth else 0
                line.forEach { (placeable, left) ->
                    val y = if (alignBaseline) baselines[index] - placeable.baseline() else (heights[index] - placeable.height) / 2
                    placeable.place(shift + left, top + y)
                }
                top += heights[index] + gap
            }
        }
    }
}

private val LineSpacing = 2.dp

/** 항목 사이에 두는 가운뎃점입니다. 글자를 이어 붙일 때도 이 글자를 씁니다. */
const val SEPARATOR = " · "

/** [SeparatedRow]의 구분점입니다. 크기와 색은 옆 항목에 맞춰 부르는 쪽이 정합니다. */
@Composable
fun SeparatorDot(style: TextStyle, color: Color) {
    OvalitText(text = SEPARATOR, style = style, color = color)
}
