package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.roundToInt

enum class Side {
    ATTACK,
    DEFENSE,
}

/**
 * 개선 포인트 문장에서 공격과 수비로 나눠 보는 지표입니다.
 *
 * 비율 지표는 화면에 보이는 %끼리 [MIN_PERCENT_GAP]%p 이상, 피해량은 기간 평균의
 * [MIN_DAMAGE_GAP_RATIO]만큼 벌어져야 문장을 만듭니다. 공격과 수비가 각각 최소 표본을 넘겨야 합니다.
 * 셋 다 시작 기준선이고 실데이터를 보고 조정합니다.
 */
enum class SideMetric(
    val value: (MatchMetrics) -> Double?,
    private val sample: (MatchMetrics) -> Int,
    private val minSample: Int,
) {
    SURVIVAL_RATE({ it.survivalRate }, { it.rounds }, 40),
    KAST({ it.kast }, { it.rounds }, 40),
    FIRST_DUEL_WIN_RATE({ it.firstDuelWinRate }, { it.firstKills + it.firstDeaths }, 15),
    DAMAGE({ it.adr }, { it.rounds }, 40),
    ;

    internal fun isMeasurable(metrics: MatchMetrics) = sample(metrics) >= minSample
}

const val MIN_PERCENT_GAP = 10
const val MIN_DAMAGE_GAP_RATIO = 0.15

/**
 * @property isRolePriority 고른 지표가 역할의 우선 지표입니다. 화면에는 그때만 "전략가에게 생존율은
 * 먼저 보는 지표예요"를 붙입니다.
 */
data class SideInsight(
    val metric: SideMetric,
    val attack: MatchMetrics,
    val defense: MatchMetrics,
    val isRolePriority: Boolean,
)

/**
 * 기간 경기를 공격과 수비로 나눠 가장 벌어진 지표 하나를 고릅니다. 역할의 우선 지표가 기준을 넘으면
 * 그것부터, 아니면 나머지 중 기준 대비 가장 많이 벌어진 것입니다. 역할에서 크게 띄우지 않는 지표는
 * 뺍니다. 넘는 게 없으면 없습니다.
 */
internal fun List<Match>.sideInsight(role: Role?): SideInsight? {
    val attack = map { it.metrics(Side.ATTACK) }.sum()
    val defense = map { it.metrics(Side.DEFENSE) }.sum()
    val overall = map { it.metrics() }.sum()

    val gaps = SideMetric.entries
        .filter { role == null || it !in role.mutedSideMetrics }
        .mapNotNull { metric -> metric.gapRatio(attack, defense, overall)?.let { metric to it } }
    val priority = role?.prioritySideMetric
    val chosen = gaps.firstOrNull { it.first == priority }?.first
        ?: gaps.maxByOrNull { it.second }?.first
        ?: return null

    return SideInsight(chosen, attack, defense, isRolePriority = chosen == priority)
}

/** 기준 대비 얼마나 벌어졌는지입니다. 1 미만이거나 표본이 모자라면 없습니다. */
private fun SideMetric.gapRatio(attack: MatchMetrics, defense: MatchMetrics, overall: MatchMetrics): Double? {
    if (!isMeasurable(attack) || !isMeasurable(defense)) return null
    val a = value(attack) ?: return null
    val d = value(defense) ?: return null

    // 화면에 보이는 자릿수로 반올림한 값끼리 뺀다. 48%와 35%를 띄워 놓고 14%p라고 쓰면 틀려 보인다.
    val ratio = if (this == SideMetric.DAMAGE) {
        val usual = overall.adr?.takeIf { it > 0 } ?: return null
        abs(a.roundToInt() - d.roundToInt()) / (usual * MIN_DAMAGE_GAP_RATIO)
    } else {
        abs((a * 100).roundToInt() - (d * 100).roundToInt()).toDouble() / MIN_PERCENT_GAP
    }
    return ratio.takeIf { it >= 1.0 }
}

// CLAUDE.md 역할군 표의 우선 지표 중 공수로 나눠 볼 만한 것
private val Role.prioritySideMetric: SideMetric
    get() = when (this) {
        Role.DUELIST -> SideMetric.FIRST_DUEL_WIN_RATE
        Role.INITIATOR -> SideMetric.KAST
        Role.CONTROLLER, Role.SENTINEL -> SideMetric.SURVIVAL_RATE
    }

// 전략가와 감시자는 퍼블을 크게 띄우지 않는다
private val Role.mutedSideMetrics: Set<SideMetric>
    get() = when (this) {
        Role.CONTROLLER, Role.SENTINEL -> setOf(SideMetric.FIRST_DUEL_WIN_RATE)
        Role.DUELIST, Role.INITIATOR -> emptySet()
    }
