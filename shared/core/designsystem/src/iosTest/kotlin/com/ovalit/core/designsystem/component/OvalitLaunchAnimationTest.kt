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

    // 켤 때마다 보는 화면이라 1.25초 안에 끝나야 한다.
    // 스플래시가 움직이지 않으면 점이 떠올라 한 번 출렁인 뒤 벌어진다.
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

    // 스플래시가 이미 출렁이고 있었으면 처음부터 다시 하지 않고 그 출렁임이 끝나는 자리에서 벌어진다
    @Test
    fun `움직이던 스플래시를 넘겨받으면 같은 장면을 그린 뒤 스플래시를 걷고 더 빨리 끝난다`() = runComposeUiTest {
        var finished = false
        val splash = FakeSplash(iconElapsed = 800)
        mainClock.autoAdvance = false
        setContent { OvalitTheme { OvalitLaunchAnimation(onFinished = { finished = true }, splash = splash) } }

        mainClock.advanceTimeBy(48)
        assertEquals(0, splash.takenOver)
        mainClock.advanceTimeBy(100)
        assertEquals(1, splash.takenOver)
        mainClock.advanceTimeBy(800)
        assertTrue(finished)
    }

    @Test
    fun `넘겨받은 때가 출렁이는 중이면 그 출렁임이 끝난 뒤에 벌어지고 쉬는 중이면 바로 벌어진다`() {
        // 첫 출렁임은 350ms에 시작해 760ms에 끝나고 900ms마다 다시 출렁인다
        assertEquals(760f, spreadStart(150f))
        assertEquals(760f, spreadStart(500f))
        assertEquals(1000f, spreadStart(1000f))
        assertEquals(1660f, spreadStart(1300f))
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

    private class FakeSplash(private val iconElapsed: Long?) : SplashHandoff {
        var takenOver = 0
        override suspend fun awaitIconElapsed(): Long? = iconElapsed
        override fun takeOver() {
            takenOver++
        }
    }
}
