package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EconomyTest {

    @Test
    fun `전반과 후반 첫 라운드는 피스톨 라운드로 따로 둔다`() {
        assertEquals(BuyType.PISTOL, round(number = 1, teamLoadout = 4_500).buyType(Queue.COMPETITIVE))
        assertEquals(BuyType.PISTOL, round(number = 13, teamLoadout = 4_500).buyType(Queue.COMPETITIVE))
        assertEquals(BuyType.FULL_BUY, round(number = 14, teamLoadout = 4_500).buyType(Queue.COMPETITIVE))
    }

    @Test
    fun `우리 팀 평균 장비 가치로 이코와 포스바이와 풀바이를 가른다`() {
        fun buy(loadout: Int) = round(number = 5, teamLoadout = loadout).buyType(Queue.UNRATED)

        assertEquals(BuyType.ECO, buy(1_999))
        assertEquals(BuyType.FORCE_BUY, buy(2_000))
        assertEquals(BuyType.FORCE_BUY, buy(3_899))
        assertEquals(BuyType.FULL_BUY, buy(3_900))
    }

    @Test
    fun `이코노미 규칙이 다른 모드는 가르지 않는다`() {
        assertNull(round(number = 5, teamLoadout = 4_500).buyType(Queue.SPIKE_RUSH))
        assertNull(round(number = 5, teamLoadout = 4_500).buyType(Queue.SWIFTPLAY))
        assertNull(round(number = 1, teamLoadout = 800).buyType(Queue.OTHER))
    }

    @Test
    fun `장비 가치를 모르면 가르지 않는다`() {
        assertNull(round(number = 5).buyType(Queue.COMPETITIVE))
    }

    @Test
    fun `라운드 유형마다 이긴 비율을 따로 센다`() {
        val metrics = match(
            round(number = 1, teamLoadout = 800, won = true),
            round(number = 2, teamLoadout = 2_500, won = true),
            round(number = 3, teamLoadout = 2_500, won = false),
            round(number = 4, teamLoadout = 4_200, won = true),
        ).metrics()

        assertEquals(2, metrics.forceBuyRounds)
        assertRate(0.5, metrics.forceBuyWinRate)
        assertRate(1.0, metrics.fullBuyWinRate)
        assertNull(metrics.ecoWinRate)
    }
}
