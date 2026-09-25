package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MatchScoreTest {

    @Test
    fun `스코어는 내가 튕겨서 못 뛴 라운드까지 센다`() {
        val played = List(20) { round(won = it < 11) }
        val game = match(*played.toTypedArray()).copy(roundOutcomes = List(22) { it < 13 })

        assertEquals(Score(myTeam = 13, enemyTeam = 9), game.score)
    }

    @Test
    fun `전반과 후반은 12라운드에서 나눈다`() {
        val outcomes = List(12) { it < 8 } + List(10) { it < 5 }
        val game = match().copy(roundOutcomes = outcomes)

        assertEquals(listOf(Score(8, 4), Score(5, 5)), game.halfScores)
    }

    @Test
    fun `24라운드를 넘기면 연장을 따로 센다`() {
        val outcomes = List(12) { it < 6 } + List(12) { it < 6 } + listOf(true, false, true, true)
        val game = match().copy(roundOutcomes = outcomes)

        assertEquals(listOf(Score(6, 6), Score(6, 6), Score(3, 1)), game.halfScores)
    }

    @Test
    fun `라운드제가 아닌 모드는 전후반을 나누지 않는다`() {
        val game = match(queue = Queue.OTHER).copy(roundOutcomes = listOf(true, false))

        assertEquals(emptyList(), game.halfScores)
    }

    // 일반전에도 티어가 실려 오지만 티어가 오르내리는 건 경쟁전뿐이다
    @Test
    fun `지금 티어는 가장 최근 경쟁전에서 읽는다`() {
        val matches = listOf(
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15),
            game(Queue.COMPETITIVE, startedAt = 2, tier = 16),
            game(Queue.UNRATED, startedAt = 3, tier = 18),
        )

        assertEquals(16, matches.latestTier())
    }

    // 마지막 경기가 일반전이어도 그때 고른 요원으로 아바타를 그린다
    @Test
    fun `아바타 요원은 큐와 상관없이 가장 최근 경기에서 읽는다`() {
        val matches = listOf(
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15, agent = "old"),
            game(Queue.UNRATED, startedAt = 2, tier = 15, agent = "new"),
        )

        assertEquals(AgentId("new"), matches.latestAgent())
    }
}

private fun game(queue: Queue, startedAt: Long, tier: Int?, agent: String = "agent") = match(
    agent = AgentId(agent),
    queue = queue,
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    players = listOf(
        Scoreline(
            player = Me,
            riotId = "나#KR1",
            agent = AgentId("agent"),
            onMyTeam = true,
            tier = tier,
            playerCard = null,
            kills = 0,
            deaths = 0,
            assists = 0,
            combatScore = 0,
            damage = 0,
            roundsPlayed = 0,
        ),
    ),
)
