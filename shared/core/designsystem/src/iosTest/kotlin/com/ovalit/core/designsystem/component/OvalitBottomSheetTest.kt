package com.ovalit.core.designsystem.component

import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OvalitBottomSheetTest {

    @Test
    fun `제목과 정식 약어는 글자 기준선을 맞춘다`() = runComposeUiTest {
        setContent { OvalitTheme { OvalitBottomSheet(title = "전투점수", titleNote = "ACS", onDismiss = {}) {} } }

        val title = onNodeWithText("전투점수")
        val note = onNodeWithText("ACS")
        val titleBaseline = title.getUnclippedBoundsInRoot().top + title.getAlignmentLinePosition(FirstBaseline)
        val noteBaseline = note.getUnclippedBoundsInRoot().top + note.getAlignmentLinePosition(FirstBaseline)
        assertTrue(abs((titleBaseline - noteBaseline).value) < 1f, "기준선이 $titleBaseline, $noteBaseline 로 어긋났다")
    }

    // 제목이 한 줄을 다 쓰면 약어가 남은 자리에 끼어 한 글자씩 꺾인다.
    // 그럴 때는 약어를 다음 줄로 넘긴다.
    @Test
    fun `제목이 길면 정식 약어를 다음 줄로 넘기고 꺾지 않는다`() = runComposeUiTest {
        val long = "아주 긴 제목이 들어와서 한 줄을 넘기고 두 줄까지 채우는 경우를 만들어 보려고 길게 적은 제목"
        setContent { OvalitTheme { OvalitBottomSheet(title = long, titleNote = "KAST", onDismiss = {}) {} } }

        val title = onNodeWithText(long).getUnclippedBoundsInRoot()
        val note = onNodeWithText("KAST").getUnclippedBoundsInRoot()
        assertTrue(note.top >= title.bottom, "약어가 제목 옆에 끼었다")
        assertTrue(note.bottom - note.top < 40.dp, "약어가 여러 줄로 꺾였다: 높이 ${note.bottom - note.top}")
    }
}
