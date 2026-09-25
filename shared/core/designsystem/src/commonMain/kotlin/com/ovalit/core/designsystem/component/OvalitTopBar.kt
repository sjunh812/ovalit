package com.ovalit.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.resources.Res
import com.ovalit.core.designsystem.resources.back
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import org.jetbrains.compose.resources.stringResource

private val HeaderHeight = 56.dp
private val IconTouchSize = 44.dp

/**
 * 하단 탭 네 화면(홈, 경기, 친구, 설정)의 맨 위 줄입니다. 탭을 오갈 때 제목이 같은 자리에 있어야 해서 높이와 여백을
 * 여기 한곳에서 정합니다. 화면마다 따로 두면 몇 dp씩 어긋납니다.
 *
 * @param actions 오른쪽 버튼입니다. 44dp 누름 영역이 오른쪽 여백을 대신하므로 끝 여백이 왼쪽보다 좁습니다.
 */
@Composable
fun OvalitTabHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    OvalitTabHeader(modifier = modifier, actions = actions) {
        OvalitText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = OvalitTheme.typography.titleL,
            maxLines = 1,
        )
    }
}

/** 제목 자리에 글자 대신 다른 것을 둘 때 씁니다. 홈은 여기에 ㅇㅂㅇ 로고를 둡니다. */
@Composable
fun OvalitTabHeader(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = OvalitSpacing.sm)
            .heightIn(min = HeaderHeight)
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) { leading() }
        actions()
    }
}

/**
 * 뒤로 가기가 있는 화면의 맨 위 줄입니다. 탭 머리 줄과 높이가 같아서 탭에서 들어가도 제목과 버튼이 같은 높이에
 * 있습니다. 화살표는 누름 영역 안쪽에 있어 왼쪽 여백을 좁게 둡니다. 그래야 화살표가 본문 왼쪽 선과 맞습니다.
 *
 * @param title 없으면 버튼만 둡니다. 프로필처럼 제목을 본문이 대신하는 화면이 그렇습니다.
 * @param actions 오른쪽에 둘 것입니다. 아이콘은 [OvalitIconButton], 글자는 [OvalitTopBarCaption]을 씁니다.
 */
@Composable
fun OvalitBackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = OvalitSpacing.sm)
            .heightIn(min = HeaderHeight)
            .padding(horizontal = OvalitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitIconButton(OvalitIcons.Back, stringResource(Res.string.back), onBack)
        Spacer(Modifier.width(OvalitSpacing.xs))
        Box(modifier = Modifier.weight(1f)) {
            if (title != null) {
                OvalitText(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = OvalitTheme.typography.titleL,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/** 머리 줄에 두는 아이콘 버튼입니다. 누름 영역은 44dp이고 물결은 원 안에서만 퍼집니다. */
@Composable
fun OvalitIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(IconTouchSize).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        OvalitIcon(icon, contentDescription = contentDescription, tint = OvalitTheme.colors.t1)
    }
}

/** 머리 줄 오른쪽의 짧은 글자입니다. 끝을 본문 오른쪽 선에 맞추려고 아이콘 버튼보다 안쪽에 둡니다. */
@Composable
fun OvalitTopBarCaption(text: String, color: Color = OvalitTheme.colors.t3) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(end = OvalitSpacing.gutter - OvalitSpacing.sm),
        style = OvalitTheme.typography.caption,
        color = color,
        maxLines = 1,
    )
}
