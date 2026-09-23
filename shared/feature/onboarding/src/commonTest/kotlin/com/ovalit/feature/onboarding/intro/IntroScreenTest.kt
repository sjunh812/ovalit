package com.ovalit.feature.onboarding.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IntroScreenTest {

    @Test
    fun `시작 버튼을 누르면 onStart가 한 번 불린다`() = runComposeUiTest {
        var started = 0

        setContent {
            OvalitTheme { IntroScreen(onStart = { started++ }) }
        }

        onNodeWithText("Riot 계정으로 시작하기").performClick()

        assertEquals(1, started)
    }

    // 낭독기가 "버튼"이라고 읽어야 눌러도 되는 자리인지 안다. clickable만 걸면 그냥 글자다.
    @Test
    fun `시작 버튼을 낭독기가 버튼으로 읽는다`() = runComposeUiTest {
        setContent {
            OvalitTheme { IntroScreen(onStart = {}) }
        }

        onNodeWithText("Riot 계정으로 시작하기")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    // 지우면 프로덕션 키가 회수되는 문구다. 화면에서 사라지면 테스트가 먼저 잡는다.
    @Test
    fun `비공식 고지가 화면에 있다`() = runComposeUiTest {
        setContent {
            OvalitTheme { IntroScreen(onStart = {}) }
        }

        onNodeWithText("Riot Games", substring = true).assertExists()
    }

    // 헤드라인이 늘어나면 아래 것들을 밀어낸다. 작은 기기 + 큰 글자가 가장 빡빡한 조합이다.
    @Test
    fun `작은 기기에서 글자를 키워도 시작 버튼이 화면 안에 있다`() = runComposeUiTest {
        setContent {
            val dense = Density(density = 2f, fontScale = 1.5f)
            CompositionLocalProvider(LocalDensity provides dense) {
                OvalitTheme {
                    Box(Modifier.width(320.dp).height(568.dp)) {
                        IntroScreen(onStart = {})
                    }
                }
            }
        }

        onNodeWithText("Riot 계정으로 시작하기").assertIsDisplayed()
    }
}
