package com.ovalit.importing

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** 앱 화면이 하나라도 보이는지 셉니다. 보고 있을 때 수집 완료 알림과 ㅇㅂㅇ 알림을 띄우지 않는 데 씁니다. */
object AppVisibility : Application.ActivityLifecycleCallbacks {
    // 메인 스레드에서 바뀌고 수집 작업의 스레드에서 읽는다
    @Volatile
    private var started = 0

    val isVisible: Boolean get() = started > 0

    /** 메인 스레드에서 부릅니다. 보이기 시작하면 `true`, 모두 가려지면 `false`입니다. */
    var onChange: (visible: Boolean) -> Unit = {}

    // 언어나 테마를 바꿔 화면을 다시 만드는 중이다.
    // 그때는 잠깐 0이 돼도 가려진 게 아니라서 알리지 않는다.
    // 알리면 받던 새 경기를 작업으로 넘기거나 친구를 다시 받는다.
    private var recreating = false

    override fun onActivityStarted(activity: Activity) {
        started++
        if (started == 1 && !recreating) onChange(true)
        recreating = false
    }

    override fun onActivityStopped(activity: Activity) {
        started--
        recreating = activity.isChangingConfigurations
        if (started == 0 && !recreating) onChange(false)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
