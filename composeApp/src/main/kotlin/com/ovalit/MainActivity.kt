package com.ovalit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.onboarding.intro.IntroScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OvalitTheme {
                IntroScreen(onStart = { /* S0-2 연동 동의 */ })
            }
        }
    }
}
