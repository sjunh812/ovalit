package com.ovalit.core.model

/**
 * 홈 "이번 주 짚을 점"입니다. 움직인 숫자만 짚고 무엇을 쓰라고 하지 않습니다(CLAUDE.md 지켜야 할 선). 요원은 둘
 * 이상일 때만 적습니다.
 *
 * @property moved 평소 주간 변동폭보다 크게 움직인 고정 지표 중 가장 크게 움직인 것입니다. 없으면 `null`입니다.
 * @property weapon [moved]와 같은 지표가 같은 쪽으로 평소보다 크게 움직인 무기 중 가장 크게 움직인 것입니다. [moved]가
 * 없거나 맞는 무기가 없으면 `null`입니다.
 * @property agents 이번 기간에 이긴 판이 진 판보다 많았던 요원입니다. 승률 높은 순이고, 둘이 안 되면 빈 목록입니다.
 */
data class WeekNote(
    val moved: MovedMetric?,
    val weapon: MovedWeapon?,
    val agents: List<AgentStats>,
)

data class MovedMetric(val metric: FixedMetric, val current: Double, val usual: Double) {
    val rose: Boolean get() = current > usual
}

data class MovedWeapon(val weapon: WeaponId, val metric: WeaponMetric, val current: Double, val usual: Double)

/**
 * 무기 하나의 이번 기간, 비교 기준, 주간 성적입니다. [WeaponStats.value]가 표본을 넘긴 값만 주므로 모자란 주는
 * 변동폭에서 빠집니다.
 */
internal class WeaponTrend(
    val weapon: WeaponId,
    val current: WeaponStats?,
    val baseline: WeaponStats?,
    val weekly: List<WeaponStats>,
)

/** 짚을 요원이 되려면 이만큼은 승패가 갈린 판이 있어야 합니다. 한 판 이긴 걸로 "잘 풀렸다"고 하면 과장입니다. */
internal const val MIN_NOTE_AGENT_DECIDED = 2

internal const val MAX_NOTE_AGENTS = 3

/**
 * 고정 지표 중 평소 주간 변동폭의 [MOVEMENT_THRESHOLD]배를 넘게 움직인 것 하나, 같은 지표가 같은 쪽으로 가장 크게
 * 움직인 무기, 이긴 판이 더 많았던 요원 둘 이상을 고릅니다. 셋 다 없으면 `null`이라 칸을 두지 않습니다.
 *
 * 역할이 크게 띄우지 않는 지표(척후대와 전략가의 K/D)는 고르지 않습니다. 동적 칸과 같은 규칙입니다. 기타 모드에서는
 * 부르지 않아서 고정 지표 넷을 다 봅니다.
 *
 * @param history 기간 앞 주들의 주간 지표입니다. S1-a 막대처럼 라운드가 [MIN_TREND_ROUNDS]에 못 미치는 주는 변동폭에서
 * 뺍니다.
 */
internal fun chooseWeekNote(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
    role: Role?,
    weapons: List<WeaponTrend>,
    agents: List<AgentStats>,
): WeekNote? {
    val moved = movedFixedMetric(current, baseline, history, role)
    val weapon = moved?.let { movedWeapon(it, weapons) }
    val wellPlayed = agents
        .filter { it.decided >= MIN_NOTE_AGENT_DECIDED && it.wins * 2 > it.decided }
        .sortedWith(compareByDescending<AgentStats> { it.winRate }.thenByDescending { it.decided })
        .take(MAX_NOTE_AGENTS)
        // 하나뿐이면 그 요원을 골라 주는 것처럼 읽힌다
        .takeIf { it.size >= 2 }
        .orEmpty()

    if (moved == null && wellPlayed.isEmpty()) return null
    return WeekNote(moved = moved, weapon = weapon, agents = wellPlayed)
}

private fun movedFixedMetric(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
    role: Role?,
): MovedMetric? {
    if (baseline == null || current.rounds < MIN_TREND_ROUNDS || baseline.rounds < MIN_TREND_ROUNDS) return null
    val weeks = history.filter { it.rounds >= MIN_TREND_ROUNDS }
    val muted = role?.mutedFixedMetrics.orEmpty()

    return FixedMetric.entries
        .filter { it !in muted }
        .mapNotNull { metric ->
            val now = metric.value(current) ?: return@mapNotNull null
            val usual = metric.value(baseline) ?: return@mapNotNull null
            val assessment = assessMovement(now, usual, weekly = weeks.mapNotNull(metric.value))
            if (assessment.movement != Movement.MOVED) return@mapNotNull null
            MovedMetric(metric, now, usual) to assessment.strength
        }
        .maxByOrNull { it.second }
        ?.first
}

private fun movedWeapon(moved: MovedMetric, weapons: List<WeaponTrend>): MovedWeapon? {
    // 전투점수는 무기별로 나눌 수 없다
    val metric = moved.metric.weaponMetric ?: return null

    return weapons
        // S6 위쪽 표처럼 헤드샷과 K/D·피해량 표본을 둘 다 넘긴 무기만 본다. 한쪽만 넘긴 무기를 짚으면 S6에서는 그 무기가
        // "이번 액트 기준"으로 떠서 숫자가 맞지 않는다.
        .filter { it.current?.isMeasurable == true && it.current.isCarriedMeasurable }
        .mapNotNull { trend ->
            val now = trend.current?.value(metric) ?: return@mapNotNull null
            val usual = trend.baseline?.value(metric) ?: return@mapNotNull null
            if ((now > usual) != moved.rose || now == usual) return@mapNotNull null
            val assessment = assessMovement(now, usual, weekly = trend.weekly.mapNotNull { it.value(metric) })
            if (assessment.movement != Movement.MOVED) return@mapNotNull null
            MovedWeapon(trend.weapon, metric, now, usual) to assessment.strength
        }
        .maxByOrNull { it.second }
        ?.first
}

private val FixedMetric.weaponMetric: WeaponMetric?
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> null
        FixedMetric.KD -> WeaponMetric.KD
        FixedMetric.DAMAGE -> WeaponMetric.DAMAGE_PER_ROUND
        FixedMetric.HEADSHOT_RATE -> WeaponMetric.HEADSHOT_RATE
    }

// CLAUDE.md 역할군 표의 "크게 띄우지 않는 것" 중 고정 지표에 해당하는 것만 옮겼다
private val Role.mutedFixedMetrics: Set<FixedMetric>
    get() = when (this) {
        Role.INITIATOR, Role.CONTROLLER -> setOf(FixedMetric.KD)
        Role.DUELIST, Role.SENTINEL -> emptySet()
    }
