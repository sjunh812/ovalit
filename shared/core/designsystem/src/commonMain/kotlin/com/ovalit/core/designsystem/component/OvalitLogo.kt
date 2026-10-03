package com.ovalit.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.ovalit.core.designsystem.theme.OvalitTheme

// 좌표는 CLAUDE.md의 로고 항목과 같은 값이다.
// 같은 마크를 네 군데서 따로 그린다. 여기, 런처 아이콘(ic_launcher_foreground.xml), 스플래시(ic_splash_mark.xml),
// 알림 작은 아이콘(ic_notification.xml). 하나를 고치면 나머지도 같이 고친다.
private const val VIEW_BOX_WIDTH = 160f
private const val VIEW_BOX_HEIGHT = 76f
private const val STROKE_WIDTH = 10f

/**
 * 초성 ㅇㅂㅇ입니다. ㅇ은 모서리를 깎은 팔각이고 ㅂ은 아래 두 모서리를 ㅇ처럼 깎아 ㅇ보다 4 내립니다. 획 끝은 자르고 꺾이는
 * 곳만 살짝 둥글립니다. 둥근 획과 동그라미 ㅇ은 귀엽게 읽혀서 발로란트 화면의 각진 선을 따랐습니다(사용자 결정, 2026-10-03).
 */
@Composable
fun OvalitLogo(
    modifier: Modifier = Modifier,
    color: Color = OvalitTheme.colors.t1,
) {
    val paths = remember { logoPaths() }

    Canvas(modifier = modifier.aspectRatio(VIEW_BOX_WIDTH / VIEW_BOX_HEIGHT)) {
        val ratio = size.width / VIEW_BOX_WIDTH
        val stroke = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        scale(scaleX = ratio, scaleY = ratio, pivot = Offset.Zero) {
            paths.forEach { drawPath(path = it, color = color, style = stroke) }
        }
    }
}

private fun logoPaths(): List<Path> = listOf(
    octagon(left = 10f),
    // ㅂ: 위가 열린 세로 두 획과 밑 획. 아래 두 모서리를 ㅇ처럼 8씩 깎는다. 한쪽만 깎으면 잘린 것처럼 보였다.
    Path().apply {
        moveTo(63f, 17f)
        lineTo(63f, 50f)
        lineTo(71f, 58f)
        lineTo(89f, 58f)
        lineTo(97f, 50f)
        lineTo(97f, 17f)
    },
    Path().apply {
        moveTo(63f, 38f)
        lineTo(97f, 38f)
    },
    octagon(left = 116f),
)

// 폭 34, 높이 36에 모서리를 8씩 깎은 ㅇ이다
private fun octagon(left: Float): Path = Path().apply {
    moveTo(left + 8f, 18f)
    lineTo(left + 26f, 18f)
    lineTo(left + 34f, 26f)
    lineTo(left + 34f, 46f)
    lineTo(left + 26f, 54f)
    lineTo(left + 8f, 54f)
    lineTo(left, 46f)
    lineTo(left, 26f)
    close()
}
