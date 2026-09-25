package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitChip
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPullToRefresh
import com.ovalit.core.designsystem.component.OvalitTabHeader
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.dayLabel
import com.ovalit.core.ui.label
import com.ovalit.core.ui.mapName
import com.ovalit.core.ui.matchTimeLabel
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.empty_filter
import com.ovalit.feature.match.resources.empty_queue
import com.ovalit.feature.match.resources.filter
import com.ovalit.feature.match.resources.filter_active
import com.ovalit.feature.match.resources.filter_agent
import com.ovalit.feature.match.resources.filter_all
import com.ovalit.feature.match.resources.filter_map
import com.ovalit.feature.match.resources.filter_reset
import com.ovalit.feature.match.resources.matches_title
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val TouchSize = 44.dp

/** S2 경기 목록입니다. 하단 탭의 경기에서 엽니다. */
@Composable
fun MatchesRoute(
    onOpenMatch: (MatchId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    MatchesScreen(
        uiState = uiState,
        onSelectQueue = viewModel::selectQueue,
        onFilter = viewModel::setFilter,
        onOpenMatch = onOpenMatch,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun MatchesScreen(
    uiState: MatchesUiState,
    onSelectQueue: (QueueFilter) -> Unit,
    onFilter: (MatchFilter) -> Unit,
    onOpenMatch: (MatchId) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val colors = OvalitTheme.colors
    var filtering by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is MatchesUiState.Success) return@Box

        OvalitPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    OvalitTabHeader(title = stringResource(Res.string.matches_title)) {
                        FilterButton(active = uiState.filter.isActive, onClick = { filtering = true })
                    }
                    Spacer(Modifier.height(OvalitSpacing.xs))
                    QueueChips(selected = uiState.queueFilter, onSelect = onSelectQueue)
                    Spacer(Modifier.height(OvalitSpacing.md))
                }

                if (uiState.days.isEmpty()) {
                    item {
                        OvalitText(
                            text = stringResource(if (uiState.filter.isActive) Res.string.empty_filter else Res.string.empty_queue),
                            modifier = Modifier.padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xl),
                            style = OvalitTheme.typography.body,
                            color = colors.t2,
                        )
                    }
                }

                uiState.days.forEach { day ->
                    item(key = "day-${day.date}") {
                        DayHeader(day.date, today = uiState.now.toLocalDateTime(uiState.timeZone).date)
                    }
                    items(day.matches, key = { it.id.value }) { match ->
                        Column {
                            MatchRow(
                                match = match,
                                catalog = uiState.catalog,
                                timeLabel = matchTimeLabel(match.startedAt, uiState.now, uiState.timeZone),
                                style = MatchRowStyle.LIST,
                                onClick = { onOpenMatch(match.id) },
                            )
                            if (match != day.matches.last()) {
                                OvalitDivider(Modifier.padding(start = 87.dp), color = colors.lineWeak)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(OvalitSpacing.xxl)) }
            }
        }
    }

    if (filtering && uiState is MatchesUiState.Success) {
        FilterSheet(uiState, onFilter = onFilter, onDismiss = { filtering = false })
    }
}

@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (active) Res.string.filter_active else Res.string.filter)
    Box(
        modifier = Modifier
            .size(TouchSize)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        OvalitIcon(OvalitIcons.Filter, contentDescription = description, tint = if (active) OvalitTheme.colors.t1 else OvalitTheme.colors.t2)
        // 필터를 켜 둔 걸 잊고 경기가 모자란다고 여기지 않게 작은 점을 찍는다
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 9.dp)
                    .size(6.dp)
                    .background(OvalitTheme.colors.t1, CircleShape),
            )
        }
    }
}

@Composable
private fun QueueChips(selected: QueueFilter, onSelect: (QueueFilter) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OvalitSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        QueueFilter.entries.forEach { filter ->
            OvalitChip(text = stringResource(filter.label), selected = filter == selected, onClick = { onSelect(filter) })
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, today: LocalDate) {
    OvalitText(
        text = dayLabel(date, today),
        modifier = Modifier
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs)
            .semantics { heading() },
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
private fun FilterSheet(uiState: MatchesUiState.Success, onFilter: (MatchFilter) -> Unit, onDismiss: () -> Unit) {
    val filter = uiState.filter
    OvalitBottomSheet(title = stringResource(Res.string.filter), onDismiss = onDismiss) {
        FilterGroup(
            title = stringResource(Res.string.filter_agent),
            options = uiState.agents,
            selected = filter.agent,
            name = { uiState.catalog.agentName(it) },
            onSelect = { onFilter(filter.copy(agent = it)) },
        )
        Spacer(Modifier.height(OvalitSpacing.lg))
        FilterGroup(
            title = stringResource(Res.string.filter_map),
            options = uiState.maps,
            selected = filter.map,
            name = { uiState.catalog.mapName(it) },
            onSelect = { onFilter(filter.copy(map = it)) },
        )
        Spacer(Modifier.height(OvalitSpacing.lg))
        OvalitTextButton(
            text = stringResource(Res.string.filter_reset),
            onClick = { onFilter(MatchFilter()) },
            enabled = filter.isActive,
        )
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<T>,
    selected: T?,
    name: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    OvalitText(text = title, style = OvalitTheme.typography.label, color = OvalitTheme.colors.t3)
    Spacer(Modifier.height(OvalitSpacing.xs))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs)) {
        OvalitChip(text = stringResource(Res.string.filter_all), selected = selected == null, onClick = { onSelect(null) })
        options.forEach { option ->
            OvalitChip(text = name(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}
