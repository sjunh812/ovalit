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
import com.ovalit.core.model.WeeklyReport
import kotlinx.datetime.LocalDate

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
        ),
        weeks = 4,
    )

    val moved = WeeklyReport.Ready(
        act = ActId("preview"),
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 21), weeks = 1),
        metrics = thisWeek,
        baseline = lastFourWeeks,
        mainRole = Role.DUELIST,
        dynamic = listOf(
            DynamicSlot(DynamicMetric.FIRST_DUEL_INVOLVEMENT, Movement.MOVED),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.MOVED),
            DynamicSlot(DynamicMetric.KAST, Movement.STEADY),
        ),
    )

    val steady = moved.copy(
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 14), weeks = 2),
        dynamic = listOf(
            DynamicSlot(DynamicMetric.KAST, Movement.STEADY),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.STEADY),
            DynamicSlot(DynamicMetric.FIRST_KILL_WIN_RATE, Movement.STEADY),
        ),
    )

    val unknown = moved.copy(
        baseline = null,
        mainRole = null,
        dynamic = listOf(
            DynamicSlot(DynamicMetric.KAST, Movement.UNKNOWN),
            DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.UNKNOWN),
            DynamicSlot(DynamicMetric.FIRST_KILL_WIN_RATE, Movement.UNKNOWN),
        ),
    )

    val notEnough = WeeklyReport.NotEnoughMatches(played = 3)
}
