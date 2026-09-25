package com.ovalit.core.model

/**
 * 경기 하나, 또는 여러 경기를 합친 지표입니다.
 *
 * 총량만 들고 있고 비율은 그때그때 나눠서 냅니다. 경기마다 ACS를 먼저 구해 평균 내면
 * 13-2로 끝난 경기와 13-11로 끝난 경기가 같은 무게로 섞입니다. 총량을 [plus]로 더한 뒤
 * 나누면 라운드 수만큼 무게가 실립니다.
 *
 * 비율은 분모가 0이면 `null`입니다. 0으로 채우면 "헤드샷 0%"처럼 틀린 숫자가 뜹니다.
 */
data class MatchMetrics(
    val matches: Int,
    val rounds: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val combatScore: Int,
    val damage: Int,
    val shots: Shots,
    val kastRounds: Int,
    val survivedRounds: Int,
    val firstKills: Int,
    val firstDeaths: Int,
    val firstKillRoundsWon: Int,
) {
    /** 전투점수(ACS). 라운드당 전투 점수입니다. */
    val acs: Double? get() = combatScore over rounds

    /** 피해량(ADR). 라운드당 준 피해입니다. */
    val adr: Double? get() = damage over rounds

    /** 데스가 없으면 `null`입니다. 킬 수를 그대로 보여주면 한 판짜리 기록이 과장됩니다. */
    val kd: Double? get() = kills over deaths

    /** 맞힌 탄 중 머리에 맞은 비율입니다. 킬 중 헤드샷 킬 비율이 아닙니다. */
    val headshotRate: Double? get() = shots.head over shots.total

    /** 관여율(KAST). 킬·어시스트·생존·트레이드 중 하나라도 한 라운드의 비율입니다. */
    val kast: Double? get() = kastRounds over rounds

    val survivalRate: Double? get() = survivedRounds over rounds

    val assistsPerRound: Double? get() = assists over rounds

    /** 퍼블 승률. 내가 퍼블을 딴 라운드 중 이긴 비율입니다. 우리 팀 누군가의 퍼블은 세지 않습니다. */
    val firstKillWinRate: Double? get() = firstKillRoundsWon over firstKills

    /** 퍼블 관여율. 라운드 첫 교전에 내가 들어간 비율입니다. 퍼블과 퍼데를 모두 셉니다. */
    val firstDuelInvolvement: Double? get() = (firstKills + firstDeaths) over rounds

    /** 첫 교전 승률. 첫 교전에 들어갔을 때 내가 퍼블을 딴 비율입니다. 라운드 승패와는 무관합니다. */
    val firstDuelWinRate: Double? get() = firstKills over (firstKills + firstDeaths)

    operator fun plus(other: MatchMetrics) = MatchMetrics(
        matches = matches + other.matches,
        rounds = rounds + other.rounds,
        kills = kills + other.kills,
        deaths = deaths + other.deaths,
        assists = assists + other.assists,
        combatScore = combatScore + other.combatScore,
        damage = damage + other.damage,
        shots = shots + other.shots,
        kastRounds = kastRounds + other.kastRounds,
        survivedRounds = survivedRounds + other.survivedRounds,
        firstKills = firstKills + other.firstKills,
        firstDeaths = firstDeaths + other.firstDeaths,
        firstKillRoundsWon = firstKillRoundsWon + other.firstKillRoundsWon,
    )

    companion object {
        val Empty = MatchMetrics(
            matches = 0,
            rounds = 0,
            kills = 0,
            deaths = 0,
            assists = 0,
            combatScore = 0,
            damage = 0,
            shots = Shots.None,
            kastRounds = 0,
            survivedRounds = 0,
            firstKills = 0,
            firstDeaths = 0,
            firstKillRoundsWon = 0,
        )
    }
}

fun Iterable<MatchMetrics>.sum(): MatchMetrics = fold(MatchMetrics.Empty, MatchMetrics::plus)

internal infix fun Int.over(denominator: Int): Double? =
    if (denominator == 0) null else toDouble() / denominator
