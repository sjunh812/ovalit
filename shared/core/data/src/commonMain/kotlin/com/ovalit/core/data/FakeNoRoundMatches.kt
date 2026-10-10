package com.ovalit.core.data

import com.ovalit.core.model.AgentId
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchTeam
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.Shots
import kotlin.random.Random
import kotlin.time.Instant

// 라운드가 없는 가짜 경기다. 데스매치, 팀 데스매치, 건틀릿: 글리치의 응답 모양은 아직 못 봤다. Riot 문서대로 teams[]에
// 데스매치는 사람마다 한 줄(numPoints가 킬), 팀 모드는 팀마다 한 줄을 두고 라운드 기록은 비운다.

private const val DEATHMATCH_PLAYERS = 14
private const val DEATHMATCH_KILLS_TO_WIN = 40
private const val TEAM_DEATHMATCH_KILLS_TO_WIN = 100
private const val GAUNTLET_TEAMS = 8

// 건틀릿 경기장과 로봇은 카탈로그에 없다. 이름을 지어 붙이지 않고 화면이 "알 수 없는 맵"과 빈 얼굴로 그리는지 본다.
private val GauntletArena = MapId("fake-gauntlet-arena")
private val GauntletRobots = List(4) { AgentId("fake-gauntlet-robot-$it") }

/** 데스매치입니다. 열네 명이 각자 싸우고 40킬을 먼저 채운 사람이 1등입니다. 나는 대개 중간쯤입니다. */
internal fun Random.fakeDeathmatch(id: MatchId, startedAt: Instant, owner: Owner): Match {
    val agent = AgentPool.random(this)
    val others = Strangers.shuffled(this).take(DEATHMATCH_PLAYERS - 1)
    val winner = nextInt(others.size)
    val myKills = nextInt(18, 37)
    val players = listOf(
        noRoundLine(owner.id, owner.riotId, agent.id, onMyTeam = true, kills = myKills, deaths = nextInt(20, 34), card = owner.card, tier = owner.tier),
    ) + others.mapIndexed { index, player ->
        val kills = if (index == winner) DEATHMATCH_KILLS_TO_WIN else nextInt(8, DEATHMATCH_KILLS_TO_WIN)
        noRoundLine(player.id, player.riotId, AgentPool.random(this).id, onMyTeam = false, kills = kills, deaths = nextInt(15, 36), card = player.card, tier = owner.tier)
    }
    return noRoundMatch(id, Queue.DEATHMATCH, FakeMaps.random(this).id, startedAt, owner, agent.id, players).copy(
        myRole = agent.role,
        teams = players.map { MatchTeam(members = setOf(it.player), won = it.kills == DEATHMATCH_KILLS_TO_WIN, points = it.kills) },
        myTeamWon = myKills == DEATHMATCH_KILLS_TO_WIN,
    )
}

/** 팀 데스매치입니다. 다섯 명씩 다시 살아나며 100킬을 먼저 채운 팀이 이깁니다. */
internal fun Random.fakeTeamDeathmatch(id: MatchId, startedAt: Instant, owner: Owner): Match {
    val agent = AgentPool.random(this)
    val strangers = Strangers.shuffled(this)
    val won = nextBoolean()
    val loserPoints = nextInt(60, TEAM_DEATHMATCH_KILLS_TO_WIN)
    val myPoints = if (won) TEAM_DEATHMATCH_KILLS_TO_WIN else loserPoints
    val theirPoints = if (won) loserPoints else TEAM_DEATHMATCH_KILLS_TO_WIN
    val mine = listOf(FakePlayer(owner.id, owner.riotId, owner.card)) + strangers.take(4)
    val theirs = strangers.drop(4).take(5)
    fun team(people: List<FakePlayer>, points: Int, enemyPoints: Int, onMyTeam: Boolean) =
        people.zip(split(points, people.size)).zip(split(enemyPoints, people.size)) { (player, kills), deaths ->
            val playerAgent = if (player.id == owner.id) agent else AgentPool.random(this)
            noRoundLine(player.id, player.riotId, playerAgent.id, onMyTeam, kills, deaths, card = player.card, tier = owner.tier)
        }
    val players = team(mine, myPoints, theirPoints, onMyTeam = true) + team(theirs, theirPoints, myPoints, onMyTeam = false)
    return noRoundMatch(id, Queue.TEAM_DEATHMATCH, FakeTeamDeathmatchMaps.random(this).id, startedAt, owner, agent.id, players).copy(
        myRole = agent.role,
        allies = mine.drop(1).map { it.id }.toSet(),
        teams = listOf(
            MatchTeam(members = mine.map { it.id }.toSet(), won = won, points = myPoints),
            MatchTeam(members = theirs.map { it.id }.toSet(), won = !won, points = theirPoints),
        ),
        myTeamWon = won,
    )
}

/**
 * 건틀릿: 글리치입니다. 두 명씩 여덟 팀이 2대2 대진을 이어 가며 마지막 한 팀이 남을 때까지 싸웁니다. 요원 대신 로봇을 써서
 * 역할이 없습니다. 응답이 등수를 어떻게 주는지 몰라 등수를 그대로 담습니다.
 */
internal fun Random.fakeGauntlet(id: MatchId, startedAt: Instant, owner: Owner): Match {
    val robot = GauntletRobots.random(this)
    val pairs = (listOf(FakePlayer(owner.id, owner.riotId, owner.card)) + Strangers.shuffled(this).take(GAUNTLET_TEAMS * 2 - 1)).chunked(2)
    val placements = (1..GAUNTLET_TEAMS).shuffled(this)
    val players = pairs.flatMapIndexed { team, pair ->
        // 위로 올라간 팀일수록 대진을 많이 치러 킬도 많다
        val brackets = bracketWins(placements[team]) + 1
        pair.map { player ->
            val playerRobot = if (player.id == owner.id) robot else GauntletRobots.random(this)
            noRoundLine(player.id, player.riotId, playerRobot, onMyTeam = team == 0, kills = brackets * nextInt(2, 5), deaths = brackets * nextInt(1, 4), card = player.card, tier = owner.tier)
        }
    }
    return noRoundMatch(id, Queue.OTHER, GauntletArena, startedAt, owner, robot, players).copy(
        allies = pairs.first().drop(1).map { it.id }.toSet(),
        teams = pairs.mapIndexed { team, pair ->
            MatchTeam(members = pair.map { it.id }.toSet(), won = placements[team] == 1, points = bracketWins(placements[team]), placement = placements[team])
        },
        myTeamWon = placements.first() == 1,
    )
}

// 여덟 팀 토너먼트에서 그 등수까지 이긴 대진 수다. 1등은 셋, 2등은 둘, 3·4등은 하나다.
private fun bracketWins(placement: Int): Int = when (placement) {
    1 -> 3
    2 -> 2
    3, 4 -> 1
    else -> 0
}

// 팀 점수를 사람마다 들쭉날쭉하게 나눈다. 다 더하면 팀 점수와 같다.
private fun Random.split(total: Int, people: Int): List<Int> {
    val weights = List(people) { nextDouble(0.6, 1.4) }
    val shares = weights.map { (total * it / weights.sum()).toInt() }
    return shares.mapIndexed { index, share -> if (index == 0) share + total - shares.sum() else share }
}

// 응답이 한 판을 한 라운드로 줄 수도 있어 뛴 라운드를 1로 둔다. 화면이 전투점수를 나누지 않는지 이 경우로 본다.
private fun Random.noRoundLine(
    player: PlayerId,
    riotId: String,
    agent: AgentId,
    onMyTeam: Boolean,
    kills: Int,
    deaths: Int,
    card: PlayerCardId,
    tier: Int,
): Scoreline {
    val hits = kills * nextInt(3, 6)
    val head = (hits * nextDouble(0.15, 0.35)).toInt()
    val leg = (hits * nextDouble(0.03, 0.1)).toInt()
    return Scoreline(
        player = player,
        riotId = riotId,
        agent = agent,
        onMyTeam = onMyTeam,
        tier = tier,
        playerCard = card,
        kills = kills,
        deaths = deaths,
        assists = nextInt(0, 6),
        combatScore = kills * nextInt(180, 240),
        damage = kills * nextInt(130, 170),
        roundsPlayed = 1,
        shots = Shots(head = head, body = hits - head - leg, leg = leg),
    )
}

private fun Random.noRoundMatch(
    id: MatchId,
    queue: Queue,
    map: MapId,
    startedAt: Instant,
    owner: Owner,
    agent: AgentId,
    players: List<Scoreline>,
) = Match(
    id = id,
    queue = queue,
    act = FakeAct,
    map = map,
    startedAt = startedAt,
    lengthMillis = nextLong(420_000, 600_000),
    me = owner.id,
    myAgent = agent,
    myRole = null,
    allies = emptySet(),
    myCombatScore = players.first { it.player == owner.id }.combatScore,
    myTeamWon = null,
    roundOutcomes = emptyList(),
    rounds = emptyList(),
    players = players,
)
