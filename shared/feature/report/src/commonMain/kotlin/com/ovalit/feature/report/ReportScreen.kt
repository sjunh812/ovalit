package com.ovalit.feature.report

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MIN_MATCHES_PER_REPORT
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.feature.report.component.DynamicMetricSection
import com.ovalit.feature.report.component.FixedMetricRow
import com.ovalit.feature.report.component.FixedMetricSummary
import com.ovalit.feature.report.component.FriendRankingSection
import com.ovalit.feature.report.component.HorizontalLine
import com.ovalit.feature.report.component.InsightSection
import com.ovalit.feature.report.component.MetricSheet
import com.ovalit.feature.report.component.PeriodHeader
import com.ovalit.feature.report.component.QueueChips
import com.ovalit.feature.report.component.ReportTopBar
import com.ovalit.feature.report.component.RivalSection
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.not_enough_body
import com.ovalit.feature.report.resources.not_enough_title
import com.ovalit.feature.report.resources.not_enough_title_none
import com.ovalit.feature.report.resources.other_queue_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** S1 홈입니다. 주간 리포트를 보여줍니다. */
@Composable
fun ReportRoute(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val badge by viewModel.badge.collectAsStateWithLifecycle()

    ReportScreen(
        uiState = uiState,
        onSelectQueue = viewModel::selectQueue,
        badge = badge,
        onOpenProfile = onOpenProfile,
        modifier = modifier,
    )
}

@Composable
internal fun ReportScreen(
    uiState: ReportUiState,
    onSelectQueue: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
    badge: PlayerBadge? = null,
    onOpenProfile: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.bg),
    ) {
        when (uiState) {
            // 수집이 끝나기 전에는 숫자를 띄우지 않는다. 헤드샷 24%가 잠시 뒤 19%로 바뀌면
            // 유저는 그 뒤로 숫자를 믿지 않는다.
            ReportUiState.Loading -> Unit
            is ReportUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(OvalitSpacing.gutter))
                ReportTopBar(badge = badge, onOpenProfile = onOpenProfile)
                Spacer(Modifier.height(OvalitSpacing.md))
                QueueChips(selected = uiState.queueFilter, onSelect = onSelectQueue)
                Spacer(Modifier.height(OvalitSpacing.md))

                when (val report = uiState.report) {
                    is WeeklyReport.Ready -> ReportContent(report, uiState.queueFilter, uiState.rival, uiState.friends)
                    is WeeklyReport.NotEnoughMatches -> NotEnoughMatches(played = report.played)
                }

                Spacer(Modifier.height(OvalitSpacing.xxl))
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
) {
    var openMetric by rememberSaveable { mutableStateOf<FixedMetric?>(null) }

    PeriodHeader(report)
    Spacer(Modifier.height(OvalitSpacing.lg))
    FixedMetricRow(
        metrics = report.metrics,
        baseline = report.baseline,
        fixedMetrics = queueFilter.fixedMetrics,
        onOpenMetric = { openMetric = it },
    )
    Spacer(Modifier.height(13.dp))
    FixedMetricSummary(metrics = report.metrics, baseline = report.baseline, fixedMetrics = queueFilter.fixedMetrics)
    Spacer(Modifier.height(22.dp))

    if (queueFilter.hasDynamicMetrics) {
        DynamicMetricSection(report)
        report.insight?.let { insight ->
            Spacer(Modifier.height(20.dp))
            HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            InsightSection(insight = insight, role = report.mainRole)
        }
        rival?.let {
            Spacer(Modifier.height(20.dp))
            HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            RivalSection(report = report, mine = report.metrics, rival = it)
        }
        if (friends.any { it.metrics != null }) {
            Spacer(Modifier.height(20.dp))
            HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            FriendRankingSection(mine = report.metrics, friends = friends)
        }
    } else {
        HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
        Spacer(Modifier.height(18.dp))
        OvalitText(
            text = stringResource(Res.string.other_queue_hint),
            modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }

    openMetric?.let { metric ->
        MetricSheet(metric = metric, report = report, onDismiss = { openMetric = null })
    }
}

@Composable
private fun NotEnoughMatches(played: Int) {
    Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
        Spacer(Modifier.height(OvalitSpacing.xl))
        OvalitText(
            text = if (played == 0) {
                stringResource(Res.string.not_enough_title_none)
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
