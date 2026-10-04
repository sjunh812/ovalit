package com.ovalit.core.model

import kotlin.time.Instant

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/** 오발있을 시작 몇 분 전에 알릴지입니다. 서버가 이 시간에 맞춰 보내서 [minutes]를 서버에 맡깁니다. 0은 받지 않습니다. */
enum class PingReminder(val minutes: Int) {
    TEN_MINUTES(10),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    OFF(0),
}

/**
 * @property statsPublic 끄면 친구도 내 리포트와 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property notifyWeeklyReport 월요일 오전 주간 리포트 알림입니다. 서버가 월요일 9시에 FCM 토픽으로 한 번 보내고, 끄면 토픽
 * 구독을 풉니다.
 * @property notifyPing 친구가 보낸 ㅇㅂㅇ과 내가 보낸 ㅇㅂㅇ의 답을 알릴지입니다.
 * @property pingReminder 내가 부르거나 간다고 답한 ㅇㅂㅇ을 시작 몇 분 전에 알릴지입니다.
 * @property focus S0-4와 설정에서 고른 관심사입니다. 동적 칸 순서와 개선 포인트 문장을 고를 때 씁니다.
 * @property seenProfileHint 홈 오른쪽 위에 "내 프로필은 여기서 볼 수 있어요"를 한 번 띄웠는지입니다. 고르는 설정이 아니라
 * 한 번만 띄우려고 적어 둡니다.
 * @property adFreeUntil 보상형 광고를 끝까지 보고 받은 "광고 없이 보기"가 끝나는 시각입니다. 그때까지 광고 자리가 비어
 * 있습니다. 한 번 보면 24시간이라 그동안은 다시 볼 수 없어 하루 한 번이 저절로 지켜집니다.
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
