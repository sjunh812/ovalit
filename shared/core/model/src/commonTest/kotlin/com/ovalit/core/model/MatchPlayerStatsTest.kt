package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MatchPlayerStatsTest {

    private val board = listOf(
        line(Me, onMyTeam = true),
        line(Ally, onMyTeam = true),
        line(Enemy, onMyTeam = false),
        line(OtherEnemy, onMyTeam = false),
    )

    @Test
    fun `라운드에서 처음 나온 적 처치로 퍼블과 퍼데를 센다`() {
        val match = match(
            round(kill(20.0, Ally, OtherEnemy), kill(12.0, Enemy, Me)),
            round(kill(8.0, Me, Enemy)),
            players = board,
        )

        val stats = match.playerStats()

        assertEquals(1, stats.getValue(Enemy).firstKills)
        assertEquals(1, stats.getValue(Me).firstDeaths)
        assertEquals(1, stats.getValue(Me).firstKills)
        assertEquals(0, stats.getValue(Ally).firstKills)
    }

    // 리포트와 같은 규칙이다. 스킬로 자기를 죽였거나 팀킬은 퍼블이 아니다.
    @Test
    fun `자기 스킬 사망과 팀킬은 퍼블과 퍼데로 세지 않는다`() {
        val match = match(
            round(kill(3.0, Enemy, Enemy), kill(5.0, Ally, Me), kill(9.0, Me, OtherEnemy)),
            players = board,
        )

        val stats = match.playerStats()

        assertEquals(1, stats.getValue(Me).firstKills)
        assertEquals(1, stats.getValue(OtherEnemy).firstDeaths)
        assertEquals(0, stats.getValue(Me).firstDeaths)
    }

    @Test
    fun `적을 둘 이상 잡은 라운드를 멀티킬로 센다`() {
        val match = match(
            round(kill(5.0, Me, Enemy), kill(9.0, Me, OtherEnemy)),
            round(kill(5.0, Me, Enemy)),
            players = board,
        )

        assertEquals(1, match.playerStats().getValue(Me).multiKillRounds)
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
