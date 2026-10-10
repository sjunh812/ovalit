package com.ovalit.core.data

import kotlin.coroutines.cancellation.CancellationException

/**
 * 어떤 화면과 기능을 쓰는지 세는 곳입니다. 안드로이드는 Firebase Analytics로 보내고, 끄는 스위치 없이 늘 보냅니다(CLAUDE.md 비용).
 * iOS와 Firebase 설정 파일이 없는 빌드는 보내지 않습니다([NoAnalytics]).
 *
 * 값에는 Riot이 준 것(PUUID, Riot ID, 친구 이름, 경기·ㅇㅂㅇ ID, 티어, 요원·맵·무기, 지표 값)을 넣지 않습니다.
 * Riot 약관은 게임 정보를 허락받지 않은 곳으로 내보내지 말라고 합니다.
 * 이름과 값은 아래처럼 정해 둔 글자만 씁니다.
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

/**
 * 이름을 바꾸면 콘솔의 지난 기록과 이어지지 않습니다.
 * Firebase는 이벤트 이름에 영문, 숫자, 밑줄만 받아서 뜻은 여기와 `docs/RELEASE.md`의 이벤트 표에 한국어로 적습니다.
 * 화면을 연 것은 이벤트 대신 [Analytics.screen]이 보냅니다.
 */
object AnalyticsEvents {
    /**
     * 새로 끝난 경기를 받으려 했습니다.
     * `source`는 당긴 곳("home", "matches")이나 앱을 다시 열어 저절로 받은 것("app_open")입니다.
     * `result`는 받은 게 있으면 "new", 없으면 "none", 실패하면 "error"입니다.
     * `new_bucket`은 받은 수의 구간입니다([newMatchesBucket]).
     */
    const val REFRESH = "refresh"

    /**
     * 연동 뒤 첫 수집을 마쳤습니다. 온보딩을 끝까지 간 사람 수입니다.
     * `match_bucket`은 받은 경기 수의 구간("0", "1-9", "10-29", "30-50")입니다.
     * `finished_in_background`는 앱을 닫은 채로 마쳤는지("true", "false")입니다.
     * Firebase가 미리 정한 이름이라 온보딩 깔때기 보고서에 바로 잡힙니다.
     */
    const val TUTORIAL_COMPLETE = "tutorial_complete"

    /**
     * 친구 요청을 보내거나 받은 요청에 답했습니다.
     * `action`은 "send", "accept", "decline"이고, `source`는 누른 곳("scoreboard", "friends_tab")입니다.
     */
    const val FRIEND_REQUEST = "friend_request"

    /** 오발있?으로 파티 모집을 보냈습니다. `friend_count`는 부른 친구 수입니다. */
    const val PING_SEND = "ping_send"

    /**
     * 받은 파티 모집에 답했습니다.
     * `answer`는 "yes", "no", "other_time"(다른 시간 제안)입니다.
     * `via`는 앱에서 답했는지("app") 알림 버튼으로 답했는지("notification")입니다.
     */
    const val PING_REPLY = "ping_reply"

    /**
     * 초대 링크를 공유 창으로 보냈습니다. 받은 쪽이 실제로 보냈는지는 모릅니다.
     * `method`는 "invite_link", `content_type`은 "invite"입니다. Firebase가 미리 정한 이름입니다.
     */
    const val SHARE = "share"

    /**
     * 보상형 광고를 끝까지 봐서 24시간 광고 없이 보기를 시작했습니다.
     * `entry`는 광고 줄의 ×에서 왔는지("ad_row") 설정에서 왔는지("settings")입니다.
     */
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
