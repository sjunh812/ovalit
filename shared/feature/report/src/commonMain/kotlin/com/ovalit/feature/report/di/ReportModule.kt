package com.ovalit.feature.report.di

import com.ovalit.core.data.minuteStarts
import com.ovalit.core.data.weekStarts
import com.ovalit.core.model.QueueFilter
import com.ovalit.feature.report.FriendRankingViewModel
import com.ovalit.feature.report.ReportViewModel
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
            minuteChanges = minuteStarts(Clock.System),
        )
    }
    viewModel { (queue: String) ->
        val timeZone = TimeZone.currentSystemDefault()
        FriendRankingViewModel(
            QueueFilter.valueOf(queue),
            get(),
            get(),
            Clock.System,
            timeZone,
            weekChanges = weekStarts(Clock.System, timeZone),
        )
    }
}
