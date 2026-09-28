package com.ovalit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.designsystem.component.LocalScreenEntering
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
import com.ovalit.feature.profile.RecordsOwner
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

@Serializable
private data class FriendAgents(val id: String) : NavKey

@Serializable
private data class FriendWeapons(val id: String) : NavKey

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
    val dimmedAlpha = dimmedAlpha(OvalitTheme.colors.isDark)

    // 전환 중에 아래 화면이 어두워 보이게 하는 검은 바탕이다(OvalitTransitions). 화면이 모두 불투명해서 평소에는 안 보인다.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                rememberOpaqueEntryDecorator(),
            ),
            transitionSpec = pushTransition(dimmedAlpha),
            popTransitionSpec = popTransition(dimmedAlpha),
            predictivePopTransitionSpec = predictivePopTransition(dimmedAlpha),
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
                    TabScaffold(selected = Report, onSelect = backStack::selectTab) {
                        ReportRoute(
                            onOpenProfile = { backStack.add(Profile) },
                            onShareInvite = { context.shareInvite(friends.inviteLink()) },
                            onOpenAgents = { backStack.add(Agents) },
                            onOpenWeapons = { backStack.add(Weapons) },
                        )
                    }
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
                entry<Matches>(metadata = TabTransitions) {
                    TabScaffold(selected = Matches, onSelect = backStack::selectTab) {
                        MatchesRoute(onOpenMatch = { backStack.add(MatchDetail(it.value)) })
                    }
                }
                entry<MatchDetail> { key ->
                    MatchDetailRoute(
                        matchId = MatchId(key.id),
                        onBack = { backStack.removeLastOrNull() },
                        onOpenFriend = { backStack.add(FriendProfile(it.value)) },
                        onShareInvite = { context.shareInvite(friends.inviteLink()) },
                    )
                }
                entry<Friends>(metadata = TabTransitions) {
                    TabScaffold(selected = Friends, onSelect = backStack::selectTab) {
                        FriendsRoute(
                            onOpenFriend = { backStack.add(FriendProfile(it.value)) },
                            onShareInvite = { link -> context.shareInvite(link) },
                        )
                    }
                }
                entry<FriendProfile> { key ->
                    FriendProfileRoute(
                        friendId = PlayerId(key.id),
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMatches = { backStack.add(FriendMatches(key.id)) },
                        onOpenAgents = { backStack.add(FriendAgents(key.id)) },
                        onOpenWeapons = { backStack.add(FriendWeapons(key.id)) },
                    )
                }
                entry<FriendMatches> { key ->
                    FriendMatchesRoute(friendId = PlayerId(key.id), onBack = { backStack.removeLastOrNull() })
                }
                entry<FriendAgents> { key ->
                    AgentsRoute(owner = RecordsOwner.Friend(PlayerId(key.id)), onBack = { backStack.removeLastOrNull() })
                }
                entry<FriendWeapons> { key ->
                    WeaponsRoute(owner = RecordsOwner.Friend(PlayerId(key.id)), onBack = { backStack.removeLastOrNull() })
                }
                entry<Agents> { AgentsRoute(owner = RecordsOwner.Me, onBack = { backStack.removeLastOrNull() }) }
                entry<Weapons> { WeaponsRoute(owner = RecordsOwner.Me, onBack = { backStack.removeLastOrNull() }) }
                entry<Settings>(metadata = TabTransitions) {
                    TabScaffold(selected = Settings, onSelect = backStack::selectTab) {
                        SettingsRoute(
                            appVersion = appVersion,
                            onUnlinked = { backStack.replaceAllWith(Intro) },
                        )
                    }
                }
            },
        )
    }
}

// 탭바는 탭 화면마다 안에 둔다(CLAUDE.md 화면). 밖에 하나만 두면 새 화면으로 넘어갈 때 탭바가 먼저 사라져서
// 밀려나는 화면이 탭바 높이만큼 늘어나고 목록이 튄다.
@Composable
private fun TabScaffold(selected: NavKey, onSelect: (NavKey) -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 탭바가 아래 내비게이션 바 높이만큼 여백을 두니 본문은 아래 여백을 또 두지 않는다. 가로 화면에서 옆에 붙는
        // 내비게이션 바는 본문도 피해야 해서 아래쪽만 쓴 것으로 친다.
        Box(
            modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) { content() }
        OvalitTabBar(
            tabs = listOf(
                OvalitTab(stringResource(R.string.tab_home), OvalitIcons.Home, OvalitIcons.HomeFilled),
                OvalitTab(stringResource(R.string.tab_matches), OvalitIcons.Matches, OvalitIcons.MatchesFilled),
                OvalitTab(stringResource(R.string.tab_friends), OvalitIcons.Friends, OvalitIcons.FriendsFilled),
                OvalitTab(stringResource(R.string.tab_settings), OvalitIcons.Settings, OvalitIcons.SettingsFilled),
            ),
            selectedIndex = TopLevel.indexOf(selected),
            onSelect = { index -> onSelect(TopLevel[index]) },
        )
    }
}

// 화면마다 바탕을 깐다. 전환 중에 바탕이 빈 화면이 있으면 뒤의 검은 바탕이 그대로 보인다. 양옆 바깥에는 선을
// 긋는다. 전환 중이 아닐 때는 화면 밖이라 안 보이고, 밀려 들어오거나 스와이프로 밀어낼 때만 두 화면 사이에 보인다.
@Composable
private fun rememberOpaqueEntryDecorator(): NavEntryDecorator<NavKey> {
    val background = OvalitTheme.colors.bg
    val edge = OvalitTheme.colors.line
    return remember(background, edge) {
        NavEntryDecorator { entry ->
            // 밀려 들어오는 동안에는 화면이 무거운 내용을 늦게 그리게 알린다(rememberContentShown)
            val transition = LocalNavAnimatedContentScope.current.transition
            val entering = transition.targetState == EnterExitState.Visible && transition.currentState != EnterExitState.Visible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val width = EdgeWidth.toPx()
                        drawRect(edge, topLeft = Offset(-width, 0f), size = Size(width, size.height))
                        drawRect(edge, topLeft = Offset(size.width, 0f), size = Size(width, size.height))
                    }
                    .background(background),
            ) {
                CompositionLocalProvider(LocalScreenEntering provides entering) { entry.Content() }
            }
        }
    }
}

private val EdgeWidth = 1.dp

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

// S0-4 아래에 "다 모으면 알림으로 알려드릴게요"라고 적혀 있어서 이 화면에 들어올 때 한 번 묻는다
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
