package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ProfileSummaryTest {

    // 일반전에도 티어가 실려 오지만 오르내리는 건 경쟁전뿐이다
    @Test
    fun `티어 흐름은 경쟁전만 치른 순서대로 잇는다`() {
        val summary = listOf(
            game(Queue.COMPETITIVE, startedAt = 3, tier = 17),
            game(Queue.UNRATED, startedAt = 2, tier = 20),
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15),
            game(Queue.COMPETITIVE, startedAt = 4, tier = 16),
        ).profileSummary()

        assertEquals(listOf(15, 17, 16), summary.competitive?.tiers)
        assertEquals(16, summary.competitive?.currentTier)
    }

    @Test
    fun `티어가 빠진 판은 흐름에서 건너뛴다`() {
        val summary = listOf(
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15),
            game(Queue.COMPETITIVE, startedAt = 2, tier = null),
        ).profileSummary()

        assertEquals(listOf(15), summary.competitive?.tiers)
        assertEquals(2, summary.competitive?.matches)
    }

    @Test
    fun `경쟁전 승률은 비긴 판을 빼고 센다`() {
        val record = listOf(
            game(Queue.COMPETITIVE, won = true),
            game(Queue.COMPETITIVE, won = true),
            game(Queue.COMPETITIVE, won = false),
            game(Queue.COMPETITIVE, won = null),
            game(Queue.UNRATED, won = false),
        ).profileSummary().competitive!!

        assertEquals(4, record.matches)
        assertEquals(2, record.wins)
        assertEquals(1, record.losses)
        assertRate(2.0 / 3, record.winRate)
    }

    @Test
    fun `경쟁전을 안 뛰었으면 티어 머리를 두지 않는다`() {
        assertNull(listOf(game(Queue.UNRATED)).profileSummary().competitive)
    }

    @Test
    fun `최다 킬은 한 경기 기준이고 플레이 시간은 경기 길이를 더한다`() {
        val summary = listOf(
            game(Queue.COMPETITIVE, kills = 3, lengthMinutes = 40),
            game(Queue.UNRATED, kills = 5, lengthMinutes = 35),
            game(Queue.COMPETITIVE, kills = 1, lengthMinutes = 30),
        ).profileSummary()

        assertEquals(5, summary.mostKills)
        assertEquals(9, summary.metrics.kills)
        assertEquals(105L * 60_000, summary.playTimeMillis)
    }

    @Test
    fun `경기가 없으면 최다 킬을 비운다`() {
        val summary = emptyList<Match>().profileSummary()

        assertNull(summary.mostKills)
        assertNull(summary.competitive)
    }
}

private fun game(
    queue: Queue,
    startedAt: Long = 0,
    tier: Int? = 16,
    won: Boolean? = true,
    kills: Int = 0,
    lengthMinutes: Long = 0,
): Match {
    val rounds = List(kills) { index -> round(kill(atSeconds = 10.0 + index, killer = Me, victim = Enemy)) }
    return match(
        *rounds.toTypedArray(),
        queue = queue,
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        won = won,
        players = listOf(
            Scoreline(
                player = Me,
                riotId = "나#KR1",
                agent = AgentId("agent"),
                onMyTeam = true,
                tier = tier,
                playerCard = null,
                kills = kills,
                deaths = 0,
                assists = 0,
                combatScore = 0,
                damage = 0,
                roundsPlayed = rounds.size,
            ),
        ),
    ).copy(lengthMillis = lengthMinutes * 60_000)
}
