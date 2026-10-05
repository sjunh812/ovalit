package com.ovalit.app

import com.ovalit.core.data.di.dataModule
import com.ovalit.feature.friend.di.friendModule
import com.ovalit.feature.match.di.matchModule
import com.ovalit.feature.onboarding.di.onboardingModule
import com.ovalit.feature.profile.di.profileModule
import com.ovalit.feature.report.di.reportModule
import com.ovalit.feature.settings.di.settingsModule
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.Module

/**
 * 안드로이드와 iOS가 같이 쓰는 Koin 모듈입니다. `ImportScheduler`와 `Analytics`는 플랫폼마다 달라 부르는 쪽이 더합니다.
 *
 * @param preferencesPath 설정을 담을 파일 경로입니다.
 * @param language 화면 언어입니다. 요원·맵·무기·티어 이름을 이 언어로 받습니다.
 */
fun ovalitModules(preferencesPath: () -> String, language: Flow<String>): List<Module> = listOf(
    dataModule(preferencesPath = preferencesPath, language = language),
    onboardingModule,
    reportModule,
    matchModule,
    friendModule,
    profileModule,
    settingsModule,
)
