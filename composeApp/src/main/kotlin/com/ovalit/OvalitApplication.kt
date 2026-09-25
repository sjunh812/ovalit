package com.ovalit

import android.app.Application
import com.ovalit.core.data.di.dataModule
import com.ovalit.di.appModule
import com.ovalit.feature.report.di.reportModule
import com.ovalit.feature.settings.di.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class OvalitApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@OvalitApplication)
            modules(
                appModule,
                dataModule(preferencesPath = { filesDir.resolve("ovalit.preferences_pb").absolutePath }),
                reportModule,
                settingsModule,
            )
        }
    }
}
