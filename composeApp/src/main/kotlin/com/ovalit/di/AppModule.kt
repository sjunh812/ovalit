package com.ovalit.di

import com.ovalit.core.data.ImportScheduler
import com.ovalit.importing.WorkManagerImportScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<ImportScheduler> { WorkManagerImportScheduler(androidContext()) }
}
