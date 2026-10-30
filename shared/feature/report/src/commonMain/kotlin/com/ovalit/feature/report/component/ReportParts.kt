package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit

/**
 * 짚을 점과 돌아보기의 헤드라인 글꼴입니다.
 * 문장은 보통 굵기로 두고 바뀐 값만 [headlineEmphasis]로 굵게 합니다.
 * 문장 전체가 굵으면 무엇이 바뀌었는지가 묻힙니다(docs/design.md 글자).
 * 한 문장이라 본문처럼 줄을 끝까지 채우면 "높아요"만 다음 줄에 남아서 제목처럼 줄 길이를 고르게 나눕니다.
 */
@Composable
internal fun headlineStyle(): TextStyle = OvalitTheme.typography.body.copy(lineBreak = LineBreak.Heading)

/** [text] 안의 첫 [value]만 [emphasis]로 칠한 헤드라인입니다. 값이 문장에 없으면 그대로 둡니다. */
internal fun headlineEmphasis(text: String, value: String, emphasis: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    val start = text.indexOf(value)
    if (start >= 0 && value.isNotEmpty()) addStyle(emphasis, start, start + value.length)
}

internal val SectionTitleGap = 16.dp

/** 막대가 있는 줄(라이벌, 친구 비교) 사이입니다. 친구 프로필의 나와 비교와 같습니다. */
internal val BarRowGap = 11.dp

@Composable
internal fun HorizontalLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OvalitTheme.colors.line),
    )
}

/** 보이는 자릿수로 견준 오르내림 색입니다. 오르면 `pos`, 내리면 `neg`, 같으면 `t3`입니다. */
@Composable
internal fun directionColor(format: MetricFormat, current: Double, baseline: Double): Color {
    val colors = OvalitTheme.colors
    return when (format.direction(current, baseline)) {
        1 -> colors.pos
        -1 -> colors.neg
        else -> colors.t3
    }
}

/**
 * 왼쪽 제목과 오른쪽 설명 한 줄입니다.
 * 글자를 키워 둘이 한 줄에 안 들어가면 설명이 다음 줄로 내려갑니다.
 * 한 줄에 억지로 넣으면 "이번 주"가 "이번 / 주"로 꺾입니다.
 */
@Composable
internal fun TitleWithCaption(
    title: String,
    titleStyle: TextStyle,
    caption: AnnotatedString?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        OvalitText(text = title, style = titleStyle, modifier = Modifier.padding(end = OvalitSpacing.sm))
        if (caption != null) {
            OvalitText(
                text = caption,
                modifier = Modifier.padding(bottom = 3.dp),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        }
    }
}

// 고정 칸과 달라진 점 칸이 같이 쓰는 격자다.
// 한 줄에 세 칸씩 폭을 나누고 넘치면 다음 줄로 넘긴다.
// 목업처럼 옆으로 밀면 세 번째 칸이 화면 끝에서 잘린다(docs/DECISIONS.md).
// 칸 사이는 보이지 않는 세로선(1dp) 양옆으로 이만큼씩, 모두 17dp다.
// 384dp 폰에서 칸이 93dp라 가장 긴 숫자 줄("1.09 −0.19")도 줄이지 않고 숫자 옆에 변화량이 든다.
internal const val MetricColumns = 3
internal val MetricColumnGap = 8.dp
internal val MetricRowGap = 24.dp

// 칸을 누르면 면을 이만큼 칸 밖으로 넓힌다.
// 칸 사이(17dp)의 절반 남짓이라 옆 칸 글자와 9dp 떨어진다.
internal val CellPressOutset = 8.dp

// 이름 뒤 간격 3dp와 화살표 10dp를 더한 폭이다
internal val ChevronSpace = 13.dp

/** 칸 하나의 숫자, 그 옆 변화량, 밑의 평소 값 글자입니다. */
internal class MetricLineText(val value: String, val change: String?, val usual: String)

/**
 * 칸 숫자 옆에 변화량을 작게 붙이고 밑에 "평소 177"을 두는 글꼴입니다(사용자 결정).
 * 숫자와 변화량을 한 덩어리로 읽고 비교 기준은 한 단계 조용히 둡니다.
 * 한 칸이라도 숫자 옆에 변화량이 안 들어가면 모든 칸의 숫자와 변화량을 같은 비율로 줄여 봅니다.
 * 가장 작게 줄여도 안 들어가면 모든 칸에서 변화량을 평소 값 앞으로 내리고 그 줄도 안 들어가면 세 줄로 나눕니다.
 * 한 칸만 내리면 그 칸만 높아져 줄이 어긋납니다.
 */
internal class MetricLineStyle(val value: TextStyle, val change: TextStyle, val usual: TextStyle, val layout: MetricLineLayout)

internal enum class MetricLineLayout {
    /** 숫자 옆에 변화량, 밑에 평소 값입니다. */
    BESIDE,

    /** 숫자 밑 한 줄에 변화량과 평소 값입니다. */
    BELOW,

    /** 숫자, 변화량, 평소 값을 한 줄에 하나씩 둡니다. */
    STACKED,
}

/**
 * @param cells 칸마다 숫자, 변화량, 평소 값 글자입니다.
 *   홈은 고정 칸과 달라진 점의 칸을 모두 넘겨 두 카드가 같은 크기, 같은 배치를 쓰게 합니다.
 *   카드마다 따로 정하면 폭이 좁은 기기에서 글자가 한 자 긴 달라진 점만 다르게 놓입니다.
 */
@Composable
internal fun rememberMetricLineStyle(cells: List<MetricLineText>, width: Dp): MetricLineStyle {
    val typography = OvalitTheme.typography
    val value = typography.metricM
    // 숫자 옆이라 색과 자리로 먼저 읽혀서 작게 두고 굵기만 Medium으로 한 단계 올린다
    val change = typography.delta.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    // 칸이 좁은 격자라 평소 값은 다른 설명 글씨(13)보다 한 단계 작은 12다
    val usual = rememberFittingStyle(cells.map { it.usual }, typography.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), width)
    // 변화량을 내릴 때 숫자는 모든 칸이 들어갈 때까지 같은 비율로 줄인다.
    // 칸마다 저절로 줄게 두면 칸마다 줄 높이가 달라져 아래 줄이 어긋난다.
    val valueAlone = rememberFittingStyle(cells.map { it.value }, value, width, min = 14.sp)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(cells, value, valueAlone, change, usual, width, density, measurer) {
        // 잰 폭은 올림한 정수라 들어간다고 봐도 그릴 때 1px 넘쳐 한 칸만 저절로 줄어들 수 있다.
        // rememberFittingStyle처럼 1px 덜 쓰게 잡는다.
        val available = with(density) { width.toPx() } - 1f
        val gap = with(density) { ChangeGap.toPx() }
        fun widthOf(text: String, style: TextStyle) = measurer.measure(text, style, softWrap = false, maxLines = 1).size.width
        fun fits(value: TextStyle, change: TextStyle) = cells.all { cell ->
            widthOf(cell.value, value) + (cell.change?.let { gap + widthOf(it, change) } ?: 0f) <= available
        }
        fun fitsBelow(change: TextStyle, usual: TextStyle) = cells.all { cell ->
            (cell.change?.let { widthOf(it, change) + gap } ?: 0f) + widthOf(cell.usual, usual) <= available
        }
        LineScales.firstNotNullOfOrNull { scale ->
            val scaledValue = value.scaled(scale)
            val scaledChange = change.scaled(scale)
            if (fits(scaledValue, scaledChange)) MetricLineStyle(scaledValue, scaledChange, usual, MetricLineLayout.BESIDE) else null
        } ?: LineScales.firstNotNullOfOrNull { scale ->
            val scaledChange = change.scaled(scale)
            val scaledUsual = usual.scaled(scale)
            if (fitsBelow(scaledChange, scaledUsual)) MetricLineStyle(valueAlone, scaledChange, scaledUsual, MetricLineLayout.BELOW) else null
        } ?: MetricLineStyle(valueAlone, change, usual, MetricLineLayout.STACKED)
    }
}

private fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale == 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

// 줄여 보는 비율이다.
// 0.85면 변화량이 10sp대까지 내려가 이보다 줄이면 읽기 어렵다.
private val LineScales = listOf(1f, 0.95f, 0.9f, 0.85f)

private val ChangeGap = 4.dp

/**
 * 고정 칸과 달라진 점 칸의 숫자 줄과 그 밑 평소 값 줄입니다.
 * 변화량이 없으면 숫자와 평소 값만 둡니다.
 * 변화량은 두 카드 모두 같은 굵기라 나란히 봐도 같은 무게입니다.
 *
 * @param value 숫자를 그립니다. 고정 칸은 바뀔 때 숫자가 구르고 달라진 점은 그냥 바뀌어서 칸이 정합니다.
 */
@Composable
internal fun MetricValueLines(
    style: MetricLineStyle,
    change: String?,
    changeColor: Color,
    usual: String,
    value: @Composable (Modifier, TextStyle) -> Unit,
) {
    val changeText: @Composable (Modifier) -> Unit = { modifier ->
        if (change != null) OvalitText(text = change, modifier = modifier, style = style.change, color = changeColor, maxLines = 1)
    }
    val usualText: @Composable (Modifier) -> Unit = { modifier ->
        OvalitText(
            text = usual,
            modifier = modifier,
            style = style.usual,
            color = OvalitTheme.colors.t3,
            maxLines = 1,
            autoSize = shrinkToFit(style.usual.fontSize),
        )
    }
    when (style.layout) {
        MetricLineLayout.BESIDE -> {
            Row {
                value(Modifier.alignByBaseline(), style.value)
                if (change != null) {
                    Spacer(Modifier.width(ChangeGap))
                    changeText(Modifier.alignByBaseline())
                }
            }
            Spacer(Modifier.height(2.dp))
            usualText(Modifier)
        }
        MetricLineLayout.BELOW -> {
            value(Modifier, style.value)
            Spacer(Modifier.height(2.dp))
            Row {
                if (change != null) {
                    changeText(Modifier.alignByBaseline())
                    Spacer(Modifier.width(ChangeGap))
                }
                usualText(Modifier.alignByBaseline())
            }
        }
        MetricLineLayout.STACKED -> {
            value(Modifier, style.value)
            Spacer(Modifier.height(2.dp))
            changeText(Modifier)
            usualText(Modifier)
        }
    }
}
