package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 아래에서 올라오는 시트입니다. 고르기와 되묻기를 다 여기서 합니다. 화면 가운데 뜨는 대화상자보다
 * 손가락이 닿기 쉽고, 무엇을 고르는지 제목이 위에 크게 남습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvalitBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    body: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OvalitTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = if (colors.isDark) colors.raised else colors.bg,
        contentColor = colors.t1,
        scrimColor = Color.Black.copy(alpha = if (colors.isDark) 0.6f else 0.32f),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp)) {
                Box(Modifier.size(width = 34.dp, height = 4.dp).background(colors.t5, CircleShape))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OvalitSpacing.xl)
                .padding(top = OvalitSpacing.lg, bottom = OvalitSpacing.lg),
            horizontalAlignment = Alignment.Start,
        ) {
            OvalitText(text = title, style = OvalitTheme.typography.titleM)
            if (body != null) {
                Spacer(Modifier.height(OvalitSpacing.sm))
                OvalitText(text = body, style = OvalitTheme.typography.body, color = colors.t2)
            }
            Spacer(Modifier.height(OvalitSpacing.lg))
            content()
        }
    }
}
