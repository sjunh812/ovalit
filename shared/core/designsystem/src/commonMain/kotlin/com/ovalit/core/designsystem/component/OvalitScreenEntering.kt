package com.ovalit.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** 이 화면이 밀려 들어오는 중인지입니다. 화면 전환을 정하는 `composeApp`이 채웁니다. 전환 밖에서는 `false`입니다. */
val LocalScreenEntering = compositionLocalOf { false }

/**
 * 받아 온 내용을 지금 그려도 되는지 돌려줍니다.
 * 화면이 밀려 들어오는 중에 [loaded]가 되면 다 들어올 때까지 `false`입니다.
 * 긴 화면을 전환 한가운데서 한꺼번에 그리면 그 프레임이 늦어져 전환이 한 번 멈춘 것처럼 보입니다.
 *
 * 처음부터 내용이 있었거나 한 번 그린 뒤에는 기다리지 않습니다.
 */
@Composable
fun rememberContentShown(loaded: Boolean): Boolean {
    val entering = LocalScreenEntering.current
    // 처음부터 내용이 있으면 탭을 오가거나 뒤로 돌아온 경우라 기다리지 않는다
    val shownOnce = remember { mutableStateOf(loaded) }
    val shown = shownOnce.value || (loaded && !entering)
    if (shown && !shownOnce.value) SideEffect { shownOnce.value = true }
    return shown
}
