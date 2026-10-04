package com.ovalit.core.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
 * @property dynamic 같은 지표의 동적 칸입니다. 값과 최소 표본을 여기서 가져와 동적 칸과 갈리지 않게 합니다. 피해량, 헤드샷,
 * 승률은 동적 칸 후보가 아니라 `null`입니다.
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

    /**
     * 경기 승률입니다. 요원, 역할, 맵끼리 견줄 때 봅니다. 관여율이나 첫 킬과 달리 역할에 따라 뜻이 뒤집히지 않아서 역할이
     * 다른 것끼리도 견줄 수 있습니다. 두 쪽 모두 승패가 갈린 판이 [MIN_AGENT_MATCHES]판을 넘겨야 합니다.
     */
    WIN_RATE(dynamic = null),

    /** 무기끼리 견줄 때만 봅니다. 공수·요원·맵에서는 고정 칸의 헤드샷과 겹칩니다. */
    HEADSHOT_RATE(dynamic = null),
    FORCE_BUY_WIN_RATE(DynamicMetric.FORCE_BUY_WIN_RATE, focusOnly = true),
    ECO_WIN_RATE(DynamicMetric.ECO_WIN_RATE, focusOnly = true),
    FULL_BUY_WIN_RATE(DynamicMetric.FULL_BUY_WIN_RATE, focusOnly = true),
    ;

    val isPercent: Boolean get() = this != DAMAGE
}

/** 개선 포인트의 피해량이 넘겨야 하는 라운드입니다. 동적 칸의 라운드 기준과 같습니다. */
const val DAMAGE_MIN_ROUNDS = 40

const val MIN_PERCENT_GAP = 10
const val MIN_DAMAGE_GAP_RATIO = 0.15

/**
 * 두 쪽 차이가 우연히 벌어질 만한 폭의 이 배수를 넘어야 문장을 만듭니다. 한 액트에 견주는 조합이 수십 개라, 한 번 견줄 때
 * 흔히 쓰는 2배로는 공수·요원·맵·무기가 성적과 아무 상관 없는 사람에게도 절반 넘게 문장 하나가 걸립니다. 3.5배면 50판까지
 * 스무 번에 한 번 안팎이고, 실제로 수비 생존율이 10%p 낮은 사람은 50판에서 절반쯤 짚습니다(2026-09-27 시뮬레이션).
 * 실데이터를 보고 조정할 시작값입니다.
 */
const val MIN_GAP_Z = 3.5

/** 흔들림을 경기마다 묶어 재려면 한쪽에 경기가 이만큼은 있어야 합니다. 두 판으로는 그날 컨디션과 가를 수 없습니다. */
const val MIN_INSIGHT_MATCHES = 3

/**
 * 앞 판이 끝나고 이만큼 안에 다음 판을 시작하면 연달아 뛴 것으로 봅니다. 큐 잡는 시간과 잠깐 쉬는 시간을 넉넉히 넣은
 * 시작 기준선입니다.
 */
const val SESSION_BREAK_MILLIS = 60 * 60 * 1000L

/** 연달아 뛴 판 가운데 이 판부터를 늦은 판으로 묶습니다. 화면 문구("세 번째 판부터", "첫 두 판")도 이 값에 맞춰 있습니다. */
const val LATE_SESSION_GAME = 3

/** 견주는 한쪽이 무엇인지입니다. 화면은 이것으로 문장 틀과 이름을 고릅니다. */
sealed interface InsightSubject {
    data class OnSide(val side: Side) : InsightSubject

    data class OnAgent(val agent: AgentId) : InsightSubject

    /** 같은 역할의 나머지 요원 둘 이상입니다. 하나뿐이면 [OnAgent]로 바꿔 둡니다. */
    data class OtherAgents(val role: Role, val agents: List<AgentId>) : InsightSubject

    data class OnRole(val role: Role) : InsightSubject

    /** 나머지 역할 둘 이상입니다. 하나뿐이면 [OnRole]로 바꿔 둡니다. */
    data class OtherRoles(val roles: List<Role>) : InsightSubject

    data class OnMap(val map: MapId) : InsightSubject

    /** 나머지 맵 둘 이상입니다. 하나뿐이면 [OnMap]으로 바꿔 둡니다. */
    data class OtherMaps(val maps: List<MapId>) : InsightSubject

    data class WithWeapon(val weapon: WeaponId) : InsightSubject

    /** 같은 계열의 나머지 무기 둘 이상입니다. 하나뿐이면 [WithWeapon]으로 바꿔 둡니다. */
    data class OtherWeapons(val category: WeaponCategory, val weapons: List<WeaponId>) : InsightSubject

    /** 연달아 뛴 판 가운데 [LATE_SESSION_GAME]번째 판부터입니다. */
    data object LateInSession : InsightSubject

    /** 연달아 뛴 판 가운데 [LATE_SESSION_GAME]번째 판 앞까지입니다. */
    data object EarlyInSession : InsightSubject
}

// 주어로 두지 않는 쪽이다. 묶음을 주어로 두면 "다른 맵에서는 헤이븐보다 높아요"처럼 어색해진다. 첫 두 판도 주어로 두면
// "첫 두 판은 세 번째 판부터보다 높아요"가 되어 무엇을 짚는지 흐려져서, 늘 세 번째 판부터를 주어로 둔다.
private val InsightSubject.isGroup: Boolean
    get() = when (this) {
        is InsightSubject.OtherAgents -> agents.size > 1
        is InsightSubject.OtherRoles -> roles.size > 1
        is InsightSubject.OtherMaps -> maps.size > 1
        is InsightSubject.OtherWeapons -> weapons.size > 1
        InsightSubject.EarlyInSession -> true
        else -> false
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
 * 개선 포인트 문장 하나입니다. [lead]를 주어로 [other]와 견줍니다.
 *
 * 두 쪽 모두 이름이 있으면(공격과 수비, 제트와 레이즈) 높은 쪽이 [lead]라 "제트로 뛴 판은 승률이 레이즈보다 높아요"가
 * 됩니다. 한쪽이 여럿을 묶은 쪽이면(다른 맵) 이름 있는 쪽이 [lead]이고, 낮으면 "헤이븐에서는 관여율이 다른 맵보다
 * 낮아요"가 됩니다.
 *
 * @property leadIsHigher [lead]가 [other]보다 높은지입니다.
 * @property isRolePriority 고른 지표가 역할의 우선 지표인지입니다. 화면은 이때 "전략가에게 생존율은 먼저 보는
 * 지표예요"를 붙입니다.
 * @property focus 관심사 지표라서 골랐으면 그 관심사입니다. 화면은 이때 "에임 올리기를 고르셔서 먼저 봤어요"를 붙입니다.
 * @property recent 홈 리포트 기간(이번 주)만 셌을 때 두 쪽 값입니다. 액트 동안 보인 차이가 이번 주에도 이어졌는지 보여줍니다.
 * 두 쪽 중 하나라도 기간 표본이 모자라거나, 기간이 이번 액트 경기를 모두 담아 위 숫자와 같으면 `null`입니다.
 * @property matches 둘로 나눠 견준 경기 수입니다. 홈은 이번 액트 경기 수를 묶음 제목 옆에 적습니다.
 */
data class Insight(
    val metric: InsightMetric,
    val lead: InsightPart,
    val other: InsightPart,
    val leadIsHigher: Boolean,
    val isRolePriority: Boolean,
    val focus: Focus? = null,
    val recent: InsightRecent? = null,
    val matches: Int = 0,
)

/**
 * 리포트 기간의 두 쪽 값입니다. 한 주 표본은 작아서 우연을 거르지 않고, 달라졌다고 판단하지도 않습니다. 숫자만 적습니다.
 *
 * @property lead [Insight.lead]와 같은 쪽의 값, [other]는 [Insight.other]와 같은 쪽의 값입니다.
 */
data class InsightRecent(val lead: Double, val other: Double)

/**
 * 경기를 공격과 수비, 역할끼리, 같은 역할의 요원끼리, 맵끼리, 같은 계열의 무기끼리 나눠 견주고 문장 하나를 고릅니다. 홈은
 * 이번 액트 경기를 넘깁니다. 한 주 경기를 둘로 나누면 표본이 작아 우연한 차이가 대부분입니다.
 *
 * 격차가 기준을 넘어도 우연히 벌어질 만한 폭의 [minZ]배에 못 미치면 후보에서 뺍니다. 흔들림은 경기마다 묶어서 잽니다.
 *
 * 얼마나 벌어졌는지만 보지 않고 두 쪽을 얼마나 뛰었는지도 봅니다. 두 쪽이 기간 라운드에서 차지한 비중을 곱하고 격차를
 * 곱해서, 나에게 영향이 가장 컸던 것을 고릅니다. 두 판 뛴 맵의 큰 격차보다 절반씩 뛴 공수의 작은 격차가 먼저일 수
 * 있습니다.
 *
 * 관심사 지표 가운데 기준을 넘는 게 있으면 그중 영향이 가장 큰 것을 먼저 고릅니다. 역할이 크게 띄우지 않는 지표여도
 * 봅니다. 없으면 역할의 우선 지표, 그것도 기준에 못 미치면 나머지 가운데 영향이 가장 큰 것입니다. 관심사로 고른 게
 * 아니면 역할이 크게 띄우지 않는 지표와 [InsightMetric.focusOnly] 지표는 뺍니다. 기준을 넘는 게 없으면 `null`입니다.
 *
 * @param categories 무기 계열입니다. 비어 있으면 무기끼리는 견주지 않습니다.
 * @param minZ 차이가 넘겨야 하는 흔들림의 배수입니다. 테스트는 0으로 우연 거르기를 끄고 나머지 기준만 봅니다.
 */
internal fun List<Match>.insight(
    role: Role?,
    focus: Focus = Focus.NONE,
    categories: Map<WeaponId, WeaponCategory> = emptyMap(),
    minZ: Double = MIN_GAP_Z,
): Insight? {
    val rounds = sumOf { it.rounds.size }
    if (rounds == 0) return null
    val scale = Scale(rounds, minZ)
    val candidates = sideCandidates(scale) + roleCandidates(scale) + agentCandidates(scale) + mapCandidates(scale) +
        weaponCandidates(categories, scale) + sessionCandidates(scale)

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

private class Candidate(val metric: InsightMetric, val a: InsightPart, val b: InsightPart, val impact: Double) {
    fun toInsight(isRolePriority: Boolean, focus: Focus? = null): Insight {
        val aIsHigher = a.value > b.value
        // 한쪽이 묶음이면 이름 있는 쪽을, 둘 다 이름이 있으면 높은 쪽을 주어로 둔다
        val lead = when {
            b.subject.isGroup -> a
            a.subject.isGroup -> b
            aIsHigher -> a
            else -> b
        }
        val other = if (lead === a) b else a
        return Insight(
            metric = metric,
            lead = lead.copy(subject = lead.subject.single()),
            other = other.copy(subject = other.subject.single()),
            leadIsHigher = lead.value > other.value,
            isRolePriority = isRolePriority,
            focus = focus,
        )
    }
}

// 하나뿐인 "나머지"는 그 하나로 부른다. 화면은 주어의 종류로 문장 틀을 고른다.
private fun InsightSubject.single(): InsightSubject = when (this) {
    is InsightSubject.OtherAgents -> agents.singleOrNull()?.let { InsightSubject.OnAgent(it) } ?: this
    is InsightSubject.OtherRoles -> roles.singleOrNull()?.let { InsightSubject.OnRole(it) } ?: this
    is InsightSubject.OtherMaps -> maps.singleOrNull()?.let { InsightSubject.OnMap(it) } ?: this
    is InsightSubject.OtherWeapons -> weapons.singleOrNull()?.let { InsightSubject.WithWeapon(it) } ?: this
    else -> this
}

/**
 * [matches]만 셌을 때 두 쪽 값을 [Insight.recent]에 담습니다. 홈은 리포트 기간 경기를 넘깁니다. 두 쪽 모두 동적 칸과 같은
 * 최소 표본을 넘겨야 합니다. 무기는 S6과 같은 표본입니다.
 */
internal fun Insight.during(matches: List<Match>, categories: Map<WeaponId, WeaponCategory>): Insight {
    val lead = lead.subject.value(metric, matches, categories) ?: return this
    val other = other.subject.value(metric, matches, categories) ?: return this
    return copy(recent = InsightRecent(lead, other))
}

private fun InsightSubject.value(metric: InsightMetric, matches: List<Match>, categories: Map<WeaponId, WeaponCategory>): Double? {
    val group = when (this) {
        is InsightSubject.OnSide -> return matches.sideGroup(side).takeIf { it.hasSample(metric) }?.value(metric)
        is InsightSubject.OnAgent -> matches.filter { it.myAgent == agent }
        is InsightSubject.OtherAgents -> matches.filter { it.myAgent in agents }
        is InsightSubject.OnRole -> matches.filter { it.myRole == role }
        is InsightSubject.OtherRoles -> matches.filter { it.myRole in roles }
        is InsightSubject.OnMap -> matches.filter { it.map == map }
        is InsightSubject.OtherMaps -> matches.filter { it.map in maps }
        is InsightSubject.WithWeapon -> return matches.weaponValue(setOf(weapon), metric)
        is InsightSubject.OtherWeapons -> return matches.weaponValue(weapons.toSet(), metric)
        // 기간 경기만으로 몇 번째 판인지 센다. 일요일 밤에 시작해 월요일로 넘어간 판은 기간 첫 판으로 세지만 드물다.
        InsightSubject.LateInSession -> matches.bySessionGame(late = true)
        InsightSubject.EarlyInSession -> matches.bySessionGame(late = false)
    }
    return group.group(this).takeIf { it.hasSample(metric) }?.value(metric)
}

private fun List<Match>.weaponValue(weapons: Set<WeaponId>, metric: InsightMetric): Double? {
    val stats = weaponStats().filter { it.weapon in weapons }.reduceOrNull(WeaponStats::plus) ?: return null
    return stats.value(if (metric == InsightMetric.HEADSHOT_RATE) WeaponMetric.HEADSHOT_RATE else WeaponMetric.DAMAGE_PER_ROUND)
}

// 경기 지표로 셀 수 있는 것들이다. 헤드샷은 무기끼리만 본다.
private val MatchInsightMetrics = InsightMetric.entries - InsightMetric.HEADSHOT_RATE

/** 후보마다 같이 쓰는 값입니다. [rounds]는 기간 전체 라운드, [minZ]는 차이가 넘겨야 하는 흔들림의 배수입니다. */
private class Scale(val rounds: Int, val minZ: Double)

/** 경기 하나가 낸 분자와 분모입니다. 생존율이면 살아남은 라운드와 뛴 라운드입니다. */
private class Share(val part: Int, val whole: Int)

/**
 * 견주는 한쪽의 값과 그 값을 낸 경기별 몫입니다.
 *
 * @property damages 이쪽 라운드마다 입힌 피해량입니다. 피해량이 우연히 흔들리는 폭의 바닥을 라운드 단위로 잽니다.
 */
private class Sample(val value: Double, val shares: List<Share>, val damages: List<Int>) {
    val matches: Int get() = shares.count { it.whole > 0 }
}

/**
 * 견주는 한쪽입니다.
 *
 * @property perMatch [matches]와 같은 순서로, 경기마다 이쪽에 든 라운드의 지표입니다. 공수로 나누면 그 진영 라운드만 셉니다.
 */
private class Group(
    val subject: InsightSubject,
    val matches: List<Match>,
    val perMatch: List<MatchMetrics>,
    val damages: List<Int>,
) {
    val metrics: MatchMetrics = perMatch.sum()

    val decided: Int get() = matches.count { it.myTeamWon != null }

    fun value(metric: InsightMetric): Double? = when {
        metric == InsightMetric.WIN_RATE -> matches.count { it.myTeamWon == true } over decided
        metric.dynamic != null -> metric.dynamic.value(metrics)
        else -> metrics.adr
    }

    fun isMeasurable(metric: InsightMetric): Boolean = matches.size >= MIN_INSIGHT_MATCHES && hasSample(metric)

    // 동적 칸과 같은 최소 표본이다. 이번 주 숫자는 우연을 거르지 않으니 판 수는 보지 않는다.
    fun hasSample(metric: InsightMetric): Boolean = when {
        metric == InsightMetric.WIN_RATE -> decided >= MIN_AGENT_MATCHES
        metric.dynamic != null -> metric.dynamic.isMeasurable(metrics)
        else -> metrics.rounds >= DAMAGE_MIN_ROUNDS
    }

    fun sample(metric: InsightMetric, value: Double) =
        Sample(value, matches.zip(perMatch) { match, metrics -> metric.share(match, metrics) }, damages)
}

private fun List<Match>.group(subject: InsightSubject) =
    Group(subject, this, map { it.metrics() }, flatMap { match -> match.rounds.map { it.myDamage } })

private fun List<Match>.sideGroup(side: Side) = Group(
    subject = InsightSubject.OnSide(side),
    matches = this,
    perMatch = map { it.metrics(side) },
    damages = flatMap { match -> match.rounds.filter { it.mySide == side }.map { it.myDamage } },
)

// 동적 칸과 같은 분자와 분모다. 승률만 경기 하나를 한 번으로 센다.
private fun InsightMetric.share(match: Match, metrics: MatchMetrics): Share = when (this) {
    InsightMetric.SURVIVAL_RATE -> Share(metrics.survivedRounds, metrics.rounds)
    InsightMetric.KAST -> Share(metrics.kastRounds, metrics.rounds)
    InsightMetric.FIRST_DUEL_WIN_RATE -> Share(metrics.firstKills, metrics.firstKills + metrics.firstDeaths)
    InsightMetric.DAMAGE -> Share(metrics.damage, metrics.rounds)
    InsightMetric.MULTI_KILL_RATE -> Share(metrics.multiKillRounds, metrics.rounds)
    InsightMetric.WIN_RATE -> Share(if (match.myTeamWon == true) 1 else 0, if (match.myTeamWon != null) 1 else 0)
    InsightMetric.HEADSHOT_RATE -> Share(metrics.shots.head, metrics.shots.total)
    InsightMetric.FORCE_BUY_WIN_RATE -> Share(metrics.forceBuyRoundsWon, metrics.forceBuyRounds)
    InsightMetric.ECO_WIN_RATE -> Share(metrics.ecoRoundsWon, metrics.ecoRounds)
    InsightMetric.FULL_BUY_WIN_RATE -> Share(metrics.fullBuyRoundsWon, metrics.fullBuyRounds)
}

// 공수는 라운드로 나누니 경기 승률이 없다
private fun List<Match>.sideCandidates(scale: Scale): List<Candidate> {
    val attack = sideGroup(Side.ATTACK)
    val defense = sideGroup(Side.DEFENSE)
    return (MatchInsightMetrics - InsightMetric.WIN_RATE).mapNotNull { compare(it, attack, defense, scale) }
}

// 역할이 다르면 관여율이나 첫 킬 같은 숫자는 뜻이 뒤집힌다. 역할끼리는 승률만 견준다.
private fun List<Match>.roleCandidates(scale: Scale): List<Candidate> {
    val byRole = filter { it.myRole != null }.groupBy { it.myRole!! }
    if (byRole.size < 2) return emptyList()
    return byRole.mapNotNull { (role, mine) ->
        val rest = filter { it.myRole != null && it.myRole != role }
        val others = InsightSubject.OtherRoles((byRole.keys - role).toList())
        compare(InsightMetric.WIN_RATE, mine.group(InsightSubject.OnRole(role)), rest.group(others), scale)
    }
}

// 요원은 같은 역할끼리만 견준다
private fun List<Match>.agentCandidates(scale: Scale): List<Candidate> = this
    .filter { it.myRole != null }
    .groupBy { it.myRole!! }
    .flatMap { (role, matches) ->
        val byAgent = matches.groupBy { it.myAgent }
        if (byAgent.size < 2) return@flatMap emptyList()
        byAgent.flatMap { (agent, mine) ->
            val one = mine.group(InsightSubject.OnAgent(agent))
            val others = matches.filter { it.myAgent != agent }
                .group(InsightSubject.OtherAgents(role, (byAgent.keys - agent).toList()))
            MatchInsightMetrics.mapNotNull { compare(it, one, others, scale) }
        }
    }

private fun List<Match>.mapCandidates(scale: Scale): List<Candidate> {
    val byMap = groupBy { it.map }
    if (byMap.size < 2) return emptyList()
    return byMap.flatMap { (map, mine) ->
        val one = mine.group(InsightSubject.OnMap(map))
        val others = filter { it.map != map }.group(InsightSubject.OtherMaps((byMap.keys - map).toList()))
        MatchInsightMetrics.mapNotNull { compare(it, one, others, scale) }
    }
}

// 사용자 요청(2026-09-27): 연달아 뛴 판이 뒤로 갈수록 어떤지 본다. 숫자만 적고 쉬라고 하지 않는다(CLAUDE.md 지켜야 할 선).
private fun List<Match>.sessionCandidates(scale: Scale): List<Candidate> {
    val late = bySessionGame(late = true).group(InsightSubject.LateInSession)
    val early = bySessionGame(late = false).group(InsightSubject.EarlyInSession)
    return MatchInsightMetrics.mapNotNull { compare(it, late, early, scale) }
}

/**
 * 연달아 뛴 판 가운데 [LATE_SESSION_GAME]번째 판부터이거나([late]) 그 앞까지인 경기입니다. 앞 판이 끝나고
 * [SESSION_BREAK_MILLIS] 넘게 쉬면 새로 셉니다.
 */
private fun List<Match>.bySessionGame(late: Boolean): List<Match> {
    var game = 0
    var previous: Match? = null
    return sortedBy { it.startedAt }.filter { match ->
        val rested = previous?.let { match.startedAt.toEpochMilliseconds() - it.startedAt.toEpochMilliseconds() - it.lengthMillis > SESSION_BREAK_MILLIS }
        game = if (rested == false) game + 1 else 1
        previous = match
        (game >= LATE_SESSION_GAME) == late
    }
}

private fun compare(metric: InsightMetric, a: Group, b: Group, scale: Scale): Candidate? {
    if (!a.isMeasurable(metric) || !b.isMeasurable(metric)) return null
    val first = a.value(metric) ?: return null
    val second = b.value(metric) ?: return null
    val ratio = gapRatio(metric, first, second, damageBase = (a.metrics + b.metrics).adr) ?: return null
    if (!isClear(metric, a.sample(metric, first), b.sample(metric, second), scale.minZ)) return null
    return Candidate(
        metric = metric,
        a = InsightPart(a.subject, first, a.matches.size, a.metrics.rounds),
        b = InsightPart(b.subject, second, b.matches.size, b.metrics.rounds),
        impact = impact(ratio, a.metrics.rounds, b.metrics.rounds, scale.rounds),
    )
}

/** 무기 여럿을 합친 한쪽입니다. [perMatch]는 경기마다 이 무기들의 성적이고, 안 쓴 경기는 `null`입니다. */
private class WeaponGroup(val stats: WeaponStats, val perMatch: List<WeaponStats?>, val damages: List<Int>)

private fun List<Match>.weaponGroup(weapons: Set<WeaponId>, perMatch: List<List<WeaponStats>>): WeaponGroup {
    val mine = perMatch.map { stats -> stats.filter { it.weapon in weapons }.reduceOrNull(WeaponStats::plus) }
    return WeaponGroup(
        stats = mine.filterNotNull().reduce(WeaponStats::plus),
        perMatch = mine,
        damages = flatMap { match -> match.rounds.filter { it.economy?.myWeapon in weapons }.map { it.myDamage } },
    )
}

// 무기는 같은 계열끼리만 견준다. 권총과 소총을 견주면 이코 라운드와 풀바이 라운드를 견주는 셈이 된다.
private fun List<Match>.weaponCandidates(categories: Map<WeaponId, WeaponCategory>, scale: Scale): List<Candidate> {
    if (categories.isEmpty()) return emptyList()
    val perMatch = map { listOf(it).weaponStats() }
    return weaponStats()
        .filter { categories[it.weapon] != null && categories[it.weapon] != WeaponCategory.MELEE }
        .groupBy { categories.getValue(it.weapon) }
        .flatMap { (category, weapons) ->
            if (weapons.size < 2) return@flatMap emptyList()
            weapons.flatMap { mine ->
                val rest = (weapons - mine).map { it.weapon }
                val one = weaponGroup(setOf(mine.weapon), perMatch)
                val others = weaponGroup(rest.toSet(), perMatch)
                val subject = InsightSubject.OtherWeapons(category, rest)
                listOfNotNull(
                    compareWeapons(InsightMetric.HEADSHOT_RATE, mine.weapon, one, others, subject, scale),
                    compareWeapons(InsightMetric.DAMAGE, mine.weapon, one, others, subject, scale),
                )
            }
        }
}

private fun compareWeapons(
    metric: InsightMetric,
    weapon: WeaponId,
    mine: WeaponGroup,
    rest: WeaponGroup,
    others: InsightSubject,
    scale: Scale,
): Candidate? {
    val isHeadshot = metric == InsightMetric.HEADSHOT_RATE
    val first = mine.stats.value(if (isHeadshot) WeaponMetric.HEADSHOT_RATE else WeaponMetric.DAMAGE_PER_ROUND) ?: return null
    val second = rest.stats.value(if (isHeadshot) WeaponMetric.HEADSHOT_RATE else WeaponMetric.DAMAGE_PER_ROUND) ?: return null
    val ratio = gapRatio(metric, first, second, damageBase = (mine.stats + rest.stats).damagePerRound) ?: return null
    fun WeaponStats?.share() = when {
        this == null -> Share(0, 0)
        isHeadshot -> Share(shots.head, shots.total)
        else -> Share(damage, carriedRounds)
    }
    fun WeaponGroup.sample(value: Double) = Sample(value, perMatch.map { it.share() }, damages)
    val firstSample = mine.sample(first)
    val secondSample = rest.sample(second)
    if (firstSample.matches < MIN_INSIGHT_MATCHES || secondSample.matches < MIN_INSIGHT_MATCHES) return null
    if (!isClear(metric, firstSample, secondSample, scale.minZ)) return null
    fun WeaponStats.rounds() = if (isHeadshot) singleWeaponRounds else carriedRounds
    return Candidate(
        metric = metric,
        a = InsightPart(InsightSubject.WithWeapon(weapon), first, matches = 0, rounds = mine.stats.rounds()),
        b = InsightPart(others, second, matches = 0, rounds = rest.stats.rounds()),
        impact = impact(ratio, mine.stats.rounds(), rest.stats.rounds(), scale.rounds),
    )
}

// 차이가 두 쪽이 우연히 흔들리는 폭을 합친 것의 [minZ]배를 넘는지 본다. 같은 사람이 같은 실력으로 뛰어도 열 판만 보면
// 공수 생존율이 10%p쯤은 쉽게 벌어진다.
private fun isClear(metric: InsightMetric, first: Sample, second: Sample, minZ: Double): Boolean {
    val firstWhole = first.shares.sumOf { it.whole }
    val secondWhole = second.shares.sumOf { it.whole }
    if (firstWhole == 0 || secondWhole == 0) return false
    val pooled = (first.value * firstWhole + second.value * secondWhole) / (firstWhole + secondWhole)
    val scale = matchScale(first, second)
    val spread = sqrt(first.variance(metric, pooled, scale) + second.variance(metric, pooled, scale))
    return spread > 0 && abs(first.value - second.value) >= minZ * spread
}

/**
 * 경기 하나가 제 쪽 값에서 얼마나 벗어나는지를 두 쪽 경기를 모두 모아 잽니다. 두 표본 t검정의 합친 분산과 같은 생각입니다.
 * 세 판뿐인 맵을 그 세 판만으로 재면 우연히 고른 날만 걸려 흔들림이 작게 잡히곤 해서, 경기 수를 늘려 안정되게 잽니다.
 * 각자 제 값에서 벗어난 만큼만 세서 두 쪽의 실제 차이는 흔들림에 들어가지 않습니다.
 */
private fun matchScale(first: Sample, second: Sample): Double {
    val deviations = first.deviations() + second.deviations()
    // 두 쪽 값을 하나씩 맞춰 썼으니 그만큼 자유도가 준다
    if (deviations.size < 3) return 0.0
    val spread = deviations.sumOf { (gap, _) -> gap * gap }
    val weight = deviations.sumOf { (_, whole) -> whole * whole }
    return deviations.size / (deviations.size - 2.0) * spread / weight
}

// 경기마다 제 쪽 값에서 벗어난 몫과 그 경기의 분모다
private fun Sample.deviations(): List<Pair<Double, Double>> =
    shares.filter { it.whole > 0 }.map { (it.part - value * it.whole) to it.whole.toDouble() }

/**
 * 이쪽 값이 우연히 흔들리는 폭(분산)입니다. 경기마다 묶어서 잽니다. 한 경기 라운드들은 그날 컨디션을 같이 타서, 라운드마다
 * 따로 셈하면 흔들림이 실제보다 작게 잡힙니다. 이쪽 경기만으로 잰 값, 두 쪽 경기를 모아 잰 [scale], 라운드가 서로 따로
 * 논다고 쳤을 때의 값 가운데 가장 큰 것을 씁니다. 우연을 차이로 읽는 쪽보다 차이를 놓치는 쪽이 낫습니다.
 *
 * @param pooled 두 쪽을 합친 비율입니다. 한쪽이 0%나 100%여도 흔들림이 0으로 잡히지 않게 바닥은 이 값으로 잽니다.
 */
private fun Sample.variance(metric: InsightMetric, pooled: Double, scale: Double): Double {
    val deviations = deviations()
    val whole = deviations.sumOf { (_, whole) -> whole }
    val floor = if (metric.isPercent) pooled * (1 - pooled) / whole else damages.spread() / damages.size.coerceAtLeast(1)
    val shared = scale * deviations.sumOf { (_, whole) -> whole * whole } / (whole * whole)
    if (deviations.size < 2) return maxOf(floor, shared)
    val own = deviations.size / (deviations.size - 1.0) * deviations.sumOf { (gap, _) -> gap * gap } / (whole * whole)
    return maxOf(own, floor, shared)
}

// 라운드 하나하나의 표본분산이다
private fun List<Int>.spread(): Double {
    if (size < 2) return 0.0
    val mean = average()
    return sumOf { (it - mean) * (it - mean) } / (size - 1)
}

// 두 쪽의 비중을 곱한다. 절반씩 뛰었을 때 격차 그대로이고, 한쪽이 작을수록 줄어든다. 한쪽만 곱하면 다섯 판과 스무 판을
// 견줄 때 어느 쪽을 기준으로 보느냐에 따라 영향이 달라진다.
private fun impact(ratio: Double, first: Int, second: Int, rounds: Int): Double =
    ratio * 4.0 * first / rounds * second / rounds

/** 격차를 기준으로 나눈 값입니다. 기준에 못 미치면 `null`입니다. */
private fun gapRatio(metric: InsightMetric, first: Double, second: Double, damageBase: Double?): Double? {
    // 화면에 보이는 자릿수로 반올림한 값끼리 뺀다. 48%와 35%를 띄워 놓고 14%p라고 쓰면 틀려 보인다.
    val ratio = if (metric.isPercent) {
        abs((first * 100).roundToInt() - (second * 100).roundToInt()).toDouble() / MIN_PERCENT_GAP
    } else {
        val usual = damageBase?.takeIf { it > 0 } ?: return null
        abs(first.roundToInt() - second.roundToInt()) / (usual * MIN_DAMAGE_GAP_RATIO)
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

// 동적 칸과 같다. 전략가와 감시자는 첫 킬을, 척후대와 전략가는 멀티킬을 크게 띄우지 않는다.
private val Role.mutedInsightMetrics: Set<InsightMetric>
    get() = when (this) {
        Role.CONTROLLER -> setOf(InsightMetric.FIRST_DUEL_WIN_RATE, InsightMetric.MULTI_KILL_RATE)
        Role.SENTINEL -> setOf(InsightMetric.FIRST_DUEL_WIN_RATE)
        Role.INITIATOR -> setOf(InsightMetric.MULTI_KILL_RATE)
        Role.DUELIST -> emptySet()
    }
