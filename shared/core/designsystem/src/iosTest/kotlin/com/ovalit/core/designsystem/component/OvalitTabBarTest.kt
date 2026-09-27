package com.ovalit.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OvalitTabBarTest {

    // 높이를 고정하면 글자를 키웠을 때 탭 이름 아래가 잘린다
    @Test
    fun `글자를 키우면 탭바가 같이 커져 탭 이름이 잘리지 않는다`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2f)) {
                OvalitTheme {
                    OvalitTabBar(
                        tabs = listOf(OvalitTab("홈", OvalitIcons.Home, OvalitIcons.HomeFilled)),
                        selectedIndex = 0,
                        onSelect = {},
                    )
                }
            }
        }

        val tab = onNodeWithText("홈").getUnclippedBoundsInRoot()
        val labelNode = onNodeWithText("홈", useUnmergedTree = true)
        val label = labelNode.getUnclippedBoundsInRoot()
        val layouts = mutableListOf<TextLayoutResult>()
        labelNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertFalse(layouts.single().didOverflowHeight, "탭 이름이 받은 높이보다 커서 아래가 잘렸다")
        assertTrue(label.bottom <= tab.bottom, "탭 이름 아래 ${label.bottom}이 탭 아래 ${tab.bottom}보다 밑이다")
    }
}
