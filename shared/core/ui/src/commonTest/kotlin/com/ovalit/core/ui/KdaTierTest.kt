package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class KdaTierTest {

    // 사용자 결정: op.gg처럼 1 미만, 1~2, 2~3, 3 이상으로 나눠 칠한다
    @Test
    fun `KDA는 1과 2와 3을 경계로 구간을 나눈다`() {
        assertEquals(KdaTier.BELOW_ONE, kdaTier(0.84))
        assertEquals(KdaTier.ONE, kdaTier(1.0))
        assertEquals(KdaTier.ONE, kdaTier(1.62))
        assertEquals(KdaTier.TWO, kdaTier(2.0))
        assertEquals(KdaTier.TWO, kdaTier(2.41))
        assertEquals(KdaTier.THREE, kdaTier(3.0))
        assertEquals(KdaTier.THREE, kdaTier(5.5))
    }

    // 1.995는 화면에 "2.00"으로 뜬다. 1~2 구간 색이면 틀려 보인다.
    @Test
    fun `구간은 화면에 보이는 두 자리로 가른다`() {
        assertEquals(KdaTier.TWO, kdaTier(1.995))
        assertEquals(KdaTier.ONE, kdaTier(0.996))
        assertEquals(KdaTier.TWO, kdaTier(2.994))
        assertEquals(KdaTier.THREE, kdaTier(2.995))
    }
}
