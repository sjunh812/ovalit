package com.ovalit.feature.match.di

import com.ovalit.core.data.minuteStarts
import com.ovalit.core.model.MatchId
import com.ovalit.feature.match.MatchDetailViewModel
import com.ovalit.feature.match.MatchesViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val matchModule = module {
    viewModel {
        MatchesViewModel(get(), get(), get(), Clock.System, TimeZone.currentSystemDefault(), analytics = get(), minuteChanges = minuteStarts(Clock.System))
    }
    viewModel { (id: String) ->
        MatchDetailViewModel(MatchId(id), get(), get(), get(), TimeZone.currentSystemDefault(), analytics = get())
    }
}
