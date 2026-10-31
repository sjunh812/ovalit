package com.ovalit.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.ovalit.MainActivity
import com.ovalit.R

internal const val FRIEND_CHANNEL = "friend_request"
internal const val EXTRA_OPEN_FRIENDS = "com.ovalit.OPEN_FRIENDS"

/**
 * 친구 요청을 받거나 내 요청이 수락됐을 때의 알림입니다.
 * 서버는 종류와 이름만 보내고 문구는 여기서 앱 언어로 만듭니다.
 * 누르면 친구 탭이 열립니다.
 */
internal object FriendNotifications {

    fun show(context: Context, data: Map<String, String>) {
        if (!canNotify(context)) return
        // 닉네임 뒤에는 받침에 따라 바뀌는 조사를 붙이지 않는다(CLAUDE.md 용어)
        val name = data["name"].orEmpty()
        val (title, text) = when (data["type"]) {
            "friend_request" -> context.getString(R.string.friend_request_title, name) to context.getString(R.string.friend_request_body)
            "friend_accept" -> context.getString(R.string.friend_accept_title) to context.getString(R.string.friend_accept_body, name)
            else -> return
        }
        val notification = NotificationCompat.Builder(
            context,
            channel(context, FRIEND_CHANNEL, R.string.notification_channel_friend, NotificationManager.IMPORTANCE_DEFAULT),
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openFriends(context))
            .setAutoCancel(true)
            .build()
        // 같은 사람의 요청과 수락은 한 자리에서 바뀌고 여러 사람의 것은 쌓인다
        notify(context, "friend:$name", notification)
    }

    private fun openFriends(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("$NOTIFICATION_SCHEME://friends"))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_FRIENDS, true)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
