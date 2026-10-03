package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")

class PingDayTest {

    // 사용자 요청(2026-10-03): 밤 11시에 고른 0시 30분을 "내일"이라고 하면 하루 뒤처럼 읽힌다
    @Test
    fun `자정을 넘긴 오늘 밤은 새벽이다`() {
        val now = at(3, 23, 0)

        assertEquals(PingDay.TODAY, pingDayOf(at(3, 23, 30), now, Seoul))
        assertEquals(PingDay.DAWN, pingDayOf(at(4, 0, 30), now, Seoul))
        assertEquals(PingDay.DAWN, pingDayOf(at(4, 5, 30), now, Seoul))
        assertEquals(PingDay.TOMORROW, pingDayOf(at(4, 6, 0), now, Seoul))
    }

    // 지금이 새벽이면 다음 날 새벽은 하루 뒤다
    @Test
    fun `새벽에 고른 다음 날 새벽은 내일이다`() {
        val now = at(4, 1, 0)

        assertEquals(PingDay.TODAY, pingDayOf(at(4, 3, 0), now, Seoul))
        assertEquals(PingDay.TOMORROW, pingDayOf(at(5, 0, 30), now, Seoul))
    }
}

private fun at(day: Int, hour: Int, minute: Int) = LocalDateTime(2026, 10, day, hour, minute).toInstant(Seoul)
