package com.ovalit.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private val HeaderHeight = 56.dp

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
