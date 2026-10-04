package com.ovalit.core.model

/**
 * 스코어보드 한 사람의 그 판 기록 중 K/D/A와 ADR 밖의 것입니다. S3에서 줄을 펼치면 보여 줍니다(사용자 요청, 2026-10-04). 그 판
 * 안의 기록이라 앱을 안 쓰는 사람에게도 보여 줄 수 있습니다(CLAUDE.md 지켜야 할 선).
 *
 * 내가 뛴 라운드의 킬 기록으로 셉니다. 내가 튕겨서 못 뛴 라운드는 빠집니다. 퍼블과 퍼데는 리포트와 같은 규칙으로, 라운드에서
 * 처음 나온 적 처치를 시각 순으로 찾고 스킬로 자기를 죽인 것과 팀킬은 건너뜁니다.
 *
 * @property multiKillRounds 적을 둘 이상 잡은 라운드 수입니다.
 */
data class MatchPlayerStats(
    val firstKills: Int,
    val firstDeaths: Int,
    val multiKillRounds: Int,
)

/** 스코어보드 사람마다의 [MatchPlayerStats]입니다. */
fun Match.playerStats(): Map<PlayerId, MatchPlayerStats> {
    val onMyTeam = players.associate { it.player to it.onMyTeam }
    val firstKills = mutableMapOf<PlayerId, Int>()
    val firstDeaths = mutableMapOf<PlayerId, Int>()
    val multiKills = mutableMapOf<PlayerId, Int>()

    for (round in rounds) {
        val enemyKills = round.kills.filter { kill ->
            val killerSide = onMyTeam[kill.killer]
            val victimSide = onMyTeam[kill.victim]
            kill.killer != kill.victim && killerSide != null && victimSide != null && killerSide != victimSide
        }
        enemyKills.minByOrNull { it.atMillis }?.let { first ->
            firstKills[first.killer] = (firstKills[first.killer] ?: 0) + 1
            firstDeaths[first.victim] = (firstDeaths[first.victim] ?: 0) + 1
        }
        enemyKills.groupingBy { it.killer }.eachCount()
            .filterValues { it >= 2 }
            .keys
            .forEach { killer -> multiKills[killer] = (multiKills[killer] ?: 0) + 1 }
    }

    return players.associate { line ->
        line.player to MatchPlayerStats(
            firstKills = firstKills[line.player] ?: 0,
            firstDeaths = firstDeaths[line.player] ?: 0,
            multiKillRounds = multiKills[line.player] ?: 0,
        )
    }
}
