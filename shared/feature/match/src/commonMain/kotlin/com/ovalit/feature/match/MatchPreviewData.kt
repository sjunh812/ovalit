package com.ovalit.feature.match

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.KillEvent
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchTeam
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.Round
import com.ovalit.core.model.RoundEconomy
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.buyRecords
import com.ovalit.core.model.roundSummaries
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// 프리뷰와 UI 테스트가 같이 쓴다. 목업 S3의 어센트 13 – 9 경기에 맞췄다.
// ID는 번들 이미지가 붙도록 카탈로그 UUID를 쓴다.
internal object MatchPreviewData {

    private val seoul = TimeZone.of("Asia/Seoul")
    val now: Instant = Instant.parse("2026-09-24T13:00:00Z")

    private val me = PlayerId("me")
    val junho = PlayerId("junho")
    val bloom = PlayerId("bloom")
    val hwan = PlayerId("hwan")

    private val ascent = MapId("7eaecc1b-4337-bbf6-6ab9-04b8f06b3319")
    private val haven = MapId("2bee0dc9-4ffe-519b-1cbd-7fbe763a6047")
    private val jett = AgentId("add6443a-41bd-e414-f6ad-e58d267f4e95")
    private val omen = AgentId("8e253930-4c05-31dd-1b6c-968525494517")
    private val sova = AgentId("320b2a48-4d9b-a075-30f1-1f93a9b638fa")
    private val killjoy = AgentId("1e58de9c-4950-5125-93e9-a0aee9f98746")
    private val raze = AgentId("f94c3b30-42be-e959-889c-5aa313dba261")

    val catalog = ContentCatalog(
        agents = mapOf(jett to "제트", omen to "오멘", sova to "소바", killjoy to "킬조이", raze to "레이즈"),
        weapons = emptyMap(),
        maps = mapOf(ascent to "어센트", haven to "헤이븐"),
        tiers = mapOf(16 to "플래티넘 2", 19 to "다이아몬드 2"),
    )

    // 전반 8–4, 후반 5–5
    private val outcomes = "WWLWLLWWWLWW" + "LWLWLWLWWL"

    private fun line(id: PlayerId, riotId: String, agent: AgentId, onMyTeam: Boolean, k: Int, d: Int, a: Int, adr: Int) =
        Scoreline(id, riotId, agent, onMyTeam, tier = if (id == junho) 19 else 16, playerCard = null,
            kills = k, deaths = d, assists = a, combatScore = (adr + k * 3) * outcomes.length, damage = adr * outcomes.length,
            roundsPlayed = outcomes.length)

    val detailMatch = match(
        id = "ascent",
        map = ascent,
        startedAt = now - 2.hours,
        outcomes = outcomes,
        players = listOf(
            line(me, "오발러#KR1", jett, true, 21, 13, 5, 174),
            line(junho, "준호#KR1", omen, true, 18, 14, 3, 151),
            line(PlayerId("h"), "Hwan#KR2", sova, true, 14, 16, 9, 139),
            line(bloom, "bloom#1004", killjoy, true, 11, 15, 12, 121),
            line(PlayerId("n"), "난나야#KR1", raze, true, 13, 14, 7, 144),
            line(PlayerId("e1"), "민석#KR3", raze, false, 19, 15, 4, 163),
            line(hwan, "Ash#KR1", sova, false, 17, 16, 8, 147),
            line(PlayerId("e3"), "rev#9922", omen, false, 14, 17, 6, 129),
            line(PlayerId("e4"), "하늘#KR1", killjoy, false, 12, 18, 5, 118),
            line(PlayerId("e5"), "pixel#KR1", jett, false, 10, 19, 3, 102),
        ),
    ).withScenes()

    // 1라운드는 에이스, 2라운드는 준호가 첫 킬을 내고 쓰러진 뒤 내가 혼자 넷을 상대한 클러치다
    private fun Match.withScenes(): Match {
        val enemies = players.filterNot { it.onMyTeam }.map { it.player }
        val ace = enemies.mapIndexed { index, enemy -> KillEvent(12_000L + 8_000L * index, me, enemy, emptySet(), weapon = null) }
        val clutch = listOf(
            KillEvent(8_000, junho, enemies[0], emptySet(), weapon = null),
            KillEvent(14_000, enemies[1], junho, emptySet(), weapon = null),
        ) + enemies.drop(1).mapIndexed { index, enemy -> KillEvent(30_000L + 6_000L * index, me, enemy, emptySet(), weapon = null) }
        return copy(
            rounds = rounds.map { round ->
                when (round.number) {
                    1 -> round.copy(kills = ace)
                    2 -> round.copy(kills = clutch)
                    else -> round
                }
            },
        )
    }

    private fun relation(player: PlayerId) = when (player) {
        me -> PlayerRelation.ME
        junho -> PlayerRelation.FRIEND
        bloom -> PlayerRelation.APP_USER
        hwan -> PlayerRelation.REQUESTED_ME
        else -> PlayerRelation.NOT_APP_USER
    }

    private fun detailOf(match: Match) = MatchDetailUiState.Success(
        match = match,
        catalog = catalog,
        groups = match.scoreboardGroups(::relation),
        rounds = match.roundSummaries(),
        buys = match.buyRecords(),
        timeZone = seoul,
    )

    val detail = detailOf(detailMatch)

    private val strangers = listOf(
        "Hwan#KR2", "bloom#1004", "난나야#KR1", "Ash#KR1", "rev#9922", "하늘#KR1", "moonlight#KR3", "도윤#0412",
        "pixel#KR1", "새벽#KR2", "Ryu#KR5", "보라#KR1", "kite#7777", "태오#KR4", "Nova#KR2",
    )

    // 라운드가 없는 모드의 줄이다.
    // 응답이 한 판을 한 라운드로 주면 나눈 값이 경기 전체 점수가 되니 그 경우로 둔다.
    private fun noRoundLine(id: PlayerId, riotId: String, agent: AgentId, onMyTeam: Boolean, kills: Int, deaths: Int, assists: Int = 0) =
        Scoreline(id, riotId, agent, onMyTeam, tier = 16, playerCard = null, kills = kills, deaths = deaths, assists = assists,
            combatScore = kills * 150, damage = kills * 140, roundsPlayed = 1)

    // 데스매치 열네 명이다. 나는 킬 31로 3등이다. 응답의 팀은 사람마다 하나이고 점수가 곧 킬이다.
    val deathmatch: Match = run {
        val agents = listOf(jett, omen, sova, killjoy, raze)
        val others = strangers.take(13).mapIndexed { index, riotId ->
            val kills = if (index < 2) 40 - index * 6 else 29 - index
            noRoundLine(PlayerId("dm-$index"), riotId, agents[index % agents.size], onMyTeam = false, kills = kills, deaths = 24 + index % 5)
        }
        val players = listOf(noRoundLine(me, "오발러#KR1", jett, onMyTeam = true, kills = 31, deaths = 26)) + others
        noRoundMatch("deathmatch", Queue.DEATHMATCH, haven, now - 50.minutes, players).copy(
            teams = players.map { MatchTeam(members = setOf(it.player), won = it.kills == 40, points = it.kills) },
            myTeamWon = false,
        )
    }

    // 건틀릿: 글리치 두 명씩 여덟 팀이다. 나와 준호가 2등이다.
    // 로봇 요원과 경기장은 카탈로그에 없어 얼굴 자리가 빈 면이고 맵은 "알 수 없는 맵"이다.
    val gauntlet: Match = run {
        val robot = { index: Int -> AgentId("gauntlet-robot-${index % 4}") }
        val partners = listOf(me to "오발러#KR1", junho to "준호#KR1") +
            strangers.take(14).mapIndexed { index, riotId -> PlayerId("g-$index") to riotId }
        // 1등 팀이 맨 앞에 오도록 내 팀을 둘째 자리에 둔다
        val pairs = partners.chunked(2).let { listOf(it[1], it[0]) + it.drop(2) }
        val players = pairs.flatMapIndexed { team, pair ->
            pair.mapIndexed { index, (id, riotId) ->
                noRoundLine(id, riotId, robot(team * 2 + index), onMyTeam = team == 1, kills = 12 - team, deaths = 4 + team, assists = 2)
            }
        }
        noRoundMatch("gauntlet", Queue.OTHER, MapId("gauntlet-arena"), now - 26.hours - 20.minutes, players).copy(
            allies = setOf(junho),
            myAgent = robot(2),
            teams = pairs.mapIndexed { team, pair ->
                MatchTeam(members = pair.map { it.first }.toSet(), won = team == 0, points = 3 - minOf(team, 3), placement = team + 1)
            },
            myTeamWon = false,
        )
    }

    // 팀 데스매치는 라운드 없이 팀 킬 100을 먼저 채우면 이긴다
    private val teamDeathmatch: Match = run {
        val players = listOf(noRoundLine(me, "오발러#KR1", jett, onMyTeam = true, kills = 24, deaths = 17, assists = 6)) +
            strangers.take(9).mapIndexed { index, riotId ->
                noRoundLine(PlayerId("tdm-$index"), riotId, raze, onMyTeam = index < 4, kills = 22 - index, deaths = 18, assists = 4)
            }
        noRoundMatch("team-deathmatch", Queue.TEAM_DEATHMATCH, ascent, now - 27.hours, players).copy(
            teams = listOf(
                MatchTeam(members = players.filter { it.onMyTeam }.map { it.player }.toSet(), won = true, points = 100),
                MatchTeam(members = players.filterNot { it.onMyTeam }.map { it.player }.toSet(), won = false, points = 87),
            ),
            myTeamWon = true,
        )
    }

    val deathmatchDetail = detailOf(deathmatch)

    val gauntletDetail = detailOf(gauntlet)

    private fun noRoundMatch(id: String, queue: Queue, map: MapId, startedAt: Instant, players: List<Scoreline>) = Match(
        id = MatchId(id),
        queue = queue,
        act = ActId("act"),
        map = map,
        startedAt = startedAt,
        lengthMillis = 9.minutes.inWholeMilliseconds,
        me = me,
        myAgent = jett,
        myRole = null,
        allies = emptySet(),
        myCombatScore = 0,
        myTeamWon = null,
        roundOutcomes = emptyList(),
        rounds = emptyList(),
        players = players,
    )

    private val list = listOf(
        detailMatch,
        match("haven-1", haven, now - 3.hours, "LLWLLWLWLLWLLWLLLWLWL", won = false),
        match("ascent-2", ascent, now - 26.hours, "WWLWLWWLWWLWWLLWWWLWLW"),
        match("haven-3", haven, now - 27.hours, "WLWWLWWLLWWLWWWWLLW"),
    )

    val matches = MatchesUiState.Success(
        queueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
        filter = MatchFilter(),
        days = list.groupBy { it.startedAt.date() }.map { (date, matches) -> MatchDay(date, matches) },
        agents = listOf(jett),
        maps = listOf(ascent, haven),
        catalog = catalog,
        now = now,
        timeZone = seoul,
    )

    val empty = matches.copy(days = emptyList())

    // 기타 칩이다. 리포트에 넣는 스파이크 돌격과 목록에만 두는 데스매치, 건틀릿, 팀 데스매치가 섞인다.
    val otherMatches = matches.copy(
        queueFilter = QueueFilter.OTHER,
        days = listOf(
            deathmatch,
            match("spike-rush", ascent, now - 3.hours, "WLWWLW", queue = Queue.SPIKE_RUSH),
            gauntlet,
            teamDeathmatch,
        ).groupBy { it.startedAt.date() }.map { (date, matches) -> MatchDay(date, matches) },
    )

    private fun Instant.date(): LocalDate = toLocalDateTime(seoul).date

    private fun match(
        id: String,
        map: MapId,
        startedAt: Instant,
        outcomes: String,
        won: Boolean? = null,
        players: List<Scoreline> = listOf(line(me, "오발러#KR1", jett, true, 16, 14, 6, 140)),
        queue: Queue = Queue.COMPETITIVE,
    ): Match {
        val results = outcomes.map { it == 'W' }
        val rounds = results.mapIndexed { index, roundWon ->
            val number = index + 1
            val attacking = number <= 12
            Round(
                number = number,
                won = roundWon,
                kills = if (index % 3 == 0) listOf(KillEvent(15_000, me, PlayerId("e1"), emptySet(), weapon = null)) else emptyList(),
                myDamage = 150,
                myShots = Shots(head = 2, body = 5, leg = 0),
                mySide = if (attacking) Side.ATTACK else Side.DEFENSE,
                ending = if (roundWon) RoundEnding.ELIMINATION else RoundEnding.SPIKE_DEFUSED,
                economy = when {
                    number == 1 || number == 13 -> RoundEconomy(800, 800, 800)
                    index % 5 == 2 -> RoundEconomy(1_100, 1_200, 4_200)
                    index % 5 == 4 -> RoundEconomy(2_600, 2_700, 4_100)
                    else -> RoundEconomy(4_300, 4_400, 4_000)
                },
            )
        }
        return Match(
            id = MatchId(id),
            queue = queue,
            act = ActId("act"),
            map = map,
            startedAt = startedAt,
            lengthMillis = 38.minutes.inWholeMilliseconds,
            me = me,
            myAgent = jett,
            myRole = null,
            allies = setOf(junho),
            myCombatScore = 0,
            myTeamWon = won ?: (results.count { it } > results.count { !it }),
            roundOutcomes = results,
            rounds = rounds,
            players = players,
        )
    }
}
