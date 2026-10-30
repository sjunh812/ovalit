package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * 앱을 켤 때 시스템 스플래시에 이어 한 번 재생하는 로고 애니메이션입니다.
 * 빨간 화면 가운데 말풍선에서 입력 중 점 셋이 한 번 출렁인 뒤 양옆으로 벌어지며 ㅇㅂㅇ이 됩니다.
 * 첫 장면은 시스템 스플래시(`ic_splash_mark.xml`)와 같은 자리, 같은 크기라 끊기지 않고 이어 보입니다.
 * 다 그리면 화면이 흐려지며 [onFinished]를 부릅니다.
 *
 * iOS 시작 화면과 안드로이드 12 미만의 스플래시는 움직일 수 없어서, 움직임은 앱이 뜬 뒤 여기서 그립니다(docs/design.md 로고).
 * 재생하는 동안 밑 화면은 눌리지 않습니다.
 */
@Composable
fun OvalitLaunchAnimation(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val paths = remember { LaunchPaths() }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(PLAY_MILLIS, easing = LinearEasing))
        fade.animateTo(0f, tween(FADE_MILLIS))
        onFinished()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .clearAndSetSemantics {},
    ) {
        drawRect(colors.accent)
        // 스플래시 드로어블처럼 그림 좌표 (512, 512)를 화면 가운데에 두고 1을 UNIT만큼으로 그린다
        val unit = UNIT.toPx()
        translate(left = center.x - CENTER * unit, top = center.y - CENTER * unit) {
            scale(scale = unit, pivot = Offset.Zero) {
                drawLaunchFrame(paths, progress.value * PLAY_MILLIS, colors.onAccent, colors.accent)
            }
        }
    }
}

private fun DrawScope.drawLaunchFrame(paths: LaunchPaths, millis: Float, bubble: Color, ink: Color) {
    drawPath(paths.bubble, bubble)

    // 점 셋이 차례로 한 번씩 튄다
    val spread = FastOutSlowInEasing.transform(segment(millis, SPREAD_START, SPREAD_END))
    DOTS.forEachIndexed { index, dot ->
        val hop = segment(millis, dot.hopStart, dot.hopStart + HOP_MILLIS)
        val y = DOT_Y - HOP_HEIGHT * sin(PI * hop).toFloat()
        // 벌어지면서 글자 자리로 가고 흐려진다
        val x = dot.x + (dot.spreadTo - dot.x) * spread
        drawCircle(ink, radius = DOT_RADIUS, center = Offset(x, y), alpha = 1f - spread)
    }

    // 글자는 작게 나타나 살짝 넘쳤다가 제 크기로 선다
    val grow = segment(millis, LETTERS_START, LETTERS_END)
    if (grow > 0f) {
        val size = LETTER_FROM + (1f - LETTER_FROM) * Overshoot.transform(grow)
        val alpha = (grow * 2f).coerceAtMost(1f)
        paths.letters.forEach { (path, pivot) ->
            scale(scale = size, pivot = pivot) { drawPath(path, ink, alpha = alpha) }
        }
    }
}

private fun segment(millis: Float, start: Float, end: Float): Float = ((millis - start) / (end - start)).coerceIn(0f, 1f)

private class Dot(val x: Float, val spreadTo: Float, val hopStart: Float)

private class LaunchPaths {
    val bubble: Path = parse(BUBBLE)
    val letters: List<Pair<Path, Offset>> = listOf(
        parse(LEFT_RING, evenOdd = true) to Offset(316f, 499f),
        parse(BIEUP, evenOdd = true) to Offset(512f, 498f),
        parse(RIGHT_RING, evenOdd = true) to Offset(708f, 499f),
    )

    private fun parse(data: String, evenOdd: Boolean = false): Path =
        PathParser().parsePathString(data).toPath().apply { if (evenOdd) fillType = PathFillType.EvenOdd }
}

// 그림 좌표는 앱 아이콘 그림(1024 정사각)의 픽셀이고 docs/design.md 로고의 값이다.
// 스플래시 드로어블은 이 그림을 0.1875배로 그려 1이 0.1875dp다.
private val UNIT = 0.1875.dp
private const val CENTER = 512f

private const val BUBBLE =
    "M272,280H752A120,120 0,0 1,872,400V598A120,120 0,0 1,752,718H432L322.5,813.8Q312,823 312,809V718H272A120,120 0,0 1,152,598V400A120,120 0,0 1,272,280Z"
private const val LEFT_RING = "M228,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM272,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0Z"
private const val RIGHT_RING = "M620,499a88,88 0,1 0,176,0a88,88 0,1 0,-176,0ZM664,499a44,44 0,1 0,88,0a44,44 0,1 0,-88,0Z"
private const val BIEUP =
    "M430,422A8,8 0,0 1,438,414H466A8,8 0,0 1,474,422V463H550V422A8,8 0,0 1,558,414H586A8,8 0,0 1,594,422V573A10,10 0,0 1,584,583H440A10,10 0,0 1,430,573ZM474,499H550V548H474Z"

// 입력 중 점은 메신저의 점처럼 가운데로 모아 둔다.
// 첫 장면이라 스플래시 드로어블의 구멍 셋과 자리와 크기가 같다.
private const val DOT_Y = 499f
private const val DOT_RADIUS = 34f
private const val HOP_HEIGHT = 26f
private const val HOP_MILLIS = 250f
private val DOTS = listOf(
    Dot(x = 392f, spreadTo = 316f, hopStart = 150f),
    Dot(x = 512f, spreadTo = 512f, hopStart = 230f),
    Dot(x = 632f, spreadTo = 708f, hopStart = 310f),
)

private const val SPREAD_START = 560f
private const val SPREAD_END = 720f
private const val LETTERS_START = 600f
private const val LETTERS_END = 800f
private const val LETTER_FROM = 0.35f
private val Overshoot = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)

// 글자가 선 뒤 잠깐 보여 주고 흐려진다.
// 켤 때마다 보는 화면이라 모두 합쳐 1.25초를 넘기지 않는다.
private const val PLAY_MILLIS = 1000
private const val FADE_MILLIS = 250
