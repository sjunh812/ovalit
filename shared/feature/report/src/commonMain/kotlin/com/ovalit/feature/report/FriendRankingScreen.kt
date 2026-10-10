package com.ovalit.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.loading
import com.ovalit.feature.report.component.BarRowGap
import com.ovalit.feature.report.component.RankMetricSheet
import com.ovalit.feature.report.component.RankRow
import com.ovalit.feature.report.component.RankingTitle
import com.ovalit.feature.report.component.SectionTitleGap
import com.ovalit.feature.report.component.rememberRankWidths
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.friends_me
import com.ovalit.feature.report.resources.friends_ranking_caption
import com.ovalit.feature.report.resources.friends_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 홈 친구 비교의 "전체 보기"로 여는 전체 순위입니다. 홈 카드와 같은 막대 줄을 줄 세운 사람 수만큼 늘어놓습니다.
 *
 * @param metric 홈 카드에서 고른 지표입니다. 여기서 바꿔도 홈 카드는 그대로입니다.
 */
@Composable
fun FriendRankingRoute(
    queueFilter: QueueFilter,
    metric: FixedMetric,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendRankingViewModel = koinViewModel(key = queueFilter.name) { parametersOf(queueFilter.name) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState == FriendRankingUiState.Gone) onBack()
    }
    FriendRankingScreen(uiState, initialMetric = metric, onBack = onBack, modifier = modifier)
}

@Composable
internal fun FriendRankingScreen(
    uiState: FriendRankingUiState,
    initialMetric: FixedMetric,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    var metricName by rememberSaveable { mutableStateOf(initialMetric.name) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val metric = FixedMetric.valueOf(metricName)
    val title = stringResource(Res.string.friends_title)

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        // 밀려 들어오는 중에 도착하면 다 들어올 때까지 줄 자리만 잡아 둔다
        val shown = rememberContentShown(loaded = uiState is FriendRankingUiState.Success)
        if (uiState !is FriendRankingUiState.Success || !shown) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                OvalitBackTopBar(onBack = onBack, title = title)
                Spacer(Modifier.height(OvalitSpacing.lg))
                RankingSkeleton()
            }
            return@Box
        }
        val me = stringResource(Res.string.friends_me)
        val ranked = remember(uiState, metric, me) { rankFriends(me, uiState.mine, uiState.friends, metric) }
        val widths = rememberRankWidths(ranked, metric)
        val top = ranked.firstOrNull()?.value?.takeIf { it > 0 } ?: 1.0
        val caption = stringResource(Res.string.friends_ranking_caption, periodLabel(uiState.period), ranked.size)

        LazyColumn(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            item(key = "header") {
                Column {
                    OvalitBackTopBar(onBack = onBack, title = title)
                    Spacer(Modifier.height(OvalitSpacing.sm))
                    RankingTitle(
                        title = { OvalitText(text = caption, style = OvalitTheme.typography.label, color = colors.t2) },
                        metric = metric,
                        onChoose = { choosing = true },
                        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                    )
                    Spacer(Modifier.height(SectionTitleGap))
                }
            }
            items(ranked, key = { it.id?.value ?: "me" }) { entry ->
                RankRow(
                    entry = entry,
                    metric = metric,
                    top = top,
                    widths = widths,
                    modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = BarRowGap),
                )
            }
            item { Spacer(Modifier.height(OvalitSpacing.xxl)) }
        }
    }

    if (choosing) {
        RankMetricSheet(
            selected = metric,
            onPick = {
                metricName = it.name
                choosing = false
            },
            onDismiss = { choosing = false },
        )
    }
}

@Composable
private fun RankingSkeleton() {
    OvalitSkeleton(stringResource(CoreUiRes.string.loading), Modifier.padding(horizontal = OvalitSpacing.gutter)) {
        Column(verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
            repeat(SKELETON_ROWS) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(width = 12.dp, height = 12.dp)
                    Spacer(Modifier.width(OvalitSpacing.sm))
                    SkeletonBlock(width = 56.dp, height = 12.dp)
                    Spacer(Modifier.width(OvalitSpacing.sm))
                    SkeletonBlock(width = 160.dp, height = 3.dp, radius = 0.dp)
                }
            }
        }
    }
}

private const val SKELETON_ROWS = 10
