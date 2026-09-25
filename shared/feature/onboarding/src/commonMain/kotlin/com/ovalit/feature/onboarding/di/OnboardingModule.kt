package com.ovalit.feature.onboarding.di

import com.ovalit.feature.onboarding.importing.ImportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule = module {
    viewModel { ImportViewModel(get(), get()) }
}
