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
 * 비율 지표는 화면에 보이는 %끼리 [MIN_PERCENT_GAP]%p 이상, 피해량은 기간 평균의 [MIN_DAMAGE_GAP_RATIO] 이상
 * 벌어져야 문장을 만듭니다. 공격과 수비가 각각 최소 표본도 넘겨야 합니다. 기준과 표본 모두 실데이터를 보고 조정할
 * 시작값입니다.
 *
 * @property dynamic 같은 지표의 동적 칸입니다. 값과 최소 표본을 여기서 가져와 동적 칸과 갈리지 않게 합니다. 피해량은
 * 동적 칸 후보가 아니라 `null`이고 [DAMAGE_MIN_ROUNDS]를 씁니다.
 * @property focusOnly 관심사로 골랐을 때만 후보에 넣습니다. 이코·포스바이·풀바이 승률은 라운드 운영 다듬기를 고른
 * 사람에게만 봅니다.
 */
enum class SideMetric(
    internal val dynamic: DynamicMetric?,
    internal val focusOnly: Boolean = false,
) {
    SURVIVAL_RATE(DynamicMetric.SURVIVAL_RATE),
    KAST(DynamicMetric.KAST),
    FIRST_DUEL_WIN_RATE(DynamicMetric.FIRST_DUEL_WIN_RATE),
    DAMAGE(dynamic = null),
    FORCE_BUY_WIN_RATE(DynamicMetric.FORCE_BUY_WIN_RATE, focusOnly = true),
    ECO_WIN_RATE(DynamicMetric.ECO_WIN_RATE, focusOnly = true),
    FULL_BUY_WIN_RATE(DynamicMetric.FULL_BUY_WIN_RATE, focusOnly = true),
    ;

    fun value(metrics: MatchMetrics): Double? = if (dynamic != null) dynamic.value(metrics) else metrics.adr

    internal fun isMeasurable(metrics: MatchMetrics): Boolean =
        dynamic?.isMeasurable(metrics) ?: (metrics.rounds >= DAMAGE_MIN_ROUNDS)
}

/** 개선 포인트의 피해량이 넘겨야 하는 진영별 라운드입니다. 동적 칸의 라운드 기준과 같습니다. */
const val DAMAGE_MIN_ROUNDS = 40

const val MIN_PERCENT_GAP = 10
const val MIN_DAMAGE_GAP_RATIO = 0.15

/**
 * @property isRolePriority 고른 지표가 역할의 우선 지표인지입니다. 화면은 이때 "전략가에게 생존율은
 * 먼저 보는 지표예요"를 붙입니다.
 * @property focus 관심사 지표라서 골랐으면 그 관심사입니다. 화면은 이때 "에임 올리기를 고르셔서 먼저 봤어요"를 붙입니다.
 */
data class SideInsight(
    val metric: SideMetric,
    val attack: MatchMetrics,
    val defense: MatchMetrics,
    val isRolePriority: Boolean,
    val focus: Focus? = null,
)

/**
 * 기간 경기를 공격과 수비로 나눠서 가장 크게 벌어진 지표 하나를 고릅니다.
 *
 * 관심사 지표 가운데 기준을 넘는 게 있으면 그중 가장 벌어진 것을 먼저 고릅니다. 사용자가 보겠다고 고른 지표라
 * 역할이 크게 띄우지 않는 지표여도 봅니다. 없으면 역할의 우선 지표, 그것도 기준에 못 미치면 나머지 가운데 기준보다
 * 가장 많이 벌어진 걸 고릅니다. 기준을 넘는 지표가 없으면 `null`입니다.
 */
internal fun List<Match>.sideInsight(role: Role?, focus: Focus = Focus.NONE): SideInsight? {
    val attack = map { it.metrics(Side.ATTACK) }.sum()
    val defense = map { it.metrics(Side.DEFENSE) }.sum()
    val overall = map { it.metrics() }.sum()
    fun List<SideMetric>.gaps() = mapNotNull { metric -> metric.gapRatio(attack, defense, overall)?.let { metric to it } }

    focus.sideMetrics.gaps().maxByOrNull { it.second }?.let { (metric, _) ->
        return SideInsight(metric, attack, defense, isRolePriority = false, focus = focus)
    }

    val gaps = SideMetric.entries
        .filter { !it.focusOnly && (role == null || it !in role.mutedSideMetrics) }
        .gaps()
    val priority = role?.prioritySideMetric
    val chosen = gaps.firstOrNull { it.first == priority }?.first
        ?: gaps.maxByOrNull { it.second }?.first
        ?: return null

    return SideInsight(chosen, attack, defense, isRolePriority = chosen == priority)
}

/** 격차를 기준으로 나눈 값입니다. 1보다 작거나 표본이 모자라면 `null`입니다. */
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

// 관심사 지표 중 공수로 나눠 셀 수 있는 것이다. 관심사 목록에서 바로 뽑아 동적 칸과 갈리지 않게 한다.
private val Focus.sideMetrics: List<SideMetric>
    get() = metrics.mapNotNull { metric -> SideMetric.entries.firstOrNull { it.dynamic == metric } }

// CLAUDE.md 역할군 표의 우선 지표 가운데 공수로 나눠 셀 수 있는 것
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
