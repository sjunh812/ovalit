package com.ovalit.benchmarks

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱의 Baseline Profile을 만듭니다.
 * `./gradlew :composeApp:generateBaselineProfile`이 이 클래스만 돌리고 결과를 앱 소스에 넣습니다.
 *
 * 시작 프로필(앱을 켤 때 읽는 클래스를 첫 dex에 모으는 것)에는 켜서 홈이 뜰 때까지만 넣습니다.
 * 다른 화면까지 넣으면 첫 dex가 커져 오히려 시작이 느려집니다.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupToHome() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        passOnboarding()
    }

    @Test
    fun mainScreens() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        passOnboarding()
        scrollHome()
        visitMatches()
        visitOtherTabs()
    }
}
