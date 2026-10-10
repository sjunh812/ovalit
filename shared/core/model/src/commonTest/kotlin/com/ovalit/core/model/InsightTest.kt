package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private val OneDayLater = Instant.fromEpochMilliseconds(0) + 1.days

class InsightTest {

    // 네 판 모두 공격 40라운드 중 30라운드, 수비 40라운드 중 20라운드를 살아남는다. 75%와 50%다.
    @Test
    fun `공수 생존율이 크게 벌어지면 높은 쪽을 주어로 문장을 만든다`() {
        val insight = assertNotNull(sides(attack = 30 to 10, defense = 20 to 20).insight(Role.SENTINEL))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(InsightSubject.OnSide(Side.ATTACK), insight.lead.subject)
        assertEquals(0.75, insight.lead.value)
        assertEquals(InsightSubject.OnSide(Side.DEFENSE), insight.other.subject)
        assertEquals(0.5, insight.other.value)
        assertEquals(true, insight.leadIsHigher)
    }

    @Test
    fun `9퍼센트포인트 차이면 표본이 많아도 문장을 만들지 않는다`() {
        assertNull(sides(attack = 75 to 25, defense = 66 to 34, games = 10).insight(Role.SENTINEL))
    }

    // 75%와 65%를 네 판 160라운드씩으로 쟀다. 같은 사람이 같은 실력으로 뛰어도 이 정도는 우연히 벌어진다.
    @Test
    fun `격차가 기준을 넘어도 우연히 벌어질 만한 폭이면 문장을 만들지 않는다`() {
        val matches = sides(attack = 30 to 10, defense = 26 to 14)

        assertNull(matches.insight(Role.SENTINEL))
        // 우연을 거르지 않으면 문장이 된다
        assertEquals(InsightMetric.SURVIVAL_RATE, matches.insight(Role.SENTINEL, minZ = 0.0)?.metric)
    }

    // 합치면 75%와 50%로 첫 테스트와 같지만 수비가 판마다 0%와 100%를 오갔다.
    // 그날 컨디션이 크게 흔들린 것이라 우연과 가를 수 없다.
    @Test
    fun `경기마다 크게 흔들리면 같은 격차도 우연으로 본다`() {
        val matches = listOf(40 to 0, 20 to 20, 40 to 0, 20 to 20).zip(listOf(0 to 40, 40 to 0, 0 to 40, 40 to 0)) { attack, defense ->
            match(*(outcomes(attack.first, attack.second, Side.ATTACK) + outcomes(defense.first, defense.second, Side.DEFENSE)))
        }

        assertNull(matches.insight(Role.SENTINEL))
    }

    // 제트는 60판 내내 40%였고 레이즈는 세 판 중 두 판은 다 살고 한 판은 다 죽었다.
    // 두 쪽을 모아 재면 흔들림이 작아 보이지만 레이즈만 보면 세 판이 들쭉날쭉해서 67%를 믿을 수 없다.
    @Test
    fun `한쪽 경기가 크게 흔들리면 다른 쪽이 고르더라도 우연으로 본다`() {
        val raze = listOf(40 to 0, 40 to 0, 0 to 40).map { (survived, died) -> match(*outcomes(survived, died, side = null), role = Role.DUELIST, agent = Raze) }

        assertNull((agentMatches(Jett, Role.DUELIST, survived = 16, died = 24, games = 60) + raze).insight(role = null))
    }

    // 제트는 판마다 다 살거나 다 죽었다. 그런 사람이면 레이즈 세 판을 모두 산 것도 우연히 좋은 날이 겹친 것일 수 있다.
    // 레이즈 세 판만 보면 흔들림이 0이라 두 쪽 경기를 모아 잰다.
    @Test
    fun `경기가 적은 쪽은 두 쪽 경기를 모아 잰 흔들림도 본다`() {
        val jett = List(30) { index -> match(*outcomes(if (index % 2 == 0) 40 else 0, if (index % 2 == 0) 0 else 40, side = null), role = Role.DUELIST, agent = Jett) }

        assertNull((jett + agentMatches(Raze, Role.DUELIST, survived = 40, died = 0)).insight(role = null))
    }

    @Test
    fun `한쪽이라도 40라운드에 못 미치면 문장을 만들지 않는다`() {
        assertNull(sides(attack = 12 to 1, defense = 3 to 10, games = 3).insight(Role.SENTINEL))
    }

    @Test
    fun `진영을 모르는 라운드는 어느 쪽에도 넣지 않는다`() {
        val matches = List(4) {
            match(*(outcomes(30, 10, Side.ATTACK) + outcomes(20, 10, Side.DEFENSE) + outcomes(0, 10, side = null)))
        }

        // 진영을 모르는 라운드까지 수비로 세면 75%와 50%지만, 빼면 75%와 67%라 우연을 거르지 않아도 기준에 못 미친다
        assertNull(matches.insight(Role.SENTINEL, minZ = 0.0))
    }

    // 감시자는 생존율이 우선이다. 관여율이 더 벌어져도 생존율부터 본다.
    @Test
    fun `역할의 우선 지표가 기준을 넘으면 그것부터 고른다`() {
        val insight = assertNotNull(sides(attack = 30 to 10, defense = 20 to 20, attackOpenedByMe = 10).insight(Role.SENTINEL))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(true, insight.isRolePriority)
    }

    @Test
    fun `우선 지표가 기준에 못 미치면 가장 벌어진 다른 지표를 고른다`() {
        val insight = assertNotNull(sides(attack = 20 to 20, defense = 20 to 20, attackOpenedByMe = 20).insight(Role.SENTINEL))

        assertEquals(InsightMetric.KAST, insight.metric)
        assertEquals(false, insight.isRolePriority)
    }

    // CLAUDE.md 역할군 표: 전략가와 감시자는 첫 킬을 크게 띄우지 않는다
    @Test
    fun `전략가는 첫 교전 승률로 문장을 만들지 않는다`() {
        val matches = sides(attack = 0 to 40, defense = 0 to 40, attackOpenedByMe = 20)

        assertEquals(InsightMetric.KAST, matches.insight(Role.CONTROLLER)?.metric)
        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, matches.insight(Role.DUELIST)?.metric)
    }

    @Test
    fun `피해량은 두 쪽을 합친 평균의 15퍼센트만큼 벌어져야 한다`() {
        val wide = sides(attack = 40 to 0, defense = 40 to 0, attackDamage = 170, defenseDamage = 130)
        val narrow = sides(attack = 40 to 0, defense = 40 to 0, attackDamage = 160, defenseDamage = 140)

        assertEquals(InsightMetric.DAMAGE, wide.insight(Role.DUELIST)?.metric)
        assertNull(narrow.insight(Role.DUELIST))
    }

    // 기타 모드는 라운드 수와 크레드 규칙이 달라 공수 비교도 맞지 않는다
    @Test
    fun `기타 모드는 개선 포인트 문장을 만들지 않는다`() {
        fun insight(queue: Queue, filter: QueueFilter) =
            (sides(attack = 18 to 6, defense = 12 to 12, games = 5, queue = queue)
                .weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, queueFilter = filter) as WeeklyReport.Ready).insight

        assertNotNull(insight(Queue.COMPETITIVE, QueueFilter.COMPETITIVE))
        assertNull(insight(Queue.SPIKE_RUSH, QueueFilter.OTHER))
    }

    // 한 주 다섯 판은 공수가 비슷했지만 이번 액트 앞 주들까지 합치면 수비가 크게 낮다. 지난 액트 경기는 넣지 않는다.
    @Test
    fun `홈 리포트는 이번 액트 경기를 모두 모아 견준다`() {
        val now = Instant.fromEpochMilliseconds(0) + 30.days
        val thisWeek = sides(attack = 20 to 4, defense = 19 to 5, games = 5, startedAt = now - 1.days)
        val earlier = sides(attack = 20 to 4, defense = 8 to 16, games = 10, startedAt = now - 10.days)
        val lastAct = sides(attack = 4 to 20, defense = 24 to 0, games = 20, startedAt = now - 20.days, act = ActId("last"))

        fun insight(matches: List<Match>) = (matches.weeklyReport(now = now, timeZone = TimeZone.UTC) as WeeklyReport.Ready).insight

        assertNull(insight(thisWeek))
        val insight = assertNotNull(insight(thisWeek + earlier + lastAct))
        assertEquals(InsightSubject.OnSide(Side.ATTACK), insight.lead.subject)
        assertEquals(24 * 15, insight.lead.rounds)
        assertEquals(15, insight.matches)
        // 액트 동안의 차이가 이번 주에도 이어졌는지 이번 주 값을 붙인다
        assertEquals(100 / 120.0 to 95 / 120.0, insight.recent.values())
        // 이번 주 표본도 액트 쪽과 같은 기준(그 진영 라운드)으로 센다
        assertEquals(120, insight.recent?.lead?.rounds)
    }

    // 이번 액트 경기가 모두 이번 주 것이면 이번 주 값이 위 숫자와 같다
    @Test
    fun `기간이 이번 액트 경기를 모두 담으면 이번 주 값을 붙이지 않는다`() {
        val report = aimGap(games = 5).weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC) as WeeklyReport.Ready

        assertNull(assertNotNull(report.insight).recent)
    }

    // 이번 주 레이즈는 30라운드뿐이라 동적 칸 최소 표본(40라운드)에 못 미친다. 한쪽만 적으면 무엇과 견주는지 모른다.
    @Test
    fun `이번 주에 한쪽 표본이 모자라면 이번 주 값을 붙이지 않는다`() {
        val act = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15) + agentMatches(Raze, Role.DUELIST, survived = 20, died = 20)
        val insight = assertNotNull(act.insight(role = null))
        val shortRaze = agentMatches(Raze, Role.DUELIST, survived = 15, died = 15, games = 1)

        assertNull(insight.during(act.take(1) + shortRaze, categories = emptyMap()).recent)
        assertEquals(0.75 to 0.5, insight.during(act.take(1) + act.last(), categories = emptyMap()).recent.values())
    }

    @Test
    fun `이번 주 공수 라운드가 모자라면 이번 주 값을 붙이지 않는다`() {
        val insight = assertNotNull(sides(attack = 30 to 10, defense = 20 to 20).insight(Role.SENTINEL))

        assertNull(insight.during(sides(attack = 15 to 5, defense = 10 to 10, games = 1), categories = emptyMap()).recent)
        val recent = insight.during(sides(attack = 15 to 5, defense = 10 to 10, games = 2), categories = emptyMap()).recent
        assertEquals(0.75 to 0.5, recent.values())
    }

    @Test
    fun `무기의 이번 주 값은 S6과 같은 표본으로 센다`() {
        val matches = weaponMatches(Vandal, head = 1) + weaponMatches(Phantom, head = 3)
        val rifles = mapOf(Vandal to WeaponCategory.RIFLE, Phantom to WeaponCategory.RIFLE)
        val insight = assertNotNull(matches.insight(role = null, categories = rifles))

        // 한 경기 10라운드씩이라 두 경기면 S6 기준 20라운드를 채운다
        assertNull(insight.during(matches.take(1) + matches.takeLast(1), rifles).recent)
        assertEquals(0.3 to 0.1, insight.during(matches.take(2) + matches.takeLast(2), rifles).recent.values())
    }

    // 감시자는 첫 킬 쪽을 크게 띄우지 않지만, 에임 올리기를 골랐으면 첫 교전 승률부터 본다
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
        val matches = buys(attack = 30 to 10, defense = 10 to 30)

        val insight = assertNotNull(matches.insight(Role.CONTROLLER, Focus.ROUND_PLAY))

        assertEquals(InsightMetric.FORCE_BUY_WIN_RATE, insight.metric)
        assertEquals(Focus.ROUND_PLAY, insight.focus)
        // 관심사가 아니면 이코·포스바이·풀바이 승률은 후보가 아니다
        assertNull(matches.insight(Role.CONTROLLER))
    }

    @Test
    fun `관심사 지표가 표본이나 기준에 못 미치면 역할 기준으로 돌아간다`() {
        val matches = buys(attack = 2 to 1, defense = 1 to 2) + sides(attack = 30 to 10, defense = 20 to 20)

        val insight = assertNotNull(matches.insight(Role.SENTINEL, Focus.ROUND_PLAY))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(null, insight.focus)
        assertEquals(true, insight.isRolePriority)
    }

    // 역할을 모르는 경기라 관심사가 없으면 가장 벌어진 첫 교전 승률이 뽑힌다
    @Test
    fun `홈 리포트의 개선 포인트도 관심사를 따른다`() {
        fun insight(focus: Focus) =
            (aimGap(games = 5).weeklyReport(now = OneDayLater, timeZone = TimeZone.UTC, focus = focus) as WeeklyReport.Ready).insight

        assertEquals(InsightMetric.FIRST_DUEL_WIN_RATE, insight(Focus.NONE)?.metric)
        assertEquals(InsightMetric.KAST, insight(Focus.CONSISTENCY)?.metric)
    }

    // 멀티킬 라운드 비율도 공수로 나눠 본다. 타격대의 우선 지표인 첫 교전 승률은 두 진영이 같다.
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

    // 두 쪽 모두 이름이 있으면 높은 쪽을 주어로 둔다. "제트로 뛴 판은 레이즈보다 높아요"
    @Test
    fun `같은 역할의 요원끼리 견주고 높은 쪽을 주어로 둔다`() {
        val matches = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15) + agentMatches(Raze, Role.DUELIST, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(InsightSubject.OnAgent(Jett), insight.lead.subject)
        assertEquals(0.75, insight.lead.value)
        assertEquals(3, insight.lead.matches)
        assertEquals(InsightSubject.OnAgent(Raze), insight.other.subject)
        assertEquals(0.5, insight.other.value)
    }

    // 두 판으로는 그날 컨디션과 요원을 가를 수 없다
    @Test
    fun `한쪽이 세 판에 못 미치면 견주지 않는다`() {
        val matches = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15, games = 2) +
            agentMatches(Raze, Role.DUELIST, survived = 10, died = 50)

        assertNull(matches.insight(role = null))
    }

    // CLAUDE.md 역할군: 역할이 다르면 같은 숫자도 뜻이 뒤집힌다
    @Test
    fun `역할이 다른 요원끼리는 견주지 않는다`() {
        val matches = agentMatches(Jett, Role.DUELIST, survived = 45, died = 15) + agentMatches(Omen, Role.CONTROLLER, survived = 20, died = 20)

        assertNull(matches.insight(role = null))
    }

    // 승률은 차이가 클 때만 나온다. 10판씩이면 100%와 20%쯤은 벌어져야 우연을 넘는다.
    @Test
    fun `역할끼리는 승률을 견주고 차이가 클 때만 문장을 만든다`() {
        fun games(role: Role, agent: AgentId, won: Int, lost: Int) = List(won + lost) { index ->
            match(*outcomes(12, 12, side = null), role = role, agent = agent, won = index < won)
        }
        val wide = games(Role.CONTROLLER, Omen, won = 10, lost = 0) + games(Role.DUELIST, Jett, won = 2, lost = 8)
        val narrow = games(Role.CONTROLLER, Omen, won = 8, lost = 2) + games(Role.DUELIST, Jett, won = 4, lost = 6)

        val insight = assertNotNull(wide.insight(role = null))
        assertEquals(InsightMetric.WIN_RATE, insight.metric)
        assertEquals(InsightSubject.OnRole(Role.CONTROLLER), insight.lead.subject)
        assertEquals(InsightSubject.OnRole(Role.DUELIST), insight.other.subject)
        assertNull(narrow.insight(role = null))
    }

    @Test
    fun `맵 둘을 견주면 높은 쪽을 주어로 둔다`() {
        val matches = mapMatches(Ascent, survived = 45, died = 15) + mapMatches(Haven, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightSubject.OnMap(Ascent), insight.lead.subject)
        assertEquals(InsightSubject.OnMap(Haven), insight.other.subject)
    }

    // "다른 맵에서는 헤이븐보다 높아요"는 어색하다. 묶음과 견주면 이름 있는 쪽이 주어고 낮으면 "낮아요"다.
    @Test
    fun `나머지 여럿과 견주면 이름 있는 쪽을 주어로 둔다`() {
        val matches = mapMatches(Ascent, survived = 45, died = 15) + mapMatches(Bind, survived = 45, died = 15) +
            mapMatches(Haven, survived = 20, died = 20)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightSubject.OnMap(Haven), insight.lead.subject)
        assertEquals(InsightSubject.OtherMaps(listOf(Ascent, Bind)), insight.other.subject)
        assertEquals(false, insight.leadIsHigher)
    }

    // 헤이븐은 격차가 20%p지만 800라운드 중 160라운드이고, 공수 격차는 16%p지만 절반씩 뛰었다.
    // 나에게 영향이 큰 공수를 적는다.
    // 두 격차 모두 우연은 넘는다.
    @Test
    fun `격차가 더 커도 적게 뛴 쪽보다 많이 뛴 쪽의 격차를 먼저 적는다`() {
        val matches = mapSides(Ascent, games = 8, attack = 40 to 32, defense = 40 to 24) + mapSides(Haven, games = 5, attack = 16 to 8, defense = 16 to 8)

        val insight = assertNotNull(matches.insight(role = null))

        assertEquals(InsightSubject.OnSide(Side.ATTACK), insight.lead.subject)
        // 진영을 모르면 맵만 견주고 헤이븐이 뽑힌다
        val mapsOnly = mapSides(Ascent, games = 8, attack = 80 to 56, defense = null) + mapSides(Haven, games = 5, attack = 32 to 16, defense = null)
        assertEquals(InsightSubject.OnMap(Haven), mapsOnly.insight(role = null)?.other?.subject)
    }

    // 권총과 소총을 견주면 이코 라운드와 풀바이 라운드를 견주는 셈이라 같은 계열끼리만 본다
    @Test
    fun `같은 계열의 무기끼리 헤드샷을 견준다`() {
        val matches = weaponMatches(Vandal, head = 1) + weaponMatches(Phantom, head = 3)
        val rifles = mapOf(Vandal to WeaponCategory.RIFLE, Phantom to WeaponCategory.RIFLE)

        val insight = assertNotNull(matches.insight(role = null, categories = rifles))

        assertEquals(InsightMetric.HEADSHOT_RATE, insight.metric)
        assertEquals(InsightSubject.WithWeapon(Phantom), insight.lead.subject)
        assertEquals(InsightSubject.WithWeapon(Vandal), insight.other.subject)
        assertEquals(30, insight.other.rounds)
        assertNull(matches.insight(role = null))
        assertNull(matches.insight(role = null, categories = mapOf(Vandal to WeaponCategory.RIFLE, Phantom to WeaponCategory.SMG)))
    }

    // 연달아 뛸수록 어떤지 본다. 첫 두 판을 주어로 두면 무엇을 짚는지 흐려져서 늘 세 번째 판부터가 주어다.
    @Test
    fun `연달아 뛴 세 번째 판부터를 첫 두 판과 견준다`() {
        val insight = assertNotNull(sessions(early = 30 to 10, late = 20 to 20).insight(role = null))

        assertEquals(InsightMetric.SURVIVAL_RATE, insight.metric)
        assertEquals(InsightSubject.LateInSession, insight.lead.subject)
        assertEquals(12, insight.lead.matches)
        assertEquals(InsightSubject.EarlyInSession, insight.other.subject)
        assertEquals(false, insight.leadIsHigher)
    }

    @Test
    fun `세 번째 판부터가 더 높아도 세 번째 판부터를 주어로 둔다`() {
        val insight = assertNotNull(sessions(early = 20 to 20, late = 30 to 10).insight(role = null))

        assertEquals(InsightSubject.LateInSession, insight.lead.subject)
        assertEquals(true, insight.leadIsHigher)
    }

    // 앞 판이 끝나고 한 시간 넘게 쉬면 새로 센다. 판 길이도 쉰 시간에서 뺀다.
    @Test
    fun `한 시간 넘게 쉬었으면 다시 첫 판부터 센다`() {
        assertNull(sessions(early = 30 to 10, late = 20 to 20, gapMinutes = 61).insight(role = null))
        assertNotNull(sessions(early = 30 to 10, late = 20 to 20, gapMinutes = 60).insight(role = null))
        // 90분 간격이지만 한 판이 40분이라 쉰 건 50분이다
        val long = sessions(early = 30 to 10, late = 20 to 20, gapMinutes = 90).map { it.copy(lengthMillis = 40 * 60_000L) }
        assertEquals(InsightSubject.LateInSession, long.insight(role = null)?.lead?.subject)
    }

    @Test
    fun `연달아 뛴 판도 이번 주 값을 붙인다`() {
        val insight = assertNotNull(sessions(early = 30 to 10, late = 20 to 20).insight(role = null))

        assertEquals(0.5 to 0.75, insight.during(sessions(early = 30 to 10, late = 20 to 20, days = 1), emptyMap()).recent.values())
    }

    @Test
    fun `역할을 모르면 가장 벌어진 지표를 고르고 우선 지표라고 하지 않는다`() {
        val insight = assertNotNull(sides(attack = 30 to 10, defense = 20 to 20).insight(role = null))

        assertEquals(false, insight.isRolePriority)
    }
}

/**
 * 경기 [games]개에 똑같은 공수 라운드를 담습니다.
 * 판마다 같아서 흔들림은 라운드가 서로 따로 논다고 쳤을 때의 값만 남습니다.
 * [attack]과 [defense]는 (살아남은 라운드, 죽은 라운드)입니다.
 * 공격에서 죽는 라운드 중 앞의 [attackOpenedByMe]라운드는 내가 첫 킬을 낸 뒤 죽고, 나머지는 첫 데스입니다.
 * 피해량은 라운드마다 평균 위아래로 20씩 오갑니다.
 */
private fun sides(
    attack: Pair<Int, Int>,
    defense: Pair<Int, Int>,
    games: Int = 4,
    attackOpenedByMe: Int = 0,
    defenseOpenedByMe: Int = 0,
    attackDamage: Int = 0,
    defenseDamage: Int = 0,
    queue: Queue = Queue.COMPETITIVE,
    startedAt: Instant = Instant.fromEpochMilliseconds(0),
    act: ActId = ActId("act"),
): List<Match> = List(games) {
    val rounds = side(Side.ATTACK, attack.first, attack.second, attackOpenedByMe, attackDamage) +
        side(Side.DEFENSE, defense.first, defense.second, defenseOpenedByMe, defenseDamage)
    match(*rounds.toTypedArray(), queue = queue, startedAt = startedAt, act = act)
}

private fun side(side: Side?, survived: Int, died: Int, openedByMe: Int, damage: Int): List<Round> {
    fun damage(index: Int) = if (damage == 0) 0 else damage + if (index % 2 == 0) 20 else -20
    return List(survived) { round(damage = damage(it), side = side) } +
        List(died) { index ->
            if (index < openedByMe) {
                round(kill(5.0, Me, Enemy), kill(10.0, OtherEnemy, Me), damage = damage(survived + index), side = side)
            } else {
                round(kill(10.0, Enemy, Me), damage = damage(survived + index), side = side)
            }
        }
}

/**
 * 공격에서는 첫 킬 20번에 첫 데스 10번, 수비에서는 첫 킬 5번에 첫 데스 25번인 경기들입니다.
 * 첫 교전 승률이 67%와 17%로 벌어지고, 관여율도 75%와 38%로 벌어집니다.
 * 생존율은 두 진영 모두 25%입니다.
 */
private fun aimGap(games: Int = 4) = sides(attack = 10 to 30, defense = 10 to 30, games = games, attackOpenedByMe = 20, defenseOpenedByMe = 5)

/**
 * 공격 40라운드 중 12라운드, 수비 40라운드 중 2라운드에서 적을 둘 잡는 경기 넷입니다.
 * 멀티킬 라운드 비율이 30%와 5%로 벌어집니다.
 * 한 번도 죽지 않아 생존율과 관여율은 두 진영 모두 100%이고, 첫 교전도 모두 이겨 두 진영이 같습니다.
 */
private fun multiKillGap(): List<Match> {
    fun side(side: Side, multi: Int) = List(40) { index ->
        if (index < multi) round(kill(5.0, Me, Enemy), kill(8.0, Me, OtherEnemy), side = side) else round(side = side)
    }
    return List(4) { match(*(side(Side.ATTACK, multi = 12) + side(Side.DEFENSE, multi = 2)).toTypedArray()) }
}

/** 공수 포스바이 라운드를 (이긴 라운드, 진 라운드)로 담은 경기 넷입니다. 피스톨 라운드가 아니게 번호를 5로 둡니다. */
private fun buys(attack: Pair<Int, Int>, defense: Pair<Int, Int>): List<Match> {
    fun side(side: Side, won: Int, lost: Int) = List(won + lost) { index -> round(side = side, number = 5, teamLoadout = 3000, won = index < won) }
    return List(4) { match(*(side(Side.ATTACK, attack.first, attack.second) + side(Side.DEFENSE, defense.first, defense.second)).toTypedArray()) }
}

/**
 * 하루에 네 판씩, 판 시작이 [gapMinutes]분 간격인 [days]일입니다.
 * 앞 두 판과 뒤 두 판의 (살아남은 라운드, 죽은 라운드)를 받고, 진영은 모릅니다.
 */
private fun sessions(early: Pair<Int, Int>, late: Pair<Int, Int>, days: Int = 6, gapMinutes: Int = 40): List<Match> =
    (0 until days).flatMap { day ->
        List(4) { game ->
            val (survived, died) = if (game < 2) early else late
            match(*outcomes(survived, died, side = null), startedAt = Instant.fromEpochMilliseconds(0) + day.days + (game * gapMinutes).minutes)
        }
    }

private val Jett = AgentId("jett")
private val Raze = AgentId("raze")
private val Omen = AgentId("omen")
private val Ascent = MapId("ascent")
private val Bind = MapId("bind")
private val Haven = MapId("haven")
private val Vandal = WeaponId("vandal")
private val Phantom = WeaponId("phantom")

// 살아남은 라운드와 트레이드 없이 죽은 라운드만 있어 생존율과 관여율이 같다. 진영은 모른다.
private fun agentMatches(agent: AgentId, role: Role, survived: Int, died: Int, games: Int = 3): List<Match> =
    List(games) { match(*outcomes(survived, died, side = null), role = role, agent = agent) }

private fun mapMatches(map: MapId, survived: Int, died: Int): List<Match> =
    List(3) { match(*outcomes(survived, died, side = null), map = map) }

// 맵 하나에서 공격과 수비를 (라운드, 살아남은 라운드)로 나눠 담은 경기 [games]개다. 수비가 null이면 진영을 모른다.
private fun mapSides(map: MapId, games: Int, attack: Pair<Int, Int>, defense: Pair<Int, Int>?): List<Match> = List(games) {
    val attackRounds = outcomes(attack.second, attack.first - attack.second, side = if (defense == null) null else Side.ATTACK)
    val defenseRounds = defense?.let { outcomes(it.second, it.first - it.second, side = Side.DEFENSE) }.orEmpty()
    match(*(attackRounds + defenseRounds), map = map)
}

private fun outcomes(survived: Int, died: Int, side: Side?): Array<Round> =
    (List(survived) { round(side = side) } + List(died) { round(kill(10.0, Enemy, Me), side = side) }).toTypedArray()

// 그 무기를 들고 시작해 그 무기로 한 명씩 잡은 라운드 10개짜리 경기 셋이다. 맞힌 탄 10발 중 [head]발이 머리다.
private fun weaponMatches(weapon: WeaponId, head: Int): List<Match> = List(3) {
    match(*Array(10) { round(kill(10.0, Me, Enemy, weapon = weapon), shots = Shots(head = head, body = 10 - head, leg = 0), carried = weapon) })
}

private fun InsightRecent?.values() = this?.let { it.lead.value to it.other.value }
