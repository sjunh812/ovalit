package com.ovalit.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class FakeContentRepositoryTest {

    @Test
    fun `앱이 켜진 채로 언어를 바꾸면 카탈로그도 그 언어로 다시 낸다`() = runTest {
        val language = MutableStateFlow("ko")
        val repository = FakeContentRepository(language)
        val haven = FakeMaps.first { it.name == "헤이븐" }.id

        val names = mutableListOf<String?>()
        val collecting = launch { repository.catalog.take(2).toList().forEach { names += it.maps[haven] } }
        runCurrent()
        language.value = "ja"
        collecting.join()

        assertEquals(listOf<String?>("헤이븐", FakeNamesJa.getValue("헤이븐")), names)
    }

    // 영어 화면에서 요원·맵·무기·티어 이름만 한국어로 남으면 안 된다
    @Test
    fun `영어 화면이면 이름과 티어를 모두 영어로 낸다`() = runTest {
        val catalog = FakeContentRepository(MutableStateFlow("en")).catalog.first()

        assertEquals(FakeNamesEn.keys.size, FakeNamesJa.keys.size)
        assertTrue(catalog.agents.values.all { name -> name.none { it in '가'..'힣' } }, "${catalog.agents.values}")
        assertTrue(catalog.maps.values.all { name -> name.none { it in '가'..'힣' } }, "${catalog.maps.values}")
        assertTrue(catalog.weapons.values.all { info -> info.name.none { it in '가'..'힣' } }, "${catalog.weapons.values}")
        assertEquals("Radiant", catalog.tiers[27])
    }
}
