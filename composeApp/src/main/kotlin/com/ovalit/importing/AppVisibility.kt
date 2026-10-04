package com.ovalit.importing

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 앱 화면이 보이고 있는지입니다. 보고 있을 때는 수집 완료 알림을 보내지 않습니다. 화면이 보이기 시작하거나 모두 가려질 때
 * [onChange]를 부릅니다. 앱을 다시 열면 새 경기를 확인하고, 받던 중에 떠나면 WorkManager에 넘깁니다.
 */
object AppVisibility : Application.ActivityLifecycleCallbacks {
    // 메인 스레드에서 바뀌고 수집 작업의 스레드에서 읽는다
    @Volatile
    private var started = 0

    val isVisible: Boolean get() = started > 0

    /** 메인 스레드에서 부릅니다. 보이기 시작하면 `true`, 모두 가려지면 `false`입니다. */
    var onChange: (visible: Boolean) -> Unit = {}

    override fun onActivityStarted(activity: Activity) {
        started++
        if (started == 1) onChange(true)
    }

    override fun onActivityStopped(activity: Activity) {
        started--
        if (started == 0) onChange(false)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
