package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.roundToInt

enum class Side {
    ATTACK,
    DEFENSE,
}

/**
 * 개선 포인트 문장에서 두 쪽으로 나눠 견주는 지표입니다.
 *
 * 비율 지표는 화면에 보이는 %끼리 [MIN_PERCENT_GAP]%p 이상, 피해량은 두 쪽을 합친 평균의 [MIN_DAMAGE_GAP_RATIO] 이상
 * 벌어져야 문장을 만듭니다. 두 쪽이 각각 최소 표본도 넘겨야 합니다. 기준과 표본 모두 실데이터를 보고 조정할 시작값입니다.
 *
 * @property dynamic 같은 지표의 동적 칸입니다. 값과 최소 표본을 여기서 가져와 동적 칸과 갈리지 않게 합니다. 피해량과
 * 헤드샷은 동적 칸 후보가 아니라 `null`입니다.
 * @property focusOnly 관심사로 골랐을 때만 후보에 넣습니다. 이코·포스바이·풀바이 승률은 라운드 운영 다듬기를 고른
 * 사람에게만 봅니다.
 */
enum class InsightMetric(
    internal val dynamic: DynamicMetric?,
    internal val focusOnly: Boolean = false,
) {
    SURVIVAL_RATE(DynamicMetric.SURVIVAL_RATE),
    KAST(DynamicMetric.KAST),
    FIRST_DUEL_WIN_RATE(DynamicMetric.FIRST_DUEL_WIN_RATE),
    DAMAGE(dynamic = null),
    MULTI_KILL_RATE(DynamicMetric.MULTI_KILL_RATE),

    /** 무기끼리 견줄 때만 봅니다. 공수·요원·맵에서는 고정 칸의 헤드샷과 겹칩니다. */
    HEADSHOT_RATE(dynamic = null),
    FORCE_BUY_WIN_RATE(DynamicMetric.FORCE_BUY_WIN_RATE, focusOnly = true),
    ECO_WIN_RATE(DynamicMetric.ECO_WIN_RATE, focusOnly = true),
    FULL_BUY_WIN_RATE(DynamicMetric.FULL_BUY_WIN_RATE, focusOnly = true),
    ;

    val isPercent: Boolean get() = this != DAMAGE

    internal fun value(metrics: MatchMetrics): Double? = if (dynamic != null) dynamic.value(metrics) else metrics.adr

    internal fun isMeasurable(metrics: MatchMetrics): Boolean =
        dynamic?.isMeasurable(metrics) ?: (metrics.rounds >= DAMAGE_MIN_ROUNDS)
}

/** 개선 포인트의 피해량이 넘겨야 하는 라운드입니다. 동적 칸의 라운드 기준과 같습니다. */
const val DAMAGE_MIN_ROUNDS = 40

const val MIN_PERCENT_GAP = 10
const val MIN_DAMAGE_GAP_RATIO = 0.15

/** 견주는 한쪽이 무엇인지입니다. 화면은 이것으로 문장 틀과 이름을 고릅니다. */
sealed interface InsightSubject {
    data class OnSide(val side: Side) : InsightSubject

    data class OnAgent(val agent: AgentId) : InsightSubject

    /** 같은 역할의 나머지 요원입니다. 하나뿐이면 화면은 그 요원 이름을 씁니다. */
    data class OtherAgents(val role: Role, val agents: List<AgentId>) : InsightSubject

    data class OnMap(val map: MapId) : InsightSubject

    data object OtherMaps : InsightSubject

    data class WithWeapon(val weapon: WeaponId) : InsightSubject

    /** 같은 계열의 나머지 무기입니다. 하나뿐이면 화면은 그 무기 이름을 씁니다. */
    data class OtherWeapons(val category: WeaponCategory, val weapons: List<WeaponId>) : InsightSubject
}

/**
 * 견주는 한쪽의 값과 표본입니다.
 *
 * @property rounds 이 값을 낸 라운드입니다. 무기의 헤드샷은 한 무기만 쓴 라운드, 피해량은 들고 시작한 라운드입니다.
 */
data class InsightPart(
    val subject: InsightSubject,
    val value: Double,
    val matches: Int,
    val rounds: Int,
)

/**
 * 개선 포인트 문장 하나입니다. [weak]가 [other]보다 [metric]이 낮은 쪽입니다.
 *
 * @property isRolePriority 고른 지표가 역할의 우선 지표인지입니다. 화면은 이때 "전략가에게 생존율은 먼저 보는
 * 지표예요"를 붙입니다.
 * @property focus 관심사 지표라서 골랐으면 그 관심사입니다. 화면은 이때 "에임 올리기를 고르셔서 먼저 봤어요"를 붙입니다.
 */
data class Insight(
    val metric: InsightMetric,
    val weak: InsightPart,
    val other: InsightPart,
    val isRolePriority: Boolean,
    val focus: Focus? = null,
)

/**
 * 기간 경기를 공격과 수비, 같은 역할의 요원끼리, 맵끼리, 같은 계열의 무기끼리 나눠 견주고 문장 하나를 고릅니다.
 *
 * 어느 쪽이 얼마나 벌어졌는지만 보지 않고 그 쪽을 얼마나 뛰었는지도 봅니다. 낮은 쪽이 기간 라운드에서 차지하는 비중에
 * 격차를 곱해서, 나에게 영향이 가장 컸던 것을 고릅니다. 두 판 뛴 맵의 큰 격차보다 절반을 뛴 수비의 작은 격차가 먼저일 수
 * 있습니다.
 *
 * 관심사 지표 가운데 기준을 넘는 게 있으면 그중 영향이 가장 큰 것을 먼저 고릅니다. 역할이 크게 띄우지 않는 지표여도
 * 봅니다. 없으면 역할의 우선 지표, 그것도 기준에 못 미치면 나머지 가운데 영향이 가장 큰 것입니다. 관심사로 고른 게
 * 아니면 역할이 크게 띄우지 않는 지표와 [InsightMetric.focusOnly] 지표는 뺍니다. 기준을 넘는 게 없으면 `null`입니다.
 *
 * @param categories 무기 계열입니다. 비어 있으면 무기끼리는 견주지 않습니다.
 */
internal fun List<Match>.insight(
    role: Role?,
    focus: Focus = Focus.NONE,
    categories: Map<WeaponId, WeaponCategory> = emptyMap(),
): Insight? {
    val rounds = sumOf { it.rounds.size }
    if (rounds == 0) return null
    val candidates = sideCandidates(rounds) + agentCandidates(rounds) + mapCandidates(rounds) + weaponCandidates(categories, rounds)

    val focusMetrics = focus.insightMetrics
    candidates.filter { it.metric in focusMetrics }.maxByOrNull { it.impact }?.let {
        return it.toInsight(isRolePriority = false, focus = focus)
    }

    val general = candidates.filter { !it.metric.focusOnly && (role == null || it.metric !in role.mutedInsightMetrics) }
    val priority = role?.priorityInsightMetric
    general.filter { it.metric == priority }.maxByOrNull { it.impact }?.let {
        return it.toInsight(isRolePriority = true)
    }
    return general.maxByOrNull { it.impact }?.toInsight(isRolePriority = false)
}

private class Candidate(val metric: InsightMetric, val weak: InsightPart, val other: InsightPart, val impact: Double) {
    fun toInsight(isRolePriority: Boolean, focus: Focus? = null) = Insight(metric, weak, other, isRolePriority, focus)
}

// 경기 지표로 셀 수 있는 것들이다. 헤드샷은 무기끼리만 본다.
private val MatchInsightMetrics = InsightMetric.entries - InsightMetric.HEADSHOT_RATE

private class Group(val subject: InsightSubject, val matches: List<Match>, val metrics: MatchMetrics)

private fun List<Match>.sideCandidates(rounds: Int): List<Candidate> {
    val attack = Group(InsightSubject.OnSide(Side.ATTACK), this, map { it.metrics(Side.ATTACK) }.sum())
    val defense = Group(InsightSubject.OnSide(Side.DEFENSE), this, map { it.metrics(Side.DEFENSE) }.sum())
    // 공수는 둘뿐이라 낮은 쪽이 어느 쪽이든 문장이 된다
    return MatchInsightMetrics.mapNotNull { metric -> compare(metric, attack, defense, rounds) ?: compare(metric, defense, attack, rounds) }
}

// 역할이 다르면 같은 숫자도 뜻이 뒤집힌다. 같은 역할의 요원끼리만 견준다.
private fun List<Match>.agentCandidates(rounds: Int): List<Candidate> = this
    .filter { it.myRole != null }
    .groupBy { it.myRole!! }
    .flatMap { (role, matches) ->
        val byAgent = matches.groupBy { it.myAgent }
        if (byAgent.size < 2) return@flatMap emptyList()
        byAgent.flatMap { (agent, mine) ->
            val rest = matches.filter { it.myAgent != agent }
            val one = Group(InsightSubject.OnAgent(agent), mine, mine.totalMetrics())
            val others = Group(InsightSubject.OtherAgents(role, (byAgent.keys - agent).toList()), rest, rest.totalMetrics())
            MatchInsightMetrics.mapNotNull { compare(it, one, others, rounds) }
        }
    }

private fun List<Match>.mapCandidates(rounds: Int): List<Candidate> {
    val byMap = groupBy { it.map }
    if (byMap.size < 2) return emptyList()
    return byMap.flatMap { (map, mine) ->
        val rest = filter { it.map != map }
        val one = Group(InsightSubject.OnMap(map), mine, mine.totalMetrics())
        val others = Group(InsightSubject.OtherMaps, rest, rest.totalMetrics())
        MatchInsightMetrics.mapNotNull { compare(it, one, others, rounds) }
    }
}

// [weak]가 [other]보다 낮을 때만 후보다. 높은 쪽은 개선할 곳이 아니다.
private fun compare(metric: InsightMetric, weak: Group, other: Group, rounds: Int): Candidate? {
    if (!metric.isMeasurable(weak.metrics) || !metric.isMeasurable(other.metrics)) return null
    val low = metric.value(weak.metrics) ?: return null
    val high = metric.value(other.metrics) ?: return null
    val damageBase = (weak.metrics + other.metrics).adr
    val ratio = gapRatio(metric, low, high, damageBase) ?: return null
    return Candidate(
        metric = metric,
        weak = InsightPart(weak.subject, low, weak.matches.size, weak.metrics.rounds),
        other = InsightPart(other.subject, high, other.matches.size, other.metrics.rounds),
        impact = ratio * weak.metrics.rounds / rounds,
    )
}

// 무기는 같은 계열끼리만 견준다. 권총과 소총을 견주면 이코 라운드와 풀바이 라운드를 견주는 셈이 된다.
private fun List<Match>.weaponCandidates(categories: Map<WeaponId, WeaponCategory>, rounds: Int): List<Candidate> {
    if (categories.isEmpty()) return emptyList()
    return weaponStats()
        .filter { categories[it.weapon] != null && categories[it.weapon] != WeaponCategory.MELEE }
        .groupBy { categories.getValue(it.weapon) }
        .flatMap { (category, weapons) ->
            if (weapons.size < 2) return@flatMap emptyList()
            weapons.flatMap { mine ->
                val rest = (weapons - mine).reduce(WeaponStats::plus)
                val others = InsightSubject.OtherWeapons(category, (weapons - mine).map { it.weapon })
                listOfNotNull(
                    compareWeapons(InsightMetric.HEADSHOT_RATE, mine, rest, others, rounds),
                    compareWeapons(InsightMetric.DAMAGE, mine, rest, others, rounds),
                )
            }
        }
}

private fun compareWeapons(metric: InsightMetric, mine: WeaponStats, rest: WeaponStats, others: InsightSubject, rounds: Int): Candidate? {
    val weaponMetric = if (metric == InsightMetric.HEADSHOT_RATE) WeaponMetric.HEADSHOT_RATE else WeaponMetric.DAMAGE_PER_ROUND
    val low = mine.value(weaponMetric) ?: return null
    val high = rest.value(weaponMetric) ?: return null
    val ratio = gapRatio(metric, low, high, damageBase = (mine + rest).damagePerRound) ?: return null
    fun sample(stats: WeaponStats) = if (metric == InsightMetric.HEADSHOT_RATE) stats.singleWeaponRounds else stats.carriedRounds
    return Candidate(
        metric = metric,
        weak = InsightPart(InsightSubject.WithWeapon(mine.weapon), low, matches = 0, rounds = sample(mine)),
        other = InsightPart(others, high, matches = 0, rounds = sample(rest)),
        impact = ratio * sample(mine) / rounds,
    )
}

/** 격차를 기준으로 나눈 값입니다. [low]가 [high]보다 낮지 않거나 기준에 못 미치면 `null`입니다. */
private fun gapRatio(metric: InsightMetric, low: Double, high: Double, damageBase: Double?): Double? {
    // 화면에 보이는 자릿수로 반올림한 값끼리 뺀다. 48%와 35%를 띄워 놓고 14%p라고 쓰면 틀려 보인다.
    val ratio = if (metric.isPercent) {
        ((high * 100).roundToInt() - (low * 100).roundToInt()).toDouble() / MIN_PERCENT_GAP
    } else {
        val usual = damageBase?.takeIf { it > 0 } ?: return null
        (high.roundToInt() - low.roundToInt()) / (usual * MIN_DAMAGE_GAP_RATIO)
    }
    return ratio.takeIf { it >= 1.0 }
}

private operator fun WeaponStats.plus(other: WeaponStats) = WeaponStats(
    weapon = weapon,
    kills = kills + other.kills,
    singleWeaponRounds = singleWeaponRounds + other.singleWeaponRounds,
    shots = shots + other.shots,
    carriedRounds = carriedRounds + other.carriedRounds,
    deaths = deaths + other.deaths,
    assists = assists + other.assists,
    damage = damage + other.damage,
)

private fun List<Match>.totalMetrics(): MatchMetrics = map { it.metrics() }.sum()

// 관심사 지표 가운데 두 쪽으로 나눠 셀 수 있는 것이다. 관심사 목록에서 바로 뽑아 동적 칸과 갈리지 않게 한다.
private val Focus.insightMetrics: List<InsightMetric>
    get() = metrics.mapNotNull { metric -> InsightMetric.entries.firstOrNull { it.dynamic == metric } }

// CLAUDE.md 역할군 표의 우선 지표 가운데 두 쪽으로 나눠 셀 수 있는 것
private val Role.priorityInsightMetric: InsightMetric
    get() = when (this) {
        Role.DUELIST -> InsightMetric.FIRST_DUEL_WIN_RATE
        Role.INITIATOR -> InsightMetric.KAST
        Role.CONTROLLER, Role.SENTINEL -> InsightMetric.SURVIVAL_RATE
    }

// 동적 칸과 같다. 전략가와 감시자는 퍼블을, 척후대와 전략가는 멀티킬을 크게 띄우지 않는다.
private val Role.mutedInsightMetrics: Set<InsightMetric>
    get() = when (this) {
        Role.CONTROLLER -> setOf(InsightMetric.FIRST_DUEL_WIN_RATE, InsightMetric.MULTI_KILL_RATE)
        Role.SENTINEL -> setOf(InsightMetric.FIRST_DUEL_WIN_RATE)
        Role.INITIATOR -> setOf(InsightMetric.MULTI_KILL_RATE)
        Role.DUELIST -> emptySet()
    }
