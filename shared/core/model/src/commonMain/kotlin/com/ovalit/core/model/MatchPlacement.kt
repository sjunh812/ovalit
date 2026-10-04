package com.ovalit.core.model

enum class MatchAward {
    /** 두 팀을 통틀어 전투점수가 가장 높은 사람입니다. */
    MVP,

    /** [MVP]가 없는 팀에서 전투점수가 가장 높은 사람입니다. */
    TEAM_MVP,
}

/**
 * 그 판 스코어보드에서의 자리입니다. 게임 스코어보드처럼 전투점수(ACS) 순이라 우리가 만든 분포에서의 등수가 아니라 그 판
 * 열 명 안의 자리입니다(CLAUDE.md 지켜야 할 선).
 *
 * 13.06부터 게임은 ACS 대신 Performance Score를 띄워서 게임이 고른 MVP와 다를 수 있습니다. 지금은 전투점수로 세고, 경기
 * 응답에 Performance Score가 오면 그 점수로 셉니다.
 *
 * @property rank 1부터 셉니다.
 * @property players 이 경기 스코어보드에 있는 사람 수입니다.
 */
data class MatchPlacement(
    val rank: Int,
    val players: Int,
    val award: MatchAward?,
)

/**
 * 스코어보드 사람마다의 자리입니다. 전투점수가 같으면 킬이 많은 쪽, 그다음 데스가 적은 쪽이 앞입니다. 뛴 라운드가 없어
 * 전투점수를 셀 수 없는 사람은 맨 뒤입니다. 팀 MVP는 두 팀이 라운드를 겨루는 모드에만 있습니다. 데스매치는 팀이 없습니다.
 */
fun Match.placements(): Map<PlayerId, MatchPlacement> {
    val ordered = players.sortedWith(
        compareByDescending<Scoreline> { it.acs ?: Double.NEGATIVE_INFINITY }
            .thenByDescending { it.kills }
            .thenBy { it.deaths },
    )
    val mvp = ordered.firstOrNull()
    val teamMvp = mvp?.takeIf { queue.halfRounds != null }?.let { best -> ordered.firstOrNull { it.onMyTeam != best.onMyTeam } }
    return ordered.withIndex().associate { (index, line) ->
        val award = when (line) {
            mvp -> MatchAward.MVP
            teamMvp -> MatchAward.TEAM_MVP
            else -> null
        }
        line.player to MatchPlacement(rank = index + 1, players = ordered.size, award = award)
    }
}

/** 내 자리입니다. 스코어보드에 내가 없으면 `null`입니다. */
val Match.myPlacement: MatchPlacement?
    get() = placements()[me]
