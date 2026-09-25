package com.ovalit.feature.friend.di

import com.ovalit.core.model.PlayerId
import com.ovalit.feature.friend.FriendProfileViewModel
import com.ovalit.feature.friend.FriendsViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val friendModule = module {
    viewModel { FriendsViewModel(get(), Clock.System, TimeZone.currentSystemDefault()) }
    viewModel { (id: String) ->
        FriendProfileViewModel(PlayerId(id), get(), get(), get(), Clock.System, TimeZone.currentSystemDefault())
    }
}
