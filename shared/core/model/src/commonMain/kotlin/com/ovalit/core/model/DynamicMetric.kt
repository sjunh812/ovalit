package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.sqrt

const val DYNAMIC_SLOTS = 3
const val VOLATILITY_WEEKS = 8
const val MIN_VOLATILITY_WEEKS = 4
const val MOVEMENT_THRESHOLD = 1.5

/**
 * 홈의 동적 칸에 오를 수 있는 지표입니다. 고정 4개(전투점수, K/D, 피해량, 헤드샷)는 여기 없습니다.
 *
 * 이번 기간, 비교 기준, 변동폭을 재는 각 주가 모두 최소 표본을 넘겨야 판단합니다.
 */
enum class DynamicMetric(
    val value: (MatchMetrics) -> Double?,
    private val sample: (MatchMetrics) -> Int,
    private val minSample: Int,
) {
    KAST({ it.kast }, { it.rounds }, 40),
    SURVIVAL_RATE({ it.survivalRate }, { it.rounds }, 40),
    FIRST_KILL_WIN_RATE({ it.firstKillWinRate }, { it.firstKills }, 10),
    FIRST_DUEL_INVOLVEMENT({ it.firstDuelInvolvement }, { it.rounds }, 40),
    FIRST_DUEL_WIN_RATE({ it.firstDuelWinRate }, { it.firstKills + it.firstDeaths }, 15),
    ASSISTS_PER_ROUND({ it.assistsPerRound }, { it.rounds }, 40),
    ECO_WIN_RATE({ it.ecoWinRate }, { it.ecoRounds }, 15),
    FORCE_BUY_WIN_RATE({ it.forceBuyWinRate }, { it.forceBuyRounds }, 15),
    FULL_BUY_WIN_RATE({ it.fullBuyWinRate }, { it.fullBuyRounds }, 40),
    ;

    internal fun isMeasurable(metrics: MatchMetrics) = sample(metrics) >= minSample
}

enum class Movement {
    MOVED,
    STEADY,

    /** 표본이나 지난 기록이 모자라 판단하지 못했습니다. "큰 변화 없음"으로 띄우면 안 됩니다. */
    UNKNOWN,
}

data class DynamicSlot(
    val metric: DynamicMetric,
    val movement: Movement,
)

private val Defaults = listOf(
    DynamicMetric.KAST,
    DynamicMetric.SURVIVAL_RATE,
    DynamicMetric.FIRST_KILL_WIN_RATE,
)

/**
 * 움직인 지표를 역할의 우선 지표, 관심사 지표, 많이 움직인 순으로 놓습니다. 남은 칸은 관심사 지표와
 * 기본 지표로 채웁니다. 역할이 크게 띄우지 않는 지표는 어느 쪽에도 넣지 않습니다.
 *
 * @param history 집계 기간 앞 주들의 주간 지표입니다. 평소 변동폭을 여기서 잽니다.
 */
internal fun selectDynamicMetrics(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
    role: Role?,
    focus: Focus = Focus.NONE,
): List<DynamicSlot> {
    val muted = role?.muted.orEmpty()
    val priority = role?.priority.orEmpty()
    fun List<DynamicMetric>.rank(metric: DynamicMetric) = indexOf(metric).takeIf { it >= 0 } ?: size
    val assessed = DynamicMetric.entries
        .filterNot { it in muted }
        .associateWith { it.assess(current, baseline, history) }

    val moved = assessed
        .filterValues { it.movement == Movement.MOVED }
        .keys
        .sortedWith(
            compareBy<DynamicMetric> { priority.rank(it) }
                .thenBy { focus.metrics.rank(it) }
                .thenByDescending { assessed.getValue(it).strength },
        )
        .take(DYNAMIC_SLOTS)
    val fillers = (focus.metrics + Defaults + DynamicMetric.entries)
        .distinct()
        .filter { it in assessed && it !in moved }
        .take(DYNAMIC_SLOTS - moved.size)

    return (moved + fillers).map { DynamicSlot(it, assessed.getValue(it).movement) }
}

internal class Assessment(val movement: Movement, val strength: Double = 0.0)

internal fun DynamicMetric.assess(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
): Assessment {
    val unknown = Assessment(Movement.UNKNOWN)
    if (baseline == null || !isMeasurable(current) || !isMeasurable(baseline)) return unknown
    val now = value(current) ?: return unknown
    val usual = value(baseline) ?: return unknown

    return assessMovement(now, usual, weekly = history.filter(::isMeasurable).mapNotNull(value))
}

/**
 * 이번 기간 값이 평소 주간 변동폭보다 크게 움직였는지 봅니다. [weekly]에는 표본을 넘긴 주의
 * 값만 넣습니다. 그런 주가 [MIN_VOLATILITY_WEEKS]주가 안 되면 판단하지 않습니다.
 */
internal fun assessMovement(now: Double, usual: Double, weekly: List<Double>): Assessment {
    if (weekly.size < MIN_VOLATILITY_WEEKS) return Assessment(Movement.UNKNOWN)

    val change = abs(now - usual)
    val volatility = weekly.sampleStandardDeviation()
    val movement = if (change > MOVEMENT_THRESHOLD * volatility) Movement.MOVED else Movement.STEADY
    return Assessment(movement, strength = change / volatility)
}

// 평균의 평균을 막는 규칙과 다른 얘기다. 주마다 얼마나 흔들리는지를 보는 값이라 주마다 같은 무게를 준다.
private fun List<Double>.sampleStandardDeviation(): Double {
    val mean = average()
    return sqrt(sumOf { (it - mean) * (it - mean) } / (size - 1))
}

// CLAUDE.md 역할군 표에서 지금 계산할 수 있는 것만 옮겼다. K/D는 고정 칸이라 빠진다.
// 스킬 활용, 공수별 성적, 클러치, 사이트 수비는 데이터가 생기면 여기 넣는다.
private val Role.priority: List<DynamicMetric>
    get() = when (this) {
        Role.DUELIST -> listOf(DynamicMetric.FIRST_DUEL_INVOLVEMENT, DynamicMetric.FIRST_DUEL_WIN_RATE)
        Role.INITIATOR -> listOf(DynamicMetric.ASSISTS_PER_ROUND, DynamicMetric.KAST)
        Role.CONTROLLER -> listOf(DynamicMetric.KAST, DynamicMetric.SURVIVAL_RATE)
        Role.SENTINEL -> listOf(DynamicMetric.SURVIVAL_RATE)
    }

private val Role.muted: Set<DynamicMetric>
    get() = when (this) {
        Role.DUELIST -> setOf(DynamicMetric.ASSISTS_PER_ROUND)
        Role.INITIATOR -> emptySet()
        Role.CONTROLLER, Role.SENTINEL -> FirstBloodMetrics
    }

private val FirstBloodMetrics = setOf(
    DynamicMetric.FIRST_KILL_WIN_RATE,
    DynamicMetric.FIRST_DUEL_INVOLVEMENT,
    DynamicMetric.FIRST_DUEL_WIN_RATE,
)
