package com.ovalit.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test

// Riot 개발자 포털 Core Policies가 지정한 문구다. 한 글자라도 바뀌면 키 심사에서 걸린다.
private const val REQUIRED_BOILERPLATE =
    "Ovalit isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games " +
        "or anyone officially involved in producing or managing Riot Games properties. " +
        "Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc."

@OptIn(ExperimentalTestApi::class)
class OvalitDisclaimerTest {

    @Test
    fun `비공식 고지는 개발자 포털이 지정한 문구를 그대로 띄운다`() = runComposeUiTest {
        setContent { OvalitTheme { OvalitDisclaimer() } }

        onNodeWithText(REQUIRED_BOILERPLATE).assertExists()
    }
}
