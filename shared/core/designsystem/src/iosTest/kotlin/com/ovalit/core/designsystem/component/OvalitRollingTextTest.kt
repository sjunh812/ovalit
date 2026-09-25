package com.ovalit.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OvalitRollingTextTest {

    // 굴러가는 동안에는 두 숫자가 겹쳐 그려진다. 다 바뀐 뒤에 옛 숫자가 남아 있으면 칸에 숫자가 두 개 뜬다.
    @Test
    fun `숫자가 바뀌면 다 굴러간 뒤에는 새 숫자만 남는다`() = runComposeUiTest {
        var text by mutableStateOf("188")
        setContent { OvalitTheme { OvalitRollingText(text = text) } }

        text = "191"
        waitForIdle()

        onNodeWithText("191").assertExists()
        onNodeWithText("188").assertDoesNotExist()
    }
}
