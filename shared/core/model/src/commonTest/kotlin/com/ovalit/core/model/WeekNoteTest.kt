package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekNoteTest {

    // 헤드샷 표준편차는 0.0076이라 기준선은 0.011이다. 21% → 30%는 표준편차의 12배다.
    @Test
    fun `평소 변동폭보다 크게 움직인 고정 지표를 짚는다`() {
        val note = note(current = fixed(head = 30))

        assertEquals(FixedMetric.HEADSHOT_RATE, note?.moved?.metric)
        assertTrue(note!!.moved!!.rose)
    }

    @Test
    fun `평소 범위 안에서 흔들린 지표는 짚지 않는다`() {
        assertNull(note(current = fixed(head = 22)))
    }

    // K/D 1.10 → 1.60은 표준편차 0.045의 11배, 헤드샷 21% → 26%는 6.6배다. 더 크게 움직인 K/D를 고른다.
    @Test
    fun `여럿이 움직이면 평소 변동폭에 견줘 가장 크게 움직인 지표를 고른다`() {
        val note = note(current = fixed(kills = 160, head = 26))

        assertEquals(FixedMetric.KD, note?.moved?.metric)
    }

    // K/D 1.10 → 1.25는 3.3배, 헤드샷 21% → 30%는 12배다. 화면 순서가 앞선 K/D가 아니라 헤드샷을 고른다.
    @Test
    fun `화면 순서가 아니라 움직인 크기로 고른다`() {
        val note = note(current = fixed(kills = 125, head = 30))

        assertEquals(FixedMetric.HEADSHOT_RATE, note?.moved?.metric)
    }

    // CLAUDE.md 역할군: 척후대와 전략가는 K/D를 크게 띄우지 않는다
    @Test
    fun `척후대는 K_D가 더 크게 움직여도 다른 지표를 짚는다`() {
        val note = note(current = fixed(kills = 160, head = 26), role = Role.INITIATOR)

        assertEquals(FixedMetric.HEADSHOT_RATE, note?.moved?.metric)
    }

    @Test
    fun `같은 지표가 같은 쪽으로 움직인 무기를 붙인다`() {
        val note = note(current = fixed(head = 30), weapons = listOf(vandal(head = 30), phantom(head = 18)))

        assertEquals(Vandal, note?.weapon?.weapon)
        assertEquals(WeaponMetric.HEADSHOT_RATE, note?.weapon?.metric)
    }

    // S6 위쪽 표는 두 표본을 다 넘겨야 기간 값을 띄운다. 한쪽만 넘긴 무기를 짚으면 S6에서 "이번 액트 기준"으로 떠서
    // 숫자가 맞지 않는다.
    @Test
    fun `들고 시작한 라운드가 모자란 무기는 헤드샷이 올라도 붙이지 않는다`() {
        val note = note(current = fixed(head = 30), weapons = listOf(trend(Vandal, head = 30, carriedRounds = 12)))

        assertNull(note?.weapon)
    }

    // 헤드샷이 올랐는데 팬텀만 크게 떨어졌다. 반대로 움직인 무기를 붙이면 "헤드샷이 올랐다"와 어긋난다.
    @Test
    fun `반대로 움직인 무기는 붙이지 않는다`() {
        val note = note(current = fixed(head = 30), weapons = listOf(phantom(head = 12)))

        assertNull(note?.weapon)
    }

    // 사용자 요청(2026-09-27): 크게 달라진 무기보다 전체 변화를 많이 끌어간 무기를 붙인다. 밴달은 25%p 올랐지만 20라운드,
    // 팬텀은 15%p 올랐지만 60라운드라 팬텀이 헤드샷을 더 끌어올렸다.
    @Test
    fun `무기는 비중과 차이를 곱해 가장 많이 끌어간 것을 붙인다`() {
        val note = note(
            current = fixed(head = 30),
            weapons = listOf(trend(Vandal, head = 46, singleRounds = 20), trend(Phantom, head = 36, singleRounds = 60)),
        )

        assertEquals(Phantom, note?.weapon?.weapon)
        assertEquals(60, note?.weapon?.rounds)
    }

    // 제트는 9%p 올랐지만 60라운드, 레이즈는 11%p 올랐지만 40라운드라 제트가 헤드샷을 더 끌어올렸다
    @Test
    fun `요원도 비중과 차이를 곱해 가장 많이 끌어간 것을 붙인다`() {
        val note = note(
            current = fixed(head = 30),
            agentTrends = listOf(agentTrend(Jett, rounds = 60, head = 30), agentTrend(Raze, rounds = 40, head = 32)),
        )

        assertEquals(Jett, note?.agent?.agent)
    }

    @Test
    fun `표본이 모자라거나 반대로 움직인 요원은 붙이지 않는다`() {
        val note = note(
            current = fixed(head = 30),
            agentTrends = listOf(agentTrend(Jett, rounds = 30, head = 40), agentTrend(Raze, rounds = 60, head = 15)),
        )

        assertNull(note?.agent)
    }

    @Test
    fun `전투점수에는 무기를 붙이지 않는다`() {
        val note = note(current = fixed(score = 26_000), weapons = listOf(vandal(head = 30)))

        assertEquals(FixedMetric.COMBAT_SCORE, note?.moved?.metric)
        assertNull(note?.weapon)
    }

    // Riot 정책: 결정을 없애지 말고 선택지를 여럿 준다. 한 요원만 적으면 골라 주는 것처럼 읽힌다.
    @Test
    fun `이긴 판이 더 많은 요원이 둘 이상일 때만 요원을 적는다`() {
        val two = note(current = fixed(head = 30), agents = listOf(agent(Sova, 2, 0), agent(Raze, 2, 1), agent(Jett, 1, 3)))
        val one = note(current = fixed(head = 30), agents = listOf(agent(Sova, 2, 0), agent(Jett, 1, 3)))

        assertEquals(listOf(Sova, Raze), two?.agents?.map { it.agent })
        assertEquals(emptyList(), one?.agents)
    }

    @Test
    fun `한 판만 이긴 요원은 잘 풀렸다고 하지 않는다`() {
        val note = note(current = fixed(head = 30), agents = listOf(agent(Sova, 1, 0), agent(Raze, 1, 0)))

        assertEquals(emptyList(), note?.agents)
    }

    @Test
    fun `움직인 지표가 없어도 잘 풀린 요원이 둘 이상이면 짚는다`() {
        val note = note(current = Usual, agents = listOf(agent(Sova, 3, 0), agent(Raze, 2, 1)))

        assertNull(note?.moved)
        assertEquals(listOf(Sova, Raze), note?.agents?.map { it.agent })
    }

    @Test
    fun `비교할 기록이 없으면 지표를 짚지 않는다`() {
        assertNull(note(current = fixed(head = 30), baseline = null))
    }

    private fun note(
        current: MatchMetrics,
        baseline: MatchMetrics? = Usual,
        role: Role? = Role.DUELIST,
        weapons: List<WeaponTrend> = emptyList(),
        agents: List<AgentStats> = emptyList(),
        agentTrends: List<AgentTrend> = emptyList(),
    ) = chooseWeekNote(
        current = current,
        baseline = baseline,
        history = UsualWeeks,
        role = role,
        weapons = weapons,
        agents = agents,
        agentTrends = agentTrends,
    )

    private companion object {
        val Vandal = WeaponId("vandal")
        val Phantom = WeaponId("phantom")
        val Sova = AgentId("sova")
        val Raze = AgentId("raze")
        val Jett = AgentId("jett")

        // 100라운드, K/D 1.10, 전투점수 200, 피해량 140, 헤드샷 21%
        fun fixed(kills: Int = 110, score: Int = 20_000, damage: Int = 14_000, head: Int = 21) = MatchMetrics.Empty.copy(
            matches = 5,
            rounds = 100,
            kills = kills,
            deaths = 100,
            combatScore = score,
            damage = damage,
            shots = Shots(head = head, body = 100 - head, leg = 0),
        )

        val Usual = fixed()

        // 지표마다 평소 값 위아래로 조금씩 흔들린다. K/D 표준편차는 0.045, 헤드샷은 0.0076이다.
        val UsualWeeks = listOf(-1, 1, 0, 0, -1, 1, 0, 0).map { d ->
            fixed(kills = 110 + 6 * d, score = 20_000 + 400 * d, damage = 14_000 + 300 * d, head = 21 + d)
        }

        fun weapon(id: WeaponId, head: Int, carriedRounds: Int = 40, singleRounds: Int = 40) = WeaponStats(
            weapon = id,
            kills = 40,
            singleWeaponRounds = singleRounds,
            shots = Shots(head = head, body = 100 - head, leg = 0),
            carriedRounds = carriedRounds,
            deaths = 30,
            damage = carriedRounds * 140,
        )

        fun trend(id: WeaponId, head: Int, carriedRounds: Int = 40, singleRounds: Int = 40) = WeaponTrend(
            weapon = id,
            current = weapon(id, head, carriedRounds, singleRounds),
            baseline = weapon(id, 21),
        )

        fun vandal(head: Int) = trend(Vandal, head)

        fun phantom(head: Int) = trend(Phantom, head)

        // 평소 헤드샷은 21%다
        fun agentTrend(id: AgentId, rounds: Int, head: Int) = AgentTrend(
            agent = id,
            current = fixed(head = head).copy(rounds = rounds, matches = rounds / 20),
            baseline = fixed().copy(rounds = 80),
        )

        fun agent(id: AgentId, wins: Int, losses: Int) = AgentStats(
            agent = id,
            role = Role.DUELIST,
            matches = wins + losses,
            wins = wins,
            decided = wins + losses,
            metrics = MatchMetrics.Empty,
        )
    }
}
