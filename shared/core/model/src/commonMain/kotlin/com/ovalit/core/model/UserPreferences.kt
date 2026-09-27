package com.ovalit.core.model

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/**
 * @property statsPublic 끄면 친구도 내 리포트와 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property notifyWeeklyReport 월요일 오전 주간 리포트 알림입니다. 보낼 길이 생길 때까지 설정에 스위치를 두지 않아서
 * 기본값 그대로입니다. 알림을 만들면 설정에 다시 둡니다.
 * @property focus S0-4와 설정에서 고른 관심사입니다. 동적 칸 순서와 개선 포인트 문장을 고를 때 씁니다.
 */
data class UserPreferences(
    val theme: ThemePreference,
    val defaultQueue: QueueFilter,
    val statsPublic: Boolean,
    val notifyAnalysisDone: Boolean,
    val notifyWeeklyReport: Boolean,
    val focus: Focus,
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
