package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val PULSE_MILLIS = 900
private const val PULSE_DIM = 0.45f

/**
 * 불러오는 동안 화면 모양대로 자리만 잡아 두는 틀입니다.
 * 안의 칸이 모두 같이 천천히 깜빡이고 화면 읽기 프로그램에는 [description] 한 줄만 읽힙니다.
 *
 * 깜빡임은 틀마다 따로 시작하지 않고 프레임 시각에서 바로 셉니다([skeletonPulse]).
 * 따로 시작하면 새 틀이 나타날 때마다 가장 흐린 데서 다시 시작해서, 틀이 바뀌는 순간 툭 튀고 한 화면의 틀끼리 엇갈려 깜빡였습니다.
 */
@Composable
fun OvalitSkeleton(description: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alpha = remember { mutableFloatStateOf(PULSE_DIM) }
    LaunchedEffect(alpha) {
        // 무한 애니메이션으로 기다려야 테스트처럼 무한 애니메이션을 멈추는 환경에서 멈춘다
        while (true) withInfiniteAnimationFrameMillis { alpha.floatValue = skeletonPulse(it) }
    }
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            // 그리는 단계에서만 읽어 깜빡일 때 다시 짜지 않는다
            .graphicsLayer { this.alpha = alpha.floatValue },
    ) {
        content()
    }
}

/**
 * [frameMillis] 시각의 깜빡임 밝기입니다.
 * [PULSE_MILLIS] 동안 밝아지고 같은 시간 동안 흐려지는 것을 되풀이합니다.
 * 같은 시각이면 어느 틀이든 같은 값이라 한 화면의 틀이 함께 깜빡입니다.
 */
internal fun skeletonPulse(frameMillis: Long): Float {
    val phase = (frameMillis % (PULSE_MILLIS * 2)).toFloat() / PULSE_MILLIS
    val rising = if (phase <= 1f) phase else 2f - phase
    return PULSE_DIM + (1f - PULSE_DIM) * FastOutSlowInEasing.transform(rising)
}

/** [OvalitSkeleton] 안에 두는 회색 칸 하나입니다. */
@Composable
fun SkeletonBlock(width: Dp, height: Dp, modifier: Modifier = Modifier, radius: Dp = 6.dp) {
    Box(modifier.size(width = width, height = height).background(OvalitTheme.colors.fill, RoundedCornerShape(radius)))
}
