package com.ovalit.core.testing

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * ViewModel의 `computation`에 넘깁니다. 앱은 `Dispatchers.Default`에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서
 * 셉니다.
 */
val SameThread: CoroutineContext = EmptyCoroutineContext
