package com.ovalit.feature.report.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.profile_hint
import com.ovalit.feature.report.resources.profile_hint_close
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

private val ArrowWidth = 12.dp
private val ArrowHeight = 6.dp
private val BubbleShape = RoundedCornerShape(10.dp)

// 말풍선 오른쪽 끝을 아바타 오른쪽 끝보다 이만큼 밖에 둔다
private val BubbleOverhang = 4.dp

/**
 * 홈 오른쪽 위 아바타 밑에 한 번 띄우는 "내 프로필은 여기서 볼 수 있어요" 말풍선입니다. 누르면 닫힙니다.
 *
 * 아바타 자리를 받아 그 밑에 두므로 머리 줄과 함께 스크롤됩니다. 화면 전체를 덮지만 말풍선 밖의 터치는 밑으로 그대로 갑니다.
 *
 * @param avatar 이 칸 안에서 아바타가 차지한 자리입니다. 아직 모르면 `null`이고 그리지 않습니다.
 */
@Composable
internal fun ProfileHint(avatar: Rect?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (avatar == null) return
    val colors = OvalitTheme.colors
    // 화살표가 아바타 가운데를 가리키도록 말풍선 오른쪽 끝에서 띄우는 폭이다
    val arrowEnd = (with(LocalDensity.current) { (avatar.width / 2).toDp() } + BubbleOverhang - ArrowWidth / 2).coerceAtLeast(0.dp)

    Layout(
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(Res.string.profile_hint_close),
                    role = Role.Button,
                    onClick = onDismiss,
                ),
                horizontalAlignment = Alignment.End,
            ) {
                Canvas(Modifier.padding(end = arrowEnd).size(ArrowWidth, ArrowHeight)) {
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
                        .background(colors.t1, BubbleShape)
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 바탕과 반대로 칠해 라이트에서는 어둡고 다크에서는 밝다
                    OvalitText(text = stringResource(Res.string.profile_hint), style = OvalitTheme.typography.label, color = colors.bg)
                    OvalitIcon(OvalitIcons.Close, contentDescription = null, tint = colors.bg.copy(alpha = 0.7f), size = 14.dp)
                }
            }
        },
    ) { measurables, constraints ->
        val bubble = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val right = avatar.right + BubbleOverhang.toPx()
            bubble.place(
                x = (right - bubble.width).roundToInt().coerceAtLeast(0),
                y = (avatar.bottom + 2.dp.toPx()).roundToInt(),
            )
        }
    }
}
