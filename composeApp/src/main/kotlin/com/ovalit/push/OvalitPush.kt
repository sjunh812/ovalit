package com.ovalit.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.ovalit.core.data.AccountRepository
import com.ovalit.telemetry.OvalitFirebase
import com.ovalit.core.data.PushRepository
import com.ovalit.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 서버 `src/scheduled.ts`의 토픽 이름과 같아야 합니다. */
internal const val WEEKLY_REPORT_TOPIC = "weekly_report"

/** FCM 토큰을 서버에 맡기고 주간 리포트 토픽을 구독합니다. Firebase 값이 없으면 아무것도 하지 않고 푸시 없이 돕니다. */
internal object OvalitPush {

    val enabled: Boolean get() = OvalitFirebase.enabled

    fun start(
        context: Context,
        scope: CoroutineScope,
        account: AccountRepository,
        preferences: UserPreferencesRepository,
        push: PushRepository,
    ) {
        if (!enabled) return
        val messaging = FirebaseMessaging.getInstance()
        messaging.isAutoInitEnabled = true
        // 해제하면 토큰을 버리고 떠 있는 알림을 지운다. 안 버리면 해제한 기기로 옛 계정의 ㅇㅂㅇ 알림이 계속 온다.
        // 다시 연동하면 FCM이 새 토큰을 준다.
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
        // 서버는 누가 스위치를 켰는지 모르고 토픽에 한 번만 보낸다. 그래서 스위치와 연동 여부는 토픽 구독으로 지킨다.
        scope.launch {
            combine(account.account.map { it != null }, preferences.preferences.map { it.notifyWeeklyReport }) { linked, on -> linked && on }
                .distinctUntilChanged()
                .collect { on ->
                    if (on) messaging.subscribeToTopic(WEEKLY_REPORT_TOPIC) else messaging.unsubscribeFromTopic(WEEKLY_REPORT_TOPIC)
                }
        }
    }
}
