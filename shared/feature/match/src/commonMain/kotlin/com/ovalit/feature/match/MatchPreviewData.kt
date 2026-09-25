package com.ovalit.feature.match

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.KillEvent
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
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

// 프리뷰와 UI 테스트가 같이 쓴다. 목업 S3의 어센트 13 – 9 경기에 맞췄다. ID는 번들한 이미지가 붙도록
// 카탈로그의 UUID다.
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
    )

    val detail = MatchDetailUiState.Success(
        match = detailMatch,
        catalog = catalog,
        myTeam = detailMatch.players.filter { it.onMyTeam }.sortedByDescending { it.combatScore }.map { line ->
            ScoreboardRow(
                line,
                when (line.player) {
                    me -> PlayerRelation.ME
                    junho -> PlayerRelation.FRIEND
                    bloom -> PlayerRelation.APP_USER
                    else -> PlayerRelation.NOT_APP_USER
                },
            )
        },
        enemyTeam = detailMatch.players.filter { !it.onMyTeam }.sortedByDescending { it.combatScore }.map { line ->
            ScoreboardRow(line, if (line.player == hwan) PlayerRelation.REQUESTED_ME else PlayerRelation.NOT_APP_USER)
        },
        rounds = detailMatch.roundSummaries(),
        buys = detailMatch.buyRecords(),
        timeZone = seoul,
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

    private fun Instant.date(): LocalDate = toLocalDateTime(seoul).date

    private fun match(
        id: String,
        map: MapId,
        startedAt: Instant,
        outcomes: String,
        won: Boolean? = null,
        players: List<Scoreline> = listOf(line(me, "오발러#KR1", jett, true, 16, 14, 6, 140)),
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
            queue = Queue.COMPETITIVE,
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
