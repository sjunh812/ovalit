package com.ovalit.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PlayerId
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

private val AvatarSize = 36.dp

/**
 * ㅇㅂㅇ 한 줄입니다. 부른 사람 얼굴, "민석의 초대 · 21:00", 내 답이나 참석 수, 화살표를 둡니다. 홈 카드와 친구 탭 목록이
 * 같이 쓰고, 자세한 것은 누르면 열리는 초대 화면에서 봅니다.
 *
 * 받은 초대에 아직 답하지 않았으면 둘째 줄 "답을 기다리고 있어요"를 `--accent-ink`로 칠합니다. 새 소식이 아니라 답해야 할
 * 일이라 토스처럼 점 대신 할 일을 글자로 알립니다. 회색 글자를 진하게만 하면 한눈에 안 들어옵니다.
 */
@Composable
fun PingSummaryRow(ping: Ping, me: PlayerId, now: Instant, timeZone: TimeZone, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val waiting = !ping.isHostedBy(me) && ping.memberOf(me)?.answer == PingAnswer.PENDING
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // 받은 것이면 친구, 보낸 것이면 내 얼굴이다. 방향은 제목("민석의 초대", "내 초대")이 말한다.
        PlayerAvatar(riotId = ping.host.riotId, size = AvatarSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OvalitText(
                text = pingHomeTitle(ping, me, now, timeZone),
                style = OvalitTheme.typography.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OvalitText(
                text = pingSummary(ping, me, now, timeZone),
                style = OvalitTheme.typography.caption.copy(fontWeight = if (waiting) FontWeight.Medium else FontWeight.Normal),
                color = if (waiting) colors.accentInk else colors.t3,
            )
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 16.dp)
    }
}
