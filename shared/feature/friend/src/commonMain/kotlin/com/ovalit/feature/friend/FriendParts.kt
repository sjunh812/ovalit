package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.PlayerAvatar

@Composable
internal fun Avatar(riotId: String, size: Dp, modifier: Modifier = Modifier) {
    PlayerAvatar(riotId = riotId, modifier = modifier.size(size))
}

/**
 * 줄 안에 들어가는 작은 버튼입니다. 보이는 높이는 34dp이고 눌리는 영역은 44dp입니다. 누름 효과는 눌리는
 * 영역이 아니라 보이는 버튼에 답니다.
 */
@Composable
internal fun SmallButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val shape = RoundedCornerShape(9.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(interactionSource = interactionSource, indication = null, role = SemanticsRole.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .indication(interactionSource, pressIndication(shape, if (filled) colors.onAccent else colors.t2))
                .clip(shape)
                .then(
                    if (filled) Modifier.background(colors.accent, shape) else Modifier.border(1.dp, colors.line, shape),
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

