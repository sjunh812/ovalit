package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 트레이드 받은 데스 비율은 관여율의 트레이드(5초 안에 우리 팀이 내 킬러를 잡음)와 같은 기준으로 셉니다. */
class TradedDeathTest {

    @Test
    fun `트레이드 받은 데스를 내 데스로 나눈다`() {
        val metrics = match(
            round(kill(10.0, Enemy, Me), kill(12.0, Ally, Enemy)),
            round(kill(10.0, Enemy, Me)),
            quietRound(),
        ).metrics()

        assertEquals(1, metrics.tradedDeaths)
        assertRate(0.5, metrics.tradedDeathRate)
    }

    // 관여율과 같은 규칙이다. 한쪽만 바뀌면 트레이드를 받았는데 관여하지 못한 라운드가 생긴다.
    @Test
    fun `5초가 지났거나 내 킬러가 아닌 적을 잡은 건 트레이드 받은 데스가 아니다`() {
        val metrics = match(
            round(kill(10.0, Enemy, Me), kill(15.001, Ally, Enemy)),
            round(kill(10.0, Enemy, Me), kill(12.0, Ally, OtherEnemy)),
            round(kill(10.0, Enemy, Me), kill(12.0, Enemy, Enemy)),
        ).metrics()

        assertEquals(0, metrics.tradedDeaths)
        assertEquals(metrics.kastRounds, metrics.tradedDeaths)
    }

    @Test
    fun `데스가 없으면 트레이드 받은 데스 비율을 비워 둔다`() {
        assertNull(match(quietRound()).metrics().tradedDeathRate)
    }

    @Test
    fun `여러 경기를 더하면 트레이드 받은 데스도 더한다`() {
        val traded = match(round(kill(10.0, Enemy, Me), kill(12.0, Ally, Enemy))).metrics()
        val alone = match(round(kill(10.0, Enemy, Me)), round(kill(10.0, Enemy, Me))).metrics()

        assertRate(1 / 3.0, (traded + alone).tradedDeathRate)
    }
}
