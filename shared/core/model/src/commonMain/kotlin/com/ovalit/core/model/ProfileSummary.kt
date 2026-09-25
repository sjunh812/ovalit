package com.ovalit.core.model

/**
 * 내 프로필 위쪽의 이번 액트 요약입니다. [currentActMatches]로 추린 경쟁 + 일반 경기를 넘깁니다. 모두 내
 * 경기끼리만 셉니다.
 *
 * @property mostKills 한 경기에서 낸 가장 많은 킬입니다. 판당 K/D/A와 같은 규칙으로 셉니다.
 * @property playTimeMillis 경기 길이를 더한 값입니다. 중간에 나갔다 들어온 경기도 경기 전체 길이로 셉니다.
 * @property competitive 경쟁전을 한 판도 안 뛰었으면 `null`입니다.
 */
data class ProfileSummary(
    val metrics: MatchMetrics,
    val mostKills: Int?,
    val playTimeMillis: Long,
    val competitive: CompetitiveRecord?,
)

/**
 * @property tiers 경쟁전마다 응답에 실려 온 내 티어를 치른 순서대로 담았습니다. 티어가 빠진 판은 건너뜁니다.
 * RR은 API에 없어서 티어가 바뀐 판에서만 흐름이 꺾입니다.
 */
data class CompetitiveRecord(
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val tiers: List<Int>,
) {
    /** 비긴 판은 분모에 넣지 않습니다. */
    val winRate: Double? get() = wins over (wins + losses)

    val currentTier: Int? get() = tiers.lastOrNull()
}

fun List<Match>.profileSummary(): ProfileSummary {
    val perMatch = map { it.metrics() }
    val competitive = filter { it.queue == Queue.COMPETITIVE }.sortedBy { it.startedAt }
    return ProfileSummary(
        metrics = perMatch.sum(),
        mostKills = perMatch.maxOfOrNull { it.kills },
        playTimeMillis = sumOf { it.lengthMillis },
        competitive = competitive.takeIf { it.isNotEmpty() }?.let { games ->
            CompetitiveRecord(
                matches = games.size,
                wins = games.count { it.myTeamWon == true },
                losses = games.count { it.myTeamWon == false },
                tiers = games.mapNotNull { it.myScoreline?.tier },
            )
        },
    )
}
