package com.ovalit.app

import androidx.compose.runtime.Composable
import com.ovalit.core.designsystem.component.OvalitToastState
import com.ovalit.core.ui.NotificationPermission
import com.ovalit.feature.settings.NotificationBlocks

/** 안드로이드와 iOS가 다르게 하는 일입니다. [OvalitApp]이 부릅니다. */
interface OvalitPlatform {

    /** 피드백 메일의 받는 주소입니다. 없으면 설정에 "피드백 보내기" 줄이 없습니다. */
    val feedbackAddress: String?

    /** 초대 링크를 시스템 공유 창으로 보냅니다. */
    fun shareInvite(link: String)

    /** 피드백 메일을 씁니다. 메일 앱이 없으면 `false`이고, 그때는 [feedbackAddress]를 토스트로 알려 줍니다. */
    fun sendFeedback(appVersion: String): Boolean

    /** 탭을 하나도 쌓지 않은 홈에서 뒤로 가기를 받습니다. 시스템 뒤로 가기가 없는 iOS는 아무것도 하지 않습니다. */
    @Composable
    fun HomeBackHandler(toast: OvalitToastState) {}

    /**
     * 알림 권한과 그걸 묻는 길입니다. 앱 맨 위에서 한 번 부르고, 앱으로 돌아올 때마다 다시 확인합니다. 알림을 보내지 않는 iOS는
     * 물을 게 없습니다.
     */
    @Composable
    fun rememberNotificationPermission(): NotificationPermission = NotificationPermission.NotNeeded

    /** 초대 화면을 열 때 그 초대의 알림을 거둡니다. 알림이 없는 iOS는 아무것도 하지 않습니다. */
    fun clearPingNotification(pingId: String) {}

    /** 휴대폰 설정에서 막아 둔 알림입니다. 앱으로 돌아올 때마다 다시 확인합니다. 알림을 보내지 않는 iOS는 막힌 게 없습니다. */
    @Composable
    fun rememberNotificationBlocks(): NotificationBlocks = NotificationBlocks.None

    /** 휴대폰 설정의 이 앱 알림 화면을 엽니다. */
    fun openNotificationSettings() {}
}
