package com.ovalit.core.model

/**
 * 홈 "이번 주 짚을 점"입니다. 게임 결정을 대신하지 않고 움직인 숫자만 짚습니다. Riot 정책은 결정을 없애지 말고, 중요한
 * 결정을 짚되 선택지를 여럿 주라고 합니다("may highlight decisions that are important and give multiple choices").
 * 그래서 "밴달을 쓰세요"나 "추천"이라고 하나를 골라 주지 않고, 요원은 둘 이상일 때만 적습니다.
 *
 * @property moved 평소 주간 변동폭보다 크게 움직인 고정 지표 중 가장 크게 움직인 것입니다.
 * @property weapon [moved]와 같은 지표가 같은 쪽으로 평소보다 크게 움직인 무기 중 가장 크게 움직인 것입니다.
 * @property agents 이번 기간에 이긴 판이 진 판보다 많았던 요원입니다. 승률 높은 순입니다.
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
 * 무기 하나의 이번 기간, 비교 기준, 주간 성적입니다. [WeaponStats.value]가 표본을 넘긴 값만 주므로 모자란 주는 빠집니다.
 */
internal class WeaponTrend(
    val weapon: WeaponId,
    val current: WeaponStats?,
    val baseline: WeaponStats?,
    val weekly: List<WeaponStats>,
)

/** 짚을 요원이 되려면 이만큼은 승패가 갈린 판이 있어야 합니다. 한 판 이긴 걸로 "잘 풀렸다"고 하면 과장입니다. */
internal const val MIN_NOTE_AGENT_DECIDED = 2

/** 요원을 이보다 많이 적으면 짚는 게 아니라 목록이 됩니다. */
internal const val MAX_NOTE_AGENTS = 3

/**
 * 고정 지표 중 평소 주간 변동폭의 [MOVEMENT_THRESHOLD]배를 넘게 움직인 것 하나, 같은 지표가 같은 쪽으로 가장 크게
 * 움직인 무기, 이긴 판이 더 많았던 요원 둘 이상을 고릅니다. 하나도 없으면 `null`이라 칸을 두지 않습니다.
 *
 * 역할이 크게 띄우지 않는 지표(척후대와 전략가의 K/D)는 고르지 않습니다. 동적 칸과 같은 규칙입니다.
 *
 * @param history 기간 앞 주들의 주간 지표입니다. 라운드가 [MIN_TREND_ROUNDS]에 못 미치는 주는 변동폭에서 뺍니다.
 */
internal fun chooseWeekNote(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
    role: Role?,
    fixedMetrics: List<FixedMetric>,
    weapons: List<WeaponTrend>,
    agents: List<AgentStats>,
): WeekNote? {
    val moved = movedFixedMetric(current, baseline, history, role, fixedMetrics)
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
    fixedMetrics: List<FixedMetric>,
): MovedMetric? {
    if (baseline == null || current.rounds < MIN_TREND_ROUNDS || baseline.rounds < MIN_TREND_ROUNDS) return null
    val weeks = history.filter { it.rounds >= MIN_TREND_ROUNDS }
    val muted = role?.mutedFixedMetrics.orEmpty()

    return fixedMetrics
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
