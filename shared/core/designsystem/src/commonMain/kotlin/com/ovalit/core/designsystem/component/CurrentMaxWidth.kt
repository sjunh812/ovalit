package com.ovalit.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * 지금 밀도로 다시 센 [BoxWithConstraintsScope.maxWidth]입니다.
 *
 * 설정에서 화면 크기만 바꾸고 앱으로 돌아오면 픽셀 제약은 그대로라 `BoxWithConstraints`가 안을 다시 재지 않고, `maxWidth`는
 * 옛 밀도로 잰 dp를 그대로 줍니다. 그 폭으로 글자 크기를 맞추면 이름이 너무 작아지거나 칸을 넘쳐 화살표가 다음 줄로
 * 떨어졌습니다. 칸 폭으로 글자를 맞추는 곳은 이 값을 씁니다.
 */
val BoxWithConstraintsScope.currentMaxWidth: Dp
    @Composable get() = if (constraints.hasBoundedWidth) {
        with(LocalDensity.current) { constraints.maxWidth.toDp() }
    } else {
        Dp.Infinity
    }

/** [currentMaxWidth]의 높이입니다. 화면을 채우는 최소 높이를 잡는 곳이 씁니다. */
val BoxWithConstraintsScope.currentMaxHeight: Dp
    @Composable get() = if (constraints.hasBoundedHeight) {
        with(LocalDensity.current) { constraints.maxHeight.toDp() }
    } else {
        Dp.Infinity
    }
