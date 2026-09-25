package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.MetricFormat

internal const val NO_VALUE = "–"

internal const val SEPARATOR = " · "

@Composable
internal fun VerticalLine() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(OvalitTheme.colors.line),
    )
}

@Composable
internal fun HorizontalLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OvalitTheme.colors.line),
    )
}

/** 오르면 [OvalitTheme.colors.pos], 내리면 [OvalitTheme.colors.neg]. 화면에 보이는 값이 같으면 무채색입니다. */
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
