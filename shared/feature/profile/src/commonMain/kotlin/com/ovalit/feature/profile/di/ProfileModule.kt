package com.ovalit.feature.profile.di

import com.ovalit.feature.profile.ProfileViewModel
import com.ovalit.feature.profile.RecordsOwner
import com.ovalit.feature.profile.RecordsViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel { ProfileViewModel(get(), get(), get(), Clock.System, TimeZone.currentSystemDefault()) }
    viewModel { (owner: RecordsOwner) ->
        RecordsViewModel(owner, get(), get(), get(), Clock.System, TimeZone.currentSystemDefault())
    }
}
