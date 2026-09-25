package com.ovalit.core.ui

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Scoreline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val Me = PlayerId("me")

class PlayerBadgeTest {

    @Test
    fun `티어 이름은 카탈로그에서 번호로 찾는다`() {
        val badge = playerBadge("나#KR1", listOf(game(Queue.COMPETITIVE, tier = 16)), catalog(16 to "플래티넘 2"))

        assertEquals(16, badge.tier)
        assertEquals("플래티넘 2", badge.tierName)
        assertEquals(AgentId("agent"), badge.agent)
    }

    // 이름을 모르는 번호를 숫자 그대로 띄우지 않는다
    @Test
    fun `카탈로그에 없는 번호면 티어 이름을 비운다`() {
        assertNull(playerBadge("나#KR1", listOf(game(Queue.COMPETITIVE, tier = 28)), catalog()).tierName)
    }

    @Test
    fun `경쟁전을 안 뛰었으면 티어가 없다`() {
        val badge = playerBadge("나#KR1", listOf(game(Queue.UNRATED, tier = 16)), catalog(16 to "플래티넘 2"))

        assertNull(badge.tier)
        assertNull(badge.tierName)
    }
}

private fun catalog(vararg tiers: Pair<Int, String>) = ContentCatalog.Empty.copy(tiers = tiers.toMap())

private fun game(queue: Queue, tier: Int) = Match(
    id = MatchId("match"),
    queue = queue,
    act = ActId("act"),
    map = MapId("map"),
    startedAt = Instant.fromEpochMilliseconds(0),
    lengthMillis = 0,
    me = Me,
    myAgent = AgentId("agent"),
    myRole = null,
    allies = emptySet(),
    myCombatScore = 0,
    myTeamWon = null,
    roundOutcomes = emptyList(),
    rounds = emptyList(),
    players = listOf(
        Scoreline(Me, "나#KR1", AgentId("agent"), onMyTeam = true, tier = tier, playerCard = PlayerCardId("card"),
            kills = 0, deaths = 0, assists = 0, combatScore = 0, damage = 0, roundsPlayed = 0),
    ),
)
