package com.ovalit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ovalit.app.OvalitPlatform
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.designsystem.component.OvalitToastState
import com.ovalit.core.ui.NotificationPermission
import com.ovalit.feature.settings.NotificationBlocks
import com.ovalit.importing.ANALYSIS_CHANNEL
import com.ovalit.push.PING_CHANNEL
import com.ovalit.push.PingNotifications
import com.ovalit.push.WEEKLY_CHANNEL
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** 안드로이드에서 [com.ovalit.app.OvalitApp]이 맡기는 일입니다. [context]는 화면을 띄운 액티비티입니다. */
internal class AndroidOvalitPlatform(private val context: Context) : OvalitPlatform {

    override val feedbackAddress: String? = BuildConfig.FEEDBACK_EMAIL.takeIf { it.isNotBlank() }

    override fun shareInvite(link: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_invite)))
    }

    /** 받는 주소와 제목, 앱 버전과 기기만 채우고 Riot ID나 전적은 넣지 않습니다. 본문은 화면 문구가 아니라 메일이라 줄바꿈을 넣어 둡니다. */
    override fun sendFeedback(appVersion: String): Boolean {
        val body = context.getString(R.string.feedback_body, appVersion, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.RELEASE)
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.FEEDBACK_EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject))
            .putExtra(Intent.EXTRA_TEXT, body)
        return try {
            context.startActivity(mail)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    @Composable
    override fun HomeBackHandler(toast: OvalitToastState) = ExitOnSecondBack(toast)

    // 휴대폰 설정에서 켜고 돌아오면 바로 맞게 화면으로 돌아올 때마다 다시 본다
    @Composable
    override fun rememberNotificationPermission(): NotificationPermission {
        var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
        LifecycleResumeEffect(Unit) {
            enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            onPauseOrDispose {}
        }
        val activity = LocalActivity.current
        val preferences = koinInject<UserPreferencesRepository>()
        val asked by remember(preferences) { preferences.preferences.map { it.askedNotificationPermission } }.collectAsState(initial = false)
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return remember(enabled, asked, activity, launcher) {
            NotificationPermission(missing = !enabled) {
                if (context.canAskPermission(activity, asked)) {
                    scope.launch { runCatching { preferences.setAskedNotificationPermission() } }
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings()
                }
            }
        }
    }

    override fun clearPingNotification(pingId: String) = PingNotifications.clear(context, pingId)

    // 휴대폰 설정에서 바꾸고 돌아오면 바로 맞게 화면으로 돌아올 때마다 다시 본다
    @Composable
    override fun rememberNotificationBlocks(): NotificationBlocks {
        var blocks by remember { mutableStateOf(notificationBlocks(context)) }
        LifecycleResumeEffect(Unit) {
            blocks = notificationBlocks(context)
            onPauseOrDispose {}
        }
        return blocks
    }

    override fun openNotificationSettings() {
        val notifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        try {
            context.startActivity(notifications)
        } catch (_: ActivityNotFoundException) {
            // 알림 설정 화면을 못 여는 기기도 있어서 앱 정보 화면으로 보낸다
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }
    }
}

// 앱 알림을 통째로 껐으면 셋 다 막힌 것이다. 아직 만들지 않은 채널은 막을 수도 없어 열린 것으로 본다.
private fun notificationBlocks(context: Context): NotificationBlocks {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return NotificationBlocks(analysisDone = true, weeklyReport = true, ping = true)
    fun blocked(channel: String) = manager.getNotificationChannelCompat(channel)?.importance == NotificationManagerCompat.IMPORTANCE_NONE
    return NotificationBlocks(
        analysisDone = blocked(ANALYSIS_CHANNEL),
        weeklyReport = blocked(WEEKLY_CHANNEL),
        ping = blocked(PING_CHANNEL),
    )
}

/**
 * 토스처럼 홈에서 뒤로 가기를 한 번 누르면 안내만 띄우고, 안내가 떠 있는 동안 한 번 더 누르면 앱을 닫습니다. 한 번에 닫히면 위로
 * 스크롤하려다 실수로 닫히기 쉽습니다.
 */
@Composable
private fun ExitOnSecondBack(toast: OvalitToastState) {
    var armed by remember { mutableStateOf(false) }
    val message = stringResource(R.string.exit_confirm)
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    BackHandler {
        // 두 번째는 시스템에 넘기지 않고 직접 닫는다. 안드로이드 12부터 시스템 뒤로 가기는 첫 화면을 닫지 않고 앱을 뒤로 보내기만
        // 해서, 다시 열면 홈이 그대로 떠 있다.
        if (armed) {
            activity?.finish()
            return@BackHandler
        }
        armed = true
        scope.launch {
            try {
                toast.show(message, ExitWindow)
            } finally {
                armed = false
            }
        }
    }
}

private val ExitWindow = 2.seconds

// 시스템 권한 창을 띄울 수 있는지다. 안드로이드 12까지는 권한 창이 없고, 권한이 있는데 알림이 꺼졌으면 사용자가 설정에서 끈
// 것이라 둘 다 설정 화면에서만 켤 수 있다. 두 번 거절하면 시스템이 창을 더는 띄우지 않고 바로 거절로 돌려준다. 그때 다시 띄우면
// 아무 일도 일어나지 않아서, 띄운 적이 있는데 다시 물어도 된다는 신호(rationale)가 없으면 설정 화면을 연다.
private fun Context.canAskPermission(activity: Activity?, askedBefore: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < 33) return false
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
    return !askedBefore || activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
}
