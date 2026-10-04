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
    val ecoRounds: Int,
    val ecoRoundsWon: Int,
    val forceBuyRounds: Int,
    val forceBuyRoundsWon: Int,
    val fullBuyRounds: Int,
    val fullBuyRoundsWon: Int,
    /** 내가 적을 둘 이상 잡은 라운드 수입니다. */
    val multiKillRounds: Int = 0,
    /** 우리 팀이 트레이드해 준 내 데스 수입니다. 관여율의 트레이드와 같은 기준입니다. */
    val tradedDeaths: Int = 0,
) {
    val acs: Double? get() = combatScore over rounds

    val adr: Double? get() = damage over rounds

    /** 데스가 없으면 `null`입니다. 킬 수를 그대로 K/D로 띄우면 실제보다 부풀려 보입니다. */
    val kd: Double? get() = kills over deaths

    /**
     * (킬 + 어시스트) ÷ 데스입니다. 데스가 없으면 `null`입니다. 킬과 어시를 더한 값이라 "평점"이라 부르지
     * 않습니다(CLAUDE.md 지켜야 할 선).
     */
    val kda: Double? get() = (kills + assists) over deaths

    /** 맞힌 탄 중 머리에 맞은 비율입니다. 킬 중 헤드샷 킬 비율이 아닙니다. */
    val headshotRate: Double? get() = shots.head over shots.total

    /** 관여율(KAST)입니다. 킬·어시스트·생존·트레이드 중 하나라도 있었던 라운드의 비율입니다. */
    val kast: Double? get() = kastRounds over rounds

    val survivalRate: Double? get() = survivedRounds over rounds

    val assistsPerRound: Double? get() = assists over rounds

    /** 내가 첫 킬을 낸 라운드 중 이긴 비율입니다. 우리 팀 누군가의 첫 킬은 세지 않습니다. */
    val firstKillWinRate: Double? get() = firstKillRoundsWon over firstKills

    /** 라운드 첫 교전에 내가 들어간 비율입니다. 첫 킬과 첫 데스를 모두 셉니다. */
    val firstDuelInvolvement: Double? get() = (firstKills + firstDeaths) over rounds

    /** 첫 교전에 들어갔을 때 내가 첫 킬을 낸 비율입니다. 라운드 승패와는 상관없습니다. */
    val firstDuelWinRate: Double? get() = firstKills over (firstKills + firstDeaths)

    /** 이코·포스바이·풀바이 승률은 [buyType]으로 가른 라운드 중 이긴 비율입니다. */
    val ecoWinRate: Double? get() = ecoRoundsWon over ecoRounds

    val forceBuyWinRate: Double? get() = forceBuyRoundsWon over forceBuyRounds

    val fullBuyWinRate: Double? get() = fullBuyRoundsWon over fullBuyRounds

    val multiKillRate: Double? get() = multiKillRounds over rounds

    /** 내 데스 중 [TRADE_WINDOW_MILLIS] 안에 우리 팀이 내 킬러를 잡은 비율입니다. */
    val tradedDeathRate: Double? get() = tradedDeaths over deaths

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
        ecoRounds = ecoRounds + other.ecoRounds,
        ecoRoundsWon = ecoRoundsWon + other.ecoRoundsWon,
        forceBuyRounds = forceBuyRounds + other.forceBuyRounds,
        forceBuyRoundsWon = forceBuyRoundsWon + other.forceBuyRoundsWon,
        fullBuyRounds = fullBuyRounds + other.fullBuyRounds,
        fullBuyRoundsWon = fullBuyRoundsWon + other.fullBuyRoundsWon,
        multiKillRounds = multiKillRounds + other.multiKillRounds,
        tradedDeaths = tradedDeaths + other.tradedDeaths,
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
            ecoRounds = 0,
            ecoRoundsWon = 0,
            forceBuyRounds = 0,
            forceBuyRoundsWon = 0,
            fullBuyRounds = 0,
            fullBuyRoundsWon = 0,
        )
    }
}

fun Iterable<MatchMetrics>.sum(): MatchMetrics = fold(MatchMetrics.Empty, MatchMetrics::plus)

internal infix fun Int.over(denominator: Int): Double? =
    if (denominator == 0) null else toDouble() / denominator
