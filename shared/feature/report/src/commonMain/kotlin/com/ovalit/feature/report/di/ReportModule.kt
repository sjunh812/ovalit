package com.ovalit.feature.report.di

import com.ovalit.feature.report.ReportViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportModule = module {
    viewModel { ReportViewModel(get(), Clock.System, TimeZone.currentSystemDefault()) }
}
