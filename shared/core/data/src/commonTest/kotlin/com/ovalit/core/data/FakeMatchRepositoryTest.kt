package com.ovalit.core.data

import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Movement
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.metrics
import com.ovalit.core.model.weeklyReport
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)
private val ThursdayClock = object : Clock {
    override fun now(): Instant = Thursday
}

class FakeMatchRepositoryTest {

    // 가짜 데이터로 화면을 볼 때 세 상태가 다 나와야 한다. 움직인 칸이 없으면 동적 칸을 확인할 수 없다.
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

        assertEquals(FixedMetric.DAMAGE, note.moved?.metric)
        assertTrue(note.moved!!.rose)
        assertTrue(report.metrics.kd!! < baseline.kd!!)
        assertTrue(note.agents.size >= 2, note.agents.toString())
    }

    // 홈과 경기 탭이 같이 당기면 레이트 리밋을 두 번 쓴다
    @Test
    fun `받는 중에 또 당기면 새로 받지 않는다`() = runTest {
        val repository = FakeMatchRepository(clock = ThursdayClock)
        val before = repository.observeMatches().first().size

        val counts = listOf(async { repository.refresh() }, async { repository.refresh() }).awaitAll()

        assertEquals(listOf(1, 0), counts.sortedDescending())
        assertEquals(before + 1, repository.observeMatches().first().size)
    }

    // 다시 연동하면 지난 수집의 "다 불러왔어요"가 남아 새 수집 전에 리포트 보기가 떴다
    @Test
    fun `경기를 지우면 첫 수집 진행도도 지운다`() = runTest {
        val repository = FakeMatchRepository(clock = ThursdayClock)
        repository.importRecent()
        assertTrue(assertNotNull(repository.importProgress.first()).isDone)

        repository.deleteAll()

        assertNull(repository.importProgress.first())
    }

    @Test
    fun `가짜 경기는 매번 같다`() {
        assertEquals(fakeMatches(Thursday), fakeMatches(Thursday))
    }

    // 스코어가 14:10처럼 나오면 경기 목록이 가짜로 보인다
    @Test
    fun `가짜 경기는 13승을 먼저 한 쪽이 이기고 경쟁전 연장은 두 라운드 차이로 끝난다`() {
        for (match in fakeMatches(Thursday)) {
            val (mine, theirs) = match.score
            assertEquals(13, minOf(maxOf(mine, theirs), 13), match.score.toString())
            if (match.queue == Queue.COMPETITIVE) assertTrue(abs(mine - theirs) >= 2, match.score.toString())
        }
    }

    @Test
    fun `스코어보드는 우리 팀과 상대 팀 다섯 명씩이고 내 줄은 라운드에서 센 숫자와 같다`() {
        for (match in fakeMatches(Thursday)) {
            assertEquals(5, match.players.count { it.onMyTeam })
            assertEquals(5, match.players.count { !it.onMyTeam })
            val mine = assertNotNull(match.myScoreline)
            assertEquals(match.metrics().kills, mine.kills)
            assertEquals(match.metrics().deaths, mine.deaths)
        }
    }
}
