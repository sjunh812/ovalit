package com.ovalit.core.network

import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RiotJsonTest {

    @Test
    fun `모르는 필드는 건너뛰고 null로 온 목록은 빈 목록으로 읽는다`() {
        val match = decodeMatch(
            """
            {
              "matchInfo": {"matchId": "m-1", "queueId": "competitive", "gameStartMillis": 1, "futureField": {"a": [1, 2]}},
              "players": null,
              "teams": null,
              "roundResults": [{"roundNum": 0, "playerStats": null, "brandNew": true}],
              "kills": []
            }
            """.trimIndent(),
        )

        assertTrue(match.players.isEmpty())
        assertTrue(match.teams.isEmpty())
        assertTrue(match.roundResults.single().playerStats.isEmpty())
    }

    @Test
    fun `읽지 못하면 어느 필드인지만 알리고 응답 원문은 담지 않는다`() {
        val secret = "p-secret-puuid"
        val error = assertFailsWith<RiotResponseFormatException> {
            decodeMatch("""{"players": [{"puuid": "$secret", "gameName": "비밀", "stats": {"kills": "many"}}]}""")
        }

        assertEquals("$.players[0].stats.kills", error.path)
        assertFalse(secret in error.message.orEmpty())
        assertFalse("비밀" in error.message.orEmpty())
        assertNull(error.cause)
    }

    @Test
    fun `JSON이 아니어도 원문을 담지 않는다`() {
        val error = assertFailsWith<RiotResponseFormatException> { decodeMatch("<html>p-secret-puuid</html>") }

        assertFalse("p-secret-puuid" in error.message.orEmpty())
    }

    @Test
    fun `경기 ID가 빠진 경기 상세는 저장할 수 없어 던진다`() {
        val error = assertFailsWith<RiotResponseFormatException> {
            decodeMatch("""{"matchInfo": {"queueId": "competitive", "gameStartMillis": 1}}""").toMatch(PlayerId(ME)) { null }
        }

        assertEquals("$.matchInfo.matchId", error.path)
    }

    @Test
    fun `경기 ID 목록에서 커스텀 게임을 빼고 큐와 시작 시각을 옮긴다`() {
        val entries = decodeMatchlist(
            """
            {
              "puuid": "$ME",
              "history": [
                {"matchId": "m-1", "gameStartTimeMillis": 1789000000000, "queueId": "competitive"},
                {"matchId": "m-2", "gameStartTimeMillis": 1788990000000, "queueId": ""},
                {"matchId": "m-3", "gameStartTimeMillis": 1788980000000, "queueId": "deathmatch"},
                {"matchId": "m-4", "gameStartTimeMillis": 1788970000000, "queueId": "gauntletglitch"},
                {"matchId": "m-5", "gameStartTimeMillis": 1788960000000}
              ]
            }
            """.trimIndent(),
        ).toEntries()

        assertEquals(listOf("m-1", "m-3", "m-4", "m-5").map(::MatchId), entries.map { it.id })
        assertEquals(listOf(Queue.COMPETITIVE, Queue.DEATHMATCH, Queue.OTHER, null), entries.map { it.queue })
        assertEquals(Instant.fromEpochMilliseconds(1_789_000_000_000), entries.first().startedAt)
    }

    @Test
    fun `시작 시각이 빠진 줄이 있으면 8주 밖인지 몰라 던진다`() {
        val error = assertFailsWith<RiotResponseFormatException> {
            decodeMatchlist("""{"history": [{"matchId": "m-1", "queueId": "competitive"}]}""").toEntries()
        }

        assertEquals("$.history[0].gameStartTimeMillis", error.path)
    }
}
