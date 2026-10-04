package com.ovalit

import android.app.Application
import com.ovalit.ads.AdMobRenderer
import com.ovalit.core.data.NewMatchesWatcher
import com.ovalit.core.data.di.dataModule
import com.ovalit.di.appModule
import com.ovalit.feature.friend.di.friendModule
import com.ovalit.feature.match.di.matchModule
import com.ovalit.feature.onboarding.di.onboardingModule
import com.ovalit.feature.profile.di.profileModule
import com.ovalit.feature.report.di.reportModule
import com.ovalit.feature.settings.di.settingsModule
import com.ovalit.importing.AppVisibility
import com.ovalit.push.OvalitPush
import com.ovalit.telemetry.OvalitFirebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class OvalitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 비정상 종료 보고를 놓치지 않게 무엇보다 먼저 띄운다. 광고 SDK보다도 앞서야 동의 신호가 맞게 이어진다.
        OvalitFirebase.start(this)
        registerActivityLifecycleCallbacks(AppVisibility)

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@OvalitApplication)
            modules(
                appModule,
                dataModule(preferencesPath = { filesDir.resolve("ovalit.preferences_pb").absolutePath }),
                onboardingModule,
                reportModule,
                matchModule,
                friendModule,
                profileModule,
                settingsModule,
            )
        }
        OvalitFirebase.follow(this, appScope, preferences = get(), account = get())
        // 앱이 살아 있는 동안 토큰을 서버에 맡기고 주간 리포트 토픽 구독을 설정에 맞춘다
        OvalitPush.start(this, appScope, account = get(), preferences = get(), push = get())
        AdMobRenderer.start(this, appScope)
        // 앱을 다시 열면 그사이 끝난 경기를 받고, 많이 남은 채로 떠나면 WorkManager가 이어 받는다
        val newMatches = get<NewMatchesWatcher>()
        AppVisibility.onChange = { visible -> if (visible) newMatches.onAppVisible() else newMatches.onAppHidden() }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + LogBackgroundFailure)
}
