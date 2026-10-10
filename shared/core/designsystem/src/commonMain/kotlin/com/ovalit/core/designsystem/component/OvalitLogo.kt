package com.ovalit.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import com.ovalit.core.designsystem.theme.OvalitTheme

// 좌표는 docs/design.md 로고 항목의 값이다.
// 앱 아이콘 그림(1024 정사각)의 픽셀이라 말풍선 칸만 잘라 그린다.
// 같은 마크를 여기와 ic_launcher_foreground.xml, ic_splash_mark.xml, ic_notification.xml에서 따로 그린다.
// 하나를 고치면 넷을 다 고친다.
private const val BUBBLE_LEFT = 152f
private const val BUBBLE_TOP = 280f
private const val BUBBLE_WIDTH = 720f
private const val BUBBLE_HEIGHT = 543f

// 말풍선, ㅇ 고리 둘, ㅂ을 한 경로에 담고 evenOdd로 칠해 글자 자리를 비운다
private const val LOGO_PATH =
    "M272,280H752A120,120 0,0 1,872,400V598A120,120 0,0 1,752,718H432L322.5,813.8Q312,823 312,809V718H272A120,120 0,0 1,152,598V400A120,120 0,0 1,272,280ZM228,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM272,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0ZM430,422A8,8 0,0 1,438,414H466A8,8 0,0 1,474,422V463H550V422A8,8 0,0 1,558,414H586A8,8 0,0 1,594,422V573A10,10 0,0 1,584,583H440A10,10 0,0 1,430,573ZM474,499H550V548H474ZM620,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM664,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0Z"

/**
 * 말풍선 속 ㅇㅂㅇ입니다.
 * 말풍선을 [color]로 칠하고 글자 자리는 파내서 뒤 바탕이 비칩니다.
 *
 * 앱 아이콘은 빨강 바탕에 흰 말풍선이지만, 흰 말풍선은 라이트 바탕에서 안 보여서 화면에서는 말풍선을 빨강으로 칠합니다.
 */
@Composable
fun OvalitLogo(
    modifier: Modifier = Modifier,
    color: Color = OvalitTheme.colors.accent,
) {
    val path = remember {
        PathParser().parsePathString(LOGO_PATH).toPath().apply { fillType = PathFillType.EvenOdd }
    }

    Canvas(modifier = modifier.aspectRatio(BUBBLE_WIDTH / BUBBLE_HEIGHT)) {
        val ratio = size.width / BUBBLE_WIDTH
        scale(scaleX = ratio, scaleY = ratio, pivot = Offset.Zero) {
            translate(left = -BUBBLE_LEFT, top = -BUBBLE_TOP) { drawPath(path = path, color = color) }
        }
    }
}
