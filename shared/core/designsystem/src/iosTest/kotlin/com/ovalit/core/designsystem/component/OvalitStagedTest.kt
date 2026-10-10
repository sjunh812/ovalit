package com.ovalit.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OvalitStagedTest {

    // 탭을 오가거나 뒤로 돌아온 화면은 내용이 이미 있다.
    // 자리 틀을 거치면 깜빡인다.
    @Test
    fun `처음부터 준비돼 있으면 자리 틀 없이 한 번에 그린다`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Staged(ready = true) }

        onNodeWithText("틀").assertDoesNotExist()
        onNodeWithText("가").assertExists()
        onNodeWithText("나").assertExists()
    }

    @Test
    fun `나중에 준비되면 묶음을 한 프레임에 하나씩 그린다`() = runComposeUiTest {
        var ready by mutableStateOf(false)
        val composed = mutableListOf<String>()
        mainClock.autoAdvance = false
        setContent { Staged(ready = ready, onComposed = { composed += it }) }

        ready = true
        mainClock.advanceTimeByFrame()
        assertEquals(listOf("가"), composed)
        mainClock.advanceTimeByFrame()
        assertEquals(listOf("가", "나"), composed)
    }

    @Test
    fun `다 그리기 전에는 내용을 낭독기에 감추고 자리 틀을 둔다`() = runComposeUiTest {
        var ready by mutableStateOf(false)
        val composed = mutableListOf<String>()
        mainClock.autoAdvance = false
        setContent { Staged(ready = ready, onComposed = { composed += it }) }

        ready = true
        mainClock.advanceTimeByFrame()
        assertEquals(listOf("가"), composed)
        onNodeWithText("가").assertDoesNotExist()
        onNodeWithText("틀").assertExists()
    }

    @Test
    fun `다 나타나면 자리 틀을 치운다`() = runComposeUiTest {
        var ready by mutableStateOf(false)
        setContent { Staged(ready = ready) }

        ready = true
        waitForIdle()
        onNodeWithText("틀").assertDoesNotExist()
        onNodeWithText("가").assertExists()
        onNodeWithText("나").assertExists()
    }

    @Test
    fun `준비가 풀리면 다시 자리 틀을 보인다`() = runComposeUiTest {
        var ready by mutableStateOf(true)
        setContent { Staged(ready = ready) }

        ready = false
        waitForIdle()
        onNodeWithText("틀").assertExists()
        onNodeWithText("가").assertDoesNotExist()
    }
}

@Composable
private fun Staged(ready: Boolean, onComposed: (String) -> Unit = {}) {
    OvalitTheme {
        OvalitStaged(ready = ready, placeholder = { BasicText("틀") }) {
            OvalitStage {
                SideEffect { onComposed("가") }
                BasicText("가")
            }
            OvalitStage {
                SideEffect { onComposed("나") }
                BasicText("나")
            }
        }
    }
}
