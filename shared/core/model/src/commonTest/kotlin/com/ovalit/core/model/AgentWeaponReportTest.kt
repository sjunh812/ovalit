package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val ThisMonday = LocalDate(2026, 9, 21)
private val Now = LocalDateTime(2026, 9, 23, 12, 0).toInstant(Seoul)
private val Jett = AgentId("jett")
private val Omen = AgentId("omen")
private val Phantom = WeaponId("phantom")
private val Vandal = WeaponId("vandal")
private val Classic = WeaponId("classic")

class AgentWeaponReportTest {

    // CLAUDE.md: 액트 경계를 넘는 평균은 만들지 않는다
    @Test
    fun `요원과 무기 화면은 가장 최근 경기의 액트만 센다`() {
        val matches = List(3) { game(act = ActId("new")) } + List(4) { game(act = ActId("old"), weeksAgo = 3) }

        assertEquals(3, matches.currentActMatches(QueueFilter.COMPETITIVE_AND_UNRATED).size)
    }

    @Test
    fun `주 역할은 판 수가 아니라 라운드 수로 정한다`() {
        val matches = List(3) { game(agent = Jett, role = Role.DUELIST, rounds = 13) } +
            List(2) { game(agent = Omen, role = Role.CONTROLLER, rounds = 24) }

        val report = matches.agentReport()

        assertEquals(Role.CONTROLLER, report.mainRole)
        assertEquals(listOf(Role.CONTROLLER, Role.DUELIST), report.roles.map { it.role })
    }

    @Test
    fun `주 역할 비중은 역할을 모르는 경기를 빼고 나눈다`() {
        val matches = List(2) { game(agent = Omen, role = Role.CONTROLLER, rounds = 24) } +
            game(agent = Jett, role = Role.DUELIST, rounds = 16) +
            game(role = null, rounds = 20)

        assertRate(48.0 / 64, matches.agentReport().mainRoleShare)
    }

    @Test
    fun `비긴 경기는 승률 분모에서 뺀다`() {
        val matches = listOf(game(won = true), game(won = true), game(won = false), game(won = null))

        val jett = matches.agentReport().agents.single()

        assertEquals(2.0 / 3, jett.winRate)
    }

    @Test
    fun `5판에 못 미친 요원은 표본 부족이다`() {
        val report = (List(5) { game(agent = Jett) } + List(4) { game(agent = Omen) }).agentReport()

        assertEquals(listOf(true, false), report.agents.map { it.isMeasurable })
    }

    // 응답에는 맞힌 탄이 라운드별로만 있어서 무기별로 나눌 수 없다
    @Test
    fun `헤드샷은 내 킬이 전부 그 무기로 난 라운드만 센다`() {
        val onlyPhantom = round(myKill(Phantom), shots = Shots(head = 3, body = 7, leg = 0))
        val mixed = round(myKill(Phantom), myKill(Vandal, at = 20.0), shots = Shots(head = 10, body = 0, leg = 0))
        val noKill = round(shots = Shots(head = 10, body = 0, leg = 0))

        val phantom = listOf(match(onlyPhantom, mixed, noKill)).weaponStats().first { it.weapon == Phantom }

        assertEquals(2, phantom.kills)
        assertEquals(1, phantom.singleWeaponRounds)
        assertEquals(0.3, phantom.headshotRate)
    }

    // 죽을 때 든 무기와 피해를 준 무기는 응답에 없다. 라운드를 시작할 때 든 무기로 나눈다.
    @Test
    fun `데스와 어시스트와 피해량은 그 무기를 들고 시작한 라운드에서 센다`() {
        val died = round(myKill(Vandal), kill(20.0, Enemy, Me), damage = 150, carried = Phantom)
        val assisted = round(kill(10.0, Ally, Enemy, assistedBy = setOf(Me)), damage = 90, carried = Phantom)
        val other = round(kill(10.0, Enemy, Me), damage = 40, carried = Vandal)

        val stats = listOf(match(died, assisted, other)).weaponStats()
        val phantom = stats.single { it.weapon == Phantom }

        assertEquals(0, phantom.kills)
        assertEquals(2, phantom.carriedRounds)
        assertEquals(1, phantom.deaths)
        assertEquals(1, phantom.assists)
        assertEquals(120.0, phantom.damagePerRound)
        assertEquals(1, stats.single { it.weapon == Vandal }.kills)
    }

    @Test
    fun `들고 시작한 라운드가 20에 못 미치면 피해량은 표본 부족이다`() {
        val rounds = List(19) { round(myKill(Phantom), damage = 100, carried = Phantom) }

        val phantom = listOf(match(*rounds.toTypedArray())).weaponStats().single()

        assertFalse(phantom.isCarriedMeasurable)
        assertEquals(null, phantom.value(WeaponMetric.DAMAGE_PER_ROUND))
        assertTrue(listOf(match(*(rounds + rounds.first()).toTypedArray())).weaponStats().single().isCarriedMeasurable)
    }

    @Test
    fun `스킬 킬이 섞인 라운드는 단일 무기 라운드가 아니다`() {
        val withAbility = round(myKill(Phantom), myKill(weapon = null, at = 20.0), shots = Shots(1, 1, 0))

        val phantom = listOf(match(withAbility)).weaponStats().single()

        assertEquals(0, phantom.singleWeaponRounds)
    }

    @Test
    fun `무기 목록은 킬이 많은 순서다`() {
        val matches = List(5) {
            game(rounds = 0, extra = listOf(round(myKill(Vandal)), round(myKill(Phantom)), round(myKill(Phantom))))
        }

        val report = matches.weaponReport(now = Now, timeZone = Seoul)

        assertEquals(listOf(Phantom, Vandal), report.weapons.map { it.weapon })
        assertEquals(15, report.kills)
    }

    @Test
    fun `위쪽 무기도 킬이 많은 순서다`() {
        val matches = List(5) {
            game(
                rounds = 0,
                extra = listOf(
                    round(myKill(Phantom), myKill(Phantom, at = 20.0), carried = Phantom),
                    round(myKill(Vandal), carried = Vandal),
                    round(carried = Vandal),
                    round(carried = Classic),
                ),
            )
        }

        val report = matches.weaponReport(now = Now, timeZone = Seoul)

        assertEquals(listOf(Phantom, Vandal, Classic), report.highlights.map { it.act.weapon })
    }

    @Test
    fun `평소보다 크게 오른 무기만 움직였다고 본다`() {
        // 앞 8주는 헤드샷 20% 안팎, 이번 주는 40%. 피해량은 늘 140 안팎이다.
        val usual = (1..8).flatMap { weeksAgo ->
            val even = weeksAgo % 2 == 0
            phantomWeek(weeksAgo, head = if (even) 19 else 21, damage = if (even) 138 else 142)
        }
        val matches = phantomWeek(weeksAgo = 0, head = 40) + usual

        val phantom = matches.weaponReport(now = Now, timeZone = Seoul).highlights.single()

        assertEquals(Movement.MOVED, phantom.movement(WeaponMetric.HEADSHOT_RATE))
        assertEquals(Movement.STEADY, phantom.movement(WeaponMetric.DAMAGE_PER_ROUND))
        assertEquals(4, phantom.baselineWeeks)
    }

    // 열 라운드짜리 주의 0%와 100%까지 넣으면 변동폭이 커져서 40%로 오른 걸 놓친다
    @Test
    fun `변동폭은 한 무기만 쓴 라운드가 20을 넘긴 주로만 잰다`() {
        val usual = (1..4).flatMap { phantomWeek(it, head = if (it % 2 == 0) 19 else 21) }
        val thin = (5..8).flatMap { phantomWeek(it, head = if (it % 2 == 0) 0 else 40, rounds = 10) }
        val matches = phantomWeek(weeksAgo = 0, head = 40) + usual + thin

        assertEquals(Movement.MOVED, matches.weaponReport(now = Now, timeZone = Seoul).highlights.single().movement(WeaponMetric.HEADSHOT_RATE))
    }

    @Test
    fun `라운드가 모자란 주가 많으면 판단하지 않는다`() {
        val usual = (1..3).flatMap { phantomWeek(it, head = 20) }
        val matches = phantomWeek(weeksAgo = 0, head = 40) + usual

        assertEquals(Movement.UNKNOWN, matches.weaponReport(now = Now, timeZone = Seoul).highlights.single().movement(WeaponMetric.HEADSHOT_RATE))
    }

    @Test
    fun `무기의 킬데스 비율은 그 무기로 낸 킬을 그 무기를 들고 시작한 라운드의 데스로 나눈다`() {
        val died = round(myKill(Phantom), myKill(Phantom, at = 15.0), kill(20.0, Enemy, Me), carried = Phantom)
        val survived = round(myKill(Phantom), carried = Phantom)

        assertEquals(3.0, listOf(match(died, survived)).weaponStats().single().kd)
        assertEquals(null, listOf(match(survived)).weaponStats().single().kd)
    }

    // 한 줄에 이번 기간 헤드샷과 이번 액트 K/D가 섞이면 어느 숫자가 언제 것인지 모른다
    @Test
    fun `기간에 들고 시작한 라운드가 모자라면 줄 전체를 이번 액트로 띄운다`() {
        val thisWeek = phantomWeek(weeksAgo = 0, head = 40, carried = 15)

        val phantom = thisWeek.weaponReport(now = Now, timeZone = Seoul).highlights.single()

        assertEquals(null, phantom.current)
        assertEquals(Movement.UNKNOWN, phantom.movement(WeaponMetric.HEADSHOT_RATE))
    }

    @Test
    fun `피해량은 헤드샷과 따로 평소보다 크게 움직였는지 본다`() {
        // 헤드샷은 늘 20% 안팎이고 피해량만 이번 주에 크게 오른다
        val usual = (1..8).flatMap { weeksAgo ->
            phantomWeek(weeksAgo, head = if (weeksAgo % 2 == 0) 19 else 21, damage = if (weeksAgo % 2 == 0) 138 else 142)
        }
        val matches = phantomWeek(weeksAgo = 0, head = 20, damage = 200) + usual

        val phantom = matches.weaponReport(now = Now, timeZone = Seoul).highlights.single()

        assertEquals(Movement.MOVED, phantom.movement(WeaponMetric.DAMAGE_PER_ROUND))
        assertEquals(Movement.STEADY, phantom.movement(WeaponMetric.HEADSHOT_RATE))
        assertEquals(200.0, phantom.current?.damagePerRound)
    }
}

private fun myKill(weapon: WeaponId?, at: Double = 10.0) =
    KillEvent((at * 1000).toLong(), Me, Enemy, emptySet(), weapon)

private fun game(
    act: ActId = ActId("act"),
    agent: AgentId = Jett,
    role: Role? = Role.DUELIST,
    rounds: Int = 13,
    won: Boolean? = true,
    weeksAgo: Int = 0,
    extra: List<Round> = emptyList(),
): Match {
    val date = ThisMonday.minus(weeksAgo, DateTimeUnit.WEEK).plus(1, DateTimeUnit.DAY)
    val all = List(rounds) { quietRound() } + extra
    return match(
        *all.toTypedArray(),
        act = act,
        role = role,
        agent = agent,
        won = won,
        startedAt = date.atTime(21, 0).toInstant(Seoul),
    )
}

/**
 * 팬텀만 쓴 [rounds]라운드를 5경기에 나눠 담는다. 라운드마다 4발을 맞히고 모두 합쳐 [head]발이 머리다. 앞의 [carried]라운드는
 * 팬텀을 들고 시작해 라운드마다 [damage]를 넣는다.
 */
private fun phantomWeek(weeksAgo: Int, head: Int, rounds: Int = 25, carried: Int = rounds, damage: Int = 140): List<Match> {
    val all = List(rounds) { index ->
        val heads = if (index < head / 4) 4 else if (index == head / 4) head % 4 else 0
        round(
            myKill(Phantom),
            shots = Shots(head = heads, body = 4 - heads, leg = 0),
            damage = damage,
            carried = Phantom.takeIf { index < carried },
        )
    }
    return all.chunked(rounds / 5).map { game(rounds = 0, weeksAgo = weeksAgo, extra = it) }
}
