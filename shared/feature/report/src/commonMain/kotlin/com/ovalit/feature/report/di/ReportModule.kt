package com.ovalit.feature.report.di

import com.ovalit.feature.report.ReportViewModel
import com.ovalit.feature.report.weekStarts
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportModule = module {
    viewModel {
        val timeZone = TimeZone.currentSystemDefault()
        ReportViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            Clock.System,
            timeZone,
            weekStarts(Clock.System, timeZone),
            pingRepository = get(),
            analytics = get(),
        )
    }
}
