package com.ovalit.core.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

// 경로는 화면 목업의 SVG를 그대로 옮겼다. 20×20 격자에 선으로 그린다. 채운 아이콘은 하단 탭에서 고른 탭에만 쓴다.
object OvalitIcons {
    val Home: ImageVector by lazy { strokeIcon(HOME) }

    // 하단 탭에서 고른 탭은 채운 아이콘으로 바꾼다. 색만 바꾸면 선이 가늘어서 어느 탭인지 한눈에 안 들어온다.
    // 채운 아이콘도 같은 선을 한 번 더 그려 모서리를 선 아이콘처럼 둥글게 맞춘다.
    val HomeFilled: ImageVector by lazy { icon(fills = listOf(HOME), strokes = listOf(HOME)) }

    val Settings: ImageVector by lazy { strokeIcon(SETTINGS_RAILS, SETTINGS_KNOB_TOP, SETTINGS_KNOB_BOTTOM) }

    val SettingsFilled: ImageVector by lazy {
        icon(
            fills = listOf(SETTINGS_KNOB_TOP, SETTINGS_KNOB_BOTTOM),
            strokes = listOf(SETTINGS_RAILS, SETTINGS_KNOB_TOP, SETTINGS_KNOB_BOTTOM),
        )
    }

    val ChevronRight: ImageVector by lazy { strokeIcon("M8 4l6 6-6 6", strokeWidth = 1.8f) }

    val Friends: ImageVector by lazy { strokeIcon(FRIEND_HEAD, FRIEND_BODY, FRIEND_BACK_HEAD, FRIEND_BACK_BODY) }

    // 앞사람만 채우고 뒷사람은 선으로 둔다. 둘 다 채우면 겹친 자리가 한 덩어리로 뭉친다.
    val FriendsFilled: ImageVector by lazy {
        icon(
            fills = listOf(FRIEND_HEAD, "${FRIEND_BODY}z"),
            strokes = listOf(FRIEND_HEAD, "${FRIEND_BODY}z", FRIEND_BACK_HEAD, FRIEND_BACK_BODY),
        )
    }

    // 목업은 채운 점 세 개다. 길이가 0인 선을 둥근 끝으로 그리면 같은 점이 된다.
    val More: ImageVector by lazy { strokeIcon("M10 4.5h0", "M10 10h0", "M10 15.5h0", strokeWidth = 3f) }

    val ChevronDown: ImageVector by lazy { strokeIcon("M4 8l6 6 6-6", strokeWidth = 2f) }

    val Back: ImageVector by lazy { strokeIcon("M12 4l-6 6 6 6", strokeWidth = 1.8f) }

    val Check: ImageVector by lazy { strokeIcon("M4 10.5l4 4 8-9", strokeWidth = 2f) }

    // 목업의 세 줄은 메뉴 버튼처럼 읽혀서 경기 기록 한 장을 뜻하는 카드로 바꿨다
    val Matches: ImageVector by lazy { strokeIcon(MATCH_CARD, MATCH_LINES) }

    // 카드를 채우고 안쪽 두 줄은 구멍으로 뚫는다. 색 하나로 칠하는 아이콘이라 줄을 다른 색으로 그릴 수 없다.
    val MatchesFilled: ImageVector by lazy {
        icon(fills = listOf(MATCH_CARD + MATCH_LINE_HOLES), strokes = listOf(MATCH_CARD), evenOdd = true)
    }

    val Filter: ImageVector by lazy { strokeIcon("M3 5.5h14M6 10h8M8.5 14.5h3", strokeWidth = 1.7f) }

    val Chart: ImageVector by lazy { strokeIcon("M3 15.5V9M7.3 15.5V5M11.7 15.5v-4M16 15.5V7") }

    val Restore: ImageVector by lazy { strokeIcon("M7.5 3.2A7.2 7.2 0 1 1 3.4 12", "M3 4.2v3.6h3.6") }

    val Lock: ImageVector by lazy {
        strokeIcon(
            "M5.6 8.6h8.8a1.6 1.6 0 0 1 1.6 1.6v4.8a1.6 1.6 0 0 1-1.6 1.6H5.6A1.6 1.6 0 0 1 4 15v-4.8a1.6 1.6 0 0 1 1.6-1.6z",
            "M7 8.6V6.2a3 3 0 0 1 6 0v2.4",
        )
    }

    val ArrowRight: ImageVector by lazy { strokeIcon("M4 10h11M11 5.5l4.5 4.5L11 14.5", strokeWidth = 2f) }
}

/** @param contentDescription 옆에 같은 뜻의 글자가 있으면 `null`로 둡니다. 낭독기가 두 번 읽습니다. */
@Composable
fun OvalitIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = OvalitTheme.colors.t2,
    size: Dp = 20.dp,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

private const val HOME = "M3 9.2 10 3.2l7 6V17H12.5v-4.6h-5V17H3z"
private const val SETTINGS_RAILS = "M3 6.6h12M5 13.4h12"
private const val SETTINGS_KNOB_TOP = "M6 6.6a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
private const val SETTINGS_KNOB_BOTTOM = "M10 13.4a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
private const val FRIEND_HEAD = "M10.2 7a2.6 2.6 0 1 1-5.2 0a2.6 2.6 0 1 1 5.2 0"
private const val FRIEND_BODY = "M2.8 16.2c0-2.6 2.1-4.3 4.8-4.3s4.8 1.7 4.8 4.3"
private const val FRIEND_BACK_HEAD = "M13.4 5.2a2.6 2.6 0 0 1 0 5"
private const val FRIEND_BACK_BODY = "M14.6 12.3c1.7.5 2.8 1.9 2.8 3.9"
private const val MATCH_CARD = "M4.6 4h10.8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4.6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
private const val MATCH_LINES = "M6.2 8.3h7.6M6.2 11.7h4.6"
private const val MATCH_LINE_HOLES = "M5.4 7.5h9.2v1.6H5.4zM5.4 10.9h5.8v1.6H5.4z"

private fun strokeIcon(vararg paths: String, strokeWidth: Float = 1.6f): ImageVector =
    icon(strokes = paths.toList(), strokeWidth = strokeWidth)

/** @param evenOdd 채운 경로 안에 겹친 경로를 구멍으로 뚫습니다. */
private fun icon(
    fills: List<String> = emptyList(),
    strokes: List<String> = emptyList(),
    strokeWidth: Float = 1.6f,
    evenOdd: Boolean = false,
): ImageVector =
    ImageVector.Builder(defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            fills.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                    fill = SolidColor(Color.Black),
                )
            }
            strokes.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        .build()
