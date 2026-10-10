package com.ovalit.core.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.component.OvalitChip
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.model.QueueFilter
import org.jetbrains.compose.resources.stringResource

/** 홈과 경기 탭 맨 위의 큐 칩(경쟁 + 일반 · 경쟁 · 일반 · 기타)입니다. 고른 칩은 저장하지 않습니다(CLAUDE.md 큐). */
@Composable
fun QueueChips(
    selected: QueueFilter,
    onSelect: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OvalitSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        QueueFilter.entries.forEach { filter ->
            OvalitChip(text = stringResource(filter.label), selected = filter == selected, onClick = { onSelect(filter) })
        }
    }
}
