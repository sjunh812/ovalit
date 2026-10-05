package com.ovalit.app

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * iOS에서 [OvalitApp]이 맡기는 일입니다. 시뮬레이터에서 화면을 보는 데까지가 범위라(CLAUDE.md 아키텍처) 피드백 메일과 알림 권한은
 * 두지 않습니다.
 */
internal class IosOvalitPlatform : OvalitPlatform {

    override val feedbackAddress: String? = null

    override fun shareInvite(link: String) {
        val share = UIActivityViewController(activityItems = listOf(link), applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(share, animated = true, completion = null)
    }

    override fun sendFeedback(appVersion: String): Boolean = false
}
