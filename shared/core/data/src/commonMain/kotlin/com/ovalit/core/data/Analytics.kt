package com.ovalit.core.data

import kotlin.coroutines.cancellation.CancellationException

/**
 * 어떤 화면과 기능을 쓰는지 세는 곳입니다. 안드로이드는 Firebase Analytics로 보내고, 사용자가 설정에서 "사용 통계 보내기"를
 * 끄면 보내지 않습니다.
 *
 * 값에는 Riot이 준 것(PUUID, Riot ID, 친구 이름, 경기·ㅇㅂㅇ ID, 티어, 요원·맵·무기, 지표 값)을 넣지 않습니다. Riot 약관은 게임
 * 정보를 허락받지 않은 곳으로 내보내지 말라고 합니다. 이름과 값은 아래처럼 정해 둔 글자만 씁니다.
 */
interface Analytics {
    fun log(event: String, params: Map<String, String> = emptyMap())

    /** 화면을 열었다고 적습니다. [name]은 화면마다 정해 둔 글자입니다("report", "match_detail"). */
    fun screen(name: String)
}

/** 아무것도 보내지 않습니다. Firebase가 없을 때와 테스트가 씁니다. */
object NoAnalytics : Analytics {
    override fun log(event: String, params: Map<String, String>) = Unit

    override fun screen(name: String) = Unit
}

/** 보내는 이벤트 이름입니다. 이름을 바꾸면 콘솔의 지난 기록과 이어지지 않습니다. */
object AnalyticsEvents {
    const val REFRESH = "refresh"
    const val TUTORIAL_COMPLETE = "tutorial_complete"
    const val FRIEND_REQUEST = "friend_request"
    const val PING_SEND = "ping_send"
    const val PING_REPLY = "ping_reply"
    const val SHARE = "share"
    const val AD_FREE_START = "ad_free_start"
}

/** 받은 새 경기 수를 몇 구간으로 줄입니다. 정확한 수는 쓸모가 적고 구간이면 진행 줄을 띄운 비율을 볼 수 있습니다. */
fun newMatchesBucket(count: Int): String = when {
    count <= 0 -> "0"
    count < 5 -> "1-4"
    count < 20 -> "5-19"
    else -> "20+"
}

/** 새 경기 받기를 [block]으로 하고 결과를 적습니다. [source]는 당긴 곳("home", "matches")이나 "app_open"입니다. */
suspend fun logRefresh(analytics: Analytics, source: String, block: suspend () -> Int): Int {
    val received = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        analytics.log(AnalyticsEvents.REFRESH, mapOf("source" to source, "result" to "error"))
        throw e
    }
    analytics.log(
        AnalyticsEvents.REFRESH,
        mapOf("source" to source, "result" to if (received > 0) "new" else "none", "new_bucket" to newMatchesBucket(received)),
    )
    return received
}
