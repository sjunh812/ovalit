package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val Now = Instant.fromEpochMilliseconds(100L * 24 * 60 * 60 * 1000)

class FirstImportTest {

    @Test
    fun `8주를 넘는 경기는 받지 않는다`() {
        val started = listOf(Now - 1.days, Now - 56.days, Now - 56.days - 1.hours)

        assertEquals(started.take(2), started.forFirstImport(Now) { it })
    }

    @Test
    fun `최근 50경기까지만 최근 것부터 받는다`() {
        val started = List(60) { Now - it.hours }.shuffled()

        val picked = started.forFirstImport(Now) { it }

        assertEquals(50, picked.size)
        assertEquals(List(50) { Now - it.hours }, picked)
    }

    @Test
    fun `받을 경기를 다 받아야 끝난다`() {
        assertFalse(ImportProgress(total = 3, results = listOf(true, false)).isDone)
        assertTrue(ImportProgress(total = 3, results = listOf(true, false, null)).isDone)
        assertTrue(ImportProgress(total = 0, results = emptyList()).isDone)
    }
}
