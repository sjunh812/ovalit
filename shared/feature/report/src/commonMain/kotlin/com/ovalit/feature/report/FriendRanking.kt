package com.ovalit.feature.report

import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import com.ovalit.core.ui.format

/** 홈 친구 비교 카드에 두는 줄 수입니다. 친구가 수백 명이어도 카드는 이만큼만 그리고 나머지는 전체 순위 화면에서 봅니다. */
internal const val HOME_RANKING_ROWS = 5

/**
 * @property id 친구면 그 친구, 내 줄이면 `null`입니다.
 *   이름은 태그를 떼서 겹칠 수 있어 목록 키로 이걸 씁니다.
 * @property rank 보이는 자릿수로 같은 값이면 같은 등수입니다.
 */
internal data class RankedEntry(val id: PlayerId?, val name: String, val value: Double, val rank: Int) {
    val isMe: Boolean get() = id == null
}

/**
 * 나와 [friends]를 [metric] 값이 큰 순서로 세웁니다.
 * 서로 수락한 친구끼리만 세우고, 그 기간에 경기가 없는 친구는 뺍니다.
 *
 * 보이는 자릿수로 같은 값이면 같은 등수이고 그 안에서는 내가 맨 앞입니다.
 * 그래서 내 등수가 n이면 나는 늘 n번째 줄 안에 있습니다.
 *
 * @param me 내 줄에 쓸 이름("나")입니다.
 */
internal fun rankFriends(me: String, mine: MatchMetrics, friends: List<FriendStanding>, metric: FixedMetric): List<RankedEntry> {
    val format = metric.format
    val candidates = buildList {
        metric.value(mine)?.let { add(RankedEntry(id = null, name = me, value = it, rank = 0)) }
        friends.forEach { friend ->
            friend.metrics?.let(metric.value)?.let { add(RankedEntry(friend.id, friend.riotId.substringBefore('#'), value = it, rank = 0)) }
        }
    }
    val sorted = candidates.sortedWith(
        compareByDescending<RankedEntry> { format.steps(it.value) }.thenByDescending { it.isMe }.thenByDescending { it.value },
    )
    // 값이 큰 순서로 늘어서 있어서 보이는 값이 같은 줄은 붙어 있다. 바로 앞 줄과 견주면 한 번에 등수를 매긴다.
    var rank = 0
    var previous: Int? = null
    return sorted.mapIndexed { index, entry ->
        val steps = format.steps(entry.value)
        if (steps != previous) {
            rank = index + 1
            previous = steps
        }
        entry.copy(rank = rank)
    }
}

/**
 * 홈 카드에 둘 줄입니다.
 * 위 [HOME_RANKING_ROWS]줄을 두고, 내가 그 밖이면 내 줄을 [mine]에 따로 둡니다.
 *
 * @property total 줄을 세운 사람 수이고 나도 들어 있습니다.
 * @property skipsRows 위 줄과 내 줄 사이에 빠진 줄이 있는지입니다.
 *   있으면 그 사이를 띄워 내 줄이 바로 다음 등수처럼 읽히지 않게 합니다.
 *   내가 바로 다음 줄이면 띄우지 않습니다.
 */
internal class RankingPreview(val top: List<RankedEntry>, val mine: RankedEntry?, val total: Int, val skipsRows: Boolean) {
    val hasMore: Boolean get() = total > top.size + (if (mine != null) 1 else 0)
}

internal fun List<RankedEntry>.preview(rows: Int = HOME_RANKING_ROWS): RankingPreview {
    val myIndex = indexOfFirst { it.isMe }
    return RankingPreview(
        top = take(rows),
        mine = getOrNull(myIndex)?.takeIf { myIndex >= rows },
        total = size,
        skipsRows = myIndex > rows,
    )
}
