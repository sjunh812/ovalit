package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPickerButton
import com.ovalit.core.designsystem.component.OvalitSheetOption
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTopBarCaption
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Role
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.RoleIcon
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaRatioText
import com.ovalit.core.ui.label
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.rememberFitsOnOneLine
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.act_matches
import com.ovalit.core.ui.resources.agents_matches
import com.ovalit.core.ui.resources.column_win_rate
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.winRateColor
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.agents_by_agent
import com.ovalit.feature.profile.resources.agents_columns_button
import com.ovalit.feature.profile.resources.agents_columns_fight
import com.ovalit.feature.profile.resources.agents_columns_first_duel
import com.ovalit.feature.profile.resources.agents_columns_kast
import com.ovalit.feature.profile.resources.agents_columns_note
import com.ovalit.feature.profile.resources.agents_columns_role_default
import com.ovalit.feature.profile.resources.agents_columns_score
import com.ovalit.feature.profile.resources.agents_columns_sheet_title
import com.ovalit.feature.profile.resources.agents_focus_controller
import com.ovalit.feature.profile.resources.agents_focus_duelist
import com.ovalit.feature.profile.resources.agents_focus_initiator
import com.ovalit.feature.profile.resources.agents_focus_sentinel
import com.ovalit.feature.profile.resources.agents_main_role
import com.ovalit.feature.profile.resources.agents_role_controller
import com.ovalit.feature.profile.resources.agents_role_duelist
import com.ovalit.feature.profile.resources.agents_role_initiator
import com.ovalit.feature.profile.resources.agents_role_sentinel
import com.ovalit.feature.profile.resources.agents_title
import com.ovalit.feature.profile.resources.column_first_duel_involvement
import com.ovalit.feature.profile.resources.column_first_duel_win
import com.ovalit.feature.profile.resources.column_kast
import com.ovalit.feature.profile.resources.column_survival
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
            OvalitBackTopBar(onBack = onBack, title = stringResource(Res.string.agents_title)) {
                OvalitTopBarCaption(stringResource(CoreUiRes.string.act_matches, report.matches))
            }
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
            // 처음에는 주 역할에 맞춘 묶음이다. 고른 건 화면을 떠나기 전까지 기억한다.
            var chosen by rememberSaveable { mutableStateOf<String?>(null) }
            var choosing by remember { mutableStateOf(false) }
            val roleDefault = AgentColumns.defaultFor(mainRole)
            val shown = chosen?.let(AgentColumns::valueOf) ?: roleDefault
            AgentTable(report.agents, shown, onChoose = { choosing = true }, uiState.catalog)
            if (choosing) {
                AgentColumnsSheet(
                    selected = shown,
                    roleDefault = roleDefault,
                    mainRole = mainRole,
                    onSelect = { chosen = it.name },
                    onDismiss = { choosing = false },
                )
            }
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
            // 프로필 머리처럼 역할 아이콘을 이름 바로 앞에 둔다. 글자 크기에 맞춰 키워서 글씨를 키운 사용자에게도 같은 비율이다.
            val titleStyle = OvalitTheme.typography.titleL
            val iconSize = with(LocalDensity.current) { titleStyle.fontSize.toDp() }
            Row(modifier = Modifier.alignByBaseline(), verticalAlignment = Alignment.CenterVertically) {
                RoleIcon(role, tint = colors.t1, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(OvalitSpacing.sm))
                OvalitText(text = stringResource(role.label), style = titleStyle)
            }
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
                    text = stringResource(CoreUiRes.string.agents_matches, share.matches),
                    modifier = Modifier.width(48.dp),
                    style = OvalitTheme.typography.metricS,
                    color = if (isMain) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private class MetricColumnSpec(val title: StringResource, val format: MetricFormat, val value: (MatchMetrics) -> Double?)

/**
 * 요원별 표의 오른쪽 두 열입니다. 처음에는 주 역할에 맞춘 묶음을 보여 주고([defaultFor]), 표 위 버튼으로 바꿉니다.
 * 두 열씩 묶어 두면 무엇을 골라도 표 모양이 그대로입니다.
 */
private enum class AgentColumns(val label: StringResource, private val specs: () -> List<MetricColumnSpec>) {
    FIRST_DUEL(
        Res.string.agents_columns_first_duel,
        {
            listOf(
                MetricColumnSpec(Res.string.column_first_duel_involvement, MetricFormat.PERCENT) { it.firstDuelInvolvement },
                MetricColumnSpec(Res.string.column_first_duel_win, MetricFormat.PERCENT) { it.firstDuelWinRate },
            )
        },
    ),
    KAST(
        Res.string.agents_columns_kast,
        {
            listOf(
                MetricColumnSpec(Res.string.column_kast, MetricFormat.PERCENT) { it.kast },
                MetricColumnSpec(Res.string.column_survival, MetricFormat.PERCENT) { it.survivalRate },
            )
        },
    ),
    FIGHT(Res.string.agents_columns_fight, { listOf(FixedMetric.KD.column(), FixedMetric.DAMAGE.column()) }),
    SCORE(Res.string.agents_columns_score, { listOf(FixedMetric.COMBAT_SCORE.column(), FixedMetric.HEADSHOT_RATE.column()) }),
    ;

    val columns: List<MetricColumnSpec> get() = specs()

    companion object {
        // 타격대는 퍼블 쪽으로 보고 나머지 역할은 관여율과 생존율로 본다
        fun defaultFor(role: Role): AgentColumns = if (role == Role.DUELIST) FIRST_DUEL else KAST
    }
}

private fun FixedMetric.column() = MetricColumnSpec(label, format) { value(it) }

@Composable
private fun AgentColumnsSheet(
    selected: AgentColumns,
    roleDefault: AgentColumns,
    mainRole: Role,
    onSelect: (AgentColumns) -> Unit,
    onDismiss: () -> Unit,
) {
    OvalitBottomSheet(title = stringResource(Res.string.agents_columns_sheet_title), onDismiss = onDismiss) {
        Column(modifier = Modifier.selectableGroup()) {
            AgentColumns.entries.forEach { option ->
                OvalitSheetOption(
                    text = stringResource(option.label),
                    selected = option == selected,
                    caption = if (option == roleDefault) {
                        stringResource(Res.string.agents_columns_role_default, stringResource(mainRole.label))
                    } else {
                        null
                    },
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentTable(agents: List<AgentStats>, shown: AgentColumns, onChoose: () -> Unit, catalog: ContentCatalog) {
    val columns = shown.columns
    val shownLabel = stringResource(shown.label)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = stringResource(Res.string.agents_by_agent),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        OvalitPickerButton(
            text = shownLabel,
            onClickLabel = stringResource(Res.string.agents_columns_button, shownLabel),
            onClick = onChoose,
        )
    }
    // 열 제목을 칸마다 따로 줄이면 "전투점수"만 작아진다. 가장 긴 제목에 맞춘 크기를 셋에 같이 쓴다.
    val titles = listOf(stringResource(CoreUiRes.string.column_win_rate)) + columns.map { stringResource(it.title) }
    val headerStyle = rememberFittingStyle(titles, OvalitTheme.typography.caption, MetricColumn - ColumnGap)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter)) {
        Spacer(Modifier.weight(1f))
        HeaderCell(titles[0], WinColumn, headerStyle)
        titles.drop(1).forEach { HeaderCell(it, MetricColumn, headerStyle) }
    }
    Spacer(Modifier.height(OvalitSpacing.sm))
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 한 줄이라도 "타격대 · 20판 · KDA 1.88"이 이름 밑에 안 들어가면 모든 줄에서 KDA를 한 줄 내린다. 줄마다 따로
        // 꺾으면 줄 높이가 제각각이다.
        val nameWidth = maxWidth - OvalitSpacing.gutter * 2 - ThumbnailSize - OvalitSpacing.md - WinColumn - MetricColumn * columns.size
        val captions = agents.map { agent ->
            AnnotatedString(listOfNotNull(roleText(agent), stringResource(CoreUiRes.string.agents_matches, agent.matches), rowKda(agent)?.text).joinToString(" · "))
        }
        val stackKda = !rememberFitsOnOneLine(captions, OvalitTheme.typography.caption, nameWidth)
        Column {
            agents.forEachIndexed { index, agent ->
                if (index > 0) {
                    OvalitDivider(Modifier.padding(start = OvalitSpacing.gutter + ThumbnailSize + OvalitSpacing.md), color = OvalitTheme.colors.lineWeak)
                }
                AgentRow(agent, columns, catalog, stackKda)
            }
        }
    }
}

@Composable
private fun roleText(agent: AgentStats): String = agent.role?.let { stringResource(it.label) } ?: NO_VALUE

// 승률처럼 5판을 넘긴 요원만 KDA를 적는다
@Composable
private fun rowKda(agent: AgentStats): AnnotatedString? = agent.metrics.kda?.takeIf { agent.isMeasurable }?.let { kdaRatioText(it) }

@Composable
private fun HeaderCell(text: String, width: Dp, style: TextStyle) {
    OvalitText(
        text = text,
        modifier = Modifier.width(width).padding(start = ColumnGap),
        style = style,
        color = OvalitTheme.colors.t3,
        textAlign = TextAlign.End,
        maxLines = 1,
        autoSize = shrinkToFit(style.fontSize, min = 7.sp),
    )
}

@Composable
private fun AgentRow(agent: AgentStats, columns: List<MetricColumnSpec>, catalog: ContentCatalog, stackKda: Boolean) {
    val name = catalog.agentName(agent.agent)
    val kda = rowKda(agent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = OvalitSpacing.gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentImage(agent.agent, name, Modifier.size(ThumbnailSize).clip(RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            OvalitText(text = name, style = OvalitTheme.typography.bodyStrong, maxLines = 1)
            // 좁은 칸에서 판 수를 통째로 다음 줄에 내린다. 한 글줄로 두면 점이 줄 끝에 남는다.
            val caption = OvalitTheme.typography.caption
            SeparatedRow(
                items = listOfNotNull<@Composable () -> Unit>(
                    { OvalitText(roleText(agent), style = caption, color = OvalitTheme.colors.t3) },
                    { OvalitText(stringResource(CoreUiRes.string.agents_matches, agent.matches), style = caption, color = OvalitTheme.colors.t3) },
                    kda?.takeIf { !stackKda }?.let { { OvalitText(text = it, style = caption, color = OvalitTheme.colors.t3) } },
                ),
                separator = { OvalitText(text = " · ", style = caption, color = OvalitTheme.colors.t3) },
            )
            if (stackKda && kda != null) {
                OvalitText(text = kda, style = caption, color = OvalitTheme.colors.t3, maxLines = 1)
            }
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
                text = column.value(agent.metrics)?.let { column.format.valueText(it) } ?: NO_VALUE,
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
