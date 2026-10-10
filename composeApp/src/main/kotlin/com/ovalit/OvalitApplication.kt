package com.ovalit

import android.app.Application
import android.content.res.Configuration
import com.ovalit.ads.AdMobRenderer
import com.ovalit.app.ovalitModules
import com.ovalit.core.data.NewMatchesWatcher
import com.ovalit.core.data.SocialWatcher
import com.ovalit.di.appModule
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
        registerActivityLifecycleCallbacks(AppVisibility)
        AppLanguage.update(this)

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@OvalitApplication)
            modules(
                ovalitModules(
                    preferencesPath = { filesDir.resolve("ovalit.preferences_pb").absolutePath },
                    language = AppLanguage.current,
                ) + appModule,
            )
        }
        OvalitFirebase.follow(this, appScope, account = get())
        OvalitPush.start(this, appScope, account = get(), preferences = get(), push = get())
        AdMobRenderer.start(this, appScope)
        // 앱을 다시 열면 그사이 끝난 경기와 ㅇㅂㅇ, 친구를 받고, 경기가 많이 남은 채로 떠나면 WorkManager가 이어 받는다
        val newMatches = get<NewMatchesWatcher>()
        val social = get<SocialWatcher>()
        AppVisibility.onChange = { visible ->
            if (visible) {
                newMatches.onAppVisible()
                social.onAppVisible()
            } else {
                newMatches.onAppHidden()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguage.update(this)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + LogBackgroundFailure)
}
