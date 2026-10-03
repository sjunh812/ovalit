package com.ovalit.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 무엇을 보여 줄지 고르는 작은 버튼입니다. 지금 고른 것을 적고 옆에 아래 화살표를 둡니다. 누르면 [OvalitSheetOption]을
 * 담은 바텀시트를 엽니다. 눌리는 높이는 44dp이고 오른쪽 끝을 본문 오른쪽 선에 맞춥니다.
 */
@Composable
fun OvalitPickerButton(text: String, onClickLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = PickerHeight)
            // 오른쪽 끝을 본문 선에 맞추느라 안쪽 여백을 못 둔다. 누른 면을 양옆으로 넓혀 화살표에 붙지 않게 한다.
            .clickable(
                interactionSource = null,
                indication = pressIndication(horizontalOutset = OvalitSpacing.sm),
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(2.dp))
        OvalitIcon(OvalitIcons.ChevronDown, contentDescription = null, tint = OvalitTheme.colors.t3, size = 12.dp)
    }
}

private val PickerHeight = 44.dp

/**
 * 묶음 제목과 [OvalitPickerButton]을 한 줄에 둡니다. 줄 높이는 제목 글자에 맞추고, 버튼의 눌리는 영역(44dp)은 제목
 * 위아래로 넘치게 둡니다. 버튼 높이로 줄을 늘리면 이 묶음만 선에서 제목까지, 제목에서 내용까지가 다른 묶음보다 떠
 * 보입니다. 제목은 버튼을 뺀 폭에서 꺾입니다.
 */
@Composable
fun OvalitPickerTitle(title: @Composable () -> Unit, picker: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(title, picker), modifier = modifier) { (titles, pickers), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = pickers.first().measure(loose)
        val text = titles.first().measure(loose.copy(maxWidth = (constraints.maxWidth - button.width).coerceAtLeast(0)))
        val width = constraints.maxWidth
        layout(width, text.height) {
            text.place(0, 0)
            button.place(width - button.width, (text.height - button.height) / 2)
        }
    }
}

/**
 * 바텀시트에서 하나를 고르는 줄입니다. 고른 줄은 굵게 쓰고 오른쪽에 체크를 둡니다. 여러 줄을 `selectableGroup`으로
 * 묶어서 씁니다.
 *
 * @param caption 이름 밑에 붙는 짧은 설명입니다("타격대 기준").
 */
@Composable
fun OvalitSheetOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val colors = OvalitTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            // 글자와 체크가 시트 여백 선에 붙어 있어 누른 면을 양옆으로 넓힌다
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = pressIndication(horizontalOutset = OvalitPressOutset),
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = OvalitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OvalitText(
                text = text,
                style = if (selected) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body,
                color = if (selected) colors.t1 else colors.t2,
            )
            if (caption != null) {
                OvalitText(text = caption, style = OvalitTheme.typography.caption, color = colors.t3)
            }
        }
        if (selected) OvalitIcon(OvalitIcons.Check, contentDescription = null, tint = colors.t1)
    }
}
