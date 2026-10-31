package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private val ChipShape = RoundedCornerShape(8.dp)

// 칩 자체는 28dp 남짓이라 손가락으로 누르기엔 작다.
// 눌리는 영역만 위아래로 늘린다.
private val ChipTouchHeight = 48.dp
private val ChipPadding = PaddingValues(horizontal = OvalitSpacing.md, vertical = 6.dp)

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
                .indication(interactionSource, pressIndication(ChipShape))
                .clip(ChipShape)
                .background(if (selected) colors.fill else Color.Transparent)
                .padding(ChipPadding),
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

/**
 * [OvalitSkeleton] 안에서 칩 자리를 잡는 회색 칩입니다.
 * 눌리는 높이와 보이는 칩 크기가 [OvalitChip]과 같아서 칩이 나타날 때 아래 내용이 움직이지 않습니다.
 */
@Composable
fun OvalitChipSkeleton(width: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.heightIn(min = ChipTouchHeight), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(width)
                .clip(ChipShape)
                .background(OvalitTheme.colors.fill)
                .padding(ChipPadding),
        ) {
            // 보이지 않는 글자로 칩 높이를 잡는다
            OvalitText(text = "0", modifier = Modifier.alpha(0f), style = OvalitTheme.typography.label, maxLines = 1)
        }
    }
}
