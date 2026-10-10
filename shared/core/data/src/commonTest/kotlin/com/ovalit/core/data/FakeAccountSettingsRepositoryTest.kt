package com.ovalit.core.data

import com.ovalit.core.model.AccountSettings
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.PingReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAccountSettingsRepositoryTest {

    private val repository = FakeAccountSettingsRepository(latency = 1.seconds)

    // docs/screens.md: 전적 공개 토글은 기본으로 켜져 있다
    @Test
    fun `처음에는 전적을 공개하고 시작 10분 전에 알린다`() = runTest {
        assertEquals(AccountSettings(statsPublic = true, pingReminder = PingReminder.TEN_MINUTES), repository.settings.first())
    }

    @Test
    fun `바꾼 값은 서버 답을 기다리지 않고 바로 내보낸다`() = runTest {
        launch { repository.setStatsPublic(false) }
        runCurrent()

        assertEquals(false, repository.settings.first().statsPublic)
        advanceUntilIdle()
        assertEquals(false, repository.settings.first().statsPublic)
    }

    @Test
    fun `서버에 보내지 못하면 되돌리고 까닭을 던진다`() = runTest {
        repository.failure = OvalitError.Offline

        val error = assertFailsWith<OvalitException> { repository.setPingReminder(PingReminder.OFF) }

        assertEquals(OvalitError.Offline, error.error)
        assertEquals(AccountSettings.Default, repository.settings.first())
    }

    @Test
    fun `실패한 변경만 되돌리고 뒤에 바꾼 다른 설정은 남긴다`() = runTest {
        repository.failure = OvalitError.Offline
        val statsPublic = async { runCatching { repository.setStatsPublic(false) } }
        launch { repository.setPingReminder(PingReminder.ONE_HOUR) }
        runCurrent()
        assertEquals(AccountSettings(statsPublic = false, pingReminder = PingReminder.ONE_HOUR), repository.settings.first())

        // 하나씩 보내니 앞의 전적 공개만 실패하고 시작 전 알림은 그 뒤에 보낸다
        advanceTimeBy(1.seconds)
        runCurrent()
        repository.failure = null
        advanceUntilIdle()

        assertTrue(statsPublic.await().isFailure)
        assertEquals(AccountSettings(statsPublic = true, pingReminder = PingReminder.ONE_HOUR), repository.settings.first())
    }

    // 앞선 요청의 답을 그대로 띄우면 스위치가 껐다 켰다 한 번 더 튄다
    @Test
    fun `연달아 바꾸면 앞선 답이 와도 마지막에 고른 값을 그대로 보여 준다`() = runTest {
        val seen = collectStatsPublic()

        launch { repository.setStatsPublic(false) }
        launch { repository.setStatsPublic(true) }
        advanceUntilIdle()

        assertEquals(listOf(true, false, true), seen)
    }

    @Test
    fun `다시 받는 동안 바꾼 값을 서버의 옛 값으로 덮지 않는다`() = runTest {
        val seen = collectStatsPublic()

        launch { repository.refresh() }
        launch { repository.setStatsPublic(false) }
        advanceUntilIdle()

        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun `보내다 취소되면 바꾼 값을 되돌린다`() = runTest {
        val sending = launch { repository.setStatsPublic(false) }
        runCurrent()

        sending.cancel()
        runCurrent()

        assertEquals(AccountSettings.Default, repository.settings.first())
    }

    @Test
    fun `보내는 중에 처음 값으로 돌리면 늦게 온 답도 버린다`() = runTest {
        val sending = async { runCatching { repository.setStatsPublic(false) } }
        runCurrent()

        repository.reset()
        advanceUntilIdle()
        repository.refresh()

        assertEquals(OvalitError.SessionExpired, (sending.await().exceptionOrNull() as OvalitException).error)
        assertEquals(AccountSettings.Default, repository.settings.first())
    }

    private fun TestScope.collectStatsPublic(): List<Boolean> {
        val seen = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.settings.collect { seen += it.statsPublic } }
        return seen
    }
}
