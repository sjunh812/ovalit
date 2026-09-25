package com.ovalit

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.ovalit.core.model.PlayerId
import com.ovalit.feature.friend.FriendProfileRoute
import com.ovalit.feature.friend.FriendsRoute
import com.ovalit.feature.onboarding.intro.IntroScreen
import com.ovalit.feature.profile.AgentsRoute
import com.ovalit.feature.profile.ProfileRoute
import com.ovalit.feature.profile.WeaponsRoute
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

@Serializable
private data object Friends : NavKey

@Serializable
private data class FriendProfile(val id: String) : NavKey

@Serializable
private data object Profile : NavKey

@Serializable
private data object Agents : NavKey

@Serializable
private data object Weapons : NavKey

// 경기 화면이 생기면 여기에 탭을 더한다. 갈 화면이 없는 탭은 미리 두지 않는다.
private val TopLevel = listOf(Report, Friends, Settings)

@Composable
fun OvalitApp(appVersion: String) {
    val backStack = rememberNavBackStack(Intro)
    // RSO가 붙기 전까지는 가짜 계정이다. 시작 버튼이 연동을 대신한다.
    val account = koinInject<FakeAccountRepository>()
    val context = LocalContext.current
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
                    entry<Report> { ReportRoute(onOpenProfile = { backStack.add(Profile) }) }
                    entry<Profile> {
                        ProfileRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenAgents = { backStack.add(Agents) },
                            onOpenWeapons = { backStack.add(Weapons) },
                        )
                    }
                    entry<Friends> {
                        FriendsRoute(
                            onOpenFriend = { backStack.add(FriendProfile(it.value)) },
                            onShareInvite = { link -> context.shareInvite(link) },
                        )
                    }
                    entry<FriendProfile> { key ->
                        FriendProfileRoute(friendId = PlayerId(key.id), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<Agents> { AgentsRoute(onBack = { backStack.removeLastOrNull() }) }
                    entry<Weapons> { WeaponsRoute(onBack = { backStack.removeLastOrNull() }) }
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
                    OvalitTab(stringResource(R.string.tab_friends), OvalitIcons.Friends),
                    OvalitTab(stringResource(R.string.tab_settings), OvalitIcons.Settings),
                ),
                selectedIndex = selectedTab,
                onSelect = { index ->
                    // 홈은 늘 스택 맨 아래에 두고 다른 탭은 그 위에 하나만 둔다. 그래야 어느 탭에서 뒤로 가도
                    // 홈이 나오고 홈에서 뒤로 가면 앱이 닫힌다. 홈은 새로 띄우지 않아서 스크롤과 칩이 남는다.
                    val tab = TopLevel[index]
                    if (backStack.last() != tab) {
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        if (tab != Report) backStack.add(tab)
                    }
                },
            )
        }
    }
}

// NavDisplay에 빈 스택을 넘기면 예외가 난다. 그래서 새 화면을 먼저 넣고 나머지를 뺀다.
private fun NavBackStack<NavKey>.replaceAllWith(vararg keys: NavKey) {
    addAll(keys)
    repeat(size - keys.size) { removeAt(0) }
}

private fun Context.shareInvite(link: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    startActivity(Intent.createChooser(send, getString(R.string.share_invite)))
}
