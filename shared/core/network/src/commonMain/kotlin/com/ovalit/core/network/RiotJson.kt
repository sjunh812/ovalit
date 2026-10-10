package com.ovalit.core.network

import com.ovalit.core.network.dto.MatchDto
import com.ovalit.core.network.dto.MatchlistDto
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * Riot 응답을 읽는 [Json]입니다. 우리 서버가 Riot 응답을 그대로 넘겨주니 서버 응답도 이걸로 읽습니다.
 *
 * Riot은 패치마다 필드를 더하고 모드에 따라 필드를 빼서, 모르는 필드는 건너뛰고 null이 온 목록은 빈 목록으로 읽습니다.
 *
 * Ktor `ContentNegotiation`에 그대로 걸지 않습니다.
 * 읽다 실패하면 kotlinx.serialization이 응답 원문 일부를 예외 문구에 담습니다.
 * 그 예외가 비정상 종료 보고로 나가면 PUUID와 이름이 Riot 약관이 허락하지 않은 곳으로 갑니다(CLAUDE.md 지켜야 할 선).
 * 그래서 본문을 문자열로 받아 [decodeMatch], [decodeMatchlist]로 읽습니다.
 */
val RiotJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/** 경기 상세를 읽습니다. 모양이 어긋나면 응답 원문을 담지 않은 [RiotResponseFormatException]을 던집니다. */
fun decodeMatch(text: String): MatchDto = decodeRiot(MatchDto.serializer(), text)

/** 경기 ID 목록을 읽습니다. 모양이 어긋나면 응답 원문을 담지 않은 [RiotResponseFormatException]을 던집니다. */
fun decodeMatchlist(text: String): MatchlistDto = decodeRiot(MatchlistDto.serializer(), text)

/**
 * Riot 응답을 읽지 못했습니다.
 * 문구에는 어느 필드에서 걸렸는지(`$.players[0].stats`)만 담고 응답 원문은 담지 않습니다.
 * 원래 예외도 원문을 담고 있어서 [cause]로 잇지 않습니다.
 */
class RiotResponseFormatException(val path: String?) :
    IllegalStateException("Riot 응답을 읽지 못했습니다" + (path?.let { " ($it)" } ?: ""))

// SerializationException도 IllegalArgumentException이라 여기서 같이 잡힌다
private fun <T> decodeRiot(strategy: DeserializationStrategy<T>, text: String): T = try {
    RiotJson.decodeFromString(strategy, text)
} catch (e: IllegalArgumentException) {
    throw RiotResponseFormatException(e.jsonPath())
}

// 경로는 DTO 필드 이름과 목록 위치로만 이뤄져 Riot 값이 섞이지 않는다. DTO에 Map을 두면 키가 PUUID일 수 있으니 그때 다시 본다.
private val JsonPath = Regex("""at path: (\$[\w.\[\]]*)""")

private fun Throwable.jsonPath(): String? = message?.let { JsonPath.find(it)?.groupValues?.get(1) }
