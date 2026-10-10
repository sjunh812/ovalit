package com.ovalit.feature.match

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPressOutset
import com.ovalit.core.designsystem.component.OvalitStaged
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTopBarCaption
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Standing
import com.ovalit.core.model.halfScores
import com.ovalit.core.model.myStanding
import com.ovalit.core.ui.AdPlacement
import com.ovalit.core.ui.AdSlot
import com.ovalit.core.ui.FailureNoticesEffect
import com.ovalit.core.ui.MapImage
import com.ovalit.core.ui.MapImageStyle
import com.ovalit.core.ui.ResultLabel
import com.ovalit.core.ui.SEPARATOR
import com.ovalit.core.ui.label
import com.ovalit.core.ui.mapName
import com.ovalit.core.ui.rankText
import com.ovalit.core.ui.resultColor
import com.ovalit.core.ui.standingColor
import com.ovalit.core.ui.standingText
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.detail_caption
import com.ovalit.feature.match.resources.detail_date
import com.ovalit.feature.match.resources.half_first
import com.ovalit.feature.match.resources.half_overtime
import com.ovalit.feature.match.resources.half_second
import com.ovalit.feature.match.resources.score_description
import com.ovalit.feature.match.resources.standing_of_players
import com.ovalit.feature.match.resources.standing_of_teams
import com.ovalit.feature.match.resources.tab_report
import com.ovalit.feature.match.resources.tab_rounds
import com.ovalit.feature.match.resources.tab_scoreboard
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val BannerHeight = 196.dp
private val TouchSize = 44.dp

internal enum class DetailTab { SCOREBOARD, ROUNDS, REPORT }

/**
 * S3 경기 상세입니다.
 *
 * @param onOpenMe 스코어보드에서 내 줄을 누르면 부릅니다.
 * @param onShareInvite 앱을 안 쓰거나 쓰는지 모르는 플레이어에게 초대 링크를 보낼 때 부릅니다.
 *   공유 시트는 앱 모듈이 띄웁니다.
 * @param onFriendAdded 스코어보드에서 친구 요청을 수락하면 부릅니다. 앱 모듈이 이때 알림을 켜 달라고 묻습니다.
 */
@Composable
fun MatchDetailRoute(
    matchId: MatchId,
    onBack: () -> Unit,
    onOpenFriend: (PlayerId) -> Unit,
    onOpenMe: () -> Unit,
    onShareInvite: () -> Unit,
    modifier: Modifier = Modifier,
    onFriendAdded: () -> Unit = {},
    viewModel: MatchDetailViewModel = koinViewModel(key = matchId.value) { parametersOf(matchId.value) },
) {
    FailureNoticesEffect(viewModel.notices)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState == MatchDetailUiState.Gone) onBack()
    }
    MatchDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenFriend = onOpenFriend,
        onOpenMe = onOpenMe,
        onSendRequest = viewModel::sendRequest,
        onAccept = { viewModel.accept(it, onAccepted = onFriendAdded) },
        onShareInvite = onShareInvite,
        modifier = modifier,
    )
}

@Composable
internal fun MatchDetailScreen(
    uiState: MatchDetailUiState,
    onBack: () -> Unit,
    onOpenFriend: (PlayerId) -> Unit,
    onSendRequest: (PlayerId) -> Unit,
    onAccept: (PlayerId) -> Unit,
    onShareInvite: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMe: () -> Unit = {},
) {
    val colors = OvalitTheme.colors
    var tab by rememberSaveable { mutableStateOf(DetailTab.SCOREBOARD) }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }

    // 밀려 들어오는 중에 내용이 도착하면 다 들어올 때까지 스켈레톤을 두었다가 서서히 바꾼다.
    val loaded = uiState is MatchDetailUiState.Success
    val shown = rememberContentShown(loaded = loaded)
    OvalitStaged(
        ready = loaded && shown,
        placeholder = { MatchDetailSkeleton(bannerHeight = BannerHeight, onBack = onBack) },
        modifier = modifier.fillMaxSize().background(colors.bg),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState !is MatchDetailUiState.Success) return@Box
            val match = uiState.match
            // 라운드가 없는 모드는 라운드 기록이 어떻게 오는지 몰라 내 기록 탭도 두지 않는다
            val tabs = buildList {
                add(DetailTab.SCOREBOARD)
                if (match.format == MatchFormat.ROUNDS) {
                    if (match.queue.halfRounds != null) add(DetailTab.ROUNDS)
                    add(DetailTab.REPORT)
                }
            }

            val scrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Banner(uiState, onBack)
                if (match.queue.halfRounds != null) {
                    RoundStrip(uiState)
                }
                Spacer(Modifier.height(22.dp))
                if (tabs.size > 1) {
                    Tabs(tabs, selected = tab, onSelect = { tab = it })
                }
                Crossfade(targetState = tab.takeIf { it in tabs } ?: DetailTab.SCOREBOARD, animationSpec = tween(180)) { shown ->
                    when (shown) {
                        DetailTab.SCOREBOARD -> Scoreboard(
                            uiState = uiState,
                            onOpenPlayer = { row ->
                                when (row.relation) {
                                    PlayerRelation.ME -> onOpenMe()
                                    PlayerRelation.FRIEND -> onOpenFriend(row.line.player)
                                    else -> sheetFor = row.line.player.value
                                }
                            },
                        )
                        DetailTab.ROUNDS -> RoundList(uiState)
                        DetailTab.REPORT -> MatchReport(uiState)
                    }
                }
                AdSlot(AdPlacement.MATCH_DETAIL) { ad ->
                    Column {
                        Spacer(Modifier.height(OvalitSpacing.lg))
                        OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = colors.lineWeak)
                        ad()
                    }
                }
                Spacer(Modifier.height(OvalitSpacing.xxl))
                // 탭바가 없는 화면이라 마지막 줄이 내비게이션 바에 덮이지 않게 그 높이만큼 띄운다.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            StatusBarScrim(scrollState)
        }
    }

    val success = uiState as? MatchDetailUiState.Success
    val selected = success?.let { state -> state.rows.firstOrNull { it.line.player.value == sheetFor } }
    if (selected != null) {
        PlayerSheet(
            row = selected,
            onSendRequest = { onSendRequest(selected.line.player) },
            onAccept = { onAccept(selected.line.player) },
            onShareInvite = onShareInvite,
            onDismiss = { sheetFor = null },
        )
    }
}

/**
 * 내리면 상태 표시줄 자리를 바탕색으로 덮습니다.
 * 배너가 화면 맨 위까지 깔려 있어서 안 덮으면 내린 글자가 시계와 겹칩니다.
 * 상태 표시줄 높이만큼 내리는 동안 서서히 덮어서 맨 위에서는 맵 그림이 그대로 보입니다.
 */
@Composable
private fun BoxScope.StatusBarScrim(scrollState: ScrollState) {
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density).coerceAtLeast(1)
    val cover by remember(scrollState, statusBar) { derivedStateOf { (scrollState.value.toFloat() / statusBar).coerceIn(0f, 1f) } }
    if (cover > 0f) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = cover }
                .background(OvalitTheme.colors.bg),
        )
    }
}

@Composable
private fun Banner(uiState: MatchDetailUiState.Success, onBack: () -> Unit) {
    val colors = OvalitTheme.colors
    val match = uiState.match
    val start = match.startedAt.toLocalDateTime(uiState.timeZone)
    val date = stringResource(
        Res.string.detail_date,
        start.month.number,
        start.day,
        start.hour.toString().padStart(2, '0'),
        start.minute.toString().padStart(2, '0'),
    )
    val caption = stringResource(Res.string.detail_caption, stringResource(match.queue.label), date, (match.lengthMillis / 60_000).toInt())

    Box(modifier = Modifier.fillMaxWidth().height(BannerHeight)) {
        MapImage(match.map, MapImageStyle.BANNER, Modifier.matchParentSize())
        // 맵 그림 위에 글자를 얹으니 위아래로 흐림막을 깐다. 아래는 목업처럼 바탕색으로 이어진다.
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(Brush.verticalGradient(listOf(colors.bg.copy(alpha = 0.9f), colors.bg.copy(alpha = 0.6f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bg))),
        )
        OvalitBackTopBar(onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            OvalitTopBarCaption(caption, color = colors.t1)
        }
        ScoreHeadline(uiState, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun ScoreHeadline(uiState: MatchDetailUiState.Success, modifier: Modifier) {
    val match = uiState.match
    val standing = remember(match) { match.myStanding }
    if (standing != null) {
        StandingHeadline(uiState, standing, modifier)
        return
    }
    val colors = OvalitTheme.colors
    val score = match.score
    val big = OvalitTheme.typography.metricL.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold)
    val halves = match.halfScores.mapIndexed { index, half ->
        val label = when (index) {
            0 -> Res.string.half_first
            1 -> Res.string.half_second
            else -> Res.string.half_overtime
        }
        stringResource(label, half.myTeam, half.enemyTeam)
    }.joinToString(SEPARATOR)
    val scoreDescription = stringResource(Res.string.score_description, score.myTeam, score.enemyTeam)

    Column(modifier = modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = 14.dp)) {
        // 맵 이름 옆에는 승패만 적는다. MVP와 순위는 바로 밑 스코어보드에 있다.
        Row(verticalAlignment = Alignment.Bottom) {
            OvalitText(
                text = uiState.catalog.mapName(match.map),
                modifier = Modifier.weight(1f, fill = false),
                style = OvalitTheme.typography.display,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(OvalitSpacing.sm))
            ResultLabel(match.myTeamWon, Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = scoreDescription },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                // 목업처럼 내 팀 점수만 결과 색이다
                OvalitText(text = score.myTeam.toString(), style = big, color = resultColor(match.myTeamWon))
                OvalitText(text = "–", style = OvalitTheme.typography.titleM, color = colors.t4, modifier = Modifier.padding(bottom = 6.dp))
                OvalitText(text = score.enemyTeam.toString(), style = big, color = colors.t4)
            }
            Spacer(Modifier.weight(1f))
            OvalitText(
                text = halves,
                modifier = Modifier.padding(start = OvalitSpacing.sm, bottom = 4.dp),
                style = OvalitTheme.typography.caption,
                color = colors.t2,
                maxLines = 1,
            )
        }
    }
}

// 데스매치와 건틀릿은 승패 대신 등수로 끝나서 맵 이름 옆 승패를 빼고 스코어 자리에 "14명 중 3등"을 크게 둔다
@Composable
private fun StandingHeadline(uiState: MatchDetailUiState.Success, standing: Standing, modifier: Modifier) {
    val match = uiState.match
    val big = OvalitTheme.typography.metricL.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold)
    val description = standingText(standing, match.format)
    val of = stringResource(
        if (match.format == MatchFormat.FREE_FOR_ALL) Res.string.standing_of_players else Res.string.standing_of_teams,
        standing.teams,
    )

    Column(modifier = modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = 14.dp)) {
        OvalitText(
            text = uiState.catalog.mapName(match.map),
            style = OvalitTheme.typography.display,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            OvalitText(text = of, style = OvalitTheme.typography.titleM, color = OvalitTheme.colors.t4, modifier = Modifier.padding(bottom = 6.dp))
            OvalitText(text = rankText(standing.rank), style = big, color = standingColor(standing.rank))
        }
    }
}

@Composable
private fun RoundStrip(uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    val match = uiState.match
    val half = match.queue.halfRounds ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 14.dp)
            .height(18.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        match.roundOutcomes.forEachIndexed { index, won ->
            if (index == half || index == half * 2) Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(if (won) 18.dp else 11.dp)
                    .background(if (won) resultColor(true) else colors.bar),
            )
        }
    }
}

@Composable
private fun Tabs(tabs: List<DetailTab>, selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    val colors = OvalitTheme.colors
    Column {
        Row(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(min = TouchSize)
                        // 탭 폭이 글자 폭이라 누른 면을 탭 사이 간격의 절반만큼 양옆으로 넓힌다
                        .selectable(
                            selected = isSelected,
                            interactionSource = null,
                            indication = pressIndication(horizontalOutset = OvalitPressOutset),
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        ),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    OvalitText(
                        text = stringResource(
                            when (tab) {
                                DetailTab.SCOREBOARD -> Res.string.tab_scoreboard
                                DetailTab.ROUNDS -> Res.string.tab_rounds
                                DetailTab.REPORT -> Res.string.tab_report
                            },
                        ),
                        modifier = Modifier.padding(bottom = 11.dp),
                        style = OvalitTheme.typography.label.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (isSelected) colors.t1 else colors.t3,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) colors.t1 else Color.Transparent),
                    )
                }
            }
        }
        OvalitDivider(color = colors.lineWeak)
    }
}
