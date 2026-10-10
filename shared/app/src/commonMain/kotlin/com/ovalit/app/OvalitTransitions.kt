package com.ovalit.app

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

// 화면 전환은 iOS 내비게이션을 따른다(docs/design.md).
// 아래 화면은 불투명도를 낮춰 NavDisplay 뒤에 깐 검은 바탕이 비치게 해서 어둡게 한다.
// 그래서 위 화면은 늘 불투명해야 하고 크로스페이드는 쓰지 않는다. 쓰면 전환 가운데서 검은 바탕이 드러난다.

private const val SLIDE_MILLIS = 300
private const val PARALLAX_DIVISOR = 4

// 들어갈 때와 나갈 때 같은 곡선을 쓴다. FastOutSlowIn처럼 천천히 출발하는 곡선은 나가는 화면이 굼떠 보인다.
private val SlideEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// 다크 바탕은 검은색에 가까워 조금 어둡게 해서는 두 화면이 갈리지 않는다
private const val LIGHT_DIMMED_ALPHA = 0.9f
private const val DARK_DIMMED_ALPHA = 0.6f

internal fun dimmedAlpha(isDark: Boolean): Float = if (isDark) DARK_DIMMED_ALPHA else LIGHT_DIMMED_ALPHA

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
 * 뒤로 가기 스와이프를 하는 동안의 전환입니다. 손가락을 따라오게 진행도만큼 그대로 움직이고 속도 곡선은 두지 않습니다.
 * 오른쪽 가장자리에서 시작해도 뒤로 가기 버튼처럼 오른쪽으로 뺍니다.
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
 * 뒤로 가기 스와이프로 홈에 돌아갈 때도 스와이프하는 동안은 그대로 두고 손을 떼면 바꿉니다(docs/screens.md).
 */
internal val TabTransitions: Map<String, Any> = run {
    val instant: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform = {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    }
    NavDisplay.transitionSpec(instant) +
        NavDisplay.popTransitionSpec(instant) +
        NavDisplay.predictivePopTransitionSpec { instant() }
}
