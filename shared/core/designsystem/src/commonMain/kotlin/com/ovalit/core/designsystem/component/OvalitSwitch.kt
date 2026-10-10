package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

private val TrackWidth = 42.dp
private val TrackHeight = 25.dp
private val KnobSize = 19.dp
private val KnobInset = 3.dp

/**
 * 모양만 그립니다.
 * 누름은 줄 전체에 `toggleable`을 걸어 받습니다.
 * 스위치에만 걸면 누를 곳이 작고 화면 읽기 프로그램이 스위치와 설명을 따로 읽습니다.
 *
 * @param enabled 휴대폰 설정에서 알림을 막아 바꿔도 소용없을 때처럼 못 바꾸면 `false`입니다.
 *   켜져 있어도 액센트를 칠하지 않고 손잡이 자리로만 보여 줍니다.
 */
@Composable
fun OvalitSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OvalitTheme.colors
    val knobOffset by animateDpAsState(if (checked) TrackWidth - KnobSize - KnobInset * 2 else 0.dp)

    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .background(if (checked && enabled) colors.accent else colors.fill, CircleShape)
            .padding(KnobInset),
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(KnobSize)
                .background(
                    when {
                        !enabled -> colors.t5
                        checked -> colors.onAccent
                        else -> colors.t4
                    },
                    CircleShape,
                ),
        )
    }
}
