package com.ovalit.core.model

import kotlin.time.Instant

/**
 * 경기 ID 목록(`matchlists/by-puuid`)의 한 줄입니다. 상세를 받기 전에 큐와 시작 시각만 보고 받을 경기를 고릅니다.
 *
 * @property queue 목록의 `queueId`를 [Queue.fromRiot]으로 옮긴 값입니다. 커스텀 게임이면 `null`이고 받지 않습니다.
 */
data class MatchListEntry(
    val id: MatchId,
    val startedAt: Instant,
    val queue: Queue?,
)
