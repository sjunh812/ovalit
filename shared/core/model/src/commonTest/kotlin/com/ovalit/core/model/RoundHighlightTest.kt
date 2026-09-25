package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoundHighlightTest {

    private val allies = setOf(Ally, OtherAlly)
    private val enemies = setOf(Enemy, OtherEnemy)

    private fun Round.highlight() = highlight(me = Me, allies = allies, enemies = enemies)

    @Test
    fun `상대를 모두 내가 잡으면 에이스다`() {
        val round = round(kill(10.0, Me, Enemy), kill(20.0, Me, OtherEnemy))

        assertTrue(round.highlight().ace)
    }

    @Test
    fun `한 명이라도 우리 팀이 잡았으면 에이스가 아니다`() {
        val round = round(kill(10.0, Me, Enemy), kill(20.0, Ally, OtherEnemy))

        assertFalse(round.highlight().ace)
    }

    // 응답에는 스킬로 우리 팀을 죽인 것도 킬로 들어온다. 그 킬로 상대 수를 채우면 안 된다.
    @Test
    fun `우리 팀을 죽인 킬은 에이스에 들어가지 않는다`() {
        val round = round(kill(10.0, Me, Enemy), kill(20.0, Me, Ally))

        assertFalse(round.highlight().ace)
    }

    @Test
    fun `우리 팀이 모두 죽고 나만 남으면 그때 살아 있던 상대 수로 클러치를 잰다`() {
        val round = round(
            kill(10.0, Enemy, Ally),
            kill(20.0, Me, Enemy),
            kill(30.0, OtherEnemy, OtherAlly),
            kill(40.0, Me, OtherEnemy),
            won = true,
        )

        assertEquals(Clutch(against = 1, won = true), round.highlight().clutch)
    }

    @Test
    fun `나만 남은 라운드를 지면 클러치 시도로만 남는다`() {
        val round = round(
            kill(10.0, Enemy, Ally),
            kill(20.0, OtherEnemy, OtherAlly),
            kill(30.0, Enemy, Me),
            won = false,
        )

        assertEquals(Clutch(against = 2, won = false), round.highlight().clutch)
    }

    @Test
    fun `우리 팀보다 내가 먼저 죽으면 클러치가 아니다`() {
        val round = round(
            kill(10.0, Enemy, Ally),
            kill(20.0, Enemy, Me),
            kill(30.0, OtherEnemy, OtherAlly),
            won = true,
        )

        assertNull(round.highlight().clutch)
    }

    // 목록 순서가 아니라 시각 순서로 본다. 응답의 킬 목록이 시각 순이라는 보장이 없다.
    @Test
    fun `클러치는 킬이 일어난 시각 순서로 가린다`() {
        val round = round(
            kill(30.0, Enemy, Me),
            kill(10.0, Enemy, Ally),
            kill(20.0, OtherEnemy, OtherAlly),
            won = false,
        )

        assertEquals(Clutch(against = 2, won = false), round.highlight().clutch)
    }

    @Test
    fun `상대를 다 잡은 뒤에 우리 팀이 모두 죽은 건 클러치가 아니다`() {
        val round = round(
            kill(10.0, Me, Enemy),
            kill(20.0, Me, OtherEnemy),
            kill(30.0, Ally, OtherAlly),
            kill(31.0, Ally, Ally),
        )

        assertNull(round.highlight().clutch)
    }

    @Test
    fun `처음부터 우리 팀이 나 혼자면 클러치로 치지 않는다`() {
        val round = round(kill(10.0, Me, Enemy), won = true)

        assertNull(round.highlight(me = Me, allies = emptySet(), enemies = enemies).clutch)
    }

    @Test
    fun `라운드제가 아닌 모드에는 에이스와 클러치가 없다`() {
        val deathmatch = match(round(kill(10.0, Me, Enemy)), queue = Queue.OTHER, players = scoreboard())

        assertTrue(deathmatch.highlights().isEmpty())
    }

    @Test
    fun `프로필에는 에이스 수와 클러치 성공 수와 시도 수를 센다`() {
        val ace = round(kill(10.0, Me, Enemy), kill(20.0, Me, OtherEnemy), number = 1)
        val clutchWon = round(kill(10.0, Enemy, Ally), kill(20.0, Enemy, OtherAlly), kill(30.0, Me, Enemy), number = 2)
        val clutchLost = round(
            kill(10.0, Enemy, Ally),
            kill(20.0, Enemy, OtherAlly),
            kill(30.0, Enemy, Me),
            won = false,
            number = 3,
        )

        val count = listOf(match(ace, clutchWon, clutchLost, players = scoreboard())).highlightCount()

        assertEquals(HighlightCount(aces = 1, clutches = 1, clutchAttempts = 2), count)
    }

    @Test
    fun `경기 상세 라운드 줄에 에이스와 클러치가 실린다`() {
        val summaries = match(
            round(kill(10.0, Me, Enemy), kill(20.0, Me, OtherEnemy), number = 1),
            players = scoreboard(),
        ).roundSummaries()

        assertEquals(true, summaries.single().highlight?.ace)
    }

    private fun scoreboard() = listOf(Me, Ally, OtherAlly, Enemy, OtherEnemy).map { player ->
        Scoreline(
            player = player,
            riotId = player.value,
            agent = AgentId("agent"),
            onMyTeam = player == Me || player == Ally || player == OtherAlly,
            tier = null,
            playerCard = null,
            kills = 0,
            deaths = 0,
            assists = 0,
            combatScore = 0,
            damage = 0,
            roundsPlayed = 1,
        )
    }
}
