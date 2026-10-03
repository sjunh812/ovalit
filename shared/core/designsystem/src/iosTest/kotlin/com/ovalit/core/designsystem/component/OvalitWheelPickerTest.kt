package com.ovalit.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OvalitWheelPickerTest {

    private val items = List(10) { "${it}시" }

    @Test
    fun `처음에는 고른 줄을 가운데에 둔다`() = runComposeUiTest {
        setContent { OvalitTheme { OvalitWheelPicker(items, selected = 4, onSelect = {}) } }

        onNodeWithText("4시").assertIsSelected()
    }

    @Test
    fun `줄을 누르면 그 줄로 돌아가 멈춘 뒤에 고른다`() = runComposeUiTest {
        val picked = mutableListOf<Int>()
        setContent { OvalitTheme { OvalitWheelPicker(items, selected = 0, onSelect = { picked += it }) } }

        onNodeWithText("2시").performClick()
        waitForIdle()

        onNodeWithText("2시").assertIsSelected()
        // 돌아가는 동안 지나간 1시는 고르지 않는다
        assertEquals(listOf(0, 2), picked)
    }
}
