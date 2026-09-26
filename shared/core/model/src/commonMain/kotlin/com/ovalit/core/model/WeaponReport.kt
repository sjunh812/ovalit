package com.ovalit.core.model

/**
 * 한 무기만 쓴 라운드가 이만큼은 있어야 그 무기의 헤드샷 비율을, 들고 시작한 라운드가 이만큼은 있어야 K/D와
 * 피해량을 보여줍니다. 실데이터를 보고 조정할 시작값입니다.
 */
const val MIN_WEAPON_ROUNDS = 20

/**
 * 무기 하나의 성적입니다. 숫자마다 무엇을 기준으로 이 무기 몫을 가르는지가 다릅니다.
 *
 * - 킬은 그 무기로 낸 킬입니다(`finishingDamage.damageItem`). 정확합니다.
 * - 헤드샷은 내 킬이 전부 이 무기로 난 라운드만 셉니다(단일 무기 라운드). 응답에 맞힌 탄이 라운드별로만 있고
 *   무기별로는 없어서입니다.
 * - 데스, 어시스트, 피해량은 이 무기를 들고 시작한 라운드에서 셉니다([carriedRounds], `economy.weapon`).
 *   죽을 때 든 무기와 피해를 준 무기는 응답에 없어서입니다. 주워 쓴 총은 여기에 안 잡힙니다.
 */
data class WeaponStats(
    val weapon: WeaponId,
    val kills: Int,
    val singleWeaponRounds: Int,
    val shots: Shots,
    val carriedRounds: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
) {
    val headshotRate: Double? get() = shots.head over shots.total

    val isMeasurable: Boolean get() = singleWeaponRounds >= MIN_WEAPON_ROUNDS

    /** 이 무기로 낸 킬 ÷ 이 무기를 들고 시작한 라운드의 데스입니다. 데스가 없으면 비웁니다. */
    val kd: Double? get() = kills over deaths

    /** (이 무기로 낸 킬 + 이 무기를 들고 시작한 라운드의 어시스트) ÷ 그 라운드의 데스입니다. [MatchMetrics.kda]와 같은 쓰임입니다. */
    val kda: Double? get() = (kills + assists) over deaths

    /** 이 무기를 들고 시작한 라운드의 라운드당 피해량입니다. */
    val damagePerRound: Double? get() = damage over carriedRounds

    /** K/D와 라운드당 피해량을 보여줄 만큼 이 무기를 들고 시작했는지입니다. 헤드샷과 같은 최소 라운드를 씁니다. */
    val isCarriedMeasurable: Boolean get() = carriedRounds >= MIN_WEAPON_ROUNDS

    /** 표본을 넘긴 값만 돌려줍니다. */
    fun value(metric: WeaponMetric): Double? = when (metric) {
        WeaponMetric.KD -> kd.takeIf { isCarriedMeasurable }
        WeaponMetric.DAMAGE_PER_ROUND -> damagePerRound.takeIf { isCarriedMeasurable }
        WeaponMetric.HEADSHOT_RATE -> headshotRate.takeIf { isMeasurable }
    }
}

/** S6 무기 화면 위쪽 표에 나란히 두는 지표입니다. 순서가 곧 열 순서입니다. */
enum class WeaponMetric { KD, DAMAGE_PER_ROUND, HEADSHOT_RATE }

/**
 * S6 무기 화면 위쪽 표의 한 줄입니다.
 *
 * @property current 홈 리포트와 같은 기간의 성적입니다. 세 지표 중 하나라도 표본이 모자라면 `null`이고 화면은 [act]를
 * 띄웁니다. 한 줄 안에서 기간이 섞이면 어느 숫자가 언제 것인지 알 수 없습니다. 리포트를 만들 만큼 경기가 없어도
 * `null`입니다.
 * @property baseline 기간 바로 앞 4주입니다.
 * @property movements 지표마다 동적 칸과 같은 규칙으로 봅니다. 화면은 [Movement.MOVED]인 변화량만 칠합니다.
 */
data class WeaponHighlight(
    val act: WeaponStats,
    val current: WeaponStats?,
    val baseline: WeaponStats?,
    val baselineWeeks: Int,
    val movements: Map<WeaponMetric, Movement>,
) {
    fun movement(metric: WeaponMetric): Movement = movements[metric] ?: Movement.UNKNOWN
}

/**
 * @property weapons 이번 액트에서 킬을 많이 낸 순서입니다.
 * @property highlights 그중 앞의 [HIGHLIGHTED_WEAPONS]개입니다. 많이 들고 시작한 순서가 아니라 킬 순서입니다.
 * @property period [highlights]가 보는 홈 리포트 기간입니다. 리포트를 만들 만큼 경기가 없으면 `null`입니다.
 */
data class WeaponReport(
    val matches: Int,
    val kills: Int,
    val weapons: List<WeaponStats>,
    val highlights: List<WeaponHighlight>,
    val period: ReportPeriod? = null,
)

const val HIGHLIGHTED_WEAPONS = 3

internal fun List<Match>.weaponStats(): List<WeaponStats> {
    val kills = mutableMapOf<WeaponId, Int>()
    val rounds = mutableMapOf<WeaponId, Int>()
    val shots = mutableMapOf<WeaponId, Shots>()
    val carried = mutableMapOf<WeaponId, CarriedTotals>()

    for (match in this) {
        val myTeam = match.allies + match.me
        for (round in match.rounds) {
            round.economy?.myWeapon?.let { weapon ->
                // 어시스트는 리포트와 같은 기준이다. 스킬로 자기나 우리 팀을 죽인 킬은 세지 않는다.
                val assists = round.kills.count { match.me in it.assistants && (it.killer in myTeam) != (it.victim in myTeam) }
                val died = round.kills.any { it.victim == match.me }
                carried[weapon] = (carried[weapon] ?: CarriedTotals()).plus(died = died, assists = assists, damage = round.myDamage)
            }
            val myKills = round.kills.filter { it.killer == match.me && it.victim !in myTeam }
            myKills.mapNotNull { it.weapon }.forEach { kills[it] = (kills[it] ?: 0) + 1 }
            val weapon = myKills.map { it.weapon }.distinct().singleOrNull() ?: continue
            rounds[weapon] = (rounds[weapon] ?: 0) + 1
            shots[weapon] = (shots[weapon] ?: Shots.None) + round.myShots
        }
    }

    // 들고 시작만 하고 킬이 없는 무기도 넣는다. 오딘을 다섯 라운드 사서 한 명도 못 잡은 것도 알아야 한다.
    return (kills.keys + carried.keys)
        .map { weapon ->
            WeaponStats(
                weapon = weapon,
                kills = kills[weapon] ?: 0,
                singleWeaponRounds = rounds[weapon] ?: 0,
                shots = shots[weapon] ?: Shots.None,
                carriedRounds = carried[weapon]?.rounds ?: 0,
                deaths = carried[weapon]?.deaths ?: 0,
                assists = carried[weapon]?.assists ?: 0,
                damage = carried[weapon]?.damage ?: 0,
            )
        }
        .sortedByDescending { it.kills }
}

private data class CarriedTotals(val rounds: Int = 0, val deaths: Int = 0, val assists: Int = 0, val damage: Int = 0) {
    fun plus(died: Boolean, assists: Int, damage: Int) = CarriedTotals(
        rounds = rounds + 1,
        deaths = deaths + if (died) 1 else 0,
        assists = this.assists + assists,
        damage = this.damage + damage,
    )
}
