package com.ovalit.core.model

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT,
}

/**
 * @property statsPublic 끄면 친구도 내 리포트와 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property notifyWeeklyReport 월요일 오전 주간 리포트 알림입니다. 서버가 월요일 9시에 FCM 토픽으로 한 번 보내고, 끄면 토픽
 * 구독을 풉니다.
 * @property notifyPing 친구가 보낸 ㅇㅂㅇ과 내가 보낸 ㅇㅂㅇ의 답을 알릴지입니다.
 * @property focus S0-4와 설정에서 고른 관심사입니다. 동적 칸 순서와 개선 포인트 문장을 고를 때 씁니다.
 * @property seenProfileHint 홈 오른쪽 위에 "내 프로필은 여기서 볼 수 있어요"를 한 번 띄웠는지입니다. 고르는 설정이 아니라
 * 한 번만 띄우려고 적어 둡니다.
 */
data class UserPreferences(
    val theme: ThemePreference,
    val defaultQueue: QueueFilter,
    val statsPublic: Boolean,
    val notifyAnalysisDone: Boolean,
    val notifyWeeklyReport: Boolean,
    val focus: Focus,
    val notifyPing: Boolean = true,
    val seenProfileHint: Boolean = false,
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
