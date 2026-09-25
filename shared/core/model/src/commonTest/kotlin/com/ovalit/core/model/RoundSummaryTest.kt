package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundSummaryTest {

    @Test
    fun `내가 못 뛴 라운드도 승패는 남기고 뛰지 않았다고 표시한다`() {
        val game = match(round(number = 1, won = true), round(number = 3, won = false))
            .copy(roundOutcomes = listOf(true, true, false))

        val summaries = game.roundSummaries()

        assertEquals(listOf(1, 2, 3), summaries.map { it.number })
        assertEquals(listOf(true, false, true), summaries.map { it.played })
        assertTrue(summaries[1].won)
    }

    @Test
    fun `라운드마다 내 킬과 퍼블을 센다`() {
        val game = match(
            round(kill(10.0, killer = Me, victim = Enemy), kill(30.0, killer = Me, victim = OtherEnemy), number = 1),
            round(kill(12.0, killer = Enemy, victim = Me), number = 2),
        )

        val (first, second) = game.roundSummaries()

        assertEquals(2, first.myKills)
        assertTrue(first.firstKill)
        assertFalse(second.firstKill)
        assertTrue(second.firstDeath)
    }

    @Test
    fun `이코노미 요약은 뛴 라운드만 유형별로 센다`() {
        val game = match(
            round(number = 1, teamLoadout = 800, won = true),
            round(number = 2, teamLoadout = 1_200, won = false),
            round(number = 3, teamLoadout = 2_600, won = true),
            round(number = 4, teamLoadout = 4_300, won = true),
            round(number = 5, teamLoadout = 4_300, won = false),
        )

        assertEquals(
            listOf(
                BuyRecord(BuyType.FULL_BUY, rounds = 2, wins = 1),
                BuyRecord(BuyType.FORCE_BUY, rounds = 1, wins = 1),
                BuyRecord(BuyType.ECO, rounds = 1, wins = 0),
            ),
            game.buyRecords(),
        )
    }
}
