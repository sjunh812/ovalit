package com.ovalit.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OvalitScreenEnteringTest {

    @Test
    fun `밀려 들어오는 중에 도착한 내용은 다 들어올 때까지 기다린다`() = runComposeUiTest {
        var entering by mutableStateOf(true)
        var loaded by mutableStateOf(false)
        var shown = false
        setContent {
            CompositionLocalProvider(LocalScreenEntering provides entering) { shown = rememberContentShown(loaded) }
        }

        loaded = true
        waitForIdle()
        assertFalse(shown)

        entering = false
        waitForIdle()
        assertTrue(shown)
    }

    // 탭을 오가거나 뒤로 돌아온 화면은 내용이 이미 있다.
    // 전환 중이라고 스켈레톤으로 되돌리면 깜빡인다.
    @Test
    fun `처음부터 내용이 있으면 전환 중에도 바로 그린다`() = runComposeUiTest {
        var shown = false
        setContent {
            CompositionLocalProvider(LocalScreenEntering provides true) { shown = rememberContentShown(loaded = true) }
        }

        waitForIdle()
        assertTrue(shown)
    }

    @Test
    fun `한 번 그린 내용은 다시 전환이 시작돼도 거두지 않는다`() = runComposeUiTest {
        var entering by mutableStateOf(false)
        var loaded by mutableStateOf(false)
        var shown = false
        setContent {
            CompositionLocalProvider(LocalScreenEntering provides entering) { shown = rememberContentShown(loaded) }
        }

        loaded = true
        waitForIdle()
        entering = true
        waitForIdle()
        assertTrue(shown)
    }
}
