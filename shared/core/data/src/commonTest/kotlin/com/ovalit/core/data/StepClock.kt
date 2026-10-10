package com.ovalit.core.data

import kotlin.time.Clock
import kotlin.time.Instant

/** 테스트가 직접 넘기는 시계입니다. 가상 시간과 따로 움직여서, 떠났다 온 시간을 한 번에 건너뜁니다. */
internal class StepClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}
