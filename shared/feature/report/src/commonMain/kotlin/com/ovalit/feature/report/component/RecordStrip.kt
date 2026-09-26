package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.winRateColor
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.record_value
import org.jetbrains.compose.resources.stringResource

private val MaxCell = 14.dp
private val MaxCellGap = 3.dp

/**
 * 기간 경기의 승패입니다. 오래된 경기부터 한 칸씩 이긴 판은 `--pos`, 진 판은 `--neg`, 비긴 판은 `--bar`로 칠하고 옆에
 * "4승 2패 · 67%"를 적습니다. S0-4에서 받은 경기를 채우는 칸과 같은 색 규칙입니다. 승패가 난 경기가 없으면 두지 않습니다.
 */
@Composable
internal fun RecordStrip(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val results = report.results
    if (results.none { it != null }) return
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            // 기간을 넓히면 경기가 많아진다. 칸이 줄 밖으로 넘치지 않게 칸과 간격을 같이 줄인다.
            val slot = maxWidth / results.size
            val gap = minOf(MaxCellGap, slot / 4)
            val cell = minOf(MaxCell, slot - gap)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                results.forEach { won ->
                    val color = when (won) {
                        true -> colors.pos
                        false -> colors.neg
                        null -> colors.bar
                    }
                    Box(Modifier.size(width = cell, height = MaxCell).background(color, RoundedCornerShape(3.dp)))
                }
            }
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(text = stringResource(Res.string.record_value, report.wins, report.losses), style = caption, color = colors.t2)
        OvalitText(text = SEPARATOR, style = caption, color = colors.t3)
        OvalitText(
            text = percentText(report.winRate),
            style = caption.copy(fontWeight = FontWeight.SemiBold),
            color = winRateColor(report.winRate),
        )
    }
}
