package com.ovalit.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CurrentMaxWidthTest {

    // 설정에서 화면 크기만 바꾸면 픽셀 제약은 그대로이고 밀도만 바뀐다. maxWidth는 처음 잰 밀도로 센 dp를 그대로 준다.
    @Test
    fun `칸 폭과 높이는 지금 밀도로 다시 센다`() = runComposeUiTest {
        var width = 0.dp
        var height = 0.dp
        setContent {
            Box(Modifier.size(300.dp, 200.dp)) {
                BoxWithConstraints {
                    val doubled = Density(LocalDensity.current.density * 2, LocalDensity.current.fontScale)
                    CompositionLocalProvider(LocalDensity provides doubled) {
                        width = currentMaxWidth
                        height = currentMaxHeight
                    }
                }
            }
        }

        waitForIdle()
        assertEquals(150.dp, width)
        assertEquals(100.dp, height)
    }

    @Test
    fun `제약이 없는 쪽은 무한이다`() = runComposeUiTest {
        var width = 0.dp
        setContent {
            LazyRow {
                item { BoxWithConstraints { width = currentMaxWidth } }
            }
        }

        waitForIdle()
        assertEquals(Dp.Infinity, width)
    }
}
