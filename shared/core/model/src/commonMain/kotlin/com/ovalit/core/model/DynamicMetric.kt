package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.sqrt

/** 동적 칸은 적어도 이만큼 둡니다. 관심사 지표와 움직인 지표가 이보다 적으면 기본 지표로 채웁니다. */
const val MIN_DYNAMIC_SLOTS = 3

/** 관심사 지표와 움직인 지표가 많아도 이만큼까지만 둡니다. */
const val MAX_DYNAMIC_SLOTS = 5
const val VOLATILITY_WEEKS = 8
const val MIN_VOLATILITY_WEEKS = 4
const val MOVEMENT_THRESHOLD = 1.5
private const val FLAT_VOLATILITY = 1e-9

/**
 * 홈의 동적 칸에 오를 수 있는 지표입니다. 고정 4개(전투점수, K/D, 피해량, 헤드샷)는 여기 없습니다.
 */
enum class DynamicMetric(
    val value: (MatchMetrics) -> Double?,
    private val sample: (MatchMetrics) -> Int,
    /** 이번 기간, 비교 기준, 변동폭을 재는 각 주가 넘겨야 하는 표본입니다. 표본은 그 비율의 분모입니다. */
    val minSample: Int,
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

    // 사용자 결정(2026-09-27): 에임 올리기에 첫 교전 다음 교전까지 이겨 내는지를 더했다. 기본 칸을 채우는 순서가
    // 바뀌지 않게 맨 뒤에 둔다.
    MULTI_KILL_RATE({ it.multiKillRate }, { it.rounds }, 40),

    // 코치들이 가장 많이 보는 지표라 넣었다(2026-09-27). 역할마다 우선 지표나 크게 띄우지 않는 것에는 두지 않는다. 동적 칸은
    // 내 지난 기록과 견주니 역할마다 원래 높고 낮은 건 상관없다. 기본 칸을 채우는 순서가 바뀌지 않게 맨 뒤에 둔다.
    TRADED_DEATH_RATE({ it.tradedDeathRate }, { it.deaths }, 40),
    ;

    internal fun isMeasurable(metrics: MatchMetrics) = sample(metrics) >= minSample
}

enum class Movement {
    MOVED,
    STEADY,

    /** 표본이나 지난 기록이 모자라거나 주마다 값이 같아 판단하지 못했습니다. "큰 변화 없음"으로 띄우면 안 됩니다. */
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
 * 관심사 지표를 관심사에 적힌 순서대로 맨 앞에 늘 둡니다. 움직이지 않았어도, 역할이 크게 띄우지 않는 지표여도
 * 넣습니다. 그 뒤에 움직인 지표를 역할의 우선 지표, 많이 움직인 순으로 놓고 [MAX_DYNAMIC_SLOTS]개에서 자릅니다.
 * [MIN_DYNAMIC_SLOTS]개가 안 되면 기본 지표부터 채웁니다. 관심사가 아니면 역할이 크게 띄우지 않는 지표는 움직였어도,
 * 빈칸을 채울 때도 넣지 않습니다.
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
        .filter { it in focus.metrics || it !in muted }
        .associateWith { it.assess(current, baseline, history) }

    val moved = assessed
        .filter { (metric, assessment) -> assessment.movement == Movement.MOVED && metric !in focus.metrics }
        .keys
        .sortedWith(
            compareBy<DynamicMetric> { priority.rank(it) }
                .thenByDescending { assessed.getValue(it).strength },
        )
    val chosen = (focus.metrics + moved).take(MAX_DYNAMIC_SLOTS)
    val fillers = (Defaults + DynamicMetric.entries)
        .distinct()
        .filter { it in assessed && it !in chosen }
        .take((MIN_DYNAMIC_SLOTS - chosen.size).coerceAtLeast(0))

    return (chosen + fillers).map { DynamicSlot(it, assessed.getValue(it).movement) }
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
 * 이번 기간 값이 평소 주간 변동폭의 [MOVEMENT_THRESHOLD]배를 넘게 움직였는지 봅니다. [weekly]에는 표본을 넘긴 주의
 * 값만 넣습니다. 그런 주가 [MIN_VOLATILITY_WEEKS]주가 안 되면 [Movement.UNKNOWN]입니다.
 */
internal fun assessMovement(now: Double, usual: Double, weekly: List<Double>): Assessment {
    if (weekly.size < MIN_VOLATILITY_WEEKS) return Assessment(Movement.UNKNOWN)

    val change = abs(now - usual)
    val volatility = weekly.sampleStandardDeviation()
    // 주마다 값이 똑같았으면 변동폭이 0이라 조금만 달라도 무한히 크게 움직인 것이 된다. 기준을 잴 수 없으니 판단하지 않는다.
    // 같은 값을 평균 내도 부동소수 오차로 정확히 0이 나오지 않아서 아주 작은 값과 견준다.
    if (volatility < FLAT_VOLATILITY) return Assessment(Movement.UNKNOWN)
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
        Role.DUELIST -> listOf(DynamicMetric.FIRST_DUEL_INVOLVEMENT, DynamicMetric.FIRST_DUEL_WIN_RATE, DynamicMetric.MULTI_KILL_RATE)
        Role.INITIATOR -> listOf(DynamicMetric.ASSISTS_PER_ROUND, DynamicMetric.KAST)
        Role.CONTROLLER -> listOf(DynamicMetric.KAST, DynamicMetric.SURVIVAL_RATE)
        Role.SENTINEL -> listOf(DynamicMetric.SURVIVAL_RATE)
    }

// 멀티킬은 K/D처럼 킬을 따내는 쪽이라 K/D를 크게 띄우지 않는 척후대와 전략가에게서 뺀다
private val Role.muted: Set<DynamicMetric>
    get() = when (this) {
        Role.DUELIST -> setOf(DynamicMetric.ASSISTS_PER_ROUND)
        Role.INITIATOR -> setOf(DynamicMetric.MULTI_KILL_RATE)
        Role.CONTROLLER -> FirstBloodMetrics + DynamicMetric.MULTI_KILL_RATE
        Role.SENTINEL -> FirstBloodMetrics
    }

private val FirstBloodMetrics = setOf(
    DynamicMetric.FIRST_KILL_WIN_RATE,
    DynamicMetric.FIRST_DUEL_INVOLVEMENT,
    DynamicMetric.FIRST_DUEL_WIN_RATE,
)
