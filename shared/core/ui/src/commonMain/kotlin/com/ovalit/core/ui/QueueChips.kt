package com.ovalit.core.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitChip
import com.ovalit.core.designsystem.component.OvalitChipSkeleton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.model.QueueFilter
import org.jetbrains.compose.resources.stringResource

/**
 * 큐 칩입니다. 고른 칩은 저장하지 않습니다(CLAUDE.md 큐).
 * 홈과 경기 탭은 맨 위에 경쟁 + 일반 · 경쟁 · 일반 · 기타를 두고 프로필은 티어 카드 밑에 기타를 뺀 셋을 둡니다([ProfileQueueChips]).
 */
@Composable
fun QueueChips(
    selected: QueueFilter,
    onSelect: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<QueueFilter> = QueueFilter.entries,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OvalitSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        filters.forEach { filter ->
            OvalitChip(text = stringResource(filter.label), selected = filter == selected, onClick = { onSelect(filter) })
        }
    }
}

/**
 * 큐 칩을 받기 전의 자리입니다.
 * [OvalitSkeleton] 안에 둡니다.
 * 홈과 경기 탭이 같이 쓰고 [QueueChips]와 자리도 높이도 같아서 칩이 나타나도 아래 내용이 움직이지 않습니다.
 */
@Composable
fun QueueChipsSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = OvalitSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        OvalitChipSkeleton(width = 88.dp)
        repeat(QueueFilter.entries.size - 1) { OvalitChipSkeleton(width = 52.dp) }
    }
}
