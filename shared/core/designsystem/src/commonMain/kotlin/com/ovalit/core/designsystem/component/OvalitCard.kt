package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

val OvalitCardShape: Shape = RoundedCornerShape(20.dp)

/** 화면 가장자리와 카드 사이, 카드와 카드 사이입니다. */
val OvalitCardGap = 12.dp

private val CardVerticalPadding = 20.dp

/**
 * 화면의 큰 묶음 하나를 담는 면입니다. 홈, 내 프로필, S5, 친구 탭, 설정이 묶음마다 하나씩 씁니다.
 *
 * 카드는 양옆 안쪽 여백이 없으니 안의 내용이 화면에 바로 둘 때처럼 `OvalitSpacing.gutter`를 둡니다. 카드 안에서 면을 칠하는
 * 칸은 `--fill`을 씁니다. 다크에서 `--raised`는 카드와 같은 색이라 안 보입니다.
 *
 * @param onClick 있으면 카드 전체가 눌립니다.
 * @param bottomPadding 마지막 줄이 제 위아래 여백을 가진 목록이면 줄입니다. 안 줄이면 두 여백이 겹쳐 아래만 넓어 보입니다.
 */
@Composable
fun OvalitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    bottomPadding: Dp = CardVerticalPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = OvalitCardGap)
            .fillMaxWidth()
            // 면까지 같이 줄도록 누름 효과를 면보다 앞에 단다
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = pressIndication(OvalitCardShape),
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .clip(OvalitCardShape)
            .background(OvalitTheme.colors.card)
            .padding(top = CardVerticalPadding, bottom = bottomPadding),
        content = content,
    )
}
