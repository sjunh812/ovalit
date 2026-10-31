package com.ovalit.core.network

import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 우리 서버를 부르는 HTTP 클라이언트입니다(specs/001-ovalit-baseline/contracts/server-api.md).
 * 세션은 `Authorization: Bearer`로 보내고 본문은 문자열로 돌려줍니다.
 * 본문을 읽는 건 부르는 쪽이 [decodeMatch]처럼 직접 합니다.
 * Ktor `ContentNegotiation`에 걸면 읽다 실패한 예외가 응답 원문을 담습니다.
 *
 * 실패하면 [OvalitException]을 던집니다.
 * 예외 문구와 원인에는 응답 본문과 요청 경로를 담지 않습니다.
 * 경로에는 친구 PUUID와 경기 ID가 들어가서 그 예외가 비정상 종료 보고로 나가면 Riot이 준 값이 같이 나갑니다(CLAUDE.md 지켜야 할 선).
 *
 * @param baseUrl 서버 주소입니다.
 *   끝에 `/`를 붙이지 않습니다.
 * @param session 지금 세션 토큰입니다.
 *   연동 전이면 `null`이고 헤더를 붙이지 않습니다.
 */
class OvalitApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val session: suspend () -> String?,
) {
    suspend fun get(path: String): String = send(HttpMethod.Get, path)

    suspend fun post(path: String, body: String? = null): String = send(HttpMethod.Post, path, body)

    suspend fun put(path: String, body: String? = null): String = send(HttpMethod.Put, path, body)

    suspend fun patch(path: String, body: String): String = send(HttpMethod.Patch, path, body)

    suspend fun delete(path: String, body: String? = null): String = send(HttpMethod.Delete, path, body)

    private suspend fun send(method: HttpMethod, path: String, body: String? = null): String {
        val token = session()
        val response = try {
            client.request(baseUrl + path) {
                this.method = method
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: HttpRequestTimeoutException) {
            // Ktor 예외 문구에는 요청 주소가 들어 있어 원인으로 잇지 않는다
            throw OvalitException(OvalitError.Offline)
        } catch (_: IOException) {
            throw OvalitException(OvalitError.Offline)
        } catch (_: Exception) {
            throw OvalitException(OvalitError.Unknown)
        }
        if (response.status.value in 200..299) return readBody(response)
        throw OvalitException(errorOf(response.status.value, errorCode(response)))
    }

    private suspend fun readBody(response: HttpResponse): String = try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        throw OvalitException(OvalitError.Offline)
    } catch (_: Exception) {
        throw OvalitException(OvalitError.Unknown)
    }

    // 실패 본문은 `{ "error": "<코드>" }`다.
    // 코드만 꺼내고 본문은 어디에도 남기지 않는다.
    private suspend fun errorCode(response: HttpResponse): String? = try {
        val json = RiotJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
        (json?.get("error") as? JsonPrimitive)?.takeIf { it.isString }?.content
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

/**
 * 서버 응답을 [OvalitError]로 옮깁니다(contracts/server-api.md 공통).
 * 사용자마다 Riot 호출 한도를 넘긴 429는 Riot이 붐빌 때와 같이 봅니다.
 * 실제 `MatchRemoteSource`를 붙이면 새 경기를 받다 이게 올 때 새 경기가 없는 것처럼 둡니다(T036).
 */
internal fun errorOf(status: Int, code: String?): OvalitError = when {
    status == 401 -> OvalitError.SessionExpired
    status == 503 && code == "riot_rate_limited" -> OvalitError.RiotBusy
    status == 429 -> OvalitError.RiotBusy
    status == 502 && code == "riot_unavailable" -> OvalitError.RiotDown
    else -> OvalitError.Unknown
}
