package com.ovalit.core.model

/** 한 요원으로 이만큼은 뛰어야 승률과 비율을 보여줍니다. 실데이터를 보고 조정할 시작값입니다. */
const val MIN_AGENT_MATCHES = 5

/**
 * S7 요원 화면에 쓰는 집계입니다. 액트 경계를 넘는 평균은 만들지 않으니 [currentActMatches]로
 * 이번 액트 경기만 추려서 넘깁니다.
 *
 * @property mainRole 라운드를 가장 많이 뛴 역할입니다. 홈의 "주로 타격대"와 같은 규칙입니다.
 * @property roles 라운드를 많이 뛴 역할 순서입니다.
 * @property agents 많이 뛴 요원 순서입니다.
 */
data class AgentReport(
    val matches: Int,
    val mainRole: Role?,
    val roles: List<RoleShare>,
    val agents: List<AgentStats>,
)

data class RoleShare(
    val role: Role,
    val matches: Int,
    val rounds: Int,
)

/**
 * @property decided 승패가 난 경기 수입니다. 비긴 경기는 승률 분모에 넣지 않습니다.
 */
data class AgentStats(
    val agent: AgentId,
    val role: Role?,
    val matches: Int,
    val wins: Int,
    val decided: Int,
    val metrics: MatchMetrics,
) {
    val winRate: Double? get() = wins over decided

    val isMeasurable: Boolean get() = matches >= MIN_AGENT_MATCHES
}

fun List<Match>.agentReport(): AgentReport {
    val roles = mapNotNull { match -> match.myRole?.let { it to match } }
        .groupBy({ it.first }, { it.second })
        .map { (role, matches) -> RoleShare(role, matches.size, matches.sumOf { it.rounds.size }) }
        .sortedByDescending { it.rounds }

    val agents = groupBy { it.myAgent }
        .map { (agent, matches) ->
            AgentStats(
                agent = agent,
                role = matches.firstNotNullOfOrNull { it.myRole },
                matches = matches.size,
                wins = matches.count { it.myTeamWon == true },
                decided = matches.count { it.myTeamWon != null },
                metrics = matches.map { it.metrics() }.sum(),
            )
        }
        .sortedByDescending { it.matches }

    return AgentReport(matches = size, mainRole = roles.firstOrNull()?.role, roles = roles, agents = agents)
}
