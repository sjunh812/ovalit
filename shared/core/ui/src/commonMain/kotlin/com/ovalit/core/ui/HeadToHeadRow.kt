package com.ovalit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchMetrics
import org.jetbrains.compose.resources.stringResource

private const val NO_VALUE = "–"
private val LabelWidth = 48.dp
private val ValueWidth = 40.dp

// 글자를 키우면 "전투점수"가 "전투"로 잘리고 숫자에 붙는다. 줄을 바꾸지 않고 글자를 줄인다.
private fun shrinkToFit(size: TextUnit) = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size)

/**
 * 라이벌 대결과 S5 "나와 비교"의 한 줄입니다. 앞선 쪽이 차지하는 몫만큼 그쪽 끝에서부터 막대를
 * 채웁니다. 내가 앞서면 밝게, 상대가 앞서면 흐리게 칠합니다.
 */
@Composable
fun HeadToHeadRow(metric: FixedMetric, mine: MatchMetrics, theirs: MatchMetrics?) {
    val colors = OvalitTheme.colors
    val my = metric.value(mine)
    val their = theirs?.let(metric.value)
    val result = if (theirs == null) 0 else compare(metric, mine, theirs)
    val share = if (my != null && their != null && my + their > 0) {
        (maxOf(my, their) / (my + their)).toFloat()
    } else {
        0f
    }

    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = stringResource(metric.label),
            modifier = Modifier.width(LabelWidth),
            style = OvalitTheme.typography.caption,
            color = colors.t2,
            maxLines = 1,
            autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize),
        )
        Spacer(Modifier.width(OvalitSpacing.xs))
        Value(my?.let { metric.format.valueText(it) } ?: NO_VALUE, leading = result > 0, alignEnd = false)
        Spacer(Modifier.width(OvalitSpacing.sm))
        Box(modifier = Modifier.weight(1f).height(3.dp).background(colors.fill)) {
            if (result != 0) {
                Box(
                    modifier = Modifier
                        .align(if (result > 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .fillMaxWidth(share)
                        .height(3.dp)
                        .background(if (result > 0) colors.t1 else colors.t4),
                )
            }
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        Value(their?.let { metric.format.valueText(it) } ?: NO_VALUE, leading = result < 0, alignEnd = true)
    }
}

@Composable
private fun Value(text: String, leading: Boolean, alignEnd: Boolean) {
    OvalitText(
        text = text,
        modifier = Modifier.width(ValueWidth),
        style = OvalitTheme.typography.metricS.copy(fontWeight = if (leading) FontWeight.Bold else FontWeight.Medium),
        color = if (leading) OvalitTheme.colors.t1 else OvalitTheme.colors.t3,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        maxLines = 1,
        autoSize = shrinkToFit(OvalitTheme.typography.metricS.fontSize),
    )
}



/** 보이는 자릿수로 반올림한 값끼리 겨룹니다. 내가 앞서면 1, 뒤지면 -1, 같거나 모르면 0입니다. */
fun compare(metric: FixedMetric, mine: MatchMetrics, theirs: MatchMetrics): Int {
    val my = metric.value(mine) ?: return 0
    val their = metric.value(theirs) ?: return 0
    return metric.format.direction(my, their)
}
