package com.ovalit.core.model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

internal val Me = PlayerId("me")
internal val Ally = PlayerId("ally")
internal val OtherAlly = PlayerId("other-ally")
internal val Enemy = PlayerId("enemy")
internal val OtherEnemy = PlayerId("other-enemy")

internal fun match(
    vararg rounds: Round,
    combatScore: Int = 0,
    queue: Queue = Queue.COMPETITIVE,
    act: ActId = ActId("act"),
    startedAt: Instant = Instant.fromEpochMilliseconds(0),
    role: Role? = null,
    agent: AgentId = AgentId("agent"),
    won: Boolean? = null,
    players: List<Scoreline> = emptyList(),
) = Match(
    id = MatchId("match"),
    queue = queue,
    act = act,
    map = MapId("map"),
    startedAt = startedAt,
    lengthMillis = 0,
    me = Me,
    myAgent = agent,
    myRole = role,
    allies = setOf(Ally, OtherAlly),
    myCombatScore = combatScore,
    myTeamWon = won,
    roundOutcomes = rounds.map { it.won },
    rounds = rounds.toList(),
    players = players,
)

internal fun round(
    vararg kills: KillEvent,
    damage: Int = 0,
    shots: Shots = Shots.None,
    won: Boolean = true,
    side: Side? = null,
    number: Int = 1,
    teamLoadout: Int? = null,
) = Round(
    number = number,
    won = won,
    kills = kills.toList(),
    myDamage = damage,
    myShots = shots,
    mySide = side,
    ending = null,
    economy = teamLoadout?.let { RoundEconomy(myLoadout = it, teamLoadout = it, enemyLoadout = it) },
)

/** 아무 일도 없이 끝난 라운드. 나는 살아남는다. */
internal fun quietRound() = round()

internal fun kill(
    atSeconds: Double,
    killer: PlayerId,
    victim: PlayerId,
    assistedBy: Set<PlayerId> = emptySet(),
) = KillEvent(
    atMillis = (atSeconds * 1000).toLong(),
    killer = killer,
    victim = victim,
    assistants = assistedBy,
    weapon = null,
)

internal fun assertRate(expected: Double, actual: Double?) {
    assertNotNull(actual, "비율이 null이다. 분모가 0인지 확인할 것")
    assertEquals(expected, actual, absoluteTolerance = 0.0001)
}
