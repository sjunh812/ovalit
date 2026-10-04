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
    multiKill: Double = 0.15,
    deaths: Int = 70,
    traded: Double = 0.30,
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
    multiKillRounds = (rounds * multiKill).roundToInt(),
    deaths = deaths,
    tradedDeaths = (deaths * traded).roundToInt(),
)

/** 지난 4주 평균입니다. 아래 8주의 가운데 값입니다. */
internal val Usual = stats()

/**
 * 지난 8주입니다. 모든 지표가 [Usual] 위아래로 조금씩 흔들립니다.
 * 관여율·생존율·어시·첫 교전 관여율·멀티킬 라운드의 표준편차는 0.0151이라 1.5배 기준선은 0.0227입니다.
 * 포스바이 승률의 표준편차는 0.0378, 트레이드 받은 데스 비율(데스 70번)은 0.0108입니다.
 */
internal val UsualWeeks = listOf(-1, 1, 0, 0, -1, 1, 0, 0).map { d ->
    stats(
        kast = 0.70 + 0.02 * d,
        survival = 0.30 + 0.02 * d,
        assistsPerRound = 0.40 + 0.02 * d,
        firstDeaths = 16 + 2 * d,
        firstKillWins = 12 + 2 * d,
        forceBuyWins = 8 + d,
        multiKill = 0.15 + 0.02 * d,
        traded = 0.30 + 0.02 * d,
    )
}

internal fun moved(metric: DynamicMetric) = DynamicSlot(metric, Movement.MOVED)

internal fun steady(metric: DynamicMetric) = DynamicSlot(metric, Movement.STEADY)

internal fun unknown(metric: DynamicMetric) = DynamicSlot(metric, Movement.UNKNOWN)
