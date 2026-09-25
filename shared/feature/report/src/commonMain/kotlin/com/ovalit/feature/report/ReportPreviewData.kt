package com.ovalit.feature.report

import com.ovalit.core.model.ActId
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Role
import com.ovalit.core.model.Shots
import com.ovalit.core.model.SideInsight
import com.ovalit.core.model.SideMetric
import com.ovalit.core.model.TrendWeek
import com.ovalit.core.model.WeeklyReport
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// 프리뷰와 UI 테스트가 같이 쓴다. 숫자는 가짜 저장소가 목요일 밤에 내놓는 값에 맞췄다.
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

    // 7주 전부터 이번 주까지. 셋째 주는 두 판만 뛰어 막대가 비어 있다.
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
    private val firstDuelBySide = SideInsight(
        metric = SideMetric.FIRST_DUEL_WIN_RATE,
        attack = thisWeek.copy(rounds = 74, firstKills = 22, firstDeaths = 9),
        defense = thisWeek.copy(rounds = 72, firstKills = 15, firstDeaths = 18),
        isRolePriority = true,
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
    )

    // 4주 전에 액트가 바뀌었다. 그 앞 주는 평소 범위에서 빠진다.
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

    val otherQueue = moved.copy(dynamic = emptyList())

    val notEnough = WeeklyReport.NotEnoughMatches(played = 3)

    val nothingPlayed = WeeklyReport.NotEnoughMatches(played = 0)
}
