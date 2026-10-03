package com.ovalit.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.ovalit.BuildConfig
import com.ovalit.core.data.PushRepository
import com.ovalit.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 서버가 월요일 9시에 한 번 보내는 토픽입니다. 서버의 `src/scheduled.ts`와 이름이 같아야 합니다. */
internal const val WEEKLY_REPORT_TOPIC = "weekly_report"

/**
 * FCM을 띄웁니다. google-services 플러그인 대신 `local.properties`의 네 값으로 직접 띄웁니다. 값이 없으면 아무것도 하지 않고
 * 앱은 푸시 없이 돕니다. 가짜 저장소로 돌릴 때와 Firebase 프로젝트를 만들기 전이 그렇습니다.
 */
internal object OvalitPush {

    val enabled: Boolean get() = BuildConfig.FIREBASE_APP_ID.isNotBlank()

    fun start(
        context: Context,
        scope: CoroutineScope,
        preferences: UserPreferencesRepository,
        push: PushRepository,
    ) {
        if (!enabled) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        val messaging = FirebaseMessaging.getInstance()
        messaging.isAutoInitEnabled = true
        messaging.token.addOnSuccessListener { token -> scope.launch { push.register(token) } }
        // 주간 리포트 스위치를 따라 토픽을 구독하거나 푼다. 서버는 누가 켰는지 모르고 토픽에 한 번만 보낸다.
        scope.launch {
            preferences.preferences.map { it.notifyWeeklyReport }.distinctUntilChanged().collect { on ->
                if (on) messaging.subscribeToTopic(WEEKLY_REPORT_TOPIC) else messaging.unsubscribeFromTopic(WEEKLY_REPORT_TOPIC)
            }
        }
    }
}
