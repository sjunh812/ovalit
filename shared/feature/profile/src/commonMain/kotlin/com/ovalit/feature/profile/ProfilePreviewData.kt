package com.ovalit.feature.profile

import com.ovalit.core.model.Account
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.AgentReport
import com.ovalit.core.model.AgentStats
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.Role
import com.ovalit.core.model.RoleShare
import com.ovalit.core.model.Shots
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponHighlight
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeaponInfo
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import kotlinx.datetime.LocalDate

// 프리뷰와 UI 테스트가 같이 쓴다. 목업의 전략가 예시에 맞췄다.
internal object ProfilePreviewData {

    private val omen = AgentId("omen")
    private val viper = AgentId("viper")
    private val jett = AgentId("jett")
    private val killjoy = AgentId("killjoy")
    private val newAgent = AgentId("new-agent")
    private val phantom = WeaponId("phantom")
    private val vandal = WeaponId("vandal")
    private val ghost = WeaponId("ghost")
    private val newWeapon = WeaponId("new-weapon")

    val catalog = ContentCatalog(
        agents = mapOf(omen to "오멘", viper to "바이퍼", jett to "제트", killjoy to "킬조이"),
        weapons = mapOf(
            phantom to WeaponInfo("팬텀", WeaponCategory.RIFLE),
            vandal to WeaponInfo("밴달", WeaponCategory.RIFLE),
            ghost to WeaponInfo("고스트", WeaponCategory.PISTOL),
        ),
    )

    private fun metrics(rounds: Int, kast: Int, survived: Int, firstKills: Int = 10, firstDeaths: Int = 10) =
        MatchMetrics(
            matches = 1,
            rounds = rounds,
            kills = 0,
            deaths = 0,
            assists = 0,
            combatScore = 0,
            damage = 0,
            shots = Shots.None,
            kastRounds = kast,
            survivedRounds = survived,
            firstKills = firstKills,
            firstDeaths = firstDeaths,
            firstKillRoundsWon = 0,
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

    private fun weapon(id: WeaponId, kills: Int, rounds: Int, head: Int, total: Int = 100) =
        WeaponStats(id, kills, rounds, Shots(head = head, body = total - head, leg = 0))

    val weapons = WeaponReport(
        matches = 50,
        kills = 520,
        weapons = listOf(
            weapon(phantom, kills = 254, rounds = 118, head = 27),
            weapon(vandal, kills = 198, rounds = 94, head = 15),
            weapon(ghost, kills = 46, rounds = 12, head = 30),
            weapon(newWeapon, kills = 22, rounds = 21, head = 20),
        ),
        highlights = listOf(
            WeaponHighlight(
                act = weapon(phantom, kills = 254, rounds = 118, head = 27),
                current = weapon(phantom, kills = 60, rounds = 30, head = 27),
                baseline = weapon(phantom, kills = 110, rounds = 50, head = 21),
                baselineWeeks = 4,
                movement = Movement.MOVED,
            ),
            WeaponHighlight(
                act = weapon(vandal, kills = 198, rounds = 94, head = 15),
                current = weapon(vandal, kills = 40, rounds = 22, head = 15),
                baseline = weapon(vandal, kills = 90, rounds = 44, head = 17),
                baselineWeeks = 4,
                movement = Movement.STEADY,
            ),
        ),
    )

    val success = ProfileUiState.Success(
        account = Account(riotId = "오발러#KR1", linkedOn = LocalDate(2026, 9, 19)),
        agents = agents,
        weapons = weapons,
        catalog = catalog,
    )
}
