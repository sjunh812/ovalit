package com.ovalit.core.model

/**
 * 내가 죽은 뒤 이 시간 안에 우리 팀이 내 킬러를 잡으면 트레이드로 칩니다.
 *
 * 공식 값이 없어 업계에서 흔히 쓰는 5초로 시작합니다. 동적 지표 기준선(표준편차 1.5배)처럼
 * 실데이터를 보고 조정할 값입니다. 바꾸면 KAST가 통째로 움직이니 CLAUDE.md도 같이 고칩니다.
 */
const val TRADE_WINDOW_MILLIS: Long = 5_000

/**
 * [side]를 주면 그 진영 라운드만 셉니다. 전투 점수는 응답에 경기 합계로만 있어서 진영별로는 0입니다.
 */
fun Match.metrics(side: Side? = null): MatchMetrics {
    val rounds = if (side == null) rounds else rounds.filter { it.mySide == side }
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
        combatScore = if (side == null) myCombatScore else 0,
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
    )
}

private class RoundResult(
    val won: Boolean,
    val kills: Int,
    val assists: Int,
    val died: Boolean,
    val kast: Boolean,
    val firstKill: Boolean,
    val firstDeath: Boolean,
)

private fun Round.analyze(me: PlayerId, allies: Set<PlayerId>): RoundResult {
    val myTeam = allies + me
    // 스킬로 자기를 죽이거나 같은 팀을 죽인 건 킬로 세지 않는다. 응답에는 둘 다 킬로 들어온다.
    val enemyKills = kills.filter { (it.killer in myTeam) != (it.victim in myTeam) }

    val myKills = enemyKills.count { it.killer == me }
    val myAssists = enemyKills.count { me in it.assistants }
    val myDeath = kills.firstOrNull { it.victim == me }
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
    )
}
