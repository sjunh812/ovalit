package com.ovalit.feature.report

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.weekStarts
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.nextWeekStart
import com.ovalit.core.model.weeklyReport
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// 앱은 Dispatchers.Default에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서 센다
private val SameThread = EmptyCoroutineContext

private val Seoul = TimeZone.of("Asia/Seoul")
private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)
private val ThursdayClock = object : Clock {
    override fun now(): Instant = Thursday
}

private class StepClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `경기를 받기 전에는 불러오는 중이다`() {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `내 프로필 안내는 띄웠다고 적으면 다시 띄우지 않는다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.profileHint.collect() }

        assertTrue(viewModel.profileHint.value)
        viewModel.markProfileHintSeen()
        advanceUntilIdle()
        assertFalse(viewModel.profileHint.value)
    }

    // 메인 스레드에서 세면 첫 수집 뒤 홈으로 넘어가는 전환이 멈춘다
    @Test
    fun `리포트와 배지는 넘겨받은 곳에서 센다`() = runTest {
        val computation = StandardTestDispatcher(testScheduler)
        val matches = FakeMatchRepository(ThursdayClock)
        val viewModel = ReportViewModel(matches, FakeAccountRepository(matches, ThursdayClock), StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = computation)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.badge.collect() }

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)
        assertNull(viewModel.badge.value)
        advanceUntilIdle()
        assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertNotNull(viewModel.badge.value)
    }

    @Test
    fun `받은 경기로 주간 리포트를 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(QueueFilter.COMPETITIVE_AND_UNRATED, state.queueFilter)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    @Test
    fun `경기가 새로 들어오면 리포트를 다시 만든다`() = runTest {
        val matches = MutableStateFlow<List<Match>>(emptyList())
        val imported = flowOf(ImportProgress(total = 0, results = emptyList()))
        val viewModel = ReportViewModel(StubRepository(matches, imported), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(
            ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, WeeklyReport.NotEnoughMatches(played = 0)),
            viewModel.uiState.value,
        )

        matches.value = FakeMatchRepository(ThursdayClock).observeMatches().first()

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    // 설정에서 저장된 데이터를 지우면 경기도 첫 수집 진행도도 없다. 안 뛴 게 아니라 지운 거라 다시 불러오기를 권한다.
    @Test
    fun `저장된 경기를 지웠으면 다시 불러오기를 권한다`() = runTest {
        val viewModel = ReportViewModel(StubRepository(flowOf(emptyList())), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertTrue(assertIs<ReportUiState.Success>(viewModel.uiState.value).needsImport)
    }

    // 홈을 켜 둔 채 월요일 0시를 넘기면 새 경기가 없어도 이번 주가 지난주가 된다
    @Test
    fun `주가 바뀌면 새 경기가 없어도 리포트 기간을 다시 잡는다`() = runTest {
        var now = Thursday
        val clock = object : Clock {
            override fun now(): Instant = now
        }
        val weekChanges = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        val matches = FakeMatchRepository(ThursdayClock)
        val viewModel = ReportViewModel(matches, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), clock, Seoul, weekChanges, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val before = assertIs<WeeklyReport.Ready>(assertIs<ReportUiState.Success>(viewModel.uiState.value).report).period
        assertTrue(before.includesThisWeek)

        now = Thursday.nextWeekStart(Seoul) + 1.hours
        weekChanges.emit(Unit)

        val after = assertIs<WeeklyReport.Ready>(assertIs<ReportUiState.Success>(viewModel.uiState.value).report).period
        assertFalse(after.includesThisWeek)
    }

    // 앱에서 넘기는 흐름은 지금 한 번, 그다음은 월요일 0시에 흐른다
    @Test
    fun `주 바뀜 흐름은 다음 월요일 0시까지 기다렸다 흐른다`() = runTest {
        val clock = object : Clock {
            override fun now(): Instant = Thursday + testScheduler.currentTime.milliseconds
        }
        val ticks = mutableListOf<Long>()
        backgroundScope.launch { weekStarts(clock, Seoul).take(2).collect { ticks += testScheduler.currentTime } }
        advanceTimeBy((Thursday.nextWeekStart(Seoul) - Thursday).inWholeMilliseconds + 1)

        assertEquals(listOf(0L, (Thursday.nextWeekStart(Seoul) - Thursday).inWholeMilliseconds), ticks)
    }

    // 가짜 경기는 최근 두 주에 다른 모드를 여덟 판 섞었다. 기타 리포트는 그중 리포트에 넣는 스파이크 돌격, 신속 플레이, 프리미어
    // 다섯 판만 센다. 데스매치, 팀 데스매치, 건틀릿은 목록에만 둔다.
    @Test
    fun `큐를 바꾸면 그 큐 경기로 리포트를 다시 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectQueue(QueueFilter.OTHER)

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        val report = assertIs<WeeklyReport.Ready>(state.report)
        assertEquals(QueueFilter.OTHER, state.queueFilter)
        assertEquals(5, report.metrics.matches)
        assertEquals(emptyList(), report.dynamic)
    }

    @Test
    fun `칩을 고르기 전에는 설정의 기본 큐로 시작한다`() = runTest {
        val preferences = StubPreferences(UserPreferences.Default.copy(defaultQueue = QueueFilter.OTHER))
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, preferences, NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(QueueFilter.OTHER, assertIs<ReportUiState.Success>(viewModel.uiState.value).queueFilter)
    }

    @Test
    fun `전적을 공개한 친구만 내 리포트와 같은 기간으로 센다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("민석#KR3", "재현#KR2", "준호#KR1"), state.friends.map { it.riotId }.sorted())
        assertEquals(null, state.rival)
    }

    // 친구가 수백 명이어도 다시 같이 할 사람이 라이벌 고르기 시트 위에 온다
    @Test
    fun `라이벌 후보는 최근에 같이 뛴 친구부터 세우고 같이 뛴 적 없으면 그 기간 경기가 많은 친구부터 둔다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock)
        val mine = matches.observeMatches().first()
        val recent = Friend(PlayerId("recent"), "최근#KR1", null, statsPublic = true, matches = emptyList(), lastPlayedTogether = Thursday - 1.days)
        val many = Friend(PlayerId("many"), "많이#KR1", null, statsPublic = true, matches = mine)
        val few = Friend(PlayerId("few"), "조금#KR1", null, statsPublic = true, matches = mine.sortedByDescending { it.startedAt }.take(1))
        val viewModel = ReportViewModel(matches, NoAccount, StubPreferences(), StubFriends(listOf(few, many, recent)), FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("recent", "many", "few"), state.friends.map { it.id.value })
    }

    @Test
    fun `전체 순위는 홈과 같은 큐와 기간으로 센다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock)
        val friends = FakeFriendRepository(ThursdayClock)
        val home = ReportViewModel(matches, NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        val ranking = FriendRankingViewModel(QueueFilter.COMPETITIVE, matches, friends, ThursdayClock, Seoul, computation = SameThread)
        home.selectQueue(QueueFilter.COMPETITIVE)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { ranking.uiState.collect() }

        val homeState = assertIs<ReportUiState.Success>(home.uiState.value)
        val rankingState = assertIs<FriendRankingUiState.Success>(ranking.uiState.value)
        assertEquals(assertIs<WeeklyReport.Ready>(homeState.report).period, rankingState.period)
        assertEquals(homeState.friends, rankingState.friends)
    }

    // 기타 모드에는 친구 비교가 없다
    @Test
    fun `전체 순위는 기타 모드면 닫는다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock)
        val ranking = FriendRankingViewModel(QueueFilter.OTHER, matches, FakeFriendRepository(ThursdayClock), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { ranking.uiState.collect() }

        assertEquals(FriendRankingUiState.Gone, ranking.uiState.value)
    }

    @Test
    fun `라이벌을 고르면 그 친구를 라이벌 칸에 올린다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val minseok = friends.friends.first().first { it.riotId == "민석#KR3" }.id
        friends.setRival(minseok)

        assertEquals("민석#KR3", assertIs<ReportUiState.Success>(viewModel.uiState.value).rival?.riotId)
    }
    @Test
    fun `친구가 없으면 라이벌 칸 자리에 초대를 권한다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(HomeNudge.INVITE_FRIEND, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
    }

    @Test
    fun `친구는 있고 라이벌이 없으면 라이벌 고르기를 권한다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), FakeFriendRepository(ThursdayClock), FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(HomeNudge.PICK_RIVAL, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
    }

    @Test
    fun `홈에서 라이벌을 고르면 권하던 칸 대신 라이벌 대결을 둔다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectRival(friends.friends.first().first { it.riotId == "준호#KR1" }.id)

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals("준호#KR1", state.rival?.riotId)
        assertEquals(null, state.nudge)
    }

    // 비공개 친구도 친구다. 초대를 권하면 이미 친구가 있는데 친구를 부르라는 말이 된다.
    @Test
    fun `친구가 모두 전적 비공개면 초대도 라이벌도 권하지 않는다`() = runTest {
        val hidden = Friend(PlayerId("seoyeon"), "서연#KR7", playerCard = null, statsPublic = false, matches = emptyList())
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), StubFriends(listOf(hidden)), FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(null, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
    }

    @Test
    fun `홈을 당겨 새 경기를 받으면 리포트를 다시 만든다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock, scope = this)
        val viewModel = ReportViewModel(matches, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val before = assertIs<ReportUiState.Success>(viewModel.uiState.value).report
        val countBefore = matches.observeMatches().first().size

        viewModel.refresh()
        advanceUntilIdle()

        // 새 경기로 이번 주가 다섯 판을 채우면 기간이 좁아져 기간 경기 수가 오히려 준다. 그래서 받은 경기 전체로 다시
        // 만든 리포트와 같은지 본다.
        val all = matches.observeMatches().first()
        val after = assertIs<ReportUiState.Success>(viewModel.uiState.value).report
        assertEquals(countBefore + 1, all.size)
        assertNotEquals(before, after)
        assertEquals(all.weeklyReport(Thursday, Seoul), after)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    // 받는 대로 숫자가 바뀌면 첫 수집처럼 믿을 수 없다. 이번 주를 보고 있었으면 새 경기도 이번 주라 기간은 그대로다.
    @Test
    fun `새 경기를 여러 판 받는 동안 숫자는 그대로 두고 다 받은 뒤 한 번만 바꾼다`() = runTest {
        val clock = StepClock(Thursday)
        val matches = FakeMatchRepository(clock, scope = this)
        matches.importRecent()
        val viewModel = ReportViewModel(matches, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), clock, Seoul, computation = SameThread)
        val reports = mutableListOf<WeeklyReport>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { (it as? ReportUiState.Success)?.let { state -> reports += state.report } }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.newMatches.collect() }
        val before = reports.single()
        clock.now += 10.minutes

        viewModel.refresh()
        advanceTimeBy(3_000.milliseconds)

        val receiving = assertNotNull(viewModel.newMatches.value)
        assertTrue(receiving.received in 1..<receiving.total)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(listOf(before), reports.distinct())
        assertFalse(assertIs<ReportUiState.Success>(viewModel.uiState.value).waitingForNewMatches)

        advanceUntilIdle()

        assertNull(viewModel.newMatches.value)
        assertEquals(listOf(before, matches.observeMatches().first().weeklyReport(clock.now, Seoul)), reports.distinct())
    }

    // 일주일 넘게 쉬면 받기 전 경기로는 "지난주"를 센다. 그 숫자를 띄우면 다 받은 뒤 기간까지 바뀌어 틀린 말이 된다.
    @Test
    fun `이번 주가 아닌 기간을 보던 중 여러 판을 받으면 리포트 자리를 비워 둔다`() = runTest {
        val clock = StepClock(Thursday)
        val matches = FakeMatchRepository(clock, scope = this)
        matches.importRecent()
        clock.now += 7.days
        val viewModel = ReportViewModel(matches, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), clock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.newMatches.collect() }
        val stale = assertIs<WeeklyReport.Ready>(assertIs<ReportUiState.Success>(viewModel.uiState.value).report)
        assertFalse(stale.period.includesThisWeek)

        viewModel.refresh()
        advanceTimeBy(3_000.milliseconds)
        assertTrue(assertIs<ReportUiState.Success>(viewModel.uiState.value).waitingForNewMatches)

        advanceUntilIdle()

        val fresh = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertFalse(fresh.waitingForNewMatches)
        assertTrue(assertIs<WeeklyReport.Ready>(fresh.report).period.includesThisWeek)
    }

    // 리포트를 만들 기록이 모자라면 그 안내가 먼저다. 친구가 없어도 초대를 권하지 않는다.
    @Test
    fun `리포트를 만들 기록이 없으면 아무것도 권하지 않는다`() {
        val nudge = homeNudge(
            report = WeeklyReport.NotEnoughMatches(played = 2),
            queueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
            hasFriends = false,
            rivalCandidates = emptyList(),
            rival = null,
        )

        assertEquals(null, nudge)
    }

    @Test
    fun `기타 모드에는 친구 칸이 없어서 권하지 않는다`() {
        assertEquals(null, homeNudge(ReportPreviewData.otherQueue, QueueFilter.OTHER, hasFriends = false, rivalCandidates = emptyList(), rival = null))
    }

    // 전적을 공개하지 않은 친구는 라이벌로 고를 수 없어서, 고를 사람이 없는데 고르라고 하지 않는다
    @Test
    fun `친구가 모두 전적 비공개면 라이벌을 권하지 않는다`() {
        assertEquals(null, homeNudge(ReportPreviewData.moved, QueueFilter.COMPETITIVE_AND_UNRATED, hasFriends = true, rivalCandidates = emptyList(), rival = null))
    }

    @Test
    fun `라이벌이 있으면 아무것도 권하지 않는다`() {
        val junho = FriendStanding(PlayerId("junho"), "준호#KR1", null)
        assertEquals(null, homeNudge(ReportPreviewData.moved, QueueFilter.COMPETITIVE_AND_UNRATED, hasFriends = true, rivalCandidates = listOf(junho), rival = junho))
    }

    // 라이벌로 둔 친구가 전적을 비공개로 바꾸면 라이벌 칸은 없어도 라이벌은 그대로다
    @Test
    fun `라이벌이 비공개로 바뀌었으면 다른 라이벌을 고르라고 하지 않는다`() {
        val junho = FriendStanding(PlayerId("junho"), "준호#KR1", null)
        val nudge = homeNudge(
            ReportPreviewData.moved,
            QueueFilter.COMPETITIVE_AND_UNRATED,
            hasFriends = true,
            rivalCandidates = listOf(junho),
            rival = null,
            hasRival = true,
        )

        assertEquals(null, nudge)
    }

    @Test
    fun `첫 수집이 끝나기 전에는 리포트를 띄우지 않는다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock).observeMatches().first()
        val progress = MutableStateFlow<ImportProgress?>(ImportProgress(total = matches.size, results = listOf(true)))
        val repository = StubRepository(MutableStateFlow(matches.take(1)), importProgress = progress)
        val viewModel = ReportViewModel(repository, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)

        progress.value = ImportProgress(total = 1, results = listOf(true))

        assertIs<ReportUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `고른 관심사로 달라진 점 순서를 정한다`() = runTest {
        val preferences = StubPreferences(UserPreferences.Default.copy(focus = Focus.ROUND_PLAY))
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, preferences, NoFriends, FakeContentRepository(), ThursdayClock, Seoul, computation = SameThread)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val report = assertIs<WeeklyReport.Ready>(assertIs<ReportUiState.Success>(viewModel.uiState.value).report)
        val expected = FakeMatchRepository(ThursdayClock).observeMatches().first()
            .weeklyReport(ThursdayClock.now(), Seoul, focus = Focus.ROUND_PLAY)

        assertEquals(assertIs<WeeklyReport.Ready>(expected).dynamic, report.dynamic)
    }
}

private open class StubFriends(list: List<Friend> = emptyList()) : FriendRepository {
    override val friends: Flow<List<Friend>> = flowOf(list)
    override val requests: Flow<List<FriendRequest>> = flowOf(emptyList())
    override val rival: Flow<PlayerId?> = flowOf(null)
    override val sentRequests: Flow<Set<PlayerId>> = flowOf(emptySet())

    override suspend fun appUsersAmong(players: Collection<PlayerId>) = emptySet<PlayerId>()

    override suspend fun sendRequest(id: PlayerId) = Unit

    override suspend fun accept(id: PlayerId) = Unit

    override suspend fun decline(id: PlayerId) = Unit

    override suspend fun unfriend(id: PlayerId) = Unit

    override suspend fun setRival(id: PlayerId?) = Unit

    override suspend fun refresh() = Unit

    override fun inviteLink() = ""
}

private object NoFriends : StubFriends()

private object NoAccount : AccountRepository {
    override val account: Flow<Account?> = flowOf(null)

    override suspend fun unlink() = Unit
}

private class StubRepository(
    private val matches: Flow<List<Match>>,
    override val importProgress: Flow<ImportProgress?> = flowOf(null),
    override val newMatchesProgress: Flow<NewMatchesProgress?> = flowOf(null),
) : MatchRepository {
    override val checkedAt: Flow<Instant?> = flowOf(null)

    override fun observeMatches(): Flow<List<Match>> = matches

    override suspend fun importRecent() = Unit

    override suspend fun refresh() = 0

    override suspend fun deleteAll() = Unit
}

private class StubPreferences(initial: UserPreferences = UserPreferences.Default) : UserPreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun setTheme(theme: ThemePreference) = Unit

    override suspend fun setDefaultQueue(queue: QueueFilter) = Unit

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = Unit

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = Unit

    override suspend fun setNotifyPing(enabled: Boolean) = Unit

    override suspend fun setFocus(focus: Focus) = Unit

    override suspend fun setSeenProfileHint() = preferences.update { it.copy(seenProfileHint = true) }

    override suspend fun setAdFreeUntil(until: Instant) = preferences.update { it.copy(adFreeUntil = until) }
}
