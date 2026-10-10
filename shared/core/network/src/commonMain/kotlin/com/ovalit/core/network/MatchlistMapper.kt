package com.ovalit.core.network

import com.ovalit.core.model.MatchId
import com.ovalit.core.model.Queue
import com.ovalit.core.network.dto.MatchlistDto
import kotlin.time.Instant

/**
 * 경기 ID 목록의 한 줄입니다. 상세를 받기 전에 8주 밖의 경기와 커스텀 게임을 거를 때 씁니다.
 *
 * @property queue 목록에 큐가 빠져 있으면 `null`입니다. 그때는 상세를 받아 [toMatch]가 가립니다.
 */
data class MatchlistEntry(
    val id: MatchId,
    val queue: Queue?,
    val startedAt: Instant,
)

/**
 * 경기 ID 목록을 옮깁니다. 커스텀 게임(빈 `queueId`)은 상세를 받지도 저장하지도 않아서 여기서 뺍니다. 연습과 내전은 내 실력
 * 흐름과 상관없고, 받으면 레이트 리밋만 씁니다.
 *
 * @throws RiotResponseFormatException 경기 ID나 시작 시각이 빠진 줄이 있으면 던집니다. 시작 시각이 없으면 8주 밖인지 가릴 수
 * 없습니다.
 */
fun MatchlistDto.toEntries(): List<MatchlistEntry> = history.mapIndexedNotNull { index, entry ->
    val id = entry.matchId?.takeIf { it.isNotBlank() } ?: throw RiotResponseFormatException("$.history[$index].matchId")
    val startedAt = entry.gameStartTimeMillis ?: throw RiotResponseFormatException("$.history[$index].gameStartTimeMillis")
    val queue = entry.queueId?.let { Queue.fromRiot(it) ?: return@mapIndexedNotNull null }
    MatchlistEntry(id = MatchId(id), queue = queue, startedAt = Instant.fromEpochMilliseconds(startedAt))
}
