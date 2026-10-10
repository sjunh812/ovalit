package com.ovalit.core.model

/**
 * 내가 죽은 뒤 이 시간 안에 우리 팀이 내 킬러를 잡으면 트레이드로 칩니다.
 *
 * 공식 값이 없어 흔히 쓰는 5초로 시작하고 실데이터를 보고 조정합니다. 바꾸면 KAST와 트레이드 받은 데스 비율이 통째로
 * 움직이니 CLAUDE.md도 같이 고칩니다.
 */
const val TRADE_WINDOW_MILLIS: Long = 5_000

/**
 * [side]를 주면 그 진영 라운드만 셉니다. 전투점수는 응답에 경기 합계로만 있어서 진영별 값에서는 0이니 [MatchMetrics.acs]를
 * 읽지 않습니다.
 *
 * 데스매치처럼 라운드가 없는 모드는 스코어보드의 K/D/A와 맞힌 부위만 담고 라운드는 0입니다. 그래서 전투점수, 피해량, 관여율 같은
 * 라운드 값은 모두 `null`입니다.
 */
fun Match.metrics(side: Side? = null): MatchMetrics = when {
    format != MatchFormat.ROUNDS -> if (side == null) scorelineMetrics() else MatchMetrics.Empty.copy(matches = 1)
    side == null -> metricsOf(rounds, combatScore = myCombatScore)
    else -> metricsOf(rounds.filter { it.mySide == side }, combatScore = 0)
}

// 라운드가 없는 모드는 라운드별 킬 기록이 어떻게 오는지 아직 몰라 응답의 K/D/A를 그대로 쓴다. 한 판을 한 라운드로 받아
// 나누면 경기 전체 점수가 라운드당 전투점수로 뜬다.
private fun Match.scorelineMetrics(): MatchMetrics {
    val line = myScoreline
    return MatchMetrics.Empty.copy(
        matches = 1,
        kills = line?.kills ?: 0,
        deaths = line?.deaths ?: 0,
        assists = line?.assists ?: 0,
        shots = line?.shots ?: rounds.fold(Shots.None) { acc, round -> acc + round.myShots },
    )
}

/**
 * [keep]에 드는 라운드만 셉니다. 이코 라운드나 밴달을 들고 시작한 라운드처럼 경기 안에서 라운드를 가를 때 씁니다. 전투점수는
 * 경기 합계뿐이라 0으로 두고, 드는 라운드가 없으면 경기 수도 0입니다.
 */
internal fun Match.roundMetrics(keep: (Round) -> Boolean): MatchMetrics {
    val kept = rounds.filter(keep)
    return metricsOf(kept, combatScore = 0).copy(matches = if (kept.isEmpty()) 0 else 1)
}

private fun Match.metricsOf(rounds: List<Round>, combatScore: Int): MatchMetrics {
    val perRound = rounds.map { it.analyze(me = me, allies = allies) }
    val byBuy = rounds.groupBy { it.buyType(queue) }
    fun played(buy: BuyType) = byBuy[buy].orEmpty().size
    fun won(buy: BuyType) = byBuy[buy].orEmpty().count { it.won }

    return MatchMetrics(
        matches = 1,
        rounds = rounds.size,
        kills = perRound.sumOf { it.kills },
        deaths = perRound.count { it.died },
        assists = perRound.sumOf { it.assists },
        combatScore = combatScore,
        damage = rounds.sumOf { it.myDamage },
        shots = rounds.fold(Shots.None) { acc, round -> acc + round.myShots },
        kastRounds = perRound.count { it.kast },
        survivedRounds = perRound.count { !it.died },
        firstKills = perRound.count { it.firstKill },
        firstDeaths = perRound.count { it.firstDeath },
        firstKillRoundsWon = perRound.count { it.firstKill && it.won },
        ecoRounds = played(BuyType.ECO),
        ecoRoundsWon = won(BuyType.ECO),
        forceBuyRounds = played(BuyType.FORCE_BUY),
        forceBuyRoundsWon = won(BuyType.FORCE_BUY),
        fullBuyRounds = played(BuyType.FULL_BUY),
        fullBuyRoundsWon = won(BuyType.FULL_BUY),
        // 킬은 적을 잡은 것만 센다(analyze). 스킬로 우리 팀을 죽인 건 멀티킬에도 들어가지 않는다.
        multiKillRounds = perRound.count { it.kills >= 2 },
        tradedDeaths = perRound.count { it.traded },
    )
}

internal class RoundResult(
    val won: Boolean,
    val kills: Int,
    val assists: Int,
    val died: Boolean,
    val kast: Boolean,
    val firstKill: Boolean,
    val firstDeath: Boolean,
    val traded: Boolean,
)

internal fun Round.analyze(me: PlayerId, allies: Set<PlayerId>): RoundResult {
    val myTeam = allies + me
    // 스킬로 자기를 죽이거나 같은 팀을 죽인 건 킬로 세지 않는다. 응답에는 둘 다 킬로 들어온다.
    val enemyKills = kills.filter { (it.killer in myTeam) != (it.victim in myTeam) }

    val myKills = enemyKills.count { it.killer == me }
    val myAssists = enemyKills.count { me in it.assistants }
    // 세이지 부활로 한 라운드에 두 번 죽을 수 있어서, 목록 순서가 아니라 시각으로 먼저 죽은 것을 고른다.
    val myDeath = kills.filter { it.victim == me }.minByOrNull { it.atMillis }
    val firstBlood = enemyKills.minByOrNull { it.atMillis }

    val traded = myDeath != null && enemyKills.any { revenge ->
        revenge.victim == myDeath.killer &&
            revenge.killer in allies &&
            revenge.atMillis - myDeath.atMillis in 0..TRADE_WINDOW_MILLIS
    }

    return RoundResult(
        won = won,
        kills = myKills,
        assists = myAssists,
        died = myDeath != null,
        kast = myKills > 0 || myAssists > 0 || myDeath == null || traded,
        firstKill = firstBlood?.killer == me,
        firstDeath = firstBlood?.victim == me,
        traded = traded,
    )
}
