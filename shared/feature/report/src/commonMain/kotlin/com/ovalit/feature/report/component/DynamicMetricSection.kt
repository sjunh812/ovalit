package com.ovalit.feature.report.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Baseline
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeeklyReport
import com.ovalit.feature.report.format
import com.ovalit.feature.report.hasGoodDirection
import com.ovalit.feature.report.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.baseline_average
import com.ovalit.feature.report.resources.baseline_missing
import com.ovalit.feature.report.resources.dynamic_caption
import com.ovalit.feature.report.resources.dynamic_caption_with_role
import com.ovalit.feature.report.resources.dynamic_title_moved
import com.ovalit.feature.report.resources.dynamic_title_steady
import com.ovalit.feature.report.resources.dynamic_title_unknown
import com.ovalit.feature.report.resources.dynamic_unknown_hint
import com.ovalit.feature.report.sampleText
import com.ovalit.core.ui.valueText
import org.jetbrains.compose.resources.stringResource

// 세 번째 칸이 화면 끝에서 살짝 잘리게 둔다. 옆으로 밀어 볼 수 있다는 표시다.
// 간격은 구분선 양옆에만 준다. 칸 폭 안에 간격을 넣으면 첫 칸만 내용이 넓어진다.
private val ColumnWidth = 124.dp
private val ColumnGap = 18.dp

@Composable
internal fun DynamicMetricSection(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalLine(Modifier.padding(horizontal = OvalitSpacing.gutter))
        Spacer(Modifier.height(18.dp))
        DynamicSectionTitle(report)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .padding(horizontal = OvalitSpacing.gutter),
        ) {
            report.dynamic.forEachIndexed { index, slot ->
                if (index > 0) {
                    Spacer(Modifier.width(ColumnGap))
                    VerticalLine()
                    Spacer(Modifier.width(ColumnGap))
                }
                DynamicMetricColumn(
                    slot = slot,
                    metrics = report.metrics,
                    baseline = report.baseline,
                    modifier = Modifier.width(ColumnWidth),
                )
            }
        }
    }
}

@Composable
private fun DynamicSectionTitle(report: WeeklyReport.Ready) {
    val movements = report.dynamic.map { it.movement }

    // 판단을 보류한 칸만 있을 때 "큰 변화 없음"이라고 하면 안 된다. 변화가 없는 게 아니라 모르는 것이다.
    val title = when {
        Movement.MOVED in movements -> stringResource(Res.string.dynamic_title_moved)
        Movement.STEADY in movements -> stringResource(Res.string.dynamic_title_steady, periodLabel(report.period))
        else -> stringResource(Res.string.dynamic_title_unknown)
    }
    val caption = report.baseline?.let { baseline ->
        report.mainRole?.let { role ->
            stringResource(Res.string.dynamic_caption_with_role, stringResource(role.label), baseline.weeks)
        } ?: stringResource(Res.string.dynamic_caption, baseline.weeks)
    }

    Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = title,
            titleStyle = OvalitTheme.typography.bodyStrong,
            caption = caption?.let(::AnnotatedString),
        )
        if (movements.all { it == Movement.UNKNOWN }) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.dynamic_unknown_hint),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
            )
        }
    }
}

@Composable
private fun DynamicMetricColumn(
    slot: DynamicSlot,
    metrics: MatchMetrics,
    baseline: Baseline?,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    val metric = slot.metric
    val current = metric.value(metrics)
    val usual = baseline?.let { metric.value(it.metrics) }
    val judged = slot.movement != Movement.UNKNOWN && baseline != null && current != null && usual != null

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        OvalitText(
            text = stringResource(metric.label),
            style = OvalitTheme.typography.caption,
            color = colors.t2,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            OvalitText(
                text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.metricM,
                color = if (judged) colors.t1 else colors.t2,
            )
            if (judged) {
                Spacer(Modifier.width(6.dp))
                OvalitText(
                    text = metric.format.formatChange(current, usual),
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.metricS,
                    color = changeColor(slot, current, usual),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OvalitText(
            text = if (judged) {
                stringResource(Res.string.baseline_average, baseline.weeks, metric.format.valueText(usual))
            } else {
                stringResource(Res.string.baseline_missing)
            },
            style = OvalitTheme.typography.caption,
            color = colors.t3,
        )
        OvalitText(
            text = metric.sampleText(metrics),
            style = OvalitTheme.typography.caption,
            color = colors.t3,
        )
    }
}

// 움직였다고 판단한 칸만 색을 칠한다. 평소 범위 안의 변화에 색을 칠하면 흔들림이 경고처럼 읽힌다.
@Composable
private fun changeColor(slot: DynamicSlot, current: Double, usual: Double): Color = when {
    slot.movement != Movement.MOVED -> OvalitTheme.colors.t3
    !slot.metric.hasGoodDirection -> OvalitTheme.colors.t1
    else -> directionColor(slot.metric.format, current, usual)
}
