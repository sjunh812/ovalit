package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MatchReportTest {

    @Test
    fun `상대마다 잡고 잡히고 도운 수와 주고받은 피해를 센다`() {
        val match = match(
            round(kill(5.0, Me, Enemy), kill(9.0, OtherEnemy, Me)).copy(
                myDamageTo = mapOf(Enemy to 150, OtherEnemy to 40),
                myDamageFrom = mapOf(OtherEnemy to 140),
            ),
            round(kill(5.0, Ally, Enemy, assistedBy = setOf(Me))).copy(myDamageTo = mapOf(Enemy to 60)),
            players = listOf(line(Me, true), line(Enemy, false), line(OtherEnemy, false)),
        )

        val duels = match.duels().associateBy { it.opponent }

        assertEquals(Duel(Enemy, kills = 1, deaths = 0, assists = 1, damageDealt = 210, damageTaken = 0), duels[Enemy])
        assertEquals(Duel(OtherEnemy, kills = 0, deaths = 1, assists = 0, damageDealt = 40, damageTaken = 140), duels[OtherEnemy])
    }

    @Test
    fun `이 경기에서 쓴 무기를 킬이 많은 순으로 센다`() {
        val vandal = WeaponId("vandal")
        val sheriff = WeaponId("sheriff")
        val match = match(
            round(kill(5.0, Me, Enemy, weapon = vandal), kill(8.0, Me, OtherEnemy, weapon = vandal)),
            round(kill(5.0, Me, Enemy, weapon = sheriff)),
        )

        assertEquals(listOf(vandal to 2, sheriff to 1), match.myWeapons().map { it.weapon to it.kills })
    }
}

private fun line(player: PlayerId, onMyTeam: Boolean) = Scoreline(
    player = player,
    riotId = "${player.value}#KR1",
    agent = AgentId("agent"),
    onMyTeam = onMyTeam,
    tier = null,
    playerCard = null,
    kills = 0,
    deaths = 0,
    assists = 0,
    combatScore = 0,
    damage = 0,
    roundsPlayed = 2,
)
