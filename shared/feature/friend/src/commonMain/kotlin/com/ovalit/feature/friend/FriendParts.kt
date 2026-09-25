package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Role
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.role_controller
import com.ovalit.feature.friend.resources.role_duelist
import com.ovalit.feature.friend.resources.role_initiator
import com.ovalit.feature.friend.resources.role_sentinel
import org.jetbrains.compose.resources.StringResource

// 플레이어 카드가 붙기 전까지 Riot ID 첫 글자로 자리를 잡는다
@Composable
internal fun Avatar(riotId: String, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val colors = OvalitTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.fill, CircleShape)
            .then(if (ring) Modifier.border(3.dp, colors.bg, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        OvalitText(
            text = riotId.take(1),
            style = if (size > 48.dp) OvalitTheme.typography.titleM else OvalitTheme.typography.bodyStrong,
            color = colors.t2,
        )
    }
}

/** 줄 안에 들어가는 작은 버튼입니다. 보이는 높이는 34dp이고 눌리는 영역은 44dp입니다. */
@Composable
internal fun SmallButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(role = SemanticsRole.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 34.dp)
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

internal val Role.label: StringResource
    get() = when (this) {
        Role.DUELIST -> Res.string.role_duelist
        Role.INITIATOR -> Res.string.role_initiator
        Role.CONTROLLER -> Res.string.role_controller
        Role.SENTINEL -> Res.string.role_sentinel
    }
