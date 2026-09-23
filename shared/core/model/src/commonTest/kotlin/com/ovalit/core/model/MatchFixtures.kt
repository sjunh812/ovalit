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
) = Match(
    id = MatchId("match"),
    queue = Queue.COMPETITIVE,
    act = ActId("act"),
    startedAt = Instant.fromEpochMilliseconds(0),
    me = Me,
    myAgent = AgentId("agent"),
    allies = setOf(Ally, OtherAlly),
    myCombatScore = combatScore,
    rounds = rounds.toList(),
)

internal fun round(
    vararg kills: KillEvent,
    damage: Int = 0,
    shots: Shots = Shots.None,
    won: Boolean = true,
) = Round(
    number = 1,
    won = won,
    kills = kills.toList(),
    myDamage = damage,
    myShots = shots,
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
