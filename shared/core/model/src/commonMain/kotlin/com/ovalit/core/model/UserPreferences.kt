package com.ovalit.core.model

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/** @property statsPublic 끄면 친구도 내 리포트와 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다. */
data class UserPreferences(
    val theme: ThemePreference,
    val defaultQueue: QueueFilter,
    val statsPublic: Boolean,
    val notifyAnalysisDone: Boolean,
    val notifyWeeklyReport: Boolean,
) {
    companion object {
        val Default = UserPreferences(
            theme = ThemePreference.SYSTEM,
            defaultQueue = QueueFilter.COMPETITIVE_AND_UNRATED,
            statsPublic = true,
            notifyAnalysisDone = true,
            notifyWeeklyReport = true,
        )
    }
}
