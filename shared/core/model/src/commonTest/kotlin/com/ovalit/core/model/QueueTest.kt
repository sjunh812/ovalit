package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueTest {

    @Test
    fun `응답의 큐 ID를 큐로 옮긴다`() {
        val expected = mapOf(
            "competitive" to Queue.COMPETITIVE,
            "unrated" to Queue.UNRATED,
            "premier" to Queue.PREMIER,
            "spikerush" to Queue.SPIKE_RUSH,
            "swiftplay" to Queue.SWIFTPLAY,
            "onefa" to Queue.REPLICATION,
            "ggteam" to Queue.ESCALATION,
            "deathmatch" to Queue.DEATHMATCH,
            "hurm" to Queue.TEAM_DEATHMATCH,
            "snowball" to Queue.SNOWBALL_FIGHT,
        )

        expected.forEach { (id, queue) -> assertEquals(queue, Queue.fromRiot(id), id) }
    }

    // 새 맵 큐는 새 맵에서만 도는 일반전이다
    @Test
    fun `새 맵 큐는 일반으로 센다`() {
        assertEquals(Queue.UNRATED, Queue.fromRiot("newmap"))
    }

    // 13.06 건틀릿: 글리치처럼 큐 ID를 모르는 새 모드가 와도 앱이 죽지 않고 기타 목록에 뜬다
    @Test
    fun `모르는 큐 ID는 기타로 옮긴다`() {
        assertEquals(Queue.OTHER, Queue.fromRiot("gauntlet"))
        assertEquals(Queue.OTHER, Queue.fromRiot("something-new"))
        assertEquals(Queue.COMPETITIVE, Queue.fromRiot(" Competitive "))
    }

    @Test
    fun `커스텀 게임은 받지 않는다`() {
        assertNull(Queue.fromRiot(""))
        assertNull(Queue.fromRiot("custom"))
        assertNull(Queue.fromRiot("competitive", provisioningFlowId = "CustomGame"))
        assertEquals(Queue.COMPETITIVE, Queue.fromRiot("competitive", provisioningFlowId = "Matchmaking"))
    }

    // 스파이크 돌격은 4라운드 선취라 3라운드 뒤에, 신속 플레이와 복제는 5라운드 선취라 4라운드 뒤에 공수가 바뀐다
    @Test
    fun `전반 라운드 수는 모드마다 다르고 라운드가 없는 모드는 비운다`() {
        assertEquals(12, Queue.COMPETITIVE.halfRounds)
        assertEquals(12, Queue.UNRATED.halfRounds)
        assertEquals(12, Queue.PREMIER.halfRounds)
        assertEquals(3, Queue.SPIKE_RUSH.halfRounds)
        assertEquals(4, Queue.SWIFTPLAY.halfRounds)
        assertEquals(4, Queue.REPLICATION.halfRounds)
        listOf(Queue.DEATHMATCH, Queue.TEAM_DEATHMATCH, Queue.ESCALATION, Queue.SNOWBALL_FIGHT, Queue.OTHER).forEach {
            assertNull(it.halfRounds, it.name)
        }
    }

    @Test
    fun `이코노미는 경쟁전과 크레드 규칙이 같은 모드만 가른다`() {
        assertEquals(setOf(Queue.COMPETITIVE, Queue.UNRATED, Queue.PREMIER), Queue.entries.filter { it.hasEconomy }.toSet())
    }

    @Test
    fun `기타 칩은 경쟁과 일반 밖의 모든 모드를 띄우고 라운드제 모드만 센다`() {
        assertEquals(Queue.entries.toSet() - Queue.COMPETITIVE - Queue.UNRATED, QueueFilter.OTHER.queues)
        assertEquals(
            setOf(Queue.PREMIER, Queue.SPIKE_RUSH, Queue.SWIFTPLAY, Queue.REPLICATION),
            QueueFilter.OTHER.countedQueues,
        )
        assertEquals(QueueFilter.COMPETITIVE_AND_UNRATED.queues, QueueFilter.COMPETITIVE_AND_UNRATED.countedQueues)
    }

    // 프리미어는 규칙이 경쟁전과 같지만 티어가 걸리지 않는다. 경쟁 + 일반 리포트와 티어에 섞지 않는다.
    @Test
    fun `프리미어는 경쟁 + 일반과 티어에 넣지 않는다`() {
        val premier = match(queue = Queue.PREMIER, players = listOf(myLine(tier = 20)))

        assertFalse(Queue.PREMIER in QueueFilter.COMPETITIVE_AND_UNRATED.queues)
        assertTrue(Queue.PREMIER in QueueFilter.OTHER.countedQueues)
        assertNull(listOf(premier).latestTier())
    }
}

private fun myLine(tier: Int) = Scoreline(
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
)
