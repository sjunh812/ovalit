package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchPlacementTest {

    @Test
    fun `전투점수 순으로 자리를 매기고 1등이 MVP 다른 팀 1등이 팀 MVP다`() {
        val match = match(
            queue = Queue.COMPETITIVE,
            players = listOf(
                line(Me, onMyTeam = true, acs = 250),
                line(Ally, onMyTeam = true, acs = 180),
                line(Enemy, onMyTeam = false, acs = 300),
                line(OtherEnemy, onMyTeam = false, acs = 200),
            ),
        )

        val placements = match.placements()

        assertEquals(MatchPlacement(rank = 1, players = 4, award = MatchAward.MVP), placements[Enemy])
        assertEquals(MatchPlacement(rank = 2, players = 4, award = MatchAward.TEAM_MVP), placements[Me])
        assertEquals(MatchPlacement(rank = 4, players = 4, award = null), placements[Ally])
        assertEquals(placements[Me], match.myPlacement)
    }

    // 게임 스코어보드는 라운드당 점수로 줄을 세운다. 튕겨서 덜 뛴 사람이 합계로 밀리면 안 된다.
    @Test
    fun `합계가 아니라 라운드당 전투점수로 견준다`() {
        val match = match(
            players = listOf(
                line(Me, onMyTeam = true, acs = 200, rounds = 20),
                line(Ally, onMyTeam = true, acs = 260, rounds = 10),
            ),
        )

        assertEquals(1, match.placements()[Ally]?.rank)
    }

    @Test
    fun `전투점수가 같으면 킬이 많은 쪽이 앞이다`() {
        val match = match(
            players = listOf(
                line(Me, onMyTeam = true, acs = 200, kills = 12),
                line(Enemy, onMyTeam = false, acs = 200, kills = 15),
            ),
        )

        assertEquals(MatchAward.MVP, match.placements()[Enemy]?.award)
    }

    // 데스매치 응답이 한 판을 한 라운드로 주면 경기 전체 점수가 라운드당 값이 된다. 게임도 데스매치는 킬로 줄을 세운다.
    @Test
    fun `데스매치는 전투점수가 아니라 킬로 등수를 매기고 MVP가 없다`() {
        val match = match(
            queue = Queue.DEATHMATCH,
            players = listOf(
                line(Me, onMyTeam = true, acs = 4_000, rounds = 1, kills = 25),
                line(Enemy, onMyTeam = false, acs = 300, rounds = 1, kills = 40),
                line(OtherEnemy, onMyTeam = false, acs = 900, rounds = 1, kills = 31),
            ),
        )

        assertEquals(MatchPlacement(rank = 3, players = 3, award = null), match.myPlacement)
        assertEquals(1, match.placements()[Enemy]?.rank)
        assertNull(match.acsOf(match.players.first()))
    }

    @Test
    fun `라운드가 없는 팀 모드는 자리를 매기지 않는다`() {
        val match = match(
            queue = Queue.TEAM_DEATHMATCH,
            players = listOf(
                line(Me, onMyTeam = true, acs = 300),
                line(Enemy, onMyTeam = false, acs = 200),
            ),
        )

        assertEquals(emptyMap(), match.placements())
        assertNull(match.myPlacement)
    }

    @Test
    fun `스코어보드에 내가 없으면 내 자리는 없다`() {
        assertNull(match(players = listOf(line(Enemy, onMyTeam = false, acs = 200))).myPlacement)
    }
}

// rounds 라운드를 뛰어 라운드당 acs를 낸 줄이다
private fun line(player: PlayerId, onMyTeam: Boolean, acs: Int, rounds: Int = 20, kills: Int = 10) = Scoreline(
    player = player,
    riotId = "${player.value}#KR1",
    agent = AgentId("agent"),
    onMyTeam = onMyTeam,
    tier = null,
    playerCard = null,
    kills = kills,
    deaths = 10,
    assists = 3,
    combatScore = acs * rounds,
    damage = 0,
    roundsPlayed = rounds,
)
