package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val PULSE_MILLIS = 900

/**
 * 불러오는 동안 화면 모양대로 자리만 잡아 두는 틀입니다. 빈 화면보다 무엇이 올지 먼저 보여서 기다림이 짧게
 * 느껴집니다. 안의 칸이 모두 같이 천천히 깜빡이고, 화면 낭독기에는 [description] 한 줄만 읽힙니다.
 */
@Composable
fun OvalitSkeleton(description: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

/** [OvalitSkeleton] 안에 두는 회색 칸 하나입니다. */
@Composable
fun SkeletonBlock(width: Dp, height: Dp, modifier: Modifier = Modifier, radius: Dp = 6.dp) {
    Box(modifier.size(width = width, height = height).background(OvalitTheme.colors.fill, RoundedCornerShape(radius)))
}
