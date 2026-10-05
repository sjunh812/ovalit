package com.ovalit.app

import androidx.compose.runtime.Composable
import com.ovalit.core.designsystem.component.OvalitToastState

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

    /** S0-4에 들어올 때 부릅니다. 안드로이드는 다 모으면 보낼 알림의 권한을 여기서 묻습니다. */
    @Composable
    fun ImportEntered() {}
}
