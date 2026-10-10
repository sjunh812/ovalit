package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

@Composable
fun OvalitBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    body: String? = null,
    titleNote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurface(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 양옆 여백을 스크롤 안에 둬야 밖으로 넓힌 누름 면이 스크롤 경계에서 잘리지 않는다
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OvalitSheetGutter, vertical = OvalitSpacing.lg),
            horizontalAlignment = Alignment.Start,
        ) {
            SheetHeader(title = title, titleNote = titleNote, body = body)
            content()
        }
    }
}

/**
 * 줄이 수백 개일 수 있는 목록을 담는 바텀시트입니다.
 * 머리 모양은 [OvalitBottomSheet]와 같고, 줄은 화면에 보이는 것만 그립니다.
 * 제목과 설명도 목록과 같이 스크롤됩니다.
 * 양옆 여백은 목록 안쪽 여백이라 넓힌 누름 면이 잘리지 않습니다.
 */
@Composable
fun OvalitLazyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    body: String? = null,
    content: LazyListScope.() -> Unit,
) {
    SheetSurface(onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = OvalitSheetGutter, vertical = OvalitSpacing.lg),
        ) {
            item(key = SHEET_HEADER_KEY) { SheetHeader(title = title, titleNote = null, body = body) }
            content()
        }
    }
}

val OvalitSheetGutter = OvalitSpacing.xl

/**
 * 바텀시트 양옆 여백을 넘어 시트 끝까지 넓힙니다.
 * 옆으로 미는 줄에 달고 같은 여백을 `contentPadding`으로 주면 첫 칸은 본문 선에 맞고, 밀면 칸이 시트 끝까지 이어집니다.
 * 여백 안에서 끝나면 칸이 본문 선에서 잘려 보입니다.
 */
fun Modifier.ovalitSheetFullWidth(): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val gutter = OvalitSheetGutter.roundToPx()
    val width = constraints.maxWidth + gutter * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-gutter, 0) }
}

private const val SHEET_HEADER_KEY = "sheet-header"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetSurface(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
        content = content,
    )
}

@Composable
private fun SheetHeader(title: String, titleNote: String?, body: String?) {
    val colors = OvalitTheme.colors
    Column {
        // 제목이 길면 정식 약어를 다음 줄로 넘긴다.
        // 한 줄에 우겨 넣으면 약어가 글자 단위로 꺾인다.
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
    }
}
