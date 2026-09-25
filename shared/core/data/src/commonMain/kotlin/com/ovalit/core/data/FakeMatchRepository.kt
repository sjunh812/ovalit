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
// 수비에서 첫 교전을 더 자주 져서 홈에 개선 포인트 문장이 뜬다. 평균은 0.55다.
private const val ATTACK_FIRST_DUEL_WIN_RATE = 0.66
private const val DEFENSE_FIRST_DUEL_WIN_RATE = 0.44
private const val HALF_ROUNDS = 12
private const val EXTRA_KILL_RATE = 0.35
private const val LATE_DEATH_RATE = 0.5
private const val TRADE_RATE = 0.4
private const val ASSIST_RATE = 0.3
private const val HEADSHOT_RATE = 0.22
private const val LEGSHOT_RATE = 0.08
private const val KILL_SCORE = 60

private val Me = PlayerId("me")
private val Allies = List(4) { PlayerId("ally-$it") }
private val Enemies = List(5) { PlayerId("enemy-$it") }
private val FakeAct = ActId("fake-act")

internal fun fakeMatches(now: Instant): List<Match> {
    val random = Random(SEED)
    return (0 until DAYS).flatMap { daysAgo ->
        List(random.nextInt(0, 3)) { index ->
            random.fakeMatch(
                id = MatchId("fake-$daysAgo-$index"),
                startedAt = now - daysAgo.days - 50.minutes * (index + 1),
                firstDuelRate = if (daysAgo < 7) RECENT_FIRST_DUEL_RATE else USUAL_FIRST_DUEL_RATE,
            )
        }
    }
}

private fun Random.fakeMatch(id: MatchId, startedAt: Instant, firstDuelRate: Double): Match {
    val startsOnAttack = nextBoolean()
    val rounds = List(nextInt(18, 25)) { index ->
        val firstHalf = index < HALF_ROUNDS
        val side = if (firstHalf == startsOnAttack) Side.ATTACK else Side.DEFENSE
        fakeRound(number = index + 1, side = side, firstDuelRate = firstDuelRate)
    }

    return Match(
        id = id,
        queue = if (nextDouble() < 0.8) Queue.COMPETITIVE else Queue.UNRATED,
        act = FakeAct,
        startedAt = startedAt,
        me = Me,
        myAgent = AgentId("fake-agent"),
        myRole = if (nextDouble() < 0.8) Role.DUELIST else Role.INITIATOR,
        allies = Allies.toSet(),
        myCombatScore = rounds.sumOf { round ->
            round.myDamage + KILL_SCORE * round.kills.count { it.killer == Me }
        },
        rounds = rounds,
    )
}

private fun Random.fakeRound(number: Int, side: Side, firstDuelRate: Double): Round {
    val aliveEnemies = Enemies.shuffled(this).toMutableList()
    val kills = mutableListOf<KillEvent>()
    var myDeath: KillEvent? = null

    fun killEnemy(atMillis: Long, killer: PlayerId, assistants: Set<PlayerId> = emptySet()) {
        if (aliveEnemies.isEmpty()) return
        kills += KillEvent(atMillis, killer, aliveEnemies.removeAt(0), assistants, weapon = null)
    }

    val opener = aliveEnemies.first()
    when {
        nextDouble() >= firstDuelRate -> if (nextBoolean()) {
            killEnemy(15_000, killer = Allies.random(this))
        } else {
            kills += KillEvent(15_000, opener, Allies.random(this), emptySet(), weapon = null)
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
            kills += KillEvent(death.atMillis + 2_000, Allies.random(this), death.killer, emptySet(), weapon = null)
        }
    }
    if (nextDouble() < ASSIST_RATE) killEnemy(45_000, killer = Allies.random(this), assistants = setOf(Me))

    val myKills = kills.count { it.killer == Me }
    val hits = myKills * 3 + nextInt(0, 6)
    val head = (0 until hits).count { nextDouble() < HEADSHOT_RATE }
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
