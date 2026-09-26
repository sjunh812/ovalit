package com.ovalit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.designsystem.component.OvalitTab
import com.ovalit.core.designsystem.component.OvalitTabBar
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.feature.friend.FriendMatchesRoute
import com.ovalit.feature.friend.FriendProfileRoute
import com.ovalit.feature.friend.FriendsRoute
import com.ovalit.feature.match.MatchDetailRoute
import com.ovalit.feature.match.MatchesRoute
import com.ovalit.feature.onboarding.consent.ConsentScreen
import com.ovalit.feature.onboarding.importing.ImportRoute
import com.ovalit.feature.onboarding.intro.IntroScreen
import com.ovalit.feature.profile.AgentsRoute
import com.ovalit.feature.profile.ProfileRoute
import com.ovalit.feature.profile.WeaponsRoute
import com.ovalit.feature.report.ReportRoute
import com.ovalit.feature.settings.SettingsRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private data object Intro : NavKey

@Serializable
private data object Consent : NavKey

@Serializable
private data object Import : NavKey

@Serializable
private data object Report : NavKey

@Serializable
private data object Settings : NavKey

@Serializable
private data object Matches : NavKey

@Serializable
private data class MatchDetail(val id: String) : NavKey

@Serializable
private data object Friends : NavKey

@Serializable
private data class FriendProfile(val id: String) : NavKey

@Serializable
private data class FriendMatches(val id: String) : NavKey

@Serializable
private data object Profile : NavKey

@Serializable
private data object Agents : NavKey

@Serializable
private data object Weapons : NavKey

private val TopLevel = listOf(Report, Matches, Friends, Settings)

@Composable
fun OvalitApp(appVersion: String) {
    val backStack = rememberNavBackStack(Intro)
    // RSO가 붙기 전까지는 가짜 계정이다. S0-2의 계속하기가 연동을 대신한다.
    val account = koinInject<FakeAccountRepository>()
    val friends = koinInject<FriendRepository>()
    val importScheduler = koinInject<ImportScheduler>()
    val scope = rememberCoroutineScope()
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
                        IntroScreen(onStart = { backStack.add(Consent) })
                    }
                    entry<Consent> {
                        ConsentScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onContinue = {
                                // S0-3 Riot 로그인은 RSO가 붙으면 여기서 Custom Tabs로 연다. 지금은 가짜 계정으로 연동한다.
                                scope.launch {
                                    account.link()
                                    importScheduler.start()
                                }
                                backStack.replaceAllWith(Import)
                            },
                        )
                    }
                    entry<Import> {
                        RequestNotificationPermission()
                        ImportRoute(onOpenReport = { backStack.replaceAllWith(Report) })
                    }
                    entry<Report> {
                        ReportRoute(
                            onOpenProfile = { backStack.add(Profile) },
                            onShareInvite = { context.shareInvite(friends.inviteLink()) },
                            onOpenAgents = { backStack.add(Agents) },
                            onOpenWeapons = { backStack.add(Weapons) },
                        )
                    }
                    entry<Profile> {
                        ProfileRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenAgents = { backStack.add(Agents) },
                            onOpenWeapons = { backStack.add(Weapons) },
                            onOpenMatch = { backStack.add(MatchDetail(it.value)) },
                            onOpenMatches = { backStack.selectTab(Matches) },
                        )
                    }
                    entry<Matches> {
                        MatchesRoute(onOpenMatch = { backStack.add(MatchDetail(it.value)) })
                    }
                    entry<MatchDetail> { key ->
                        MatchDetailRoute(
                            matchId = MatchId(key.id),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenFriend = { backStack.add(FriendProfile(it.value)) },
                            onShareInvite = { context.shareInvite(friends.inviteLink()) },
                        )
                    }
                    entry<Friends> {
                        FriendsRoute(
                            onOpenFriend = { backStack.add(FriendProfile(it.value)) },
                            onShareInvite = { link -> context.shareInvite(link) },
                        )
                    }
                    entry<FriendProfile> { key ->
                        FriendProfileRoute(
                            friendId = PlayerId(key.id),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenMatches = { backStack.add(FriendMatches(key.id)) },
                        )
                    }
                    entry<FriendMatches> { key ->
                        FriendMatchesRoute(friendId = PlayerId(key.id), onBack = { backStack.removeLastOrNull() })
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
                    OvalitTab(stringResource(R.string.tab_home), OvalitIcons.Home, OvalitIcons.HomeFilled),
                    OvalitTab(stringResource(R.string.tab_matches), OvalitIcons.Matches, OvalitIcons.MatchesFilled),
                    OvalitTab(stringResource(R.string.tab_friends), OvalitIcons.Friends, OvalitIcons.FriendsFilled),
                    OvalitTab(stringResource(R.string.tab_settings), OvalitIcons.Settings, OvalitIcons.SettingsFilled),
                ),
                selectedIndex = selectedTab,
                onSelect = { index -> backStack.selectTab(TopLevel[index]) },
            )
        }
    }
}

// 홈은 늘 스택 맨 아래에 두고 다른 탭은 그 위에 하나만 둔다. 그래야 어느 탭에서 뒤로 가도 홈이 나오고 홈에서
// 뒤로 가면 앱이 닫힌다. 홈은 새로 띄우지 않아서 스크롤과 칩이 남는다.
private fun NavBackStack<NavKey>.selectTab(tab: NavKey) {
    if (last() == tab) return
    while (size > 1) removeAt(lastIndex)
    if (tab != Report) add(tab)
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

// S0-4 아래에 "다 모으면 알림으로 알려드릴게요"가 있어서 이 화면에 들어올 때 한 번 묻는다
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
