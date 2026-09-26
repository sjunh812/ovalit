package com.ovalit.core.model

import com.ovalit.core.model.DynamicMetric.ASSISTS_PER_ROUND
import com.ovalit.core.model.DynamicMetric.ECO_WIN_RATE
import com.ovalit.core.model.DynamicMetric.FIRST_DUEL_INVOLVEMENT
import com.ovalit.core.model.DynamicMetric.FIRST_DUEL_WIN_RATE
import com.ovalit.core.model.DynamicMetric.FIRST_KILL_WIN_RATE
import com.ovalit.core.model.DynamicMetric.FORCE_BUY_WIN_RATE
import com.ovalit.core.model.DynamicMetric.FULL_BUY_WIN_RATE
import com.ovalit.core.model.DynamicMetric.KAST
import com.ovalit.core.model.DynamicMetric.SURVIVAL_RATE
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicSelectionTest {

    @Test
    fun `움직인 지표가 없으면 기본 3개로 채운다`() {
        assertEquals(
            listOf(steady(KAST), steady(SURVIVAL_RATE), steady(FIRST_KILL_WIN_RATE)),
            select(Usual),
        )
    }

    @Test
    fun `움직인 지표를 앞에 두고 남은 칸을 기본 지표로 채운다`() {
        assertEquals(
            listOf(moved(ASSISTS_PER_ROUND), steady(KAST), steady(SURVIVAL_RATE)),
            select(stats(assistsPerRound = 0.60)),
        )
    }

    // 평소 변동폭 대비 관여율 13배, 생존율 6.6배, 어시 5.3배, 퍼블 승률 4배
    @Test
    fun `움직인 지표가 3개를 넘으면 많이 움직인 순으로 모두 둔다`() {
        val current = stats(kast = 0.90, survival = 0.40, assistsPerRound = 0.48, firstKillWins = 18)

        assertEquals(
            listOf(moved(KAST), moved(SURVIVAL_RATE), moved(ASSISTS_PER_ROUND), moved(FIRST_KILL_WIN_RATE)),
            select(current),
        )
    }

    // 관여율 13배, 퍼블 관여율 9.3배, 포스바이 승률 7.9배, 첫 교전 승률 6.6배, 어시 5.3배, 퍼블 승률 4배
    @Test
    fun `움직인 지표는 5개까지만 둔다`() {
        val current = stats(kast = 0.90, assistsPerRound = 0.48, firstDeaths = 30, firstKillWins = 18, forceBuyWins = 14)

        assertEquals(
            listOf(
                moved(KAST),
                moved(FIRST_DUEL_INVOLVEMENT),
                moved(FORCE_BUY_WIN_RATE),
                moved(FIRST_DUEL_WIN_RATE),
                moved(ASSISTS_PER_ROUND),
            ),
            select(current),
        )
    }

    @Test
    fun `비교할 기록이 없으면 기본 3개를 판단 보류로 채운다`() {
        assertEquals(
            listOf(unknown(KAST), unknown(SURVIVAL_RATE), unknown(FIRST_KILL_WIN_RATE)),
            select(stats(kast = 0.90), baseline = null),
        )
    }

    // 퍼데가 늘어 퍼블 관여율과 첫 교전 승률이 조금 움직였다. 관여율이 훨씬 크게 움직였어도 뒤로 간다.
    @Test
    fun `타격대는 퍼블 관여율과 첫 교전 승률을 먼저 띄운다`() {
        val current = stats(kast = 0.90, survival = 0.40, firstDeaths = 20)

        assertEquals(
            listOf(moved(FIRST_DUEL_INVOLVEMENT), moved(FIRST_DUEL_WIN_RATE), moved(KAST), moved(SURVIVAL_RATE)),
            select(current, Role.DUELIST),
        )
    }

    @Test
    fun `타격대에게는 어시스트를 띄우지 않는다`() {
        assertEquals(
            listOf(steady(KAST), steady(SURVIVAL_RATE), steady(FIRST_KILL_WIN_RATE)),
            select(stats(assistsPerRound = 0.60), Role.DUELIST),
        )
    }

    @Test
    fun `척후대는 어시스트와 관여율을 먼저 띄운다`() {
        val current = stats(kast = 0.90, survival = 0.40, assistsPerRound = 0.44)

        assertEquals(
            listOf(moved(ASSISTS_PER_ROUND), moved(KAST), moved(SURVIVAL_RATE)),
            select(current, Role.INITIATOR),
        )
    }

    // 어시가 가장 크게 움직였지만 관여율과 생존율이 앞에 온다
    @Test
    fun `전략가는 관여율과 생존율을 먼저 띄운다`() {
        val current = stats(kast = 0.73, survival = 0.33, assistsPerRound = 0.60)

        assertEquals(
            listOf(moved(KAST), moved(SURVIVAL_RATE), moved(ASSISTS_PER_ROUND)),
            select(current, Role.CONTROLLER),
        )
    }

    @Test
    fun `전략가에게는 퍼블 계열을 띄우지 않고 빈칸도 다른 지표로 채운다`() {
        val current = stats(firstDeaths = 26, firstKillWins = 18)

        assertEquals(
            listOf(steady(KAST), steady(SURVIVAL_RATE), steady(ASSISTS_PER_ROUND)),
            select(current, Role.CONTROLLER),
        )
    }

    @Test
    fun `감시자는 생존율을 먼저 띄우고 퍼블 계열은 띄우지 않는다`() {
        val current = stats(kast = 0.90, survival = 0.33, firstKillWins = 18)

        assertEquals(
            listOf(moved(SURVIVAL_RATE), moved(KAST), steady(ASSISTS_PER_ROUND)),
            select(current, Role.SENTINEL),
        )
    }

    // 이코와 풀바이는 라운드가 없어 판단을 보류했지만 관심사라 자리를 지킨다. 그 뒤에 전략가 우선 지표가 오고,
    // 가장 크게 움직인 어시(13배)는 다섯 칸을 넘어 빠진다.
    @Test
    fun `관심사 지표는 역할 우선 지표보다 앞에 늘 둔다`() {
        val current = stats(kast = 0.73, survival = 0.40, assistsPerRound = 0.60, forceBuyWins = 14)

        assertEquals(
            listOf(
                moved(FORCE_BUY_WIN_RATE),
                unknown(ECO_WIN_RATE),
                unknown(FULL_BUY_WIN_RATE),
                moved(KAST),
                moved(SURVIVAL_RATE),
            ),
            select(current, Role.CONTROLLER, focus = Focus.ROUND_PLAY),
        )
    }

    @Test
    fun `관심사 지표는 움직이지 않았어도 움직인 지표보다 앞에 둔다`() {
        assertEquals(
            listOf(steady(KAST), steady(SURVIVAL_RATE), moved(ASSISTS_PER_ROUND)),
            select(stats(assistsPerRound = 0.60), focus = Focus.CONSISTENCY),
        )
    }

    @Test
    fun `움직인 지표가 없으면 관심사 지표부터 채운다`() {
        assertEquals(
            listOf(steady(FIRST_DUEL_WIN_RATE), steady(KAST), steady(SURVIVAL_RATE)),
            select(Usual, focus = Focus.AIM),
        )
    }

    // 전략가에게 퍼블 계열은 크게 띄우지 않는 지표지만, 사용자가 에임을 보겠다고 골랐다
    @Test
    fun `역할이 크게 띄우지 않는 지표라도 관심사로 고르면 넣는다`() {
        assertEquals(
            listOf(steady(FIRST_DUEL_WIN_RATE), steady(KAST), steady(SURVIVAL_RATE)),
            select(Usual, Role.CONTROLLER, focus = Focus.AIM),
        )
    }
}

private fun select(
    current: MatchMetrics,
    role: Role? = null,
    baseline: MatchMetrics? = Usual,
    focus: Focus = Focus.NONE,
) = selectDynamicMetrics(current, baseline, UsualWeeks, role, focus)
