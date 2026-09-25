package com.ovalit

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ThemePreference
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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

            // 앱 테마를 시스템과 다르게 고르면 상태 표시줄 글자색도 같이 바꿔야 한다.
            // 안 바꾸면 라이트 배경에 흰 시계가 뜬다.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            OvalitTheme(darkTheme = darkTheme) {
                OvalitApp(appVersion = BuildConfig.VERSION_NAME)
            }
        }
    }
}
