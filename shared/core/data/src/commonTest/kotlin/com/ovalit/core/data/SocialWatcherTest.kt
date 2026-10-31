package com.ovalit.core.data

import com.ovalit.core.model.Account
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.testing.NoAccount
import com.ovalit.core.testing.StepClock
import com.ovalit.core.testing.TestFriendRepository
import com.ovalit.core.testing.TestPingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

private val Start = Instant.parse("2026-10-10T12:00:00Z")

class SocialWatcherTest {

    // 푸시가 늦거나 오지 않으면 다시 열어도 옛 초대와 친구 목록이 남는다
    @Test
    fun `앱이 다시 보이면 ㅇㅂㅇ과 친구를 받고 1분 안에는 다시 받지 않는다`() = runTest {
        val clock = StepClock(Start)
        val (pings, friends) = TestPingRepository() to TestFriendRepository()
        val watcher = SocialWatcher(pings, friends, Linked, backgroundScope, clock)

        watcher.onAppVisible()
        settle()
        clock.now += 30.seconds
        watcher.onAppVisible()
        settle()
        assertEquals(1, pings.refreshed)
        assertEquals(1, friends.refreshed)

        clock.now += 1.minutes
        watcher.onAppVisible()
        settle()
        assertEquals(2, pings.refreshed)
        assertEquals(2, friends.refreshed)
    }

    // 같이 받다 한쪽이 실패하면 다른 쪽까지 끊기거나 받은 쪽까지 곧 다시 받는다
    @Test
    fun `하나를 받지 못해도 다른 하나는 받고 못 받은 쪽만 곧 다시 받는다`() = runTest {
        val clock = StepClock(Start)
        val (pings, friends) = TestPingRepository() to TestFriendRepository()
        val watcher = SocialWatcher(pings, friends, Linked, backgroundScope, clock)
        pings.refreshFailure = OvalitException(OvalitError.Offline)

        watcher.onAppVisible()
        settle()
        pings.refreshFailure = null
        clock.now += 10.seconds
        watcher.onAppVisible()
        settle()
        assertEquals(2, pings.refreshed)
        assertEquals(1, friends.refreshed)

        friends.refreshFailure = OvalitException(OvalitError.Offline)
        clock.now += 1.minutes
        watcher.onAppVisible()
        settle()
        friends.refreshFailure = null
        clock.now += 10.seconds
        watcher.onAppVisible()
        settle()
        assertEquals(3, pings.refreshed)
        assertEquals(3, friends.refreshed)
    }

    // 받은 시각이 미래로 남으면 시계가 그 시각을 다시 지날 때까지 받지 않는다
    @Test
    fun `기기 시계를 되돌렸으면 바로 다시 받는다`() = runTest {
        val clock = StepClock(Start)
        val (pings, friends) = TestPingRepository() to TestFriendRepository()
        val watcher = SocialWatcher(pings, friends, Linked, backgroundScope, clock)
        watcher.onAppVisible()
        settle()

        clock.now = Start - 1.hours
        watcher.onAppVisible()
        settle()

        assertEquals(2, pings.refreshed)
        assertEquals(2, friends.refreshed)
    }

    // RSO 세션 없이 서버를 부르지 않는다
    @Test
    fun `연동하지 않았으면 받지 않는다`() = runTest {
        val (pings, friends) = TestPingRepository() to TestFriendRepository()
        val watcher = SocialWatcher(pings, friends, NoAccount, backgroundScope, StepClock(Start))

        watcher.onAppVisible()
        settle()

        assertEquals(0, pings.refreshed + friends.refreshed)
    }

    private fun TestScope.settle() = runCurrent()
}

private object Linked : AccountRepository {
    override val account: Flow<Account?> = flowOf(Account(id = Me, riotId = MY_RIOT_ID, linkedOn = LocalDate(2026, 10, 1)))

    override suspend fun unlink() = Unit
}
