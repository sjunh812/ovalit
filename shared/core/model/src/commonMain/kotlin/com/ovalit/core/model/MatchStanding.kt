package com.ovalit.core.model

/**
 * 등수로 끝나는 경기([MatchFormat.FREE_FOR_ALL], [MatchFormat.TEAM_PLACEMENT])에서 한 팀의 자리입니다. 데스매치는 한 사람이
 * 한 팀입니다. 게임이 그 판 안에서 매긴 등수라 우리가 만든 분포에서의 등수가 아닙니다(CLAUDE.md 지켜야 할 선).
 *
 * @property rank 1부터 셉니다. 점수가 같으면 같은 등수입니다.
 * @property members 팀 안에서 킬이 많은 순입니다.
 */
data class TeamStanding(
    val rank: Int,
    val points: Int,
    val members: List<Scoreline>,
)

/** 내 등수입니다. [teams]는 데스매치면 사람 수, 건틀릿이면 팀 수입니다. */
data class Standing(
    val rank: Int,
    val teams: Int,
)

/**
 * 등수 순으로 늘어놓은 팀입니다. 두 팀이 겨루는 모드면 빈 목록입니다.
 *
 * 응답이 등수를 주면 그대로 쓰고, 안 주면 팀 점수(`numPoints`)가 높은 순입니다. 데스매치는 점수가 곧 킬이라 `teams[]`가 비어
 * 있으면 스코어보드의 킬로 셉니다.
 */
fun Match.standings(): List<TeamStanding> {
    if (format != MatchFormat.FREE_FOR_ALL && format != MatchFormat.TEAM_PLACEMENT) return emptyList()
    val lines = players.associateBy { it.player }
    val sides = teams.ifEmpty { players.map { MatchTeam(members = setOf(it.player), won = null, points = it.kills) } }
    val unranked = sides.map { team ->
        val members = team.members.mapNotNull { lines[it] }.sortedByDescending { it.kills }
        TeamStanding(rank = team.placement ?: 0, points = team.points ?: members.sumOf { it.kills }, members = members)
    }
    return unranked
        .map { team -> if (team.rank > 0) team else team.copy(rank = 1 + unranked.count { it.points > team.points }) }
        .sortedWith(compareBy<TeamStanding> { it.rank }.thenByDescending { it.points })
}

/** 등수로 끝나는 경기에서 우리 팀(데스매치는 나)의 등수입니다. 두 팀이 겨루는 모드거나 스코어보드에 내가 없으면 `null`입니다. */
val Match.myStanding: Standing?
    get() {
        val standings = standings()
        val mine = standings.firstOrNull { team -> team.members.any { it.player == me } } ?: return null
        return Standing(rank = mine.rank, teams = standings.size)
    }
