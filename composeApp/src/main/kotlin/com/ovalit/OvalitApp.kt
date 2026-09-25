package com.ovalit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.designsystem.component.OvalitTab
import com.ovalit.core.designsystem.component.OvalitTabBar
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.onboarding.intro.IntroScreen
import com.ovalit.feature.report.ReportRoute
import com.ovalit.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private data object Intro : NavKey

@Serializable
private data object Report : NavKey

@Serializable
private data object Settings : NavKey

// 경기와 친구 화면이 생기면 여기에 탭을 더한다. 갈 화면이 없는 탭은 미리 두지 않는다.
private val TopLevel = listOf(Report, Settings)

@Composable
fun OvalitApp(appVersion: String) {
    val backStack = rememberNavBackStack(Intro)
    // RSO가 붙기 전까지는 가짜 계정이다. 시작 버튼이 연동을 대신한다.
    val account = koinInject<FakeAccountRepository>()
    val selectedTab = TopLevel.indexOf(backStack.lastOrNull())
    val showTabBar = selectedTab >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.bg),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (showTabBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
        ) {
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
                        IntroScreen(
                            onStart = {
                                account.link()
                                backStack.replaceAllWith(Report)
                            },
                        )
                    }
                    entry<Report> { ReportRoute() }
                    entry<Settings> {
                        SettingsRoute(
                            appVersion = appVersion,
                            onUnlinked = { backStack.replaceAllWith(Intro) },
                        )
                    }
                },
            )
        }

        if (showTabBar) {
            OvalitTabBar(
                tabs = listOf(
                    OvalitTab(stringResource(R.string.tab_home), OvalitIcons.Home),
                    OvalitTab(stringResource(R.string.tab_settings), OvalitIcons.Settings),
                ),
                selectedIndex = selectedTab,
                onSelect = { index ->
                    // 홈이 늘 바닥에 있다. 설정에서 뒤로 가면 홈으로, 홈에서 뒤로 가면 앱을 나간다.
                    // 홈은 새로 만들지 않고 위에 쌓인 것만 걷어서 스크롤과 고른 칩을 그대로 둔다.
                    val tab = TopLevel[index]
                    when {
                        tab == Report -> while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        backStack.last() != tab -> backStack.add(tab)
                    }
                },
            )
        }
    }
}

// 스택을 비우는 순간이 없게 새 키를 먼저 넣고 나머지를 뺀다. NavDisplay는 빈 스택을 받지 않는다.
private fun NavBackStack<NavKey>.replaceAllWith(vararg keys: NavKey) {
    addAll(keys)
    repeat(size - keys.size) { removeAt(0) }
}
