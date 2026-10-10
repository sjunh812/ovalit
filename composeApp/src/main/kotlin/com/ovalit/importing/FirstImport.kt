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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ovalit.MainActivity
import com.ovalit.R
import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val WORK_NAME = "first-import"
private const val NEW_MATCHES_WORK_NAME = "new-matches"
/** 분석 완료와 쌓인 새 경기 알림의 채널입니다. 설정이 이 채널이 막혔는지 봅니다. */
internal const val ANALYSIS_CHANNEL = "analysis_done"
private const val KEY_TOTAL = "total"
// 알림은 태그로 가른다. ㅇㅂㅇ 알림 ID는 초대 ID에서 만들어 어느 숫자와도 겹칠 수 있다.
private const val FIRST_IMPORT_TAG = "first_import"
private const val NEW_MATCHES_TAG = "new_matches"

/**
 * 같은 작업이 이미 돌거나 기다리고 있으면 새로 걸지 않습니다([retry]만 갈아 끼웁니다).
 *
 * 첫 수집은 네트워크를 기다리지 않고 바로 시작합니다. 연결을 조건으로 걸면 인터넷이 없을 때 작업이 아예 시작하지 않아 저장소가
 * 멈춘 까닭을 적지 못하고, S0-4가 오프라인 안내와 다시 시도 버튼 없이 "불러오는 중"에 머뭅니다. 받는 중에 끊겨도 작업을 멈추지 않고
 * 저장소가 끊긴 까닭을 적게 둡니다. 새 경기 이어 받기는 화면 밖 일이라 연결될 때까지 기다립니다.
 */
class WorkManagerImportScheduler(private val context: Context) : ImportScheduler {
    override fun start() {
        enqueue<FirstImportWorker>(WORK_NAME, needsNetwork = false)
    }

    override fun retry() {
        enqueue<FirstImportWorker>(WORK_NAME, policy = ExistingWorkPolicy.REPLACE, needsNetwork = false)
    }

    override fun continueNewMatches(total: Int) {
        enqueue<NewMatchesWorker>(NEW_MATCHES_WORK_NAME, workDataOf(KEY_TOTAL to total), needsNetwork = true)
    }

    override fun cancel() {
        WorkManager.getInstance(context).run {
            cancelUniqueWork(WORK_NAME)
            cancelUniqueWork(NEW_MATCHES_WORK_NAME)
        }
    }

    private inline fun <reified W : ListenableWorker> enqueue(
        name: String,
        input: Data = Data.EMPTY,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        needsNetwork: Boolean,
    ) {
        val network = if (needsNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
        val request = OneTimeWorkRequestBuilder<W>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, policy, request)
    }
}

class FirstImportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val account: AccountRepository by inject()
    private val matches: MatchRepository by inject()
    private val preferences: UserPreferencesRepository by inject()
    private val analytics: Analytics by inject()

    override suspend fun doWork(): Result {
        // 연동을 해제한 뒤 기다리던 작업이 돌면 RSO 세션 없이 전적을 요청하게 된다(CLAUDE.md 지켜야 할 선)
        if (account.account.first() == null) return Result.success()
        // 실패하면 WorkManager가 30초부터 늘려 가며 다시 띄운다. 받은 경기는 그대로라 남은 것만 받는다.
        try {
            matches.importRecent()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.retry()
        }
        analytics.log(
            AnalyticsEvents.TUTORIAL_COMPLETE,
            mapOf("match_bucket" to importBucket(matches.observeMatches().first().size), "finished_in_background" to (!AppVisibility.isVisible).toString()),
        )
        // 화면을 보고 있으면 S0-4가 알려 주니 보내지 않는다. 받는 사이 연동을 해제했어도 보내지 않는다.
        if (account.account.first() != null && preferences.preferences.first().notifyAnalysisDone && !AppVisibility.isVisible) {
            val count = matches.observeMatches().first().size
            notify(
                applicationContext,
                tag = FIRST_IMPORT_TAG,
                title = applicationContext.getString(R.string.notification_analysis_title),
                body = if (count > 0) {
                    applicationContext.getString(R.string.notification_analysis_body, count)
                } else {
                    applicationContext.getString(R.string.notification_analysis_body_none)
                },
            )
        }
        return Result.success()
    }
}

/**
 * 오래 쉬었다 와서 쌓인 새 경기를 앱을 닫아도 이어 받습니다. 다 받으면 "분석 완료" 알림 설정을 따라 알립니다. 받을 판 수는
 * 넘길 때 입력으로 받아 두어서, 작업이 시작되기 전에 앱에서 다 받았어도 알립니다.
 */
class NewMatchesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val account: AccountRepository by inject()
    private val matches: MatchRepository by inject()
    private val preferences: UserPreferencesRepository by inject()

    override suspend fun doWork(): Result {
        if (account.account.first() == null) return Result.success()
        // 그사이 앱에서 다 받았으면 경기 ID 목록을 다시 묻지 않는다
        if (matches.newMatchesProgress.first() != null) {
            try {
                matches.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return Result.retry()
            }
        }
        val total = inputData.getInt(KEY_TOTAL, 0)
        if (total > 0 && account.account.first() != null && preferences.preferences.first().notifyAnalysisDone && !AppVisibility.isVisible) {
            notify(
                applicationContext,
                tag = NEW_MATCHES_TAG,
                title = applicationContext.getString(R.string.notification_new_matches_title),
                body = applicationContext.getString(R.string.notification_new_matches_body, total),
            )
        }
        return Result.success()
    }
}

private fun notify(context: Context, tag: String, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(ANALYSIS_CHANNEL, context.getString(R.string.notification_channel_analysis), NotificationManager.IMPORTANCE_DEFAULT),
    )
    val open = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, ANALYSIS_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(tag, 0, notification)
}

// 사용 통계에는 받은 경기 수를 구간으로만 보낸다
private fun importBucket(count: Int): String = when {
    count == 0 -> "0"
    count < 10 -> "1-9"
    count < 30 -> "10-29"
    else -> "30-50"
}
