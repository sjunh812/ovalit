package com.ovalit.feature.profile

import com.ovalit.core.model.Account
import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.CompetitiveRecord
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.HighlightCount
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.ProfileSummary
import com.ovalit.core.model.ReportPeriod
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.RoleShare
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponHighlight
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeaponInfo
import com.ovalit.core.model.WeaponMetric
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.ui.PlayerBadge
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

// 프리뷰와 UI 테스트가 같이 쓴다. 목업의 전략가 예시에 맞췄다. ID는 번들한 이미지가 붙도록 카탈로그의 UUID다.
internal object ProfilePreviewData {

    private val omen = AgentId("8e253930-4c05-31dd-1b6c-968525494517")
    private val viper = AgentId("707eab51-4836-f488-046a-cda6bf494859")
    private val jett = AgentId("add6443a-41bd-e414-f6ad-e58d267f4e95")
    private val killjoy = AgentId("1e58de9c-4950-5125-93e9-a0aee9f98746")
    private val newAgent = AgentId("new-agent")
    private val phantom = WeaponId("EE8E8D15-496B-07AC-E5F6-8FAE5D4C7B1A")
    private val vandal = WeaponId("9C82E19D-4575-0200-1A81-3EACF00CF872")
    private val ghost = WeaponId("1BAA85B4-4C70-1284-64BB-6481DFC3BB4E")
    private val classic = WeaponId("29A0CFAB-485B-F5D5-779A-B59F85E204A8")
    private val newWeapon = WeaponId("new-weapon")

    val catalog = ContentCatalog(
        agents = mapOf(omen to "오멘", viper to "바이퍼", jett to "제트", killjoy to "킬조이"),
        weapons = mapOf(
            phantom to WeaponInfo("팬텀", WeaponCategory.RIFLE),
            vandal to WeaponInfo("밴달", WeaponCategory.RIFLE),
            ghost to WeaponInfo("고스트", WeaponCategory.PISTOL),
            classic to WeaponInfo("클래식", WeaponCategory.PISTOL),
        ),
        maps = emptyMap(),
        tiers = mapOf(16 to "플래티넘 2"),
    )

    // 킬, 데스, 어시는 라운드당 0.85, 0.65, 0.3으로 둔다. 요원 칸의 KDA가 비지 않게 한다.
    private fun metrics(rounds: Int, kast: Int, survived: Int, firstKills: Int = 10, firstDeaths: Int = 10) =
        MatchMetrics(
            matches = 1,
            rounds = rounds,
            kills = rounds * 17 / 20,
            deaths = rounds * 13 / 20,
            assists = rounds * 6 / 20,
            combatScore = 0,
            damage = 0,
            shots = Shots.None,
            kastRounds = kast,
            survivedRounds = survived,
            firstKills = firstKills,
            firstDeaths = firstDeaths,
            firstKillRoundsWon = 0,
            ecoRounds = 0,
            ecoRoundsWon = 0,
            forceBuyRounds = 0,
            forceBuyRoundsWon = 0,
            fullBuyRounds = 0,
            fullBuyRoundsWon = 0,
        )

    val agents = AgentReport(
        matches = 50,
        mainRole = Role.CONTROLLER,
        roles = listOf(
            RoleShare(Role.CONTROLLER, matches = 28, rounds = 610),
            RoleShare(Role.DUELIST, matches = 12, rounds = 260),
            RoleShare(Role.SENTINEL, matches = 3, rounds = 66),
        ),
        agents = listOf(
            AgentStats(omen, Role.CONTROLLER, matches = 18, wins = 11, decided = 18, metrics = metrics(400, 284, 184)),
            AgentStats(viper, Role.CONTROLLER, matches = 10, wins = 5, decided = 10, metrics = metrics(210, 143, 92)),
            AgentStats(jett, Role.DUELIST, matches = 9, wins = 4, decided = 9, metrics = metrics(200, 128, 64)),
            AgentStats(newAgent, null, matches = 6, wins = 3, decided = 6, metrics = metrics(130, 90, 50)),
            AgentStats(killjoy, Role.SENTINEL, matches = 3, wins = 2, decided = 3, metrics = metrics(66, 40, 30)),
        ),
    )

    val duelistAgents = agents.copy(mainRole = Role.DUELIST, roles = agents.roles.sortedByDescending { it.role == Role.DUELIST })

    private fun weapon(id: WeaponId, kills: Int, rounds: Int, head: Int, total: Int = 100, carried: Int = 0, deaths: Int = 0, adr: Int = 0) =
        WeaponStats(
            weapon = id,
            kills = kills,
            singleWeaponRounds = rounds,
            shots = Shots(head = head, body = total - head, leg = 0),
            carriedRounds = carried,
            deaths = deaths,
            assists = carried / 2,
            damage = adr * carried,
        )

    val weapons = WeaponReport(
        matches = 50,
        kills = 520,
        weapons = listOf(
            weapon(phantom, kills = 254, rounds = 118, head = 27, carried = 140, deaths = 180, adr = 142),
            weapon(vandal, kills = 198, rounds = 94, head = 15, carried = 110, deaths = 150, adr = 135),
            weapon(ghost, kills = 46, rounds = 12, head = 30, carried = 36, deaths = 30, adr = 88),
            weapon(newWeapon, kills = 22, rounds = 21, head = 20, carried = 6, deaths = 4, adr = 120),
            // 한 무기만 쓴 라운드도, 들고 시작한 라운드도 모자라다
            weapon(classic, kills = 5, rounds = 3, head = 20, carried = 8, deaths = 7, adr = 60),
        ),
        highlights = listOf(
            WeaponHighlight(
                act = weapon(phantom, kills = 254, rounds = 118, head = 27, carried = 140, deaths = 180, adr = 142),
                current = weapon(phantom, kills = 60, rounds = 30, head = 27, carried = 34, deaths = 40, adr = 151),
                baseline = weapon(phantom, kills = 110, rounds = 50, head = 21, carried = 70, deaths = 90, adr = 139),
                baselineWeeks = 4,
                movements = mapOf(
                    WeaponMetric.KD to Movement.STEADY,
                    WeaponMetric.DAMAGE_PER_ROUND to Movement.STEADY,
                    WeaponMetric.HEADSHOT_RATE to Movement.MOVED,
                ),
            ),
            WeaponHighlight(
                act = weapon(vandal, kills = 198, rounds = 94, head = 15, carried = 110, deaths = 150, adr = 135),
                current = weapon(vandal, kills = 40, rounds = 22, head = 15, carried = 26, deaths = 32, adr = 130),
                baseline = weapon(vandal, kills = 90, rounds = 44, head = 17, carried = 60, deaths = 70, adr = 136),
                baselineWeeks = 4,
                movements = WeaponMetric.entries.associateWith { Movement.STEADY },
            ),
            // 이번 주에 들고 시작한 라운드가 모자라서 이번 액트 값을 띄운다
            WeaponHighlight(
                act = weapon(ghost, kills = 46, rounds = 12, head = 30, carried = 36, deaths = 30, adr = 88),
                current = null,
                baseline = null,
                baselineWeeks = 4,
                movements = emptyMap(),
            ),
        ),
        period = ReportPeriod(firstDay = LocalDate(2026, 9, 21), weeks = 1, includesThisWeek = true),
    )

    val now: Instant = Instant.parse("2026-09-24T13:00:00Z")
    private val seoul = TimeZone.of("Asia/Seoul")
    private val me = PlayerId("me")
    private val ascent = MapId("7eaecc1b-4337-bbf6-6ab9-04b8f06b3319")
    private val haven = MapId("2bee0dc9-4ffe-519b-1cbd-7fbe763a6047")

    val summary = ProfileSummary(
        metrics = MatchMetrics.Empty.copy(
            matches = 50,
            rounds = 1_127,
            kills = 860,
            deaths = 768,
            assists = 312,
            combatScore = 188 * 1_127,
            damage = 139 * 1_127,
            shots = Shots(head = 442, body = 1_253, leg = 147),
        ),
        mostKills = 28,
        playTimeMillis = (31.hours + 20.minutes).inWholeMilliseconds,
        competitive = CompetitiveRecord(matches = 32, wins = 18, losses = 14, currentTier = 16),
        highlights = HighlightCount(aces = 2, clutches = 3, clutchAttempts = 7),
    )

    private fun recent(id: String, map: MapId, hoursAgo: Int, won: Int, lost: Int, k: Int, d: Int, a: Int): Match {
        val rounds = won + lost
        return Match(
            id = MatchId(id),
            queue = Queue.COMPETITIVE,
            act = ActId("act"),
            map = map,
            startedAt = now - hoursAgo.hours,
            lengthMillis = 38.minutes.inWholeMilliseconds,
            me = me,
            myAgent = omen,
            myRole = Role.CONTROLLER,
            allies = emptySet(),
            myCombatScore = 0,
            myTeamWon = won > lost,
            roundOutcomes = List(won) { true } + List(lost) { false },
            rounds = emptyList(),
            players = listOf(
                Scoreline(me, "오발러#KR1", omen, onMyTeam = true, tier = 16, playerCard = null, kills = k, deaths = d,
                    assists = a, combatScore = 196 * rounds, damage = 141 * rounds, roundsPlayed = rounds),
            ),
        )
    }

    val recentMatches = listOf(
        recent("ascent", ascent, hoursAgo = 2, won = 13, lost = 9, k = 18, d = 14, a = 7),
        recent("haven", haven, hoursAgo = 3, won = 8, lost = 13, k = 12, d = 16, a = 5),
        recent("ascent-2", ascent, hoursAgo = 26, won = 13, lost = 11, k = 20, d = 15, a = 6),
    )

    val success = ProfileUiState.Success(
        account = Account(riotId = "오발러#KR1", linkedOn = LocalDate(2026, 9, 19)),
        badge = PlayerBadge(
            riotId = "오발러#KR1",
            tier = 16,
            tierName = "플래티넘 2",
        ),
        summary = summary,
        agents = agents,
        weapons = weapons,
        recentMatches = recentMatches,
        hasMoreMatches = true,
        catalog = catalog,
        now = now,
        timeZone = seoul,
    )

    // 이번 액트에 경쟁전을 안 뛰었으면 티어 칸이 없고 티어는 이름 줄에 남는다
    val noCompetitive = success.copy(summary = summary.copy(competitive = null))
}
