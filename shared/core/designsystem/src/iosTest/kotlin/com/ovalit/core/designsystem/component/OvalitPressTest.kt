package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class OvalitPressTest {

    // 사용자 요청(2026-10-03): 홈 K/D 칸처럼 글자에 딱 붙은 칸은 누른 면이 글자 끝에서 잘린 것처럼 보였다
    @Test
    fun `넓힌 만큼 칸 밖에도 누른 면을 깐다`() = runComposeUiTest {
        setContent { PressedCell(outset = 10.dp) }

        onNodeWithTag(CELL).performTouchInput { down(center) }
        mainClock.advanceTimeBy(300)

        val pixels = onRoot().captureToImage().toPixelMap()
        val px = { at: Dp -> with(density) { at.toPx() }.roundToInt() }
        // 칸은 30~70dp이고 면은 20~80dp다
        assertNotEquals(Color.White, pixels[px(25.dp), px(50.dp)])
        assertEquals(Color.White, pixels[px(15.dp), px(50.dp)])
    }

    @Test
    fun `넓히지 않으면 칸 안에만 깐다`() = runComposeUiTest {
        setContent { PressedCell(outset = 0.dp) }

        onNodeWithTag(CELL).performTouchInput { down(center) }
        mainClock.advanceTimeBy(300)

        val pixels = onRoot().captureToImage().toPixelMap()
        val px = { at: Dp -> with(density) { at.toPx() }.roundToInt() }
        assertEquals(Color.White, pixels[px(25.dp), px(50.dp)])
        assertNotEquals(Color.White, pixels[px(40.dp), px(50.dp)])
    }
}

private const val CELL = "cell"

@Composable
private fun PressedCell(outset: Dp) {
    Box(Modifier.size(100.dp).background(Color.White), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(40.dp)
                .testTag(CELL)
                .clickable(
                    interactionSource = null,
                    indication = OvalitPressIndication(Color.Black, RectangleShape, outset, outset),
                    onClick = {},
                ),
        )
    }
}
