@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.ovalit.app

import com.ovalit.core.data.NewMatchesWatcher
import com.ovalit.core.data.SocialWatcher
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import org.koin.mp.KoinPlatform
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow

/** 창을 하나 만들어 [mainViewController]를 띄웁니다. 앱이 보이고 가려질 때 새 경기 확인을 알립니다(CLAUDE.md 경기 받기). */
class OvalitAppDelegate @OverrideInit constructor() : UIResponder(), UIApplicationDelegateProtocol {
    companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta

    private var appWindow: UIWindow? = null

    override fun window(): UIWindow? = appWindow

    override fun setWindow(window: UIWindow?) {
        appWindow = window
    }

    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean {
        startOvalitKoin()
        appWindow = UIWindow(frame = UIScreen.mainScreen.bounds).apply {
            rootViewController = mainViewController()
            makeKeyAndVisible()
        }
        return true
    }

    override fun applicationDidBecomeActive(application: UIApplication) {
        KoinPlatform.getKoin().get<NewMatchesWatcher>().onAppVisible()
        KoinPlatform.getKoin().get<SocialWatcher>().onAppVisible()
    }

    override fun applicationDidEnterBackground(application: UIApplication) {
        KoinPlatform.getKoin().get<NewMatchesWatcher>().onAppHidden()
    }
}
