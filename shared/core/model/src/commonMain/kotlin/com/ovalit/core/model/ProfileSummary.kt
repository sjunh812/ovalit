package com.ovalit.core.model

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * 프로필(내 프로필, S5)의 통계 칸에 쓰는 이번 액트 합계입니다.
 * [currentActMatches]로 추린 경기를 넘깁니다.
 *
 * @property mostKills 한 경기에서 낸 가장 많은 킬입니다.
 *   판당 K/D/A와 같은 규칙으로 셉니다.
 * @property playTimeMillis 경기 길이를 더한 값입니다.
 *   중간에 나갔다 들어온 경기도 경기 전체 길이로 셉니다.
 */
data class ProfileSummary(
    val metrics: MatchMetrics,
    val mostKills: Int?,
    val playTimeMillis: Long,
    val highlights: HighlightCount = HighlightCount(aces = 0, clutches = 0, clutchAttempts = 0),
)

/**
 * @property currentTier [latestTier]와 같은 규칙으로 고른 이번 액트 티어입니다.
 *   이번 액트 경쟁전에 티어가 한 판도 없으면 `null`입니다.
 */
data class CompetitiveRecord(
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val currentTier: Int?,
) {
    /** 비긴 판은 분모에 넣지 않습니다. */
    val winRate: Double? get() = wins over (wins + losses)
}

/**
 * 프로필(내 프로필, S5)에 넣는 이번 액트 성적입니다.
 * 머리의 경기 수와 역할, 티어 카드는 큐 칩과 상관없이 경쟁 + 일반([QueueFilter.PROFILE])으로 셉니다.
 * 통계, 맞힌 부위, 요원, 무기는 칩이 고른 [queueFilter]로 셉니다.
 *
 * @property actMatches 머리의 "이번 액트 42경기"입니다.
 * @property mainRole 머리의 역할입니다.
 *   칩을 바꿔도 머리가 흔들리지 않게 경쟁 + 일반으로 정합니다.
 * @property competitive 티어 카드입니다.
 *   이번 액트에 경쟁전을 한 판도 안 뛰었으면 `null`입니다.
 */
data class ProfileReport(
    val queueFilter: QueueFilter,
    val actMatches: Int,
    val mainRole: Role?,
    val mainRoleShare: Double?,
    val competitive: CompetitiveRecord?,
    val summary: ProfileSummary,
    val agents: AgentReport,
    val weapons: WeaponReport,
) {
    /** 고른 큐에 이번 액트 경기가 없어 통계부터 무기까지 대신 한 줄만 둘 때입니다. */
    val isQueueEmpty: Boolean get() = agents.matches == 0
}

fun List<Match>.profileSummary(): ProfileSummary {
    val perMatch = map { it.metrics() }
    return ProfileSummary(
        metrics = perMatch.sum(),
        mostKills = perMatch.maxOfOrNull { it.kills },
        playTimeMillis = sumOf { it.lengthMillis },
        highlights = highlightCount(),
    )
}

/** 넘긴 경기 중 경쟁전의 판 수와 승패, 지금 티어입니다. 경쟁전이 없으면 `null`입니다. */
fun List<Match>.competitiveRecord(): CompetitiveRecord? {
    val games = filter { it.queue == Queue.COMPETITIVE }.sortedBy { it.startedAt }
    if (games.isEmpty()) return null
    return CompetitiveRecord(
        matches = games.size,
        wins = games.count { it.myTeamWon == true },
        losses = games.count { it.myTeamWon == false },
        currentTier = games.latestTier(),
    )
}

/**
 * 내 경기나 친구 경기 전체를 받아 프로필 칸을 셉니다.
 * 이번 액트는 칩과 상관없이 경쟁 + 일반의 가장 최근 경기로 정합니다([currentActMatches]).
 */
fun List<Match>.profileReport(queueFilter: QueueFilter, now: Instant, timeZone: TimeZone): ProfileReport {
    val act = currentActMatches(QueueFilter.PROFILE)
    val roles = act.agentReport()
    val selected = if (queueFilter == QueueFilter.PROFILE) act else currentActMatches(queueFilter)
    return ProfileReport(
        queueFilter = queueFilter,
        actMatches = act.size,
        mainRole = roles.mainRole,
        mainRoleShare = roles.mainRoleShare,
        competitive = act.competitiveRecord(),
        summary = selected.profileSummary(),
        agents = if (queueFilter == QueueFilter.PROFILE) roles else selected.agentReport(),
        weapons = weaponReport(now = now, timeZone = timeZone, queueFilter = queueFilter),
    )
}
