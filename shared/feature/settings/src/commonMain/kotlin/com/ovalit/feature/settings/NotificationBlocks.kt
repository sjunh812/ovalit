package com.ovalit.feature.settings

/**
 * 휴대폰 설정에서 막아 둔 알림입니다. 앱의 스위치를 켜 둬도 오지 않아서, 설정은 그 카드 맨 위에 켜러 가는 줄을 두고 막힌 스위치를
 * 흐리게 둡니다. 알림 채널을 따로 막을 수 있어 종류마다 둡니다. 알림을 보내지 않는 iOS는 늘 [None]입니다.
 */
data class NotificationBlocks(
    val analysisDone: Boolean = false,
    val weeklyReport: Boolean = false,
    val ping: Boolean = false,
) {
    companion object {
        val None = NotificationBlocks()
    }
}
