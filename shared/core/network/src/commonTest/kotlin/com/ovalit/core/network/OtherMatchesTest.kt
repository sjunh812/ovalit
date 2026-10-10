package com.ovalit.core.network

import com.ovalit.core.model.AgentId
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Score
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.Standing
import com.ovalit.core.model.acsOf
import com.ovalit.core.model.buyType
import com.ovalit.core.model.halfScores
import com.ovalit.core.model.metrics
import com.ovalit.core.model.myStanding
import com.ovalit.core.model.standings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtherMatchesTest {

    private val noRoles: (AgentId) -> Role? = { null }

    @Test
    fun `스파이크 돌격은 1부터 오는 roundNum도 그대로 1부터 센다`() {
        val match = assertNotNull(decodeMatch(SpikeRushMatch).toMatch(PlayerId(ME), noRoles))

        assertEquals(Queue.SPIKE_RUSH, match.queue)
        assertEquals(MatchFormat.ROUNDS, match.format)
        assertEquals(listOf(1, 2, 3, 4, 5), match.rounds.map { it.number })
        assertEquals(listOf(Score(2, 1), Score(2, 0)), match.halfScores)
        assertEquals(RoundEnding.SPIKE_DETONATED, match.rounds.first().ending)
        // 크레드 규칙이 달라 이코·포스바이를 가르지 않는다
        assertTrue(match.rounds.all { it.buyType(match.queue) == null })
        assertEquals(setOf(PlayerId(A1)), match.allies)
    }

    @Test
    fun `데스매치는 라운드를 0으로 두고 모두를 상대로 본다`() {
        val match = assertNotNull(decodeMatch(DeathmatchMatch).toMatch(PlayerId(ME), noRoles))

        assertEquals(Queue.DEATHMATCH, match.queue)
        assertEquals(MatchFormat.FREE_FOR_ALL, match.format)
        assertTrue(match.allies.isEmpty())
        assertTrue(match.rounds.isEmpty())
        assertTrue(match.roundOutcomes.isEmpty())
        assertEquals(listOf(PlayerId(ME)), match.players.filter { it.onMyTeam }.map { it.player })
        // 응답의 roundsPlayed가 1이어도 나누지 않는다. 한 판 점수가 라운드당 전투점수로 뜬다.
        assertTrue(match.players.all { it.roundsPlayed == 0 })
        assertNull(match.myScoreline?.let(match::acsOf))
        assertEquals(false, match.myTeamWon)
    }

    @Test
    fun `데스매치 지표는 스코어보드 K-D-A와 damage에서 모은 맞힌 부위로 센다`() {
        val match = assertNotNull(decodeMatch(DeathmatchMatch).toMatch(PlayerId(ME), noRoles))
        val metrics = match.metrics()

        assertEquals(25, metrics.kills)
        assertEquals(30, metrics.deaths)
        assertEquals(0, metrics.rounds)
        assertEquals(Shots(head = 20, body = 60, leg = 5), metrics.shots)
        assertNull(metrics.acs)
    }

    @Test
    fun `데스매치 등수는 numPoints로 세고 같은 점수는 같은 등수다`() {
        val match = assertNotNull(decodeMatch(DeathmatchMatch).toMatch(PlayerId(ME), noRoles))

        assertEquals(listOf(1, 2, 3, 3), match.standings().map { it.rank })
        assertEquals(Standing(rank = 3, teams = 4), match.myStanding)
    }

    // 데스매치 플레이어 줄의 teamId가 문서처럼 PUUID로 오는지 모른다. 모두 같은 값으로 와도 서로 한편이 되면 안 된다.
    @Test
    fun `데스매치 플레이어 줄의 teamId가 모두 같아도 서로 상대로 보고 팀은 PUUID로 찾는다`() {
        val json = matchJson(
            queueId = "deathmatch",
            players = listOf(ME, DM1, DM2).map { puuid ->
                player(puuid, "Neutral", JETT, kills = if (puuid == DM1) 40 else 20, deaths = 20, assists = 0, score = 1_000, roundsPlayed = 1)
            },
            teams = listOf(team(ME, won = false, roundsWon = 0, points = 30), team(DM1, won = true, roundsWon = 1, points = 40), team(DM2, won = false, roundsWon = 0, points = 20)),
            rounds = emptyList(),
        )

        val match = assertNotNull(decodeMatch(json).toMatch(PlayerId(ME), noRoles))
        assertTrue(match.allies.isEmpty())
        assertEquals(listOf(PlayerId(ME)), match.players.filter { it.onMyTeam }.map { it.player })
        assertEquals(Standing(rank = 2, teams = 3), match.myStanding)
    }

    @Test
    fun `큐를 모르는 여덟 팀 모드는 기타로 옮기고 팀 점수로 등수를 센다`() {
        val match = assertNotNull(decodeMatch(GauntletMatch).toMatch(PlayerId(ME), noRoles))

        assertEquals(Queue.OTHER, match.queue)
        assertEquals(MatchFormat.TEAM_PLACEMENT, match.format)
        assertEquals(setOf(PlayerId("p-g1-b")), match.allies)
        assertEquals(Standing(rank = 2, teams = 8), match.myStanding)
        assertEquals(false, match.myTeamWon)
        assertNull(match.myRole)
        assertTrue(match.rounds.isEmpty())
        // 라운드 기록이 비었으면 맞힌 부위를 모른다
        assertNull(match.myScoreline?.shots)
    }

    @Test
    fun `커스텀 게임은 옮기지 않는다`() {
        val custom = matchJson(players = listOf(player(ME, "Blue", RAZE, 1, 1, 1, 100, 1)), teams = emptyList(), rounds = emptyList(), queueId = "", provisioningFlowId = "CustomGame")
        val customWithQueue = custom.replace("\"queueId\": \"\"", "\"queueId\": \"competitive\"")

        assertNull(decodeMatch(custom).toMatch(PlayerId(ME), noRoles))
        assertNull(decodeMatch(customWithQueue).toMatch(PlayerId(ME), noRoles))
    }

    @Test
    fun `끝나지 않은 경기와 내가 뛰지 않은 경기는 옮기지 않는다`() {
        val unfinished = SpikeRushMatch.replace("\"isCompleted\": true", "\"isCompleted\": false")

        assertNull(decodeMatch(unfinished).toMatch(PlayerId(ME), noRoles))
        assertNull(decodeMatch(SpikeRushMatch).toMatch(PlayerId("p-stranger"), noRoles))
        assertNull(decodeMatch(CompetitiveMatch).toMatch(PlayerId("p-observer"), noRoles))
    }

    @Test
    fun `서버가 가린 친구 경기도 친구 눈으로 옮긴다`() {
        val match = assertNotNull(decodeMatch(RedactedFriendMatch).toMatch(PlayerId(FRIEND), noRoles))

        assertEquals(setOf(PlayerId("anon-1")), match.allies)
        assertEquals(4, match.players.size)
        val hidden = match.players.first { it.player == PlayerId("anon-2") }
        assertEquals("", hidden.riotId)
        assertNull(hidden.playerCard)
        assertEquals("민석#KR1", match.myScoreline?.riotId)

        val metrics = match.metrics()
        assertEquals(3, metrics.kills)
        assertEquals(1, metrics.deaths)
        assertEquals(290, metrics.damage)
        assertEquals(2, metrics.firstKills)
        assertEquals(1, metrics.firstDeaths)
        assertEquals(Score(2, 1), match.score)
    }

    @Test
    fun `내가 안 뛴 친구 경기를 내 눈으로 옮기면 null이다`() {
        assertNull(decodeMatch(RedactedFriendMatch).toMatch(PlayerId(ME), noRoles))
    }

    @Test
    fun `항복한 라운드는 내 라운드에서 빼고 teams가 셌을 때만 스코어에 넣는다`() {
        fun surrendered(blueRoundsWon: Int) = matchJson(
            players = listOf(player(ME, "Blue", RAZE, 2, 0, 0, 500, 3), player(E1, "Red", JETT, 0, 2, 0, 100, 3)),
            teams = listOf(team("Blue", won = true, roundsWon = blueRoundsWon), team("Red", won = false, roundsWon = 0)),
            rounds = listOf(
                round(0, "Blue", "Attacker", "Eliminated", "Elimination", bluePlayers = listOf(ME), redPlayers = listOf(E1)),
                round(1, "Blue", "Attacker", "Eliminated", "Elimination", bluePlayers = listOf(ME), redPlayers = listOf(E1)),
                round(2, "Blue", "Attacker", "Surrendered", "Surrendered", bluePlayers = listOf(ME), redPlayers = listOf(E1)),
            ),
        )

        val notCounted = assertNotNull(decodeMatch(surrendered(blueRoundsWon = 2)).toMatch(PlayerId(ME), noRoles))
        assertEquals(Score(2, 0), notCounted.score)
        assertEquals(2, notCounted.rounds.size)
        assertEquals(1.0, notCounted.metrics().survivalRate)

        val counted = assertNotNull(decodeMatch(surrendered(blueRoundsWon = 3)).toMatch(PlayerId(ME), noRoles))
        assertEquals(Score(3, 0), counted.score)
        assertEquals(2, counted.rounds.size)
    }

    @Test
    fun `두 사람의 kills에 같은 킬이 겹쳐 와도 한 번만 센다`() {
        val sameKill = kill(9_000, E1, ME)
        val json = matchJson(
            players = listOf(player(ME, "Blue", RAZE, 0, 1, 0, 100, 1), player(E1, "Red", JETT, 1, 0, 0, 300, 1)),
            teams = listOf(team("Blue", won = false, roundsWon = 0), team("Red", won = true, roundsWon = 1)),
            rounds = listOf(
                round(
                    0, "Red", "Defender", "Eliminated", "Elimination", bluePlayers = listOf(ME), redPlayers = listOf(E1),
                    kills = mapOf(E1 to listOf(sameKill), ME to listOf(sameKill)),
                ),
            ),
        )

        val match = assertNotNull(decodeMatch(json).toMatch(PlayerId(ME), noRoles))
        assertEquals(1, match.metrics().deaths)
    }

    @Test
    fun `스파이크에 죽은 건 누구의 킬도 아니고 내 데스로만 센다`() {
        val json = matchJson(
            players = listOf(player(ME, "Blue", RAZE, 0, 1, 0, 100, 1), player(E1, "Red", JETT, 0, 0, 0, 300, 1)),
            teams = listOf(team("Blue", won = false, roundsWon = 0), team("Red", won = true, roundsWon = 1)),
            rounds = listOf(
                round(
                    0, "Red", "Attacker", "Bomb detonated", "Detonate", bluePlayers = listOf(ME), redPlayers = listOf(E1),
                    kills = mapOf(E1 to listOf(kill(95_000, E1, ME, damageType = "Bomb", damageItem = ""))),
                ),
            ),
        )

        val match = assertNotNull(decodeMatch(json).toMatch(PlayerId(ME), noRoles))
        val spike = match.rounds.single().kills.single()
        assertEquals(PlayerId(ME), spike.killer)
        assertNull(spike.weapon)
        assertEquals(1, match.metrics().deaths)
        assertEquals(0, match.metrics().firstDeaths)
        // 상대가 공격으로 이겼으니 나는 수비였다
        assertEquals(Side.DEFENSE, match.rounds.single().mySide)
    }

    @Test
    fun `roundNum 0이 있으면 1을 더하고 없으면 그대로 둔다`() {
        assertEquals(1, roundNumberOffset(listOf(0, 1, 2)))
        assertEquals(0, roundNumberOffset(listOf(1, 2, 3)))
    }
}
