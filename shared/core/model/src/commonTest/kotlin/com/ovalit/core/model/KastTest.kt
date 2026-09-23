package com.ovalit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 관여율(KAST)은 라운드마다 킬(K)·어시스트(A)·생존(S)·트레이드(T) 중 하나라도 했는지 봅니다.
 * 하나씩 떼어서 확인하고, 아무것도 못 한 라운드가 빠지는지 봅니다.
 */
class KastTest {

    @Test
    fun `킬을 하면 죽었어도 관여한 라운드다`() {
        val metrics = match(round(kill(10.0, Me, Enemy), kill(20.0, OtherEnemy, Me))).metrics()

        assertEquals(1, metrics.kastRounds)
    }

    @Test
    fun `어시스트만 해도 관여한 라운드다`() {
        val metrics = match(
            round(kill(10.0, Ally, Enemy, assistedBy = setOf(Me)), kill(20.0, OtherEnemy, Me)),
        ).metrics()

        assertEquals(1, metrics.kastRounds)
    }

    @Test
    fun `아무것도 안 하고 살아남기만 해도 관여한 라운드다`() {
        val metrics = match(quietRound()).metrics()

        assertEquals(1, metrics.kastRounds)
    }

    @Test
    fun `죽었어도 우리 팀이 곧바로 내 킬러를 잡으면 관여한 라운드다`() {
        val metrics = match(round(kill(10.0, Enemy, Me), kill(12.0, Ally, Enemy))).metrics()

        assertEquals(1, metrics.kastRounds)
    }

    @Test
    fun `트레이드는 정확히 5초까지 인정한다`() {
        val metrics = match(round(kill(10.0, Enemy, Me), kill(15.0, Ally, Enemy))).metrics()

        assertEquals(1, metrics.kastRounds)
    }

    @Test
    fun `5초가 지나서 잡으면 트레이드가 아니다`() {
        val metrics = match(round(kill(10.0, Enemy, Me), kill(15.001, Ally, Enemy))).metrics()

        assertEquals(0, metrics.kastRounds)
    }

    // 우리 팀이 누군가를 잡았다고 다 트레이드가 아니다. 나를 죽인 사람이어야 한다.
    @Test
    fun `내 킬러가 아닌 다른 적을 잡은 건 트레이드가 아니다`() {
        val metrics = match(round(kill(10.0, Enemy, Me), kill(11.0, Ally, OtherEnemy))).metrics()

        assertEquals(0, metrics.kastRounds)
    }

    // 레이즈 궁으로 나를 잡고 자기도 같이 터지는 경우. 내 킬러가 죽긴 했지만 우리가 잡은 게 아니다.
    @Test
    fun `내 킬러가 자기 스킬로 죽은 건 트레이드가 아니다`() {
        val metrics = match(round(kill(10.0, Enemy, Me), kill(10.5, Enemy, Enemy))).metrics()

        assertEquals(0, metrics.kastRounds)
    }

    @Test
    fun `킬도 어시스트도 못 하고 죽은 채로 끝나면 관여하지 못한 라운드다`() {
        val metrics = match(round(kill(10.0, Enemy, Me))).metrics()

        assertEquals(0, metrics.kastRounds)
    }

    @Test
    fun `관여율은 관여한 라운드를 전체 라운드로 나눈다`() {
        val metrics = match(
            round(kill(10.0, Me, Enemy)),
            quietRound(),
            round(kill(10.0, Enemy, Me)),
            round(kill(10.0, Enemy, Me), kill(11.0, Ally, Enemy)),
            round(kill(10.0, Enemy, Me)),
        ).metrics()

        // 킬, 생존, 트레이드 세 라운드만 관여했다
        assertEquals(3, metrics.kastRounds)
        assertRate(0.6, metrics.kast)
    }
}
