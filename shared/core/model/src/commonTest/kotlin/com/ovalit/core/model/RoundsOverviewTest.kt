package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundsOverviewTest {

    @Test
    fun `라운드 킬 순서는 시각 순이고 팀킬과 자기 스킬 사망은 뺀다`() {
        val match = match(round(kill(30.0, Enemy, Ally), kill(5.0, Me, Me), kill(8.0, Ally, Me), kill(12.0, Me, Enemy)))

        val kills = match.roundSummaries().single().kills

        assertEquals(listOf(Me to Enemy, Enemy to Ally), kills.map { it.killer to it.victim })
        assertEquals(listOf(true, false), kills.map { it.firstBlood })
        assertEquals(listOf(true, false), kills.map { it.byMyTeam })
    }

    @Test
    fun `퍼블을 딴 쪽마다 라운드를 몇 번 이겼는지 센다`() {
        val match = match(
            round(kill(5.0, Me, Enemy), won = true, number = 1),
            round(kill(5.0, Ally, Enemy), won = false, number = 2),
            round(kill(5.0, Enemy, Me), won = true, number = 3),
        )

        val overview = match.roundsOverview()

        assertEquals(WinRecord(rounds = 2, wins = 1), overview.ourFirstBlood)
        assertEquals(WinRecord(rounds = 1, wins = 1), overview.theirFirstBlood)
    }

    @Test
    fun `공격과 수비에서 몇 번 이겼는지 센다`() {
        val match = match(
            round(won = true, side = Side.ATTACK, number = 1),
            round(won = false, side = Side.ATTACK, number = 2),
            round(won = true, side = Side.DEFENSE, number = 3),
        )

        val overview = match.roundsOverview()

        assertEquals(WinRecord(rounds = 2, wins = 1), overview.attack)
        assertEquals(WinRecord(rounds = 1, wins = 1), overview.defense)
    }

    // 장비가 상대보다 1,000 넘게 적었는데 이긴 라운드다. 피스톨은 모두 같은 크레드라 뺀다.
    @Test
    fun `장비가 크게 밀렸는데 이긴 라운드를 센다`() {
        val match = match(
            round(won = true, number = 1),
            round(won = true, number = 2),
            round(won = true, number = 3).copy(economy = RoundEconomy(myLoadout = 1_200, teamLoadout = 1_500, enemyLoadout = 4_000)),
            round(won = false, number = 4).copy(economy = RoundEconomy(myLoadout = 1_200, teamLoadout = 1_500, enemyLoadout = 4_000)),
            round(won = true, number = 5).copy(economy = RoundEconomy(myLoadout = 3_900, teamLoadout = 3_900, enemyLoadout = 4_200)),
        )

        assertEquals(1, match.roundsOverview().upsets)
    }

    @Test
    fun `상대 팀 구매 유형도 같은 기준으로 가르고 상대가 이긴 수를 센다`() {
        val match = match(
            round(won = true, number = 1),
            round(won = true, number = 2).copy(economy = RoundEconomy(myLoadout = 4_000, teamLoadout = 4_000, enemyLoadout = 1_500)),
            round(won = false, number = 3).copy(economy = RoundEconomy(myLoadout = 4_000, teamLoadout = 4_000, enemyLoadout = 1_500)),
        )

        val eco = match.buyRecords().single { it.type == BuyType.ECO }

        assertEquals(2, eco.enemyRounds)
        assertEquals(1, eco.enemyWins)
    }
}
