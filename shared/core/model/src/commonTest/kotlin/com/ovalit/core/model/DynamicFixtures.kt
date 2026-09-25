package com.ovalit.core.model

import kotlin.math.roundToInt

internal fun stats(
    rounds: Int = 100,
    kast: Double = 0.70,
    survival: Double = 0.30,
    assistsPerRound: Double = 0.40,
    firstKills: Int = 20,
    firstDeaths: Int = 16,
    firstKillWins: Int = 12,
    forceBuyWins: Int = 8,
) = MatchMetrics.Empty.copy(
    matches = 5,
    rounds = rounds,
    kastRounds = (rounds * kast).roundToInt(),
    survivedRounds = (rounds * survival).roundToInt(),
    assists = (rounds * assistsPerRound).roundToInt(),
    firstKills = firstKills,
    firstDeaths = firstDeaths,
    firstKillRoundsWon = firstKillWins,
    forceBuyRounds = 20,
    forceBuyRoundsWon = forceBuyWins,
)

/** 지난 4주 평균. 아래 8주의 가운데 값이다. */
internal val Usual = stats()

/**
 * 지난 8주. 모든 지표가 [Usual] 위아래로 조금씩 흔들린다.
 * 관여율·생존율·어시·퍼블 관여율의 표준편차는 0.0151이라 1.5배 기준선은 0.0227이다.
 * 포스바이 승률의 표준편차는 0.0378이다.
 */
internal val UsualWeeks = listOf(-1, 1, 0, 0, -1, 1, 0, 0).map { d ->
    stats(
        kast = 0.70 + 0.02 * d,
        survival = 0.30 + 0.02 * d,
        assistsPerRound = 0.40 + 0.02 * d,
        firstDeaths = 16 + 2 * d,
        firstKillWins = 12 + 2 * d,
        forceBuyWins = 8 + d,
    )
}

internal fun moved(metric: DynamicMetric) = DynamicSlot(metric, Movement.MOVED)

internal fun steady(metric: DynamicMetric) = DynamicSlot(metric, Movement.STEADY)

internal fun unknown(metric: DynamicMetric) = DynamicSlot(metric, Movement.UNKNOWN)
