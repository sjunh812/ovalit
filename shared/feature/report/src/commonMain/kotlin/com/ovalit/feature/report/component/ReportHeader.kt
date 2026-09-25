package com.ovalit.feature.report.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitChip
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.resources.app_name
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.feature.report.label
import com.ovalit.feature.report.periodLabel
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.period_date_range
import com.ovalit.feature.report.resources.period_matches
import com.ovalit.feature.report.resources.period_no_matches_this_week
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import com.ovalit.core.designsystem.resources.Res as DesignSystemRes

@Composable
internal fun ReportTopBar(modifier: Modifier = Modifier) {
    OvalitText(
        text = stringResource(DesignSystemRes.string.app_name),
        modifier = modifier.padding(horizontal = OvalitSpacing.gutter),
        style = OvalitTheme.typography.titleM,
    )
}

@Composable
internal fun QueueChips(
    selected: QueueFilter,
    onSelect: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OvalitSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        QueueFilter.entries.forEach { filter ->
            OvalitChip(
                text = stringResource(filter.label),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
internal fun PeriodHeader(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val period = report.period

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = periodLabel(period),
            titleStyle = OvalitTheme.typography.titleL,
            caption = periodCaption(report),
        )
        if (!period.includesThisWeek) {
            Spacer(Modifier.height(2.dp))
            OvalitText(
                text = stringResource(Res.string.period_no_matches_this_week),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
            )
        }
    }
}

@Composable
private fun periodCaption(report: WeeklyReport.Ready): String {
    val first = report.period.firstDay
    val last = report.period.lastDay
    val parts = listOfNotNull(
        report.mainRole?.let { stringResource(it.label) },
        stringResource(Res.string.period_date_range, first.month.number, first.day, last.month.number, last.day),
        stringResource(Res.string.period_matches, report.metrics.matches),
    )
    return parts.joinToString(SEPARATOR)
}
