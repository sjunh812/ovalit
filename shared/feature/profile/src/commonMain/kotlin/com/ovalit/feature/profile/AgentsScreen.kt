package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Role
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.act_matches
import com.ovalit.feature.profile.resources.agents_by_agent
import com.ovalit.feature.profile.resources.agents_columns_note
import com.ovalit.feature.profile.resources.agents_focus_controller
import com.ovalit.feature.profile.resources.agents_focus_duelist
import com.ovalit.feature.profile.resources.agents_focus_initiator
import com.ovalit.feature.profile.resources.agents_focus_sentinel
import com.ovalit.feature.profile.resources.agents_main_role
import com.ovalit.feature.profile.resources.agents_matches
import com.ovalit.feature.profile.resources.agents_role_controller
import com.ovalit.feature.profile.resources.agents_role_duelist
import com.ovalit.feature.profile.resources.agents_role_initiator
import com.ovalit.feature.profile.resources.agents_role_sentinel
import com.ovalit.feature.profile.resources.agents_row_caption
import com.ovalit.feature.profile.resources.agents_title
import com.ovalit.feature.profile.resources.column_first_duel_involvement
import com.ovalit.feature.profile.resources.column_first_duel_win
import com.ovalit.feature.profile.resources.column_kast
import com.ovalit.feature.profile.resources.column_survival
import com.ovalit.feature.profile.resources.column_win_rate
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.not_enough_sample
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val WinColumn = 48.dp
private val MetricColumn = 58.dp
// 글자를 키우면 옆 열 제목끼리 붙어서 왼쪽에 간격을 둔다
private val ColumnGap = 6.dp
private val ThumbnailSize = 34.dp

/** S7 요원 화면입니다. */
@Composable
fun AgentsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AgentsScreen(uiState, onBack, modifier)
}

@Composable
internal fun AgentsScreen(uiState: ProfileUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(OvalitTheme.colors.bg)) {
        if (uiState !is ProfileUiState.Success) return@Box
        val report = uiState.agents

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            SubScreenTopBar(
                title = stringResource(Res.string.agents_title),
                caption = stringResource(Res.string.act_matches, report.matches),
                onBack = onBack,
            )
            val mainRole = report.mainRole
            if (mainRole == null) {
                OvalitText(
                    text = stringResource(Res.string.no_matches),
                    modifier = Modifier.padding(OvalitSpacing.gutter),
                    color = OvalitTheme.colors.t2,
                )
                return@Column
            }

            Spacer(Modifier.height(22.dp))
            MainRole(report, mainRole, uiState.catalog)
            Spacer(Modifier.height(22.dp))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            RoleShares(report, mainRole)
            Spacer(Modifier.height(22.dp))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            AgentTable(report.agents, mainRole, uiState.catalog)
            Spacer(Modifier.height(OvalitSpacing.lg))
            OvalitText(
                text = stringResource(Res.string.agents_columns_note),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
    }
}

@Composable
private fun MainRole(report: AgentReport, role: Role, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val share = report.roles.first().rounds.toDouble() / report.roles.sumOf { it.rounds }
    val topAgents = report.agents.filter { it.role == role }.take(2).map { catalog.agentName(it.agent) }
    val focus = stringResource(role.focus)
    val sentence = stringResource(role.sentence, focus)

    Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
        OvalitText(text = stringResource(Res.string.agents_main_role), style = OvalitTheme.typography.label, color = colors.t3)
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            OvalitText(text = stringResource(role.label), modifier = Modifier.alignByBaseline(), style = OvalitTheme.typography.titleL)
            Spacer(Modifier.width(9.dp))
            OvalitText(
                text = percentText(share),
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.metricS,
                color = colors.accentInk,
            )
            Spacer(Modifier.weight(1f))
            OvalitText(
                text = topAgents.joinToString(" · "),
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
        Spacer(Modifier.height(11.dp))
        // 이 역할을 무엇으로 보는지만 굵게 칠한다
        OvalitText(
            text = buildAnnotatedString {
                append(sentence)
                val start = sentence.indexOf(focus)
                if (start >= 0) addStyle(SpanStyle(color = colors.t1, fontWeight = FontWeight.SemiBold), start, start + focus.length)
            },
            style = OvalitTheme.typography.caption,
            color = colors.t2,
        )
    }
}

@Composable
private fun RoleShares(report: AgentReport, mainRole: Role) {
    val totalRounds = report.roles.sumOf { it.rounds }.coerceAtLeast(1)

    Column(
        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
    ) {
        report.roles.forEach { share ->
            val isMain = share.role == mainRole
            Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                val labelStyle = if (isMain) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body
                OvalitText(
                    text = stringResource(share.role.label),
                    modifier = Modifier.width(52.dp),
                    style = labelStyle,
                    color = if (isMain) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
                    maxLines = 1,
                    autoSize = shrinkToFit(labelStyle.fontSize),
                )
                Spacer(Modifier.width(OvalitSpacing.sm))
                ShareBar(
                    fraction = share.rounds.toFloat() / totalRounds,
                    highlighted = isMain,
                    modifier = Modifier.weight(1f),
                )
                OvalitText(
                    text = stringResource(Res.string.agents_matches, share.matches),
                    modifier = Modifier.width(48.dp),
                    style = OvalitTheme.typography.metricS,
                    color = if (isMain) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** 요원별 표의 오른쪽 두 열입니다. 타격대가 주 역할이면 퍼블 쪽 지표로 바뀝니다. */
private class MetricColumnSpec(val title: StringResource, val value: (MatchMetrics) -> Double?)

private fun columnsFor(role: Role): List<MetricColumnSpec> = when (role) {
    Role.DUELIST -> listOf(
        MetricColumnSpec(Res.string.column_first_duel_involvement) { it.firstDuelInvolvement },
        MetricColumnSpec(Res.string.column_first_duel_win) { it.firstDuelWinRate },
    )
    else -> listOf(
        MetricColumnSpec(Res.string.column_kast) { it.kast },
        MetricColumnSpec(Res.string.column_survival) { it.survivalRate },
    )
}

@Composable
private fun AgentTable(agents: List<AgentStats>, mainRole: Role, catalog: ContentCatalog) {
    val columns = columnsFor(mainRole)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter),
        verticalAlignment = Alignment.Bottom,
    ) {
        OvalitText(
            text = stringResource(Res.string.agents_by_agent),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        HeaderCell(stringResource(Res.string.column_win_rate), WinColumn)
        columns.forEach { HeaderCell(stringResource(it.title), MetricColumn) }
    }
    Spacer(Modifier.height(OvalitSpacing.sm))
    agents.forEachIndexed { index, agent ->
        if (index > 0) {
            OvalitDivider(Modifier.padding(start = OvalitSpacing.gutter + ThumbnailSize + OvalitSpacing.md), color = OvalitTheme.colors.lineWeak)
        }
        AgentRow(agent, columns, catalog)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    OvalitText(
        text = text,
        modifier = Modifier.width(width).padding(start = ColumnGap),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
        textAlign = TextAlign.End,
        maxLines = 1,
        autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize),
    )
}

@Composable
private fun AgentRow(agent: AgentStats, columns: List<MetricColumnSpec>, catalog: ContentCatalog) {
    val name = catalog.agentName(agent.agent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = OvalitSpacing.gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(name = name, width = ThumbnailSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            OvalitText(text = name, style = OvalitTheme.typography.bodyStrong, maxLines = 1)
            OvalitText(
                text = stringResource(
                    Res.string.agents_row_caption,
                    agent.role?.let { stringResource(it.label) } ?: NO_VALUE,
                    agent.matches,
                ),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        }
        if (!agent.isMeasurable) {
            OvalitText(
                text = stringResource(Res.string.not_enough_sample),
                modifier = Modifier.width(WinColumn + MetricColumn * columns.size),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
            return@Row
        }
        val winStyle = OvalitTheme.typography.metricS.copy(fontWeight = FontWeight.Bold)
        OvalitText(
            text = percentText(agent.winRate),
            modifier = Modifier.width(WinColumn).padding(start = ColumnGap),
            style = winStyle,
            color = winRateColor(agent.winRate),
            textAlign = TextAlign.End,
            maxLines = 1,
            autoSize = shrinkToFit(winStyle.fontSize),
        )
        columns.forEachIndexed { index, column ->
            OvalitText(
                text = percentText(column.value(agent.metrics)),
                modifier = Modifier.width(MetricColumn).padding(start = ColumnGap),
                style = OvalitTheme.typography.metricS,
                color = if (index == 0) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
                textAlign = TextAlign.End,
                maxLines = 1,
                autoSize = shrinkToFit(OvalitTheme.typography.metricS.fontSize),
            )
        }
    }
}

// 목업대로 50%를 넘으면 초록, 밑돌면 빨강이다. 색은 변화량에만 쓴다는 규칙의 예외로 CLAUDE.md에 적었다.
@Composable
private fun winRateColor(rate: Double?): Color {
    val steps = rate?.percentSteps() ?: return OvalitTheme.colors.t3
    return when {
        steps > 50 -> OvalitTheme.colors.pos
        steps < 50 -> OvalitTheme.colors.neg
        else -> OvalitTheme.colors.t2
    }
}

private val Role.sentence: StringResource
    get() = when (this) {
        Role.DUELIST -> Res.string.agents_role_duelist
        Role.INITIATOR -> Res.string.agents_role_initiator
        Role.CONTROLLER -> Res.string.agents_role_controller
        Role.SENTINEL -> Res.string.agents_role_sentinel
    }

private val Role.focus: StringResource
    get() = when (this) {
        Role.DUELIST -> Res.string.agents_focus_duelist
        Role.INITIATOR -> Res.string.agents_focus_initiator
        Role.CONTROLLER -> Res.string.agents_focus_controller
        Role.SENTINEL -> Res.string.agents_focus_sentinel
    }
