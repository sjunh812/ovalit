package com.ovalit.feature.match

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.halfScores
import com.ovalit.core.ui.MapImage
import com.ovalit.core.ui.MapImageStyle
import com.ovalit.core.ui.label
import com.ovalit.core.ui.mapName
import com.ovalit.core.ui.resultColor
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.back
import com.ovalit.feature.match.resources.detail_caption
import com.ovalit.feature.match.resources.detail_date
import com.ovalit.feature.match.resources.half_first
import com.ovalit.feature.match.resources.half_overtime
import com.ovalit.feature.match.resources.half_second
import com.ovalit.feature.match.resources.score_description
import com.ovalit.feature.match.resources.tab_economy
import com.ovalit.feature.match.resources.tab_rounds
import com.ovalit.feature.match.resources.tab_scoreboard
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val BannerHeight = 196.dp
private val TouchSize = 44.dp

internal enum class DetailTab { SCOREBOARD, ROUNDS, ECONOMY }

/**
 * S3 경기 상세입니다.
 *
 * @param onShareInvite 앱을 안 쓰는 플레이어에게 보낼 초대 링크를 받습니다. 공유 시트는 앱 모듈이 띄웁니다.
 */
@Composable
fun MatchDetailRoute(
    matchId: MatchId,
    onBack: () -> Unit,
    onOpenFriend: (PlayerId) -> Unit,
    onShareInvite: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchDetailViewModel = koinViewModel(key = matchId.value) { parametersOf(matchId.value) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState == MatchDetailUiState.Gone) onBack()
    }
    MatchDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenFriend = onOpenFriend,
        onSendRequest = viewModel::sendRequest,
        onAccept = viewModel::accept,
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
) {
    val colors = OvalitTheme.colors
    var tab by rememberSaveable { mutableStateOf(DetailTab.SCOREBOARD) }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is MatchDetailUiState.Success) return@Box
        val match = uiState.match
        val tabs = buildList {
            add(DetailTab.SCOREBOARD)
            if (match.queue.halfRounds != null) add(DetailTab.ROUNDS)
            if (match.queue.hasEconomy) add(DetailTab.ECONOMY)
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                            if (row.relation == PlayerRelation.FRIEND) onOpenFriend(row.line.player) else sheetFor = row.line.player.value
                        },
                    )
                    DetailTab.ROUNDS -> RoundList(uiState)
                    DetailTab.ECONOMY -> EconomyList(uiState)
                }
            }
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
    }

    val success = uiState as? MatchDetailUiState.Success
    val selected = success?.let { state -> (state.myTeam + state.enemyTeam).firstOrNull { it.line.player.value == sheetFor } }
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
        Row(
            modifier = Modifier
                .safeDrawingPadding()
                .fillMaxWidth()
                .padding(start = OvalitSpacing.sm, end = OvalitSpacing.gutter, top = OvalitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(TouchSize).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                OvalitIcon(OvalitIcons.Back, contentDescription = stringResource(Res.string.back), tint = colors.t1)
            }
            Spacer(Modifier.weight(1f))
            OvalitText(text = caption, style = OvalitTheme.typography.caption, color = colors.t1, maxLines = 1)
        }
        ScoreHeadline(uiState, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun ScoreHeadline(uiState: MatchDetailUiState.Success, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val match = uiState.match
    val score = match.score
    val big = OvalitTheme.typography.metricL.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold)
    val halves = match.halfScores.mapIndexed { index, half ->
        val label = when (index) {
            0 -> Res.string.half_first
            1 -> Res.string.half_second
            else -> Res.string.half_overtime
        }
        stringResource(label, half.myTeam, half.enemyTeam)
    }.joinToString(" · ")
    val scoreDescription = stringResource(Res.string.score_description, score.myTeam, score.enemyTeam)

    Column(modifier = modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = 14.dp)) {
        OvalitText(text = uiState.catalog.mapName(match.map), style = OvalitTheme.typography.titleL)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = scoreDescription },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                // 목업처럼 내 팀 점수만 결과 색으로 칠하고 상대 점수는 흐리게 둔다
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

// 이긴 라운드는 높고 색이 있고, 진 라운드는 낮고 흐리다. 전반과 후반, 연장 사이는 틈을 넓힌다.
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
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    OvalitText(
                        text = stringResource(
                            when (tab) {
                                DetailTab.SCOREBOARD -> Res.string.tab_scoreboard
                                DetailTab.ROUNDS -> Res.string.tab_rounds
                                DetailTab.ECONOMY -> Res.string.tab_economy
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
