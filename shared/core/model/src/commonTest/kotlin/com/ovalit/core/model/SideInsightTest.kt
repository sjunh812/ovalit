package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private val OneDayLater = Instant.fromEpochMilliseconds(0) + 1.days

class SideInsightTest {

    @Test
    fun `공수 생존율이 10퍼센트포인트 벌어지면 문장을 만든다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 26, died = 14)

        val insight = assertNotNull(matches.sideInsight(Role.SENTINEL))

        assertEquals(SideMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(0.75, insight.attack.survivalRate)
        assertEquals(0.65, insight.defense.survivalRate)
    }

    @Test
    fun `9퍼센트포인트 차이면 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 75, died = 25) + side(Side.DEFENSE, survived = 66, died = 34)

        assertNull(matches.sideInsight(Role.SENTINEL))
    }

    @Test
    fun `한쪽이라도 40라운드에 못 미치면 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 36, died = 3) + side(Side.DEFENSE, survived = 10, died = 30)

        assertNull(matches.sideInsight(Role.SENTINEL))
    }

    @Test
    fun `진영을 모르는 라운드는 어느 쪽에도 넣지 않는다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) +
            side(Side.DEFENSE, survived = 20, died = 10) +
            side(side = null, survived = 0, died = 10)

        assertNull(matches.sideInsight(Role.SENTINEL))
    }

    // 감시자는 생존율이 우선이다. 첫 교전 승률이 더 벌어져도 생존율부터 본다.
    @Test
    fun `역할의 우선 지표가 기준을 넘으면 그것부터 고른다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10, openedByMe = 10) +
            side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.sideInsight(Role.SENTINEL))

        assertEquals(SideMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(true, insight.isRolePriority)
    }

    @Test
    fun `우선 지표가 기준에 못 미치면 가장 벌어진 다른 지표를 고른다`() {
        val matches = side(Side.ATTACK, survived = 20, died = 20, openedByMe = 20) +
            side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.sideInsight(Role.SENTINEL))

        assertEquals(SideMetric.KAST, insight.metric)
        assertEquals(false, insight.isRolePriority)
    }

    // CLAUDE.md 역할군 표: 전략가와 감시자는 퍼블을 크게 띄우지 않는다
    @Test
    fun `전략가는 첫 교전 승률로 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 0, died = 40, openedByMe = 20) +
            side(Side.DEFENSE, survived = 0, died = 40)

        assertEquals(SideMetric.KAST, matches.sideInsight(Role.CONTROLLER)?.metric)
        assertEquals(SideMetric.FIRST_DUEL_WIN_RATE, matches.sideInsight(Role.DUELIST)?.metric)
    }

    @Test
    fun `피해량은 기간 평균의 15퍼센트만큼 벌어져야 한다`() {
        val wide = side(Side.ATTACK, survived = 40, died = 0, damage = 170) +
            side(Side.DEFENSE, survived = 40, died = 0, damage = 130)
        val narrow = side(Side.ATTACK, survived = 40, died = 0, damage = 160) +
            side(Side.DEFENSE, survived = 40, died = 0, damage = 140)

        assertEquals(SideMetric.DAMAGE, wide.sideInsight(Role.DUELIST)?.metric)
        assertNull(narrow.sideInsight(Role.DUELIST))
    }

    // 기타 모드는 라운드 수와 크레드 규칙이 달라 공수 비교도 맞지 않는다
    @Test
    fun `기타 모드는 개선 포인트 문장을 만들지 않는다`() {
        fun week(queue: Queue) = List(5) {
            side(Side.ATTACK, survived = 6, died = 2, queue = queue) + side(Side.DEFENSE, survived = 4, died = 4, queue = queue)
        }.flatten()
        fun insight(queue: Queue, filter: QueueFilter) =
            (week(queue).weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, queueFilter = filter) as WeeklyReport.Ready).insight

        assertNotNull(insight(Queue.COMPETITIVE, QueueFilter.COMPETITIVE))
        assertNull(insight(Queue.SPIKE_RUSH, QueueFilter.OTHER))
    }

    // 감시자는 퍼블 쪽을 크게 띄우지 않지만, 에임 올리기를 골랐으면 첫 교전 승률부터 본다
    @Test
    fun `관심사 지표가 기준을 넘으면 역할 기준보다 먼저 고른다`() {
        val insight = assertNotNull(aimGap().sideInsight(Role.SENTINEL, Focus.AIM))

        assertEquals(SideMetric.FIRST_DUEL_WIN_RATE, insight.metric)
        assertEquals(Focus.AIM, insight.focus)
        assertEquals(false, insight.isRolePriority)
        assertEquals(SideMetric.KAST, aimGap().sideInsight(Role.SENTINEL)?.metric)
    }

    @Test
    fun `라운드 운영을 고르면 공수별 포스바이 승률도 본다`() {
        val matches = buys(Side.ATTACK, won = 15, lost = 5) + buys(Side.DEFENSE, won = 5, lost = 15)

        val insight = assertNotNull(matches.sideInsight(Role.CONTROLLER, Focus.ROUND_PLAY))

        assertEquals(SideMetric.FORCE_BUY_WIN_RATE, insight.metric)
        assertEquals(Focus.ROUND_PLAY, insight.focus)
        // 관심사가 아니면 이코·포스바이·풀바이 승률은 후보가 아니다
        assertNull(matches.sideInsight(Role.CONTROLLER))
    }

    @Test
    fun `관심사 지표가 표본이나 기준에 못 미치면 역할 기준으로 돌아간다`() {
        val matches = buys(Side.ATTACK, won = 8, lost = 2) + buys(Side.DEFENSE, won = 2, lost = 8) +
            side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.sideInsight(Role.SENTINEL, Focus.ROUND_PLAY))

        assertEquals(SideMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(null, insight.focus)
        assertEquals(true, insight.isRolePriority)
    }

    // 역할을 모르는 경기라 관심사가 없으면 가장 벌어진 첫 교전 승률이 뽑힌다
    @Test
    fun `홈 리포트의 개선 포인트도 관심사를 따른다`() {
        val matches = List(3) { aimGap() }.flatten()
        fun insight(focus: Focus) =
            (matches.weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, focus = focus) as WeeklyReport.Ready).insight

        assertEquals(SideMetric.FIRST_DUEL_WIN_RATE, insight(Focus.NONE)?.metric)
        assertEquals(SideMetric.KAST, insight(Focus.CONSISTENCY)?.metric)
    }

    // 사용자 요청(2026-09-27): 멀티킬 라운드 비율도 공수로 나눠 본다. 타격대의 우선 지표인 첫 교전 승률은 표본이 모자라다.
    @Test
    fun `멀티킬 라운드 비율이 공수로 벌어지면 문장을 만든다`() {
        val insight = assertNotNull(multiKillGap().sideInsight(Role.DUELIST))

        assertEquals(SideMetric.MULTI_KILL_RATE, insight.metric)
        assertEquals(false, insight.isRolePriority)
    }

    // 동적 칸처럼 K/D를 크게 띄우지 않는 역할은 멀티킬도 빼지만, 에임 올리기를 골랐으면 본다
    @Test
    fun `척후대와 전략가는 에임 올리기를 고르지 않았으면 멀티킬로 문장을 만들지 않는다`() {
        for (role in listOf(Role.INITIATOR, Role.CONTROLLER)) {
            assertNull(multiKillGap().sideInsight(role), "$role")
            val insight = assertNotNull(multiKillGap().sideInsight(role, Focus.AIM), "$role")
            assertEquals(SideMetric.MULTI_KILL_RATE, insight.metric)
            assertEquals(Focus.AIM, insight.focus)
        }
        assertEquals(SideMetric.MULTI_KILL_RATE, multiKillGap().sideInsight(Role.SENTINEL)?.metric)
    }

    @Test
    fun `역할을 모르면 가장 벌어진 지표를 고르고 우선 지표라고 하지 않는다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.sideInsight(role = null))

        assertEquals(false, insight.isRolePriority)
    }
}

/**
 * 공격에서는 퍼블 20번에 퍼데 10번, 수비에서는 퍼블 5번에 퍼데 25번입니다. 첫 교전 승률이 67%와 17%로 벌어지고,
 * 관여율도 75%와 38%로 벌어집니다. 생존율은 두 진영 모두 25%입니다.
 */
private fun aimGap() = side(Side.ATTACK, survived = 10, died = 30, openedByMe = 20) +
    side(Side.DEFENSE, survived = 10, died = 30, openedByMe = 5)

/**
 * 공격 40라운드 중 12라운드, 수비 40라운드 중 2라운드에서 적을 둘 잡습니다. 멀티킬 라운드 비율이 30%와 5%로 벌어집니다.
 * 한 번도 죽지 않아 생존율과 관여율은 두 진영 모두 100%이고, 첫 교전은 15번에 못 미칩니다.
 */
private fun multiKillGap(): List<Match> {
    fun side(side: Side, multi: Int) = List(40) { index ->
        if (index < multi) round(kill(5.0, Me, Enemy), kill(8.0, Me, OtherEnemy), side = side) else round(side = side)
    }
    return listOf(match(*(side(Side.ATTACK, multi = 12) + side(Side.DEFENSE, multi = 2)).toTypedArray()))
}

/** 한 진영의 포스바이 라운드를 경기 하나에 담습니다. 피스톨 라운드가 아니게 번호를 5로 둡니다. */
private fun buys(side: Side, won: Int, lost: Int): List<Match> {
    val rounds = List(won + lost) { index -> round(side = side, number = 5, teamLoadout = 3000, won = index < won) }
    return listOf(match(*rounds.toTypedArray()))
}

/**
 * 한 진영 라운드를 경기 하나에 담습니다. [survived]라운드는 살아남고 [died]라운드는 죽습니다.
 * 죽는 라운드 중 앞의 [openedByMe]라운드는 내가 퍼블을 딴 뒤 죽고, 나머지는 퍼데입니다. 퍼블을 딴 라운드는
 * 관여율에도 들어갑니다.
 */
private fun side(
    side: Side?,
    survived: Int,
    died: Int,
    openedByMe: Int = 0,
    damage: Int = 0,
    queue: Queue = Queue.COMPETITIVE,
): List<Match> {
    val list = List(survived) { round(damage = damage, side = side) } +
        List(died) { index ->
            if (index < openedByMe) {
                round(kill(5.0, Me, Enemy), kill(10.0, OtherEnemy, Me), damage = damage, side = side)
            } else {
                round(kill(10.0, Enemy, Me), damage = damage, side = side)
            }
        }
    return listOf(match(*list.toTypedArray(), queue = queue))
}
