package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.component.LocalOvalitToast
import com.ovalit.core.designsystem.component.OvalitToastHost
import com.ovalit.core.designsystem.component.OvalitToastState
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RefreshResultTest {

    @Test
    fun `당겨서 받은 판 수를 화면 아래 한 줄로 띄운다`() = runComposeUiTest {
        val results = RefreshResults()
        setContent { Shown(results) }
        waitForIdle()

        results.send(3)
        waitUntil { onAllNodesWithText("새 경기 3판을 불러왔어요").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `새로 끝난 경기가 없으면 없다고 띄운다`() = runComposeUiTest {
        val results = RefreshResults()
        setContent { Shown(results) }
        waitForIdle()

        results.send(0)
        waitUntil { onAllNodesWithText("새로 끝난 경기가 없어요").fetchSemanticsNodes().isNotEmpty() }
    }

    // 다른 탭에 갔다 돌아온 뒤에 띄우면 언제 당긴 결과인지 모른다
    @Test
    fun `받는 화면이 없을 때 생긴 결과는 버린다`() = runComposeUiTest {
        val results = RefreshResults()
        results.send(3)
        setContent { Shown(results) }

        waitForIdle()
        onNodeWithText("새 경기 3판을 불러왔어요").assertDoesNotExist()
    }

    @Composable
    private fun Shown(results: RefreshResults) {
        val toast = remember { OvalitToastState() }
        OvalitTheme {
            CompositionLocalProvider(LocalOvalitToast provides toast) {
                RefreshResultsEffect(results.flow)
                OvalitToastHost(toast)
            }
        }
    }
}
