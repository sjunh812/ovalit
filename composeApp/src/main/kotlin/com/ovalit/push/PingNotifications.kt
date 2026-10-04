package com.ovalit.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ovalit.MainActivity
import com.ovalit.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val PING_CHANNEL = "ping"
private const val WEEKLY_CHANNEL = "weekly_report"
private const val WEEKLY_TAG = "weekly_report"

// 보낸 시각과 이만큼 안쪽이면 "지금"으로 적는다. 앱의 Ping.isNow와 같은 폭이다.
private const val NOW_WINDOW_MS = 5 * 60 * 1000L

/** 알림을 눌러 열 초대의 ID입니다. MainActivity가 받아 그 초대 화면을 엽니다. */
internal const val EXTRA_OPEN_PING = "com.ovalit.OPEN_PING"
private const val DAWN_END_HOUR = 6

/**
 * 서버가 보낸 FCM 데이터 메시지를 알림으로 바꿉니다. 서버는 글자 없이 종류와 이름, 시각만 보내고 문구는 앱이 정합니다. 그래야
 * 문구를 고칠 때 서버를 다시 배포하지 않습니다.
 *
 * 받은 ㅇㅂㅇ에는 갈게요, 다른 시간, 못 가요 버튼을 답니다. 갈게요와 못 가요는 앱을 열지 않고 [PingActionReceiver]가 답하고,
 * 다른 시간은 시각을 골라야 해서 그 초대 화면을 엽니다.
 *
 * 닉네임 뒤에는 받침에 따라 바뀌는 조사를 붙이지 않습니다("민석의 초대"). 앱 화면과 같은 규칙입니다.
 */
internal object PingNotifications {

    fun show(context: Context, data: Map<String, String>) {
        if (!canNotify(context)) return
        val pingId = data["pingId"]
        val notification = when (data["type"]) {
            "ping_new" -> pingNotification(context, pingId) {
                val host = data["hostName"].orEmpty()
                setContentTitle(
                    if (isNow(data)) {
                        context.getString(R.string.ping_new_title_now, host)
                    } else {
                        context.getString(R.string.ping_new_title, host, timeText(context, data))
                    },
                )
                setContentText(others(context, data["others"]))
                addAnswerActions(context, pingId)
            }
            "ping_time" -> pingNotification(context, pingId) {
                setContentTitle(context.getString(R.string.ping_time_title, data["hostName"].orEmpty()))
                setContentText(context.getString(R.string.ping_time_body, timeText(context, data)))
                addAnswerActions(context, pingId)
            }
            "ping_reply" -> pingNotification(context, pingId) {
                val name = data["memberName"].orEmpty()
                val proposed = data["proposedAt"]?.toLongOrNull()
                setContentTitle(
                    when (data["answer"]) {
                        "yes" -> context.getString(R.string.ping_reply_yes_title, name)
                        "no" -> context.getString(R.string.ping_reply_no_title, name)
                        else -> context.getString(R.string.ping_reply_other_title, name, clockText(context, proposed ?: 0L))
                    },
                )
                setContentText(context.getString(R.string.ping_reply_body, timeText(context, data)))
                // 다른 시간을 냈으면 보낸 사람이 알림에서 바로 그 시각으로 옮긴다
                if (data["answer"] == "other_time" && pingId != null && proposed != null) {
                    addAction(0, context.getString(R.string.ping_action_move), PingActionReceiver.move(context, pingId, proposed))
                }
            }
            "ping_cancel" -> pingNotification(context, pingId) {
                setContentTitle(context.getString(R.string.ping_cancel_title, data["hostName"].orEmpty()))
            }
            "ping_remind" -> pingNotification(context, pingId) {
                setContentTitle(remindTitle(context, data))
                setContentText(context.getString(R.string.ping_remind_body, data["names"].orEmpty().split(",").joinToString(", ")))
            }
            "weekly_report" -> NotificationCompat.Builder(context, channel(context, WEEKLY_CHANNEL, R.string.notification_channel_weekly, NotificationManager.IMPORTANCE_DEFAULT))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.weekly_report_title))
                .setContentText(context.getString(R.string.weekly_report_body))
                .setContentIntent(openApp(context, pingId = null))
                .setAutoCancel(true)
                .build()
            else -> null
        } ?: return
        notify(context, if (data["type"] == "weekly_report") WEEKLY_TAG else pingTag(pingId), notification)
    }

    /** 알림 버튼으로 답한 뒤 같은 자리의 알림을 "참석으로 답했어요"로 바꿉니다. 소리는 다시 내지 않습니다. */
    fun answered(context: Context, pingId: String, text: Int) {
        if (!canNotify(context)) return
        val notification = pingNotification(context, pingId) {
            setContentTitle(context.getString(text))
            setOnlyAlertOnce(true)
            setTimeoutAfter(10_000L)
        }
        notify(context, pingTag(pingId), notification)
    }

    // 같은 ㅇㅂㅇ의 알림은 한 자리에서 바뀐다. 답이 올 때마다 쌓이면 알림판이 지저분해진다. 초대 ID를 태그로 써서 다른 초대나
    // 다른 종류의 알림과 겹치지 않는다.
    private fun pingTag(pingId: String?): String = "ping:${pingId.orEmpty()}"

    /**
     * 알림 버튼마다 다른 [PendingIntent]가 되게 붙이는 주소입니다. 안드로이드는 extra를 보지 않고 액션과 주소로 PendingIntent를
     * 가르니, 이게 없으면 다른 초대의 버튼이 앞 초대의 것으로 덮입니다. 명시적 인텐트에만 붙여 다른 앱이 받을 수 없습니다.
     */
    fun actionUri(pingId: String?, action: String): Uri = Uri.Builder().scheme("ovalit-notification").authority(action).appendPath(pingId.orEmpty()).build()

    private fun pingNotification(context: Context, pingId: String?, build: NotificationCompat.Builder.() -> Unit) =
        NotificationCompat.Builder(context, channel(context, PING_CHANNEL, R.string.notification_channel_ping, NotificationManager.IMPORTANCE_HIGH))
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(openApp(context, pingId, action = "open"))
            .setAutoCancel(true)
            .apply(build)
            .build()

    private fun NotificationCompat.Builder.addAnswerActions(context: Context, pingId: String?) {
        if (pingId == null) return
        addAction(0, context.getString(R.string.ping_action_yes), PingActionReceiver.reply(context, pingId, "yes"))
        addAction(0, context.getString(R.string.ping_action_other), openApp(context, pingId, action = "other_time"))
        addAction(0, context.getString(R.string.ping_action_no), PingActionReceiver.reply(context, pingId, "no"))
    }

    private fun others(context: Context, others: String?): String {
        val names = others.orEmpty().split(",").filter { it.isNotBlank() }
        return if (names.isEmpty()) {
            context.getString(R.string.ping_new_body)
        } else {
            context.getString(R.string.ping_new_body_others, names.joinToString(", "))
        }
    }

    private fun isNow(data: Map<String, String>): Boolean {
        val startsAt = data["startsAt"]?.toLongOrNull() ?: return true
        return startsAt - System.currentTimeMillis() < NOW_WINDOW_MS
    }

    // 사람마다 고른 시간(10분, 30분, 1시간 전)에 오고 크론이 5분마다 돌아서, 고른 시간이 아니라 실제로 남은 시간을 적는다
    private fun remindTitle(context: Context, data: Map<String, String>): String {
        val startsAt = data["startsAt"]?.toLongOrNull() ?: return context.getString(R.string.ping_remind_title_soon)
        val minutes = ((startsAt - System.currentTimeMillis()) / 60_000.0).roundToInt()
        return when {
            minutes >= 60 -> context.getString(R.string.ping_remind_title_hour)
            minutes >= 1 -> context.getString(R.string.ping_remind_title, minutes)
            else -> context.getString(R.string.ping_remind_title_soon)
        }
    }

    private fun timeText(context: Context, data: Map<String, String>): String {
        val startsAt = data["startsAt"]?.toLongOrNull() ?: return context.getString(R.string.ping_time_now)
        return if (isNow(data)) context.getString(R.string.ping_time_now) else clockText(context, startsAt)
    }

    // 앱 화면과 같이 "21:00"이고, 자정을 넘긴 오늘 밤이면 "새벽 00:30", 그보다 뒤면 "내일 09:00"이다
    private fun clockText(context: Context, epochMs: Long): String {
        val zone = ZoneId.systemDefault()
        val at = Instant.ofEpochMilli(epochMs).atZone(zone)
        val clock = at.format(DateTimeFormatter.ofPattern("HH:mm"))
        val now = Instant.now().atZone(zone)
        // 앱 화면의 pingDayOf와 같다. 오늘 밤 자정을 넘긴 시각은 "새벽"이다.
        return when {
            at.toLocalDate() == now.toLocalDate() -> clock
            at.hour < DAWN_END_HOUR && now.hour >= DAWN_END_HOUR -> context.getString(R.string.ping_time_dawn, clock)
            else -> context.getString(R.string.ping_time_tomorrow, clock)
        }
    }

    // pingId가 있으면 그 초대 화면을 연다. 다른 시간은 거기서 고른다.
    private fun openApp(context: Context, pingId: String?, action: String = "open"): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(actionUri(pingId, action))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (pingId != null) putExtra(EXTRA_OPEN_PING, pingId) }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun channel(context: Context, id: String, name: Int, importance: Int): String {
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(id, context.getString(name), importance))
        return id
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun notify(context: Context, tag: String, notification: android.app.Notification) {
        NotificationManagerCompat.from(context).notify(tag, 0, notification)
    }
}
