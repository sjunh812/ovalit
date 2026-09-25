package com.ovalit.core.data

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.KillEvent
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.Round
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponId
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 프로덕션 키가 나오기 전까지 화면을 붙여 보는 데 쓰는 가짜 경기입니다.
 *
 * 시드가 고정이라 매번 같은 경기가 나오고 날짜만 [clock]을 따라 움직입니다. 최근 7일은 첫
 * 교전에 더 자주 들어가게 해 둬서 홈 동적 칸에 움직인 지표가 뜹니다.
 */
class FakeMatchRepository(
    private val clock: Clock = Clock.System,
) : MatchRepository {

    private val matches = MutableStateFlow(fakeMatches(clock.now()))

    override fun observeMatches(): Flow<List<Match>> = matches

    override suspend fun deleteAll() {
        matches.value = emptyList()
    }

    /** 다시 연동한 것처럼 가짜 경기를 새로 채웁니다. */
    fun refill() {
        matches.value = fakeMatches(clock.now())
    }
}

private const val SEED = 923
private const val DAYS = 70

private const val USUAL_FIRST_DUEL_RATE = 0.28
private const val RECENT_FIRST_DUEL_RATE = 0.42
// 수비에서 첫 교전을 더 자주 지게 해서 홈에 개선 포인트 문장이 뜨게 했다. 둘의 평균은 0.55다.
private const val ATTACK_FIRST_DUEL_WIN_RATE = 0.66
private const val DEFENSE_FIRST_DUEL_WIN_RATE = 0.44
private const val HALF_ROUNDS = 12
private const val EXTRA_KILL_RATE = 0.35
private const val LATE_DEATH_RATE = 0.5
private const val TRADE_RATE = 0.4
private const val ASSIST_RATE = 0.3
private const val LEGSHOT_RATE = 0.08
private const val KILL_SCORE = 60

private val Me = PlayerId("me")
private val Strangers = List(4) { PlayerId("ally-$it") }

internal val FakeFriendIds = listOf("fake-junho", "fake-minseok", "fake-jaehyun").map(::PlayerId)

// 친구가 내 편으로 끼는 비율. S5의 "같이 한 경기"가 전체 경기 수가 되지 않게 한다.
private const val FRIEND_IN_MATCH_RATE = 0.2
private val Enemies = List(5) { PlayerId("enemy-$it") }
private val FakeAct = ActId("fake-act")

internal class FakeAgent(val id: AgentId, val name: String, val role: Role, val weight: Double)

internal val FakeAgents = listOf(
    FakeAgent(AgentId("fake-jett"), "제트", Role.DUELIST, 0.45),
    FakeAgent(AgentId("fake-raze"), "레이즈", Role.DUELIST, 0.30),
    FakeAgent(AgentId("fake-sova"), "소바", Role.INITIATOR, 0.15),
    FakeAgent(AgentId("fake-omen"), "오멘", Role.CONTROLLER, 0.07),
    FakeAgent(AgentId("fake-killjoy"), "킬조이", Role.SENTINEL, 0.03),
)

internal class FakeWeapon(
    val id: WeaponId,
    val name: String,
    val category: WeaponCategory,
    val weight: Double,
    val headshotRate: Double,
)

internal val FakeRifles = listOf(
    FakeWeapon(WeaponId("fake-phantom"), "팬텀", WeaponCategory.RIFLE, 0.45, 0.24),
    FakeWeapon(WeaponId("fake-vandal"), "밴달", WeaponCategory.RIFLE, 0.30, 0.21),
    FakeWeapon(WeaponId("fake-guardian"), "가디언", WeaponCategory.RIFLE, 0.06, 0.30),
    FakeWeapon(WeaponId("fake-bulldog"), "불독", WeaponCategory.RIFLE, 0.04, 0.18),
    FakeWeapon(WeaponId("fake-spectre"), "스펙터", WeaponCategory.SMG, 0.07, 0.16),
    FakeWeapon(WeaponId("fake-operator"), "오퍼레이터", WeaponCategory.SNIPER, 0.04, 0.10),
    FakeWeapon(WeaponId("fake-judge"), "저지", WeaponCategory.SHOTGUN, 0.02, 0.08),
    FakeWeapon(WeaponId("fake-odin"), "오딘", WeaponCategory.MACHINE_GUN, 0.02, 0.12),
)

internal val FakePistols = listOf(
    FakeWeapon(WeaponId("fake-ghost"), "고스트", WeaponCategory.PISTOL, 0.5, 0.28),
    FakeWeapon(WeaponId("fake-sheriff"), "셰리프", WeaponCategory.PISTOL, 0.3, 0.35),
    FakeWeapon(WeaponId("fake-classic"), "클래식", WeaponCategory.PISTOL, 0.2, 0.22),
)

// 최근 7일은 팬텀 헤드샷을 크게 올려서 무기 화면에 "요즘 잘 맞아요"가 뜨게 했다
private const val RECENT_PHANTOM_HEADSHOT_RATE = 0.45
private val PistolRounds = setOf(1, 13)

private fun <T> Random.pick(items: List<T>, weight: (T) -> Double): T {
    var roll = nextDouble() * items.sumOf(weight)
    for (item in items) {
        roll -= weight(item)
        if (roll <= 0) return item
    }
    return items.last()
}

/**
 * @param withFriends 내 경기일 때만 켠다. 친구 경기에 다른 친구를 끼우면 같이 한 경기가 꼬인다.
 */
internal fun fakeMatches(now: Instant, seed: Int = SEED, withFriends: Boolean = true): List<Match> {
    val random = Random(seed)
    // 팀 구성은 난수를 따로 쓴다. 같은 난수에서 뽑으면 친구를 넣는 순간 내 경기 숫자가 다 바뀐다.
    val party = Random(seed + 1)
    return (0 until DAYS).flatMap { daysAgo ->
        List(random.nextInt(0, 3)) { index ->
            random.fakeMatch(
                id = MatchId("fake-$seed-$daysAgo-$index"),
                startedAt = now - daysAgo.days - 50.minutes * (index + 1),
                allies = if (withFriends) party.allies() else Strangers,
                firstDuelRate = if (daysAgo < 7) RECENT_FIRST_DUEL_RATE else USUAL_FIRST_DUEL_RATE,
                recent = daysAgo < 7,
            )
        }
    }
}

private fun Random.allies(): List<PlayerId> = Strangers.mapIndexed { index, stranger ->
    FakeFriendIds.getOrNull(index)?.takeIf { nextDouble() < FRIEND_IN_MATCH_RATE } ?: stranger
}

private fun Random.fakeMatch(
    id: MatchId,
    startedAt: Instant,
    allies: List<PlayerId>,
    firstDuelRate: Double,
    recent: Boolean,
): Match {
    val agent = pick(FakeAgents) { it.weight }
    val startsOnAttack = nextBoolean()
    val rounds = List(nextInt(18, 25)) { index ->
        val firstHalf = index < HALF_ROUNDS
        val side = if (firstHalf == startsOnAttack) Side.ATTACK else Side.DEFENSE
        val pool = if (index + 1 in PistolRounds) FakePistols else FakeRifles
        fakeRound(
            number = index + 1,
            side = side,
            allies = allies,
            firstDuelRate = firstDuelRate,
            weapon = pick(pool) { it.weight },
            recent = recent,
        )
    }

    return Match(
        id = id,
        queue = if (nextDouble() < 0.8) Queue.COMPETITIVE else Queue.UNRATED,
        act = FakeAct,
        startedAt = startedAt,
        me = Me,
        myAgent = agent.id,
        myRole = agent.role,
        allies = allies.toSet(),
        myCombatScore = rounds.sumOf { round ->
            round.myDamage + KILL_SCORE * round.kills.count { it.killer == Me }
        },
        myTeamWon = rounds.count { it.won }.let { won ->
            when {
                won * 2 > rounds.size -> true
                won * 2 < rounds.size -> false
                else -> null
            }
        },
        rounds = rounds,
    )
}

private fun Random.fakeRound(
    number: Int,
    side: Side,
    allies: List<PlayerId>,
    firstDuelRate: Double,
    weapon: FakeWeapon,
    recent: Boolean,
): Round {
    val aliveEnemies = Enemies.shuffled(this).toMutableList()
    val kills = mutableListOf<KillEvent>()
    var myDeath: KillEvent? = null

    fun killEnemy(atMillis: Long, killer: PlayerId, assistants: Set<PlayerId> = emptySet()) {
        if (aliveEnemies.isEmpty()) return
        val used = if (killer == Me) weapon.id else null
        kills += KillEvent(atMillis, killer, aliveEnemies.removeAt(0), assistants, weapon = used)
    }

    val opener = aliveEnemies.first()
    when {
        nextDouble() >= firstDuelRate -> if (nextBoolean()) {
            killEnemy(15_000, killer = allies.random(this))
        } else {
            kills += KillEvent(15_000, opener, allies.random(this), emptySet(), weapon = null)
        }
        nextDouble() < if (side == Side.ATTACK) ATTACK_FIRST_DUEL_WIN_RATE else DEFENSE_FIRST_DUEL_WIN_RATE ->
            killEnemy(15_000, killer = Me)
        else -> myDeath = KillEvent(15_000, opener, Me, emptySet(), weapon = null)
    }

    if (myDeath == null) {
        repeat(2) { if (nextDouble() < EXTRA_KILL_RATE) killEnemy(30_000L + 12_000L * it, killer = Me) }
        if (aliveEnemies.isNotEmpty() && nextDouble() < LATE_DEATH_RATE) {
            myDeath = KillEvent(60_000, aliveEnemies.first(), Me, emptySet(), weapon = null)
        }
    }
    myDeath?.let { death ->
        kills += death
        if (nextDouble() < TRADE_RATE && aliveEnemies.remove(death.killer)) {
            kills += KillEvent(death.atMillis + 2_000, allies.random(this), death.killer, emptySet(), weapon = null)
        }
    }
    if (nextDouble() < ASSIST_RATE) killEnemy(45_000, killer = allies.random(this), assistants = setOf(Me))

    val myKills = kills.count { it.killer == Me }
    val hits = myKills * 3 + nextInt(0, 6)
    val headshotRate = if (recent && weapon.name == "팬텀") RECENT_PHANTOM_HEADSHOT_RATE else weapon.headshotRate
    val head = (0 until hits).count { nextDouble() < headshotRate }
    val leg = (0 until hits - head).count { nextDouble() < LEGSHOT_RATE }
    val openedByMe = kills.minBy { it.atMillis }.killer == Me
    val winRate = 0.5 + if (openedByMe) 0.15 else if (myDeath?.atMillis == 15_000L) -0.15 else 0.0

    return Round(
        number = number,
        won = nextDouble() < winRate,
        kills = kills.sortedBy { it.atMillis },
        myDamage = myKills * nextInt(110, 150) + nextInt(0, 70),
        myShots = Shots(head = head, body = hits - head - leg, leg = leg),
        mySide = side,
    )
}
