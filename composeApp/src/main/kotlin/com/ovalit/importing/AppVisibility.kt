package com.ovalit.importing

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** 앱 화면이 보이고 있는지입니다. 보고 있을 때는 첫 수집 완료 알림을 보내지 않습니다. */
object AppVisibility : Application.ActivityLifecycleCallbacks {
    private var started = 0

    val isVisible: Boolean get() = started > 0

    override fun onActivityStarted(activity: Activity) {
        started++
    }

    override fun onActivityStopped(activity: Activity) {
        started--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
