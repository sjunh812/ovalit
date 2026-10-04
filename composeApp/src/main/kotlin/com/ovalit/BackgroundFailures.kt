package com.ovalit

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * 화면 밖에서 도는 일(푸시 토큰 등록, 알림을 받고 새로 받기)이 실패해도 앱을 죽이지 않고 기록만 남깁니다. 다음에 다시 하면
 * 되는 일들이라 사용자에게 알리지 않습니다.
 */
internal val LogBackgroundFailure = CoroutineExceptionHandler { _, error -> Log.w("Ovalit", "background work failed", error) }
