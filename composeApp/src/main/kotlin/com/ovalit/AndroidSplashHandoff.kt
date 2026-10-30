package com.ovalit

import androidx.core.splashscreen.SplashScreenViewProvider
import com.ovalit.core.designsystem.component.SplashHandoff
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

// 안드로이드 12 이상의 시스템 스플래시는 앱이 뜰 때까지 점을 출렁이게 한다(ic_splash_anim.xml).
// 앱의 첫 프레임이 그려지면 스플래시를 바로 걷지 않고 들고 있다가, 로고 애니메이션이 같은 위상을 그린 뒤 takeOver에서 걷는다.
internal class AndroidSplashHandoff(private val onRemoved: () -> Unit) : SplashHandoff {

    private val iconElapsed = CompletableDeferred<Long?>()
    private var provider: SplashScreenViewProvider? = null
    private var takenOver = false

    // 앱의 첫 프레임이 그려진 뒤 메인 스레드에서 불린다
    fun onExit(provider: SplashScreenViewProvider) {
        if (takenOver) {
            provider.remove()
            onRemoved()
            return
        }
        this.provider = provider
        // 시작 시각은 벽시계 기준이고, 스플래시가 움직이지 않으면(안드로이드 12 미만) 0이다
        val start = provider.iconAnimationStartMillis
        iconElapsed.complete(if (start > 0) System.currentTimeMillis() - start else null)
    }

    // 스플래시 없이 열린 경우(다른 앱에서 열기 등)에는 넘어올 것이 없어 오래 기다리지 않는다
    override suspend fun awaitIconElapsed(): Long? = withTimeoutOrNull(WAIT_MILLIS) { iconElapsed.await() }

    override fun takeOver() {
        takenOver = true
        provider?.let {
            it.remove()
            onRemoved()
        }
        provider = null
    }

    private companion object {
        const val WAIT_MILLIS = 300L
    }
}
