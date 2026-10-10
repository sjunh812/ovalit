package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountSettingsTest {

    // server/src/routes/me.ts의 REMIND_BEFORE_CHOICES와 같아야 한다. 다르면 PATCH /me가 400을 준다.
    @Test
    fun `시작 전 알림은 서버의 remindBefore 값과 서로 옮긴다`() {
        val expected = mapOf(
            10 to PingReminder.TEN_MINUTES,
            30 to PingReminder.THIRTY_MINUTES,
            60 to PingReminder.ONE_HOUR,
            0 to PingReminder.OFF,
        )

        expected.forEach { (minutes, reminder) ->
            assertEquals(minutes, reminder.minutes)
            assertEquals(reminder, PingReminder.fromMinutes(minutes))
        }
        assertEquals(PingReminder.entries.toSet(), expected.values.toSet())
    }

    @Test
    fun `서버가 모르는 시간을 주면 옮기지 않는다`() {
        assertNull(PingReminder.fromMinutes(15))
    }

    // server/migrations: stats_public 기본 1, remind_before 기본 10
    @Test
    fun `처음 값은 서버가 새 계정에 넣는 값과 같다`() {
        assertEquals(AccountSettings(statsPublic = true, pingReminder = PingReminder.TEN_MINUTES), AccountSettings.Default)
    }
}
