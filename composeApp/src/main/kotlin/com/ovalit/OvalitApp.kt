package com.ovalit

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ovalit.feature.onboarding.intro.IntroScreen
import com.ovalit.feature.report.ReportRoute
import kotlinx.serialization.Serializable

@Serializable
private data object Intro : NavKey

@Serializable
private data object Report : NavKey

@Composable
fun OvalitApp() {
    val backStack = rememberNavBackStack(Intro)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Intro> {
                // 연동 동의(S0-2)부터 불러오는 중(S0-4)까지는 아직 없어서 가짜 데이터로 홈을 바로 연다.
                // 홈에서 뒤로 가면 인트로가 아니라 앱을 나가야 하니 인트로는 스택에서 뺀다.
                IntroScreen(
                    onStart = {
                        backStack.add(Report)
                        backStack.remove(Intro)
                    },
                )
            }
            entry<Report> { ReportRoute() }
        },
    )
}
