package com.ovalit.app

import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.ovalit.app.resources.Res
import com.ovalit.app.resources.feedback_no_mail_app
import com.ovalit.app.resources.tab_friends
import com.ovalit.app.resources.tab_home
import com.ovalit.app.resources.tab_matches
import com.ovalit.app.resources.tab_settings
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.designsystem.component.OvalitLaunchAnimation
import com.ovalit.core.designsystem.component.SplashHandoff
import com.ovalit.core.designsystem.component.LocalOvalitToast
import com.ovalit.core.designsystem.component.LocalScreenEntering
import com.ovalit.core.designsystem.component.LocalTabReselects
import com.ovalit.core.designsystem.component.OvalitTab
import com.ovalit.core.designsystem.component.OvalitTabBar
import com.ovalit.core.designsystem.component.OvalitTabBarHeight
import com.ovalit.core.designsystem.component.OvalitToastHost
import com.ovalit.core.designsystem.component.rememberOvalitToastState
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.NotificationPrimerHost
import com.ovalit.core.ui.rememberNotificationPrimer
import com.ovalit.feature.friend.FriendMatchesRoute
import com.ovalit.feature.friend.FriendProfileRoute
import com.ovalit.feature.friend.FriendsRoute
import com.ovalit.feature.friend.PingDetailRoute
import com.ovalit.feature.match.MatchDetailRoute
import com.ovalit.feature.match.MatchesRoute
import com.ovalit.feature.onboarding.consent.ConsentScreen
import com.ovalit.feature.onboarding.importing.ImportRoute
import com.ovalit.feature.onboarding.intro.IntroScreen
import com.ovalit.feature.profile.AgentsRoute
import com.ovalit.feature.profile.ProfileRoute
import com.ovalit.feature.profile.RecordsOwner
import com.ovalit.feature.profile.WeaponsRoute
import com.ovalit.feature.report.FriendRankingRoute
import com.ovalit.feature.report.ReportRoute
import com.ovalit.feature.settings.SettingsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val TopLevel: List<NavKey> = listOf(Report, Matches, Friends, Settings)

// 스택을 저장할 때 키마다 쓸 직렬화기다.
// iOS에는 리플렉션이 없어 알려 줘야 하고, 빠진 키가 있으면 스택을 저장할 때 앱이 죽는다.
// [Screen]을 구현한 키는 모두 들어간다.
@OptIn(ExperimentalSerializationApi::class)
private val BackStackConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Screen>() }
    }
}

/**
 * 안드로이드와 iOS가 같이 쓰는 앱 뼈대입니다.
 * 화면 전환, 탭, 토스트를 두고, 플랫폼마다 다른 일은 [platform]에 맡깁니다.
 *
 * @param openPing ㅇㅂㅇ 알림을 눌러 열 초대 ID입니다.
 *   알림이 없는 iOS는 비워 둡니다.
 * @param openFriends 친구 요청 알림을 누를 때마다 하나씩 옵니다.
 *   받으면 친구 탭을 엽니다.
 * @param splashHandoff 시스템 스플래시의 점 애니메이션을 로고 애니메이션이 이어받는 길입니다.
 *   스플래시가 움직이지 않는 iOS는 비워 둡니다.
 */
@Composable
fun OvalitApp(
    appVersion: String,
    platform: OvalitPlatform,
    openPing: Flow<String> = emptyFlow(),
    openFriends: Flow<Unit> = emptyFlow(),
    splashHandoff: SplashHandoff = SplashHandoff.None,
) {
    val backStack = rememberNavBackStack(BackStackConfiguration, Intro)
    // RSO가 붙기 전까지는 가짜 계정이다.
    // S0-2의 계속하기가 연동을 대신한다.
    val account = koinInject<FakeAccountRepository>()
    // 계정을 읽기 전에는 `null`이다
    val accountLinked by remember(account) { account.account.map { it != null } }.collectAsState(initial = null)
    // 알림으로 연 초대는 열 수 있을 때까지 들고 있는다.
    // 계정을 읽기 전에 온 것을 버리면 앱이 꺼진 채 누른 알림이 그냥 앱만 연다.
    var pendingPing by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(openPing) { openPing.collect { pendingPing = it } }
    // ㅇㅂㅇ 알림을 누르면 친구 탭 위에 그 초대 화면을 연다.
    // 연동 전이면 버린다.
    // 불러오는 중(S0-4)이면 탭을 바꾸지 않고 그 위에 연다.
    // 탭을 바꾸면 불러오는 화면이 닫혀 돌아갈 곳이 없다.
    LaunchedEffect(pendingPing, accountLinked) {
        val id = pendingPing ?: return@LaunchedEffect
        val isLinked = accountLinked ?: return@LaunchedEffect
        pendingPing = null
        if (!isLinked || backStack.last() == PingDetail(id)) return@LaunchedEffect
        if (Report in backStack && Import !in backStack) backStack.selectTab(Friends)
        backStack.push(PingDetail(id))
    }
    // 친구 요청 알림을 누르면 받은 요청이 있는 친구 탭을 연다.
    // 연동 전이거나 불러오는 중(S0-4)이면 그대로 둔다.
    LaunchedEffect(openFriends) {
        openFriends.collect {
            if (Report in backStack && Import !in backStack) backStack.selectTab(Friends)
        }
    }
    val friends = koinInject<FriendRepository>()
    val importScheduler = koinInject<ImportScheduler>()
    val scope = rememberCoroutineScope()
    val dimmedAlpha = dimmedAlpha(OvalitTheme.colors.isDark)
    // 탭을 바꿀 때는 홈이 스택에 남는다.
    // 연동을 해제하면 홈까지 빠지니 그때 남겨 둔 탭 화면도 지운다.
    val linked = Report in backStack
    val tabState = rememberTabStateDecorator(keepTabs = { Report in backStack })
    LaunchedEffect(linked) { if (!linked) tabState.clearKept() }

    val toast = rememberOvalitToastState()
    val analytics = koinInject<Analytics>()

    // 알림 권한은 앱을 열 때 묻지 않고 쓸모를 알 때 묻는다.
    // S0-4는 사용자가 직접 켜는 버튼을 두고, 친구를 맺거나 파티를 모집하면 한 번만 시트로 묻는다(docs/screens.md).
    val notifications = platform.rememberNotificationPermission()
    val preferences = koinInject<UserPreferencesRepository>()
    // 기기에서 읽기 전에는 물은 적이 있는 것으로 둔다
    val primerSeen by remember(preferences) { preferences.preferences.map { it.seenNotificationPrimer } }.collectAsState(initial = true)
    val primer = rememberNotificationPrimer(
        permission = notifications,
        seen = primerSeen,
        // 적지 못하면 다음에 한 번 더 물을 뿐이라 안내하지 않는다
        onSeen = { scope.launch { runCatching { preferences.setSeenNotificationPrimer() } } },
    )
    val top = backStack.lastOrNull()
    LaunchedEffect(top) { (top as? Screen)?.let { analytics.screen(screenName(it)) } }

    // 앱을 켤 때 시스템 스플래시에 이어 로고 애니메이션을 한 번 재생한다.
    // 화면을 돌리거나 언어를 바꿔 다시 그려도 다시 재생하지 않는다.
    var launching by rememberSaveable { mutableStateOf(true) }
    // 다시 만들어진 화면에서 재생하지 않을 때도 스플래시는 걷어야 한다
    LaunchedEffect(Unit) { if (!launching) splashHandoff.takeOver() }

    // 전환 중에 아래 화면이 어두워 보이게 하는 검은 바탕이다(OvalitTransitions).
    // 화면이 모두 불투명해서 평소에는 안 보인다.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CompositionLocalProvider(LocalOvalitToast provides toast) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(tabState.decorator, rememberOpaqueEntryDecorator()),
                transitionSpec = pushTransition(dimmedAlpha),
                popTransitionSpec = popTransition(dimmedAlpha),
                predictivePopTransitionSpec = predictivePopTransition(dimmedAlpha),
                entryProvider = entryProvider {
                    entry<Intro> {
                        IntroScreen(onStart = { backStack.push(Consent) })
                    }
                    entry<Consent> {
                        ConsentScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onContinue = {
                                // 넘어가는 동안 한 번 더 누르면 로그인 창이 두 번 뜬다
                                if (backStack.lastOrNull() != Consent) return@ConsentScreen
                                // S0-3 Riot 로그인은 RSO가 붙으면 여기서 Custom Tabs로 연다.
                                // 지금은 가짜 계정으로 연동한다.
                                scope.launch {
                                    account.link()
                                    importScheduler.start()
                                }
                                backStack.replaceAllWith(Import)
                            },
                        )
                    }
                    entry<Import> {
                        ImportRoute(onOpenReport = { backStack.replaceAllWith(Report) }, notifications = notifications)
                    }
                    entry<Report> {
                        if (backStack.size == 1) platform.HomeBackHandler(toast)
                        TabScaffold(selected = Report, onSelect = backStack::selectTab) {
                            ReportRoute(
                                onOpenProfile = { backStack.push(Profile) },
                                onShareInvite = { platform.shareInvite(friends.inviteLink(), analytics) },
                                onOpenAgents = { backStack.push(Agents(it.name)) },
                                onOpenWeapons = { backStack.push(Weapons(it.name)) },
                                onOpenPing = { backStack.push(PingDetail(it.value)) },
                                // 설정에서 저장된 데이터를 지웠으면 첫 수집을 다시 띄우고 S0-4에서 받는 걸 보여준다
                                onReimport = {
                                    importScheduler.start()
                                    backStack.push(Import)
                                },
                                onOpenFriendRanking = { queue, metric -> backStack.push(FriendRanking(queue.name, metric.name)) },
                            )
                        }
                    }
                    entry<Profile> {
                        ProfileRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenAgents = { backStack.push(Agents(it.name)) },
                            onOpenWeapons = { backStack.push(Weapons(it.name)) },
                            onOpenMatch = { backStack.push(MatchDetail(it.value)) },
                            onOpenMatches = { backStack.selectTab(Matches) },
                        )
                    }
                    entry<Matches>(clazzContentKey = ::tabContentKey, metadata = TabTransitions) {
                        TabScaffold(selected = Matches, onSelect = backStack::selectTab) {
                            MatchesRoute(onOpenMatch = { backStack.push(MatchDetail(it.value)) })
                        }
                    }
                    entry<MatchDetail> { key ->
                        MatchDetailRoute(
                            matchId = MatchId(key.id),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenFriend = { backStack.push(FriendProfile(it.value)) },
                            onOpenMe = { backStack.openProfile() },
                            onShareInvite = { platform.shareInvite(friends.inviteLink(), analytics) },
                            onFriendAdded = primer::offer,
                        )
                    }
                    entry<Friends>(clazzContentKey = ::tabContentKey, metadata = TabTransitions) {
                        TabScaffold(selected = Friends, onSelect = backStack::selectTab) {
                            FriendsRoute(
                                onOpenFriend = { backStack.push(FriendProfile(it.value)) },
                                onOpenPing = { backStack.push(PingDetail(it.value)) },
                                onShareInvite = { link -> platform.shareInvite(link, analytics) },
                                onFriendAdded = primer::offer,
                                onPingSent = primer::offer,
                            )
                        }
                    }
                    entry<FriendProfile> { key ->
                        FriendProfileRoute(
                            friendId = PlayerId(key.id),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenMatches = { backStack.push(FriendMatches(key.id)) },
                            onOpenAgents = { backStack.push(FriendAgents(key.id, it.name)) },
                            onOpenWeapons = { backStack.push(FriendWeapons(key.id, it.name)) },
                        )
                    }
                    entry<PingDetail> { key ->
                        // 앱에서 초대를 열면 그 초대의 알림을 거둔다.
                        // 남은 알림 버튼을 나중에 누르면 앱에서 한 답을 덮어쓴다.
                        LaunchedEffect(key.id) { platform.clearPingNotification(key.id) }
                        PingDetailRoute(pingId = key.id, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<FriendMatches> { key ->
                        FriendMatchesRoute(friendId = PlayerId(key.id), onBack = { backStack.removeLastOrNull() })
                    }
                    entry<FriendAgents> { key ->
                        AgentsRoute(
                            owner = RecordsOwner.Friend(PlayerId(key.id)),
                            onBack = { backStack.removeLastOrNull() },
                            queueFilter = QueueFilter.valueOf(key.queue),
                        )
                    }
                    entry<FriendWeapons> { key ->
                        WeaponsRoute(
                            owner = RecordsOwner.Friend(PlayerId(key.id)),
                            onBack = { backStack.removeLastOrNull() },
                            queueFilter = QueueFilter.valueOf(key.queue),
                        )
                    }
                    entry<FriendRanking> { key ->
                        FriendRankingRoute(
                            queueFilter = QueueFilter.valueOf(key.queue),
                            metric = FixedMetric.valueOf(key.metric),
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Agents> { key ->
                        AgentsRoute(owner = RecordsOwner.Me, onBack = { backStack.removeLastOrNull() }, queueFilter = QueueFilter.valueOf(key.queue))
                    }
                    entry<Weapons> { key ->
                        WeaponsRoute(owner = RecordsOwner.Me, onBack = { backStack.removeLastOrNull() }, queueFilter = QueueFilter.valueOf(key.queue))
                    }
                    entry<Settings>(clazzContentKey = ::tabContentKey, metadata = TabTransitions) {
                        TabScaffold(selected = Settings, onSelect = backStack::selectTab) {
                            SettingsRoute(
                                appVersion = appVersion,
                                onUnlinked = { backStack.replaceAllWith(Intro) },
                                onOpenProfile = { backStack.openProfile() },
                                notificationBlocks = platform.rememberNotificationBlocks(),
                                onOpenNotificationSettings = platform::openNotificationSettings,
                                onSendFeedback = platform.feedbackAddress?.let { address ->
                                    {
                                        // 메일 앱이 없는 기기도 있어서 그때는 주소를 알려 준다
                                        if (!platform.sendFeedback(appVersion)) {
                                            scope.launch { toast.show(getString(Res.string.feedback_no_mail_app, address)) }
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
            )
            NotificationPrimerHost(primer)
        }
        // 탭 화면에서는 탭바를 가리지 않게 토스트를 그 위에 띄운다
        val onTab = backStack.lastOrNull() in TopLevel
        OvalitToastHost(
            state = toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (onTab) OvalitTabBarHeight + OvalitSpacing.md else OvalitSpacing.lg),
        )
        // 밑 화면은 그동안 그려 두고 애니메이션이 흐려지면 드러난다
        if (launching) OvalitLaunchAnimation(onFinished = { launching = false }, splash = splashHandoff)
    }
}

// 탭바는 탭 화면마다 안에 둔다(docs/screens.md).
// 밖에 하나만 두면 새 화면으로 넘어갈 때 탭바가 먼저 사라져서 밀려나는 화면이 탭바 높이만큼 늘어나고 목록이 튄다.
@Composable
private fun TabScaffold(selected: NavKey, onSelect: (NavKey) -> Unit, content: @Composable () -> Unit) {
    // 지금 탭을 한 번 더 누르면 그 탭 화면을 맨 위로 올린다(ScrollToTopOnReselect)
    val reselects = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    Column(modifier = Modifier.fillMaxSize()) {
        // 아래 내비게이션 바 여백은 탭바가 두니 본문은 또 두지 않는다.
        // 가로 화면에서 옆에 붙는 내비게이션 바는 본문도 피해야 해서 아래쪽만 소비한다.
        Box(
            modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) { CompositionLocalProvider(LocalTabReselects provides reselects) { content() } }
        OvalitTabBar(
            tabs = listOf(
                OvalitTab(stringResource(Res.string.tab_home), OvalitIcons.Home, OvalitIcons.HomeFilled),
                OvalitTab(stringResource(Res.string.tab_matches), OvalitIcons.Matches, OvalitIcons.MatchesFilled),
                OvalitTab(stringResource(Res.string.tab_friends), OvalitIcons.Friends, OvalitIcons.FriendsFilled),
                OvalitTab(stringResource(Res.string.tab_settings), OvalitIcons.Settings, OvalitIcons.SettingsFilled),
            ),
            selectedIndex = TopLevel.indexOf(selected),
            onSelect = { index ->
                val tab = TopLevel[index]
                if (tab == selected) reselects.tryEmit(Unit) else onSelect(tab)
            },
        )
    }
}

// 화면마다 바탕을 깐다.
// 바탕이 빈 화면이 있으면 전환 중에 뒤의 검은 바탕이 비친다.
// 양옆 바깥의 선은 평소에는 화면 밖이라 안 보이고 전환 중에만 두 화면 사이에 보인다.
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

// 전환 중에 같은 줄을 두 번 누르면 같은 화면이 두 번 쌓인다.
// 같은 키가 둘이면 저장 상태 키가 겹쳐 앱이 죽는다.
private fun NavBackStack<NavKey>.push(key: NavKey) {
    if (lastOrNull() != key) add(key)
}

// 홈은 늘 스택 맨 아래에 두고 다른 탭은 그 위에 하나만 둔다.
// 그래야 어느 탭에서 뒤로 가도 홈이 나오고 홈에서 뒤로 가면 앱이 닫힌다(docs/screens.md).
private fun NavBackStack<NavKey>.selectTab(tab: NavKey) {
    if (last() == tab) return
    while (size > 1) removeAt(lastIndex)
    if (tab != Report) add(tab)
}

// 내 프로필 → 최근 경기 → 나를 누르면 프로필이 또 쌓이지 않게 아래에 있던 프로필로 돌아간다
private fun NavBackStack<NavKey>.openProfile() {
    val index = lastIndexOf(Profile)
    if (index < 0) add(Profile) else while (lastIndex > index) removeAt(lastIndex)
}

// NavDisplay에 빈 스택을 넘기면 예외가 난다.
// 그래서 새 화면을 먼저 넣고 나머지를 뺀다.
private fun NavBackStack<NavKey>.replaceAllWith(vararg keys: NavKey) {
    addAll(keys)
    repeat(size - keys.size) { removeAt(0) }
}

private fun OvalitPlatform.shareInvite(link: String, analytics: Analytics) {
    analytics.log(AnalyticsEvents.SHARE, mapOf("method" to "invite_link", "content_type" to "invite"))
    shareInvite(link)
}
