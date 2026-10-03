package com.ovalit.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
 * 서버가 보낸 FCM 메시지를 받습니다. 서버는 알림 없이 데이터만 보내서 앱이 꺼져 있어도 여기로 옵니다. 설정에서 끈 알림은
 * 띄우지 않습니다. ㅇㅂㅇ은 새로 받아 화면을 맞추고, 앱을 보고 있으면 화면에 이미 뜨니 알림은 띄우지 않습니다.
 */
class OvalitMessagingService : FirebaseMessagingService(), KoinComponent {

    private val preferences: UserPreferencesRepository by inject()
    private val pings: PingRepository by inject()
    private val push: PushRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { push.register(token) }
    }

    // FCM이 일하는 스레드에서 불린다. 설정을 읽는 동안만 기다린다.
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        val settings = runBlocking { preferences.preferences.first() }
        if (type == "weekly_report") {
            if (settings.notifyWeeklyReport) PingNotifications.show(this, data)
            return
        }
        scope.launch { pings.refresh() }
        if (settings.notifyPing && !AppVisibility.isVisible) PingNotifications.show(this, data)
    }
}
