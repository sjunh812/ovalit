package com.ovalit.core.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 서로 수락한 친구입니다. Riot 정책상 데이터 공유에 동의한 사람만 다른 사람에게 보여줄 수 있어서,
 * 친구는 우리 앱에 연동한 사람끼리만 맺어집니다.
 *
 * @property matches 친구 본인의 경기입니다. 전적을 비공개로 바꾸기 전에 받아 둔 경기가 남아 있을 수 있습니다.
 * [statsPublic]이 `false`면 여기 든 경기를 보여주지 않습니다. 같이 한 경기는 내 경기에서 셉니다.
 */
data class Friend(
    val id: PlayerId,
    val riotId: String,
    val playerCard: PlayerCardId?,
    val statsPublic: Boolean,
    val matches: List<Match>,
)

/** 친구 요청은 같이 뛴 경기의 스코어보드나 초대 링크로만 옵니다. 닉네임 검색은 없습니다. */
data class FriendRequest(
    val id: PlayerId,
    val riotId: String,
    val playerCard: PlayerCardId?,
    val source: FriendRequestSource,
)

enum class FriendRequestSource {
    SCOREBOARD,
    INVITE_LINK,
}

/** S5 머리 바로 밑의 "같이 한 경기 12경기 8승 4패"입니다. 그 친구가 우리 팀이었던 내 경기만 셉니다. */
data class SharedRecord(
    val matches: Int,
    val wins: Int,
    val losses: Int,
)

fun List<Match>.sharedWith(friend: PlayerId): SharedRecord {
    val together = filter { friend in it.allies }
    return SharedRecord(
        matches = together.size,
        wins = together.count { it.myTeamWon == true },
        losses = together.count { it.myTeamWon == false },
    )
}

/**
 * 내 리포트와 같은 기간, 같은 액트, 같은 큐로 센 친구의 합계입니다. 라이벌 대결과 친구 비교에서 나와 친구를 나란히
 * 놓으려면 같은 달력 구간을 봐야 합니다. 전적을 공개하지 않았거나 그 기간에 경기가 없으면 `null`입니다.
 */
fun Friend.metricsIn(report: WeeklyReport.Ready, queueFilter: QueueFilter, timeZone: TimeZone): MatchMetrics? {
    if (!statsPublic) return null
    val period = report.period
    val inPeriod = matches.filter { match ->
        val day = match.startedAt.toLocalDateTime(timeZone).date
        match.act == report.act && match.queue in queueFilter.queues && day >= period.firstDay && day <= period.lastDay
    }
    if (inPeriod.isEmpty()) return null
    return inPeriod.map { it.metrics() }.sum()
}
