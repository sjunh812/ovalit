package com.ovalit.core.network

// Riot 문서의 VAL-MATCH-V1 모양을 따라 손으로 짠 응답이다.
// 실제 응답을 아직 못 봐서 값은 지어냈다.
// 라운드마다 열 명의 장비 줄을 다 적으면 읽을 수 없어서 줄을 만드는 함수만 두고, 무엇이 일어났는지는 경기마다 손으로 적는다.

internal const val ME = "p-me"
internal const val A1 = "p-ally-1"
internal const val A2 = "p-ally-2"
internal const val A3 = "p-ally-3"
internal const val A4 = "p-ally-4"
internal const val E1 = "p-enemy-1"
internal const val E2 = "p-enemy-2"
internal const val E3 = "p-enemy-3"
internal const val E4 = "p-enemy-4"
internal const val E5 = "p-enemy-5"
internal val ALLIES = listOf(A1, A2, A3, A4)
internal val ENEMIES = listOf(E1, E2, E3, E4, E5)

// Riot은 같은 UUID를 대문자로 주기도 한다. 옮길 때 소문자로 바뀌는지 보려고 일부러 대문자로 둔다.
internal const val RAZE = "F94C3B30-42BE-E959-889C-5AA313DBA261"
internal const val SAGE = "569fdd95-4d10-43ab-ca70-79becc718b46"
internal const val JETT = "add6443a-41bd-e414-f6ad-e58d267f4e95"
internal const val VANDAL = "9C82E19D-4575-0200-1A81-3EACF00CF872"
internal const val PHANTOM = "EE8E8D15-496B-07AC-E5F6-8FAE5D4C7B1A"
internal const val GHOST = "1BAA85B4-4C70-1284-64BB-6481DFC3BB4E"
internal const val CLASSIC = "29A0CFAB-485B-F5D5-779A-B59F85E204A8"
internal const val SHERIFF = "E336C6B8-418D-9340-D77F-7A9E4CFE0702"
internal const val SPECTRE = "462080D1-4035-2937-7C09-27AA2A5C27A7"
internal const val ACT = "52CA6698-41C1-E7DE-4008-8994D2221209"
internal const val MY_CARD = "9FB348BC-41A0-91AD-8A3E-818035C4E561"
internal const val STARTED_AT = 1_789_000_000_000L

private fun quoted(values: List<String>) = values.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

internal fun kill(
    atMillis: Long,
    killer: String,
    victim: String,
    assistants: List<String> = emptyList(),
    damageType: String = "Weapon",
    damageItem: String = VANDAL,
) = """
    {
      "timeSinceGameStartMillis": ${atMillis + 600_000},
      "timeSinceRoundStartMillis": $atMillis,
      "killer": "$killer",
      "victim": "$victim",
      "victimLocation": {"x": 1200, "y": -3400},
      "assistants": ${quoted(assistants)},
      "playerLocations": [{"puuid": "$killer", "viewRadians": 1.57, "location": {"x": 1000, "y": -3000}}],
      "finishingDamage": {"damageType": "$damageType", "damageItem": "$damageItem", "isSecondaryFireMode": false}
    }
""".trimIndent()

internal fun hit(receiver: String, damage: Int, head: Int = 0, body: Int = 0, leg: Int = 0) =
    """{"receiver": "$receiver", "damage": $damage, "legshots": $leg, "bodyshots": $body, "headshots": $head}"""

internal fun roundStats(
    puuid: String,
    loadout: Int,
    weapon: String,
    kills: List<String> = emptyList(),
    damage: List<String> = emptyList(),
) = """
    {
      "puuid": "$puuid",
      "kills": ${kills.joinToString(prefix = "[", postfix = "]")},
      "damage": ${damage.joinToString(prefix = "[", postfix = "]")},
      "score": ${kills.size * 200},
      "economy": {"loadoutValue": $loadout, "weapon": "$weapon", "armor": "", "remaining": 1200, "spent": $loadout},
      "ability": {"grenadeEffects": null, "ability1Effects": null, "ability2Effects": null, "ultimateEffects": null}
    }
""".trimIndent()

/**
 * 라운드 하나입니다. [blue]와 [red]는 팀마다 한 사람당 장비 가치이고, [kills]와 [damage]는 사람마다 낸 킬과 준 피해입니다.
 * [absent]에 든 사람은 그 라운드 `playerStats`에서 빠집니다. 튕겨서 못 뛴 사람입니다.
 */
internal fun round(
    num: Int,
    winner: String,
    winnerRole: String?,
    result: String,
    code: String,
    blue: Int = 4_500,
    red: Int = 4_500,
    myLoadout: Int = blue,
    myWeapon: String = VANDAL,
    kills: Map<String, List<String>> = emptyMap(),
    damage: Map<String, List<String>> = emptyMap(),
    absent: Set<String> = emptySet(),
    planter: String? = null,
    defuser: String? = null,
    bluePlayers: List<String> = listOf(ME) + ALLIES,
    redPlayers: List<String> = ENEMIES,
): String {
    val stats = (bluePlayers + redPlayers).filterNot { it in absent }.map { puuid ->
        val loadout = when (puuid) {
            ME -> myLoadout
            in bluePlayers -> blue
            else -> red
        }
        val weapon = if (puuid == ME) myWeapon else if (loadout < 2_000) CLASSIC else PHANTOM
        roundStats(puuid, loadout, weapon, kills[puuid].orEmpty(), damage[puuid].orEmpty())
    }
    val role = winnerRole?.let { "\"winningTeamRole\": \"$it\"," }.orEmpty()
    val plant = planter?.let { "\"bombPlanter\": \"$it\", \"plantRoundTime\": 45000, \"plantSite\": \"A\", \"plantLocation\": {\"x\": 10, \"y\": 20}," }.orEmpty()
    val defuse = defuser?.let { "\"bombDefuser\": \"$it\", \"defuseRoundTime\": 80000, \"defuseLocation\": {\"x\": 11, \"y\": 21}," }.orEmpty()
    return """
        {
          "roundNum": $num,
          "roundResult": "$result",
          "roundCeremony": "CeremonyDefault",
          "winningTeam": "$winner",
          $role
          $plant
          $defuse
          "plantPlayerLocations": [],
          "defusePlayerLocations": [],
          "playerStats": ${stats.joinToString(prefix = "[", postfix = "]")},
          "roundResultCode": "$code"
        }
    """.trimIndent()
}

internal fun player(
    puuid: String,
    team: String,
    agent: String,
    kills: Int,
    deaths: Int,
    assists: Int,
    score: Int,
    roundsPlayed: Int,
    tier: Int = 15,
    name: String = puuid,
    tag: String = "KR1",
    card: String? = "0e40e7c8-4bb3-aa18-2ae4-27ae3a68e88e",
) = """
    {
      "puuid": "$puuid",
      "gameName": "$name",
      "tagLine": "$tag",
      "teamId": "$team",
      "partyId": "party-$team",
      "characterId": "$agent",
      "stats": {
        "score": $score, "roundsPlayed": $roundsPlayed, "kills": $kills, "deaths": $deaths, "assists": $assists,
        "playtimeMillis": 2000000,
        "abilityCasts": {"grenadeCasts": 10, "ability1Casts": 12, "ability2Casts": 8, "ultimateCasts": 2}
      },
      "competitiveTier": $tier,
      "isObserver": false,
      ${card?.let { "\"playerCard\": \"$it\"," }.orEmpty()}
      "playerTitle": "a4a3a4e8-4a6b-9a27-1b1f-b8c2a4d1c3e0",
      "accountLevel": 120
    }
""".trimIndent()

internal fun team(id: String, won: Boolean, roundsWon: Int, points: Int = roundsWon, roundsPlayed: Int = 0) =
    """{"teamId": "$id", "won": $won, "roundsPlayed": $roundsPlayed, "roundsWon": $roundsWon, "numPoints": $points}"""

internal fun matchJson(
    players: List<String>,
    teams: List<String>,
    rounds: List<String>,
    queueId: String = "competitive",
    provisioningFlowId: String = "Matchmaking",
    isCompleted: Boolean = true,
    coaches: List<String> = emptyList(),
    matchId: String = "8c1f6a2e-0000-4000-8000-000000000001",
    mapId: String = "/Game/Maps/Ascent/Ascent",
) = """
    {
      "matchInfo": {
        "matchId": "$matchId",
        "mapId": "$mapId",
        "gameVersion": "release-13.06-shipping-12-1234567",
        "gameLengthMillis": 2100000,
        "region": "kr",
        "gameStartMillis": $STARTED_AT,
        "provisioningFlowId": "$provisioningFlowId",
        "isCompleted": $isCompleted,
        "customGameName": "",
        "queueId": "$queueId",
        "gameMode": "/Game/GameModes/Bomb/BombGameMode.BombGameMode_C",
        "isRanked": ${queueId == "competitive"},
        "seasonId": "$ACT",
        "premierMatchInfo": {}
      },
      "players": ${players.joinToString(prefix = "[", postfix = "]")},
      "coaches": ${coaches.joinToString(prefix = "[", postfix = "]")},
      "teams": ${teams.joinToString(prefix = "[", postfix = "]")},
      "roundResults": ${rounds.joinToString(prefix = "[", postfix = "]")}
    }
""".trimIndent()

// 우리 팀이 이기고 나와 상관없는 킬 하나만 나는 라운드다
private fun quietWin(num: Int, role: String?, blue: Int = 4_500, red: Int = 4_500, myWeapon: String = VANDAL) = round(
    num = num, winner = "Blue", winnerRole = role, result = "Eliminated", code = "Elimination",
    blue = blue, red = red, myWeapon = myWeapon,
    kills = mapOf(A2 to listOf(kill(20_000, A2, E2, damageItem = PHANTOM))),
)

/**
 * 13대3으로 이긴 경쟁전입니다. 나는 블루 팀 레이즈이고 전반에 공격합니다. `roundNum`은 0부터입니다. 라운드마다 일어난 일:
 *
 * 1. 피스톨. 고스트로 첫 킬 포함 2킬(멀티킬). 290 피해
 * 2. 이코. 10초에 죽고 3초 뒤 트레이드, 세이지가 살려 줬는데 40초에 또 죽고 7초 뒤에야 갚아 줌(데스 2, 트레이드 1). 짐
 * 3. 포스바이. 내 궁극기로 자살(데스지만 첫 데스 아님). 나한테 준 피해 150은 피해량에서 빠짐. 짐
 * 4. 수류탄으로 팀킬(킬 아님), 밴달로 첫 킬, 어시 1
 * 5. 1대2 클러치 성공
 * 6. 에이스
 * 7. 튕겨서 못 뜀. 짐
 * 8~11. 조용히 이김
 * 12. 이긴 팀 진영이 빠지고 우리 팀이 설치한 스파이크가 터져서 이김(공격으로 가림)
 * 13. 후반 피스톨. 조용히 이김
 * 14. 20초에 첫 데스, 3초 뒤 트레이드. 이김
 * 15. 이긴 팀 진영이 빠지고 상대가 설치한 스파이크를 해체해서 이김(수비로 가림)
 * 16. 이긴 팀 진영도 스파이크도 없이 이김(진영 모름)
 *
 * 관전자 한 명과 코치 한 명이 있고, 문서에 없는 필드도 섞여 있습니다.
 */
internal val CompetitiveMatch: String = matchJson(
    players = listOf(
        player(ME, "Blue", RAZE, kills = 11, deaths = 4, assists = 1, score = 4_200, roundsPlayed = 15, tier = 16, name = "봉봉이", card = MY_CARD)
            .replace("\"playtimeMillis\"", "\"performanceScore\": 287, \"playtimeMillis\""),
        player(A1, "Blue", SAGE, kills = 9, deaths = 10, assists = 8, score = 3_100, roundsPlayed = 16, tier = 0),
        player(A2, "Blue", JETT, kills = 14, deaths = 9, assists = 3, score = 3_900, roundsPlayed = 16),
        player(A3, "Blue", JETT, kills = 10, deaths = 11, assists = 4, score = 3_000, roundsPlayed = 16),
        player(A4, "Blue", JETT, kills = 8, deaths = 12, assists = 6, score = 2_600, roundsPlayed = 16),
        player(E1, "Red", JETT, kills = 12, deaths = 14, assists = 2, score = 3_500, roundsPlayed = 16),
        player(E2, "Red", JETT, kills = 9, deaths = 14, assists = 3, score = 2_900, roundsPlayed = 16),
        player(E3, "Red", JETT, kills = 7, deaths = 13, assists = 5, score = 2_400, roundsPlayed = 16),
        player(E4, "Red", JETT, kills = 6, deaths = 14, assists = 4, score = 2_100, roundsPlayed = 16),
        player(E5, "Red", JETT, kills = 5, deaths = 13, assists = 6, score = 1_900, roundsPlayed = 16),
        """{"puuid": "p-observer", "gameName": "관전자", "tagLine": "KR1", "teamId": "Neutral", "isObserver": true, "competitiveTier": 0, "newBadge": {"level": 3}}""",
    ),
    coaches = listOf("""{"puuid": "p-coach", "teamId": "Blue"}"""),
    teams = listOf(team("Blue", won = true, roundsWon = 13, roundsPlayed = 16), team("Red", won = false, roundsWon = 3, roundsPlayed = 16)),
    rounds = listOf(
        round(
            num = 0, winner = "Blue", winnerRole = "Attacker", result = "Eliminated", code = "Elimination",
            blue = 800, red = 800, myLoadout = 900, myWeapon = GHOST,
            kills = mapOf(
                ME to listOf(kill(8_000, ME, E1, damageItem = GHOST), kill(15_000, ME, E2, damageItem = GHOST)),
                A2 to listOf(kill(40_000, A2, E3, damageItem = CLASSIC)),
            ),
            damage = mapOf(ME to listOf(hit(E1, 150, head = 1, body = 1), hit(E2, 140, body = 3, leg = 1))),
        ),
        round(
            num = 1, winner = "Red", winnerRole = "Defender", result = "Bomb defused", code = "Defuse",
            blue = 1_000, red = 4_500, myWeapon = SHERIFF, planter = A2, defuser = E3,
            kills = mapOf(
                E1 to listOf(kill(10_000, E1, ME)),
                A3 to listOf(kill(13_000, A3, E1, assistants = listOf(A1), damageItem = PHANTOM)),
                E2 to listOf(kill(40_000, E2, ME)),
                A4 to listOf(kill(47_000, A4, E2, damageItem = PHANTOM)),
            ),
            damage = mapOf(
                ME to listOf(hit(E1, 50, body = 2)),
                E1 to listOf(hit(ME, 150, head = 1, body = 1)),
                E2 to listOf(hit(ME, 140, body = 4)),
            ),
        ),
        round(
            num = 2, winner = "Red", winnerRole = "Defender", result = "Round timer expired", code = "",
            blue = 3_000, red = 4_500, myWeapon = SPECTRE,
            kills = mapOf(
                ME to listOf(kill(5_000, ME, ME, damageType = "Ability", damageItem = "Ultimate")),
                E3 to listOf(kill(20_000, E3, A2)),
            ),
            damage = mapOf(ME to listOf(hit(ME, 150), hit(E3, 40, body = 1))),
        ),
        round(
            num = 3, winner = "Blue", winnerRole = "Attacker", result = "Eliminated", code = "Elimination",
            kills = mapOf(
                ME to listOf(
                    kill(4_000, ME, A4, damageType = "Ability", damageItem = "GrenadeAbility"),
                    kill(12_000, ME, E4, assistants = listOf(A2)),
                ),
                A2 to listOf(kill(20_000, A2, E5, assistants = listOf(ME), damageItem = PHANTOM)),
            ),
            damage = mapOf(ME to listOf(hit(A4, 150), hit(E4, 160, head = 1, body = 2), hit(E5, 60, body = 2))),
        ),
        round(
            num = 4, winner = "Blue", winnerRole = "Attacker", result = "Eliminated", code = "Elimination",
            kills = mapOf(
                A1 to listOf(kill(10_000, A1, E5, damageItem = PHANTOM)),
                E1 to listOf(kill(12_000, E1, A1), kill(24_000, E1, A3)),
                A2 to listOf(kill(15_000, A2, E4, damageItem = PHANTOM)),
                E2 to listOf(kill(18_000, E2, A2), kill(27_000, E2, A4)),
                A3 to listOf(kill(21_000, A3, E3, damageItem = PHANTOM)),
                ME to listOf(kill(35_000, ME, E1), kill(50_000, ME, E2)),
            ),
            damage = mapOf(ME to listOf(hit(E1, 150, head = 1, body = 1), hit(E2, 150, head = 1, body = 1))),
        ),
        round(
            num = 5, winner = "Blue", winnerRole = "Attacker", result = "Eliminated", code = "Elimination",
            kills = mapOf(ME to ENEMIES.mapIndexed { index, enemy -> kill(10_000L + 5_000L * index, ME, enemy) }),
            damage = mapOf(ME to ENEMIES.map { hit(it, 150, head = 1, body = 1) }),
        ),
        round(
            num = 6, winner = "Red", winnerRole = "Defender", result = "Bomb defused", code = "Defuse",
            absent = setOf(ME),
            kills = mapOf(E1 to listOf(kill(20_000, E1, A1))),
        ),
        quietWin(7, "Attacker"),
        quietWin(8, "Attacker"),
        quietWin(9, "Attacker"),
        quietWin(10, "Attacker"),
        round(
            num = 11, winner = "Blue", winnerRole = null, result = "Bomb detonated", code = "Detonate", planter = A2,
            kills = mapOf(A2 to listOf(kill(20_000, A2, E2, damageItem = PHANTOM))),
        ),
        quietWin(12, "Defender", blue = 800, red = 800, myWeapon = CLASSIC),
        round(
            num = 13, winner = "Blue", winnerRole = "Defender", result = "Eliminated", code = "Elimination",
            kills = mapOf(
                E1 to listOf(kill(20_000, E1, ME)),
                A1 to listOf(kill(23_000, A1, E1, damageItem = PHANTOM)),
            ),
            damage = mapOf(
                ME to listOf(hit(E1, 30, leg = 1)),
                E1 to listOf(hit(ME, 160, head = 1, body = 1)),
            ),
        ),
        round(
            num = 14, winner = "Blue", winnerRole = null, result = "Bomb defused", code = "Defuse",
            planter = E2, defuser = A1,
            kills = mapOf(A2 to listOf(kill(20_000, A2, E2, damageItem = PHANTOM))),
        ),
        quietWin(15, role = null),
    ),
).replace("\"premierMatchInfo\": {}", "\"premierMatchInfo\": {}, \"newField\": [1, 2, 3]")

/**
 * 4대1로 이긴 스파이크 돌격이고 팀마다 두 명씩만 둡니다.
 * 이 응답은 `roundNum`을 1부터 줍니다.
 * 실제 응답은 한쪽일 텐데 어느 쪽이든 1부터 세는 [com.ovalit.core.model.Round.number]로 옮기는지 봅니다.
 */
internal val SpikeRushMatch: String = run {
    val blue = listOf(ME, A1)
    val red = listOf(E1, E2)
    fun spikeRound(num: Int, winner: String, role: String, result: String, code: String, kills: Map<String, List<String>> = emptyMap()) =
        round(num, winner, role, result, code, kills = kills, bluePlayers = blue, redPlayers = red, myWeapon = PHANTOM)
    matchJson(
        queueId = "spikerush",
        players = listOf(
            player(ME, "Blue", RAZE, kills = 4, deaths = 2, assists = 0, score = 1_300, roundsPlayed = 5),
            player(A1, "Blue", SAGE, kills = 3, deaths = 3, assists = 2, score = 1_000, roundsPlayed = 5),
            player(E1, "Red", JETT, kills = 3, deaths = 4, assists = 1, score = 900, roundsPlayed = 5),
            player(E2, "Red", JETT, kills = 2, deaths = 3, assists = 1, score = 800, roundsPlayed = 5),
        ),
        teams = listOf(team("Blue", won = true, roundsWon = 4), team("Red", won = false, roundsWon = 1)),
        rounds = listOf(
            spikeRound(1, "Blue", "Attacker", "Bomb detonated", "Detonate", kills = mapOf(ME to listOf(kill(9_000, ME, E1)))),
            spikeRound(2, "Blue", "Attacker", "Eliminated", "Elimination"),
            spikeRound(3, "Red", "Defender", "Eliminated", "Elimination"),
            spikeRound(4, "Blue", "Defender", "Eliminated", "Elimination"),
            spikeRound(5, "Blue", "Defender", "Bomb defused", "Defuse"),
        ),
    )
}

internal const val DM1 = "p-dm-1"
internal const val DM2 = "p-dm-2"
internal const val DM3 = "p-dm-3"

/**
 * 네 명이 각자 싸운 데스매치입니다.
 * Riot 문서대로 `teams[]`가 사람마다 한 줄이고 팀 ID가 그 사람의 PUUID이며 `numPoints`가 킬입니다.
 * 라운드 기록이 한 라운드로 온다고 보고 맞힌 부위를 거기에 둡니다.
 * `roundsPlayed`는 1로 옵니다.
 */
internal val DeathmatchMatch: String = matchJson(
    queueId = "deathmatch",
    players = listOf(
        player(ME, ME, RAZE, kills = 25, deaths = 30, assists = 2, score = 6_000, roundsPlayed = 1),
        player(DM1, DM1, JETT, kills = 40, deaths = 20, assists = 1, score = 9_000, roundsPlayed = 1),
        player(DM2, DM2, JETT, kills = 31, deaths = 28, assists = 4, score = 7_000, roundsPlayed = 1),
        player(DM3, DM3, SAGE, kills = 25, deaths = 33, assists = 3, score = 5_800, roundsPlayed = 1),
    ),
    teams = listOf(
        team(ME, won = false, roundsWon = 0, points = 25),
        team(DM1, won = true, roundsWon = 1, points = 40),
        team(DM2, won = false, roundsWon = 0, points = 31),
        team(DM3, won = false, roundsWon = 0, points = 25),
    ),
    rounds = listOf(
        round(
            num = 0, winner = DM1, winnerRole = null, result = "", code = "",
            bluePlayers = listOf(ME, DM1, DM2, DM3), redPlayers = emptyList(), blue = 0,
            kills = mapOf(ME to listOf(kill(30_000, ME, DM1), kill(31_000, ME, DM2))),
            damage = mapOf(ME to listOf(hit(DM1, 3_000, head = 20, body = 60, leg = 5))),
        ),
    ),
)

/**
 * 두 명씩 여덟 팀이 등수를 다툰 건틀릿: 글리치입니다.
 * 큐 ID를 몰라 지어냈고 기타로 옮겨야 합니다.
 * 등수가 응답에 없어서 `numPoints`로 셉니다.
 * 우리 팀(`Team1`)은 2점으로 `Team8`과 같이 2등입니다.
 * 라운드 기록은 비어 있습니다.
 */
internal val GauntletMatch: String = run {
    val points = listOf(2, 3, 1, 1, 0, 0, 0, 2)
    val people = (1..8).flatMap { team -> listOf("Team$team" to "p-g$team-a", "Team$team" to "p-g$team-b") }
        .map { (team, puuid) -> if (puuid == "p-g1-a") team to ME else team to puuid }
    matchJson(
        queueId = "gauntletglitch",
        mapId = "/Game/Maps/Gauntlet/Arena",
        players = people.map { (team, puuid) ->
            player(puuid, team, "0d1c2b3a-0000-4000-8000-0000000000aa", kills = 4, deaths = 3, assists = 1, score = 900, roundsPlayed = 3)
        },
        teams = points.mapIndexed { index, point -> team("Team${index + 1}", won = point == 3, roundsWon = point, points = point) },
        rounds = emptyList(),
    )
}

internal const val FRIEND = "p-friend"

/**
 * 내가 안 뛴 친구 경기를 서버가 가려서 준 모양입니다(server/src/redact.ts).
 * 친구 말고는 `anon-N`이고 이름과 태그가 비었으며 플레이어 카드, 칭호, 계정 레벨이 빠졌습니다.
 * 파티 ID도 경기 안에서만 통하는 값입니다.
 * 친구는 블루 팀이고 2대1로 이깁니다.
 */
internal val RedactedFriendMatch: String = run {
    val blue = listOf(FRIEND, "anon-1")
    val red = listOf("anon-2", "anon-3")
    fun redacted(puuid: String, team: String, kills: Int) =
        """{"puuid": "$puuid", "gameName": "", "tagLine": "", "teamId": "$team", "partyId": "party-2", "characterId": "$JETT",
           "stats": {"score": 500, "roundsPlayed": 3, "kills": $kills, "deaths": 2, "assists": 0, "playtimeMillis": 300000},
           "competitiveTier": 12, "isObserver": false}"""
    matchJson(
        players = listOf(
            player(FRIEND, "Blue", RAZE, kills = 3, deaths = 1, assists = 0, score = 800, roundsPlayed = 3, name = "민석"),
            redacted("anon-1", "Blue", kills = 1),
            redacted("anon-2", "Red", kills = 1),
            redacted("anon-3", "Red", kills = 0),
        ),
        coaches = listOf("""{"puuid": "anon-4", "teamId": "Red"}"""),
        teams = listOf(team("Blue", won = true, roundsWon = 2), team("Red", won = false, roundsWon = 1)),
        rounds = listOf(
            round(
                0, "Blue", "Attacker", "Eliminated", "Elimination", bluePlayers = blue, redPlayers = red,
                kills = mapOf(FRIEND to listOf(kill(7_000, FRIEND, "anon-2"), kill(9_000, FRIEND, "anon-3"))),
                damage = mapOf(FRIEND to listOf(hit("anon-2", 150, head = 1, body = 1), hit("anon-3", 140, body = 3))),
            ),
            round(
                1, "Red", "Defender", "Eliminated", "Elimination", bluePlayers = blue, redPlayers = red,
                kills = mapOf("anon-2" to listOf(kill(11_000, "anon-2", FRIEND))),
            ),
            round(
                2, "Blue", "Attacker", "Eliminated", "Elimination", bluePlayers = blue, redPlayers = red,
                kills = mapOf(FRIEND to listOf(kill(14_000, FRIEND, "anon-2", assistants = listOf("anon-1")))),
            ),
        ),
    )
}
