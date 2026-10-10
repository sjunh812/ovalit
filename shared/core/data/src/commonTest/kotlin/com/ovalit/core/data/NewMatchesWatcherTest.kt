package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(TimeZone.of("Asia/Seoul"))

@OptIn(ExperimentalCoroutinesApi::class)
class NewMatchesWatcherTest {

    // 다른 앱을 잠깐 오갈 때마다 경기 ID 목록을 받으면 앱 전체의 Riot 몫을 쓴다
    @Test
    fun `앱이 다시 보이면 마지막으로 확인한 지 10분이 지났을 때만 받는다`() = runTest {
        val clock = StepClock(Thursday)
        val (matches, watcher) = watching(clock)
        matches.importRecent()
        settle()
        val imported = matches.observeMatches().first().size

        clock.now += 5.minutes
        watcher.onAppVisible()
        settle()
        assertEquals(imported, matches.observeMatches().first().size)

        // 가짜 서버는 30초마다 한 판이 끝난 것으로 친다. 11분이면 스물두 판이다.
        clock.now += 6.minutes
        watcher.onAppVisible()
        settle()
        assertEquals(imported + 22, matches.observeMatches().first().size)
    }

    // RSO 세션 없이 전적을 요청하지 않는다(CLAUDE.md 지켜야 할 선)
    @Test
    fun `연동하지 않았으면 앱이 보여도 받지 않는다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock)
        matches.importRecent()
        val before = matches.observeMatches().first().size
        val watcher = NewMatchesWatcher(matches, NoAccount, RecordingScheduler(), backgroundScope, ThursdayClock)

        watcher.onAppVisible()
        settle()

        assertEquals(before, matches.observeMatches().first().size)
    }

    // 첫 수집 중에는 S0-4가 받는다. 그때 새 경기까지 받으면 홈 숫자가 수집 중에 바뀐다.
    @Test
    fun `첫 수집이 끝나기 전에는 앱이 보여도 받지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val (matches, watcher) = watching(clock)

        watcher.onAppVisible()
        settle()

        assertNull(matches.newMatchesProgress.first())
        assertEquals(emptyList(), matches.observeMatches().first())
    }

    @Test
    fun `스무 판 넘게 남은 채 앱이 가려지면 이어 받기를 맡긴다`() = runTest {
        val clock = StepClock(Thursday)
        val scheduler = RecordingScheduler()
        val (matches, watcher) = watching(clock, scheduler)
        matches.importRecent()
        settle()
        clock.now += 11.minutes

        watcher.onAppVisible()
        advanceTimeBy(1.seconds)
        watcher.onAppHidden()
        runCurrent()

        assertTrue(scheduler.continued)
        settle()
    }

    // 몇 판 남지 않았으면 앱이 살아 있는 동안 금방 끝난다. 끝났다는 알림이 오히려 귀찮다.
    @Test
    fun `몇 판만 남았으면 앱이 가려져도 맡기지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val scheduler = RecordingScheduler()
        val (matches, watcher) = watching(clock, scheduler)
        matches.importRecent()
        settle()
        clock.now += 3.minutes
        launch { matches.refresh() }

        advanceTimeBy(1.seconds)
        watcher.onAppHidden()
        runCurrent()

        assertFalse(scheduler.continued)
        settle()
    }

    // 당긴 직후 앱을 나갔다 오면 방금 받은 목록을 또 받는다
    @Test
    fun `당겨서 받은 것도 확인으로 쳐서 곧 다시 열면 받지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val (matches, watcher) = watching(clock)
        matches.importRecent()
        settle()
        clock.now += 11.minutes
        matches.refresh()
        val pulled = matches.observeMatches().first().size

        clock.now += 2.minutes
        watcher.onAppVisible()
        settle()

        assertEquals(pulled, matches.observeMatches().first().size)
    }

    // watcher는 backgroundScope에서 돈다. advanceUntilIdle은 거기 띄운 일을 기다리지 않아서 시간을 직접 넘긴다.
    private fun TestScope.settle() {
        advanceTimeBy(1.minutes)
        runCurrent()
    }

    // 계정은 처음부터 연동돼 있고, 경기는 첫 수집이 채운다
    private suspend fun TestScope.watching(
        clock: Clock,
        scheduler: ImportScheduler = RecordingScheduler(),
    ): Pair<FakeMatchRepository, NewMatchesWatcher> {
        val matches = FakeMatchRepository(clock, scope = this)
        val account = FakeAccountRepository(matches, clock)
        account.link()
        return matches to NewMatchesWatcher(matches, account, scheduler, backgroundScope, clock)
    }
}

private val ThursdayClock = object : Clock {
    override fun now(): Instant = Thursday
}


private object NoAccount : AccountRepository {
    override val account: Flow<Account?> = flowOf(null)

    override suspend fun unlink() = Unit
}

private class RecordingScheduler : ImportScheduler {
    var continued = false
        private set

    override fun start() = Unit

    override fun retry() = Unit

    override fun continueNewMatches(total: Int) {
        continued = true
    }

    override fun cancel() = Unit
}
