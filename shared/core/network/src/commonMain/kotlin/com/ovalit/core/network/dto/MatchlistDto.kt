package com.ovalit.core.network.dto

import kotlinx.serialization.Serializable

// VAL-MATCH-V1 `/val/match/v1/matchlists/by-puuid/{puuid}`의 응답이다. 우리 서버의 `/riot/matchlist`가 그대로 넘겨준다.

@Serializable
data class MatchlistDto(
    val puuid: String? = null,
    val history: List<MatchlistEntryDto> = emptyList(),
)

/** @property queueId 커스텀 게임이면 빈 값입니다. 상세를 받기 전에 커스텀 게임을 거를 수 있습니다. */
@Serializable
data class MatchlistEntryDto(
    val matchId: String? = null,
    val gameStartTimeMillis: Long? = null,
    val queueId: String? = null,
)
