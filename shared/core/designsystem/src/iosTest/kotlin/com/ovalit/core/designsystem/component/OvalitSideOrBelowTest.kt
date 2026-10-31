package com.ovalit.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OvalitSideOrBelowTest {

    @Test
    fun `오른쪽 값이 줄의 절반 안이면 옆에 둔다`() {
        val (start, end) = place(rowWidth = 300.dp, startWidth = 250.dp, endWidth = 100.dp)

        assertEquals(start.top, end.top)
        assertEquals(300.dp, end.right)
    }

    // 큰 글꼴에서 오른쪽 값이 자리를 다 차지하면 왼쪽 글자가 한 자씩 꺾였다
    @Test
    fun `오른쪽 값이 줄의 절반을 넘으면 왼쪽 밑으로 내린다`() {
        val (start, end) = place(rowWidth = 300.dp, startWidth = 250.dp, endWidth = 200.dp)

        assertTrue(end.top >= start.bottom, "왼쪽 $start, 오른쪽 $end")
        assertEquals(0.dp, end.left)
    }

    @Test
    fun `둘 다 한 줄에 들어가면 오른쪽 값이 절반을 넘어도 옆에 둔다`() {
        val (start, end) = place(rowWidth = 300.dp, startWidth = 60.dp, endWidth = 200.dp)

        assertEquals(start.top, end.top)
        assertEquals(300.dp, end.right)
    }

    private fun place(rowWidth: Dp, startWidth: Dp, endWidth: Dp): Pair<DpRect, DpRect> {
        var bounds: Pair<DpRect, DpRect>? = null
        runComposeUiTest {
            setContent {
                OvalitSideOrBelow(
                    // 글자처럼 제 폭이 있고 남은 자리가 좁으면 거기에 맞춰 줄어든다
                    start = { Box(Modifier.testTag("start").size(width = startWidth, height = 20.dp)) },
                    end = { Box(Modifier.testTag("end").size(width = endWidth, height = 20.dp)) },
                    modifier = Modifier.width(rowWidth),
                    gap = 0.dp,
                    belowGap = 0.dp,
                )
            }
            bounds = onNodeWithTag("start").getUnclippedBoundsInRoot() to onNodeWithTag("end").getUnclippedBoundsInRoot()
        }
        return bounds!!
    }
}
