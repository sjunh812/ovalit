package com.ovalit.di

import com.ovalit.core.data.Analytics
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.NoAnalytics
import com.ovalit.importing.WorkManagerImportScheduler
import com.ovalit.telemetry.FirebaseAnalyticsLogger
import com.ovalit.telemetry.OvalitFirebase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<ImportScheduler> { WorkManagerImportScheduler(androidContext()) }
    single<Analytics> { if (OvalitFirebase.enabled) FirebaseAnalyticsLogger(androidContext()) else NoAnalytics }
}
