package com.ovalit.feature.profile.di

import com.ovalit.core.data.minuteStarts
import com.ovalit.core.data.weekStarts
import com.ovalit.feature.profile.ProfileViewModel
import com.ovalit.feature.profile.RecordsOwner
import com.ovalit.feature.profile.RecordsViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel {
        val timeZone = TimeZone.currentSystemDefault()
        ProfileViewModel(
            get(),
            get(),
            get(),
            Clock.System,
            timeZone,
            weekChanges = weekStarts(Clock.System, timeZone),
            minuteChanges = minuteStarts(Clock.System),
        )
    }
    viewModel { (owner: RecordsOwner) ->
        val timeZone = TimeZone.currentSystemDefault()
        RecordsViewModel(owner, get(), get(), get(), Clock.System, timeZone, weekChanges = weekStarts(Clock.System, timeZone))
    }
}
