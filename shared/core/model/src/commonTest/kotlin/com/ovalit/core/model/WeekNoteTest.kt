package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class WeekNoteTest {

    // 헤드샷 표준편차는 0.0076이라 기준선은 0.011이다. 21% → 30%는 표준편차의 12배다.
    @Test
    fun `평소 변동폭보다 크게 움직인 고정 지표를 짚는다`() {
        val note = note(current = fixed(head = 30))

        assertEquals(FixedMetric.HEADSHOT_RATE, note?.moved?.metric)
        assertTrue(note!!.moved.rose)
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

    // 풀바이 피해량은 150 그대로인데 이코 라운드가 16%에서 31%로 늘어 전체가 140에서 131로 떨어졌다. 비중을 평소처럼 맞추면
    // 140이라 떨어진 몫이 모두 비중에서 왔다.
    @Test
    fun `변화의 절반 이상이 비중에서 왔으면 무기와 요원 대신 비중을 적는다`() {
        val note = note(
            current = fixed(damage = 13_050),
            agentTrends = listOf(agentTrend(Jett, rounds = 60, head = 21, damage = 7_000)),
            mixes = listOf(ecoGrew()),
        )

        assertEquals(MixGroup.Buy(BuyType.ECO), note?.mix?.group)
        assertEquals(0.31, note?.mix?.share)
        assertEquals(0.16, note?.mix?.usualShare)
        // 비중을 모르면 제트가 피해량을 끌어내린 것으로 적혔다
        assertNull(note?.agent)
    }

    @Test
    fun `비중에 휘둘리지 않은 묶음은 짚은 묶음을 빼고 가장 많이 한 것이다`() {
        val steady = note(current = fixed(damage = 13_050), mixes = listOf(ecoGrew()))?.mix?.steady

        assertEquals(MixGroup.Buy(BuyType.FULL_BUY), steady?.group)
        assertEquals(150.0, steady?.current)
        assertEquals(150.0, steady?.usual)
    }

    // 비중은 평소와 같고 풀바이와 이코 모두 피해량이 떨어졌다. 실력이 달라진 것이라 무기와 요원을 적는다.
    @Test
    fun `비중이 그대로면 비중을 적지 않는다`() {
        val slices = listOf(
            slice(MixGroup.Buy(BuyType.FULL_BUY), current = roundsOf(84, damage = 11_760), usual = roundsOf(336, damage = 50_400)),
            slice(MixGroup.Buy(BuyType.ECO), current = roundsOf(16, damage = 1_200), usual = roundsOf(64, damage = 5_600)),
        )
        val note = note(current = fixed(damage = 12_960), agentTrends = listOf(agentTrend(Jett, rounds = 60, head = 21, damage = 7_000)), mixes = listOf(slices))

        assertNull(note?.mix)
        assertEquals(Jett, note?.agent?.agent)
    }

    // 이코 비중은 늘었지만 풀바이 피해량도 150에서 120으로 떨어졌다. 비중을 평소처럼 맞춰도 115라 떨어진 30 중 5만 비중
    // 몫이다. 실력이 달라진 게 더 크니 요원을 적는다.
    @Test
    fun `비중 몫이 변화의 절반에 못 미치면 비중을 적지 않는다`() {
        val slices = listOf(
            slice(MixGroup.Buy(BuyType.FULL_BUY), current = roundsOf(69, damage = 8_280), usual = roundsOf(336, damage = 50_400)),
            slice(MixGroup.Buy(BuyType.ECO), current = roundsOf(31, damage = 2_700), usual = roundsOf(64, damage = 5_600)),
        )
        val note = note(current = fixed(damage = 10_980), agentTrends = listOf(agentTrend(Jett, rounds = 60, head = 21, damage = 7_000)), mixes = listOf(slices))

        assertNull(note?.mix)
        assertEquals(Jett, note?.agent?.agent)
    }

    // 풀바이 비중이 68%에서 52%로 줄어 피해량이 떨어졌다. 풀바이만 보면은 풀바이가 줄었다는 말을 되풀이할 뿐이라 그다음으로
    // 많이 한 포스바이를 적는다.
    @Test
    fun `짚은 묶음이 가장 많이 한 묶음이어도 비교 묶음으로는 쓰지 않는다`() {
        val slices = listOf(
            slice(MixGroup.Buy(BuyType.FULL_BUY), current = roundsOf(104, damage = 15_600), usual = roundsOf(272, damage = 40_800)),
            slice(MixGroup.Buy(BuyType.FORCE_BUY), current = roundsOf(48, damage = 5_280), usual = roundsOf(64, damage = 7_040)),
            slice(MixGroup.Buy(BuyType.ECO), current = roundsOf(48, damage = 3_840), usual = roundsOf(64, damage = 5_120)),
        )
        val mix = note(current = fixed(damage = 12_360), baseline = fixed(damage = 13_240), mixes = listOf(slices))?.mix

        assertEquals(MixGroup.Buy(BuyType.FULL_BUY), mix?.group)
        assertEquals(MixGroup.Buy(BuyType.FORCE_BUY), mix?.steady?.group)
        assertEquals(110.0, mix?.steady?.current)
    }

    // 피해량이 떨어진 건 피해량 0인 이코가 10%에서 14%로 는 탓인데, 4%p라 짚을 만큼은 아니다. 뚜렷하게 바뀐 건 포스바이가
    // 줄어든 것뿐인데 포스바이는 평균보다 낮아서 줄면 오히려 값을 올린다. 거꾸로 짚지 않는다.
    @Test
    fun `변화와 반대로 움직인 비중은 짚지 않는다`() {
        val slices = listOf(
            slice(MixGroup.Buy(BuyType.FULL_BUY), current = roundsOf(620, damage = 93_000), usual = roundsOf(600, damage = 90_000)),
            slice(MixGroup.Buy(BuyType.FORCE_BUY), current = roundsOf(236, damage = 23_600), usual = roundsOf(300, damage = 30_000)),
            slice(MixGroup.Buy(BuyType.ECO), current = roundsOf(144), usual = roundsOf(100)),
        )

        assertNull(note(current = fixed(damage = 13_400), mixes = listOf(slices))?.mix)
    }

    // 여덟 판 중 레이즈가 한 판이라 비중이 25%에서 12.5%로 떨어졌지만 한 판 차이는 우연으로 흔히 나온다
    @Test
    fun `비중 차이가 우연 범위면 비중을 적지 않는다`() {
        val slices = listOf(
            slice(MixGroup.Agent(Raze), current = matchesOf(1, kills = 40, deaths = 20), usual = matchesOf(8, kills = 160, deaths = 80)),
            slice(MixGroup.Agent(Jett), current = matchesOf(7, kills = 55, deaths = 80), usual = matchesOf(24, kills = 280, deaths = 320)),
        )

        assertNull(note(current = fixed(kills = 95), mixes = listOf(slices))?.mix)
    }

    // 요원은 판으로 비중을 센다. 전투점수는 라운드로 가를 수 없어서 요원으로만 본다. 지난 기간에만 뛴 세이지는 이번 기간
    // 성적이 없어 비중을 맞출 수 없으니 다시 섞을 때 뺀다.
    @Test
    fun `전투점수는 요원 비중으로 짚는다`() {
        val slices = listOf(
            slice(MixGroup.Agent(Omen), current = matchesOf(6, score = 14_400), usual = matchesOf(4, score = 9_600)),
            slice(MixGroup.Agent(Jett), current = matchesOf(2, score = 5_760), usual = matchesOf(28, score = 80_640)),
            slice(MixGroup.Agent(Sage), current = MatchMetrics.Empty, usual = matchesOf(4, score = 9_600)),
        )
        val note = note(current = fixed(score = 18_000), mixes = listOf(slices))

        assertEquals(FixedMetric.COMBAT_SCORE, note?.moved?.metric)
        assertEquals(MixGroup.Agent(Omen), note?.mix?.group)
        assertEquals(0.75, note?.mix?.share)
    }

    // 헤드샷 5%인 오퍼레이터 비중이 6%에서 40%로 늘었다. 밴달은 22% 그대로다. 밴달 값은 들고 시작한 라운드로 잰 22%가
    // 아니라 S6과 같은 표본의 30%를 적는다.
    @Test
    fun `무기 비중이 헤드샷을 끌어내렸으면 무기 비중을 적고 S6과 같은 값으로 견준다`() {
        val slices = listOf(
            slice(MixGroup.Weapon(Operator), current = roundsOf(40, head = 2), usual = roundsOf(24, head = 1)),
            slice(MixGroup.Weapon(Vandal), current = roundsOf(60, head = 13), usual = roundsOf(376, head = 83)),
        )
        val note = note(current = fixed(head = 15), weapons = listOf(trend(Vandal, head = 30)), mixes = listOf(slices))

        assertEquals(MixGroup.Weapon(Operator), note?.mix?.group)
        assertEquals(MixGroup.Weapon(Vandal), note?.mix?.steady?.group)
        assertEquals(0.30, note?.mix?.steady?.current)
        assertEquals(0.21, note?.mix?.steady?.usual)
    }

    // 사용자 요청(2026-09-27): Strava와 Riot 13.06 Accolades처럼 내 과거 기록 가운데 최고를 짚는다. 평소 주들은 20~22%다.
    @Test
    fun `이번 액트 어느 주보다 높으면 이전 최고를 붙인다`() {
        assertEquals(0.22, note(current = fixed(head = 30), actWeeks = UsualWeeks)?.previousBest)
    }

    @Test
    fun `앞선 주 최고보다 낮거나 떨어졌으면 최고를 붙이지 않는다`() {
        val hot = UsualWeeks + fixed(head = 31)

        assertNull(note(current = fixed(head = 30), actWeeks = hot)?.previousBest)
        assertNull(note(current = fixed(head = 14), actWeeks = UsualWeeks)?.previousBest)
    }

    // 평소(35%)에는 라운드가 모자라 막대에서 빠진 주가 섞여 앞선 주들(20~22%)보다 높다. 30%는 그 주들보다 높지만 평소보다
    // 떨어졌다. 떨어진 달에 최고라고 적으면 헤드라인과 어긋난다.
    @Test
    fun `떨어졌으면 앞선 주들보다 높아도 최고를 붙이지 않는다`() {
        val note = note(current = fixed(head = 30), baseline = fixed(head = 35), actWeeks = UsualWeeks)

        assertEquals(false, note?.moved?.rose)
        assertNull(note?.previousBest)
    }

    // 세 주 중 최고는 최고라고 부를 만하지 않다. 라운드가 모자란 주는 S1-a 막대처럼 뺀다.
    @Test
    fun `앞선 주가 넉 주에 못 미치면 최고를 붙이지 않는다`() {
        val short = fixed(head = 35).copy(rounds = 30)

        assertNull(note(current = fixed(head = 30), actWeeks = UsualWeeks.take(3))?.previousBest)
        assertEquals(0.22, note(current = fixed(head = 30), actWeeks = UsualWeeks + short)?.previousBest)
    }

    // 두 주를 합친 값을 한 주 값들과 견주면 주간 최고라고 할 수 없다
    @Test
    fun `리포트 기간이 한 주일 때만 주간 최고를 본다`() {
        val wednesday = Instant.parse("2026-09-23T12:00:00Z")
        fun week(weeksBefore: Int, games: Int, head: Int) = List(games) { game ->
            match(
                *Array(10) { round(shots = Shots(head = head, body = 10 - head, leg = 0)) },
                startedAt = wednesday - (weeksBefore * 7).days + game.hours,
            )
        }
        val usual = listOf(2, 3, 2, 3, 2).mapIndexed { index, head -> week(weeksBefore = index + 1, games = 5, head = head) }.flatten()
        fun note(matches: List<Match>) =
            (matches.weeklyReport(now = wednesday + 1.hours, timeZone = TimeZone.UTC) as WeeklyReport.Ready).note

        assertEquals(0.3, note(usual + week(weeksBefore = 0, games = 5, head = 5))?.previousBest)
        // 이번 주 세 판뿐이라 지난주까지 넓힌다
        val twoWeeks = usual.drop(5) + week(weeksBefore = 1, games = 5, head = 5) + week(weeksBefore = 0, games = 3, head = 5)
        val wide = assertNotNull(note(twoWeeks))
        assertEquals(FixedMetric.HEADSHOT_RATE, wide.moved.metric)
        assertNull(wide.previousBest)
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
        agentTrends: List<AgentTrend> = emptyList(),
        mixes: List<List<MixSlice>> = emptyList(),
        actWeeks: List<MatchMetrics> = emptyList(),
    ) = chooseWeekNote(
        current = current,
        baseline = baseline,
        history = UsualWeeks,
        role = role,
        weapons = weapons,
        agentTrends = agentTrends,
        mixes = mixes,
        actWeeks = actWeeks,
    )

    private companion object {
        val Vandal = WeaponId("vandal")
        val Phantom = WeaponId("phantom")
        val Operator = WeaponId("operator")
        val Raze = AgentId("raze")
        val Jett = AgentId("jett")
        val Omen = AgentId("omen")
        val Sage = AgentId("sage")

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
        fun agentTrend(id: AgentId, rounds: Int, head: Int, damage: Int = 14_000) = AgentTrend(
            agent = id,
            current = fixed(head = head, damage = damage).copy(rounds = rounds, matches = rounds / 20),
            baseline = fixed().copy(rounds = 80),
        )

        fun slice(group: MixGroup, current: MatchMetrics, usual: MatchMetrics) = MixSlice(group, current, usual)

        fun roundsOf(rounds: Int, damage: Int = 0, head: Int = 0) = MatchMetrics.Empty.copy(
            matches = 1,
            rounds = rounds,
            damage = damage,
            shots = Shots(head = head, body = rounds - head, leg = 0),
        )

        // 한 판 24라운드
        fun matchesOf(matches: Int, kills: Int = 0, deaths: Int = 0, score: Int = 0) = MatchMetrics.Empty.copy(
            matches = matches,
            rounds = matches * 24,
            kills = kills,
            deaths = deaths,
            combatScore = score,
        )

        // 풀바이 피해량은 두 기간 모두 150, 이코는 87.5다. 이코 비중만 16%에서 31%로 늘었다.
        fun ecoGrew() = listOf(
            slice(MixGroup.Buy(BuyType.FULL_BUY), current = roundsOf(69, damage = 10_350), usual = roundsOf(336, damage = 50_400)),
            slice(MixGroup.Buy(BuyType.ECO), current = roundsOf(31, damage = 2_700), usual = roundsOf(64, damage = 5_600)),
        )
    }
}
