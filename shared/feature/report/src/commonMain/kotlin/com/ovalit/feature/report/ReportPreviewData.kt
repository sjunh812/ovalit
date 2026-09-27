package com.ovalit.feature.report

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Insight
import com.ovalit.core.model.InsightMetric
import com.ovalit.core.model.InsightPart
import com.ovalit.core.model.InsightSubject
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.MovedAgent
import com.ovalit.core.model.MovedMetric
import com.ovalit.core.model.MovedWeapon
import com.ovalit.core.model.Movement
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Role
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.TrendWeek
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeaponMetric
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.model.WeekNote
import com.ovalit.core.model.WeeklyReport
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// 프리뷰와 UI 테스트가 같이 쓴다. 숫자를 바꾸면 ReportScreenTest의 기대값도 같이 바꿔야 한다.
internal object ReportPreviewData {

    private val thisWeek = MatchMetrics(
        matches = 7,
        rounds = 146,
        kills = 118,
        deaths = 88,
        assists = 30,
        combatScore = 27_188,
        damage = 20_108,
        shots = Shots(head = 92, body = 310, leg = 27),
        kastRounds = 126,
        survivedRounds = 58,
        firstKills = 37,
        firstDeaths = 26,
        firstKillRoundsWon = 24,
        ecoRounds = 14,
        ecoRoundsWon = 3,
        forceBuyRounds = 22,
        forceBuyRoundsWon = 7,
        fullBuyRounds = 96,
        fullBuyRoundsWon = 55,
    )

    private val lastFourWeeks = Baseline(
        metrics = MatchMetrics(
            matches = 28,
            rounds = 630,
            kills = 460,
            deaths = 360,
            assists = 130,
            combatScore = 107_913,
            damage = 80_373,
            shots = Shots(head = 380, body = 1_230, leg = 110),
            kastRounds = 546,
            survivedRounds = 271,
            firstKills = 100,
            firstDeaths = 80,
            firstKillRoundsWon = 62,
            ecoRounds = 60,
            ecoRoundsWon = 15,
            forceBuyRounds = 95,
            forceBuyRoundsWon = 35,
            fullBuyRounds = 420,
            fullBuyRoundsWon = 230,
        ),
        weeks = 4,
    )

    private val act = ActId("preview")
    private val previousAct = ActId("previous")

    // 7주 전부터 지난주까지다. 이번 주는 아래에서 따로 붙인다. 셋째 주는 라운드가 모자라 막대를 비운 주다.
    private val weeklyDamagePerRound = listOf(128, 131, null, 126, 135, 129, 133)

    private val trend = weeklyDamagePerRound.mapIndexed { index, adr ->
        TrendWeek(
            firstDay = LocalDate(2026, 8, 3).plus(index, DateTimeUnit.WEEK),
            act = act,
            metrics = adr?.let { lastWeekLike(damagePerRound = it, headshots = 80 + index * 3) },
            startsNewAct = false,
            inPeriod = false,
        )
    } + TrendWeek(LocalDate(2026, 9, 21), act, thisWeek, startsNewAct = false, inPeriod = true)

    private fun lastWeekLike(damagePerRound: Int, headshots: Int) = MatchMetrics(
        matches = 7,
        rounds = 150,
        kills = 115,
        deaths = 90,
        assists = 32,
        combatScore = 25_800 + damagePerRound * 10,
        damage = damagePerRound * 150,
        shots = Shots(head = headshots, body = 320, leg = 30),
        kastRounds = 128,
        survivedRounds = 62,
        firstKills = 26,
        firstDeaths = 22,
        firstKillRoundsWon = 17,
        ecoRounds = 15,
        ecoRoundsWon = 4,
        forceBuyRounds = 22,
        forceBuyRoundsWon = 8,
        fullBuyRounds = 100,
        fullBuyRoundsWon = 55,
    )

    // 타격대가 수비에서 첫 교전을 자주 졌다. 공격 71%, 수비 45%.
    // 수비 첫 교전 15승 18패(45%), 공격 22승 9패(71%)
    private val firstDuelBySide = Insight(
        metric = InsightMetric.FIRST_DUEL_WIN_RATE,
        weak = InsightPart(InsightSubject.OnSide(Side.DEFENSE), value = 15 / 33.0, matches = 7, rounds = 72),
        other = InsightPart(InsightSubject.OnSide(Side.ATTACK), value = 22 / 31.0, matches = 7, rounds = 74),
        isRolePriority = true,
    )

    private fun agentWeek(kills: Int, deaths: Int, assists: Int, matches: Int) =
        thisWeek.copy(matches = matches, kills = kills, deaths = deaths, assists = assists)

    // 이번 주 일곱 판이다. 요원 칸은 판 수가 적어 승패로 적고, 무기는 한 무기만 쓴 라운드가 20을 넘긴 것만 헤드샷을 띄운다.
    private val periodAgents = listOf(
        AgentStats(AgentId("add6443a-41bd-e414-f6ad-e58d267f4e95"), Role.DUELIST, matches = 4, wins = 3, decided = 4,
            metrics = agentWeek(kills = 70, deaths = 48, assists = 18, matches = 4)),
        AgentStats(AgentId("f94c3b30-42be-e959-889c-5aa313dba261"), Role.DUELIST, matches = 3, wins = 2, decided = 3,
            metrics = agentWeek(kills = 48, deaths = 40, assists = 12, matches = 3)),
    )
    private val periodWeapons = listOf(
        WeaponStats(WeaponId("9C82E19D-4575-0200-1A81-3EACF00CF872"), kills = 64, singleWeaponRounds = 42,
            shots = Shots(head = 60, body = 180, leg = 12), carriedRounds = 70, deaths = 40, assists = 15, damage = 9_800),
        WeaponStats(WeaponId("E336C6B8-418D-9340-D77F-7A9E4CFE0702"), kills = 21, singleWeaponRounds = 12,
            shots = Shots(head = 10, body = 40, leg = 5), carriedRounds = 18, deaths = 12, assists = 4, damage = 2_200),
    )

    val moved = WeeklyReport.Ready(
        act = act,
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 21), weeks = 1, includesThisWeek = true),
        metrics = thisWeek,
        baseline = lastFourWeeks,
        mainRole = Role.DUELIST,
        mainRoleShare = 0.78,
        dynamic = listOf(
            DynamicSlot(DynamicMetric.FIRST_DUEL_INVOLVEMENT, Movement.MOVED),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.MOVED),
            DynamicSlot(DynamicMetric.KAST, Movement.STEADY),
        ),
        insight = firstDuelBySide,
        trend = trend,
        results = listOf(true, false, true, true, false, true, true),
        agents = periodAgents,
        weapons = periodWeapons,
        note = WeekNote(
            moved = MovedMetric(FixedMetric.DAMAGE, current = 20_108 / 146.0, usual = 80_373 / 630.0),
            weapon = MovedWeapon(periodWeapons.first().weapon, WeaponMetric.DAMAGE_PER_ROUND, current = 140.0, usual = 118.0, rounds = 44),
            agents = periodAgents,
            agent = MovedAgent(periodAgents.first().agent, current = 146.0, usual = 124.0, matches = 4),
        ),
    )

    // 3주 전에 액트가 바뀌었다. 기간 앞 새 액트 주가 셋뿐이라 평소 범위를 말하지 않는다.
    val newAct = moved.copy(
        trend = trend.mapIndexed { index, week ->
            when {
                index < 4 -> week.copy(act = previousAct)
                index == 4 -> week.copy(startsNewAct = true)
                else -> week
            }
        },
    )

    val steady = moved.copy(
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 14), weeks = 2, includesThisWeek = true),
        dynamic = listOf(
            DynamicSlot(DynamicMetric.KAST, Movement.STEADY),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.STEADY),
            DynamicSlot(DynamicMetric.FIRST_KILL_WIN_RATE, Movement.STEADY),
        ),
    )

    val unknown = moved.copy(
        baseline = null,
        mainRole = null,
        mainRoleShare = null,
        dynamic = listOf(
            DynamicSlot(DynamicMetric.KAST, Movement.UNKNOWN),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.UNKNOWN),
            DynamicSlot(DynamicMetric.FIRST_KILL_WIN_RATE, Movement.UNKNOWN),
        ),
    )

    val lastWeek = moved.copy(
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 14), weeks = 1, includesThisWeek = false),
    )

    // 라운드 운영을 관심사로 골랐다. 관심사 셋이 앞에 늘 있고 그 뒤에 움직인 지표가 붙어 다섯 칸이 된다.
    val focused = moved.copy(
        dynamic = listOf(
            DynamicSlot(DynamicMetric.FORCE_BUY_WIN_RATE, Movement.MOVED),
            DynamicSlot(DynamicMetric.ECO_WIN_RATE, Movement.STEADY),
            DynamicSlot(DynamicMetric.FULL_BUY_WIN_RATE, Movement.UNKNOWN),
            DynamicSlot(DynamicMetric.FIRST_DUEL_INVOLVEMENT, Movement.MOVED),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.MOVED),
        ),
    )

    val otherQueue = moved.copy(dynamic = emptyList())

    val notEnough = WeeklyReport.NotEnoughMatches(played = 3)

    val nothingPlayed = WeeklyReport.NotEnoughMatches(played = 0)
}
