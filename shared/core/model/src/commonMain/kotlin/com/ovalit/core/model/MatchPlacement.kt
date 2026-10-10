package com.ovalit.core.model

enum class MatchAward {
    /** 두 팀을 통틀어 전투점수가 가장 높은 사람입니다. */
    MVP,

    /** [MVP]가 없는 팀에서 전투점수가 가장 높은 사람입니다. */
    TEAM_MVP,
}

/**
 * 그 판 스코어보드에서 몇 번째인지입니다.
 * 게임 스코어보드처럼 라운드제 모드는 전투점수(ACS) 순, 데스매치는 킬 순이라 우리가 만든 분포에서 매긴 등수가 아닙니다(CLAUDE.md 지켜야 할 선).
 *
 * 13.06부터 게임은 ACS 대신 Performance Score를 띄워서 게임이 고른 MVP와 다를 수 있습니다.
 * 지금은 전투점수로 세고, 경기 응답에 Performance Score가 오면 그 점수로 셉니다.
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
 * 스코어보드 사람마다의 자리입니다.
 *
 * 라운드제 모드는 라운드당 전투점수 순입니다.
 * 전투점수가 같으면 킬이 많은 쪽, 그다음 데스가 적은 쪽이 앞이고, 뛴 라운드가 없어 전투점수를 셀 수 없는 사람은 맨 뒤입니다.
 * 데스매치는 게임처럼 킬 순 등수([standings])라 MVP가 없습니다.
 * 팀 데스매치와 건틀릿처럼 라운드가 없는 팀 모드는 전투점수를 라운드로 나눌 수 없어 빈 `Map`입니다.
 */
fun Match.placements(): Map<PlayerId, MatchPlacement> = when (format) {
    MatchFormat.ROUNDS -> roundPlacements()
    MatchFormat.FREE_FOR_ALL -> standings().flatMap { team ->
        team.members.map { it.player to MatchPlacement(rank = team.rank, players = players.size, award = null) }
    }.toMap()
    MatchFormat.TEAM_POINTS, MatchFormat.TEAM_PLACEMENT -> emptyMap()
}

private fun Match.roundPlacements(): Map<PlayerId, MatchPlacement> {
    val ordered = players.sortedWith(
        compareByDescending<Scoreline> { it.acs ?: Double.NEGATIVE_INFINITY }
            .thenByDescending { it.kills }
            .thenBy { it.deaths },
    )
    val mvp = ordered.firstOrNull()
    val teamMvp = mvp?.let { best -> ordered.firstOrNull { it.onMyTeam != best.onMyTeam } }
    return ordered.withIndex().associate { (index, line) ->
        val award = when (line) {
            mvp -> MatchAward.MVP
            teamMvp -> MatchAward.TEAM_MVP
            else -> null
        }
        line.player to MatchPlacement(rank = index + 1, players = ordered.size, award = award)
    }
}

/**
 * 라운드당 전투점수입니다.
 * 라운드가 없는 모드는 응답의 `roundsPlayed`가 어떻게 오는지 몰라 나누지 않고 `null`입니다.
 * 1로 오면 경기 전체 점수가 라운드당 값처럼 뜹니다.
 */
fun Match.acsOf(line: Scoreline): Double? = if (format == MatchFormat.ROUNDS) line.acs else null

/** 내 자리입니다. 스코어보드에 내가 없으면 `null`입니다. */
val Match.myPlacement: MatchPlacement?
    get() = placements()[me]
