package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchFormatTest {

    @Test
    fun `모드와 팀 수로 경기 모양을 가른다`() {
        assertEquals(MatchFormat.ROUNDS, match(queue = Queue.SPIKE_RUSH).format)
        assertEquals(MatchFormat.TEAM_POINTS, match(queue = Queue.TEAM_DEATHMATCH).format)
        assertEquals(MatchFormat.FREE_FOR_ALL, match(queue = Queue.DEATHMATCH).format)
        assertEquals(MatchFormat.FREE_FOR_ALL, deathmatch().format)
        assertEquals(MatchFormat.TEAM_PLACEMENT, gauntlet().format)
    }

    @Test
    fun `데스매치는 모두가 혼자이고 응답의 점수로 등수를 매긴다`() {
        val match = deathmatch()

        assertEquals(listOf(1, 2, 3, 4), match.standings().map { it.rank })
        assertEquals(listOf(Enemy, OtherEnemy, Me, Ally), match.standings().map { it.members.single().player })
        assertEquals(Standing(rank = 3, teams = 4), match.myStanding)
    }

    @Test
    fun `데스매치 응답에 팀 줄이 없으면 스코어보드의 킬로 센다`() {
        val match = deathmatch().copy(teams = emptyList())

        assertEquals(Standing(rank = 3, teams = 4), match.myStanding)
    }

    @Test
    fun `점수가 같으면 같은 등수다`() {
        val match = deathmatch(points = listOf(30, 40, 30, 12))

        assertEquals(Standing(rank = 2, teams = 4), match.myStanding)
        assertEquals(listOf(1, 2, 2, 4), match.standings().map { it.rank })
    }

    // 건틀릿 응답이 등수를 어떻게 주는지는 아직 모른다. 등수가 오면 점수보다 먼저 쓴다.
    @Test
    fun `건틀릿은 두 사람씩 여덟 팀이 응답이 준 등수로 늘어선다`() {
        val match = gauntlet()
        val standings = match.standings()

        assertEquals((1..8).toList(), standings.map { it.rank })
        assertTrue(standings.all { it.members.size == 2 })
        assertEquals(setOf(Me, Ally), standings[1].members.map { it.player }.toSet())
        assertEquals(Standing(rank = 2, teams = 8), match.myStanding)
        assertEquals(emptyMap(), match.placements())
    }

    @Test
    fun `팀 데스매치 스코어는 라운드가 아니라 팀 점수다`() {
        val match = match(queue = Queue.TEAM_DEATHMATCH, players = listOf(line(Me, kills = 20), line(Enemy, kills = 18))).copy(
            teams = listOf(
                MatchTeam(members = setOf(Me, Ally), won = true, points = 100),
                MatchTeam(members = setOf(Enemy, OtherEnemy), won = false, points = 87),
            ),
        )

        assertEquals(Score(myTeam = 100, enemyTeam = 87), match.score)
        assertNull(match.myStanding)
    }

    @Test
    fun `두 팀이 라운드를 겨루는 모드에는 등수가 없다`() {
        val match = match(round(won = true), round(won = false), players = listOf(line(Me, kills = 20)))

        assertEquals(emptyList(), match.standings())
        assertNull(match.myStanding)
        assertEquals(Score(myTeam = 1, enemyTeam = 1), match.score)
    }

    // 데스매치 응답이 한 판을 한 라운드로 주면 경기 전체 점수가 라운드당 전투점수로 뜨고 관여율은 늘 100%가 된다
    @Test
    fun `라운드가 없는 모드는 스코어보드의 K_D_A와 맞힌 부위만 쓰고 라운드 값은 비운다`() {
        val oneRound = round(kill(10.0, Me, Enemy), damage = 3_000, shots = Shots(head = 9, body = 30, leg = 1))
        val me = line(Me, kills = 25, deaths = 20, assists = 3, shots = Shots(head = 9, body = 30, leg = 1))
        val metrics = match(oneRound, combatScore = 5_000, queue = Queue.DEATHMATCH, players = listOf(me)).metrics()

        assertEquals(25, metrics.kills)
        assertRate(1.25, metrics.kd)
        assertRate(1.4, metrics.kda)
        assertRate(9 / 40.0, metrics.headshotRate)
        assertNull(metrics.acs)
        assertNull(metrics.adr)
        assertNull(metrics.kast)
        assertNull(metrics.survivalRate)
        assertNull(metrics.firstDuelWinRate)
        assertNull(metrics.multiKillRate)
        assertNull(metrics.tradedDeathRate)
        assertNull(match(queue = Queue.DEATHMATCH, players = listOf(me)).acsOf(me))
    }
}

// 데스매치 네 사람이다. 점수는 Me, Ally, Enemy, OtherEnemy 순이고 기본값이면 Enemy가 1등, 내가 3등이다.
private fun deathmatch(points: List<Int> = listOf(30, 12, 40, 35)): Match {
    val people = listOf(Me, Ally, Enemy, OtherEnemy)
    val players = people.zip(points) { player, kills -> line(player, kills = kills, onMyTeam = player == Me) }
    return match(queue = Queue.DEATHMATCH, players = players).copy(
        allies = emptySet(),
        teams = people.zip(points) { player, kills -> MatchTeam(members = setOf(player), won = kills == points.max(), points = kills) },
    )
}

// 건틀릿 여덟 팀이다. 나와 Ally가 2등이고, 응답의 점수와 상관없이 등수를 그대로 쓰는지 보려고 점수는 거꾸로 준다.
private fun gauntlet(): Match {
    val others = List(12) { PlayerId("p$it") }
    val pairs = listOf(listOf(Enemy, OtherEnemy), listOf(Me, Ally)) + others.chunked(2)
    return match(queue = Queue.OTHER, players = pairs.flatten().map { line(it, kills = 5) }).copy(
        allies = setOf(Ally),
        teams = pairs.mapIndexed { index, pair ->
            MatchTeam(members = pair.toSet(), won = index == 0, points = index, placement = index + 1)
        },
    )
}

private fun line(
    player: PlayerId,
    kills: Int,
    deaths: Int = 10,
    assists: Int = 0,
    shots: Shots? = null,
    onMyTeam: Boolean = player == Me || player == Ally,
) = Scoreline(
    player = player,
    riotId = "${player.value}#KR1",
    agent = AgentId("agent"),
    onMyTeam = onMyTeam,
    tier = null,
    playerCard = null,
    kills = kills,
    deaths = deaths,
    assists = assists,
    combatScore = 5_000,
    damage = 3_000,
    roundsPlayed = 1,
    shots = shots,
)
