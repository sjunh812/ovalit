package com.ovalit.core.model

/**
 * S3 라운드·이코노미 탭의 한 줄입니다. 내가 튕겨서 못 뛴 라운드는 [played]가 `false`이고 승패만 있습니다.
 *
 * @property myKills 스킬로 자기나 우리 팀을 죽인 건 빼고 셉니다. 리포트 K/D와 같은 기준입니다.
 * @property highlight 에이스와 클러치입니다. 못 뛴 라운드거나 라운드제가 아닌 모드면 `null`입니다.
 * @property enemyBuyType 상대 팀의 구매 유형입니다. 우리 팀과 같은 기준입니다.
 * @property myDamage 그 라운드에 내가 입힌 피해입니다. 못 뛴 라운드면 `null`입니다.
 * @property kills 그 라운드의 킬을 시각 순으로 담습니다. 스킬로 자기를 죽인 것과 팀킬은 뺍니다. S3 라운드 탭의 킬 순서입니다.
 */
data class RoundSummary(
    val number: Int,
    val won: Boolean,
    val played: Boolean,
    val side: Side?,
    val ending: RoundEnding?,
    val myKills: Int,
    val firstKill: Boolean,
    val firstDeath: Boolean,
    val buyType: BuyType?,
    val economy: RoundEconomy?,
    val highlight: RoundHighlight? = null,
    val enemyBuyType: BuyType? = null,
    val myDamage: Int? = null,
    val kills: List<RoundKill> = emptyList(),
)

/**
 * S3 라운드 탭 킬 순서의 한 줄입니다.
 *
 * @property byMyTeam 우리 팀이 낸 킬인지입니다.
 * @property firstBlood 그 라운드에서 처음 나온 적 처치인지입니다.
 */
data class RoundKill(
    val atMillis: Long,
    val killer: PlayerId,
    val victim: PlayerId,
    val weapon: WeaponId?,
    val byMyTeam: Boolean,
    val firstBlood: Boolean,
)

fun Match.roundSummaries(): List<RoundSummary> {
    val byNumber = rounds.associateBy { it.number }
    val enemies = enemies
    val roundBased = queue.halfRounds != null
    return roundOutcomes.mapIndexed { index, won ->
        val number = index + 1
        val round = byNumber[number]
        if (round == null) {
            RoundSummary(number, won, played = false, side = null, ending = null, myKills = 0,
                firstKill = false, firstDeath = false, buyType = null, economy = null)
        } else {
            val result = round.analyze(me = me, allies = allies)
            val myTeam = allies + me
            val enemyKills = round.kills
                .filter { it.killer != it.victim && (it.killer in myTeam) != (it.victim in myTeam) }
                .sortedBy { it.atMillis }
            RoundSummary(
                number = number,
                won = won,
                played = true,
                side = round.mySide,
                ending = round.ending,
                myKills = result.kills,
                firstKill = result.firstKill,
                firstDeath = result.firstDeath,
                buyType = round.buyType(queue),
                economy = round.economy,
                highlight = if (roundBased) round.highlight(me = me, allies = allies, enemies = enemies) else null,
                enemyBuyType = round.enemyBuyType(queue),
                myDamage = round.myDamage,
                kills = enemyKills.mapIndexed { index, kill ->
                    RoundKill(
                        atMillis = kill.atMillis,
                        killer = kill.killer,
                        victim = kill.victim,
                        weapon = kill.weapon,
                        byMyTeam = kill.killer in myTeam,
                        firstBlood = index == 0,
                    )
                },
            )
        }
    }
}

/**
 * 이코노미 탭의 구매 유형 표 한 줄입니다. 유형마다 그 유형으로 산 라운드와 이긴 라운드입니다.
 *
 * @property enemyRounds 상대 팀이 그 유형으로 산 라운드입니다. [enemyWins]는 그중 상대가 이긴 라운드입니다.
 */
data class BuyRecord(val type: BuyType, val rounds: Int, val wins: Int, val enemyRounds: Int = 0, val enemyWins: Int = 0)

// 못 뛴 라운드는 장비 가치가 없어서 어느 유형에도 들지 않는다
fun Match.buyRecords(): List<BuyRecord> {
    val summaries = roundSummaries()
    return listOf(BuyType.FULL_BUY, BuyType.FORCE_BUY, BuyType.ECO).map { type ->
        val rounds = summaries.filter { it.buyType == type }
        val enemyRounds = summaries.filter { it.enemyBuyType == type }
        BuyRecord(
            type = type,
            rounds = rounds.size,
            wins = rounds.count { it.won },
            enemyRounds = enemyRounds.size,
            enemyWins = enemyRounds.count { !it.won },
        )
    }
}

/** 몇 라운드 중 몇 번 이겼는지입니다. */
data class WinRecord(val rounds: Int, val wins: Int) {
    val losses: Int get() = rounds - wins
}

/**
 * S3 라운드 탭 맨 위의 요약입니다. 첫 킬을 낸 쪽이 라운드를 얼마나 가져갔는지, 공격과 수비에서 몇 번 이겼는지입니다. op.gg와
 * tracker.gg처럼 그 판의 흐름을 한눈에 봅니다(사용자 요청, 2026-10-04). 내가 뛴 라운드만 셉니다.
 *
 * @property ourFirstBlood 우리 팀이 첫 킬을 낸 라운드입니다. [theirFirstBlood]는 상대가 딴 라운드이고 승은 우리 팀이 이긴 수입니다.
 * @property upsets 우리 팀 장비가 상대보다 [UPSET_LOADOUT_GAP] 넘게 적었는데 이긴 라운드 수입니다.
 */
data class RoundsOverview(
    val ourFirstBlood: WinRecord,
    val theirFirstBlood: WinRecord,
    val attack: WinRecord,
    val defense: WinRecord,
    val upsets: Int,
)

/** 장비가 이만큼 넘게 적으면 불리했던 라운드로 봅니다. 시작 기준선입니다. */
const val UPSET_LOADOUT_GAP = 1_000

fun Match.roundsOverview(): RoundsOverview {
    val played = roundSummaries().filter { it.played }
    fun List<RoundSummary>.record() = WinRecord(rounds = size, wins = count { it.won })
    return RoundsOverview(
        ourFirstBlood = played.filter { it.kills.firstOrNull()?.byMyTeam == true }.record(),
        theirFirstBlood = played.filter { it.kills.firstOrNull()?.byMyTeam == false }.record(),
        attack = played.filter { it.side == Side.ATTACK }.record(),
        defense = played.filter { it.side == Side.DEFENSE }.record(),
        upsets = played.count { round ->
            val economy = round.economy
            round.won && economy != null && round.buyType != BuyType.PISTOL &&
                economy.enemyLoadout - economy.teamLoadout > UPSET_LOADOUT_GAP
        },
    )
}
