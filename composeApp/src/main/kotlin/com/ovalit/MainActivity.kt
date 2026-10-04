package com.ovalit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.ads.AdMobRenderer
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.ui.LocalAdRenderer
import com.ovalit.push.EXTRA_OPEN_PING
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    // ㅇㅂㅇ 알림을 눌러 열 초대다. 화면이 받기 전에 온 것도 남게 채널에 담는다.
    private val openPing = Channel<String>(Channel.CONFLATED)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_PING)?.let(openPing::trySend)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 앱별 언어를 바꾸면 이 화면이 새 언어로 다시 만들어진다. 이름 카탈로그도 그 언어로 바꾼다.
        AppLanguage.update(this)
        if (savedInstanceState == null) intent.getStringExtra(EXTRA_OPEN_PING)?.let(openPing::trySend)

        setContent {
            val preferences = koinInject<UserPreferencesRepository>()
            val theme by preferences.preferences
                .map { it.theme }
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val darkTheme = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
            }

            // 앱 테마를 시스템과 다르게 고르면 상태 표시줄 글자색도 바꿔야 한다. 안 바꾸면 라이트 배경에 흰 시계가 뜬다.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            // 광고 단위 ID가 없으면 렌더러가 null이라 광고 자리를 그리지 않는다
            val adScope = rememberCoroutineScope()
            val analytics = koinInject<Analytics>()
            val adRenderer = remember { if (AdMobRenderer.enabled) AdMobRenderer(this, preferences, adScope, analytics) else null }
            DisposableEffect(adRenderer) { onDispose { adRenderer?.destroy() } }

            // 테마가 바뀌어 다시 그려도 같은 흐름을 넘긴다. 새로 만들면 알림을 받는 LaunchedEffect가 다시 시작한다.
            val pingsToOpen = remember { openPing.receiveAsFlow() }

            OvalitTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalAdRenderer provides adRenderer) {
                    OvalitApp(appVersion = BuildConfig.VERSION_NAME, openPing = pingsToOpen)
                    adRenderer?.Sheets()
                }
            }
        }
    }
}
