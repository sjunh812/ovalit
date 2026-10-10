package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
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
        val clock = MovingClock(Start)
        val (pings, friends) = CountingPings() to CountingFriends()
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

    // RSO 세션 없이 서버를 부르지 않는다
    @Test
    fun `연동하지 않았으면 받지 않는다`() = runTest {
        val (pings, friends) = CountingPings() to CountingFriends()
        val watcher = SocialWatcher(pings, friends, NotLinked, backgroundScope, MovingClock(Start))

        watcher.onAppVisible()
        settle()

        assertEquals(0, pings.refreshed + friends.refreshed)
    }

    private fun TestScope.settle() = runCurrent()
}

private class MovingClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

private object Linked : AccountRepository {
    override val account: Flow<Account?> = flowOf(Account(id = Me, riotId = MY_RIOT_ID, linkedOn = LocalDate(2026, 10, 1)))

    override suspend fun unlink() = Unit
}

private object NotLinked : AccountRepository {
    override val account: Flow<Account?> = flowOf(null)

    override suspend fun unlink() = Unit
}

private class CountingPings(delegate: PingRepository = FakePingRepository(FakeFriendRepository())) : PingRepository by delegate {
    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
    }
}

private class CountingFriends(delegate: FriendRepository = FakeFriendRepository()) : FriendRepository by delegate {
    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
    }
}
