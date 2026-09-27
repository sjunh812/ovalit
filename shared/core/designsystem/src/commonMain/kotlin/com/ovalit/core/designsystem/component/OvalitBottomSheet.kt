package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvalitBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    body: String? = null,
    titleNote: String? = null,
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
                .verticalScroll(rememberScrollState())
                .padding(top = OvalitSpacing.lg, bottom = OvalitSpacing.lg),
            horizontalAlignment = Alignment.Start,
        ) {
            // 제목이 길면 정식 약어를 다음 줄로 넘긴다. 한 줄에 우겨 넣으면 약어가 글자 단위로 꺾인다.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.sm)) {
                OvalitText(text = title, modifier = Modifier.alignByBaseline(), style = OvalitTheme.typography.titleM)
                if (titleNote != null) {
                    OvalitText(
                        text = titleNote,
                        modifier = Modifier.alignByBaseline(),
                        style = OvalitTheme.typography.label,
                        color = colors.t3,
                    )
                }
            }
            if (body != null) {
                Spacer(Modifier.height(OvalitSpacing.sm))
                OvalitText(text = body, style = OvalitTheme.typography.body, color = colors.t2)
            }
            Spacer(Modifier.height(OvalitSpacing.lg))
            content()
        }
    }
}
