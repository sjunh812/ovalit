package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val ItemHeight = 44.dp
private const val VISIBLE_ITEMS = 5

/**
 * 돌려서 하나를 고르는 휠입니다. 가운데 띠에 든 줄이 고른 것이고, 손을 떼면 가까운 줄에 멈춥니다. 줄을 눌러도 그 줄로
 * 돌아갑니다. 당근 약속 잡기처럼 고를 것이 많을 때 칩을 늘어놓는 대신 씁니다(사용자 요청, 2026-10-03). 칩 스물네 개는
 * 벽처럼 보였습니다.
 *
 * 멈췄을 때만 [onSelect]를 부릅니다. 돌리는 동안 지나가는 줄마다 부르면 고른 값이 계속 바뀝니다.
 *
 * @param enabled 고를 수 없는 줄은 흐리게 두고 낭독기에 비활성으로 읽힙니다. 이미 정해진 값을 보여 주되 다시 고르지 못하게
 * 할 때 씁니다. 그 줄에 멈춰도 [onSelect]는 부르므로 확인 버튼에서 막습니다.
 */
@Composable
fun OvalitWheelPicker(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (Int) -> Boolean = { true },
) {
    val colors = OvalitTheme.colors
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val scope = rememberCoroutineScope()
    val currentOnSelect by rememberUpdatedState(onSelect)
    // 가운데 띠에 가장 가까운 줄이다
    val center by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index ?: state.firstVisibleItemIndex
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to center }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, index) -> index }
            .distinctUntilChanged()
            .collect { currentOnSelect(it) }
    }

    Box(modifier = modifier.fillMaxWidth().height(ItemHeight * VISIBLE_ITEMS)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.fill),
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = ItemHeight * (VISIBLE_ITEMS / 2)),
            modifier = Modifier.fillMaxSize().selectableGroup(),
        ) {
            itemsIndexed(items) { index, text ->
                val distance = abs(index - center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .clickable(interactionSource = null, indication = null) {
                            scope.launch { state.animateScrollToItem(index) }
                        }
                        .semantics {
                            role = Role.RadioButton
                            this.selected = index == center
                            if (!enabled(index)) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 가운데에서 멀수록 옅게 둔다. 색이 아니라 밝기 단계로 가른다.
                    OvalitText(
                        text = text,
                        style = OvalitTheme.typography.body.copy(fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal),
                        color = when {
                            !enabled(index) -> colors.t4
                            distance == 0 -> colors.t1
                            distance == 1 -> colors.t2
                            else -> colors.t4
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
