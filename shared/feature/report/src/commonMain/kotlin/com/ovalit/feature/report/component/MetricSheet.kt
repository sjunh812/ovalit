package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MIN_VOLATILITY_WEEKS
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.model.usualRange
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaColor
import com.ovalit.core.ui.label
import com.ovalit.core.ui.perMatchKda
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.withThousands
import com.ovalit.feature.report.format
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.period_label_matches
import com.ovalit.feature.report.resources.sheet_combat_score_body
import com.ovalit.feature.report.resources.sheet_combat_score_formula
import com.ovalit.feature.report.resources.sheet_combat_score_method
import com.ovalit.feature.report.resources.sheet_combat_score_name
import com.ovalit.feature.report.resources.sheet_damage_body
import com.ovalit.feature.report.resources.sheet_damage_formula
import com.ovalit.feature.report.resources.sheet_damage_method
import com.ovalit.feature.report.resources.sheet_damage_name
import com.ovalit.feature.report.resources.sheet_headshot_body
import com.ovalit.feature.report.resources.sheet_headshot_formula
import com.ovalit.feature.report.resources.sheet_headshot_method
import com.ovalit.feature.report.resources.sheet_headshot_name
import com.ovalit.feature.report.resources.sheet_kd_body
import com.ovalit.feature.report.resources.sheet_kd_formula
import com.ovalit.feature.report.resources.sheet_kd_method
import com.ovalit.feature.report.resources.sheet_kd_no_deaths
import com.ovalit.feature.report.resources.sheet_kda_body
import com.ovalit.feature.report.resources.sheet_kda_formula
import com.ovalit.feature.report.resources.sheet_kda_method
import com.ovalit.feature.report.resources.sheet_kda_no_deaths
import com.ovalit.feature.report.resources.sheet_method_title
import com.ovalit.feature.report.resources.sheet_per_match_kd
import com.ovalit.feature.report.resources.sheet_per_match_kda
import com.ovalit.feature.report.resources.sheet_sample_rounds
import com.ovalit.feature.report.resources.sheet_usual_average
import com.ovalit.feature.report.resources.sheet_usual_missing
import com.ovalit.feature.report.resources.sheet_usual_range
import com.ovalit.feature.report.resources.sheet_usual_same
import com.ovalit.feature.report.resources.sheet_usual_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val TrendHeight = 62.dp

/** S1-a 지표 설명 시트입니다. 고정 지표 칸을 누르면 뜹니다. */
@Composable
internal fun MetricSheet(
    metric: FixedMetric,
    report: WeeklyReport.Ready,
    onDismiss: () -> Unit,
) {
    val text = metric.sheetText
    OvalitBottomSheet(
        title = stringResource(metric.label),
        titleNote = text.name?.let { stringResource(it) },
        body = stringResource(text.body),
        onDismiss = onDismiss,
    ) {
        MetricSheetBody(metric, report)
    }
}

// 시트는 따로 창을 띄워서 프리뷰에 안 나온다. 프리뷰와 테스트는 이 본문만 그린다.
@Composable
internal fun MetricSheetBody(metric: FixedMetric, report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CurrentValue(metric, report)
        val usual = report.baseline?.let { baseline -> metric.value(baseline.metrics)?.let { baseline.weeks to it } }
        if (usual != null) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.sheet_usual_average, usual.first, metric.format.valueText(usual.second)),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
            )
        }
        // 판당 킬·데스·어시는 홈 칸에 두지 않고 K/D와 KDA 시트에서 푼다
        perMatchText(metric, report.metrics)?.let { text ->
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(text = text, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
        }
        Spacer(Modifier.height(OvalitSpacing.xl))
        TrendBars(metric, report)
        Spacer(Modifier.height(22.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        UsualRangeSection(metric, report)
        Spacer(Modifier.height(18.dp))
        HorizontalLine()
        Spacer(Modifier.height(18.dp))
        MethodSection(metric, report.metrics)
    }
}

@Composable
private fun perMatchText(metric: FixedMetric, metrics: MatchMetrics): String? {
    val perMatch = metrics.perMatchKda() ?: return null
    return when (metric) {
        FixedMetric.KD -> stringResource(Res.string.sheet_per_match_kd, perMatch[0], perMatch[1])
        FixedMetric.KDA -> stringResource(Res.string.sheet_per_match_kda, perMatch[0], perMatch[1], perMatch[2])
        else -> null
    }
}

private class SheetText(
    val name: StringResource?,
    val body: StringResource,
    val method: StringResource,
    val formula: StringResource,
)

private val FixedMetric.sheetText: SheetText
    get() = when (this) {
        FixedMetric.COMBAT_SCORE -> SheetText(
            name = Res.string.sheet_combat_score_name,
            body = Res.string.sheet_combat_score_body,
            method = Res.string.sheet_combat_score_method,
            formula = Res.string.sheet_combat_score_formula,
        )
        // K/D는 화면 라벨이 곧 정식 명칭이다
        FixedMetric.KD -> SheetText(
            name = null,
            body = Res.string.sheet_kd_body,
            method = Res.string.sheet_kd_method,
            formula = Res.string.sheet_kd_formula,
        )
        FixedMetric.DAMAGE -> SheetText(
            name = Res.string.sheet_damage_name,
            body = Res.string.sheet_damage_body,
            method = Res.string.sheet_damage_method,
            formula = Res.string.sheet_damage_formula,
        )
        FixedMetric.HEADSHOT_RATE -> SheetText(
            name = Res.string.sheet_headshot_name,
            body = Res.string.sheet_headshot_body,
            method = Res.string.sheet_headshot_method,
            formula = Res.string.sheet_headshot_formula,
        )
        // KDA도 화면 라벨이 곧 정식 명칭이다
        FixedMetric.KDA -> SheetText(
            name = null,
            body = Res.string.sheet_kda_body,
            method = Res.string.sheet_kda_method,
            formula = Res.string.sheet_kda_formula,
        )
    }

@Composable
private fun CurrentValue(metric: FixedMetric, report: WeeklyReport.Ready) {
    val current = metric.value(report.metrics)
    val usual = report.baseline?.let { metric.value(it.metrics) }

    // 좁은 화면에서 오른쪽 표본이 먼저 자리를 잡으면 큰 숫자와 변화량이 꺾인다. 모자라면 표본을 다음 줄로 내린다.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.sm),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Row {
            OvalitText(
                text = current?.let { metric.format.valueText(it) } ?: NO_VALUE,
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.metricXl,
                // KDA 숫자는 어디서나 구간 색이다(docs/design.md)
                color = if (metric == FixedMetric.KDA && current != null) kdaColor(current, below = OvalitTheme.colors.t1) else OvalitTheme.colors.t1,
            )
            if (current != null && usual != null) {
                Spacer(Modifier.width(OvalitSpacing.sm))
                OvalitText(
                    text = metric.format.formatChange(current, usual),
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.metricS,
                    color = directionColor(metric.format, current, usual),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            OvalitText(
                text = stringResource(Res.string.period_label_matches, periodLabel(report.period), report.metrics.matches),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
            OvalitText(
                text = stringResource(Res.string.sheet_sample_rounds, report.metrics.rounds),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun TrendBars(metric: FixedMetric, report: WeeklyReport.Ready) {
    // 막대를 누르면 그 주 값을 막대 위에 띄운다. 처음에는 리포트 기간이다.
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val baseline = report.baseline?.let { baseline -> metric.value(baseline.metrics)?.let { baseline.weeks to it } }
    Column {
        TrendReadout(report, selected, metric = metric)
        Spacer(Modifier.height(OvalitSpacing.sm))
        TrendBarRow(
            metric = metric,
            report = report,
            modifier = Modifier.fillMaxWidth().height(TrendHeight),
            selected = selected,
            onSelect = { selected = it },
            baseline = baseline?.second,
        )
        Spacer(Modifier.height(OvalitSpacing.sm))
        TrendAxis(report)
        TrendNotes(report, baselineWeeks = baseline?.first)
    }
}

@Composable
private fun UsualRangeSection(metric: FixedMetric, report: WeeklyReport.Ready) {
    val range = report.usualRange(metric.value)
    val text = when {
        range == null -> stringResource(Res.string.sheet_usual_missing, MIN_VOLATILITY_WEEKS)
        metric.format.format(range.min) == metric.format.format(range.max) ->
            stringResource(Res.string.sheet_usual_same, range.weeks, metric.format.valueText(range.min))
        else -> stringResource(
            Res.string.sheet_usual_range,
            range.weeks,
            metric.format.valueText(range.min),
            metric.format.valueText(range.max),
        )
    }
    SheetSection(title = stringResource(Res.string.sheet_usual_title), body = text)
}

@Composable
private fun MethodSection(metric: FixedMetric, metrics: MatchMetrics) {
    val current = metric.value(metrics)
    val formula = current?.let {
        val result = metric.format.valueText(it)
        when (metric) {
            FixedMetric.COMBAT_SCORE -> stringResource(
                metric.sheetText.formula,
                metrics.combatScore.withThousands(),
                metrics.rounds.withThousands(),
                result,
            )
            FixedMetric.KD -> stringResource(
                metric.sheetText.formula,
                metrics.kills.withThousands(),
                metrics.deaths.withThousands(),
                result,
            )
            FixedMetric.DAMAGE -> stringResource(
                metric.sheetText.formula,
                metrics.damage.withThousands(),
                metrics.rounds.withThousands(),
                result,
            )
            FixedMetric.HEADSHOT_RATE -> stringResource(
                metric.sheetText.formula,
                metrics.shots.head.withThousands(),
                metrics.shots.total.withThousands(),
                result,
            )
            FixedMetric.KDA -> stringResource(
                metric.sheetText.formula,
                metrics.kills.withThousands(),
                metrics.assists.withThousands(),
                metrics.deaths.withThousands(),
                result,
            )
        }
    }

    SheetSection(title = stringResource(Res.string.sheet_method_title), body = stringResource(metric.sheetText.method))
    Spacer(Modifier.height(10.dp))
    if (formula != null) {
        OvalitText(
            text = formula,
            modifier = Modifier
                .fillMaxWidth()
                .background(OvalitTheme.colors.fill, RoundedCornerShape(9.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            style = OvalitTheme.typography.metricS,
            color = OvalitTheme.colors.t2,
        )
    } else if (metric == FixedMetric.KD || metric == FixedMetric.KDA) {
        OvalitText(
            text = stringResource(if (metric == FixedMetric.KD) Res.string.sheet_kd_no_deaths else Res.string.sheet_kda_no_deaths),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

@Composable
private fun SheetSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OvalitText(text = title, style = OvalitTheme.typography.bodyStrong)
        OvalitText(text = body, style = OvalitTheme.typography.body, color = OvalitTheme.colors.t2)
    }
}
