package com.ovalit.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.QueueFilter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SkeletonTest {

    // 칩은 눌리는 영역이 48dp라 보이는 칩보다 높다.
    // 스켈레톤이 보이는 칩 높이만 잡으면 칩이 나타날 때 그 아래 카드가 한 번에 밀린다.
    @Test
    fun `큐 칩 스켈레톤은 실제 칩 줄과 높이가 같다`() {
        listOf(1f, 1.3f).forEach { fontScale ->
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = fontScale)) {
                        OvalitTheme {
                            Column(Modifier.width(384.dp)) {
                                Box(Modifier.testTag("chips")) { QueueChips(selected = QueueFilter.COMPETITIVE_AND_UNRATED, onSelect = {}) }
                                Box(Modifier.testTag("skeleton")) { OvalitSkeleton(description = "") { QueueChipsSkeleton() } }
                            }
                        }
                    }
                }

                val chips = onNodeWithTag("chips").getUnclippedBoundsInRoot().let { it.bottom - it.top }
                val skeleton = onNodeWithTag("skeleton").getUnclippedBoundsInRoot().let { it.bottom - it.top }
                assertTrue(abs((chips - skeleton).value) < 1f, "×$fontScale: 칩 $chips, 스켈레톤 $skeleton")
            }
        }
    }
}
