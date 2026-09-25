package com.ovalit.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDisclaimer
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MIN_MATCHES_PER_REPORT
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeeklyReport
import com.ovalit.feature.report.component.DynamicMetricList
import com.ovalit.feature.report.component.FixedMetricGrid
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.dynamic_title_moved
import com.ovalit.feature.report.resources.dynamic_title_steady
import com.ovalit.feature.report.resources.dynamic_title_unknown
import com.ovalit.feature.report.resources.dynamic_unknown_hint
import com.ovalit.feature.report.resources.not_enough_body
import com.ovalit.feature.report.resources.not_enough_title
import com.ovalit.feature.report.resources.report_headline
import com.ovalit.feature.report.resources.report_main_role
import com.ovalit.feature.report.resources.report_since
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val LogoWidth = 44.dp

/** S1 홈입니다. 주간 리포트를 보여줍니다. */
@Composable
fun ReportRoute(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReportScreen(uiState = uiState, modifier = modifier)
}

@Composable
internal fun ReportScreen(
    uiState: ReportUiState,
    modifier: Modifier = Modifier,
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
            is ReportUiState.Success -> when (val report = uiState.report) {
                is WeeklyReport.Ready -> ReportContent(report)
                is WeeklyReport.NotEnoughMatches -> NotEnoughMatches(played = report.played)
            }
        }
    }
}

@Composable
private fun ReportContent(report: WeeklyReport.Ready) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        // 내용이 화면보다 짧아도 고지는 화면 맨 아래에 붙인다. 스크롤 안에서는 weight가 남은
        // 높이를 모르니 최소 높이를 화면 높이로 잡아 준다.
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = OvalitSpacing.lg),
        ) {
            ReportSections(report)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(OvalitSpacing.xxl))
            OvalitDisclaimer(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(OvalitSpacing.md))
        }
    }
}

@Composable
private fun ReportSections(report: WeeklyReport.Ready) {
    Column {
        Spacer(Modifier.height(OvalitSpacing.md))
        OvalitLogo(modifier = Modifier.width(LogoWidth))
        Spacer(Modifier.height(OvalitSpacing.xl))

        ReportHeader(report)
        Spacer(Modifier.height(OvalitSpacing.xl))

        FixedMetricGrid(metrics = report.metrics, baseline = report.baseline)
        Spacer(Modifier.height(OvalitSpacing.xxl))

        DynamicSectionTitle(report)
        Spacer(Modifier.height(OvalitSpacing.md))
        DynamicMetricList(slots = report.dynamic, metrics = report.metrics, baseline = report.baseline)
    }
}

@Composable
private fun ReportHeader(report: WeeklyReport.Ready) {
    val colors = OvalitTheme.colors
    val firstDay = report.period.firstDay

    OvalitText(
        text = stringResource(Res.string.report_since, firstDay.month.number, firstDay.day),
        style = OvalitTheme.typography.label,
        color = colors.t3,
    )
    Spacer(Modifier.height(OvalitSpacing.xs))
    OvalitText(
        text = stringResource(Res.string.report_headline, periodLabel(report.period), report.metrics.matches),
        style = OvalitTheme.typography.titleL,
    )
    report.mainRole?.let { role ->
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = stringResource(Res.string.report_main_role, stringResource(role.label)),
            style = OvalitTheme.typography.body,
            color = colors.t2,
        )
    }
}

@Composable
private fun DynamicSectionTitle(report: WeeklyReport.Ready) {
    val movements = report.dynamic.map { it.movement }

    // 판단을 보류한 칸만 있을 때 "큰 변화 없음"이라고 하면 안 된다. 변화가 없는 게 아니라 모르는 것이다.
    val title = when {
        Movement.MOVED in movements -> stringResource(Res.string.dynamic_title_moved)
        Movement.STEADY in movements -> stringResource(Res.string.dynamic_title_steady, periodLabel(report.period))
        else -> stringResource(Res.string.dynamic_title_unknown)
    }

    OvalitText(text = title, style = OvalitTheme.typography.titleM)
    if (movements.all { it == Movement.UNKNOWN }) {
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = stringResource(Res.string.dynamic_unknown_hint),
            style = OvalitTheme.typography.body,
            color = OvalitTheme.colors.t2,
        )
    }
}

@Composable
private fun NotEnoughMatches(played: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = OvalitSpacing.lg),
    ) {
        Spacer(Modifier.height(OvalitSpacing.md))
        OvalitLogo(modifier = Modifier.width(LogoWidth))

        Spacer(Modifier.weight(1f))

        OvalitText(
            text = stringResource(Res.string.not_enough_title, MIN_MATCHES_PER_REPORT - played),
            style = OvalitTheme.typography.titleL,
        )
        Spacer(Modifier.height(OvalitSpacing.md))
        OvalitText(
            text = stringResource(Res.string.not_enough_body, MIN_MATCHES_PER_REPORT),
            style = OvalitTheme.typography.body,
            color = OvalitTheme.colors.t2,
        )

        Spacer(Modifier.weight(1f))

        OvalitDisclaimer(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(OvalitSpacing.md))
    }
}
