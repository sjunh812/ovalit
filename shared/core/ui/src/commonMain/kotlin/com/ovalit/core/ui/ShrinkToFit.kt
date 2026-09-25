package com.ovalit.core.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * 폭이 정해진 칸에서 글자를 키운 사용자에게 "전투점수"가 "전투"로 잘리지 않게, 줄을 바꾸는 대신 글자를
 * 줄입니다. `maxLines = 1`과 같이 씁니다. [min]도 sp라서 글자 크기 설정을 따라 같이 커집니다.
 */
fun shrinkToFit(size: TextUnit, min: TextUnit = 9.sp) = TextAutoSize.StepBased(minFontSize = min, maxFontSize = size)

/**
 * 나란히 놓인 칸들의 글자를 한 크기로 맞춥니다. 칸마다 따로 줄이면 긴 글자만 작아져서 "전투점수" 옆의 "K/D"가
 * 더 커 보이고, 줄 높이도 달라져 아래 숫자의 높이가 칸마다 어긋납니다. 가장 긴 글자가 [width]에 한 줄로
 * 들어가는 크기를 모든 칸에 씁니다. 다 들어가면 [style]을 그대로 돌려줍니다.
 *
 * 크기를 재는 데 칸 폭이 필요하므로 `BoxWithConstraints` 안에서 부릅니다. 세로 구분선 높이를 맞추려고
 * `IntrinsicSize.Min`을 건 줄 안에서는 부르지 않습니다.
 *
 * @param width 한 칸에서 글자가 쓸 수 있는 폭입니다.
 */
@Composable
fun rememberFittingStyle(texts: List<String>, style: TextStyle, width: Dp, min: TextUnit = 7.sp): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts, style, width, min, density, measurer) {
        val widest = texts.maxOfOrNull { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width } ?: 0
        // 잰 폭은 올림한 정수라 같은 비율로 줄여도 1px 넘칠 수 있다. 1px 덜 쓰게 잡는다.
        val available = with(density) { width.toPx() } - 1f
        if (widest == 0 || widest <= available) return@remember style
        val scale = available / widest
        val size = (floor(style.fontSize.value * scale * 10f) / 10f).coerceAtLeast(min.value)
        style.copy(
            fontSize = size.sp,
            lineHeight = if (style.lineHeight.isSp) (style.lineHeight.value * size / style.fontSize.value).sp else style.lineHeight,
        )
    }
}

/**
 * 나란한 칸마다 놓인 짧은 줄이 모두 [width] 안에 한 줄로 들어가는지 봅니다. 하나라도 넘치면 false입니다.
 * [rememberFittingStyle]처럼 `BoxWithConstraints` 안에서 부릅니다.
 *
 * @param extra 줄 안에서 글자가 아닌 간격입니다(항목 사이 Spacer 같은 것).
 */
@Composable
fun rememberFitsOnOneLine(lines: List<AnnotatedString>, style: TextStyle, width: Dp, extra: Dp = 0.dp): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(lines, style, width, extra, density, measurer) {
        val available = with(density) { (width - extra).toPx() }
        lines.all { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width <= available }
    }
}

/** [texts] 중 가장 긴 것을 [style]로 한 줄에 그렸을 때의 폭입니다. 이름 칸 폭을 글자에 맞춰 잡을 때 씁니다. */
@Composable
fun rememberWidestWidth(texts: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts, style, density, measurer) {
        val widest = texts.maxOfOrNull { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width } ?: 0
        with(density) { widest.toDp() }
    }
}
