package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JoinKeepingPartsTest {

    // 줄을 바꿀 수 있는 자리는 점 뒤의 띄어쓰기 하나뿐이어야 한다
    @Test
    fun `항목 안과 점 앞은 붙이고 점 뒤에서만 줄을 바꾼다`() {
        val joined = joinKeepingParts(listOf("제트 3승 1패", "소바 2승 1패"))

        assertEquals("제트 3승 1패 · 소바 2승 1패", joined)
        assertEquals(1, joined.count { it == ' ' })
        assertTrue(joined.substringBefore(' ').endsWith("·"))
    }
}
