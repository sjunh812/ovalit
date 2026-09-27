package com.ovalit.feature.report

import com.ovalit.core.model.nextWeekStart
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone

/**
 * 지금 한 번 흐르고, 그 뒤로는 [timeZone] 기준 월요일 0시마다 흐릅니다. 홈을 켜 둔 채 주가 바뀌면 새 경기가 없어도
 * "이번 주"를 다시 잡아야 합니다.
 */
fun weekStarts(clock: Clock, timeZone: TimeZone): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        val now = clock.now()
        delay(now.nextWeekStart(timeZone) - now)
    }
}
