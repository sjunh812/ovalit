package com.ovalit.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ovalit.BuildConfig
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Firebase는 google-services 플러그인이 넣은 값으로 앱 시작 때 스스로 뜹니다. `composeApp/google-services.json` 없이 빌드했으면
 * [enabled]가 `false`이고 푸시, 사용 통계, 비정상 종료 보고 없이 돕니다.
 */
internal object OvalitFirebase {

    val enabled: Boolean get() = BuildConfig.FIREBASE_ENABLED

    /**
     * 설정의 "사용 통계 보내기"를 따라 수집을 켜고 끕니다. 매니페스트에서 꺼 둔 채 시작해서 설정을 읽기 전에는 보내지 않습니다.
     * 연동을 해제하면 지금까지 쌓인 사용 기록과 아직 보내지 않은 비정상 종료 보고를 지웁니다.
     */
    fun follow(context: Context, scope: CoroutineScope, preferences: UserPreferencesRepository, account: AccountRepository) {
        if (!enabled) return
        val analytics = FirebaseAnalytics.getInstance(context)
        val crashlytics = FirebaseCrashlytics.getInstance()
        scope.launch {
            preferences.preferences.map { it.shareUsageStats }.distinctUntilChanged().collect { on ->
                analytics.setAnalyticsCollectionEnabled(on)
                crashlytics.isCrashlyticsCollectionEnabled = on
            }
        }
        scope.launch {
            var wasLinked: Boolean? = null
            account.account.map { it != null }.distinctUntilChanged().collect { linked ->
                if (!linked && wasLinked == true) {
                    analytics.resetAnalyticsData()
                    crashlytics.deleteUnsentReports()
                }
                wasLinked = linked
            }
        }
    }
}

/** [Analytics]를 Firebase Analytics로 보냅니다. 수집을 껐으면 Firebase가 버립니다. */
internal class FirebaseAnalyticsLogger(context: Context) : Analytics {
    private val firebase = FirebaseAnalytics.getInstance(context)

    override fun log(event: String, params: Map<String, String>) {
        firebase.logEvent(event, Bundle().apply { params.forEach { (key, value) -> putString(key, value) } })
    }

    override fun screen(name: String) {
        firebase.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, name)
            },
        )
    }
}
