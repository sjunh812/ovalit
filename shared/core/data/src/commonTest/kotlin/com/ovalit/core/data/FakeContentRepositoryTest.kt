package com.ovalit.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
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
}
