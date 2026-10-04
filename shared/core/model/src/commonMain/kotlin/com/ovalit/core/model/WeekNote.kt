package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 홈 "이번 주 짚을 점"입니다. 움직인 숫자만 짚고 무엇을 쓰라고 하지 않습니다(CLAUDE.md 지켜야 할 선).
 *
 * @property moved 평소 주간 변동폭보다 크게 움직인 고정 지표 중 가장 크게 움직인 것입니다.
 * @property weapon [moved]를 같은 쪽으로 가장 많이 끌어간 무기입니다. 맞는 무기가 없으면 `null`입니다.
 * @property agent [moved]를 같은 쪽으로 가장 많이 끌어간 요원입니다. 맞는 요원이 없으면 `null`입니다.
 * @property mix [moved]의 절반 이상이 구매 유형, 들고 시작한 무기, 요원의 비중이 바뀐 데서 왔으면 그 변화입니다. 이때는
 * [weapon]과 [agent]를 비웁니다. 이코 라운드가 늘어 떨어진 피해량을 무기 하나가 끌어내린 것처럼 적으면 틀린 말이 됩니다.
 * @property previousBest [moved]가 올라 이번 액트 어느 주보다 높으면 그 앞 주들 중 가장 높았던 값이고, 아니면 `null`입니다.
 */
data class WeekNote(
    val moved: MovedMetric,
    val weapon: MovedWeapon? = null,
    val agent: MovedAgent? = null,
    val mix: MixShift? = null,
    val previousBest: Double? = null,
)

/** 비중을 견주는 묶음입니다. 라운드 구매, 들고 시작한 무기, 뛴 요원으로 가릅니다. */
sealed interface MixGroup {
    data class Buy(val type: BuyType) : MixGroup

    data class Weapon(val weapon: WeaponId) : MixGroup

    data class Agent(val agent: AgentId) : MixGroup
}

/**
 * 비중이 바뀐 묶음과, 비중과 상관없이 성적이 어땠는지 보여 줄 묶음입니다.
 *
 * @property share 이번 기간 비중입니다. 요원은 판으로, 나머지는 라운드로 셉니다. [usualShare]는 비교 기준 비중입니다.
 * @property steady 두 기간 모두 가장 많이 한 묶음의 성적입니다. 이코 라운드가 늘었을 때 풀바이 라운드만 보면 평소와
 * 같았는지를 보여줍니다. 표본이 모자라면 `null`입니다.
 */
data class MixShift(
    val group: MixGroup,
    val share: Double,
    val usualShare: Double,
    val steady: SteadyPart?,
)

/** @property current 이번 기간 값, [usual]은 비교 기준 값입니다. 무기는 S6과 같은 표본으로 셉니다. */
data class SteadyPart(val group: MixGroup, val current: Double, val usual: Double)

/** 묶음 하나의 이번 기간과 비교 기준 성적입니다. 그 기간에 없었으면 [MatchMetrics.Empty]입니다. */
internal class MixSlice(val group: MixGroup, val current: MatchMetrics, val usual: MatchMetrics)

/** 변화의 이만큼 이상을 비중 변화가 설명해야 무기·요원 대신 비중을 적습니다. */
internal const val MIN_MIX_EXPLAINED = 0.5

/** 보이는 %끼리 이만큼 이상 바뀐 비중만 짚습니다. "16% → 18%"는 바뀌었다고 적을 만한 차이가 아닙니다. */
internal const val MIN_MIX_SHARE_GAP = 5

/**
 * 비중 차이가 우연히 벌어질 만한 폭의 이 배수를 넘어야 짚습니다. 여덟 판 중 레이즈가 한 판 줄면 비중이 12%p 떨어지지만
 * 우연으로 흔히 나오는 차이입니다. 한 경기 라운드들은 같이 움직여서 흔히 쓰는 2배보다 넉넉히 둡니다.
 */
internal const val MIN_MIX_SHARE_Z = 2.5

data class MovedMetric(val metric: FixedMetric, val current: Double, val usual: Double) {
    val rose: Boolean get() = current > usual
}

/** @property rounds 이번 기간에 이 값을 낸 라운드입니다. 헤드샷은 한 무기만 쓴 라운드, 나머지는 들고 시작한 라운드입니다. */
data class MovedWeapon(val weapon: WeaponId, val metric: WeaponMetric, val current: Double, val usual: Double, val rounds: Int = 0)

data class MovedAgent(val agent: AgentId, val current: Double, val usual: Double, val matches: Int)

/** 무기 하나의 이번 기간과 비교 기준 성적입니다. [WeaponStats.value]가 표본을 넘긴 값만 줍니다. */
internal class WeaponTrend(
    val weapon: WeaponId,
    val current: WeaponStats?,
    val baseline: WeaponStats?,
)

/** 요원 하나의 이번 기간과 비교 기준 성적입니다. */
internal class AgentTrend(
    val agent: AgentId,
    val current: MatchMetrics,
    val baseline: MatchMetrics?,
)

/**
 * 고정 지표 중 평소 주간 변동폭의 [MOVEMENT_THRESHOLD]배를 넘게 움직인 것 하나를 고르고, 그 변화를 가장 많이 끌어간
 * 무기와 요원을 붙입니다. 움직인 게 없으면 `null`이라 칸을 두지 않습니다.
 *
 * 무기와 요원은 크게 달라진 것보다 많이 쓴 것을 봅니다. 이번 기간 라운드에서 차지하는 비중에 평소와의 차이를 곱해,
 * 전체 변화에 가장 많이 보탠 것을 고릅니다. 한 판 쓴 무기가 크게 달라졌어도 전체 숫자는 거의 안 움직입니다.
 *
 * 그 전에 변화가 실력이 아니라 비중에서 왔는지 봅니다. 이번 기간 묶음별 성적을 비교 기준의 비중으로 다시 섞어, 원래 값과의
 * 차이가 변화의 [MIN_MIX_EXPLAINED] 이상이면 무기와 요원 대신 비중 변화를 적습니다(`mixes`).
 *
 * 역할이 크게 띄우지 않는 지표(척후대와 전략가의 K/D)는 고르지 않습니다. 동적 칸과 같은 규칙입니다. KDA는 어시스트가
 * 들어가 척후대와 전략가도 봅니다. 기타 모드에서는 부르지 않습니다.
 *
 * @param mixes 라운드 구매, 들고 시작한 무기, 요원으로 나눈 묶음들입니다. 나누는 방법마다 목록 하나입니다.
 * @param actWeeks 이번 액트에서 기간 앞의 주들입니다. 기간이 한 주일 때만 넘깁니다. 두 주를 합친 값을 한 주 값들과 견주면
 * 주간 최고라고 할 수 없습니다. 라운드가 [MIN_TREND_ROUNDS]에 못 미치는 주는 뺍니다.
 * @param history 기간 앞 주들의 주간 지표입니다. S1-a 평소 범위처럼 라운드가 [MIN_TREND_ROUNDS]에 못 미치는 주는 변동폭에서
 * 뺍니다.
 */
internal fun chooseWeekNote(
    current: MatchMetrics,
    baseline: MatchMetrics?,
    history: List<MatchMetrics>,
    role: Role?,
    weapons: List<WeaponTrend>,
    agentTrends: List<AgentTrend> = emptyList(),
    mixes: List<List<MixSlice>> = emptyList(),
    actWeeks: List<MatchMetrics> = emptyList(),
): WeekNote? {
    val moved = movedFixedMetric(current, baseline, history, role) ?: return null
    val best = previousBest(moved, actWeeks)
    val mix = chooseMix(moved, mixes, weapons)
    if (mix != null) return WeekNote(moved = moved, mix = mix, previousBest = best)
    return WeekNote(
        moved = moved,
        weapon = movedWeapon(moved, weapons, current.rounds),
        agent = movedAgent(moved, agentTrends, current.rounds),
        previousBest = best,
    )
}

// 앞선 주가 몇 주뿐이면 최고라고 부를 만하지 않다. 변동폭을 재는 주 수와 같다.
private fun previousBest(moved: MovedMetric, actWeeks: List<MatchMetrics>): Double? {
    if (!moved.rose) return null
    val weekly = actWeeks.filter { it.rounds >= MIN_TREND_ROUNDS }.mapNotNull(moved.metric.value)
    if (weekly.size < MIN_VOLATILITY_WEEKS) return null
    return weekly.max().takeIf { moved.current > it }
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

// 같은 쪽으로 움직인 무기 중 비중 × 차이가 가장 큰 것이다. S6 위쪽 표가 변화량을 적는 기준과 맞춰, 두 기간 모두 표본을
// 넘긴 무기만 본다.
private fun movedWeapon(moved: MovedMetric, weapons: List<WeaponTrend>, rounds: Int): MovedWeapon? {
    // 전투점수와 KDA는 무기별로 짚을 값이 없다
    val metric = moved.metric.weaponMetric ?: return null
    if (rounds == 0) return null

    return weapons
        .filter { it.current?.isMeasurable == true && it.current.isCarriedMeasurable }
        .mapNotNull { trend ->
            val stats = trend.current ?: return@mapNotNull null
            val now = stats.value(metric) ?: return@mapNotNull null
            val usual = trend.baseline?.value(metric) ?: return@mapNotNull null
            val sample = if (metric == WeaponMetric.HEADSHOT_RATE) stats.singleWeaponRounds else stats.carriedRounds
            val pull = (now - usual) * sample / rounds * if (moved.rose) 1 else -1
            if (pull <= 0) return@mapNotNull null
            MovedWeapon(trend.weapon, metric, now, usual, sample) to pull
        }
        .maxByOrNull { it.second }
        ?.first
}

// 요원도 무기처럼 비중 × 차이로 고른다. 이번 기간과 비교 기준 모두 MIN_TREND_ROUNDS를 넘긴 요원만 본다.
private fun movedAgent(moved: MovedMetric, agents: List<AgentTrend>, rounds: Int): MovedAgent? {
    if (rounds == 0) return null
    return agents
        .filter { it.current.rounds >= MIN_TREND_ROUNDS && (it.baseline?.rounds ?: 0) >= MIN_TREND_ROUNDS }
        .mapNotNull { trend ->
            val now = moved.metric.value(trend.current) ?: return@mapNotNull null
            val usual = trend.baseline?.let(moved.metric.value) ?: return@mapNotNull null
            val pull = (now - usual) * trend.current.rounds / rounds * if (moved.rose) 1 else -1
            if (pull <= 0) return@mapNotNull null
            MovedAgent(trend.agent, now, usual, trend.current.matches) to pull
        }
        .maxByOrNull { it.second }
        ?.first
}

/**
 * 나누는 방법 가운데 비중 변화로 [moved]를 가장 많이 설명하는 것입니다. 변화의 [MIN_MIX_EXPLAINED] 이상을 설명하지 못하면
 * `null`입니다.
 *
 * 이번 기간 묶음별 성적을 그대로 두고 비중만 비교 기준처럼 맞춘 값을 구합니다. 원래 값과 그 값의 차이가 비중이 바뀐 몫입니다.
 * 전투점수는 경기 합계뿐이라 라운드로 가른 묶음에는 0으로 들어가([roundMetrics]) 비중 몫이 없고, 요원으로만 짚힙니다.
 */
private fun chooseMix(moved: MovedMetric, mixes: List<List<MixSlice>>, weapons: List<WeaponTrend>): MixShift? {
    val direction = if (moved.rose) 1.0 else -1.0
    val change = abs(moved.current - moved.usual)
    return mixes
        .mapNotNull { slices ->
            val explained = (mixEffect(moved.metric, slices) ?: return@mapNotNull null) * direction
            if (explained >= MIN_MIX_EXPLAINED * change) mixShift(moved, slices, weapons)?.let { it to explained } else null
        }
        .maxByOrNull { (_, explained) -> explained }
        ?.first
}

// 이번 기간 값에서 비중을 비교 기준처럼 맞춘 값을 뺀다. 비교 기준에 없던 묶음은 비중 0이라 빠진다.
private fun mixEffect(metric: FixedMetric, slices: List<MixSlice>): Double? {
    val common = slices.filter { it.current.rounds > 0 && it.usual.rounds > 0 }
    if (common.size < 2) return null
    val currentRounds = common.sumOf { it.current.rounds }.toDouble()
    val usualRounds = common.sumOf { it.usual.rounds }.toDouble()
    var part = 0.0
    var whole = 0.0
    for (slice in common) {
        val weight = (slice.usual.rounds / usualRounds) / (slice.current.rounds / currentRounds)
        val (slicePart, sliceWhole) = metric.fraction(slice.current)
        part += weight * slicePart
        whole += weight * sliceWhole
    }
    val (nowPart, nowWhole) = slices.map { metric.fraction(it.current) }.reduce { a, b -> a.first + b.first to a.second + b.second }
    if (whole == 0.0 || nowWhole == 0.0) return null
    return nowPart / nowWhole - part / whole
}

private fun mixShift(moved: MovedMetric, slices: List<MixSlice>, weapons: List<WeaponTrend>): MixShift? {
    val metric = moved.metric
    val direction = if (moved.rose) 1.0 else -1.0
    val now = moved.current
    val byMatches = slices.all { it.group is MixGroup.Agent }
    fun MatchMetrics.size() = if (byMatches) matches else rounds
    val currentTotal = slices.sumOf { it.current.size() }.toDouble()
    val usualTotal = slices.sumOf { it.usual.size() }.toDouble()
    if (currentTotal == 0.0 || usualTotal == 0.0) return null
    fun isClear(share: Double, usual: Double): Boolean {
        if (abs((share * 100).roundToInt() - (usual * 100).roundToInt()) < MIN_MIX_SHARE_GAP) return false
        val pooled = (share * currentTotal + usual * usualTotal) / (currentTotal + usualTotal)
        val spread = sqrt(pooled * (1 - pooled) * (1 / currentTotal + 1 / usualTotal))
        return spread > 0 && abs(share - usual) >= MIN_MIX_SHARE_Z * spread
    }

    // 평균보다 낮은 묶음의 비중이 늘면 값이 떨어진다. 비중 차이와 평균과의 차이를 곱한 값이 변화 쪽으로 가장 큰 묶음을 짚는다.
    val named = slices
        .map { slice ->
            val share = slice.current.size() / currentTotal
            val usual = slice.usual.size() / usualTotal
            val value = metric.value(slice.current) ?: metric.value(slice.usual) ?: now
            Named(slice.group, share, usual, pull = (share - usual) * (value - now) * direction)
        }
        .filter { it.pull > 0 && isClear(it.share, it.usual) }
        .maxByOrNull { it.pull }
        ?: return null

    // 짚은 묶음은 뺀다. 오퍼레이터가 늘었을 때 오퍼레이터 라운드만 봐서는 나머지가 평소와 같았는지 알 수 없다.
    val steady = slices
        .filter { it.group != named.group && it.current.rounds >= MIN_TREND_ROUNDS && it.usual.rounds >= MIN_TREND_ROUNDS }
        .maxByOrNull { it.current.rounds + it.usual.rounds }
        ?.let { slice -> steadyPart(metric, slice, weapons) }
    return MixShift(group = named.group, share = named.share, usualShare = named.usual, steady = steady)
}

private class Named(val group: MixGroup, val share: Double, val usual: Double, val pull: Double)

// 무기는 S6과 같은 표본과 값으로 적는다. 들고 시작한 라운드로 잰 헤드샷을 적으면 S6의 숫자와 갈린다.
private fun steadyPart(metric: FixedMetric, slice: MixSlice, weapons: List<WeaponTrend>): SteadyPart? {
    val group = slice.group
    if (group is MixGroup.Weapon) {
        val weaponMetric = metric.weaponMetric ?: return null
        val trend = weapons.firstOrNull { it.weapon == group.weapon } ?: return null
        val current = trend.current?.value(weaponMetric) ?: return null
        val usual = trend.baseline?.value(weaponMetric) ?: return null
        return SteadyPart(group, current, usual)
    }
    val current = metric.value(slice.current) ?: return null
    val usual = metric.value(slice.usual) ?: return null
    return SteadyPart(group, current, usual)
}

// 고정 지표의 분자와 분모다. 묶음마다 비중을 바꿔 다시 더할 때 쓴다.
private fun FixedMetric.fraction(metrics: MatchMetrics): Pair<Double, Double> = when (this) {
    FixedMetric.COMBAT_SCORE -> metrics.combatScore.toDouble() to metrics.rounds.toDouble()
    FixedMetric.KD -> metrics.kills.toDouble() to metrics.deaths.toDouble()
    FixedMetric.DAMAGE -> metrics.damage.toDouble() to metrics.rounds.toDouble()
    FixedMetric.HEADSHOT_RATE -> metrics.shots.head.toDouble() to metrics.shots.total.toDouble()
    FixedMetric.KDA -> (metrics.kills + metrics.assists).toDouble() to metrics.deaths.toDouble()
}

private val FixedMetric.weaponMetric: WeaponMetric?
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> null
        FixedMetric.KD -> WeaponMetric.KD
        FixedMetric.DAMAGE -> WeaponMetric.DAMAGE_PER_ROUND
        FixedMetric.HEADSHOT_RATE -> WeaponMetric.HEADSHOT_RATE
        // 무기별 어시스트는 들고 시작한 라운드로만 세서 무기 KDA를 짚을 값이 없다
        FixedMetric.KDA -> null
    }

// CLAUDE.md 역할군 표의 "크게 띄우지 않는 것" 중 고정 지표만 옮겼다
private val Role.mutedFixedMetrics: Set<FixedMetric>
    get() = when (this) {
        Role.INITIATOR, Role.CONTROLLER -> setOf(FixedMetric.KD)
        Role.DUELIST, Role.SENTINEL -> emptySet()
    }
