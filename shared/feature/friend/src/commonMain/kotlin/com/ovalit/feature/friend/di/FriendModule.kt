package com.ovalit.feature.friend.di

import com.ovalit.core.data.minuteStarts
import com.ovalit.core.data.weekStarts
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import com.ovalit.feature.friend.FriendProfileViewModel
import com.ovalit.feature.friend.FriendsViewModel
import com.ovalit.feature.friend.PingDetailViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val friendModule = module {
    viewModel {
        FriendsViewModel(get(), get(), get(), Clock.System, TimeZone.currentSystemDefault(), analytics = get(), minuteChanges = minuteStarts(Clock.System))
    }
    viewModel { (id: String) ->
        PingDetailViewModel(
            PingId(id),
            get(),
            get(),
            get(),
            Clock.System,
            TimeZone.currentSystemDefault(),
            analytics = get(),
            minuteChanges = minuteStarts(Clock.System),
        )
    }
    viewModel { (id: String) ->
        val timeZone = TimeZone.currentSystemDefault()
        FriendProfileViewModel(
            PlayerId(id),
            get(),
            get(),
            get(),
            get(),
            Clock.System,
            timeZone,
            weekChanges = weekStarts(Clock.System, timeZone),
            minuteChanges = minuteStarts(Clock.System),
        )
    }
}
