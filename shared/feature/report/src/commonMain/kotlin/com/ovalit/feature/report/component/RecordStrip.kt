package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.currentMaxWidth
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.rememberWidestWidth
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.record_description
import com.ovalit.feature.report.resources.record_description_draws
import com.ovalit.feature.report.resources.record_losses
import com.ovalit.feature.report.resources.record_losses_draws
import com.ovalit.feature.report.resources.record_wins
import org.jetbrains.compose.resources.stringResource

private val RecordBarHeight = 6.dp
private val RecordBarGap = 2.dp
private val RecordRateGap = OvalitSpacing.md

// 막대가 이보다 짧아지면 비율이 안 읽혀서 글자를 막대 밑 줄로 내린다
private val MinRecordBarWidth = 48.dp

/**
 * 기간 경기의 승패를 비율 막대 하나와 양 끝 "6승", "2패", 승률 "75%"로 적습니다.
 * 승패가 난 경기가 없으면 두지 않습니다.
 *
 * 색은 막대에만 둡니다(사용자 결정).
 * 막대가 이긴 비율을 이미 색으로 보여서 양 끝 글자는 회색이고 승률은 구간 색 없이 크고 굵게만 적습니다.
 * 주황 승률을 빨간 "2패" 옆에 두었더니 패배 색처럼 읽혔다.
 * 비율이라 판 수와 기간에 상관없이 모양이 같습니다.
 * 하루에 여러 판을 하거나 기간이 4주로 넓어져도 흐트러지지 않습니다.
 */
@Composable
internal fun RecordStrip(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val wins = report.wins
    val losses = report.losses
    if (wins + losses == 0) return
    val draws = report.results.count { it == null }
    val colors = OvalitTheme.colors
    val side = OvalitTheme.typography.label
    val rateStyle = OvalitTheme.typography.body.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

    val winsText = stringResource(Res.string.record_wins, wins)
    val lossesText = if (draws > 0) {
        stringResource(Res.string.record_losses_draws, losses, draws)
    } else {
        stringResource(Res.string.record_losses, losses)
    }
    val rate = percentText(report.winRate)
    // 화면 읽기 프로그램이 막대와 글자 셋을 따로 읽지 않게 한 줄로 묶는다.
    // 비긴 판도 화면처럼 진 판 뒤에 읽는다.
    val description = if (draws > 0) {
        stringResource(Res.string.record_description_draws, wins, losses, draws, rate)
    } else {
        stringResource(Res.string.record_description, wins, losses, rate)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val textWidth = rememberWidestWidth(listOf(winsText), side) + rememberWidestWidth(listOf(lossesText), side) +
            rememberWidestWidth(listOf(rate), rateStyle) + OvalitSpacing.sm * 2 + RecordRateGap
        if (currentMaxWidth - textWidth < MinRecordBarWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs)) {
                Bar(wins = wins, draws = draws, losses = losses, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OvalitText(text = winsText, style = side, color = colors.t2, maxLines = 1)
                    Spacer(Modifier.width(OvalitSpacing.sm))
                    OvalitText(text = lossesText, style = side, color = colors.t2, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    OvalitText(text = rate, style = rateStyle, color = colors.t1, maxLines = 1)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OvalitText(text = winsText, style = side, color = colors.t2, maxLines = 1)
                Spacer(Modifier.width(OvalitSpacing.sm))
                Bar(wins = wins, draws = draws, losses = losses, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(OvalitSpacing.sm))
                OvalitText(text = lossesText, style = side, color = colors.t2, maxLines = 1)
                Spacer(Modifier.width(RecordRateGap))
                OvalitText(text = rate, style = rateStyle, color = colors.t1, maxLines = 1)
            }
        }
    }
}

// 이긴 판, 비긴 판, 진 판 순으로 판 수만큼 나눠 칠한다.
// 양 끝만 둥글고 사이는 2dp 틈으로 가른다.
@Composable
private fun Bar(wins: Int, draws: Int, losses: Int, modifier: Modifier) {
    val colors = OvalitTheme.colors
    Row(
        modifier = modifier
            .height(RecordBarHeight)
            .clip(RoundedCornerShape(RecordBarHeight / 2)),
        horizontalArrangement = Arrangement.spacedBy(RecordBarGap),
    ) {
        listOf(wins to colors.pos, draws to colors.bar, losses to colors.neg).forEach { (count, color) ->
            if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(color))
        }
    }
}
