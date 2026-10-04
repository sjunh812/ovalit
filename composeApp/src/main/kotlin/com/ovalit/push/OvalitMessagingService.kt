package com.ovalit.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ovalit.LogBackgroundFailure
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.PushRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.importing.AppVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 서버가 보낸 FCM 메시지를 받습니다. 서버는 알림 메시지가 아니라 데이터 메시지만 보내서 앱이 꺼져 있어도 여기로 옵니다.
 * ㅇㅂㅇ은 받을 때마다 새로 받아 화면을 맞추고, 앱을 보고 있으면 화면에 이미 뜨니 알림은 띄우지 않습니다.
 */
class OvalitMessagingService : FirebaseMessagingService(), KoinComponent {

    private val account: AccountRepository by inject()
    private val preferences: UserPreferencesRepository by inject()
    private val pings: PingRepository by inject()
    private val push: PushRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + LogBackgroundFailure)

    override fun onNewToken(token: String) {
        scope.launch { push.register(token) }
    }

    // FCM 작업 스레드에서 불려서 막아도 된다. 설정을 읽는 동안만 막고 나머지는 scope로 넘긴다.
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        // 연동을 해제한 뒤 늦게 도착한 알림은 띄우지 않는다
        val (linked, settings) = runBlocking { (account.account.first() != null) to preferences.preferences.first() }
        if (!linked) return
        if (type == "weekly_report") {
            if (settings.notifyWeeklyReport) PingNotifications.show(this, data)
            return
        }
        scope.launch { pings.refresh() }
        if (settings.notifyPing && !AppVisibility.isVisible) PingNotifications.show(this, data)
    }
}
