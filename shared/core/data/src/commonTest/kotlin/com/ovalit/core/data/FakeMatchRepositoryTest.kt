package com.ovalit.core.data

import com.ovalit.core.model.Movement
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.metrics
import com.ovalit.core.model.weeklyReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.math.abs
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Thursday = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)

class FakeMatchRepositoryTest {

    // 가짜 데이터로 화면을 볼 때 세 상태가 다 나와야 한다. 움직인 칸이 없으면 동적 칸을 확인할 수 없다.
    @Test
    fun `가짜 경기로 리포트를 만들면 움직인 지표와 그대로인 지표가 함께 나온다`() {
        val report = assertIs<WeeklyReport.Ready>(fakeMatches(Thursday).weeklyReport(Thursday, Seoul))

        assertEquals(Role.DUELIST, report.mainRole)
        assertTrue(report.dynamic.any { it.movement == Movement.MOVED }, report.dynamic.toString())
        assertTrue(report.dynamic.any { it.movement == Movement.STEADY }, report.dynamic.toString())
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
