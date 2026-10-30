package com.ovalit.core.designsystem.component

/**
 * 시스템 스플래시에서 [OvalitLaunchAnimation]으로 넘겨받는 길입니다.
 *
 * 안드로이드 12 이상의 스플래시는 앱이 뜨기를 기다리는 동안 입력 중 점을 출렁이게 합니다.
 * 앱은 그 출렁임이 흐른 시간을 받아 같은 위상에서 이어 그리고, 같은 장면을 그린 뒤에 [takeOver]로 스플래시를 걷습니다.
 * 스플래시가 움직이지 않는 플랫폼(iOS, 안드로이드 12 미만)은 [None]입니다.
 */
interface SplashHandoff {

    /**
     * 스플래시의 점 애니메이션이 지금까지 흐른 시간(ms)입니다.
     * 스플래시가 움직이지 않았거나 넘겨받지 못하면 `null`입니다.
     * 앱의 첫 프레임이 그려져 스플래시가 넘어올 때까지 기다리고, 오래 기다리지는 않습니다.
     */
    suspend fun awaitIconElapsed(): Long?

    /** 앱이 스플래시와 같은 장면을 그렸으니 스플래시를 걷습니다. 여러 번 불러도 됩니다. */
    fun takeOver()

    object None : SplashHandoff {
        override suspend fun awaitIconElapsed(): Long? = null
        override fun takeOver() = Unit
    }
}
