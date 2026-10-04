package com.ovalit.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShrinkToFitTest {

    // StepBased를 그대로 쓰면 0.25sp 눈금으로 내려서, 칸에 맞춰 둔 23.7sp가 23.5sp로 그려진다
    @Test
    fun `다 들어가는 글자는 맞춰 둔 크기 그대로 그린다`() = runComposeUiTest {
        var layout: TextLayoutResult? = null
        setContent { Fitted("1.34", width = 300, onLayout = { layout = it }) }

        waitForIdle()
        assertEquals(23.7.sp, layout!!.layoutInput.style.fontSize)
    }

    @Test
    fun `넘치는 글자는 칸에 들어가게 줄인다`() = runComposeUiTest {
        var layout: TextLayoutResult? = null
        setContent { Fitted("전투점수 전투점수", width = 60, onLayout = { layout = it }) }

        waitForIdle()
        assertTrue(layout!!.layoutInput.style.fontSize.value < 23.7f)
        assertFalse(layout!!.didOverflowWidth)
    }
}

@Composable
private fun Fitted(text: String, width: Int, onLayout: (TextLayoutResult) -> Unit = {}) {
    Box(Modifier.width(width.dp)) {
        BasicText(
            text = text,
            style = TextStyle(fontSize = 23.7.sp),
            maxLines = 1,
            onTextLayout = onLayout,
            autoSize = shrinkToFit(23.7.sp),
        )
    }
}
