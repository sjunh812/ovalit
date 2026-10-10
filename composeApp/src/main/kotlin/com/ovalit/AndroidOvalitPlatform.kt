package com.ovalit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.ovalit.app.OvalitPlatform
import com.ovalit.core.designsystem.component.OvalitToastState
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

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

    @Composable
    override fun ImportEntered() = RequestNotificationPermission()
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

// S0-4 아래에 "다 모으면 알림으로 알려드릴게요"라고 적혀 있어서 이 화면에 들어올 때 한 번 묻는다
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
