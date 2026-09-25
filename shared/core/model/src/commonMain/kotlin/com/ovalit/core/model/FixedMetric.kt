package com.ovalit.core.model

/** 홈 위쪽에 항상 두는 4개입니다. 선언 순서가 화면 순서라 바꾸면 안 됩니다. */
enum class FixedMetric(val value: (MatchMetrics) -> Double?) {
    COMBAT_SCORE({ it.acs }),
    KD({ it.kd }),
    DAMAGE({ it.adr }),
    HEADSHOT_RATE({ it.headshotRate }),
}
