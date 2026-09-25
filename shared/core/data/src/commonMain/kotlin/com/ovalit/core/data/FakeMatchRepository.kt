package com.ovalit.core.data

import com.ovalit.core.model.ActId
import com.ovalit.core.model.KillEvent
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Round
import com.ovalit.core.model.RoundEconomy
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.metrics
import kotlin.math.abs
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
private const val ROUNDS_TO_WIN = 13
private const val EXTRA_KILL_RATE = 0.35
private const val LATE_DEATH_RATE = 0.5
private const val TRADE_RATE = 0.4
private const val ASSIST_RATE = 0.3
private const val LEGSHOT_RATE = 0.08
private const val KILL_SCORE = 60

internal val Me = PlayerId("me")
internal const val MY_RIOT_ID = "오발러#KR1"
internal val MyCard = FakeCards[0]
private const val MY_TIER = 16

// 친구가 내 편으로 끼는 비율. S5의 "같이 한 경기"가 전체 경기 수가 되지 않게 한다.
private const val FRIEND_IN_MATCH_RATE = 0.2
private val FakeAct = ActId("fake-act")

// 최근 7일은 팬텀 헤드샷을 크게 올려서 무기 화면에 "요즘 잘 맞아요"가 뜨게 했다
private const val RECENT_PHANTOM_HEADSHOT_RATE = 0.45

private fun <T> Random.pick(items: List<T>, weight: (T) -> Double): T {
    var roll = nextDouble() * items.sumOf(weight)
    for (item in items) {
        roll -= weight(item)
        if (roll <= 0) return item
    }
    return items.last()
}

/** 누구의 경기인지입니다. 내 경기면 나, 친구 경기면 그 친구입니다. */
internal class Owner(val id: PlayerId, val riotId: String, val card: PlayerCardId, val tier: Int)

internal val Myself = Owner(Me, MY_RIOT_ID, MyCard, MY_TIER)

/**
 * @param withFriends 내 경기일 때만 켠다. 친구 경기에 다른 친구를 끼우면 같이 한 경기가 꼬인다.
 */
internal fun fakeMatches(
    now: Instant,
    seed: Int = SEED,
    withFriends: Boolean = true,
    owner: Owner = Myself,
): List<Match> {
    val random = Random(seed)
    // 팀 구성과 스코어보드는 난수를 따로 쓴다. 같은 난수에서 뽑으면 친구를 넣는 순간 내 경기 숫자가 다 바뀐다.
    val party = Random(seed + 1)
    val board = Random(seed + 2)
    return (0 until DAYS).flatMap { daysAgo ->
        List(random.nextInt(0, 3)) { index ->
            val allies = party.allies(withFriends)
            val enemies = party.enemies(exclude = allies)
            random.fakeMatch(
                id = MatchId("fake-$seed-$daysAgo-$index"),
                startedAt = now - daysAgo.days - 50.minutes * (index + 1),
                owner = owner,
                allies = allies.map { it.id },
                firstDuelRate = if (daysAgo < 7) RECENT_FIRST_DUEL_RATE else USUAL_FIRST_DUEL_RATE,
                recent = daysAgo < 7,
            ).withScoreboard(board, owner, allies, enemies, tier = owner.tier - if (daysAgo > 30) 1 else 0)
        }
    }
}

private fun Random.allies(withFriends: Boolean): List<FakePlayer> {
    val strangers = Strangers.shuffled(this).take(4)
    if (!withFriends) return strangers
    return strangers.mapIndexed { index, stranger ->
        val friend = FakeFriendIds.getOrNull(index)?.takeIf { nextDouble() < FRIEND_IN_MATCH_RATE }
        if (friend == null) stranger else FakeFriendProfiles.getValue(friend)
    }
}

private fun Random.enemies(exclude: List<FakePlayer>): List<FakePlayer> =
    (Strangers - exclude.toSet()).shuffled(this).take(5)

private fun Random.fakeMatch(
    id: MatchId,
    startedAt: Instant,
    owner: Owner,
    allies: List<PlayerId>,
    firstDuelRate: Double,
    recent: Boolean,
): Match {
    val agent = pick(MyAgents) { it.weight }.agent
    val queue = if (nextDouble() < 0.8) Queue.COMPETITIVE else Queue.UNRATED
    val map = FakeMaps.random(this)
    val startsOnAttack = nextBoolean()
    val economy = FakeEconomy(this)
    val rounds = mutableListOf<Round>()

    while (!isOver(rounds, queue)) {
        val number = rounds.size + 1
        val side = if (sideIndex(number) == 0 == startsOnAttack) Side.ATTACK else Side.DEFENSE
        val pool = if (number == 1 || number == HALF_ROUNDS + 1) FakePistols else FakeRifles
        rounds += fakeRound(
            number = number,
            me = owner.id,
            side = side,
            allies = allies,
            firstDuelRate = firstDuelRate,
            weapon = pick(pool) { it.weight },
            recent = recent,
            economy = economy.next(number),
        ).also { economy.record(it.won) }
    }

    return Match(
        id = id,
        queue = queue,
        act = FakeAct,
        map = map.id,
        startedAt = startedAt,
        lengthMillis = rounds.size * nextLong(90_000, 115_000) + 60_000,
        me = owner.id,
        myAgent = agent.id,
        myRole = agent.role,
        allies = allies.toSet(),
        myCombatScore = rounds.sumOf { round ->
            round.myDamage + KILL_SCORE * round.kills.count { it.killer == owner.id }
        },
        myTeamWon = rounds.count { it.won }.let { won ->
            when {
                won * 2 > rounds.size -> true
                won * 2 < rounds.size -> false
                else -> null
            }
        },
        roundOutcomes = rounds.map { it.won },
        rounds = rounds,
        players = emptyList(),
    )
}

// 경쟁전 연장은 두 라운드 차이가 날 때까지 간다. 일반전은 12:12에서 한 라운드로 끝낸다.
private fun isOver(rounds: List<Round>, queue: Queue): Boolean {
    val won = rounds.count { it.won }
    val lost = rounds.size - won
    val leader = maxOf(won, lost)
    return when (queue) {
        Queue.COMPETITIVE -> leader >= ROUNDS_TO_WIN && abs(won - lost) >= 2
        else -> leader >= ROUNDS_TO_WIN
    }
}

// 전반은 0, 후반은 1을 돌려준다. 연장은 라운드마다 공수가 바뀌어 0과 1을 오간다.
private fun sideIndex(number: Int): Int = when {
    number <= HALF_ROUNDS -> 0
    number <= HALF_ROUNDS * 2 -> 1
    else -> (number - HALF_ROUNDS * 2 - 1) % 2
}

/** 지난 라운드 결과로 이번 라운드 장비를 정합니다. 이기면 풀바이, 지면 이코와 포스바이를 오갑니다. */
private class FakeEconomy(private val random: Random) {
    private var myTeamWonLast: Boolean? = null
    private var myTeamSaved = false
    private var enemySaved = false

    fun next(number: Int): RoundEconomy {
        val pistol = number == 1 || number == HALF_ROUNDS + 1
        val overtime = number > HALF_ROUNDS * 2
        val team = when {
            pistol -> random.nextInt(700, 900)
            overtime -> random.nextInt(4_200, 4_900)
            else -> loadout(won = myTeamWonLast, saved = myTeamSaved)
        }
        val enemy = when {
            pistol -> random.nextInt(700, 900)
            overtime -> random.nextInt(4_200, 4_900)
            else -> loadout(won = myTeamWonLast?.not(), saved = enemySaved)
        }
        myTeamSaved = team < 2_000
        enemySaved = enemy < 2_000
        return RoundEconomy(
            myLoadout = (team + random.nextInt(-300, 300)).coerceAtLeast(0),
            teamLoadout = team,
            enemyLoadout = enemy,
        )
    }

    fun record(won: Boolean) {
        myTeamWonLast = won
    }

    private fun loadout(won: Boolean?, saved: Boolean): Int = when {
        won == true || saved -> if (random.nextDouble() < 0.85) random.nextInt(3_900, 4_800) else random.nextInt(2_200, 3_600)
        random.nextBoolean() -> random.nextInt(500, 1_600)
        else -> random.nextInt(2_200, 3_600)
    }
}

private fun Random.fakeRound(
    number: Int,
    me: PlayerId,
    side: Side,
    allies: List<PlayerId>,
    firstDuelRate: Double,
    weapon: FakeWeapon,
    recent: Boolean,
    economy: RoundEconomy,
): Round {
    val aliveEnemies = EnemySlots.shuffled(this).toMutableList()
    val kills = mutableListOf<KillEvent>()
    var myDeath: KillEvent? = null

    fun killEnemy(atMillis: Long, killer: PlayerId, assistants: Set<PlayerId> = emptySet()) {
        if (aliveEnemies.isEmpty()) return
        val used = if (killer == me) weapon.id else null
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
            killEnemy(15_000, killer = me)
        else -> myDeath = KillEvent(15_000, opener, me, emptySet(), weapon = null)
    }

    if (myDeath == null) {
        repeat(2) { if (nextDouble() < EXTRA_KILL_RATE) killEnemy(30_000L + 12_000L * it, killer = me) }
        if (aliveEnemies.isNotEmpty() && nextDouble() < LATE_DEATH_RATE) {
            myDeath = KillEvent(60_000, aliveEnemies.first(), me, emptySet(), weapon = null)
        }
    }
    myDeath?.let { death ->
        kills += death
        if (nextDouble() < TRADE_RATE && aliveEnemies.remove(death.killer)) {
            kills += KillEvent(death.atMillis + 2_000, allies.random(this), death.killer, emptySet(), weapon = null)
        }
    }
    if (nextDouble() < ASSIST_RATE) killEnemy(45_000, killer = allies.random(this), assistants = setOf(me))

    val myKills = kills.count { it.killer == me }
    val hits = myKills * 3 + nextInt(0, 6)
    val headshotRate = if (recent && weapon.name == "팬텀") RECENT_PHANTOM_HEADSHOT_RATE else weapon.headshotRate
    val head = (0 until hits).count { nextDouble() < headshotRate }
    val leg = (0 until hits - head).count { nextDouble() < LEGSHOT_RATE }
    val openedByMe = kills.minBy { it.atMillis }.killer == me
    val duel = if (openedByMe) 0.15 else if (myDeath?.atMillis == 15_000L) -0.15 else 0.0
    val gear = (economy.teamLoadout - economy.enemyLoadout) / 10_000.0
    val won = nextDouble() < (0.5 + duel + gear).coerceIn(0.1, 0.9)

    return Round(
        number = number,
        won = won,
        kills = kills.sortedBy { it.atMillis },
        myDamage = myKills * nextInt(110, 150) + nextInt(0, 70),
        myShots = Shots(head = head, body = hits - head - leg, leg = leg),
        mySide = side,
        ending = ending(won = won, attacking = side == Side.ATTACK),
        economy = economy,
    )
}

// 라운드를 만들 때는 적을 이 다섯 자리로만 구분한다. 실제 상대 ID는 스코어보드를 붙이면서 바꿔 넣는다.
private val EnemySlots = List(5) { PlayerId("enemy-$it") }

private fun Random.ending(won: Boolean, attacking: Boolean): RoundEnding {
    val attackersWon = won == attacking
    val roll = nextDouble()
    return when {
        attackersWon -> if (roll < 0.7) RoundEnding.ELIMINATION else RoundEnding.SPIKE_DETONATED
        roll < 0.6 -> RoundEnding.ELIMINATION
        roll < 0.85 -> RoundEnding.SPIKE_DEFUSED
        else -> RoundEnding.TIME_EXPIRED
    }
}

/**
 * 스코어보드를 붙입니다. 내 줄(친구 경기면 친구 줄)은 라운드에서 센 숫자를 그대로 쓰고, 나머지 아홉
 * 명은 그럴듯한 범위에서 뽑습니다. 킬 기록 속 적 자리(`enemy-0`…)도 실제 상대 ID로 바꿉니다.
 */
private fun Match.withScoreboard(
    random: Random,
    owner: Owner,
    allies: List<FakePlayer>,
    enemies: List<FakePlayer>,
    tier: Int,
): Match {
    val slotToEnemy = EnemySlots.zip(enemies.map { it.id }).toMap()
    fun PlayerId.real() = slotToEnemy[this] ?: this
    val renamed = rounds.map { round ->
        round.copy(kills = round.kills.map { it.copy(killer = it.killer.real(), victim = it.victim.real()) })
    }
    val mine = copy(rounds = renamed).metrics()
    val roundCount = roundOutcomes.size
    val agents = AgentPool.filter { it.id != myAgent }.shuffled(random)

    fun other(player: FakePlayer, onMyTeam: Boolean, index: Int): Scoreline {
        val won = if (onMyTeam) myTeamWon == true else myTeamWon == false
        val kills = (roundCount * random.nextDouble(0.45, 0.95) + if (won) 2 else 0).toInt()
        val adr = random.nextInt(95, 175)
        return Scoreline(
            player = player.id,
            riotId = player.riotId,
            agent = agents[index].id,
            onMyTeam = onMyTeam,
            tier = (tier + random.nextInt(-2, 3)).coerceIn(3, 27),
            playerCard = player.card,
            kills = kills,
            deaths = (roundCount * random.nextDouble(0.5, 0.85)).toInt(),
            assists = (roundCount * random.nextDouble(0.1, 0.45)).toInt(),
            combatScore = (adr + kills * 60 / roundCount.coerceAtLeast(1)) * roundCount,
            damage = adr * roundCount,
            roundsPlayed = roundCount,
        )
    }

    val me = Scoreline(
        player = owner.id,
        riotId = owner.riotId,
        agent = myAgent,
        onMyTeam = true,
        tier = tier,
        playerCard = owner.card,
        kills = mine.kills,
        deaths = mine.deaths,
        assists = mine.assists,
        combatScore = myCombatScore,
        damage = mine.damage,
        roundsPlayed = rounds.size,
    )
    return copy(
        rounds = renamed,
        players = listOf(me) +
            allies.mapIndexed { index, player -> other(player, onMyTeam = true, index = index) } +
            enemies.mapIndexed { index, player -> other(player, onMyTeam = false, index = index + allies.size) },
    )
}
