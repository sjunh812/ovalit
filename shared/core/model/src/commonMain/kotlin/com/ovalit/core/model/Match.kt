package com.ovalit.core.model

import kotlin.time.Instant

/**
 * 내가 뛴 경기 하나입니다. 지표는 전부 여기서 나옵니다.
 *
 * API 응답을 그대로 옮긴 모양이 아닙니다. 응답에는 아직 확인 못 한 부분이 남아 있어서,
 * 계산에 필요한 것만 도메인 모양으로 정해 두고 네트워크 계층이 여기로 옮겨 담게 합니다.
 *
 * @property myRole 콘텐츠 카탈로그에 아직 안 올라온 새 요원이면 없습니다.
 * @property allies 나를 뺀 우리 팀. 내가 죽은 뒤 누가 복수했는지(트레이드) 가를 때 씁니다.
 * @property myTeamWon 비겼거나 결과를 모르면 없습니다. 승률을 낼 때 분모에서 뺍니다.
 * @property myCombatScore `players[].stats.score`. 라운드별이 아니라 경기 전체 합입니다.
 * @property rounds 내가 뛴 라운드만 담습니다. 중간에 튕겼다 들어온 경기에서 전체 라운드를
 * 넣으면 ACS와 ADR이 실제보다 낮게 나옵니다. 응답의 `stats.roundsPlayed`와 개수가 같아야 합니다.
 * @property roundOutcomes 라운드마다 우리 팀이 이겼는지입니다. [rounds]와 달리 내가 못 뛴 라운드도
 * 들어갑니다. 스코어와 라운드 막대는 이걸로 그립니다.
 * @property players 스코어보드입니다. 나도 들어갑니다. 앱을 쓰지 않는 사람은 이 경기 안의 기록까지만
 * 보여줄 수 있습니다.
 */
data class Match(
    val id: MatchId,
    val queue: Queue,
    val act: ActId,
    val map: MapId,
    val startedAt: Instant,
    val lengthMillis: Long,
    val me: PlayerId,
    val myAgent: AgentId,
    val myRole: Role?,
    val allies: Set<PlayerId>,
    val myCombatScore: Int,
    val myTeamWon: Boolean?,
    val roundOutcomes: List<Boolean>,
    val rounds: List<Round>,
    val players: List<Scoreline>,
) {
    val score: Score
        get() = Score(myTeam = roundOutcomes.count { it }, enemyTeam = roundOutcomes.count { !it })

    val myScoreline: Scoreline?
        get() = players.firstOrNull { it.player == me }
}

data class Score(
    val myTeam: Int,
    val enemyTeam: Int,
)

/**
 * 전반, 후반, 연장 스코어를 순서대로 담습니다. 연장까지 안 갔으면 둘입니다. 라운드제가 아닌 모드면
 * 비어 있습니다.
 */
val Match.halfScores: List<Score>
    get() {
        val half = queue.halfRounds ?: return emptyList()
        return roundOutcomes.withIndex()
            .groupBy { (index, _) -> minOf(index / half, 2) }
            .values
            .map { part -> Score(myTeam = part.count { it.value }, enemyTeam = part.count { !it.value }) }
    }

/** 가장 최근 경쟁전의 내 티어 번호입니다. 응답은 경기 당시 티어만 줘서 이걸 지금 티어로 씁니다. */
fun Iterable<Match>.latestTier(): Int? = this
    .filter { it.queue == Queue.COMPETITIVE }
    .maxByOrNull { it.startedAt }
    ?.myScoreline
    ?.tier

/**
 * 스코어보드 한 줄입니다. 게임 스코어보드와 같은 숫자를 보여주려고 응답의 `players[].stats`를 그대로
 * 옮깁니다. 그래서 내 줄의 킬 수가 리포트 K/D의 킬 수와 다를 수 있습니다. 리포트는 스킬로 자기나 우리
 * 팀을 죽인 걸 빼고 셉니다.
 *
 * @property tier 경기 당시 티어 번호(`competitiveTier`)입니다. 배치를 안 끝냈으면 `null`입니다.
 * @property damage 이 경기에서 입힌 피해 합계입니다. 응답에는 라운드별로만 있어서 더해서 담습니다.
 */
data class Scoreline(
    val player: PlayerId,
    val riotId: String,
    val agent: AgentId,
    val onMyTeam: Boolean,
    val tier: Int?,
    val playerCard: PlayerCardId?,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val combatScore: Int,
    val damage: Int,
    val roundsPlayed: Int,
) {
    val acs: Double? get() = combatScore over roundsPlayed

    val adr: Double? get() = damage over roundsPlayed
}

/**
 * 라운드 하나입니다.
 *
 * @property kills 라운드에서 일어난 킬 전부입니다. 나와 무관한 킬도 들어갑니다. 트레이드와
 * 퍼블을 가르려면 누가 먼저 죽었는지 알아야 합니다.
 * @property myShots 내가 맞힌 부위별 횟수입니다. 킬 수가 아니라 적중 수입니다.
 * @property mySide 그 라운드에 내가 공격이었는지 수비였는지입니다. 모르면 없고, 공수를 나눠 셀 때
 * 양쪽 다 빠집니다.
 * @property ending 라운드가 어떻게 끝났는지입니다. 응답의 `roundResult`에서 옵니다.
 */
data class Round(
    val number: Int,
    val won: Boolean,
    val kills: List<KillEvent>,
    val myDamage: Int,
    val myShots: Shots,
    val mySide: Side?,
    val ending: RoundEnding?,
    val economy: RoundEconomy?,
)

enum class RoundEnding {
    ELIMINATION,
    SPIKE_DETONATED,
    SPIKE_DEFUSED,
    TIME_EXPIRED,
    SURRENDERED,
}

/**
 * 라운드를 시작할 때 들고 있던 장비 가치(`economy.loadoutValue`)입니다. 팀 값은 한 사람당 평균이라
 * 누가 튕겨 네 명이 뛴 라운드도 같은 기준으로 가를 수 있습니다.
 *
 * @property myWeapon 라운드를 시작할 때 내가 든 주무기(`economy.weapon`)입니다. 킬을 무기별로 셀 때는 쓰지 않습니다.
 * 주워 쓴 총이 안 잡혀서입니다. 무기별 데스와 라운드당 피해량처럼 킬로는 무기를 알 수 없는 숫자의 기준으로만 씁니다.
 */
data class RoundEconomy(
    val myLoadout: Int,
    val teamLoadout: Int,
    val enemyLoadout: Int,
    val myWeapon: WeaponId? = null,
)

/**
 * @property atMillis 라운드 시작부터 잰 시각. 응답의 `timeSinceRoundStartMillis`입니다.
 * @property weapon 킬을 낸 무기. `finishingDamage.damageItem`에서 옵니다.
 * `economy.weapon`을 쓰면 주워 쓴 총이 안 잡힙니다. 스킬 킬이면 없습니다.
 */
data class KillEvent(
    val atMillis: Long,
    val killer: PlayerId,
    val victim: PlayerId,
    val assistants: Set<PlayerId>,
    val weapon: WeaponId?,
)

data class Shots(
    val head: Int,
    val body: Int,
    val leg: Int,
) {
    val total: Int get() = head + body + leg

    operator fun plus(other: Shots) = Shots(
        head = head + other.head,
        body = body + other.body,
        leg = leg + other.leg,
    )

    companion object {
        val None = Shots(head = 0, body = 0, leg = 0)
    }
}
