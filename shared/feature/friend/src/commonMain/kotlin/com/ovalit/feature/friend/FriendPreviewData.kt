package com.ovalit.feature.friend

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.FriendRequestSource
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Role
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.SharedRecord
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerBadge
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

// 프리뷰와 UI 테스트가 같이 쓴다. 목업 S5의 민석 예시에 맞췄다.
internal object FriendPreviewData {

    private fun metrics(matches: Int, rounds: Int, kills: Int, deaths: Int, score: Int, damage: Int, head: Int) =
        MatchMetrics(
            matches = matches,
            rounds = rounds,
            kills = kills,
            deaths = deaths,
            assists = matches * 4,
            combatScore = score,
            damage = damage,
            shots = Shots(head = head, body = 100 - head, leg = 0),
            kastRounds = rounds * 7 / 10,
            survivedRounds = rounds * 4 / 10,
            firstKills = rounds / 5,
            firstDeaths = rounds / 6,
            firstKillRoundsWon = rounds / 8,
            ecoRounds = rounds / 10,
            ecoRoundsWon = rounds / 40,
            forceBuyRounds = rounds / 7,
            forceBuyRoundsWon = rounds / 20,
            fullBuyRounds = rounds * 2 / 3,
            fullBuyRoundsWon = rounds / 3,
        )

    private val thisWeek = ReportPeriod(firstDay = LocalDate(2026, 9, 21), weeks = 1, includesThisWeek = true)

    private fun ready(metrics: MatchMetrics, baseline: MatchMetrics, role: Role) = WeeklyReport.Ready(
        act = ActId("preview"),
        period = thisWeek,
        metrics = metrics,
        baseline = Baseline(baseline, weeks = 4),
        mainRole = role,
        dynamic = emptyList(),
        insight = null,
        trend = emptyList(),
    )

    private val minseokWeek = metrics(9, 200, 160, 136, 43_600, 29_400, 27)
    private val minseokBefore = metrics(20, 440, 350, 290, 92_000, 63_400, 25)
    private val myWeek = metrics(11, 240, 210, 148, 57_840, 38_880, 24)
    private val myBefore = metrics(24, 520, 420, 320, 116_000, 78_000, 19)

    private val now = Instant.parse("2026-09-24T13:00:00Z")
    private val jett = AgentId("add6443a-41bd-e414-f6ad-e58d267f4e95")
    private val ascent = MapId("7eaecc1b-4337-bbf6-6ab9-04b8f06b3319")
    private val lotus = MapId("2fe4ed3a-450a-948b-6d6b-e89a78e680a9")
    private val pearl = MapId("fd267378-4d1d-484f-ff52-77821ed10dc2")
    private val catalog = ContentCatalog.Empty.copy(
        agents = mapOf(jett to "제트"),
        maps = mapOf(ascent to "어센트", lotus to "로터스", pearl to "펄"),
    )

    // 목업 S5 "민석의 최근 경기"의 세 판에 한 판을 더 얹었다. 넘치는 판이 있어야 "전체 보기"가 뜬다.
    private fun game(id: String, map: MapId, hoursAgo: Int, mine: Int, theirs: Int, k: Int, d: Int, a: Int, adr: Int): Match {
        val outcomes = List(mine) { true } + List(theirs) { false }
        val rounds = outcomes.size
        val player = PlayerId("minseok")
        return Match(
            id = MatchId(id), queue = Queue.COMPETITIVE, act = ActId("preview"), map = map,
            startedAt = now - hoursAgo.hours, lengthMillis = 0, me = player, myAgent = jett, myRole = Role.DUELIST,
            allies = emptySet(), myCombatScore = 0, myTeamWon = mine > theirs, roundOutcomes = outcomes, rounds = emptyList(),
            players = listOf(
                Scoreline(player, "민석#KR3", jett, onMyTeam = true, tier = 19, playerCard = null,
                    kills = k, deaths = d, assists = a, combatScore = 0, damage = adr * rounds, roundsPlayed = rounds),
            ),
        )
    }

    val minseok = Friend(
        PlayerId("minseok"),
        "민석#KR3",
        playerCard = PlayerCardId("BFBC000C-4121-3227-E7F5-A3ABA576FA3C"),
        statsPublic = true,
        matches = listOf(
            game("m1", ascent, hoursAgo = 2, mine = 9, theirs = 13, k = 19, d = 15, a = 4, adr = 163),
            game("m2", lotus, hoursAgo = 26, mine = 13, theirs = 10, k = 17, d = 12, a = 6, adr = 151),
            game("m3", pearl, hoursAgo = 28, mine = 11, theirs = 13, k = 12, d = 16, a = 9, adr = 128),
            game("m4", ascent, hoursAgo = 50, mine = 13, theirs = 7, k = 21, d = 9, a = 5, adr = 180),
        ),
    )
    private val junho = Friend(PlayerId("junho"), "준호#KR1", playerCard = PlayerCardId("89FDD50E-439B-EBEB-0EF2-AF8271550943"), statsPublic = true, matches = emptyList())
    private val seoyeon = Friend(PlayerId("seoyeon"), "서연#KR7", playerCard = null, statsPublic = false, matches = emptyList())

    val friends = FriendsUiState.Success(
        requests = listOf(
            FriendRequest(PlayerId("jiwoo"), "지우#KR5", PlayerCardId("CABD47C0-44B9-A3E0-F100-EA87B692DC86"), FriendRequestSource.SCOREBOARD),
            FriendRequest(PlayerId("hyun"), "현#KR9", null, FriendRequestSource.INVITE_LINK),
        ),
        friends = listOf(
            FriendRow(junho, WeeklyReport.NotEnoughMatches(played = 3)),
            FriendRow(minseok, ready(minseokWeek, minseokBefore, Role.DUELIST)),
            FriendRow(seoyeon, report = null),
        ),
        rivalId = PlayerId("junho"),
    )

    val noFriends = FriendsUiState.Success(requests = emptyList(), friends = emptyList(), rivalId = null)

    val profile = FriendProfileUiState.Success(
        friend = minseok,
        badge = PlayerBadge(minseok.riotId, jett, tier = 19, tierName = "다이아몬드 2"),
        catalog = catalog,
        isRival = false,
        shared = SharedRecord(matches = 12, wins = 8, losses = 4),
        theirReport = ready(minseokWeek, minseokBefore, Role.DUELIST),
        myReport = ready(myWeek, myBefore, Role.CONTROLLER),
        theirMetricsInMyPeriod = minseokWeek,
        now = now,
        timeZone = TimeZone.of("Asia/Seoul"),
    )

    val privateProfile = profile.copy(
        friend = seoyeon,
        badge = PlayerBadge(seoyeon.riotId, agent = null, tier = null, tierName = null),
        theirReport = null,
        theirMetricsInMyPeriod = null,
        shared = SharedRecord(matches = 3, wins = 1, losses = 2),
    )
}
