package com.ovalit.core.data

import com.ovalit.core.model.ActId
import com.ovalit.core.model.ImportProgress
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
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.metrics
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 프로덕션 키가 나오기 전까지 쓰는 내 경기 저장소입니다.
 * 받기 규칙은 [OfflineFirstMatchRepository]가 지키고, 여기서는 가짜 서버([FakeMatchRemoteSource])와 메모리 저장([InMemoryMatchStore])을 묶기만 합니다.
 *
 * 처음부터 첫 수집을 마친 채로 시작합니다. 실제 저장소도 앱을 다시 켜면 마친 진행도를 기기에서 읽습니다.
 * 비워 두면 첫 수집을 마쳤는지 보는 곳(새 경기 확인, 당겨서 받기)마다 판단이 갈립니다.
 *
 * @param importDelay 첫 수집에서 한 판을 받는 시간입니다.
 * @param downloadDelay 새 경기 한 판을 받는 시간입니다.
 * @param scope 새 경기를 받는 곳입니다. 부른 화면이 사라져도 받기가 끝까지 가도록 앱이 사는 동안 도는 스코프를 넘깁니다.
 *   테스트는 가상 시간으로 돌리려고 `TestScope`를 넘깁니다.
 */
class FakeMatchRepository private constructor(
    private val remote: FakeMatchRemoteSource,
    private val repository: OfflineFirstMatchRepository,
) : MatchRepository by repository {

    constructor(
        clock: Clock = Clock.System,
        importDelay: Duration = 70.milliseconds,
        downloadDelay: Duration = 500.milliseconds,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    ) : this(FakeMatchRemoteSource(clock, importDelay, downloadDelay), clock, scope)

    private constructor(remote: FakeMatchRemoteSource, clock: Clock, scope: CoroutineScope) : this(
        remote,
        OfflineFirstMatchRepository(remote, importedStore(remote.startImported()), clock, scope),
    )

    /** 가짜 서버도 끝난 경기를 잊게 합니다. 지운 뒤 다시 받는 첫 수집은 늘 같은 50경기를 받습니다. */
    override suspend fun deleteAll() {
        repository.deleteAll()
        remote.forget()
    }
}

private fun importedStore(matches: List<Match>) = InMemoryMatchStore(
    matches = matches,
    importProgress = ImportProgress(total = matches.size, results = matches.map { it.myTeamWon }),
)

internal const val SEED = 923
private const val DAYS = 70

private const val USUAL_FIRST_DUEL_RATE = 0.28
private const val RECENT_FIRST_DUEL_RATE = 0.42
// 수비에서 첫 교전을 더 자주 지게 해서 홈에 개선 포인트 문장이 뜨게 했다. 둘의 평균은 0.55다.
// 첫 교전은 판마다 몇 번뿐이라 첫 수집 50경기에서도 우연 거르기(흔들림의 3.5배)를 넘도록 넉넉히 벌렸다.
private const val ATTACK_FIRST_DUEL_WIN_RATE = 0.72
private const val DEFENSE_FIRST_DUEL_WIN_RATE = 0.38
// 최근 7일은 첫 교전을 공수 같게 둔다.
// 첫 교전 차이가 아래 멀티킬 차이보다 크면 에임 올리기를 골라도 첫 교전 문장이 앞선다.
private const val RECENT_FIRST_DUEL_WIN_RATE = 0.55
private const val HALF_ROUNDS = 12
private const val EXTRA_KILL_RATE = 0.35
private const val LATE_DEATH_RATE = 0.5
private const val TRADE_RATE = 0.4
private const val ASSIST_RATE = 0.3
private const val LEGSHOT_RATE = 0.08
private const val KILL_SCORE = 60
private const val ACE_RATE = 0.02
private const val CLUTCH_RATE = 0.06

internal val Me = PlayerId("me")
internal const val MY_RIOT_ID = "오발러#KR1"
internal val MyCard = FakeCards[0]
private const val MY_TIER = 16

// 친구가 내 편으로 끼는 비율. S5의 "같이 뛴 경기"가 전체 경기 수가 되지 않게 한다.
private const val FRIEND_IN_MATCH_RATE = 0.2
internal val FakeAct = ActId("fake-act")

// 최근 7일은 팬텀 헤드샷을 크게 올려서 무기 화면에 "요즘 잘 맞아요"가 뜨게 했다
private const val RECENT_PHANTOM_HEADSHOT_RATE = 0.45

// 최근 7일은 공격에서만 교전을 이어 이기고 수비에서는 첫 교전 뒤로 거의 못 잡는다.
// 멀티킬 라운드 비율이 공수로 크게 벌어져서 에임 올리기를 고르면 개선 포인트에 멀티킬 문장이 뜬다.
// 평균 킬은 평소보다 적어 K/D는 내려간다.
private const val RECENT_ATTACK_EXTRA_KILL_RATE = 0.5
private const val RECENT_DEFENSE_EXTRA_KILL_RATE = 0.05

// 최근 7일은 맞히고도 마무리하지 못한 피해가 늘어 피해량이 오른다.
// 홈 "이번 주 짚을 점"이 헤드샷 말고 다른 지표로도 뜨는지 보려고 넣었다.
// 이긴 판도 조금 늘려서 짚을 점에 요원 줄이 뜨게 했다.
private const val RECENT_CHIP_DAMAGE = 90
private const val RECENT_WIN_BONUS = 0.04

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
 * 프로덕션 키가 나오기 전까지 화면에 띄울 가짜 경기입니다.
 *
 * 시드가 고정이라 매번 같은 경기가 나오고 날짜만 [now]를 따라 움직입니다.
 * 홈에 움직인 지표가 뜨도록 최근 7일은 첫 교전, 공격과 수비의 킬, 팬텀 헤드샷, 피해량, 승률을 일부러 바꿔 뒀습니다(`RECENT_`로 시작하는 상수).
 * 최근 두 주에는 데스매치, 건틀릿, 스파이크 돌격 같은 다른 모드를 몇 판 섞어 기타 칩과 경기 탭에서 모양을 볼 수 있습니다([OtherModeDays]).
 *
 * @param withFriends 내 경기일 때만 켭니다. 친구 경기에 다른 친구를 끼우면 S5의 같이 뛴 경기 수가 틀어집니다.
 */
internal fun fakeMatches(
    now: Instant,
    seed: Int = SEED,
    withFriends: Boolean = true,
    owner: Owner = Myself,
): List<Match> {
    val random = Random(seed)
    // 팀 구성, 스코어보드, 에이스·클러치 장면은 난수를 따로 쓴다.
    // 같은 난수에서 뽑으면 친구를 넣는 순간 내 경기 숫자가 다 바뀐다.
    val party = Random(seed + 1)
    val board = Random(seed + 2)
    val scenes = Random(seed + 3)
    val modes = Random(seed + 4)
    val main = (0 until DAYS).flatMap { daysAgo ->
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
            ).withHighlights(scenes).withScoreboard(board, owner, allies, enemies, tier = owner.tier - if (daysAgo > 30) 1 else 0)
        }
    }
    val others = OtherModeDays.map { (daysAgo, queue) ->
        modes.otherModeMatch(
            id = MatchId("fake-$seed-$daysAgo-${queue.name.lowercase()}"),
            startedAt = now - daysAgo.days - OTHER_MODE_EARLIER,
            owner = owner,
            queue = queue,
        )
    }
    return main + others
}

/**
 * 경쟁·일반 밖의 모드를 섞을 날(며칠 전)과 모드입니다.
 * 첫 수집이 최근 50경기라 더 넣으면 홈 리포트에 쓸 경쟁·일반 경기가 그만큼 빠집니다.
 * 리포트에 넣는 모드(스파이크 돌격, 신속 플레이, 프리미어)가 다섯 판이라 기타 칩 리포트도 나옵니다.
 * 건틀릿: 글리치는 큐 ID를 몰라 [Queue.OTHER]로 옵니다.
 */
internal val OtherModeDays = listOf(
    0 to Queue.DEATHMATCH,
    1 to Queue.OTHER,
    2 to Queue.SPIKE_RUSH,
    3 to Queue.TEAM_DEATHMATCH,
    5 to Queue.SWIFTPLAY,
    6 to Queue.PREMIER,
    9 to Queue.SPIKE_RUSH,
    12 to Queue.SWIFTPLAY,
)

// 경쟁·일반 경기는 그날 마지막 판부터 50분씩 앞에 둔다. 다른 모드는 그보다 앞, 몸 풀기로 뛴 판이다.
private val OTHER_MODE_EARLIER = 150.minutes

private fun Random.otherModeMatch(id: MatchId, startedAt: Instant, owner: Owner, queue: Queue): Match = when (queue) {
    Queue.DEATHMATCH -> fakeDeathmatch(id, startedAt, owner)
    Queue.TEAM_DEATHMATCH -> fakeTeamDeathmatch(id, startedAt, owner)
    Queue.OTHER -> fakeGauntlet(id, startedAt, owner)
    else -> {
        val allies = allies(withFriends = false)
        val enemies = enemies(exclude = allies)
        fakeMatch(
            id = id,
            startedAt = startedAt,
            owner = owner,
            allies = allies.map { it.id },
            firstDuelRate = USUAL_FIRST_DUEL_RATE,
            recent = false,
            queue = queue,
        ).withHighlights(this).withScoreboard(this, owner, allies, enemies, tier = owner.tier)
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
    queue: Queue? = null,
): Match {
    val agent = pick(MyAgents) { it.weight }.agent
    val queue = queue ?: if (nextDouble() < 0.8) Queue.COMPETITIVE else Queue.UNRATED
    val half = queue.halfRounds ?: HALF_ROUNDS
    val map = FakeMaps.random(this)
    val startsOnAttack = nextBoolean()
    val economy = FakeEconomy(this, half)
    val rounds = mutableListOf<Round>()

    while (!isOver(rounds, queue, half)) {
        val number = rounds.size + 1
        val side = if (sideIndex(number, half) == 0 == startsOnAttack) Side.ATTACK else Side.DEFENSE
        val pool = if (number == 1 || number == half + 1) FakePistols else FakeRifles
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

// 경쟁전과 프리미어 연장은 두 라운드 차이가 날 때까지 간다.
// 일반전은 12:12에서 한 라운드로 끝내고, 스파이크 돌격과 신속 플레이도 마지막 한 라운드로 끝난다.
private fun isOver(rounds: List<Round>, queue: Queue, half: Int): Boolean {
    val won = rounds.count { it.won }
    val lost = rounds.size - won
    val leader = maxOf(won, lost)
    return when (queue) {
        Queue.COMPETITIVE, Queue.PREMIER -> leader > half && abs(won - lost) >= 2
        else -> leader > half
    }
}

// 전반은 0, 후반은 1을 돌려준다. 연장은 라운드마다 공수가 바뀌어 0과 1을 오간다.
private fun sideIndex(number: Int, half: Int): Int = when {
    number <= half -> 0
    number <= half * 2 -> 1
    else -> (number - half * 2 - 1) % 2
}

/** 지난 라운드 결과로 이번 라운드 장비를 정합니다. 이겼거나 지난 라운드에 아꼈으면 대개 풀바이, 지면 이코나 포스바이입니다. */
private class FakeEconomy(private val random: Random, private val half: Int) {
    private var myTeamWonLast: Boolean? = null
    private var myTeamSaved = false
    private var enemySaved = false

    fun next(number: Int): RoundEconomy {
        val pistol = number == 1 || number == half + 1
        val overtime = number > half * 2
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
        nextDouble() < when {
            recent -> RECENT_FIRST_DUEL_WIN_RATE
            side == Side.ATTACK -> ATTACK_FIRST_DUEL_WIN_RATE
            else -> DEFENSE_FIRST_DUEL_WIN_RATE
        } ->
            killEnemy(15_000, killer = me)
        else -> myDeath = KillEvent(15_000, opener, me, emptySet(), weapon = null)
    }

    if (myDeath == null) {
        val extraKillRate = when {
            !recent -> EXTRA_KILL_RATE
            side == Side.ATTACK -> RECENT_ATTACK_EXTRA_KILL_RATE
            else -> RECENT_DEFENSE_EXTRA_KILL_RATE
        }
        repeat(2) { if (nextDouble() < extraKillRate) killEnemy(30_000L + 12_000L * it, killer = me) }
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
    val bonus = if (recent) RECENT_WIN_BONUS else 0.0
    val won = nextDouble() < (0.5 + duel + gear + bonus).coerceIn(0.1, 0.9)

    return Round(
        number = number,
        won = won,
        kills = kills.sortedBy { it.atMillis },
        myDamage = myKills * nextInt(110, 150) + nextInt(0, 70) + if (recent) RECENT_CHIP_DAMAGE else 0,
        myShots = Shots(head = head, body = hits - head - leg, leg = leg),
        mySide = side,
        ending = ending(won = won, attacking = side == Side.ATTACK),
        economy = economy.copy(myWeapon = weapon.id),
    )
}

/**
 * 가끔 에이스와 클러치가 나오게 킬 기록을 바꿉니다.
 * 승패는 그대로 둬서 스코어가 바뀌지 않습니다. 난수를 따로 써서 나머지 가짜 숫자가 흔들리지 않게 합니다.
 *
 * 관여율, 생존율, 첫 킬 쪽 지표가 그대로인 라운드만 고릅니다.
 * 홈 동적 칸이 이 장면 때문에 움직이면 가짜 데이터로 "움직인 칸과 그대로인 칸"을 같이 보여줄 수 없습니다.
 * 이긴 장면은 내가 첫 킬을 내고 살아남은 라운드에, 진 장면은 킬도 어시스트도 트레이드도 없이 죽은 라운드에만 넣습니다.
 * 피해량과 맞힌 탄은 그대로라 그 라운드만 조금 어긋납니다.
 */
private fun Match.withHighlights(random: Random): Match {
    if (allies.size != 4) return this
    val team = allies.toList()
    return copy(
        rounds = rounds.map { round ->
            val roll = random.nextDouble()
            val weapon = round.economy?.myWeapon ?: return@map round
            when {
                roll < ACE_RATE && round.wonWithMyOpener(me) -> round.copy(kills = aceKills(weapon))
                roll >= ACE_RATE + CLUTCH_RATE -> round
                round.wonWithMyOpener(me) -> round.copy(kills = wonClutchKills(random, team, weapon))
                round.lostQuietly(me, allies) -> round.copy(kills = lostClutchKills(random, team))
                else -> round
            }
        },
    )
}

private fun Round.wonWithMyOpener(me: PlayerId): Boolean =
    won && kills.none { it.victim == me } && kills.minByOrNull { it.atMillis }?.killer == me

private fun Round.lostQuietly(me: PlayerId, allies: Set<PlayerId>): Boolean {
    val death = kills.firstOrNull { it.victim == me } ?: return false
    val traded = kills.any { it.victim == death.killer && it.killer in allies && it.atMillis - death.atMillis in 0..5_000 }
    return !won && kills.minByOrNull { it.atMillis } != death && kills.none { it.killer == me || me in it.assistants } && !traded
}

private fun Match.aceKills(weapon: WeaponId): List<KillEvent> =
    EnemySlots.mapIndexed { index, enemy -> KillEvent(12_000L + 9_000L * index, me, enemy, emptySet(), weapon) }

// 내가 첫 킬을 낸 뒤 우리 팀 넷이 차례로 쓰러지고, 나 혼자 남은 1~3명을 모두 잡는다
private fun Match.wonClutchKills(random: Random, team: List<PlayerId>, weapon: WeaponId): List<KillEvent> {
    val enemies = EnemySlots.shuffled(random)
    val left = random.nextInt(1, 4)
    val survivors = enemies.takeLast(left)
    val kills = mutableListOf(KillEvent(8_000L, me, enemies.first(), emptySet(), weapon))
    kills += teamFalls(random, team, downed = enemies.drop(1).dropLast(left), survivors = survivors)
    survivors.forEachIndexed { index, enemy -> kills += KillEvent(42_000L + 5_000L * index, me, enemy, emptySet(), weapon) }
    return kills.sortedBy { it.atMillis }
}

// 우리 팀 넷이 먼저 쓰러지고 나는 아무도 못 잡고 죽는다
private fun Match.lostClutchKills(random: Random, team: List<PlayerId>): List<KillEvent> {
    val enemies = EnemySlots.shuffled(random)
    val left = random.nextInt(1, 4)
    val survivors = enemies.takeLast(left)
    val kills = teamFalls(random, team, downed = enemies.dropLast(left), survivors = survivors).toMutableList()
    kills += KillEvent(55_000L, survivors.random(random), me, emptySet(), weapon = null)
    return kills.sortedBy { it.atMillis }
}

// 우리 팀이 차례로 [downed]를 한 명씩 잡고 쓰러진다. 우리 팀을 쓰러뜨리는 건 끝까지 살아남는 상대다.
private fun teamFalls(random: Random, team: List<PlayerId>, downed: List<PlayerId>, survivors: List<PlayerId>): List<KillEvent> =
    team.flatMapIndexed { index, ally ->
        val at = 10_000L + 6_000L * index
        listOfNotNull(
            downed.getOrNull(index)?.let { KillEvent(at, ally, it, emptySet(), weapon = null) },
            KillEvent(at + 3_000L, survivors.random(random), ally, emptySet(), weapon = null),
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
 * 스코어보드를 붙입니다.
 * 내 줄(친구 경기면 친구 줄)은 라운드에서 센 숫자를 그대로 쓰고, 나머지 아홉 명은 그럴듯한 범위에서 뽑습니다.
 * 킬 기록 속 적 자리(`enemy-0`…)도 실제 상대 ID로 바꿉니다.
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
    val enemyIds = enemies.map { it.id }
    val renamed = rounds.map { round ->
        round.copy(kills = round.kills.map { it.copy(killer = it.killer.real(), victim = it.victim.real()) })
            .withDamageMaps(random, owner.id, enemyIds)
    }
    val mine = copy(rounds = renamed).metrics()
    val roundCount = roundOutcomes.size
    val agents = AgentPool.filter { it.id != myAgent }.shuffled(random)

    fun other(player: FakePlayer, onMyTeam: Boolean, index: Int): Scoreline {
        val won = if (onMyTeam) myTeamWon == true else myTeamWon == false
        val kills = (roundCount * random.nextDouble(0.5, 1.05) + if (won) 2 else 0).toInt()
        val adr = random.nextInt(105, 215)
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
            // 내 줄과 같은 식으로 센다. 식이 다르면 늘 내가 MVP가 된다.
            combatScore = adr * roundCount + KILL_SCORE * kills,
            damage = adr * roundCount,
            roundsPlayed = roundCount,
            shots = randomShots(random, roundCount),
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
        shots = renamed.fold(Shots.None) { total, round -> total + round.myShots },
    )
    return copy(
        rounds = renamed,
        players = listOf(me) +
            allies.mapIndexed { index, player -> other(player, onMyTeam = true, index = index) } +
            enemies.mapIndexed { index, player -> other(player, onMyTeam = false, index = index + allies.size) },
    )
}

// 다른 사람의 맞힌 부위다. 라운드마다 여섯에서 열한 발쯤 맞히고 그중 헤드샷이 12~32%다.
private fun randomShots(random: Random, rounds: Int): Shots {
    val total = rounds * random.nextInt(6, 12)
    val head = (total * random.nextDouble(0.12, 0.32)).toInt()
    val leg = (total * random.nextDouble(0.03, 0.1)).toInt()
    return Shots(head = head, body = total - head - leg, leg = leg)
}


// 상대마다 주고받은 피해다. 내가 잡은 상대에게는 한 번에 110~150을, 남은 피해는 아무 상대에게 준다.
// 내가 죽었으면 잡은 상대에게 100~150을 받고, 가끔 다른 상대에게도 조금 받는다.
private fun Round.withDamageMaps(random: Random, me: PlayerId, enemies: List<PlayerId>): Round {
    if (enemies.isEmpty()) return this
    val dealt = mutableMapOf<PlayerId, Int>()
    kills.filter { it.killer == me && it.victim in enemies }.forEach { dealt[it.victim] = (dealt[it.victim] ?: 0) + random.nextInt(110, 151) }
    val left = myDamage - dealt.values.sum()
    if (left > 0) enemies.random(random).let { dealt[it] = (dealt[it] ?: 0) + left }
    val taken = mutableMapOf<PlayerId, Int>()
    kills.firstOrNull { it.victim == me && it.killer in enemies }?.let { taken[it.killer] = random.nextInt(100, 151) }
    if (random.nextDouble() < 0.6) enemies.random(random).let { taken[it] = (taken[it] ?: 0) + random.nextInt(20, 81) }
    return copy(myDamageTo = dealt, myDamageFrom = taken)
}
