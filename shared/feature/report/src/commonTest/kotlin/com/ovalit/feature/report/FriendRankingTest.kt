package com.ovalit.feature.report

import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Base = MatchMetrics.Empty.copy(matches = 5, rounds = 100)

private fun adr(damagePerRound: Double): MatchMetrics = Base.copy(damage = (damagePerRound * Base.rounds).toInt())

private fun friend(name: String, damagePerRound: Double?) =
    FriendStanding(PlayerId(name), "$name#KR1", damagePerRound?.let(::adr))

class FriendRankingTest {

    @Test
    fun `값이 큰 순서로 세우고 그 기간에 경기가 없는 친구는 뺀다`() {
        val ranked = rankFriends("나", adr(150.0), listOf(friend("준호", 180.0), friend("민석", null), friend("재현", 120.0)), FixedMetric.DAMAGE)

        assertEquals(listOf("준호", "나", "재현"), ranked.map { it.name })
        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
    }

    // 피해량은 정수로 보인다. 150.2와 149.8은 둘 다 150이라 같은 등수다.
    @Test
    fun `보이는 값이 같으면 같은 등수이고 그 안에서는 내가 맨 앞이다`() {
        val ranked = rankFriends(
            "나",
            adr(149.8),
            listOf(friend("준호", 200.0), friend("민석", 150.2), friend("재현", 100.0)),
            FixedMetric.DAMAGE,
        )

        assertEquals(listOf("준호", "나", "민석", "재현"), ranked.map { it.name })
        assertEquals(listOf(1, 2, 2, 4), ranked.map { it.rank })
    }

    @Test
    fun `내가 위 다섯 줄 밖이면 다섯 줄 뒤에 내 줄을 진짜 등수로 따로 둔다`() {
        val friends = (1..150).map { friend("친구$it", 300.0 - it) }

        val preview = rankFriends("나", adr(100.0), friends, FixedMetric.DAMAGE).preview()

        assertEquals(listOf("친구1", "친구2", "친구3", "친구4", "친구5"), preview.top.map { it.name })
        assertEquals(151, preview.mine?.rank)
        assertEquals(151, preview.total)
        assertTrue(preview.skipsRows)
        assertTrue(preview.hasMore)
    }

    @Test
    fun `내가 위 다섯 줄 안이면 내 줄을 따로 두지 않는다`() {
        val friends = (1..150).map { friend("친구$it", 300.0 - it) }

        val preview = rankFriends("나", adr(297.0), friends, FixedMetric.DAMAGE).preview()

        assertNull(preview.mine)
        assertTrue(preview.top.any { it.isMe })
        assertTrue(preview.hasMore)
    }

    // 동률 무리 앞에 나를 두니 내 등수가 5면 늘 다섯 줄 안에 있다. "5 … 5"처럼 같은 등수가 띄워 놓이지 않는다.
    @Test
    fun `다섯째 줄과 동률이면 내 줄을 다섯 줄 안에 둔다`() {
        val friends = listOf(200.0, 190.0, 180.0, 170.0, 160.0, 160.0, 160.0).mapIndexed { i, value -> friend("친구$i", value) }

        val preview = rankFriends("나", adr(160.0), friends, FixedMetric.DAMAGE).preview()

        assertEquals(5, preview.top.first { it.isMe }.rank)
        assertNull(preview.mine)
    }

    // 내가 6등이면 빠진 줄이 없어서 띄우지 않고, 여섯 줄로 모두 보여서 전체 보기도 두지 않는다
    @Test
    fun `내가 바로 다음 줄이면 띄우지 않고 모두 보이면 전체 보기를 두지 않는다`() {
        val friends = (1..5).map { friend("친구$it", 300.0 - it) }

        val preview = rankFriends("나", adr(100.0), friends, FixedMetric.DAMAGE).preview()

        assertEquals(6, preview.mine?.rank)
        assertFalse(preview.skipsRows)
        assertFalse(preview.hasMore)
    }
}
