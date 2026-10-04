package com.ovalit.core.model

/**
 * S3 내 기록 탭의 상대 한 사람과의 맞대결입니다. op.gg의 매치 리포트처럼 누구를 잡고 누구에게 잡혔는지 봅니다(사용자 요청,
 * 2026-10-04). 내가 뛴 라운드만 셉니다.
 *
 * @property kills 내가 그 상대를 잡은 수입니다. [deaths]는 그 상대가 나를 잡은 수, [assists]는 그 상대가 죽을 때 내가 도운 수입니다.
 * @property damageDealt 내가 그 상대에게 입힌 피해이고 [damageTaken]은 그 상대에게 받은 피해입니다.
 */
data class Duel(
    val opponent: PlayerId,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damageDealt: Int,
    val damageTaken: Int,
)

/** 상대 팀 사람마다의 [Duel]입니다. 스코어보드 자리 순입니다. 스킬로 자기를 죽인 것은 세지 않습니다. */
fun Match.duels(): List<Duel> {
    val placements = placements()
    return players
        .filterNot { it.onMyTeam }
        .sortedBy { placements[it.player]?.rank ?: Int.MAX_VALUE }
        .map { line ->
            val opponent = line.player
            val kills = rounds.flatMap { it.kills }
            Duel(
                opponent = opponent,
                kills = kills.count { it.killer == me && it.victim == opponent },
                deaths = kills.count { it.killer == opponent && it.victim == me },
                assists = kills.count { it.victim == opponent && it.killer != me && me in it.assistants },
                damageDealt = rounds.sumOf { it.myDamageTo[opponent] ?: 0 },
                damageTaken = rounds.sumOf { it.myDamageFrom[opponent] ?: 0 },
            )
        }
}

/** 이 경기에서 쓴 무기마다의 내 성적입니다. 킬이 많은 순입니다. S6과 같은 기준으로 셉니다. */
fun Match.myWeapons(): List<WeaponStats> = listOf(this).weaponStats().sortedByDescending { it.kills }
