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
import com.ovalit.feature.report.resources.record_recent
import com.ovalit.feature.report.resources.record_value
import org.jetbrains.compose.resources.stringResource

private val MaxCell = 14.dp
private val MaxCellGap = 3.dp

/** 칸은 가장 최근 경기부터 이만큼만 둡니다. 한 주에 수십 판을 뛰면 칸이 1dp 아래로 줄어 바코드처럼 보였습니다. */
internal const val MAX_RECORD_CELLS = 20

/**
 * 기간 경기의 승패입니다. 오래된 경기부터 한 칸씩 이긴 판은 `--pos`, 진 판은 `--neg`, 비긴 판은 `--bar`로 칠하고 옆에
 * "4승 2패 · 67%"를 적습니다. S0-4에서 받은 경기를 채우는 칸과 같은 색 규칙입니다. 승패가 난 경기가 없으면 두지 않습니다.
 *
 * 칸은 가장 최근 [MAX_RECORD_CELLS]경기까지만 두고, 넘치면 칸 앞에 "최근 20경기"를 적습니다. 승패 글자는 기간 전체를 셉니다.
 */
@Composable
internal fun RecordStrip(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val results = report.results
    if (results.none { it != null }) return
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption

    val cells = results.takeLast(MAX_RECORD_CELLS)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cells.size < results.size) {
            OvalitText(text = stringResource(Res.string.record_recent, cells.size), style = caption, color = colors.t3)
            Spacer(Modifier.width(OvalitSpacing.sm))
        }
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            // 좁은 화면이나 큰 글씨에서도 칸이 줄 밖으로 넘치지 않게 칸과 간격을 같이 줄인다
            val slot = maxWidth / cells.size
            val gap = minOf(MaxCellGap, slot / 4)
            val cell = minOf(MaxCell, slot - gap)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                cells.forEach { won ->
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
