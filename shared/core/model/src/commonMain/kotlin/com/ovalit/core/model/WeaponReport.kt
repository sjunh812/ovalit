package com.ovalit.core.model

/**
 * 한 무기만 쓴 라운드가 이만큼은 있어야 그 무기의 헤드샷 비율을 보여줍니다. 실데이터를 보고
 * 조정할 시작값입니다.
 */
const val MIN_WEAPON_ROUNDS = 20

/**
 * 무기 하나의 성적입니다.
 *
 * 응답에는 맞힌 탄이 라운드별로만 있고 무기별로는 없습니다. 그래서 내 킬이 전부 이 무기로 난
 * 라운드만 이 무기 몫으로 셉니다(단일 무기 라운드). 킬이 없는 라운드는 무슨 무기를 썼는지 모릅니다.
 */
data class WeaponStats(
    val weapon: WeaponId,
    val kills: Int,
    val singleWeaponRounds: Int,
    val shots: Shots,
) {
    val headshotRate: Double? get() = shots.head over shots.total

    val isMeasurable: Boolean get() = singleWeaponRounds >= MIN_WEAPON_ROUNDS
}

/**
 * S6 무기 화면 위쪽에 크게 보여주는 무기입니다.
 *
 * @property current 홈 리포트와 같은 기간의 성적입니다. 리포트를 만들 만큼 경기가 없으면 `null`입니다.
 * @property baseline 기간 바로 앞 4주입니다.
 * @property movement 동적 칸과 같은 규칙으로 봅니다. 화면은 [Movement.MOVED]일 때만 문구를 붙이고
 * 색을 칠합니다.
 */
data class WeaponHighlight(
    val act: WeaponStats,
    val current: WeaponStats?,
    val baseline: WeaponStats?,
    val baselineWeeks: Int,
    val movement: Movement,
)

/**
 * @property weapons 이번 액트에서 킬을 많이 낸 순서입니다.
 * @property highlights 그중 앞의 [HIGHLIGHTED_WEAPONS]개입니다.
 */
data class WeaponReport(
    val matches: Int,
    val kills: Int,
    val weapons: List<WeaponStats>,
    val highlights: List<WeaponHighlight>,
)

const val HIGHLIGHTED_WEAPONS = 2

internal fun List<Match>.weaponStats(): List<WeaponStats> {
    val kills = mutableMapOf<WeaponId, Int>()
    val rounds = mutableMapOf<WeaponId, Int>()
    val shots = mutableMapOf<WeaponId, Shots>()

    for (match in this) {
        val myTeam = match.allies + match.me
        for (round in match.rounds) {
            val myKills = round.kills.filter { it.killer == match.me && it.victim !in myTeam }
            myKills.mapNotNull { it.weapon }.forEach { kills[it] = (kills[it] ?: 0) + 1 }
            val weapon = myKills.map { it.weapon }.distinct().singleOrNull() ?: continue
            rounds[weapon] = (rounds[weapon] ?: 0) + 1
            shots[weapon] = (shots[weapon] ?: Shots.None) + round.myShots
        }
    }

    return kills.keys
        .map { weapon ->
            WeaponStats(
                weapon = weapon,
                kills = kills.getValue(weapon),
                singleWeaponRounds = rounds[weapon] ?: 0,
                shots = shots[weapon] ?: Shots.None,
            )
        }
        .sortedByDescending { it.kills }
}
