package com.ovalit.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.DayRecord
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.record_wins_losses
import com.ovalit.core.ui.resources.record_wins_losses_draws
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * 경기 목록의 날짜 머리입니다.
 * 내 경기 목록(S2)과 친구 경기 목록이 같은 모양으로 씁니다.
 *
 * 그날 승패는 날짜 바로 뒤에 붙입니다.
 * 오른쪽 끝에 두면 날짜를 읽은 눈이 바로 경기 줄로 내려가 승패를 놓칩니다.
 * 한 줄에 안 들어가면 날짜를 꺾지 않고 승패를 다음 줄로 내립니다.
 *
 * @param record 그날 승패입니다(`dayRecord`). 승패가 갈린 판이 없으면 `null`이라 날짜만 둡니다.
 */
@Composable
fun MatchDayHeader(date: LocalDate, today: LocalDate, record: DayRecord?, modifier: Modifier = Modifier) {
    val style = OvalitTheme.typography.label
    val colors = OvalitTheme.colors
    val recordText = record?.let { recordText(it) }
    SeparatedRow(
        items = listOfNotNull<@Composable () -> Unit>(
            { OvalitText(text = dayLabel(date, today), style = style, color = colors.t3) },
            recordText?.let { { OvalitText(text = it, style = style, color = colors.t2, maxLines = 1) } },
        ),
        separator = { SeparatorDot(style, colors.t4) },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.screen, end = OvalitSpacing.screen, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs)
            .semantics(mergeDescendants = true) { heading() },
        alignBaseline = true,
    )
}

@Composable
private fun recordText(record: DayRecord): String = if (record.draws > 0) {
    stringResource(Res.string.record_wins_losses_draws, record.wins, record.losses, record.draws)
} else {
    stringResource(Res.string.record_wins_losses, record.wins, record.losses)
}
