package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.theme.OvalitTheme

/** @property selectedIcon 고른 탭에 쓰는 채운 아이콘입니다. */
data class OvalitTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/** 시스템 내비게이션 바 높이만큼 아래를 알아서 띄웁니다. 밖에서 여백을 더 주지 않습니다. */
@Composable
fun OvalitTabBar(
    tabs: List<OvalitTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors

    Column(modifier = modifier.fillMaxWidth().background(colors.bg)) {
        OvalitDivider(color = colors.lineWeak)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(TabBarHeight)
                .selectableGroup(),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(TabBarHeight)
                        .selectable(
                            selected = selected,
                            interactionSource = null,
                            indication = ripple(bounded = false, radius = 36.dp, color = colors.t2),
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OvalitIcon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = if (selected) colors.t1 else colors.t3,
                    )
                    OvalitText(
                        text = tab.label,
                        style = OvalitTheme.typography.caption.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) colors.t1 else colors.t3,
                    )
                }
            }
        }
    }
}

private val TabBarHeight = 60.dp
