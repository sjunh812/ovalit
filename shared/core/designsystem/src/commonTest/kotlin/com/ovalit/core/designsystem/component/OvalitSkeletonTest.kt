package com.ovalit.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OvalitSkeletonTest {

    @Test
    fun `깜빡임은 흐린 데서 밝아졌다가 같은 시간 동안 다시 흐려진다`() {
        assertEquals(0.45f, skeletonPulse(0), 0.001f)
        assertEquals(1f, skeletonPulse(900), 0.001f)
        assertEquals(0.45f, skeletonPulse(1_800), 0.001f)
        assertEquals(skeletonPulse(300), skeletonPulse(1_500), 0.001f)
    }

    // 틀마다 따로 시작하면 새 틀이 가장 흐린 데서 시작해 바뀌는 순간 툭 튄다
    @Test
    fun `밝기는 프레임 시각으로만 정해져 나중에 나타난 틀도 같은 밝기다`() {
        val now = 123_456_789L
        assertEquals(skeletonPulse(now), skeletonPulse(now + 1_800 * 7), 0.001f)
    }

    @Test
    fun `한 프레임 사이에 밝기가 크게 뛰지 않는다`() {
        val steps = (0L..3_600L step 8).map(::skeletonPulse)
        val biggestJump = steps.zipWithNext { a, b -> kotlin.math.abs(a - b) }.max()
        assertTrue(biggestJump < 0.02f, "한 프레임에 $biggestJump 바뀐다")
    }
}
