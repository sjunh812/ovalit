package com.ovalit.push

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// ㅇㅂㅇ, 주간 리포트, 친구 요청 알림이 같이 쓴다.

/**
 * 채널을 만들고 그 ID를 돌려줍니다.
 * 이미 있으면 그대로 쓰고 이름만 앱 언어에 맞춰 다시 적습니다.
 */
internal fun channel(context: Context, id: String, name: Int, importance: Int): String {
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(NotificationChannel(id, context.getString(name), importance))
    return id
}

/** 안드로이드 13부터는 알림 권한이 있어야 띄울 수 있습니다. */
internal fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** 같은 [tag]의 알림은 쌓이지 않고 한 자리에서 바뀝니다. */
@Suppress("MissingPermission")
internal fun notify(context: Context, tag: String, notification: Notification) {
    NotificationManagerCompat.from(context).notify(tag, 0, notification)
}
