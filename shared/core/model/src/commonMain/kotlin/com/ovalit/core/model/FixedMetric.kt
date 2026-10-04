package com.ovalit.core.model

/**
 * 홈 위쪽에 늘 두는 지표입니다. 선언 순서가 화면 순서라 바꾸면 안 됩니다.
 *
 * KDA는 킬과 어시를 더한 값이라 "평점"이라 부르지 않습니다(CLAUDE.md 지켜야 할 선).
 */
enum class FixedMetric(val value: (MatchMetrics) -> Double?) {
    DAMAGE({ it.adr }),
    KD({ it.kd }),
    KDA({ it.kda }),
    COMBAT_SCORE({ it.acs }),
    HEADSHOT_RATE({ it.headshotRate }),
}
