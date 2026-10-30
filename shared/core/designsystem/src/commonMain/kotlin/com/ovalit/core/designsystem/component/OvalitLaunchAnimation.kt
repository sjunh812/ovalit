package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import kotlin.math.floor

/**
 * 앱을 켤 때 시스템 스플래시에 이어 한 번 재생하는 로고 애니메이션입니다.
 * 말풍선의 입력 중 점 셋이 출렁이다 양옆으로 벌어지며 ㅇㅂㅇ이 되고, 다 그리면 화면이 흐려지며 [onFinished]를 부릅니다.
 *
 * 안드로이드 12 이상은 시스템 스플래시(`ic_splash_anim.xml`)가 앱이 뜨기를 기다리는 동안 점을 출렁이게 합니다.
 * 여기서는 [splash]로 그 시간을 넘겨받아 같은 위상에서 이어 그리고, 지금 출렁임이 끝나는 자리에서 ㅇㅂㅇ으로 벌어집니다.
 * 스플래시가 움직이지 않는 iOS와 안드로이드 12 미만은 빈 말풍선에서 점이 떠올라 한 번 출렁인 뒤 벌어집니다(docs/design.md 로고).
 * 앱이 막 뜬 몇 프레임은 밑 화면을 그리느라 밀려서, 프레임이 고르게 돌기 시작한 뒤에 넘겨받습니다.
 * 재생하는 동안 밑 화면은 눌리지 않습니다.
 */
@Composable
fun OvalitLaunchAnimation(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    splash: SplashHandoff = SplashHandoff.None,
) {
    val colors = OvalitTheme.colors
    // 스플래시 애니메이션이 시작한 뒤 흐른 시간이다.
    // 넘겨받기 전에는 점이 없는 빈 말풍선이다.
    var elapsed by remember { mutableFloatStateOf(BEFORE_START) }
    var spreadAt by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val fade = remember { Animatable(1f) }
    val paths = remember { LaunchPaths() }
    LaunchedEffect(Unit) {
        val iconElapsed = splash.awaitIconElapsed()
        val anchor = withFrameMillis { it }
        val readyAt = awaitSteadyFrames()
        // 움직이지 않는 스플래시면 지금부터 점이 떠오른다
        val start = if (iconElapsed != null) anchor - iconElapsed else readyAt - APPEAR_START.toLong()
        spreadAt = spreadStart((readyAt - start).toFloat())
        // 스플래시와 같은 장면을 한 번 그린 뒤에 걷어야 그 사이에 빈 말풍선이 비치지 않는다
        elapsed = (readyAt - start).toFloat()
        withFrameMillis { elapsed = (it - start).toFloat() }
        splash.takeOver()
        while (elapsed < spreadAt + SHOW_AFTER_SPREAD) {
            withFrameMillis { elapsed = (it - start).toFloat() }
        }
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
                drawLaunchFrame(paths, elapsed, spreadAt, colors.onAccent, colors.accent)
            }
        }
    }
}

// 앱이 막 뜨면 밑 화면의 첫 그리기로 프레임이 수백 ms씩 밀린다.
// 그때 넘겨받으면 점이 멈춰 있다가 건너뛰듯 움직여서, 프레임 간격이 고르게 몇 번 이어진 뒤에 넘겨받는다.
// 그동안은 스플래시가 그대로 덮고 있고, 느린 기기에서도 MAX_WAIT_MILLIS를 넘기지 않는다.
private suspend fun awaitSteadyFrames(): Long {
    val start = withFrameMillis { it }
    var last = start
    var steady = 0
    while (steady < STEADY_FRAMES && last - start < MAX_WAIT_MILLIS) {
        val now = withFrameMillis { it }
        steady = if (now - last <= STEADY_GAP_MILLIS) steady + 1 else 0
        last = now
    }
    return last
}

// 넘겨받은 때가 출렁이는 중이면 그 출렁임이 끝나는 자리에서, 쉬는 중이면 바로 벌어진다.
// 점이 한 번은 출렁여야 입력 중으로 읽혀서 첫 출렁임이 끝나기 전에는 벌어지지 않는다.
internal fun spreadStart(readyAt: Float): Float {
    val firstWaveEnd = WAVE_START + WAVE_MILLIS
    if (readyAt <= firstWaveEnd) return firstWaveEnd
    val waveStart = WAVE_START + floor((readyAt - WAVE_START) / WAVE_PERIOD) * WAVE_PERIOD
    val waveEnd = waveStart + WAVE_MILLIS
    return if (readyAt < waveEnd) waveEnd else readyAt
}

private fun DrawScope.drawLaunchFrame(paths: LaunchPaths, millis: Float, spreadAt: Float, bubble: Color, ink: Color) {
    drawPath(paths.bubble, bubble)
    if (millis < 0f) return

    val spread = FastOutSlowInEasing.transform(segment(millis, spreadAt, spreadAt + SPREAD_MILLIS))
    DOTS.forEachIndexed { index, dot ->
        val appearStart = APPEAR_START + index * APPEAR_STAGGER
        val size = Overshoot.transform(segment(millis, appearStart, appearStart + APPEAR_MILLIS))
        if (size <= 0f) return@forEachIndexed
        val hop = if (millis < spreadAt) hopHeight(millis - WAVE_START - index * HOP_STAGGER) else 0f
        // 벌어지면서 글자 자리로 가고 흐려진다
        val x = dot.x + (dot.spreadTo - dot.x) * spread
        drawCircle(ink, radius = DOT_RADIUS * size, center = Offset(x, DOT_Y - HOP_HEIGHT * hop), alpha = 1f - spread)
    }

    // 글자는 작게 나타나 살짝 넘쳤다가 제 크기로 선다
    val lettersStart = spreadAt + LETTERS_DELAY
    val grow = segment(millis, lettersStart, lettersStart + LETTERS_MILLIS)
    if (grow > 0f) {
        val size = LETTER_FROM + (1f - LETTER_FROM) * Overshoot.transform(grow)
        val alpha = (grow * 2f).coerceAtMost(1f)
        paths.letters.forEach { (path, pivot) ->
            scale(scale = size, pivot = pivot) { drawPath(path, ink, alpha = alpha) }
        }
    }
}

// 출렁임 하나의 높이(0~1)다. 스플래시의 키프레임과 같은 곡선이라 넘겨받을 때 점이 튀지 않는다.
private fun hopHeight(sinceWaveStart: Float): Float {
    if (sinceWaveStart < 0f) return 0f
    val x = (sinceWaveStart % WAVE_PERIOD) / HOP_MILLIS
    return when {
        x >= 1f -> 0f
        x < 0.5f -> SineOut.transform(x * 2f)
        else -> 1f - SineIn.transform(x * 2f - 1f)
    }
}

private fun segment(millis: Float, start: Float, end: Float): Float = ((millis - start) / (end - start)).coerceIn(0f, 1f)

private class Dot(val x: Float, val spreadTo: Float)

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
// 자리, 크기, 시각, 곡선은 ic_splash_mark.xml, ic_splash_anim.xml과 같아야 넘겨받을 때 끊기지 않는다.
private const val DOT_Y = 499f
private const val DOT_RADIUS = 40f
private const val DOT_SPACING = 120f
private const val HOP_HEIGHT = 26f
private val DOTS = listOf(
    Dot(x = CENTER - DOT_SPACING, spreadTo = 316f),
    Dot(x = CENTER, spreadTo = CENTER),
    Dot(x = CENTER + DOT_SPACING, spreadTo = 708f),
)

// 시각은 스플래시 애니메이션이 시작한 때부터 잰다
private const val BEFORE_START = -1f
private const val APPEAR_START = 150f
private const val APPEAR_STAGGER = 60f
private const val APPEAR_MILLIS = 160f
private const val WAVE_START = 350f
private const val WAVE_PERIOD = 900f
private const val HOP_STAGGER = 80f
private const val HOP_MILLIS = 250f
private const val WAVE_MILLIS = HOP_STAGGER * 2 + HOP_MILLIS
private val Overshoot = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)
private val SineOut = CubicBezierEasing(0.39f, 0.575f, 0.565f, 1f)
private val SineIn = CubicBezierEasing(0.47f, 0f, 0.745f, 0.715f)

// 글자는 점만 한 크기에서 자라서, 점이 그 자리에서 글자로 바뀌는 것처럼 보인다.
// 88은 ㅇ 바깥 원의 반지름이다.
private const val SPREAD_MILLIS = 160f
private const val LETTERS_DELAY = 40f
private const val LETTERS_MILLIS = 200f
private const val LETTER_FROM = DOT_RADIUS / 88f

// 글자가 선 뒤 잠깐 보여 주고 흐려진다.
// 켤 때마다 보는 화면이라 넘겨받은 뒤 재생과 흐려지기를 합쳐 1.25초를 넘기지 않는다.
private const val SHOW_AFTER_SPREAD = LETTERS_DELAY + LETTERS_MILLIS + 200f
private const val FADE_MILLIS = 200

// 60Hz에서 한 프레임만 밀려도 33ms라 고르지 않은 것으로 본다
private const val STEADY_FRAMES = 3
private const val STEADY_GAP_MILLIS = 25L
private const val MAX_WAIT_MILLIS = 500L
