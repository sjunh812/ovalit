package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ListAdTest {

    // 한 날짜 안에 끼우면 그날 경기처럼 읽혀서 묶음 앞에만 둔다
    @Test
    fun `첫 광고는 세 줄을 지난 첫 묶음 앞에 둔다`() {
        assertEquals(setOf(2), listAdBefore(listOf(1, 2, 4)))
        assertEquals(setOf(1), listAdBefore(listOf(5, 1)))
    }

    @Test
    fun `그 뒤로는 여덟 줄이 넘을 때마다 하나다`() {
        assertEquals(setOf(1, 3), listAdBefore(listOf(3, 4, 4, 1)))
    }

    // 마지막 묶음 뒤에는 두지 않는다. 광고로 끝나는 목록은 경기가 더 있는 것처럼 보인다.
    @Test
    fun `줄이 모자라거나 묶음이 하나면 두지 않는다`() {
        assertEquals(emptySet(), listAdBefore(listOf(1, 1)))
        assertEquals(emptySet(), listAdBefore(listOf(10)))
        assertEquals(emptySet(), listAdBefore(emptyList()))
    }
}
