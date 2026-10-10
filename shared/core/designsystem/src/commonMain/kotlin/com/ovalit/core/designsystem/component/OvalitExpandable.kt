package com.ovalit.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitTheme

// 접을 때를 더 짧게 둔다.
// 오래 걸리면 누른 뒤 기다리는 느낌이 든다.
private const val EXPAND_MILLIS = 240
private const val COLLAPSE_MILLIS = 180

/** 높이가 부드럽게 바뀌며 접히고 펴지는 영역입니다. */
@Composable
fun OvalitExpandable(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(EXPAND_MILLIS, easing = FastOutSlowInEasing)) + fadeIn(tween(EXPAND_MILLIS)),
        exit = shrinkVertically(tween(COLLAPSE_MILLIS, easing = FastOutSlowInEasing)) + fadeOut(tween(COLLAPSE_MILLIS)),
    ) {
        content()
    }
}

/**
 * 펼침 여부를 보여주는 화살표입니다.
 * 오른쪽을 보다가 펼치면 아래로 돕니다.
 *
 * @param pointsDown 다른 화면으로 넘어가지 않고 그 자리에서 펼치기만 하는 줄이면 `true`입니다.
 *   아래를 보다가 펼치면 위로 돕니다.
 *   오른쪽 화살표는 화면을 넘기는 줄에도 쓰여서 둘을 가릅니다.
 */
@Composable
fun OvalitDisclosureIcon(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = OvalitTheme.colors.t3,
    size: Dp = 14.dp,
    pointsDown: Boolean = false,
) {
    val rotation by animateFloatAsState(
        targetValue = when {
            pointsDown -> if (expanded) 180f else 0f
            else -> if (expanded) 90f else 0f
        },
        animationSpec = tween(if (expanded) EXPAND_MILLIS else COLLAPSE_MILLIS, easing = FastOutSlowInEasing),
    )
    OvalitIcon(
        if (pointsDown) OvalitIcons.ChevronDown else OvalitIcons.ChevronRight,
        contentDescription = null,
        modifier = modifier.rotate(rotation),
        tint = tint,
        size = size,
    )
}
