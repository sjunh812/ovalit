package com.ovalit.importing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ovalit.MainActivity
import com.ovalit.R
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val WORK_NAME = "first-import"
private const val CHANNEL_ID = "analysis_done"
private const val NOTIFICATION_ID = 1

/**
 * 첫 수집을 WorkManager에 맡깁니다. 앱을 닫아도 이어 받습니다. 같은 이름의 작업이 이미 돌고 있으면
 * 새로 걸지 않습니다. 주기적으로 다시 받는 동기화는 두지 않습니다.
 */
class WorkManagerImportScheduler(private val context: Context) : ImportScheduler {
    override fun start() {
        val request = OneTimeWorkRequestBuilder<FirstImportWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}

class FirstImportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val matches: MatchRepository by inject()
    private val preferences: UserPreferencesRepository by inject()

    override suspend fun doWork(): Result {
        matches.importRecent()
        // 화면을 보고 있으면 S0-4가 이미 끝났다고 알려 주니 알림을 겹쳐 보내지 않는다
        if (preferences.preferences.first().notifyAnalysisDone && !AppVisibility.isVisible) {
            notifyDone(applicationContext, count = matches.observeMatches().first().size)
        }
        return Result.success()
    }
}

private fun notifyDone(context: Context, count: Int) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_analysis), NotificationManager.IMPORTANCE_DEFAULT),
    )
    val open = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.notification_analysis_title))
        .setContentText(context.getString(R.string.notification_analysis_body, count))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}
