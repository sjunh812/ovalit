package com.ovalit.core.data

import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Focus
import com.ovalit.core.model.InsightMetric
import com.ovalit.core.model.InsightSubject
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.Movement
import com.ovalit.core.model.Queue
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.Role
import com.ovalit.core.model.Side
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.forFirstImport
import com.ovalit.core.model.metrics
import com.ovalit.core.model.myStanding
import com.ovalit.core.model.standings
import com.ovalit.core.model.weeklyReport
import com.ovalit.core.testing.Seoul
import com.ovalit.core.testing.StepClock
import com.ovalit.core.testing.Thursday
import com.ovalit.core.testing.ThursdayClock
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FakeMatchRepositoryTest {

    // 가짜 데이터로 화면을 볼 때 움직인 칸과 그대로인 칸이 다 있어야 동적 칸을 확인할 수 있다.
    @Test
    fun `가짜 경기로 리포트를 만들면 움직인 지표와 그대로인 지표가 함께 나온다`() {
        val report = assertIs<WeeklyReport.Ready>(fakeMatches(Thursday).weeklyReport(Thursday, Seoul))

        assertEquals(Role.DUELIST, report.mainRole)
        assertTrue(report.dynamic.any { it.movement == Movement.MOVED }, report.dynamic.toString())
        assertTrue(report.dynamic.any { it.movement == Movement.STEADY }, report.dynamic.toString())
    }

    // 짚을 점이 헤드샷 말고도 뜨는지 가짜 데이터로 보려고 최근 7일은 K/D가 내려가고 피해량이 오르게 했다
    @Test
    fun `가짜 경기의 이번 주는 피해량이 올라 짚을 점에 뜨고 K_D는 내려간다`() {
        val report = assertIs<WeeklyReport.Ready>(fakeMatches(Thursday).weeklyReport(Thursday, Seoul))
        val note = assertNotNull(report.note)
        val baseline = assertNotNull(report.baseline).metrics

        assertEquals(FixedMetric.DAMAGE, note.moved.metric)
        assertTrue(note.moved.rose)
        assertTrue(report.metrics.kd!! < baseline.kd!!)
    }

    // 가짜 경기는 늘 공격에서 첫 교전을 더 잘 이긴다.
    // 개선 포인트는 이번 액트 경기로 견주니 한 주만 바꾼 멀티킬보다 이 차이가 먼저다.
    // 에임 올리기를 고르면 관심사라서 봤다는 말이 붙는다. 앱이 첫 수집으로 받는 50경기로 본다.
    @Test
    fun `가짜 경기의 개선 포인트는 이번 액트 공수 첫 교전 승률 차이다`() {
        val imported = fakeMatches(Thursday).forFirstImport(Thursday) { it.startedAt }
        fun insight(focus: Focus) =
            assertNotNull(assertIs<WeeklyReport.Ready>(imported.weeklyReport(Thursday, Seoul, focus = focus)).insight)

        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, insight(Focus.NONE).metric)
        assertEquals(InsightSubject.OnSide(Side.ATTACK), insight(Focus.NONE).lead.subject)
        assertEquals(true, insight(Focus.NONE).isRolePriority)
        assertEquals(Focus.AIM, insight(Focus.AIM).focus)
    }

    // 앱을 10분 넘게 떠났다 오면 스무 판이 넘게 쌓여 진행 줄과 WorkManager 이어 받기를 볼 수 있어야 한다
    @Test
    fun `가짜 서버는 마지막으로 목록을 준 뒤 30초마다 한 판이 끝난 것으로 친다`() = runTest {
        val clock = StepClock(Thursday)
        val repository = FakeMatchRepository(clock, scope = this)
        val before = repository.observeMatches().first().size
        clock.now += 10.minutes

        assertEquals(20, repository.refresh())

        val fresh = repository.observeMatches().first().drop(before)
        assertEquals(20, fresh.size)
        assertEquals(fresh.sortedByDescending { it.startedAt }, fresh)
    }

    // 당겨도 아무것도 안 들어오면 가짜 데이터로 새로고침을 볼 수 없다
    @Test
    fun `가짜 저장소는 당기면 늘 한 판은 받는다`() = runTest {
        val repository = FakeMatchRepository(ThursdayClock, scope = this)

        assertEquals(1, repository.refresh())
        assertEquals(1, repository.refresh())
    }

    // 홈에 맞춰 둔 가짜 지표(움직인 칸, 짚을 점, 개선 포인트)는 첫 수집 50경기 기준이다. 앞서 받은 새 경기가 섞이면 틀어진다.
    @Test
    fun `가짜 저장소는 지운 뒤 다시 받는 첫 수집에서 늘 같은 경기를 받는다`() = runTest {
        val repository = FakeMatchRepository(ThursdayClock, importDelay = Duration.ZERO, scope = this)
        repository.refresh()

        repository.deleteAll()
        repository.importRecent()

        assertEquals(fakeMatches(Thursday).forFirstImport(Thursday) { it.startedAt }, repository.observeMatches().first())
    }

    @Test
    fun `가짜 경기는 매번 같다`() {
        assertEquals(fakeMatches(Thursday), fakeMatches(Thursday))
    }

    // 스코어가 14:10처럼 나오면 경기 목록이 가짜로 보인다. 스파이크 돌격은 4승, 신속 플레이는 5승을 먼저 하면 끝난다.
    @Test
    fun `가짜 라운드제 경기는 정해진 승수를 먼저 한 쪽이 이기고 경쟁전 연장은 두 라운드 차이로 끝난다`() {
        for (match in fakeMatches(Thursday).filter { it.format == MatchFormat.ROUNDS }) {
            val (mine, theirs) = match.score
            val toWin = assertNotNull(match.queue.halfRounds) + 1
            assertEquals(toWin, minOf(maxOf(mine, theirs), toWin), "${match.queue} ${match.score}")
            if (match.queue == Queue.COMPETITIVE || match.queue == Queue.PREMIER) assertTrue(abs(mine - theirs) >= 2, match.score.toString())
        }
    }

    @Test
    fun `스코어보드는 우리 팀과 상대 팀 다섯 명씩이고 내 줄은 라운드에서 센 숫자와 같다`() {
        for (match in fakeMatches(Thursday).filter { it.format == MatchFormat.ROUNDS }) {
            assertEquals(5, match.players.count { it.onMyTeam })
            assertEquals(5, match.players.count { !it.onMyTeam })
            val mine = assertNotNull(match.myScoreline)
            assertEquals(match.metrics().kills, mine.kills)
            assertEquals(match.metrics().deaths, mine.deaths)
        }
    }

    // 기타 칩과 경기 탭에서 다른 모드의 모양을 볼 수 있어야 한다
    @Test
    fun `가짜 경기에는 최근 두 주에 다른 모드가 몇 판 섞인다`() {
        val others = fakeMatches(Thursday).filter { it.queue !in QueueFilter.COMPETITIVE_AND_UNRATED.queues }

        assertEquals(
            setOf(Queue.DEATHMATCH, Queue.TEAM_DEATHMATCH, Queue.OTHER, Queue.SPIKE_RUSH, Queue.SWIFTPLAY, Queue.PREMIER),
            others.map { it.queue }.toSet(),
        )
        assertTrue(others.all { Thursday - it.startedAt < 14.days }, others.map { it.startedAt }.toString())
    }

    @Test
    fun `가짜 데스매치는 열네 명이 각자 싸우고 40킬을 채운 사람이 1등이다`() {
        val match = fakeMatches(Thursday).single { it.queue == Queue.DEATHMATCH }
        val standings = match.standings()

        assertEquals(MatchFormat.FREE_FOR_ALL, match.format)
        assertEquals(14, match.players.size)
        assertTrue(standings.all { it.members.size == 1 })
        assertEquals(40, standings.first().points)
        assertEquals(14, assertNotNull(match.myStanding).teams)
    }

    @Test
    fun `가짜 팀 데스매치는 다섯 명씩이고 스코어는 팀 킬이다`() {
        val match = fakeMatches(Thursday).single { it.queue == Queue.TEAM_DEATHMATCH }

        assertEquals(MatchFormat.TEAM_POINTS, match.format)
        assertEquals(5, match.players.count { it.onMyTeam })
        assertEquals(100, maxOf(match.score.myTeam, match.score.enemyTeam))
        assertEquals(match.score.myTeam, match.players.filter { it.onMyTeam }.sumOf { it.kills })
    }

    // 건틀릿: 글리치는 큐 ID를 몰라 기타로 온다. 로봇은 카탈로그에 없어 이름도 역할도 없다.
    @Test
    fun `가짜 건틀릿은 두 명씩 여덟 팀이고 로봇 요원은 카탈로그에 없다`() = runTest {
        val match = fakeMatches(Thursday).single { it.queue == Queue.OTHER }
        val catalog = FakeContentRepository().catalog.first()

        assertEquals(MatchFormat.TEAM_PLACEMENT, match.format)
        assertEquals(16, match.players.size)
        assertEquals((1..8).toList(), match.standings().map { it.rank })
        assertTrue(match.standings().all { it.members.size == 2 })
        assertNull(match.myRole)
        assertTrue(match.players.none { it.agent in catalog.agents })
        assertTrue(match.map !in catalog.maps)
    }

    // 첫 수집 50경기에 다른 모드가 섞이면 그만큼 경쟁·일반이 빠진다. 그래도 홈 리포트가 움직인 칸과 짚을 점을 보여야 한다.
    @Test
    fun `첫 수집한 가짜 경기로도 홈 리포트에 움직인 지표와 짚을 점이 뜨고 기타 리포트도 만들어진다`() {
        val imported = fakeMatches(Thursday).forFirstImport(Thursday) { it.startedAt }
        val report = assertIs<WeeklyReport.Ready>(imported.weeklyReport(Thursday, Seoul))

        assertTrue(report.dynamic.any { it.movement == Movement.MOVED }, report.dynamic.toString())
        assertTrue(report.dynamic.any { it.movement == Movement.STEADY }, report.dynamic.toString())
        assertNotNull(report.note)
        assertIs<WeeklyReport.Ready>(imported.weeklyReport(Thursday, Seoul, queueFilter = QueueFilter.OTHER))
    }
}
