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

    // 데스매치는 팀이 없어서 팀 MVP를 붙이지 않는다
    @Test
    fun `라운드제가 아닌 모드에는 팀 MVP가 없다`() {
        val match = match(
            queue = Queue.OTHER,
            players = listOf(
                line(Me, onMyTeam = true, acs = 300),
                line(Enemy, onMyTeam = false, acs = 200),
            ),
        )

        assertEquals(MatchAward.MVP, match.placements()[Me]?.award)
        assertNull(match.placements()[Enemy]?.award)
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
