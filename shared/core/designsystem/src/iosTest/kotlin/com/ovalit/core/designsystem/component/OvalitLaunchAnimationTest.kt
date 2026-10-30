package com.ovalit.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OvalitLaunchAnimationTest {

    // 켤 때마다 보는 화면이라 1.25초 안에 끝나야 한다
    @Test
    fun `로고 애니메이션은 1초 남짓 재생하고 끝을 알린다`() = runComposeUiTest {
        var finished = false
        mainClock.autoAdvance = false
        setContent { OvalitTheme { OvalitLaunchAnimation(onFinished = { finished = true }) } }

        mainClock.advanceTimeBy(900)
        assertFalse(finished)
        mainClock.advanceTimeBy(500)
        assertTrue(finished)
    }

    // 재생하는 동안 밑 화면이 눌리면 인트로의 시작 버튼이 보이기도 전에 눌린다
    @Test
    fun `재생하는 동안 밑 화면은 눌리지 않는다`() = runComposeUiTest {
        var clicks = 0
        mainClock.autoAdvance = false
        setContent {
            OvalitTheme {
                Box(Modifier.fillMaxSize()) {
                    BasicText("시작", Modifier.fillMaxSize().clickable { clicks++ })
                    OvalitLaunchAnimation(onFinished = {})
                }
            }
        }

        mainClock.advanceTimeBy(300)
        onNodeWithText("시작").performClick()
        assertEquals(0, clicks)
    }
}
