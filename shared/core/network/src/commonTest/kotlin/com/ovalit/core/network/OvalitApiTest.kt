package com.ovalit.core.network

import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class OvalitApiTest {

    @Test
    fun `세션이 있으면 Bearer로 보내고 본문을 문자열 그대로 돌려준다`() = runTest {
        var authorization: String? = null
        val api = api(token = "session-token") { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond("""{"matchId":"m1"}""", HttpStatusCode.OK)
        }

        assertEquals("""{"matchId":"m1"}""", api.get("/riot/matches/m1"))
        assertEquals("Bearer session-token", authorization)
    }

    @Test
    fun `연동 전이면 세션 헤더를 붙이지 않는다`() = runTest {
        var authorization: String? = "unset"
        val api = api(token = null) { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK)
        }

        api.get("/content/tiers")
        assertNull(authorization)
    }

    // contracts/server-api.md 공통
    @Test
    fun `서버 실패는 까닭에 맞는 OvalitError로 옮긴다`() = runTest {
        val cases = listOf(
            HttpStatusCode.Unauthorized to "unauthorized" to OvalitError.SessionExpired,
            HttpStatusCode.ServiceUnavailable to "riot_rate_limited" to OvalitError.RiotBusy,
            HttpStatusCode.TooManyRequests to "too_many_requests" to OvalitError.RiotBusy,
            HttpStatusCode.BadGateway to "riot_unavailable" to OvalitError.RiotDown,
            HttpStatusCode.NotFound to "not_found" to OvalitError.Unknown,
            HttpStatusCode.InternalServerError to "internal" to OvalitError.Unknown,
        )
        cases.forEach { (response, expected) ->
            val (status, code) = response
            val api = api { respond("""{"error":"$code"}""", status, JsonHeaders) }
            val failure = assertFailsWith<OvalitException> { api.get("/riot/matchlist") }
            assertEquals(expected, failure.error, "$status $code")
        }
    }

    @Test
    fun `연결이 끊기면 인터넷 끊김이다`() = runTest {
        val api = api { throw IOException("Failed to connect to $BaseUrl/friends/$Puuid/matchlist") }

        val failure = assertFailsWith<OvalitException> { api.get("/friends/$Puuid/matchlist") }
        assertEquals(OvalitError.Offline, failure.error)
    }

    // 이 예외가 비정상 종료 보고로 나가면 Riot이 준 값이 같이 나간다(CLAUDE.md 지켜야 할 선, T082).
    // 경로의 친구 PUUID와 경기 ID, 응답 본문의 이름이 예외 문구와 원인 어디에도 남지 않아야 한다.
    @Test
    fun `실패 예외에는 응답 본문과 요청 경로의 Riot 값이 남지 않는다`() = runTest {
        val failures = listOf(
            api { respond("""{"error":"stats_private","gameName":"$RiotName","puuid":"$Puuid"}""", HttpStatusCode.Forbidden, JsonHeaders) },
            api { respond("<html>$RiotName $Puuid</html>", HttpStatusCode.BadGateway) },
            api { throw IOException("Connect timeout [url=$BaseUrl/friends/$Puuid/matches/$MatchId]") },
            api { throw IllegalStateException("Text: \"$RiotName $Puuid\"") },
        ).map { api -> assertFailsWith<OvalitException> { api.get("/friends/$Puuid/matches/$MatchId") } }

        failures.forEach { failure ->
            val chain = generateSequence<Throwable>(failure) { it.cause }.toList()
            chain.forEach { thrown ->
                val text = thrown.toString() + thrown.message.orEmpty()
                listOf(Puuid, MatchId, RiotName).forEach { value -> assertFalse(value in text, "예외에 $value 가 남았다: $text") }
            }
            assertNull(failure.cause)
        }
    }

    @Test
    fun `성공 응답을 읽다 실패한 경기도 원문 없이 던진다`() = runTest {
        val api = api { respond("""{"matchInfo":{"matchId":"$MatchId"},"players":"$RiotName"}""", HttpStatusCode.OK) }

        val failure = assertFailsWith<RiotResponseFormatException> { decodeMatch(api.get("/riot/matches/$MatchId")) }
        listOf(Puuid, MatchId, RiotName).forEach { value -> assertFalse(value in failure.toString()) }
        assertNull(failure.cause)
    }

    private fun api(
        token: String? = "session-token",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): OvalitApi = OvalitApi(HttpClient(MockEngine(handler)), BaseUrl, session = { token })
}

private const val BaseUrl = "https://ovalit.example"
private const val Puuid = "aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_-aBcD"
private const val MatchId = "0f2c0b6a-6f0e-4b2a-9a51-2b4e8a7e3f11"
private const val RiotName = "봉봉이#KR1"
private val JsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
