package com.ovalit.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.component.OvalitCardGap
import com.ovalit.core.designsystem.component.OvalitPullToRefresh
import com.ovalit.core.designsystem.component.OvalitStage
import com.ovalit.core.designsystem.component.OvalitStaged
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MAX_REPORT_WEEKS
import com.ovalit.core.model.MIN_MATCHES_PER_REPORT
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.feature.report.component.DynamicMetricSection
import com.ovalit.feature.report.component.DynamicMetricSheet
import com.ovalit.feature.report.component.FixedMetricRow
import com.ovalit.feature.report.component.FixedMetricSummary
import com.ovalit.feature.report.component.FriendRankingSection
import com.ovalit.feature.report.component.InsightSection
import com.ovalit.feature.report.component.MetricSheet
import com.ovalit.feature.report.component.NudgeBanner
import com.ovalit.feature.report.component.PeriodHeader
import com.ovalit.feature.report.component.PeriodPicksSection
import com.ovalit.feature.report.component.PingHomeCard
import com.ovalit.feature.report.component.ProfileHint
import com.ovalit.feature.report.component.QueueChips
import com.ovalit.feature.report.component.RecordStrip
import com.ovalit.feature.report.component.ReportSkeleton
import com.ovalit.feature.report.component.ReportTopBar
import com.ovalit.feature.report.component.RivalPickerSheet
import com.ovalit.feature.report.component.RivalSection
import com.ovalit.feature.report.component.TrendEntry
import com.ovalit.feature.report.component.TrendSheet
import com.ovalit.feature.report.component.WeekNoteLines
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.not_enough_body
import com.ovalit.feature.report.resources.not_enough_title
import com.ovalit.feature.report.resources.not_enough_title_none
import com.ovalit.feature.report.resources.other_queue_hint
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** S1 홈입니다. 주간 리포트를 보여줍니다. */
@Composable
fun ReportRoute(
    onOpenProfile: () -> Unit,
    onShareInvite: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPing: (PingId) -> Unit = {},
    viewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val badge by viewModel.badge.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val homePing by viewModel.homePing.collectAsStateWithLifecycle()
    val profileHint by viewModel.profileHint.collectAsStateWithLifecycle()

    ReportScreen(
        uiState = uiState,
        onSelectQueue = viewModel::selectQueue,
        badge = badge,
        onOpenProfile = onOpenProfile,
        onShareInvite = onShareInvite,
        onSelectRival = viewModel::selectRival,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        catalog = catalog,
        onOpenAgents = onOpenAgents,
        onOpenWeapons = onOpenWeapons,
        modifier = modifier,
        homePing = homePing,
        onOpenPing = onOpenPing,
        timeZone = viewModel.timeZone,
        profileHint = profileHint,
        onProfileHintShown = viewModel::markProfileHintSeen,
    )
}

@Composable
internal fun ReportScreen(
    uiState: ReportUiState,
    onSelectQueue: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
    badge: PlayerBadge? = null,
    onOpenProfile: () -> Unit = {},
    onShareInvite: () -> Unit = {},
    onSelectRival: (PlayerId) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    catalog: ContentCatalog = ContentCatalog.Empty,
    onOpenAgents: () -> Unit = {},
    onOpenWeapons: () -> Unit = {},
    homePing: HomePing? = null,
    onOpenPing: (PingId) -> Unit = {},
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    profileHint: Boolean = false,
    onProfileHintShown: () -> Unit = {},
) {
    // 첫 수집 뒤 홈으로 넘어오면 리포트가 전환 한가운데 도착한다. 그때 홈 전체를 그리면 밀려 들어오던 화면이 한 번
    // 멈춰서, 다 들어올 때까지 스켈레톤을 둔다. 다 들어온 뒤에도 한 프레임에 다 그리면 그 프레임이 200ms 가까이 걸려 묶음마다
    // 나눠 그린다.
    val shown = rememberContentShown(loaded = uiState is ReportUiState.Success)
    val canvas = OvalitTheme.colors.canvas
    OvalitStaged(
        ready = uiState is ReportUiState.Success && shown,
        modifier = modifier
            .fillMaxSize()
            .background(canvas),
        contentBackground = canvas,
        // 빈 화면 대신 홈 모양대로 자리만 잡아 둔다
        placeholder = {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                ReportTopBar(badge = badge, onOpenProfile = onOpenProfile)
                Spacer(Modifier.height(OvalitSpacing.xs))
                ReportSkeleton()
            }
        },
    ) {
        if (uiState !is ReportUiState.Success) return@OvalitStaged
        // 내 프로필 안내는 한 번 띄우면 바로 봤다고 적고, 이번에 띄운 것은 누르거나 프로필을 열 때까지 둔다
        var hintShown by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(profileHint, badge != null) {
            if (profileHint && badge != null) {
                hintShown = true
                onProfileHintShown()
            }
        }
        // 머리 줄이 스크롤되면 아바타 자리가 바뀐다. 둘 다 화면 기준으로 적어 두고 그 차이로 안내를 둔다. 아바타는 잘리지 않은
        // 크기로 잰다. 잘린 자리(boundsInRoot)는 위로 스크롤돼 가려지는 동안 폭이 0까지 줄어 화살표 자리가 음수가 됐다.
        var screenOrigin by remember { mutableStateOf(Offset.Zero) }
        var avatarInRoot by remember { mutableStateOf<Rect?>(null) }
        val openProfile = {
            hintShown = false
            onOpenProfile()
        }
        Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { screenOrigin = it.positionInRoot() }) {
            OvalitPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ReportTopBar(
                        badge = badge,
                        onOpenProfile = openProfile,
                        onAvatarPositioned = { avatarInRoot = Rect(it.positionInRoot(), it.size.toSize()) },
                    )
                    Spacer(Modifier.height(OvalitSpacing.xs))
                    QueueChips(selected = uiState.queueFilter, onSelect = onSelectQueue)
                    Spacer(Modifier.height(OvalitSpacing.md))

                    // 큰 묶음마다 카드 하나다. 카드끼리는 화면 가장자리와 같은 간격을 둔다.
                    Column(verticalArrangement = Arrangement.spacedBy(OvalitCardGap)) {
                        // 답해야 하는 ㅇㅂㅇ이 리포트에 묻히지 않게 맨 위에 둔다
                        homePing?.let { home -> PingHomeCard(home = home, timeZone = timeZone, onClick = { onOpenPing(home.ping.id) }) }
                        when (val report = uiState.report) {
                            is WeeklyReport.Ready -> ReportContent(
                                report = report,
                                queueFilter = uiState.queueFilter,
                                rival = uiState.rival,
                                friends = uiState.friends,
                                nudge = uiState.nudge,
                                onShareInvite = onShareInvite,
                                onSelectRival = onSelectRival,
                                catalog = catalog,
                                onOpenAgents = onOpenAgents,
                                onOpenWeapons = onOpenWeapons,
                            )
                            is WeeklyReport.NotEnoughMatches -> NotEnoughMatches(played = report.played)
                        }
                    }

                    Spacer(Modifier.height(OvalitSpacing.xxl))
                }
            }
            if (hintShown) {
                ProfileHint(
                    avatar = avatarInRoot?.translate(-screenOrigin),
                    onDismiss = { hintShown = false },
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun ReportContent(
    report: WeeklyReport.Ready,
    queueFilter: QueueFilter,
    rival: FriendStanding?,
    friends: List<FriendStanding>,
    nudge: HomeNudge?,
    onShareInvite: () -> Unit,
    onSelectRival: (PlayerId) -> Unit,
    catalog: ContentCatalog,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
) {
    var openMetric by rememberSaveable { mutableStateOf<FixedMetric?>(null) }
    var openTrend by rememberSaveable { mutableStateOf(false) }
    var openDynamic by rememberSaveable { mutableStateOf<DynamicMetric?>(null) }
    var pickingRival by rememberSaveable { mutableStateOf(false) }

    // 한 프레임에 한 묶음씩 그린다. 무거운 칸(고정 칸, 달라진 점, 요원·무기)은 따로 묶는다.
    OvalitCard {
        OvalitStage {
            PeriodHeader(report)
            Spacer(Modifier.height(OvalitSpacing.md))
            RecordStrip(report)
        }
        Spacer(Modifier.height(OvalitSpacing.lg))
        OvalitStage {
            FixedMetricRow(
                metrics = report.metrics,
                baseline = report.baseline,
                fixedMetrics = queueFilter.fixedMetrics,
                onOpenMetric = { openMetric = it },
            )
            FixedMetricSummary(baseline = report.baseline)
        }
        // 짚을 점은 바로 위 숫자를 풀어 말하는 문장이라 고정 칸과 한 카드에 둔다(CLAUDE.md 화면)
        if (queueFilter.hasDynamicMetrics) {
            // 보이는 차이가 0이면 짚을 점이 그려지지 않으니 띄우는 것도 그 안에서 한다
            report.note?.let { note ->
                OvalitStage { WeekNoteLines(note = note, catalog = catalog, modifier = Modifier.padding(top = 18.dp)) }
            }
        }
        // 짚을 점은 바로 위 숫자를 풀어 말해서 숫자에 붙여 두고, 흐름을 보는 입구는 그 뒤에 둔다
        OvalitStage {
            TrendEntry(
                report = report,
                metric = queueFilter.fixedMetrics.first(),
                onClick = { openTrend = true },
                modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 16.dp),
            )
        }
    }

    if (queueFilter.hasDynamicMetrics) {
        if (report.dynamic.isNotEmpty()) {
            OvalitStage { OvalitCard { DynamicMetricSection(report, onOpenMetric = { openDynamic = it }) } }
        }
        report.insight?.let { insight ->
            OvalitStage {
                OvalitCard { InsightSection(insight = insight, role = report.mainRole, period = report.period, catalog = catalog) }
            }
        }
        // S6과 S7이 경쟁 + 일반만 보니 기타 모드에는 두지 않는다
        OvalitStage { PeriodPicksSection(report, catalog, onOpenAgents = onOpenAgents, onOpenWeapons = onOpenWeapons) }
        rival?.let {
            OvalitStage { OvalitCard { RivalSection(report = report, mine = report.metrics, rival = it) } }
        }
        nudge?.let {
            NudgeBanner(
                nudge = it,
                candidates = friends,
                onClick = {
                    when (it) {
                        HomeNudge.INVITE_FRIEND -> onShareInvite()
                        HomeNudge.PICK_RIVAL -> pickingRival = true
                    }
                },
            )
        }
        if (friends.any { it.metrics != null }) {
            OvalitStage { OvalitCard { FriendRankingSection(mine = report.metrics, friends = friends) } }
        }
    } else {
        OvalitCard {
            OvalitText(
                text = stringResource(Res.string.other_queue_hint),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        }
    }

    if (pickingRival) {
        RivalPickerSheet(
            report = report,
            candidates = friends,
            onPick = { id ->
                onSelectRival(id)
                pickingRival = false
            },
            onDismiss = { pickingRival = false },
        )
    }
    if (openTrend) {
        TrendSheet(report = report, metrics = queueFilter.fixedMetrics, onDismiss = { openTrend = false })
    }
    openMetric?.let { metric ->
        MetricSheet(metric = metric, report = report, onDismiss = { openMetric = null })
    }
    openDynamic?.let { metric ->
        DynamicMetricSheet(metric = metric, report = report, onDismiss = { openDynamic = null })
    }
}

@Composable
private fun NotEnoughMatches(played: Int) {
    OvalitCard {
        Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            OvalitText(
                text = if (played == 0) {
                    stringResource(Res.string.not_enough_title_none, MAX_REPORT_WEEKS)
                } else {
                    stringResource(Res.string.not_enough_title, MIN_MATCHES_PER_REPORT - played)
                },
                style = OvalitTheme.typography.titleL,
            )
            Spacer(Modifier.height(OvalitSpacing.sm))
            OvalitText(
                text = stringResource(Res.string.not_enough_body, MIN_MATCHES_PER_REPORT),
                style = OvalitTheme.typography.body,
                color = OvalitTheme.colors.t2,
            )
        }
    }
}
