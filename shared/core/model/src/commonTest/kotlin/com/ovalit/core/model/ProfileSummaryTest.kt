package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class ProfileSummaryTest {

    // 일반전 응답에도 티어가 있지만 티어가 오르내리는 건 경쟁전뿐이다
    @Test
    fun `지금 티어는 가장 최근 경쟁전에서 읽는다`() {
        val record = listOf(
            game(Queue.COMPETITIVE, startedAt = 4, tier = 16),
            game(Queue.UNRATED, startedAt = 5, tier = 20),
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15),
            game(Queue.COMPETITIVE, startedAt = 3, tier = 17),
        ).competitiveRecord()

        assertEquals(16, record?.currentTier)
    }

    @Test
    fun `가장 최근 경쟁전에 티어가 빠져 있으면 그 앞 판을 본다`() {
        val record = listOf(
            game(Queue.COMPETITIVE, startedAt = 1, tier = 15),
            game(Queue.COMPETITIVE, startedAt = 2, tier = null),
        ).competitiveRecord()

        assertEquals(15, record?.currentTier)
        assertEquals(2, record?.matches)
    }

    @Test
    fun `경쟁전 승률은 비긴 판을 빼고 센다`() {
        val record = listOf(
            game(Queue.COMPETITIVE, won = true),
            game(Queue.COMPETITIVE, won = true),
            game(Queue.COMPETITIVE, won = false),
            game(Queue.COMPETITIVE, won = null),
            game(Queue.UNRATED, won = false),
        ).competitiveRecord()!!

        assertEquals(4, record.matches)
        assertEquals(2, record.wins)
        assertEquals(1, record.losses)
        assertRate(2.0 / 3, record.winRate)
    }

    @Test
    fun `경쟁전을 안 뛰었으면 티어 머리를 두지 않는다`() {
        assertNull(listOf(game(Queue.UNRATED)).competitiveRecord())
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

    // 칩을 바꿀 때 머리와 티어 카드까지 바뀌면 칩이 어디까지 고르는지 알 수 없다
    @Test
    fun `프로필 칩은 통계부터 무기까지만 고르고 머리와 티어 카드는 경쟁 + 일반으로 센다`() {
        val matches = listOf(
            game(Queue.COMPETITIVE, startedAt = 1, kills = 2),
            game(Queue.COMPETITIVE, startedAt = 2, kills = 2),
            game(Queue.UNRATED, startedAt = 3, kills = 7),
        )

        val report = matches.profileReport(QueueFilter.UNRATED, now = Instant.fromEpochMilliseconds(4), timeZone = TimeZone.UTC)

        assertEquals(3, report.actMatches)
        assertEquals(2, report.competitive?.matches)
        assertEquals(1, report.agents.matches)
        assertEquals(7, report.summary.metrics.kills)
        assertEquals(1, report.weapons.matches)
    }

    // 지난 액트 일반전을 이번 액트처럼 띄우면 액트를 넘는 숫자가 섞인다
    @Test
    fun `고른 큐에 이번 액트 경기가 없으면 지난 액트 경기를 세지 않고 비운다`() {
        val matches = listOf(
            game(Queue.UNRATED, startedAt = 1, act = ActId("old")),
            game(Queue.COMPETITIVE, startedAt = 2, act = ActId("new")),
        )

        val report = matches.profileReport(QueueFilter.UNRATED, now = Instant.fromEpochMilliseconds(3), timeZone = TimeZone.UTC)

        assertTrue(report.isQueueEmpty)
        assertEquals(0, report.weapons.matches)
        assertEquals(1, report.actMatches)
    }

    @Test
    fun `경기가 없으면 최다 킬을 비운다`() {
        val summary = emptyList<Match>().profileSummary()

        assertNull(summary.mostKills)
        assertNull(emptyList<Match>().competitiveRecord())
    }
}

private fun game(
    queue: Queue,
    startedAt: Long = 0,
    act: ActId = ActId("act"),
    tier: Int? = 16,
    won: Boolean? = true,
    kills: Int = 0,
    lengthMinutes: Long = 0,
): Match {
    val rounds = List(kills) { index -> round(kill(atSeconds = 10.0 + index, killer = Me, victim = Enemy)) }
    return match(
        *rounds.toTypedArray(),
        queue = queue,
        act = act,
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
