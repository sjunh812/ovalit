package com.ovalit.core.network

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.BuyType
import com.ovalit.core.model.Clutch
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Score
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.buyType
import com.ovalit.core.model.duels
import com.ovalit.core.model.halfScores
import com.ovalit.core.model.highlights
import com.ovalit.core.model.metrics
import com.ovalit.core.model.myHighlights
import com.ovalit.core.model.myWeapons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

/** 13대3 경쟁전 하나를 옮긴 뒤 `core/model`이 센 지표가 손으로 센 값과 같은지 봅니다. 라운드마다 일어난 일은 [CompetitiveMatch]에 있습니다. */
class CompetitiveMatchTest {

    private val roles = mapOf(AgentId(RAZE.lowercase()) to Role.DUELIST)

    private val match: Match = assertNotNull(decodeMatch(CompetitiveMatch).toMatch(PlayerId(ME), roles::get))

    @Test
    fun `경기 정보를 옮기고 콘텐츠 UUID는 소문자로 바꾼다`() {
        assertEquals(MatchId("8c1f6a2e-0000-4000-8000-000000000001"), match.id)
        assertEquals(Queue.COMPETITIVE, match.queue)
        assertEquals(ActId(ACT.lowercase()), match.act)
        assertEquals(MapId("/Game/Maps/Ascent/Ascent"), match.map)
        assertEquals(Instant.fromEpochMilliseconds(STARTED_AT), match.startedAt)
        assertEquals(2_100_000, match.lengthMillis)
        assertEquals(AgentId(RAZE.lowercase()), match.myAgent)
        assertEquals(Role.DUELIST, match.myRole)
        assertEquals(4_200, match.myCombatScore)
        assertEquals(true, match.myTeamWon)
        assertEquals(MatchFormat.ROUNDS, match.format)
    }

    @Test
    fun `관전자와 코치는 스코어보드와 우리 팀에 넣지 않는다`() {
        assertEquals(10, match.players.size)
        assertEquals(ALLIES.map(::PlayerId).toSet(), match.allies)
        assertEquals(setOf(PlayerId(ME)) + match.allies, match.players.filter { it.onMyTeam }.map { it.player }.toSet())
        assertEquals(listOf(5, 5), match.teams.map { it.members.size })
    }

    @Test
    fun `스코어는 튕겨서 못 뛴 라운드까지 세고 내 라운드에서는 뺀다`() {
        assertEquals(Score(myTeam = 13, enemyTeam = 3), match.score)
        assertEquals(listOf(Score(9, 3), Score(4, 0)), match.halfScores)
        assertEquals(15, match.rounds.size)
        assertEquals((1..16).filter { it != 7 }, match.rounds.map { it.number })
    }

    @Test
    fun `0부터 오는 roundNum을 1부터 세어 피스톨 라운드를 가린다`() {
        val buys = match.rounds.associate { it.number to it.buyType(match.queue) }
        assertEquals(BuyType.PISTOL, buys[1])
        assertEquals(BuyType.ECO, buys[2])
        assertEquals(BuyType.FORCE_BUY, buys[3])
        assertEquals(BuyType.FULL_BUY, buys[4])
        assertEquals(BuyType.PISTOL, buys[13])
    }

    @Test
    fun `팀 장비 가치는 한 사람당 평균이고 내 무기는 라운드 시작 로드아웃이다`() {
        val first = assertNotNull(match.rounds.first().economy)
        assertEquals(900, first.myLoadout)
        assertEquals(820, first.teamLoadout)
        assertEquals(800, first.enemyLoadout)
        assertEquals(WeaponId(GHOST.lowercase()), first.myWeapon)
    }

    @Test
    fun `이긴 팀 진영으로 내 진영을 가리고 없으면 스파이크를 설치한 쪽으로 가린다`() {
        val sides = match.rounds.associate { it.number to it.mySide }
        assertEquals(List(11) { Side.ATTACK }, (1..12).mapNotNull { sides[it] })
        assertEquals(Side.DEFENSE, sides[13])
        assertEquals(Side.DEFENSE, sides[14])
        assertEquals(Side.DEFENSE, sides[15])
        assertNull(sides[16])
    }

    @Test
    fun `라운드가 끝난 방식은 roundResultCode를 먼저 보고 비었으면 roundResult를 본다`() {
        val endings = match.rounds.associate { it.number to it.ending }
        assertEquals(RoundEnding.ELIMINATION, endings[1])
        assertEquals(RoundEnding.SPIKE_DEFUSED, endings[2])
        assertEquals(RoundEnding.TIME_EXPIRED, endings[3])
        assertEquals(RoundEnding.SPIKE_DETONATED, endings[12])
    }

    @Test
    fun `무기별 킬은 마지막 피해를 준 무기로 담고 스킬 킬은 무기를 비운다`() {
        val round4 = match.rounds.first { it.number == 4 }
        assertEquals(listOf(null, WeaponId(VANDAL.lowercase()), WeaponId(PHANTOM.lowercase())), round4.kills.map { it.weapon })

        val weapons = match.myWeapons().associateBy { it.weapon }
        assertEquals(8, weapons.getValue(WeaponId(VANDAL.lowercase())).kills)
        assertEquals(11, weapons.getValue(WeaponId(VANDAL.lowercase())).carriedRounds)
        assertEquals(2, weapons.getValue(WeaponId(GHOST.lowercase())).kills)
    }

    @Test
    fun `손으로 센 지표와 같다`() {
        val metrics = match.metrics()

        assertEquals(15, metrics.rounds)
        // 팀킬과 내 궁극기 자살은 킬이 아니다. 스코어보드의 11킬과 다르다.
        assertEquals(10, metrics.kills)
        // 세이지 부활로 2라운드에 두 번 죽었고, 3라운드 자살도 데스다
        assertEquals(4, metrics.deaths)
        assertEquals(1, metrics.assists)
        // 나와 우리 팀에 준 피해 300은 뺐다
        assertEquals(1_680, metrics.damage)
        assertEquals(Shots(head = 9, body = 18, leg = 2), metrics.shots)
        assertEquals(14, metrics.kastRounds)
        assertEquals(12, metrics.survivedRounds)
        // 3라운드 자살은 첫 데스가 아니고, 4라운드 팀킬은 첫 킬이 아니다
        assertEquals(3, metrics.firstKills)
        assertEquals(2, metrics.firstDeaths)
        assertEquals(3, metrics.firstKillRoundsWon)
        assertEquals(1 to 0, metrics.ecoRounds to metrics.ecoRoundsWon)
        assertEquals(1 to 0, metrics.forceBuyRounds to metrics.forceBuyRoundsWon)
        assertEquals(11 to 11, metrics.fullBuyRounds to metrics.fullBuyRoundsWon)
        assertEquals(3, metrics.multiKillRounds)
        // 2라운드 첫 데스와 14라운드 데스만 5초 안에 갚았다
        assertEquals(2, metrics.tradedDeaths)

        assertRate(280.0, metrics.acs)
        assertRate(112.0, metrics.adr)
        assertRate(2.5, metrics.kd)
        assertRate(2.75, metrics.kda)
        assertRate(9 / 29.0, metrics.headshotRate)
        assertRate(14 / 15.0, metrics.kast)
        assertRate(0.5, metrics.tradedDeathRate)
    }

    @Test
    fun `5라운드는 1대2 클러치 성공이고 6라운드는 에이스다`() {
        val highlights = match.highlights()
        assertEquals(Clutch(against = 2, won = true), highlights[4].clutch)
        assertEquals(true, highlights[5].ace)
        assertEquals(1, match.myHighlights.aces)
        assertEquals(1, match.myHighlights.clutches)
    }

    @Test
    fun `상대마다 주고받은 피해를 담는다`() {
        val duel = match.duels().first { it.opponent == PlayerId(E1) }
        assertEquals(3, duel.kills)
        assertEquals(2, duel.deaths)
        assertEquals(530, duel.damageDealt)
        assertEquals(310, duel.damageTaken)
    }

    @Test
    fun `스코어보드는 응답의 stats를 그대로 옮기고 피해량과 맞힌 부위만 라운드에서 더한다`() {
        val me = assertNotNull(match.myScoreline)
        assertEquals("봉봉이#KR1", me.riotId)
        assertEquals(AgentId(RAZE.lowercase()), me.agent)
        assertEquals(16, me.tier)
        assertEquals(PlayerCardId(MY_CARD.lowercase()), me.playerCard)
        assertEquals(11, me.kills)
        assertEquals(4, me.deaths)
        assertEquals(15, me.roundsPlayed)
        assertEquals(1_680, me.damage)
        assertEquals(Shots(head = 9, body = 18, leg = 2), me.shots)

        // 티어 0은 티어가 없는 것이다
        assertNull(match.players.first { it.player == PlayerId(A1) }.tier)
    }
}

internal fun assertRate(expected: Double, actual: Double?) {
    assertNotNull(actual, "비율이 null이다. 분모가 0인지 확인할 것")
    assertEquals(expected, actual, absoluteTolerance = 0.0001)
}
