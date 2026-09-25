package com.ovalit

import android.app.Application
import com.ovalit.core.data.di.dataModule
import com.ovalit.di.appModule
import com.ovalit.feature.friend.di.friendModule
import com.ovalit.feature.match.di.matchModule
import com.ovalit.feature.onboarding.di.onboardingModule
import com.ovalit.feature.profile.di.profileModule
import com.ovalit.feature.report.di.reportModule
import com.ovalit.feature.settings.di.settingsModule
import com.ovalit.importing.AppVisibility
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class OvalitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
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
    }
}
