package com.ovalit.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.ovalit.core.designsystem.theme.OvalitTheme

// 좌표는 CLAUDE.md의 로고 항목과 같은 값이다.
// 같은 마크를 세 군데서 따로 그린다. 여기, 런처 아이콘(ic_launcher_foreground.xml),
// 스플래시(ic_splash_mark.xml). 하나를 고치면 나머지도 같이 고쳐야 한다.
private const val VIEW_BOX_WIDTH = 160f
private const val VIEW_BOX_HEIGHT = 74f
private const val STROKE_WIDTH = 11f

private const val CIRCLE_RADIUS = 17.5f
private const val CIRCLE_CENTER_Y = 32.5f
private const val LEFT_CIRCLE_X = 27f
private const val RIGHT_CIRCLE_X = 133f

private const val BIEUP_LEFT = 61.5f
private const val BIEUP_RIGHT = 98.5f
private const val BIEUP_TOP = 23f
private const val BIEUP_BAR_Y = 37.5f
private const val BIEUP_BOTTOM = 59f
private const val BIEUP_CORNER = 8f

@Composable
fun OvalitLogo(
    modifier: Modifier = Modifier,
    color: Color = OvalitTheme.colors.t1,
) {
    val bieup = remember { buildBieupPath() }

    Canvas(modifier = modifier.aspectRatio(VIEW_BOX_WIDTH / VIEW_BOX_HEIGHT)) {
        val ratio = size.width / VIEW_BOX_WIDTH
        val stroke = Stroke(
            width = STROKE_WIDTH,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        scale(scaleX = ratio, scaleY = ratio, pivot = Offset.Zero) {
            drawCircle(
                color = color,
                radius = CIRCLE_RADIUS,
                center = Offset(LEFT_CIRCLE_X, CIRCLE_CENTER_Y),
                style = stroke,
            )
            drawPath(path = bieup, color = color, style = stroke)
            drawLine(
                color = color,
                start = Offset(BIEUP_LEFT, BIEUP_BAR_Y),
                end = Offset(BIEUP_RIGHT, BIEUP_BAR_Y),
                strokeWidth = STROKE_WIDTH,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = CIRCLE_RADIUS,
                center = Offset(RIGHT_CIRCLE_X, CIRCLE_CENTER_Y),
                style = stroke,
            )
        }
    }
}

private fun buildBieupPath(): Path {
    val cornerTop = BIEUP_BOTTOM - BIEUP_CORNER
    return Path().apply {
        moveTo(BIEUP_LEFT, BIEUP_TOP)
        lineTo(BIEUP_LEFT, cornerTop)
        arcTo(
            rect = Rect(
                left = BIEUP_LEFT,
                top = cornerTop - BIEUP_CORNER,
                right = BIEUP_LEFT + BIEUP_CORNER * 2,
                bottom = BIEUP_BOTTOM,
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false,
        )
        lineTo(BIEUP_RIGHT - BIEUP_CORNER, BIEUP_BOTTOM)
        arcTo(
            rect = Rect(
                left = BIEUP_RIGHT - BIEUP_CORNER * 2,
                top = cornerTop - BIEUP_CORNER,
                right = BIEUP_RIGHT,
                bottom = BIEUP_BOTTOM,
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false,
        )
        lineTo(BIEUP_RIGHT, BIEUP_TOP)
    }
}
