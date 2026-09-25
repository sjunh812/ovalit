package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonMinHeight = 52.dp

@Composable
fun OvalitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colors = OvalitTheme.colors
    val content = if (enabled) colors.onAccent else colors.t4

    OvalitButtonSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        background = if (enabled) colors.accent else colors.fill,
        pressColor = colors.onAccent,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.sm)) {
            OvalitText(text = text, style = OvalitTheme.typography.titleM, color = content)
            if (trailingIcon != null) OvalitIcon(trailingIcon, contentDescription = null, tint = content, size = 16.dp)
        }
    }
}

@Composable
fun OvalitTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OvalitTheme.colors

    OvalitButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = Color.Transparent,
        pressColor = colors.t2,
    ) {
        OvalitText(
            text = text,
            style = OvalitTheme.typography.label,
            color = if (enabled) colors.t2 else colors.t4,
        )
    }
}

/** 연동 해제처럼 되돌릴 수 없는 동작에 씁니다. */
@Composable
fun OvalitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = OvalitTheme.colors.t1,
) {
    OvalitButtonSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OvalitTheme.colors.line, ButtonShape),
        enabled = true,
        background = Color.Transparent,
        pressColor = contentColor,
    ) {
        OvalitText(text = text, style = OvalitTheme.typography.bodyStrong, color = contentColor)
    }
}

/**
 * 버튼이 공통으로 갖는 것들을 한곳에 모읍니다. 눌리는 영역, 최소 높이, 그리고 화면 낭독기에
 * 필요한 두 가지입니다.
 *
 * `clickable`만 걸면 낭독기가 누를 수 있는 칸과 그 안의 글자를 따로 읽습니다.
 * `mergeDescendants`로 묶고 [Role.Button]을 달아야 "○○, 버튼"으로 한 번에 읽힙니다.
 */
@Composable
private fun OvalitButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    pressColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .semantics(mergeDescendants = true) {}
            // 누르면 버튼 면까지 같이 줄어야 해서 면을 칠하기 전에 단다
            .clickable(
                interactionSource = interactionSource,
                indication = pressIndication(ButtonShape, pressColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .clip(ButtonShape)
            .background(background)
            .padding(horizontal = OvalitSpacing.lg, vertical = OvalitSpacing.md),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
