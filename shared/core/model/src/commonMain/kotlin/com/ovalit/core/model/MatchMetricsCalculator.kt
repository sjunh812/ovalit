package com.ovalit.core.model

/**
 * 내가 죽은 뒤 이 시간 안에 우리 팀이 내 킬러를 잡으면 트레이드로 칩니다.
 *
 * 공식 값이 없어 업계에서 흔히 쓰는 5초로 시작합니다. 동적 지표 기준선(표준편차 1.5배)처럼
 * 실데이터를 보고 조정할 값입니다. 바꾸면 KAST가 통째로 움직이니 CLAUDE.md도 같이 고칩니다.
 */
const val TRADE_WINDOW_MILLIS: Long = 5_000

fun Match.metrics(): MatchMetrics {
    val perRound = rounds.map { it.analyze(me = me, allies = allies) }

    return MatchMetrics(
        matches = 1,
        rounds = rounds.size,
        kills = perRound.sumOf { it.kills },
        deaths = perRound.count { it.died },
        assists = perRound.sumOf { it.assists },
        combatScore = myCombatScore,
        damage = rounds.sumOf { it.myDamage },
        shots = rounds.fold(Shots.None) { acc, round -> acc + round.myShots },
        kastRounds = perRound.count { it.kast },
        survivedRounds = perRound.count { !it.died },
    )
}

private class RoundResult(
    val kills: Int,
    val assists: Int,
    val died: Boolean,
    val kast: Boolean,
)

private fun Round.analyze(me: PlayerId, allies: Set<PlayerId>): RoundResult {
    // 스킬로 자기를 죽이거나 우리 팀을 죽인 건 킬로 세지 않는다. 응답에는 둘 다 킬로 들어온다.
    val myKills = kills.count { it.killer == me && it.victim != me && it.victim !in allies }
    val myAssists = kills.count { me in it.assistants }
    val myDeath = kills.firstOrNull { it.victim == me }

    val traded = myDeath != null && kills.any { revenge ->
        revenge.victim == myDeath.killer &&
            revenge.killer in allies &&
            revenge.atMillis - myDeath.atMillis in 0..TRADE_WINDOW_MILLIS
    }

    return RoundResult(
        kills = myKills,
        assists = myAssists,
        died = myDeath != null,
        kast = myKills > 0 || myAssists > 0 || myDeath == null || traded,
    )
}
