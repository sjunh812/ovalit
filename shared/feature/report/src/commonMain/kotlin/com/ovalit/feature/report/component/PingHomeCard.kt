package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.ui.PingSummaryRow
import com.ovalit.feature.report.HomePing
import kotlinx.datetime.TimeZone

/**
 * 홈 맨 위의 ㅇㅂㅇ 한 줄입니다. 친구 탭 목록과 같은 줄이고, 누르면 그 초대 화면이 열려 바로 답하거나 시각을 옮깁니다.
 */
@Composable
internal fun PingHomeCard(home: HomePing, timeZone: TimeZone, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OvalitCard(modifier = modifier, onClick = onClick) {
        PingSummaryRow(
            ping = home.ping,
            me = home.me,
            now = home.now,
            timeZone = timeZone,
            modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter),
        )
    }
}
