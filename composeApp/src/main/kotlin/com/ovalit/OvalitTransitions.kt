package com.ovalit

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

// 화면 전환은 iOS 내비게이션을 따른다(CLAUDE.md 디자인). 새 화면은 오른쪽에서 밀려 들어오고, 아래 화면은 폭의
// 4분의 1만 왼쪽으로 비키면서 조금 어두워진다. 뒤로 가기 스와이프는 손가락을 따라 위 화면을 옆으로 밀어낸다.
//
// 어둡게 하는 건 아래 화면의 불투명도를 낮춰 NavDisplay 뒤에 깐 검은 바탕이 비치게 해서 만든다. 그래서 위 화면은
// 늘 불투명해야 하고, 두 화면을 같이 흐리게 하는 크로스페이드는 쓰지 않는다. 쓰면 가운데서 검은 바탕이 드러난다.

private const val SLIDE_MILLIS = 300
private const val PARALLAX_DIVISOR = 4

// 들어갈 때와 나갈 때 같은 곡선을 쓴다. 빠르게 출발해 부드럽게 멈춘다. 처음에는 FastOutSlowIn이었는데, 천천히 출발하는 곡선이라
// 나가는 화면은 움직임이 보이는 출발 구간이 느려 들어올 때보다 굼뜨게 느껴졌다(사용자 요청, 2026-10-03). 들어오는 화면은 멈추는
// 끝 구간이 보여서 몰랐다.
private val SlideEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// 다크 바탕은 검은색에 가까워 조금 어둡게 해서는 두 화면이 갈리지 않는다. 그래서 글자까지 눈에 띄게 어둡게 한다.
private const val LIGHT_DIMMED_ALPHA = 0.9f
private const val DARK_DIMMED_ALPHA = 0.6f

/** 밀려난 아래 화면의 불투명도입니다. */
internal fun dimmedAlpha(isDark: Boolean): Float = if (isDark) DARK_DIMMED_ALPHA else LIGHT_DIMMED_ALPHA

/** 새 화면을 쌓을 때의 전환입니다. */
internal fun <T : Any> pushTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    val spec = tween<Float>(SLIDE_MILLIS, easing = SlideEasing)
    val offset = tween<IntOffset>(SLIDE_MILLIS, easing = SlideEasing)
    ContentTransform(
        targetContentEnter = slideInHorizontally(offset) { it },
        initialContentExit = slideOutHorizontally(offset) { -it / PARALLAX_DIVISOR } + fadeOut(spec, targetAlpha = dimmedAlpha),
    )
}

/** 뒤로 가기 버튼처럼 스와이프 없이 화면을 뺄 때의 전환입니다. */
internal fun <T : Any> popTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    slideBack(easedSpec = true, dimmedAlpha = dimmedAlpha)
}

/**
 * 뒤로 가기 스와이프를 하는 동안의 전환입니다. 진행도만큼 그대로 움직여야 손가락을 따라오는 것처럼 보여서 속도 곡선을
 * 두지 않습니다. 안드로이드는 오른쪽 가장자리에서도 뒤로 가기를 쓸 수 있지만, 어느 쪽에서 시작해도 뒤로 가기 버튼처럼
 * 오른쪽으로 뺍니다(사용자 요청, 2026-09-28). 가장자리마다 방향이 바뀌면 같은 뒤로 가기가 두 가지로 보입니다.
 */
internal fun <T : Any> predictivePopTransition(dimmedAlpha: Float):
    AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
    slideBack(easedSpec = false, dimmedAlpha = dimmedAlpha)
}

private fun slideBack(easedSpec: Boolean, dimmedAlpha: Float): ContentTransform {
    val easing = if (easedSpec) SlideEasing else LinearEasing
    val spec = tween<Float>(SLIDE_MILLIS, easing = easing)
    val offset = tween<IntOffset>(SLIDE_MILLIS, easing = easing)
    return ContentTransform(
        targetContentEnter = slideInHorizontally(offset) { -it / PARALLAX_DIVISOR } + fadeIn(spec, initialAlpha = dimmedAlpha),
        initialContentExit = slideOutHorizontally(offset) { it },
    )
}

/**
 * 하단 탭끼리 오갈 때의 전환입니다. 탭은 쌓는 화면이 아니라서 밀거나 흐리게 하지 않고 바로 바꿉니다.
 * 뒤로 가기 스와이프로 홈에 돌아갈 때도 스와이프하는 동안은 그대로 두고 손을 떼면 바꿉니다(CLAUDE.md 화면).
 */
internal val TabTransitions: Map<String, Any> = run {
    val instant: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform = {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    }
    NavDisplay.transitionSpec(instant) +
        NavDisplay.popTransitionSpec(instant) +
        NavDisplay.predictivePopTransitionSpec { instant() }
}
