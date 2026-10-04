package com.ovalit.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * 지금 밀도로 다시 잰 [BoxWithConstraintsScope.maxWidth]입니다. 칸 폭에 맞춰 글자 크기를 정하는 곳은 이 값을 씁니다.
 *
 * 설정에서 화면 크기만 바꾸고 돌아오면 픽셀 제약이 그대로라 `maxWidth`가 옛 밀도로 잰 dp를 줍니다.
 */
val BoxWithConstraintsScope.currentMaxWidth: Dp
    @Composable get() = if (constraints.hasBoundedWidth) {
        with(LocalDensity.current) { constraints.maxWidth.toDp() }
    } else {
        Dp.Infinity
    }

/** [currentMaxWidth]처럼 지금 밀도로 다시 잰 높이입니다. */
val BoxWithConstraintsScope.currentMaxHeight: Dp
    @Composable get() = if (constraints.hasBoundedHeight) {
        with(LocalDensity.current) { constraints.maxHeight.toDp() }
    } else {
        Dp.Infinity
    }
