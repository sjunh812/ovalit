package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private val ChipShape = RoundedCornerShape(8.dp)

// 칩 자체는 28dp 남짓이라 손가락으로 누르기엔 작다. 눌리는 영역만 위아래로 늘린다.
private val ChipTouchHeight = 44.dp

/** 여러 개 중 하나를 고르는 칩입니다. 고른 칩만 바탕을 깔고 나머지는 글자만 둡니다. */
@Composable
fun OvalitChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .heightIn(min = ChipTouchHeight)
            .semantics(mergeDescendants = true) {}
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(ChipShape)
                .background(if (selected) colors.fill else Color.Transparent)
                .indication(interactionSource, ripple(color = colors.t2))
                .padding(horizontal = OvalitSpacing.md, vertical = 6.dp),
        ) {
            OvalitText(
                text = text,
                style = OvalitTheme.typography.label.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = if (selected) colors.t1 else colors.t2,
            )
        }
    }
}
