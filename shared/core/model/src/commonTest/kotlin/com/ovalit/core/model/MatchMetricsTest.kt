package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchMetricsTest {

    @Test
    fun `전투점수는 경기 전체 점수를 내가 뛴 라운드 수로 나눈다`() {
        val metrics = match(quietRound(), quietRound(), quietRound(), combatScore = 750).metrics()

        assertRate(250.0, metrics.acs)
    }

    @Test
    fun `피해량은 라운드마다 준 피해를 모아 라운드 수로 나눈다`() {
        val metrics = match(
            round(damage = 150),
            round(damage = 0),
            round(damage = 210),
        ).metrics()

        assertRate(120.0, metrics.adr)
    }

    @Test
    fun `헤드샷 비율은 맞힌 탄 전체 중 머리에 맞은 비율이다`() {
        val metrics = match(
            round(shots = Shots(head = 3, body = 6, leg = 1)),
            round(shots = Shots(head = 1, body = 4, leg = 0)),
        ).metrics()

        // 머리 4 / 전체 15
        assertRate(4.0 / 15, metrics.headshotRate)
    }

    @Test
    fun `K_D는 킬을 데스로 나눈다`() {
        val metrics = match(
            round(kill(10.0, Me, Enemy), kill(20.0, Me, OtherEnemy)),
            round(kill(15.0, Enemy, Me)),
        ).metrics()

        assertEquals(2, metrics.kills)
        assertEquals(1, metrics.deaths)
        assertRate(2.0, metrics.kd)
    }

    @Test
    fun `데스가 없으면 K_D는 비워 둔다`() {
        val metrics = match(round(kill(10.0, Me, Enemy))).metrics()

        assertNull(metrics.kd)
    }

    @Test
    fun `라운드가 없으면 비율은 전부 비워 둔다`() {
        val metrics = match(combatScore = 0).metrics()

        assertNull(metrics.acs)
        assertNull(metrics.adr)
        assertNull(metrics.kast)
        assertNull(metrics.survivalRate)
    }

    @Test
    fun `맞힌 탄이 없으면 헤드샷 비율은 0이 아니라 비워 둔다`() {
        val metrics = match(quietRound()).metrics()

        assertNull(metrics.headshotRate)
    }

    // 응답에는 스킬로 나를 죽인 것도 킬로 들어온다. 그대로 세면 K/D가 부풀어 오른다.
    @Test
    fun `나를 죽인 킬은 킬로 세지 않고 데스로만 센다`() {
        val metrics = match(round(kill(10.0, Me, Me))).metrics()

        assertEquals(0, metrics.kills)
        assertEquals(1, metrics.deaths)
    }

    @Test
    fun `우리 팀을 죽인 킬은 킬로 세지 않는다`() {
        val metrics = match(round(kill(10.0, Me, Ally))).metrics()

        assertEquals(0, metrics.kills)
    }

    @Test
    fun `생존율은 죽지 않은 라운드의 비율이다`() {
        val metrics = match(
            quietRound(),
            round(kill(10.0, Enemy, Me)),
            quietRound(),
            quietRound(),
        ).metrics()

        assertRate(0.75, metrics.survivalRate)
    }

    // 경기마다 ACS를 먼저 구해 평균을 내면 라운드가 적은 경기가 과하게 반영된다.
    @Test
    fun `여러 경기를 합치면 평균의 평균이 아니라 총량을 나눈 값이 나온다`() {
        val stomp = match(*Array(15) { quietRound() }, combatScore = 4_500)
        val close = match(*Array(24) { quietRound() }, combatScore = 4_800)

        val combined = listOf(stomp.metrics(), close.metrics()).sum()

        assertRate(300.0, stomp.metrics().acs)
        assertRate(200.0, close.metrics().acs)
        // 평균의 평균이면 250이 나온다. 총량 9,300을 39라운드로 나눠야 맞다.
        assertRate(9_300.0 / 39, combined.acs)
        assertEquals(2, combined.matches)
        assertEquals(39, combined.rounds)
    }
}
