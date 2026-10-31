package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.PlayerAvatar

@Composable
internal fun Avatar(riotId: String, size: Dp, modifier: Modifier = Modifier) {
    PlayerAvatar(riotId = riotId, size = size, modifier = modifier)
}

// 줄 안에 넣는 작은 버튼이다.
// 보이는 높이는 34dp, 눌리는 영역은 48dp이고 누름 효과는 보이는 버튼에만 단다.
// 채우지 않은 버튼은 테두리 없이 옅은 면이다(OvalitWeakButton과 같다).
// 면 색과 같은 바탕(S5 머리의 --canvas)에 놓이면 [container]로 다른 면을 깐다.
@Composable
internal fun SmallButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color? = null,
    enabled: Boolean = true,
) {
    val colors = OvalitTheme.colors
    val shape = RoundedCornerShape(9.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, role = SemanticsRole.Button, onClick = onClick)
            // 보내는 중에는 흐리게 둔다
            .alpha(if (enabled) 1f else DisabledAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .indication(interactionSource, pressIndication(shape, if (filled) colors.onAccent else colors.t2))
                .clip(shape)
                .then(
                    if (filled) Modifier.background(colors.accent, shape) else Modifier.background(container ?: colors.fill, shape),
                )
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            OvalitText(
                text = text,
                style = OvalitTheme.typography.label,
                color = if (filled) colors.onAccent else colors.t1,
            )
        }
    }
}

private const val DisabledAlpha = 0.5f
