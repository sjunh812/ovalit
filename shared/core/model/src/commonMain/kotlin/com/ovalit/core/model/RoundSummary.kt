package com.ovalit.core.model

/**
 * S3 라운드·이코노미 탭의 한 줄입니다. 내가 튕겨서 못 뛴 라운드는 [played]가 `false`이고 승패만 있습니다.
 *
 * @property myKills 스킬로 자기나 우리 팀을 죽인 건 빼고 셉니다. 리포트 K/D와 같은 기준입니다.
 * @property highlight 에이스와 클러치입니다. 못 뛴 라운드와 라운드제가 아닌 모드면 없습니다.
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
            )
        }
    }
}

/** 이코노미 탭 위쪽의 요약입니다. 유형마다 몇 라운드를 뛰어 몇 번 이겼는지입니다. */
data class BuyRecord(val type: BuyType, val rounds: Int, val wins: Int)

// 못 뛴 라운드는 장비 가치가 없어서 어느 유형에도 들지 않는다
fun Match.buyRecords(): List<BuyRecord> {
    val summaries = roundSummaries()
    return listOf(BuyType.FULL_BUY, BuyType.FORCE_BUY, BuyType.ECO).map { type ->
        val rounds = summaries.filter { it.buyType == type }
        BuyRecord(type, rounds = rounds.size, wins = rounds.count { it.won })
    }
}
