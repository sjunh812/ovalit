package com.ovalit.core.data

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchListEntry
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(TimeZone.of("Asia/Seoul"))

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstMatchRepositoryTest {

    @Test
    fun `첫 수집은 최근 50경기를 최신부터 받고 다 받으면 확인 시각을 적는다`() = runTest {
        val played = history(60)
        val remote = TestMatchRemote(played)
        val repository = repositoryOf(remote)

        repository.importRecent()

        assertEquals(played.take(50).map { it.id }, remote.fetched)
        assertEquals(played.take(50), repository.observeMatches().first())
        val progress = assertNotNull(repository.importProgress.first())
        assertEquals(50, progress.total)
        assertTrue(progress.isDone)
        assertEquals(Thursday, repository.checkedAt.first())
    }

    // 커스텀 게임은 내 실력 흐름과 상관없고, 받으면 레이트 리밋만 쓴다(CLAUDE.md 큐)
    @Test
    fun `첫 수집은 8주를 넘는 경기와 커스텀 게임을 받지 않는다`() = runTest {
        val played = history(3)
        val remote = TestMatchRemote(played + match("old", Thursday - 57.days))
        remote.playCustomGame("custom", Thursday - 10.minutes)
        remote.playCustomGame("custom-in-detail", Thursday - 20.minutes, listedAs = Queue.UNRATED)
        val repository = repositoryOf(remote)

        repository.importRecent()

        assertEquals(listOf(MatchId("custom-in-detail")) + played.map { it.id }, remote.fetched)
        assertEquals(played, repository.observeMatches().first())
        val progress = assertNotNull(repository.importProgress.first())
        assertEquals(3, progress.total)
        assertTrue(progress.isDone)
    }

    // 적지 않으면 S0-4가 멈춘 까닭과 다시 시도 버튼 없이 "불러오는 중"에 머문다
    @Test
    fun `첫 수집 중에 인터넷이 끊기면 멈춘 까닭을 적고 다시 부르면 남은 것만 받는다`() = runTest {
        val played = history(20)
        val remote = TestMatchRemote(played, listDelay = 100.milliseconds)
        val repository = repositoryOf(remote)
        remote.failAfter(8, OvalitError.Offline)

        val failure = assertFailsWith<OvalitException> { repository.importRecent() }

        assertEquals(OvalitError.Offline, failure.error)
        val stopped = assertNotNull(repository.importProgress.first())
        assertEquals(OvalitError.Offline, stopped.stoppedBy)
        assertEquals(8, stopped.loaded)
        assertFalse(stopped.isDone)
        assertEquals(played.take(8), repository.observeMatches().first())

        // 다시 받기 시작하면 목록을 받기 전에 멈춘 안내부터 거둔다. 받은 칸은 그대로 둔다.
        remote.recover()
        val resuming = launch { repository.importRecent() }
        runCurrent()
        assertEquals(stopped.copy(stoppedBy = null), repository.importProgress.first())
        resuming.join()

        assertEquals(played.map { it.id }, remote.fetched)
        assertTrue(assertNotNull(repository.importProgress.first()).isDone)
        assertEquals(played, repository.observeMatches().first())
    }

    // 몇 판을 받을지 모르는 채로 멈춰도 S0-4가 "리포트 보기"가 아니라 다시 시도를 띄워야 한다
    @Test
    fun `목록부터 받지 못해도 멈춘 까닭을 적고 끝난 것으로 보지 않는다`() = runTest {
        val remote = TestMatchRemote(history(5), listDelay = 100.milliseconds)
        val repository = repositoryOf(remote)
        remote.failListing(OvalitError.RiotDown)

        assertFailsWith<OvalitException> { repository.importRecent() }

        val stopped = assertNotNull(repository.importProgress.first())
        assertEquals(OvalitError.RiotDown, stopped.stoppedBy)
        assertFalse(stopped.isDone)
        assertNull(repository.checkedAt.first())
        assertEquals(0, repository.refresh())

        // 멈춘 까닭만 지우면 0판 중 0판이라 끝난 것으로 보인다. 목록을 받을 때까지 시작 전으로 둔다.
        remote.recover()
        val resuming = launch { repository.importRecent() }
        runCurrent()
        assertNull(repository.importProgress.first())
        resuming.join()

        assertTrue(assertNotNull(repository.importProgress.first()).isDone)
    }

    // 연동하자마자 해제하면 첫 수집이 아직 목록을 받는 중일 수 있다
    @Test
    fun `첫 수집이 목록을 받는 사이에 지워도 경기가 들어오지 않는다`() = runTest {
        val remote = TestMatchRemote(history(5), listDelay = 100.milliseconds)
        val repository = repositoryOf(remote)
        val importing = launch { repository.importRecent() }
        advanceTimeBy(50.milliseconds)

        repository.deleteAll()
        importing.join()

        assertEquals(emptyList(), remote.fetched)
        assertEquals(emptyList(), repository.observeMatches().first())
        assertNull(repository.importProgress.first())
    }

    // 화면은 OvalitError로 안내 문구를 고른다. 까닭을 모르는 실패도 그대로 던지면 안내가 갈린다.
    @Test
    fun `서버가 까닭 없이 실패해도 OvalitError를 실어 던진다`() = runTest {
        val broken = object : MatchRemoteSource {
            override suspend fun matchList(): List<MatchListEntry> = throw IllegalStateException("응답을 읽지 못했다")

            override suspend fun match(id: MatchId): Match? = null
        }
        val repository = OfflineFirstMatchRepository(broken, InMemoryMatchStore(), StepClock(Thursday), this)

        val failure = assertFailsWith<OvalitException> { repository.importRecent() }

        assertEquals(OvalitError.Unknown, failure.error)
        assertEquals(OvalitError.Unknown, repository.importProgress.first()?.stoppedBy)
    }

    // WorkManager 작업과 앱 안의 수집이 겹치면 레이트 리밋을 두 번 쓰고 S0-4 막대가 두 번 찬다
    @Test
    fun `첫 수집을 둘이 함께 불러도 한 번만 받는다`() = runTest {
        val played = history(10)
        val remote = TestMatchRemote(played)
        val repository = repositoryOf(remote)

        listOf(async { repository.importRecent() }, async { repository.importRecent() }).awaitAll()

        assertEquals(1, remote.listings)
        assertEquals(played.map { it.id }, remote.fetched)
        assertEquals(10, assertNotNull(repository.importProgress.first()).results.size)
    }

    // 첫 수집은 WorkManager 쪽에서 돌아 지우기가 멈출 수 없다. 지운 뒤에 받던 경기가 들어오면 연동을 해제해도 전적이 남는다.
    @Test
    fun `첫 수집 중에 지우면 지운 뒤에 경기가 들어오지 않는다`() = runTest {
        val repository = repositoryOf(TestMatchRemote(history(10)))
        val importing = launch { repository.importRecent() }
        advanceTimeBy(350.milliseconds)
        assertTrue(repository.observeMatches().first().isNotEmpty())

        repository.deleteAll()
        importing.join()

        assertEquals(emptyList(), repository.observeMatches().first())
        assertNull(repository.importProgress.first())
    }

    // 진행도가 남으면 다시 연동했을 때 새 수집 전에 지난 수집의 "리포트 보기"가 뜬다
    @Test
    fun `경기를 지우면 첫 수집 진행도와 확인 시각도 지운다`() = runTest {
        val repository = imported(TestMatchRemote(history(3)))

        repository.deleteAll()

        assertEquals(emptyList(), repository.observeMatches().first())
        assertNull(repository.importProgress.first())
        assertNull(repository.checkedAt.first())
    }

    // 지운 뒤 홈의 "다시 불러오기"를 당기면 첫 수집 없이 8주치를 다 받고, 첫 수집과 같은 경기를 두 번 받는다
    @Test
    fun `첫 수집 전에는 당겨도 새 경기를 받지 않는다`() = runTest {
        val remote = TestMatchRemote(history(3))
        val repository = repositoryOf(remote)

        assertEquals(0, repository.refresh())

        assertEquals(0, remote.listings)
        assertEquals(emptyList(), repository.observeMatches().first())
    }

    @Test
    fun `새 경기는 최신부터 한 판씩 받아 바로 저장하며 진행도를 올린다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(5))
        val repository = imported(remote, clock)
        val fresh = history(20, newest = Thursday + 10.hours, prefix = "new", every = 30.minutes)
        remote.finish(fresh.reversed())
        clock.now = Thursday + 11.hours
        val seen = mutableListOf<NewMatchesProgress?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.newMatchesProgress.toList(seen) }

        assertEquals(20, repository.refresh())

        assertEquals(fresh, repository.observeMatches().first().drop(5))
        assertEquals((0..20).map { NewMatchesProgress(total = 20, received = it) }, seen.filterNotNull())
        assertNull(repository.newMatchesProgress.first())
        assertEquals(Thursday + 11.hours, repository.checkedAt.first())
    }

    // 첫 수집만 최근 50경기를 받는다. 새 경기를 받으며 잘라낸 경기까지 받으면 8주치를 다 받는다.
    @Test
    fun `저장한 경기와 첫 수집이 잘라낸 경기는 새 경기로 받지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(60))
        val repository = imported(remote, clock)
        val fresh = history(3, newest = Thursday + 2.hours, prefix = "new", every = 30.minutes)
        remote.finish(fresh)
        clock.now = Thursday + 3.hours
        val before = remote.fetched.size

        assertEquals(3, repository.refresh())

        assertEquals(fresh.map { it.id }, remote.fetched.drop(before))
    }

    // 첫 수집에서 받은 경기가 없으면 저장한 경기로 가를 수 없어 8주로만 자른다
    @Test
    fun `8주를 넘는 경기와 커스텀 게임은 새 경기로 받지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(emptyList())
        val repository = imported(remote, clock)
        val fresh = match("new", Thursday + 1.hours)
        remote.finish(listOf(fresh, match("old", Thursday - 57.days)))
        remote.playCustomGame("custom", Thursday + 2.hours)
        remote.playCustomGame("custom-in-detail", Thursday + 30.minutes, listedAs = Queue.UNRATED)
        clock.now = Thursday + 3.hours

        assertEquals(1, repository.refresh())

        assertEquals(listOf(fresh), repository.observeMatches().first())
    }

    // 홈과 경기 탭이 같이 당기면 레이트 리밋을 두 번 쓴다
    @Test
    fun `받는 중에 또 당기면 새로 받지 않는다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(3))
        val repository = imported(remote, clock)
        remote.finish(listOf(match("new", Thursday + 1.hours)))
        clock.now = Thursday + 2.hours

        val counts = listOf(async { repository.refresh() }, async { repository.refresh() }).awaitAll()

        assertEquals(listOf(1, 0), counts.sortedDescending())
        assertEquals(2, remote.listings)
        assertEquals(4, repository.observeMatches().first().size)
    }

    // 홈에서 당긴 뒤 다른 화면으로 가거나 앱을 나가도 받던 경기는 마저 받는다
    @Test
    fun `부른 화면이 사라져도 새 경기를 끝까지 받는다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(3))
        val repository = imported(remote, clock)
        remote.finish(history(10, newest = Thursday + 5.hours, prefix = "new", every = 30.minutes))
        clock.now = Thursday + 6.hours

        val caller = launch { repository.refresh() }
        advanceTimeBy(250.milliseconds)
        caller.cancel()
        advanceUntilIdle()

        assertEquals(13, repository.observeMatches().first().size)
    }

    // 연동을 해제하면 저장된 경기를 지운다. 받던 경기가 그 뒤에 다시 채워지면 안 된다.
    @Test
    fun `저장한 경기를 지우면 받던 새 경기도 멈춘다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(3))
        val repository = imported(remote, clock)
        remote.finish(history(10, newest = Thursday + 5.hours, prefix = "new", every = 30.minutes))
        clock.now = Thursday + 6.hours

        launch { repository.refresh() }
        advanceTimeBy(250.milliseconds)
        repository.deleteAll()
        val fetched = remote.fetched.size
        advanceUntilIdle()

        assertEquals(emptyList(), repository.observeMatches().first())
        assertNull(repository.newMatchesProgress.first())
        assertEquals(fetched, remote.fetched.size)
    }

    // 앱에서는 받기가 Dispatchers.Default에서 돌고 지우기는 메인에서 돈다. 받던 한 판이 지운 뒤에 들어오면 연동을 해제해도 전적이 남는다.
    @Test
    fun `다른 스레드에서 받는 중에 지워도 지운 뒤에 경기가 들어오지 않는다`() = runTest {
        withContext(Dispatchers.Default) {
            val clock = StepClock(Thursday)
            val remote = TestMatchRemote(history(3), fetchDelay = 1.milliseconds)
            val repository = OfflineFirstMatchRepository(remote, InMemoryMatchStore(), clock, CoroutineScope(Dispatchers.Default))
            repository.importRecent()
            remote.finish(history(40, newest = Thursday + 1.hours, prefix = "new", every = 1.minutes))
            clock.now = Thursday + 2.hours
            launch { runCatching { repository.refresh() } }
            repository.newMatchesProgress.first { (it?.received ?: 0) >= 3 }

            repository.deleteAll()
            delay(50.milliseconds)

            assertEquals(emptyList(), repository.observeMatches().first())
        }
    }

    // 받은 경기를 다시 받으면 앱 전체에 걸린 Riot 레이트 리밋을 또 쓴다
    @Test
    fun `새 경기를 받다 Riot이 붐비면 멈추고 다음에는 목록을 다시 받지 않고 남은 것만 받는다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(3))
        val repository = imported(remote, clock)
        val fresh = history(20, newest = Thursday + 10.hours, prefix = "new", every = 30.minutes)
        remote.finish(fresh)
        clock.now = Thursday + 11.hours
        remote.failAfter(5, OvalitError.RiotBusy)

        val failure = assertFailsWith<OvalitException> { repository.refresh() }

        assertEquals(OvalitError.RiotBusy, failure.error)
        // 멈춘 것은 받는 중이 아니다. 진행 줄이 남으면 홈이 숫자를 내보내지 않고 당겨도 무시한다.
        assertNull(repository.newMatchesProgress.first())
        assertEquals(fresh.take(5), repository.observeMatches().first().drop(3))
        val listings = remote.listings

        remote.recover()
        clock.now = Thursday + 12.hours
        val seen = mutableListOf<NewMatchesProgress?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.newMatchesProgress.toList(seen) }

        assertEquals(15, repository.refresh())

        assertEquals(listings, remote.listings)
        assertEquals(fresh, repository.observeMatches().first().drop(3))
        assertEquals(NewMatchesProgress(total = 20, received = 5), seen.filterNotNull().first())
        assertEquals(Thursday + 11.hours, repository.checkedAt.first())
    }

    // 받지 못하면 확인 시각이 그대로라 다음에 앱이 보일 때 다시 확인한다(NewMatchesWatcher)
    @Test
    fun `새 경기 목록을 받지 못하면 확인 시각을 그대로 두고 까닭을 실어 던진다`() = runTest {
        val clock = StepClock(Thursday)
        val remote = TestMatchRemote(history(3))
        val repository = imported(remote, clock)
        clock.now = Thursday + 1.hours
        remote.failListing(OvalitError.Offline)

        val failure = assertFailsWith<OvalitException> { repository.refresh() }

        assertEquals(OvalitError.Offline, failure.error)
        assertEquals(Thursday, repository.checkedAt.first())
        assertNull(repository.newMatchesProgress.first())
    }

    // 실제 저장소는 받다 남은 경기를 기기에 둔다. 받던 중에 앱이 꺼졌으면 다시 켰을 때 진행 줄을 띄우고 이어 받는다.
    // 진행 줄만 띄우고 받지 않으면 홈이 새 경기를 기다리며 당겨도 무시한다.
    @Test
    fun `받던 중에 앱이 꺼졌으면 다시 켤 때 남은 새 경기를 이어 받는다`() = runTest {
        val saved = history(3)
        val fresh = history(4, newest = Thursday + 2.hours, prefix = "new", every = 20.minutes)
        val remote = TestMatchRemote(saved + fresh)
        // 맨 앞 한 판은 저장했지만 남은 목록에서 빼기 전에 꺼졌다
        val store = restarted(saved + fresh.take(1), NewMatchesBatch(total = 4, left = fresh.map { it.id }))

        val repository = OfflineFirstMatchRepository(remote, store, StepClock(Thursday + 3.hours), this)
        assertEquals(NewMatchesProgress(total = 4, received = 0), repository.newMatchesProgress.first())
        advanceUntilIdle()

        assertEquals(fresh.drop(1).map { it.id }, remote.fetched)
        assertEquals(0, remote.listings)
        assertEquals(saved + fresh, repository.observeMatches().first())
        assertNull(repository.newMatchesProgress.first())
    }

    @Test
    fun `실패해 멈춘 새 경기는 다시 켜도 당길 때까지 받지 않는다`() = runTest {
        val saved = history(3)
        val fresh = history(4, newest = Thursday + 2.hours, prefix = "new", every = 20.minutes)
        val remote = TestMatchRemote(saved + fresh)
        val store = restarted(saved, NewMatchesBatch(total = 4, left = fresh.map { it.id }, stopped = true))

        val repository = OfflineFirstMatchRepository(remote, store, StepClock(Thursday + 3.hours), this)
        advanceUntilIdle()

        assertEquals(emptyList(), remote.fetched)
        assertNull(repository.newMatchesProgress.first())
        assertEquals(4, repository.refresh())
        assertEquals(0, remote.listings)
    }

    private fun TestScope.repositoryOf(remote: MatchRemoteSource, clock: Clock = StepClock(Thursday)) =
        OfflineFirstMatchRepository(remote, InMemoryMatchStore(), clock, this)

    private suspend fun TestScope.imported(remote: MatchRemoteSource, clock: Clock = StepClock(Thursday)) =
        repositoryOf(remote, clock).also { it.importRecent() }

    // 첫 수집을 마친 뒤 새 경기를 받던 기기를 다시 켠 모양이다
    private fun restarted(matches: List<Match>, batch: NewMatchesBatch) = InMemoryMatchStore(
        matches = matches,
        importProgress = ImportProgress(total = 3, results = List(3) { true }),
        checkedAt = Thursday,
        newMatches = batch,
    )
}

/** 경기와 실패를 정해 둔 서버입니다. 목록을 몇 번 받았는지와 어떤 경기를 받았는지 남깁니다. */
private class TestMatchRemote(
    played: List<Match>,
    private val fetchDelay: Duration = 100.milliseconds,
    private val listDelay: Duration = Duration.ZERO,
) : MatchRemoteSource {
    private val matches = played.toMutableList()
    private val customGames = mutableListOf<MatchListEntry>()
    private var listingError: OvalitError? = null
    private var fetchError: OvalitError? = null
    private var fetchesBeforeError = 0

    var listings = 0
        private set

    val fetched = mutableListOf<MatchId>()

    /** 새 경기가 끝납니다. */
    fun finish(played: List<Match>) {
        matches += played
    }

    /** 목록의 큐가 커스텀 게임입니다. [listedAs]를 주면 목록에는 그 큐로 오고 상세를 받아야 커스텀 게임인 걸 압니다. */
    fun playCustomGame(id: String, startedAt: Instant, listedAs: Queue? = null) {
        customGames += MatchListEntry(MatchId(id), startedAt, listedAs)
    }

    fun failListing(error: OvalitError) {
        listingError = error
    }

    /** 경기를 [count]판 더 준 뒤로는 [error]로 실패합니다. */
    fun failAfter(count: Int, error: OvalitError) {
        fetchesBeforeError = count
        fetchError = error
    }

    fun recover() {
        listingError = null
        fetchError = null
    }

    override suspend fun matchList(): List<MatchListEntry> {
        listings++
        delay(listDelay)
        listingError?.let { throw OvalitException(it) }
        return matches.map { MatchListEntry(it.id, it.startedAt, it.queue) } + customGames
    }

    override suspend fun match(id: MatchId): Match? {
        delay(fetchDelay)
        fetchError?.let { if (fetchesBeforeError-- <= 0) throw OvalitException(it) }
        fetched += id
        return matches.firstOrNull { it.id == id }
    }
}


/** 최신 경기부터 [every]씩 앞서 시작한 경기들입니다. */
private fun history(count: Int, newest: Instant = Thursday - 1.hours, prefix: String = "played", every: Duration = 3.hours) =
    List(count) { match("$prefix-$it", newest - every * it) }

// 받기 규칙은 경기 안을 보지 않아서 ID와 시작 시각, 큐만 채운다
private fun match(id: String, startedAt: Instant, queue: Queue = Queue.COMPETITIVE) = Match(
    id = MatchId(id),
    queue = queue,
    act = ActId("act"),
    map = MapId("map"),
    startedAt = startedAt,
    lengthMillis = 0,
    me = PlayerId("me"),
    myAgent = AgentId("agent"),
    myRole = null,
    allies = emptySet(),
    myCombatScore = 0,
    myTeamWon = true,
    roundOutcomes = emptyList(),
    rounds = emptyList(),
    players = emptyList(),
)
