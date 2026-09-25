package com.ovalit.core.model

import com.ovalit.core.model.DynamicMetric.ASSISTS_PER_ROUND
import com.ovalit.core.model.DynamicMetric.FIRST_DUEL_INVOLVEMENT
import com.ovalit.core.model.DynamicMetric.FIRST_DUEL_WIN_RATE
import com.ovalit.core.model.DynamicMetric.FIRST_KILL_WIN_RATE
import com.ovalit.core.model.DynamicMetric.KAST
import com.ovalit.core.model.DynamicMetric.SURVIVAL_RATE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MovementTest {

    @Test
    fun `평소 변동폭으로 잡은 기준선을 넘게 움직이면 움직인 것이다`() {
        assertEquals(Movement.MOVED, KAST.movement(current = stats(kast = 0.73)))
    }

    @Test
    fun `평소 변동폭 안에서 움직이면 그대로다`() {
        assertEquals(Movement.STEADY, KAST.movement(current = stats(kast = 0.72)))
    }

    @Test
    fun `떨어진 것도 움직임이다`() {
        assertEquals(Movement.MOVED, KAST.movement(current = stats(kast = 0.67)))
    }

    // 네 주의 표본 표준편차는 0.0231이고 모표준편차로 재면 0.02다. 기준선은 0.0346과 0.03으로 갈린다.
    @Test
    fun `변동폭은 표본 표준편차로 잰다`() {
        val weeks = listOf(-1, 1, -1, 1).map { stats(kast = 0.70 + 0.02 * it) }

        assertEquals(Movement.STEADY, KAST.movement(current = stats(rounds = 1000, kast = 0.733), history = weeks))
    }

    @Test
    fun `비교 기준이 없으면 판단하지 않는다`() {
        assertEquals(Movement.UNKNOWN, KAST.movement(current = stats(kast = 0.90), baseline = null))
    }

    @Test
    fun `변동폭을 잴 주가 4주 미만이면 판단하지 않는다`() {
        val movement = KAST.movement(current = stats(kast = 0.90), history = UsualWeeks.take(3))

        assertEquals(Movement.UNKNOWN, movement)
    }

    // 20라운드짜리 주까지 넣으면 변동폭이 0.27로 뛰어서 0.10 변화가 묻힌다
    @Test
    fun `표본이 모자란 주는 변동폭 계산에서 뺀다`() {
        val weeks = listOf(-1, 1, -1, 1).map { stats(kast = 0.70 + 0.02 * it) } +
            stats(rounds = 20, kast = 0.10)

        assertEquals(Movement.MOVED, KAST.movement(current = stats(kast = 0.80), history = weeks))
    }

    @Test
    fun `라운드로 재는 지표는 40라운드 미만이면 판단하지 않는다`() {
        for (metric in listOf(KAST, SURVIVAL_RATE, FIRST_DUEL_INVOLVEMENT, ASSISTS_PER_ROUND)) {
            assertEquals(Movement.UNKNOWN, metric.movement(current = stats(rounds = 39)), metric.name)
        }
    }

    @Test
    fun `퍼블 승률은 내 퍼블이 10번은 돼야 판단한다`() {
        val nine = stats(firstKills = 9, firstKillWins = 5)
        val ten = stats(firstKills = 10, firstKillWins = 6)

        assertEquals(Movement.UNKNOWN, FIRST_KILL_WIN_RATE.movement(current = nine))
        assertNotEquals(Movement.UNKNOWN, FIRST_KILL_WIN_RATE.movement(current = ten))
    }

    @Test
    fun `첫 교전 승률은 첫 교전이 15번은 돼야 판단한다`() {
        val fourteen = stats(firstKills = 5, firstDeaths = 9)
        val fifteen = stats(firstKills = 5, firstDeaths = 10)

        assertEquals(Movement.UNKNOWN, FIRST_DUEL_WIN_RATE.movement(current = fourteen))
        assertNotEquals(Movement.UNKNOWN, FIRST_DUEL_WIN_RATE.movement(current = fifteen))
    }

    @Test
    fun `비교 기준도 표본이 모자라면 판단하지 않는다`() {
        val movement = FIRST_KILL_WIN_RATE.movement(
            current = Usual,
            baseline = stats(firstKills = 9, firstKillWins = 5),
        )

        assertEquals(Movement.UNKNOWN, movement)
    }
}

private fun DynamicMetric.movement(
    current: MatchMetrics,
    baseline: MatchMetrics? = Usual,
    history: List<MatchMetrics> = UsualWeeks,
) = assess(current, baseline, history).movement
