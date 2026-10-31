package com.ovalit.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import com.ovalit.core.designsystem.theme.OvalitSpacing

/**
 * [start]와 [end]를 한 줄에 두다가 [end]가 줄 폭의 절반을 넘으면 [start] 밑으로 내립니다.
 * 큰 글꼴이나 좁은 폭에서 오른쪽 값이 자리를 다 차지해 왼쪽 글자가 한 자씩 꺾이지 않게 합니다.
 * 둘 다 한 줄에 들어가면 [end]가 절반을 넘어도 그대로 둡니다.
 *
 * 한 줄일 때는 세로 가운데에 맞추고, 내리면 [start]와 같은 왼쪽 선에서 시작합니다.
 *
 * @param gap 한 줄일 때 둘 사이입니다.
 * @param belowGap 내렸을 때 둘 사이입니다.
 */
@Composable
fun OvalitSideOrBelow(
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = OvalitSpacing.md,
    belowGap: Dp = OvalitSpacing.xs,
) {
    Layout(contents = listOf(start, end), modifier = modifier) { (starts, ends), constraints ->
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gapPx = gap.roundToPx()
        val startNeeds = starts.first().maxIntrinsicWidth(constraints.maxHeight)
        val trailing = ends.first().measure(loose)
        val side = !constraints.hasBoundedWidth ||
            trailing.width + gapPx <= width / 2 ||
            startNeeds + gapPx + trailing.width <= width
        if (side) {
            val leading = starts.first().measure(loose.copy(maxWidth = (width - trailing.width - gapPx).coerceAtLeast(0)))
            val height = maxOf(leading.height, trailing.height)
            val rowWidth = if (constraints.hasBoundedWidth) width else leading.width + gapPx + trailing.width
            layout(rowWidth, height) {
                leading.place(0, (height - leading.height) / 2)
                trailing.place(rowWidth - trailing.width, (height - trailing.height) / 2)
            }
        } else {
            val leading = starts.first().measure(loose)
            val below = belowGap.roundToPx()
            layout(width, leading.height + below + trailing.height) {
                leading.place(0, 0)
                trailing.place(0, leading.height + below)
            }
        }
    }
}
