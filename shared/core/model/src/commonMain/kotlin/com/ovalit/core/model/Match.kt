package com.ovalit.core.model

import kotlin.time.Instant

/**
 * 내가 뛴 경기 하나입니다. 지표는 전부 여기서 나옵니다.
 *
 * API 응답을 그대로 옮기지 않았습니다. 응답에 아직 확인 못 한 부분이 있어 계산에 필요한 것만 정해 두고,
 * 네트워크 계층이 여기에 맞춰 옮겨 담습니다.
 *
 * @property myRole 서버의 역할 표에 아직 없는 새 요원이면 `null`입니다.
 * @property allies 나를 뺀 우리 팀입니다. 팀킬, 트레이드, 클러치, 같이 뛴 경기를 이걸로 가립니다.
 * @property myTeamWon 비겼거나 결과를 모르면 `null`입니다. 승률을 낼 때 분모에서 뺍니다.
 * @property myCombatScore `players[].stats.score`. 라운드별이 아니라 경기 전체 합입니다.
 * @property rounds 내가 뛴 라운드만 담습니다. 중간에 튕겼다 들어온 경기에서 전체 라운드를
 * 넣으면 ACS와 ADR이 실제보다 낮게 나옵니다. 응답의 `stats.roundsPlayed`와 개수가 같아야 합니다.
 * @property roundOutcomes 라운드마다 우리 팀이 이겼는지입니다. [rounds]와 달리 내가 못 뛴 라운드도
 * 들어갑니다. 스코어와 라운드 막대는 이걸로 그립니다.
 * @property players 스코어보드입니다. 나도 들어갑니다. 앱을 쓰지 않는 사람은 이 경기 안의 기록까지만
 * 보여줄 수 있습니다.
 * @property teams 응답의 `teams[]`입니다. 두 팀이 라운드를 겨루는 모드는 비워 둬도 [roundOutcomes]와 [Scoreline.onMyTeam]으로
 * 그립니다. 데스매치는 사람마다, 건틀릿은 두 사람마다 한 팀이라 셋 이상이면 등수로 끝나는 경기로 봅니다([format]).
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
    val teams: List<MatchTeam> = emptyList(),
) {
    val format: MatchFormat
        get() = when {
            teams.size > 2 -> if (teams.all { it.members.size <= 1 }) MatchFormat.FREE_FOR_ALL else MatchFormat.TEAM_PLACEMENT
            queue == Queue.DEATHMATCH -> MatchFormat.FREE_FOR_ALL
            queue.halfRounds != null -> MatchFormat.ROUNDS
            else -> MatchFormat.TEAM_POINTS
        }

    /**
     * 라운드제 모드는 이긴 라운드 수이고 내가 튕겨서 못 뛴 라운드까지 셉니다. 팀 데스매치처럼 라운드가 없는 두 팀 모드는 응답의
     * 팀 점수(`numPoints`)입니다. 등수로 끝나는 모드에는 맞지 않아 [myStanding]을 씁니다.
     */
    val score: Score
        get() = teamPoints().takeIf { format == MatchFormat.TEAM_POINTS }
            ?: Score(myTeam = roundOutcomes.count { it }, enemyTeam = roundOutcomes.count { !it })

    private fun teamPoints(): Score? {
        val mine = teams.firstOrNull { me in it.members }?.points ?: return null
        val theirs = teams.firstOrNull { me !in it.members }?.points ?: return null
        return Score(myTeam = mine, enemyTeam = theirs)
    }

    val myScoreline: Scoreline?
        get() = players.firstOrNull { it.player == me }
}

data class Score(
    val myTeam: Int,
    val enemyTeam: Int,
)

/** 경기가 어떤 모양으로 끝나는지입니다. 스코어, 스코어보드, 탭을 이걸로 고릅니다. */
enum class MatchFormat {
    /** 두 팀이 라운드를 겨룹니다. 경쟁, 일반, 프리미어, 스파이크 돌격, 신속 플레이, 복제입니다. */
    ROUNDS,

    /** 두 팀이 라운드 없이 점수를 겨룹니다. 팀 데스매치, 에스컬레이션, 눈싸움이고 큐를 모르는 두 팀 모드도 여기입니다. */
    TEAM_POINTS,

    /** 모두가 혼자 등수를 다툽니다. 데스매치입니다. */
    FREE_FOR_ALL,

    /** 작은 팀 여럿이 등수를 다툽니다. 건틀릿: 글리치가 두 명씩 여덟 팀입니다. */
    TEAM_PLACEMENT,
}

/**
 * 응답의 `teams[]` 한 줄에 그 팀 사람을 붙였습니다.
 *
 * @property members 응답의 `players[].teamId`가 이 팀인 사람입니다. 데스매치는 한 사람입니다.
 * @property won 응답의 `won`입니다.
 * @property points 응답의 `numPoints`입니다. 데스매치는 그 사람의 킬, 팀 데스매치는 팀 킬입니다. 모르면 `null`입니다.
 * @property placement 등수로 끝나는 모드에서 응답이 준 등수입니다. 1부터 셉니다. `null`이면 [points]로 셉니다.
 */
data class MatchTeam(
    val members: Set<PlayerId>,
    val won: Boolean?,
    val points: Int?,
    val placement: Int? = null,
)

/**
 * 전반, 후반, 연장 스코어를 순서대로 담습니다. 전반에 끝났으면 하나, 연장까지 안 갔으면 둘입니다. 라운드제가
 * 아닌 모드면 빈 목록입니다.
 */
val Match.halfScores: List<Score>
    get() {
        val half = queue.halfRounds ?: return emptyList()
        return roundOutcomes.withIndex()
            .groupBy { (index, _) -> minOf(index / half, 2) }
            .values
            .map { part -> Score(myTeam = part.count { it.value }, enemyTeam = part.count { !it.value }) }
    }

/**
 * 가장 최근 경쟁전의 내 티어 번호입니다. 응답은 경기 당시 티어만 줘서 이걸 지금 티어로 씁니다. 그 판에 티어가 빠져
 * 있으면 그 앞 판을 봅니다. 홈 배지와 프로필 티어 카드가 같은 규칙을 써야 두 곳의 티어가 갈리지 않습니다.
 */
fun Iterable<Match>.latestTier(): Int? = this
    .filter { it.queue == Queue.COMPETITIVE && it.myScoreline?.tier != null }
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
 * @property shots 이 경기에서 맞힌 부위별 횟수입니다. 응답에는 라운드별 `damage[]`에만 있어서 더해서 담습니다. 모르면
 * `null`이고 S3에서 펼친 줄의 헤드샷을 비웁니다.
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
    val shots: Shots? = null,
) {
    val acs: Double? get() = combatScore over roundsPlayed

    val adr: Double? get() = damage over roundsPlayed
}

/**
 * @property number 1부터 셉니다. 전반과 후반 첫 라운드(피스톨)를 이 번호로 가리고, S3 라운드 줄도 이 번호로
 * [Match.roundOutcomes]의 몇 번째 라운드인지 찾습니다. 응답의 `roundResults[].roundNum`이 0부터 온다면 1을 더해
 * 담습니다(실데이터로 확인해야 합니다).
 * @property kills 라운드에서 일어난 킬 전부입니다. 나와 상관없는 킬도 들어갑니다. 트레이드와
 * 첫 킬을 가르려면 누가 먼저 죽었는지 알아야 합니다.
 * @property myShots 내가 맞힌 부위별 횟수입니다. 킬 수가 아니라 적중 수입니다.
 * @property mySide 그 라운드에 내가 공격이었는지 수비였는지입니다. 모르면 `null`이고, 공수를 나눠 셀 때
 * 양쪽 다 빠집니다.
 * @property ending 라운드가 어떻게 끝났는지입니다. 응답의 `roundResult`에서 옵니다.
 * @property myDamageTo 내가 상대마다 입힌 피해입니다. 응답의 `roundResults[].playerStats[내 것].damage[]`를 받는 사람마다 더해
 * 담습니다.
 * @property myDamageFrom 상대마다 내가 받은 피해입니다. 다른 사람의 `damage[]` 중 받는 사람이 나인 것을 더해 담습니다.
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
    val myDamageTo: Map<PlayerId, Int> = emptyMap(),
    val myDamageFrom: Map<PlayerId, Int> = emptyMap(),
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
 * @property myWeapon 라운드를 시작할 때 내가 든 무기(`economy.weapon`)입니다. 주워 쓴 총이 안 잡혀 무기별 킬에는 쓰지
 * 않고, 무기별 데스·어시스트·라운드당 피해량에만 씁니다.
 */
data class RoundEconomy(
    val myLoadout: Int,
    val teamLoadout: Int,
    val enemyLoadout: Int,
    val myWeapon: WeaponId? = null,
)

/**
 * @property atMillis 라운드 시작부터 잰 시각(`timeSinceRoundStartMillis`)입니다.
 * @property weapon 킬을 낸 무기입니다. `finishingDamage.damageItem`에서 옵니다.
 * `economy.weapon`을 쓰면 주워 쓴 총이 안 잡힙니다. 스킬 킬이면 `null`입니다.
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
