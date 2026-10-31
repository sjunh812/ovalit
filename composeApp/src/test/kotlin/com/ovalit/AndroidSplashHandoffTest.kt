package com.ovalit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

class AndroidSplashHandoffTest {

    // 스플래시가 넘어오자마자 걷으면 로고 애니메이션이 같은 장면을 그리기 전이라 빈 말풍선이 비친다
    @Test
    fun `넘어온 스플래시는 넘겨받을 때까지 두었다가 바탕을 숨긴 다음 프레임에 걷는다`() = runTest {
        var removed = 0
        val handoff = AndroidSplashHandoff(onRemoved = { removed++ }, now = { 1_500L })
        val exit = FakeExit(iconAnimationStartMillis = 1_000L)

        handoff.handleExit(exit)
        assertEquals(500L, handoff.awaitIconElapsed())
        assertEquals(emptyList(), exit.events)

        handoff.takeOver()
        assertEquals(listOf("hide", "post"), exit.events)
        assertEquals(0, removed)
        exit.nextFrame()
        assertEquals(listOf("hide", "post", "remove"), exit.events)
        assertEquals(1, removed)
    }

    // 느린 기기에서는 1초를 기다려도 스플래시가 넘어오지 않아 앱이 먼저 넘겨받는다.
    // 늦게 온 스플래시를 바로 걷으면 빨간 바탕만 한 프레임 남는다.
    @Test
    fun `앱이 먼저 넘겨받은 뒤에 온 스플래시도 바탕을 숨긴 다음 프레임에 걷는다`() = runTest {
        var removed = 0
        val handoff = AndroidSplashHandoff(onRemoved = { removed++ }, now = { 2_000L })

        assertNull(handoff.awaitIconElapsed())
        handoff.takeOver()
        val exit = FakeExit(iconAnimationStartMillis = 1_000L)
        handoff.handleExit(exit)

        assertEquals(listOf("hide", "post"), exit.events)
        exit.nextFrame()
        assertEquals(1, removed)
    }

    // 스플래시 없이 열린 경우(다른 앱에서 열기 등)에 로고 애니메이션이 오래 멈춰 있지 않는다
    @Test
    fun `스플래시가 1초 안에 넘어오지 않으면 더 기다리지 않는다`() = runTest {
        val handoff = AndroidSplashHandoff(onRemoved = {})
        val start = currentTime

        assertNull(handoff.awaitIconElapsed())
        assertEquals(1_000L, currentTime - start)
    }

    // 안드로이드 12 미만은 스플래시가 움직이지 않아 시작 시각이 0이다
    @Test
    fun `움직이지 않는 스플래시면 흐른 시간을 알리지 않아 로고가 처음부터 재생된다`() = runTest {
        val handoff = AndroidSplashHandoff(onRemoved = {}, now = { 5_000L })

        handoff.handleExit(FakeExit(iconAnimationStartMillis = 0L))

        assertNull(handoff.awaitIconElapsed())
    }

    private class FakeExit(override val iconAnimationStartMillis: Long) : SplashExit {
        val events = mutableListOf<String>()
        private var nextFrame: (() -> Unit)? = null

        override fun hide() {
            events += "hide"
        }

        override fun removeOnNextFrame(onRemoved: () -> Unit) {
            events += "post"
            nextFrame = {
                events += "remove"
                onRemoved()
            }
        }

        fun nextFrame() {
            nextFrame?.invoke()
            nextFrame = null
        }
    }
}
