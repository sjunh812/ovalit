package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val FADE_MILLIS = 220

/**
 * 긴 화면을 묶음마다 한 프레임씩 나눠 그리고, 다 그린 뒤 [placeholder]에서 내용으로 서서히 바꿉니다. 묶음은 [OvalitStage]로
 * 감쌉니다. 한 프레임에 다 그리면 그 프레임이 길어져 스켈레톤이 멈췄다가 숫자가 툭 튀어나옵니다.
 *
 * 나눠 그리는 동안 내용은 보이지 않고 화면 읽기 프로그램에도 읽히지 않습니다. 내용은 바탕을 칠한 채 [placeholder] 위로 나타나서, 두
 * 쪽에 똑같이 있는 머리 줄은 바뀌는 동안에도 흐려지지 않습니다.
 *
 * @param ready 내용을 그려도 되는지입니다. 처음부터 `true`면 탭을 오가거나 뒤로 돌아온 경우라 나누지 않고 한 번에 그립니다.
 * @param contentBackground 내용 밑에 까는 바탕입니다. 화면 바탕과 같아야 나타나는 동안 틈이 안 보입니다.
 */
@Composable
fun OvalitStaged(
    ready: Boolean,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentBackground: Color = OvalitTheme.colors.bg,
    content: @Composable () -> Unit,
) {
    val staging = remember { Staging(done = ready) }
    val alpha = remember { Animatable(if (ready) 1f else 0f) }
    val faded by remember { derivedStateOf { alpha.value >= 1f } }
    if (ready) {
        LaunchedEffect(staging) {
            if (!staging.done) {
                // 프레임 콜백 안에서 늘려야 그 프레임에 바로 그린다. 늘린 뒤에 기다리면 한 프레임씩 밀린다.
                while (staging.revealed < staging.registered) {
                    withFrameNanos { staging.revealed++ }
                }
                // 마지막 묶음을 그린 프레임과 나타나기 시작하는 프레임을 떼어 둔다
                withFrameNanos {}
                staging.done = true
            }
            alpha.animateTo(1f, tween(FADE_MILLIS))
        }
    }

    Box(modifier = modifier) {
        if (!ready || !faded) placeholder()
        if (ready) {
            CompositionLocalProvider(LocalStaging provides staging) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (staging.done) Modifier else Modifier.clearAndSetSemantics {})
                        .graphicsLayer { this.alpha = alpha.value }
                        .background(contentBackground),
                ) {
                    content()
                }
            }
        }
    }
}

/** [OvalitStaged] 안에서 한 프레임에 그릴 묶음입니다. 바깥에서는 바로 그립니다. */
@Composable
fun OvalitStage(content: @Composable () -> Unit) {
    val staging = LocalStaging.current
    if (staging == null) {
        content()
        return
    }
    // 처음 그릴 때 나온 순서대로 번호를 받아서 위쪽 묶음부터 그린다
    val index = remember(staging) { staging.register() }
    val shown by remember(staging) { derivedStateOf { staging.done || index < staging.revealed } }
    if (shown) content()
}

private val LocalStaging = staticCompositionLocalOf<Staging?> { null }

@Stable
private class Staging(done: Boolean) {
    // 번호를 나눠 줄 때만 늘고 화면에 쓰이지 않아서 상태로 두지 않는다
    var registered = 0
        private set
    var revealed by mutableIntStateOf(1)
    var done by mutableStateOf(done)

    fun register(): Int = registered++
}
