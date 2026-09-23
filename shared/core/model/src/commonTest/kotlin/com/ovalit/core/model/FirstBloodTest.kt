package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirstBloodTest {

    @Test
    fun `라운드에서 처음 적을 잡으면 퍼블이다`() {
        val metrics = match(round(kill(5.0, Me, Enemy), kill(10.0, OtherEnemy, Ally))).metrics()

        assertEquals(1, metrics.firstKills)
        assertEquals(0, metrics.firstDeaths)
    }

    @Test
    fun `라운드에서 처음 죽으면 퍼데다`() {
        val metrics = match(round(kill(5.0, Enemy, Me), kill(10.0, Ally, OtherEnemy))).metrics()

        assertEquals(0, metrics.firstKills)
        assertEquals(1, metrics.firstDeaths)
    }

    @Test
    fun `킬이 들어온 순서가 아니라 시각으로 퍼블을 가린다`() {
        val metrics = match(round(kill(10.0, Ally, Enemy), kill(5.0, Me, OtherEnemy))).metrics()

        assertEquals(1, metrics.firstKills)
    }

    @Test
    fun `우리 팀이 먼저 잡으면 내 퍼블이 아니다`() {
        val metrics = match(round(kill(5.0, Ally, Enemy), kill(10.0, Me, OtherEnemy))).metrics()

        assertEquals(0, metrics.firstKills)
    }

    @Test
    fun `자기 스킬로 먼저 죽은 건 건너뛰고 그다음 킬을 퍼블로 본다`() {
        val metrics = match(round(kill(3.0, Enemy, Enemy), kill(8.0, Me, OtherEnemy))).metrics()

        assertEquals(1, metrics.firstKills)
    }

    @Test
    fun `팀킬이 먼저 나와도 건너뛰고 그다음 킬을 퍼블로 본다`() {
        val metrics = match(round(kill(3.0, Ally, OtherAlly), kill(8.0, Me, Enemy))).metrics()

        assertEquals(1, metrics.firstKills)
    }

    // 교전에서 진 게 아니다. 데스로는 세지만 퍼데는 아니다.
    @Test
    fun `내 스킬로 먼저 죽은 건 퍼데가 아니다`() {
        val metrics = match(round(kill(3.0, Me, Me), kill(8.0, Enemy, Ally))).metrics()

        assertEquals(1, metrics.deaths)
        assertEquals(0, metrics.firstDeaths)
    }

    @Test
    fun `퍼블 승률은 내가 퍼블을 딴 라운드 중 이긴 비율이다`() {
        val metrics = match(
            round(kill(5.0, Me, Enemy), won = true),
            round(kill(5.0, Me, Enemy), won = false),
            round(kill(5.0, Ally, Enemy), won = true),
            round(kill(5.0, Ally, Enemy), won = true),
        ).metrics()

        // 우리 팀 퍼블까지 세면 3/4가 나온다. 내 퍼블 두 번 중 한 번 이겼다.
        assertEquals(1, metrics.firstKillRoundsWon)
        assertRate(0.5, metrics.firstKillWinRate)
    }

    @Test
    fun `퍼블을 한 번도 못 따면 퍼블 승률은 비워 둔다`() {
        val metrics = match(round(kill(5.0, Ally, Enemy)), quietRound()).metrics()

        assertNull(metrics.firstKillWinRate)
    }

    @Test
    fun `퍼블 관여율은 퍼블과 퍼데를 합쳐 라운드 수로 나눈다`() {
        val metrics = match(
            round(kill(5.0, Me, Enemy)),
            round(kill(5.0, Enemy, Me)),
            round(kill(5.0, Ally, Enemy)),
            quietRound(),
        ).metrics()

        assertRate(0.5, metrics.firstDuelInvolvement)
    }

    @Test
    fun `첫 교전 승률은 첫 교전에 들어간 라운드 중 퍼블을 딴 비율이다`() {
        val metrics = match(
            round(kill(5.0, Me, Enemy), won = false),
            round(kill(5.0, Me, Enemy)),
            round(kill(5.0, Me, Enemy)),
            round(kill(5.0, Enemy, Me)),
            round(kill(5.0, Ally, Enemy)),
        ).metrics()

        // 라운드를 졌어도 첫 교전은 이긴 것이다
        assertRate(0.75, metrics.firstDuelWinRate)
    }

    @Test
    fun `첫 교전에 한 번도 안 들어가면 첫 교전 승률은 비워 둔다`() {
        val metrics = match(round(kill(5.0, Ally, Enemy)), quietRound()).metrics()

        assertNull(metrics.firstDuelWinRate)
    }

    @Test
    fun `여러 경기를 합치면 퍼블 기록도 총량으로 더한다`() {
        val won = match(round(kill(5.0, Me, Enemy), won = true), round(kill(5.0, Enemy, Me)))
        val lost = match(round(kill(5.0, Me, Enemy), won = false), quietRound())

        val combined = listOf(won.metrics(), lost.metrics()).sum()

        assertEquals(2, combined.firstKills)
        assertEquals(1, combined.firstDeaths)
        assertRate(0.5, combined.firstKillWinRate)
        assertRate(0.75, combined.firstDuelInvolvement)
    }
}
