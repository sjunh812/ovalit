package com.ovalit.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.ovalit.BuildConfig
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.PushRepository
import com.ovalit.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
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
        account: AccountRepository,
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
        // 연동하면 토큰을 서버에 맡기고, 해제하면 토큰을 버리고 떠 있는 알림을 지운다. 그대로 두면 해제한 기기로 옛 계정의
        // ㅇㅂㅇ 이름과 시각이 계속 온다. 서버는 연동 해제 때 토큰 줄을 같이 지운다. 다시 연동하면 FCM이 새 토큰을 준다.
        scope.launch {
            var wasLinked: Boolean? = null
            account.account.map { it != null }.distinctUntilChanged().collect { linked ->
                if (linked) {
                    messaging.token.addOnSuccessListener { token -> scope.launch { push.register(token) } }
                } else if (wasLinked == true) {
                    messaging.deleteToken()
                    NotificationManagerCompat.from(context).cancelAll()
                }
                wasLinked = linked
            }
        }
        // 주간 리포트 스위치를 따라 토픽을 구독하거나 푼다. 서버는 누가 켰는지 모르고 토픽에 한 번만 보낸다. 연동 전에는 받지 않는다.
        scope.launch {
            combine(account.account.map { it != null }, preferences.preferences.map { it.notifyWeeklyReport }) { linked, on -> linked && on }
                .distinctUntilChanged()
                .collect { on ->
                    if (on) messaging.subscribeToTopic(WEEKLY_REPORT_TOPIC) else messaging.unsubscribeFromTopic(WEEKLY_REPORT_TOPIC)
                }
        }
    }
}
