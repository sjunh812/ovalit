package com.ovalit.core.model

import kotlin.time.Instant

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/** ㅇㅂㅇ 시작 몇 분 전에 알릴지입니다. 알림은 서버가 보내서 [minutes]를 서버에 넘깁니다. 0이면 알리지 않습니다. */
enum class PingReminder(val minutes: Int) {
    TEN_MINUTES(10),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    OFF(0),
}

/**
 * @property statsPublic 끄면 친구도 내 프로필과 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property notifyWeeklyReport 서버가 월요일 오전 9시에 FCM 토픽으로 보내는 주간 리포트 알림입니다. 끄면 토픽 구독을 풉니다.
 * @property notifyPing 친구가 보낸 ㅇㅂㅇ과 내가 보낸 ㅇㅂㅇ의 답을 알릴지입니다.
 * @property pingReminder 내가 부르거나 간다고 답한 ㅇㅂㅇ을 시작 몇 분 전에 알릴지입니다.
 * @property focus S0-4와 설정에서 고른 관심사입니다. 동적 칸 순서와 개선 포인트 문장을 고를 때 씁니다.
 * @property seenProfileHint 홈의 "내 프로필은 여기서 볼 수 있어요" 말풍선을 띄웠는지입니다. 사용자가 고르는 값이 아니라
 * 한 번만 띄우려고 적어 둡니다.
 * @property seenNotificationPrimer 친구 요청을 수락하거나 파티를 모집했을 때 "친구가 부르면 알려드릴까요?"를 띄웠는지입니다.
 * 알림을 꺼 둔 사람에게 한 번만 묻습니다.
 * @property askedNotificationPermission 안드로이드 시스템 알림 권한 창을 띄운 적이 있는지입니다. 띄운 적이 있는데 시스템이 다시
 * 물을 수 없다고 하면(두 번 거절) 창 대신 이 앱의 알림 설정 화면을 엽니다.
 * @property adFreeUntil 보상형 광고를 끝까지 보고 받은 "광고 없이 보기"가 끝나는 시각입니다. 그때까지 광고 자리를 모두
 * 비웁니다. 끝나기 전에는 다시 볼 수 없어서 하루 한 번 제한을 따로 두지 않습니다.
 */
data class UserPreferences(
    val theme: ThemePreference,
    val defaultQueue: QueueFilter,
    val statsPublic: Boolean,
    val notifyAnalysisDone: Boolean,
    val notifyWeeklyReport: Boolean,
    val focus: Focus,
    val notifyPing: Boolean = true,
    val pingReminder: PingReminder = PingReminder.TEN_MINUTES,
    val seenProfileHint: Boolean = false,
    val seenNotificationPrimer: Boolean = false,
    val askedNotificationPermission: Boolean = false,
    val adFreeUntil: Instant? = null,
) {
    companion object {
        val Default = UserPreferences(
            theme = ThemePreference.SYSTEM,
            defaultQueue = QueueFilter.COMPETITIVE_AND_UNRATED,
            statsPublic = true,
            notifyAnalysisDone = true,
            notifyWeeklyReport = true,
            focus = Focus.NONE,
        )
    }
}
