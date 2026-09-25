package com.ovalit.feature.report

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.weeklyReport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)
private val ThursdayClock = object : Clock {
    override fun now(): Instant = Thursday
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
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `받은 경기로 주간 리포트를 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(QueueFilter.COMPETITIVE_AND_UNRATED, state.queueFilter)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    @Test
    fun `경기가 새로 들어오면 리포트를 다시 만든다`() = runTest {
        val matches = MutableStateFlow<List<Match>>(emptyList())
        val viewModel = ReportViewModel(StubRepository(matches), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(
            ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, WeeklyReport.NotEnoughMatches(played = 0)),
            viewModel.uiState.value,
        )

        matches.value = FakeMatchRepository(ThursdayClock).observeMatches().first()

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertIs<WeeklyReport.Ready>(state.report)
    }

    // 가짜 경기는 경쟁과 일반뿐이라 기타로 바꾸면 한 경기도 없다
    @Test
    fun `큐를 바꾸면 그 큐 경기로 리포트를 다시 만든다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectQueue(QueueFilter.OTHER)

        assertEquals(
            ReportUiState.Success(QueueFilter.OTHER, WeeklyReport.NotEnoughMatches(played = 0)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `칩을 고르기 전에는 설정의 기본 큐로 시작한다`() = runTest {
        val preferences = StubPreferences(UserPreferences.Default.copy(defaultQueue = QueueFilter.OTHER))
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, preferences, NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(QueueFilter.OTHER, assertIs<ReportUiState.Success>(viewModel.uiState.value).queueFilter)
    }

    @Test
    fun `전적을 공개한 친구만 내 리포트와 같은 기간으로 센다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ReportUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("준호#KR1", "민석#KR3", "재현#KR2"), state.friends.map { it.riotId })
        assertEquals(null, state.rival)
    }

    @Test
    fun `라이벌을 고르면 그 친구를 라이벌 칸에 올린다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val minseok = friends.friends.first().first { it.riotId == "민석#KR3" }.id
        friends.setRival(minseok)

        assertEquals("민석#KR3", assertIs<ReportUiState.Success>(viewModel.uiState.value).rival?.riotId)
    }
    @Test
    fun `친구가 없으면 라이벌 칸 자리에 초대를 권한다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(HomeNudge.INVITE_FRIEND, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
    }

    @Test
    fun `친구는 있고 라이벌이 없으면 라이벌 고르기를 권한다`() = runTest {
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), FakeFriendRepository(ThursdayClock), FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(HomeNudge.PICK_RIVAL, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
    }

    @Test
    fun `홈에서 라이벌을 고르면 권하던 칸 대신 라이벌 대결을 둔다`() = runTest {
        val friends = FakeFriendRepository(ThursdayClock)
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), friends, FakeContentRepository(), ThursdayClock, Seoul)
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
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, StubPreferences(), StubFriends(listOf(hidden)), FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(null, assertIs<ReportUiState.Success>(viewModel.uiState.value).nudge)
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

    @Test
    fun `첫 수집이 끝나기 전에는 리포트를 띄우지 않는다`() = runTest {
        val matches = FakeMatchRepository(ThursdayClock).observeMatches().first()
        val progress = MutableStateFlow<ImportProgress?>(ImportProgress(total = matches.size, results = listOf(true)))
        val repository = StubRepository(MutableStateFlow(matches.take(1)), importProgress = progress)
        val viewModel = ReportViewModel(repository, NoAccount, StubPreferences(), NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(ReportUiState.Loading, viewModel.uiState.value)

        progress.value = ImportProgress(total = 1, results = listOf(true))

        assertIs<ReportUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `고른 관심사로 달라진 점 순서를 정한다`() = runTest {
        val preferences = StubPreferences(UserPreferences.Default.copy(focus = Focus.ROUND_PLAY))
        val viewModel = ReportViewModel(FakeMatchRepository(ThursdayClock), NoAccount, preferences, NoFriends, FakeContentRepository(), ThursdayClock, Seoul)
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
) : MatchRepository {
    override fun observeMatches(): Flow<List<Match>> = matches

    override suspend fun importRecent() = Unit

    override suspend fun deleteAll() = Unit
}

private class StubPreferences(initial: UserPreferences = UserPreferences.Default) : UserPreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun setTheme(theme: ThemePreference) = Unit

    override suspend fun setDefaultQueue(queue: QueueFilter) = Unit

    override suspend fun setStatsPublic(public: Boolean) = Unit

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = Unit

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = Unit

    override suspend fun setFocus(focus: Focus) = Unit
}
