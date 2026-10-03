package com.ovalit.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ovalit.R
import com.ovalit.core.data.PingRepository
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ACTION_REPLY = "com.ovalit.PING_REPLY"
private const val ACTION_MOVE = "com.ovalit.PING_MOVE"
private const val EXTRA_PING = "pingId"
private const val EXTRA_ANSWER = "answer"
private const val EXTRA_TIME = "time"

/**
 * 알림의 갈게요, 못 가요, 수락 버튼입니다. 앱을 열지 않고 서버에 답한 뒤 같은 자리의 알림을 "참석으로 답했어요"로 바꿉니다.
 * 답을 보내는 동안 프로세스가 끝나지 않게 [goAsync]로 붙잡아 둡니다.
 */
class PingActionReceiver : BroadcastReceiver(), KoinComponent {

    private val pings: PingRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pingId = intent.getStringExtra(EXTRA_PING) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_REPLY -> {
                        val yes = intent.getStringExtra(EXTRA_ANSWER) == "yes"
                        pings.reply(PingId(pingId), if (yes) PingAnswer.YES else PingAnswer.NO)
                        PingNotifications.answered(context, pingId, if (yes) R.string.ping_answered_yes else R.string.ping_answered_no)
                    }
                    ACTION_MOVE -> {
                        val time = intent.getLongExtra(EXTRA_TIME, 0L).takeIf { it > 0 } ?: return@launch
                        pings.moveTo(PingId(pingId), Instant.fromEpochMilliseconds(time))
                        PingNotifications.answered(context, pingId, R.string.ping_moved)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        // 알림 버튼은 앱이 꺼져 있어도 눌린다. 받는 동안만 쓰는 범위라 따로 둔다.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun reply(context: Context, pingId: String, answer: String): PendingIntent = broadcast(
            context,
            requestCode = PingNotifications.notificationId(pingId) * 4 + if (answer == "yes") 2 else 3,
            intent = Intent(context, PingActionReceiver::class.java)
                .setAction(ACTION_REPLY)
                .putExtra(EXTRA_PING, pingId)
                .putExtra(EXTRA_ANSWER, answer),
        )

        fun move(context: Context, pingId: String, time: Long): PendingIntent = broadcast(
            context,
            requestCode = PingNotifications.notificationId(pingId) * 4,
            intent = Intent(context, PingActionReceiver::class.java)
                .setAction(ACTION_MOVE)
                .putExtra(EXTRA_PING, pingId)
                .putExtra(EXTRA_TIME, time),
        )

        private fun broadcast(context: Context, requestCode: Int, intent: Intent): PendingIntent =
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
