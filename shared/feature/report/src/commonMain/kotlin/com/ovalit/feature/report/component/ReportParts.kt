package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit

@Composable
internal fun VerticalLine() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(OvalitTheme.colors.line),
    )
}

/**
 * 짚을 점과 개선 포인트의 헤드라인 글꼴입니다. 한 문장이라 본문처럼 줄을 끝까지 채우면 "높아요"만 다음 줄에 남아서, 제목처럼
 * 줄 길이를 고르게 나눕니다(docs/design.md).
 */
@Composable
internal fun headlineStyle(): TextStyle = OvalitTheme.typography.bodyStrong.copy(lineBreak = LineBreak.Heading)

/** 묶음 제목과 내용 사이입니다. 홈의 모든 묶음이 같이 씁니다. */
internal val SectionTitleGap = 12.dp

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
 * 왼쪽 제목과 오른쪽 설명 한 줄입니다. 글자를 키워 둘이 한 줄에 안 들어가면 설명이 다음 줄로
 * 내려갑니다. 한 줄에 억지로 넣으면 "이번 주"가 "이번 / 주"로 꺾입니다.
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

// 고정 칸과 달라진 점 칸이 같이 쓰는 격자다. 한 줄에 세 칸씩 폭을 나누고 넘치면 다음 줄로 넘긴다. 목업처럼 옆으로 밀면
// 세 번째 칸이 화면 끝에서 잘린다(docs/DECISIONS.md).
internal const val MetricColumns = 3
internal val MetricColumnGap = 14.dp
internal val MetricRowGap = 20.dp

// 칸을 누르면 면을 이만큼 칸 밖으로 넓힌다. 칸 사이 간격의 절반을 조금 넘겨 옆 칸 글자와 6dp 떨어진다.
internal val CellPressOutset = 8.dp

// 이름 뒤 간격 3dp와 화살표 10dp를 더한 폭이다
internal val ChevronSpace = 13.dp

/**
 * 칸 숫자 밑 한 줄의 글꼴입니다. 변화량과 "평소 177"을 나란히 둡니다. 한 칸이라도 안 들어가면 모든 칸의 두 글자를 같은 비율로
 * 조금씩 줄여 보고, 가장 작게 줄여도 안 들어갈 때만 모든 칸에서 두 줄로 내립니다. 한 칸만 내리면 그 칸만 높아져 줄이 어긋납니다.
 */
internal class MetricSubLineStyle(val change: TextStyle, val usual: TextStyle, val stacked: Boolean)

/**
 * @param cells 칸마다 변화량과 평소 값 글자입니다. 홈은 고정 칸과 달라진 점의 칸을 모두 넘겨 두 카드가 같은 크기, 같은 줄 수를
 *   쓰게 합니다. 카드마다 따로 정하면 폭이 좁은 기기에서 글자가 한 자 긴 달라진 점만 두 줄로 내려갑니다.
 */
@Composable
internal fun rememberSubLineStyle(cells: List<Pair<String?, String>>, width: Dp): MetricSubLineStyle {
    val typography = OvalitTheme.typography
    // 변화량은 평소 값보다 한 단계 작게 둔다
    val change = typography.delta
    val usual = rememberFittingStyle(cells.map { it.second }, typography.caption, width)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(cells, change, usual, width, density, measurer) {
        val available = with(density) { width.toPx() }
        val gap = with(density) { SubLineGap.toPx() }
        fun fits(change: TextStyle, usual: TextStyle) = cells.all { (delta, normal) ->
            if (delta == null) return@all true
            val deltaWidth = measurer.measure(delta, change, softWrap = false, maxLines = 1).size.width
            val normalWidth = measurer.measure(normal, usual, softWrap = false, maxLines = 1).size.width
            deltaWidth + gap + normalWidth <= available
        }
        SubLineScales.firstNotNullOfOrNull { scale ->
            val scaledChange = change.scaled(scale)
            val scaledUsual = usual.scaled(scale)
            if (fits(scaledChange, scaledUsual)) MetricSubLineStyle(scaledChange, scaledUsual, stacked = false) else null
        } ?: MetricSubLineStyle(change, usual, stacked = true)
    }
}

private fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale == 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

// 줄여 보는 비율이다. 0.85면 변화량이 9sp대까지 내려가 이보다 줄이면 읽기 어렵다.
private val SubLineScales = listOf(1f, 0.95f, 0.9f, 0.85f)

private val SubLineGap = 6.dp

/** 고정 칸과 달라진 점 칸의 숫자 밑 한 줄입니다. 변화량이 없으면 평소 값만 둡니다. */
@Composable
internal fun MetricSubLine(change: String?, changeColor: Color, usual: String, style: MetricSubLineStyle) {
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
    when {
        change == null -> usualText(Modifier)
        style.stacked -> Column {
            OvalitText(text = change, style = style.change, color = changeColor, maxLines = 1)
            usualText(Modifier)
        }
        else -> Row {
            OvalitText(text = change, modifier = Modifier.alignByBaseline(), style = style.change, color = changeColor, maxLines = 1)
            Spacer(Modifier.width(SubLineGap))
            usualText(Modifier.alignByBaseline())
        }
    }
}
