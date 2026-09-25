package com.ovalit.core.model

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/**
 * @property statsPublic 끄면 친구도 내 리포트와 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property focus S0-4에서 고른 관심사입니다. 달라진 점의 순서를 정할 때 씁니다.
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
