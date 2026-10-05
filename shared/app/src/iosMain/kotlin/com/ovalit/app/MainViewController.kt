package com.ovalit.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.ovalit.core.designsystem.theme.OvalitTheme
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/** 앱 화면입니다. 광고와 알림이 없어 안드로이드의 광고 렌더러와 알림으로 여는 초대는 넘기지 않습니다. */
fun mainViewController(): UIViewController = ComposeUIViewController {
    val platform = remember { IosOvalitPlatform() }
    OvalitTheme(darkTheme = rememberDarkTheme()) {
        OvalitApp(appVersion = appVersion(), platform = platform)
    }
}

private fun appVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
