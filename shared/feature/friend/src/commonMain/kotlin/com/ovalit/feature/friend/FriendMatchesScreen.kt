package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.PlayerId
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.MatchRowsSkeleton
import com.ovalit.core.ui.dayLabel
import com.ovalit.core.ui.matchTimeLabel
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.matches_hidden
import com.ovalit.feature.friend.resources.matches_title
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** S5 "전체 보기"로 여는 친구의 경기 목록입니다. 줄은 눌러도 열리지 않습니다. */
@Composable
fun FriendMatchesRoute(
    friendId: PlayerId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendProfileViewModel = koinViewModel(key = friendId.value) { parametersOf(friendId.value) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState == FriendProfileUiState.Gone) onBack()
    }
    FriendMatchesScreen(uiState, onBack, modifier)
}

@Composable
internal fun FriendMatchesScreen(uiState: FriendProfileUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        // 밀려 들어오는 중에 도착하면 다 들어올 때까지 줄 자리만 잡아 둔다
        val shown = rememberContentShown(loaded = uiState is FriendProfileUiState.Success)
        if (uiState !is FriendProfileUiState.Success || !shown) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                OvalitBackTopBar(onBack = onBack)
                Spacer(Modifier.height(OvalitSpacing.lg))
                MatchRowsSkeleton(rows = 8, style = MatchRowStyle.COMPACT)
            }
            return@Box
        }
        val name = uiState.friend.riotId.substringBefore('#')
        val today = uiState.now.toLocalDateTime(uiState.timeZone).date
        val days = uiState.friend.matches
            .sortedByDescending { it.startedAt }
            .groupBy { it.startedAt.toLocalDateTime(uiState.timeZone).date }

        LazyColumn(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            item {
                OvalitBackTopBar(onBack = onBack, title = stringResource(Res.string.matches_title, name))
            }
            // 보는 중에 친구가 전적을 비공개로 바꿀 수 있다. 경기는 ViewModel이 이미 비웠고 까닭만 적는다.
            if (!uiState.friend.statsPublic) {
                item {
                    OvalitText(
                        text = stringResource(Res.string.matches_hidden),
                        modifier = Modifier.padding(OvalitSpacing.gutter),
                        color = colors.t2,
                    )
                }
            }
            days.forEach { (date, matches) ->
                item(key = "day-$date") {
                    OvalitText(
                        text = dayLabel(date, today),
                        modifier = Modifier
                            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs)
                            .semantics { heading() },
                        style = OvalitTheme.typography.label,
                        color = colors.t3,
                    )
                }
                items(matches, key = { it.id.value }) { match ->
                    Column {
                        MatchRow(
                            match = match,
                            catalog = uiState.catalog,
                            timeLabel = matchTimeLabel(match.startedAt, uiState.now, uiState.timeZone),
                            style = MatchRowStyle.COMPACT,
                        )
                        if (match != matches.last()) OvalitDivider(Modifier.padding(start = 67.dp), color = colors.lineWeak)
                    }
                }
            }
            item { Spacer(Modifier.height(OvalitSpacing.xxl)) }
        }
    }
}
