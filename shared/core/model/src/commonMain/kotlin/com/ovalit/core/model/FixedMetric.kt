package com.ovalit.core.model

/**
 * 홈 위쪽에 항상 두는 지표입니다. 선언 순서가 화면 순서라 바꾸면 안 됩니다. 앞의 넷은 한 줄 칸에, [KDA]는 그 밑 넓은
 * 칸에 둡니다.
 *
 * 사용자 결정(2026-09-29): 13.06 패치로 어시스트가 킬만큼 점수에 들어가 KDA를 고정 칸처럼 크게 둔다. 킬과 어시를 더한 값이라
 * "평점"이라 부르지 않는다(CLAUDE.md 지켜야 할 선).
 */
enum class FixedMetric(val value: (MatchMetrics) -> Double?) {
    COMBAT_SCORE({ it.acs }),
    KD({ it.kd }),
    DAMAGE({ it.adr }),
    HEADSHOT_RATE({ it.headshotRate }),
    KDA({ it.kda }),
}
