package com.ovalit.core.data

import com.ovalit.core.model.nextWeekStart
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone

/**
 * 지금 한 번 흐르고, 그 뒤로는 [timeZone] 기준 월요일 0시마다 흐릅니다.
 * 화면을 켜 둔 채 주가 바뀌면 새 경기가 없어도 "이번 주"를 다시 잡아야 합니다.
 */
fun weekStarts(clock: Clock, timeZone: TimeZone): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        val now = clock.now()
        delay(now.nextWeekStart(timeZone) - now)
    }
}

/**
 * 지금 한 번 흐르고, 그 뒤로는 분이 바뀔 때마다 흐릅니다.
 * "31분 뒤", "N분 전", 자정을 넘긴 "내일"처럼 화면을 켜 둔 동안 시간을 따라 바뀌는 글자가 씁니다.
 * 목록이 바뀔 때만 지금을 정하면 화면을 켜 둔 동안 글자가 멈춥니다.
 *
 * ViewModel은 이 흐름을 넘겨받고 기본값은 한 번만 흐르게 둡니다.
 * 끝없이 도는 흐름을 기본으로 두면 테스트가 시간을 다 넘기려다 끝나지 않습니다.
 */
fun minuteStarts(clock: Clock): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        val millis = clock.now().toEpochMilliseconds()
        delay((MINUTE_MILLIS - millis.mod(MINUTE_MILLIS)).milliseconds)
    }
}

private const val MINUTE_MILLIS = 60_000L
