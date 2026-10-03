package com.ovalit.core.model

/**
 * 홈 위쪽에 항상 두는 지표입니다. 선언 순서가 화면 순서라 바꾸면 안 됩니다. 한 줄에 세 칸씩 두 줄로 놓습니다. 순서는 목업의
 * 전투점수, K/D, 피해량, 헤드샷에서 사용자 요청으로 바꿨습니다(2026-10-03).
 *
 * 사용자 결정(2026-09-29): 13.06 패치로 어시스트가 킬만큼 점수에 들어가 KDA를 고정 칸처럼 크게 둔다. 킬과 어시를 더한 값이라
 * "평점"이라 부르지 않는다(CLAUDE.md 지켜야 할 선).
 */
enum class FixedMetric(val value: (MatchMetrics) -> Double?) {
    DAMAGE({ it.adr }),
    KD({ it.kd }),
    KDA({ it.kda }),
    COMBAT_SCORE({ it.acs }),
    HEADSHOT_RATE({ it.headshotRate }),
}
