package com.ovalit

import android.view.View
import androidx.core.splashscreen.SplashScreenViewProvider
import com.ovalit.core.designsystem.component.SplashHandoff
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

// 안드로이드 12 이상의 시스템 스플래시는 앱이 뜰 때까지 점을 출렁이게 한다(ic_splash_anim.xml).
// 앱의 첫 프레임이 그려지면 스플래시를 바로 걷지 않고 들고 있다가, 로고 애니메이션이 같은 위상을 그린 뒤 takeOver에서 걷는다.
internal class AndroidSplashHandoff(
    private val onRemoved: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) : SplashHandoff {

    private val iconElapsed = CompletableDeferred<Long?>()
    private var exit: SplashExit? = null
    private var takenOver = false

    // 앱의 첫 프레임이 그려진 뒤 메인 스레드에서 불린다
    fun onExit(provider: SplashScreenViewProvider) = handleExit(ProviderExit(provider))

    internal fun handleExit(exit: SplashExit) {
        // 느린 기기에서는 1초를 기다려도 스플래시가 넘어오지 않아 앱이 먼저 넘겨받는다.
        // 그 뒤에 넘어온 스플래시도 같은 순서로 걷어야 빨간 화면이 한 프레임 비치지 않는다.
        if (takenOver) {
            remove(exit)
            return
        }
        this.exit = exit
        // 시작 시각은 벽시계 기준이고, 스플래시가 움직이지 않으면(안드로이드 12 미만) 0이다
        val start = exit.iconAnimationStartMillis
        iconElapsed.complete(if (start > 0) now() - start else null)
    }

    // 스플래시 없이 열린 경우(다른 앱에서 열기 등)에는 넘어올 것이 없어 오래 기다리지 않는다.
    // 느린 기기는 첫 프레임까지 300ms를 넘기기도 해서 1초까지는 기다린다.
    override suspend fun awaitIconElapsed(): Long? = withTimeoutOrNull(WAIT_MILLIS) { iconElapsed.await() }

    // 걷으면 점을 그리던 아이콘 층은 바로 없어지는데 빨간 바탕은 다음 프레임에 지워져서 한 프레임 동안 말풍선 없는 빨간 화면이 비친다.
    // 그래서 이번 프레임에는 바탕까지 숨겨 밑의 로고 애니메이션이 보이게 하고 다음 프레임에 걷는다.
    override fun takeOver() {
        takenOver = true
        exit?.let(::remove)
        exit = null
    }

    private fun remove(exit: SplashExit) {
        exit.hide()
        exit.removeOnNextFrame(onRemoved)
    }

    private companion object {
        const val WAIT_MILLIS = 1_000L
    }
}

/**
 * 시스템 스플래시를 걷는 데 쓰는 것만 뽑았습니다.
 * 테스트는 가짜를 넣어 걷는 순서를 봅니다.
 */
internal interface SplashExit {
    /** 점이 움직이기 시작한 벽시계 시각입니다. 움직이지 않는 스플래시면 0입니다. */
    val iconAnimationStartMillis: Long

    /** 이번 프레임에 바탕까지 숨깁니다. */
    fun hide()

    /** 다음 프레임에 걷고 [onRemoved]를 부릅니다. */
    fun removeOnNextFrame(onRemoved: () -> Unit)
}

private class ProviderExit(private val provider: SplashScreenViewProvider) : SplashExit {
    override val iconAnimationStartMillis: Long get() = provider.iconAnimationStartMillis

    override fun hide() {
        provider.view.visibility = View.INVISIBLE
    }

    override fun removeOnNextFrame(onRemoved: () -> Unit) {
        provider.view.postOnAnimation {
            provider.remove()
            onRemoved()
        }
    }
}
