package com.ovalit.core.designsystem.component

import androidx.compose.foundation.BorderStroke
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

/** [OvalitPrimaryButton]보다 한 단계 낮은 버튼입니다. */
@Composable
fun OvalitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = OvalitTheme.colors.t1,
) {
    OvalitButtonSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = true,
        background = Color.Transparent,
        pressColor = contentColor,
        border = BorderStroke(1.dp, OvalitTheme.colors.line),
    ) {
        OvalitText(text = text, style = OvalitTheme.typography.bodyStrong, color = contentColor)
    }
}

/**
 * 되돌릴 수 없는 동작(연동 해제)의 버튼입니다.
 * 옅은 `--neg` 면에 `--neg` 글자를 올립니다.
 * 카드와 같은 흰 면에 두면 묶음 하나처럼 보이고, 회색 바탕에 테두리만 두르면 면이 없어 묻힙니다.
 */
@Composable
fun OvalitDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val neg = OvalitTheme.colors.neg
    OvalitButtonSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = true,
        background = neg.copy(alpha = DangerContainerAlpha),
        pressColor = neg,
    ) {
        OvalitText(text = text, style = OvalitTheme.typography.bodyStrong, color = neg)
    }
}

private const val DangerContainerAlpha = 0.1f

// `clickable`이 안의 글자를 묶어 읽어서 Role.Button만 달면 화면 읽기 프로그램이 "○○, 버튼"으로 한 번에 읽는다
@Composable
private fun OvalitButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    pressColor: Color,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            // 누르면 면과 테두리까지 같이 줄어야 해서 둘을 그리기 전에 단다
            .clickable(
                interactionSource = interactionSource,
                indication = pressIndication(ButtonShape, pressColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .clip(ButtonShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, ButtonShape) else Modifier)
            .padding(horizontal = OvalitSpacing.lg, vertical = OvalitSpacing.md),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
