package com.ovalit.core.testing

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

val Seoul: TimeZone = TimeZone.of("Asia/Seoul")

/**
 * 2026-09-24(목) 22:00, 서울입니다. 가짜 경기([com.ovalit.core.data.FakeMatchRepository])는 넘겨받은 시각부터 거슬러
 * 올라가며 만들어서, 리포트 숫자를 맞춰 보는 테스트는 시계를 이 시각에 멈춰 둡니다.
 */
val Thursday: Instant = LocalDateTime(2026, 9, 24, 22, 0).toInstant(Seoul)

/** [Thursday]에 멈춘 시계입니다. */
val ThursdayClock: Clock = object : Clock {
    override fun now(): Instant = Thursday
}

/** 테스트가 직접 넘기는 시계입니다. 가상 시간과 따로 움직여서, 떠났다 온 시간을 한 번에 건너뜁니다. */
class StepClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}
