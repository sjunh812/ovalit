package com.ovalit.core.model

/**
 * 한 라운드에서 눈에 띄는 장면입니다.
 *
 * @property ace 그 라운드 상대를 모두 내가 잡았으면 `true`입니다. 스킬로 우리 팀을 죽인 건 보지 않습니다.
 * @property clutch 우리 팀에서 나만 남았을 때 상대가 살아 있었으면 채웁니다.
 *   내가 먼저 죽었거나 처음부터 혼자였으면 `null`입니다.
 */
data class RoundHighlight(
    val ace: Boolean,
    val clutch: Clutch?,
)

/**
 * @property against 나만 남은 순간 살아 있던 상대 수입니다. 화면에는 "1대3"으로 띄웁니다.
 * @property won 그 라운드를 이겼는지입니다. 이기면 클러치 성공으로 칩니다.
 *   스파이크를 설치하고 죽었는데 터져서 이긴 라운드도 성공입니다.
 *   공식 기준이 없어 시작 기준선입니다.
 */
data class Clutch(
    val against: Int,
    val won: Boolean,
)

/**
 * 누가 언제 죽었는지만으로 가립니다. 킬은 목록 순서가 아니라 [KillEvent.atMillis] 순서로 봅니다.
 *
 * 튕겨서 그 라운드를 안 뛴 우리 팀은 킬 기록에 안 나와 끝까지 살아 있는 것처럼 보여서, 그런 라운드는 클러치로 잡히지 않습니다.
 * 응답이 라운드마다 누가 뛰었는지 알려 주는지는 실데이터로 확인해야 합니다.
 */
fun Round.highlight(me: PlayerId, allies: Set<PlayerId>, enemies: Set<PlayerId>): RoundHighlight {
    val ace = enemies.isNotEmpty() && enemies.all { enemy -> kills.any { it.killer == me && it.victim == enemy } }

    var clutch: Clutch? = null
    // 처음부터 우리 팀이 나 혼자면 클러치로 치지 않는다
    if (allies.isNotEmpty() && enemies.isNotEmpty()) {
        val aliveAllies = allies.toMutableSet()
        val aliveEnemies = enemies.toMutableSet()
        for (kill in kills.sortedBy { it.atMillis }) {
            if (kill.victim == me) break
            aliveAllies -= kill.victim
            aliveEnemies -= kill.victim
            if (aliveAllies.isEmpty()) {
                if (aliveEnemies.isNotEmpty()) clutch = Clutch(against = aliveEnemies.size, won = won)
                break
            }
        }
    }
    return RoundHighlight(ace = ace, clutch = clutch)
}

val Match.enemies: Set<PlayerId>
    get() = players.filterNot { it.onMyTeam }.map { it.player }.toSet()

/**
 * 라운드제 모드만 셉니다. 데스매치처럼 팀과 라운드가 없는 모드에는 에이스와 클러치가 없습니다.
 */
fun Match.highlights(): List<RoundHighlight> {
    if (queue.halfRounds == null) return emptyList()
    val enemies = enemies
    return rounds.map { it.highlight(me = me, allies = allies, enemies = enemies) }
}

/** 경기 줄 칩에 띄우는 그 판의 에이스와 이긴 클러치 수입니다. 진 클러치는 세지 않습니다. */
data class MatchHighlights(val aces: Int, val clutches: Int)

val Match.myHighlights: MatchHighlights
    get() = highlights().let { rounds ->
        MatchHighlights(aces = rounds.count { it.ace }, clutches = rounds.count { it.clutch?.won == true })
    }

/**
 * 프로필 통계의 에이스와 클러치입니다. 내 프로필과 S5가 같이 씁니다.
 *
 * @property clutchAttempts 나만 남은 채 상대가 남아 있던 라운드 수입니다. 클러치 성공률의 분모입니다.
 */
data class HighlightCount(
    val aces: Int,
    val clutches: Int,
    val clutchAttempts: Int,
)

fun List<Match>.highlightCount(): HighlightCount {
    val all = flatMap { it.highlights() }
    val clutches = all.mapNotNull { it.clutch }
    return HighlightCount(aces = all.count { it.ace }, clutches = clutches.count { it.won }, clutchAttempts = clutches.size)
}
