package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private val OneDayLater = Instant.fromEpochMilliseconds(0) + 1.days

class InsightTest {

    @Test
    fun `공수 생존율이 10퍼센트포인트 벌어지면 문장을 만든다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 26, died = 14)

        val insight = assertNotNull(matches.insight(Role.SENTINEL))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(InsightSubject.OnSide(Side.DEFENSE), insight.weak.subject)
        assertEquals(0.65, insight.weak.value)
        assertEquals(0.75, insight.other.value)
    }

    @Test
    fun `9퍼센트포인트 차이면 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 75, died = 25) + side(Side.DEFENSE, survived = 66, died = 34)

        assertNull(matches.insight(Role.SENTINEL))
    }

    @Test
    fun `한쪽이라도 40라운드에 못 미치면 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 36, died = 3) + side(Side.DEFENSE, survived = 10, died = 30)

        assertNull(matches.insight(Role.SENTINEL))
    }

    @Test
    fun `진영을 모르는 라운드는 어느 쪽에도 넣지 않는다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) +
            side(Side.DEFENSE, survived = 20, died = 10) +
            side(side = null, survived = 0, died = 10)

        assertNull(matches.insight(Role.SENTINEL))
    }

    // 감시자는 생존율이 우선이다. 첫 교전 승률이 더 벌어져도 생존율부터 본다.
    @Test
    fun `역할의 우선 지표가 기준을 넘으면 그것부터 고른다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10, openedByMe = 10) +
            side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(Role.SENTINEL))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(true, insight.isRolePriority)
    }

    @Test
    fun `우선 지표가 기준에 못 미치면 가장 벌어진 다른 지표를 고른다`() {
        val matches = side(Side.ATTACK, survived = 20, died = 20, openedByMe = 20) +
            side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(Role.SENTINEL))

        assertEquals(InsightMetric.KAST, insight.metric)
        assertEquals(false, insight.isRolePriority)
    }

    // CLAUDE.md 역할군 표: 전략가와 감시자는 퍼블을 크게 띄우지 않는다
    @Test
    fun `전략가는 첫 교전 승률로 문장을 만들지 않는다`() {
        val matches = side(Side.ATTACK, survived = 0, died = 40, openedByMe = 20) +
            side(Side.DEFENSE, survived = 0, died = 40)

        assertEquals(InsightMetric.KAST, matches.insight(Role.CONTROLLER)?.metric)
        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, matches.insight(Role.DUELIST)?.metric)
    }

    @Test
    fun `피해량은 기간 평균의 15퍼센트만큼 벌어져야 한다`() {
        val wide = side(Side.ATTACK, survived = 40, died = 0, damage = 170) +
            side(Side.DEFENSE, survived = 40, died = 0, damage = 130)
        val narrow = side(Side.ATTACK, survived = 40, died = 0, damage = 160) +
            side(Side.DEFENSE, survived = 40, died = 0, damage = 140)

        assertEquals(InsightMetric.DAMAGE, wide.insight(Role.DUELIST)?.metric)
        assertNull(narrow.insight(Role.DUELIST))
    }

    // 기타 모드는 라운드 수와 크레드 규칙이 달라 공수 비교도 맞지 않는다
    @Test
    fun `기타 모드는 개선 포인트 문장을 만들지 않는다`() {
        fun week(queue: Queue) = List(5) {
            side(Side.ATTACK, survived = 6, died = 2, queue = queue) + side(Side.DEFENSE, survived = 4, died = 4, queue = queue)
        }.flatten()
        fun insight(queue: Queue, filter: QueueFilter) =
            (week(queue).weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, queueFilter = filter) as WeeklyReport.Ready).insight

        assertNotNull(insight(Queue.COMPETITIVE, QueueFilter.COMPETITIVE))
        assertNull(insight(Queue.SPIKE_RUSH, QueueFilter.OTHER))
    }

    // 감시자는 퍼블 쪽을 크게 띄우지 않지만, 에임 올리기를 골랐으면 첫 교전 승률부터 본다
    @Test
    fun `관심사 지표가 기준을 넘으면 역할 기준보다 먼저 고른다`() {
        val insight = assertNotNull(aimGap().insight(Role.SENTINEL, Focus.AIM))

        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, insight.metric)
        assertEquals(Focus.AIM, insight.focus)
        assertEquals(false, insight.isRolePriority)
        assertEquals(InsightMetric.KAST, aimGap().insight(Role.SENTINEL)?.metric)
    }

    @Test
    fun `라운드 운영을 고르면 공수별 포스바이 승률도 본다`() {
        val matches = buys(Side.ATTACK, won = 15, lost = 5) + buys(Side.DEFENSE, won = 5, lost = 15)

        val insight = assertNotNull(matches.insight(Role.CONTROLLER, Focus.ROUND_PLAY))

        assertEquals(InsightMetric.FORCE_BUY_WIN_RATE, insight.metric)
        assertEquals(Focus.ROUND_PLAY, insight.focus)
        // 관심사가 아니면 이코·포스바이·풀바이 승률은 후보가 아니다
        assertNull(matches.insight(Role.CONTROLLER))
    }

    @Test
    fun `관심사 지표가 표본이나 기준에 못 미치면 역할 기준으로 돌아간다`() {
        val matches = buys(Side.ATTACK, won = 8, lost = 2) + buys(Side.DEFENSE, won = 2, lost = 8) +
            side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(Role.SENTINEL, Focus.ROUND_PLAY))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(null, insight.focus)
        assertEquals(true, insight.isRolePriority)
    }

    // 역할을 모르는 경기라 관심사가 없으면 가장 벌어진 첫 교전 승률이 뽑힌다
    @Test
    fun `홈 리포트의 개선 포인트도 관심사를 따른다`() {
        val matches = List(3) { aimGap() }.flatten()
        fun insight(focus: Focus) =
            (matches.weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, focus = focus) as WeeklyReport.Ready).insight

        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, insight(Focus.NONE)?.metric)
        assertEquals(InsightMetric.KAST, insight(Focus.CONSISTENCY)?.metric)
    }

    // 사용자 요청(2026-09-27): 멀티킬 라운드 비율도 공수로 나눠 본다. 타격대의 우선 지표인 첫 교전 승률은 표본이 모자라다.
    @Test
    fun `멀티킬 라운드 비율이 공수로 벌어지면 문장을 만든다`() {
        val insight = assertNotNull(multiKillGap().insight(Role.DUELIST))

        assertEquals(InsightMetric.MULTI_KILL_RATE, insight.metric)
        assertEquals(false, insight.isRolePriority)
    }

    // 동적 칸처럼 K/D를 크게 띄우지 않는 역할은 멀티킬도 빼지만, 에임 올리기를 골랐으면 본다
    @Test
    fun `척후대와 전략가는 에임 올리기를 고르지 않았으면 멀티킬로 문장을 만들지 않는다`() {
        for (role in listOf(Role.INITIATOR, Role.CONTROLLER)) {
            assertNull(multiKillGap().insight(role), "$role")
            val insight = assertNotNull(multiKillGap().insight(role, Focus.AIM), "$role")
            assertEquals(InsightMetric.MULTI_KILL_RATE, insight.metric)
            assertEquals(Focus.AIM, insight.focus)
        }
        assertEquals(InsightMetric.MULTI_KILL_RATE, multiKillGap().insight(Role.SENTINEL)?.metric)
    }

    // 사용자 요청(2026-09-27): 공수만 보지 않고 요원, 맵, 무기도 견준다
    @Test
    fun `같은 역할의 요원끼리 견준다`() {
        val matches = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15) + agentMatches(Raze, Role.DUELIST, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(InsightSubject.OnAgent(Raze), insight.weak.subject)
        assertEquals(InsightSubject.OtherAgents(Role.DUELIST, listOf(Jett)), insight.other.subject)
        assertEquals(0.5, insight.weak.value)
        assertEquals(0.75, insight.other.value)
    }

    // CLAUDE.md 역할군: 역할이 다르면 같은 숫자도 뜻이 뒤집힌다
    @Test
    fun `역할이 다른 요원끼리는 견주지 않는다`() {
        val matches = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15) + agentMatches(Omen, Role.CONTROLLER, survived = 20, died = 20)

        assertNull(matches.insight(role = null))
    }

    @Test
    fun `맵끼리 견준다`() {
        val matches = mapMatches(Ascent, survived = 45, died = 15) + mapMatches(Haven, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightSubject.OnMap(Haven), insight.weak.subject)
        assertEquals(InsightSubject.OtherMaps, insight.other.subject)
    }

    // 맵 A의 격차는 20%p지만 40라운드뿐이고, 공수 격차는 17%p지만 절반씩 뛰었다. 나에게 영향이 큰 공수를 적는다.
    @Test
    fun `격차가 더 커도 적게 뛴 쪽보다 많이 뛴 쪽의 격차를 먼저 적는다`() {
        val matches = mapSides(Ascent, attack = 100 to 80, defense = 100 to 60) + mapSides(Haven, attack = 20 to 10, defense = 20 to 10)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightSubject.OnSide(Side.DEFENSE), insight.weak.subject)
        // 맵만 견주면 헤이븐이 뽑힌다
        assertEquals(InsightSubject.OnMap(Haven), (mapSides(Ascent, 200 to 140, null) + mapSides(Haven, 40 to 20, null)).insight(role = null)?.weak?.subject)
    }

    // 권총과 소총을 견주면 이코 라운드와 풀바이 라운드를 견주는 셈이라 같은 계열끼리만 본다
    @Test
    fun `같은 계열의 무기끼리 헤드샷을 견준다`() {
        val matches = weaponMatches(Vandal, head = 1) + weaponMatches(Phantom, head = 3)
        val rifles = mapOf(Vandal to WeaponCategory.RIFLE, Phantom to WeaponCategory.RIFLE)

        val insight = assertNotNull(matches.insight(role = null, categories = rifles))

        assertEquals(InsightMetric.HEADSHOT_RATE, insight.metric)
        assertEquals(InsightSubject.WithWeapon(Vandal), insight.weak.subject)
        assertEquals(InsightSubject.OtherWeapons(WeaponCategory.RIFLE, listOf(Phantom)), insight.other.subject)
        assertNull(matches.insight(role = null))
        assertNull(matches.insight(role = null, categories = mapOf(Vandal to WeaponCategory.RIFLE, Phantom to WeaponCategory.SMG)))
    }

    @Test
    fun `역할을 모르면 가장 벌어진 지표를 고르고 우선 지표라고 하지 않는다`() {
        val matches = side(Side.ATTACK, survived = 30, died = 10) + side(Side.DEFENSE, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(false, insight.isRolePriority)
    }
}

/**
 * 공격에서는 퍼블 20번에 퍼데 10번, 수비에서는 퍼블 5번에 퍼데 25번입니다. 첫 교전 승률이 67%와 17%로 벌어지고,
 * 관여율도 75%와 38%로 벌어집니다. 생존율은 두 진영 모두 25%입니다.
 */
private fun aimGap() = side(Side.ATTACK, survived = 10, died = 30, openedByMe = 20) +
    side(Side.DEFENSE, survived = 10, died = 30, openedByMe = 5)

/**
 * 공격 40라운드 중 12라운드, 수비 40라운드 중 2라운드에서 적을 둘 잡습니다. 멀티킬 라운드 비율이 30%와 5%로 벌어집니다.
 * 한 번도 죽지 않아 생존율과 관여율은 두 진영 모두 100%이고, 첫 교전은 15번에 못 미칩니다.
 */
private fun multiKillGap(): List<Match> {
    fun side(side: Side, multi: Int) = List(40) { index ->
        if (index < multi) round(kill(5.0, Me, Enemy), kill(8.0, Me, OtherEnemy), side = side) else round(side = side)
    }
    return listOf(match(*(side(Side.ATTACK, multi = 12) + side(Side.DEFENSE, multi = 2)).toTypedArray()))
}

/** 한 진영의 포스바이 라운드를 경기 하나에 담습니다. 피스톨 라운드가 아니게 번호를 5로 둡니다. */
private fun buys(side: Side, won: Int, lost: Int): List<Match> {
    val rounds = List(won + lost) { index -> round(side = side, number = 5, teamLoadout = 3000, won = index < won) }
    return listOf(match(*rounds.toTypedArray()))
}

/**
 * 한 진영 라운드를 경기 하나에 담습니다. [survived]라운드는 살아남고 [died]라운드는 죽습니다.
 * 죽는 라운드 중 앞의 [openedByMe]라운드는 내가 퍼블을 딴 뒤 죽고, 나머지는 퍼데입니다. 퍼블을 딴 라운드는
 * 관여율에도 들어갑니다.
 */
private fun side(
    side: Side?,
    survived: Int,
    died: Int,
    openedByMe: Int = 0,
    damage: Int = 0,
    queue: Queue = Queue.COMPETITIVE,
): List<Match> {
    val list = List(survived) { round(damage = damage, side = side) } +
        List(died) { index ->
            if (index < openedByMe) {
                round(kill(5.0, Me, Enemy), kill(10.0, OtherEnemy, Me), damage = damage, side = side)
            } else {
                round(kill(10.0, Enemy, Me), damage = damage, side = side)
            }
        }
    return listOf(match(*list.toTypedArray(), queue = queue))
}

private val Jett = AgentId("jett")
private val Raze = AgentId("raze")
private val Omen = AgentId("omen")
private val Ascent = MapId("ascent")
private val Haven = MapId("haven")
private val Vandal = WeaponId("vandal")
private val Phantom = WeaponId("phantom")

// 살아남은 라운드와 트레이드 없이 죽은 라운드만 있어 생존율과 관여율이 같다. 진영은 모른다.
private fun agentMatches(agent: AgentId, role: Role, survived: Int, died: Int): List<Match> =
    listOf(match(*outcomes(survived, died, side = null), role = role, agent = agent))

private fun mapMatches(map: MapId, survived: Int, died: Int): List<Match> =
    listOf(match(*outcomes(survived, died, side = null), map = map))

// 맵 하나에서 공격과 수비를 (라운드, 살아남은 라운드)로 나눠 담는다. 수비가 null이면 진영을 모른다.
private fun mapSides(map: MapId, attack: Pair<Int, Int>, defense: Pair<Int, Int>?): List<Match> {
    val attackRounds = outcomes(attack.second, attack.first - attack.second, side = if (defense == null) null else Side.ATTACK)
    val defenseRounds = defense?.let { outcomes(it.second, it.first - it.second, side = Side.DEFENSE) }.orEmpty()
    return listOf(match(*(attackRounds + defenseRounds), map = map))
}

private fun outcomes(survived: Int, died: Int, side: Side?): Array<Round> =
    (List(survived) { round(side = side) } + List(died) { round(kill(10.0, Enemy, Me), side = side) }).toTypedArray()

// 그 무기를 들고 시작해 그 무기로 한 명씩 잡은 라운드 25개다. 맞힌 탄 10발 중 [head]발이 머리다.
private fun weaponMatches(weapon: WeaponId, head: Int): List<Match> = listOf(
    match(*Array(25) { round(kill(10.0, Me, Enemy, weapon = weapon), shots = Shots(head = head, body = 10 - head, leg = 0), carried = weapon) }),
)
