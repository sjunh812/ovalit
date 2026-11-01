package com.ovalit.feature.report.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.profile_hint
import com.ovalit.feature.report.resources.profile_hint_close
import org.jetbrains.compose.resources.stringResource

private val ArrowWidth = 12.dp
private val ArrowHeight = 6.dp
private val BubbleShape = RoundedCornerShape(10.dp)

// 말풍선 오른쪽 끝을 아바타 오른쪽 끝보다 이만큼 밖에 둔다
private val BubbleOverhang = 4.dp

/**
 * 홈 오른쪽 위 아바타 밑에 한 번 두는 "내 프로필은 여기서 볼 수 있어요" 말풍선이고 누르면 닫힙니다.
 *
 * 화면 위에 띄우지 않고 머리 줄 밑에 끼워 넣습니다([ReportTopBar]).
 * 글자가 길어지면 두 줄로 꺾여서 폭이 좁거나 글자가 커도 다른 것과 겹치지 않습니다.
 * 말풍선만 누를 수 있고 화살표 줄을 누르면 그 밑에 겹친 아바타가 눌립니다.
 *
 * @param avatarEnd 화면 오른쪽 끝에서 아바타 오른쪽 끝까지입니다.
 * @param avatarSize 화살표를 아바타 가운데에 맞추는 데 씁니다.
 */
@Composable
internal fun ProfileHint(avatarEnd: Dp, avatarSize: Dp, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.screen, end = (avatarEnd - BubbleOverhang).coerceAtLeast(0.dp)),
        horizontalAlignment = Alignment.End,
    ) {
        Canvas(Modifier.padding(end = avatarSize / 2 + BubbleOverhang - ArrowWidth / 2).size(ArrowWidth, ArrowHeight)) {
            val arrow = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(arrow, colors.t1)
        }
        Row(
            modifier = Modifier
                .clickable(
                    onClickLabel = stringResource(Res.string.profile_hint_close),
                    role = Role.Button,
                    onClick = onDismiss,
                )
                // 말풍선을 바탕과 반대로 칠해 라이트에서는 어둡고 다크에서는 밝다
                .background(colors.t1, BubbleShape)
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OvalitText(
                text = stringResource(Res.string.profile_hint),
                style = OvalitTheme.typography.label,
                color = colors.bg,
                modifier = Modifier.weight(1f, fill = false),
            )
            OvalitIcon(OvalitIcons.Close, contentDescription = null, tint = colors.bg.copy(alpha = 0.7f), size = 14.dp)
        }
    }
}
