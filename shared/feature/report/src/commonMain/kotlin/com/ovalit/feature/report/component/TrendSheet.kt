package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaColor
import com.ovalit.core.ui.label
import com.ovalit.core.ui.valueText
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.dynamic_usual
import com.ovalit.feature.report.resources.trend_body
import com.ovalit.feature.report.resources.trend_body_baseline
import com.ovalit.feature.report.resources.trend_entry
import com.ovalit.feature.report.resources.trend_title
import org.jetbrains.compose.resources.stringResource

private val EntryBarsWidth = 58.dp
private val EntryBarsHeight = 20.dp
private val RowBarsHeight = 44.dp
private val RowNameWidth = 88.dp

/**
 * 홈 고정 칸 밑의 "지난 8주 흐름 한눈에 보기" 줄입니다. 누르면 [TrendSheet]가 뜹니다. 막대를 조금 그려 두어 누르면 무엇이
 * 나오는지 보이게 합니다.
 *
 * 사용자 결정(2026-09-29): 여러 지표의 흐름을 홈에 늘어놓으면 복잡해서 시트로 뺐다. 홈에는 입구 한 줄만 둔다.
 */
@Composable
internal fun TrendEntry(report: WeeklyReport.Ready, metric: FixedMetric, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val title = stringResource(Res.string.trend_entry)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 면까지 같이 줄도록 누름 효과를 면보다 앞에 단다(CLAUDE.md 디자인)
            .clickable(role = Role.Button, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.raised)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrendBarRow(metric, report, Modifier.size(width = EntryBarsWidth, height = EntryBarsHeight), gap = 2.dp)
        Spacer(Modifier.width(10.dp))
        OvalitText(text = title, modifier = Modifier.weight(1f), style = OvalitTheme.typography.label)
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 14.dp)
    }
}

/**
 * 고정 지표의 8주 막대를 한 시트에 모은 것입니다. 지표마다 자기 범위로 막대를 그립니다. 한 축에 겹치면 K/D 1.2와
 * 피해량 140을 같은 눈금에 올리게 됩니다.
 *
 * 사용자 요청(2026-10-03): 막대만으로는 전에 얼마였고 지금 얼마인지 안 보였다. 줄마다 지난 4주 평균에 점선을 긋고, 막대를
 * 누르거나 옆으로 끌면 모든 줄이 그 주 값으로 바뀐다. 처음에는 리포트 기간 값이다.
 */
@Composable
internal fun TrendSheet(
    report: WeeklyReport.Ready,
    metrics: List<FixedMetric>,
    onDismiss: () -> Unit,
) {
    val baselineWeeks = report.baseline?.weeks
    OvalitBottomSheet(
        title = stringResource(Res.string.trend_title),
        body = if (baselineWeeks != null) {
            stringResource(Res.string.trend_body_baseline, baselineWeeks)
        } else {
            stringResource(Res.string.trend_body)
        },
        onDismiss = onDismiss,
    ) {
        TrendSheetBody(report, metrics)
    }
}

// 시트는 따로 창을 띄워서 프리뷰에 안 나온다. 프리뷰와 테스트는 이 본문만 그린다.
@Composable
internal fun TrendSheetBody(
    report: WeeklyReport.Ready,
    metrics: List<FixedMetric>,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    // 고른 주는 모든 줄이 같이 쓴다. 한 주를 골라 그 주 지표를 한꺼번에 견준다.
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(modifier = modifier) {
        TrendReadout(report, selected)
        Spacer(Modifier.height(OvalitSpacing.xs))
        metrics.forEachIndexed { index, metric ->
            if (index > 0) Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.lineWeak))
            TrendRow(report, metric, selected, onSelect = { selected = it })
        }
        // 막대 열 밑에만 "7주 전 · 이번 주"를 단다
        Row {
            Spacer(Modifier.width(RowNameWidth + OvalitSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                TrendAxis(report)
                TrendNotes(report)
            }
        }
    }
}

@Composable
private fun TrendRow(report: WeeklyReport.Ready, metric: FixedMetric, selected: Int?, onSelect: (Int?) -> Unit) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    val metrics = if (selected == null) report.metrics else report.trend.getOrNull(selected)?.metrics
    val current = metrics?.let(metric.value)
    val usual = report.baseline?.let { metric.value(it.metrics) }
    val valueStyle = typography.metricS.copy(fontSize = 17.sp, lineHeight = 22.sp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.width(RowNameWidth).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OvalitText(text = stringResource(metric.label), style = typography.caption, color = colors.t2, maxLines = 1)
            OvalitText(
                text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                style = valueStyle,
                // KDA 숫자는 어디서나 구간 색이다(CLAUDE.md 디자인)
                color = if (metric == FixedMetric.KDA && current != null) kdaColor(current, below = colors.t1) else colors.t1,
                maxLines = 1,
            )
            if (usual != null) {
                OvalitText(
                    text = stringResource(Res.string.dynamic_usual, metric.format.valueText(usual)),
                    style = typography.caption,
                    color = colors.t3,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        TrendBarRow(
            metric = metric,
            report = report,
            modifier = Modifier.weight(1f).height(RowBarsHeight),
            gap = 3.dp,
            selected = selected,
            onSelect = onSelect,
            baseline = usual,
        )
    }
}
