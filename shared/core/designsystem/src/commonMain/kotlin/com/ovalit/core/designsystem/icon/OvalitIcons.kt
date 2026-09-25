package com.ovalit.core.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

// 경로는 화면 목업의 SVG를 그대로 옮겼다. 20×20 격자에 선으로만 그린다.
object OvalitIcons {
    val Home: ImageVector by lazy { strokeIcon("M3 9.2 10 3.2l7 6V17H12.5v-4.6h-5V17H3z") }

    val Settings: ImageVector by lazy {
        strokeIcon(
            "M3 6.6h12M5 13.4h12",
            "M6 6.6a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M10 13.4a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        )
    }

    val ChevronRight: ImageVector by lazy { strokeIcon("M8 4l6 6-6 6", strokeWidth = 1.8f) }

    val Friends: ImageVector by lazy {
        strokeIcon(
            "M10.2 7a2.6 2.6 0 1 1-5.2 0a2.6 2.6 0 1 1 5.2 0",
            "M2.8 16.2c0-2.6 2.1-4.3 4.8-4.3s4.8 1.7 4.8 4.3",
            "M13.4 5.2a2.6 2.6 0 0 1 0 5",
            "M14.6 12.3c1.7.5 2.8 1.9 2.8 3.9",
        )
    }

    // 목업은 채운 점 세 개다. 길이가 0인 선을 둥근 끝으로 그리면 같은 점이 된다.
    val More: ImageVector by lazy { strokeIcon("M10 4.5h0", "M10 10h0", "M10 15.5h0", strokeWidth = 3f) }

    val ChevronDown: ImageVector by lazy { strokeIcon("M4 8l6 6 6-6", strokeWidth = 2f) }

    val Back: ImageVector by lazy { strokeIcon("M12 4l-6 6 6 6", strokeWidth = 1.8f) }

    val Check: ImageVector by lazy { strokeIcon("M4 10.5l4 4 8-9", strokeWidth = 2f) }

    val Matches: ImageVector by lazy { strokeIcon("M4 5.5h12M4 10h12M4 14.5h12") }

    val Filter: ImageVector by lazy { strokeIcon("M3 5.5h14M6 10h8M8.5 14.5h3", strokeWidth = 1.7f) }
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

private fun strokeIcon(vararg paths: String, strokeWidth: Float = 1.6f): ImageVector =
    ImageVector.Builder(defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            paths.forEach { path ->
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
