package com.ovalit.feature.onboarding.importing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Focus
import com.ovalit.core.ui.NotificationPermission
import com.ovalit.feature.onboarding.OnboardingPreviewData
import com.ovalit.feature.onboarding.consent.ConsentScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OnboardingScreensTest {

    // 신청서에 앱이 비밀번호를 만지지 않는다고 적었다. 연동 전에 그걸 알린다.
    @Test
    fun `연동 동의는 공개와 해제와 비밀번호를 알리고 계속하기로 넘어간다`() = runComposeUiTest {
        var continued = false
        setContent { OvalitTheme { ConsentScreen(onBack = {}, onContinue = { continued = true }) } }

        onNodeWithText("서로 수락한 친구에게 전적이 공개돼요").assertExists()
        onNodeWithText("언제든 해제할 수 있어요").assertExists()
        onNodeWithText("이 앱은 비밀번호를 받지도, 저장하지도 않아요", substring = true).assertExists()
        onNodeWithText("Riot 계정으로 계속하기").performClick()

        assertTrue(continued)
    }

    @Test
    fun `불러오는 동안에는 리포트로 넘어갈 수 없고 받은 수를 보여준다`() = runComposeUiTest {
        setContent { OvalitTheme { ImportScreen(OnboardingPreviewData.loading, {}, {}) } }

        onNodeWithText("37 / 50").assertExists()
        onNodeWithContentDescription("50경기 중 37경기를 불러왔어요").assertExists()
        onNodeWithText("리포트 보기").assertDoesNotExist()
    }

    // 들어오자마자 시스템 창으로 묻지 않는다. 알림을 보낼 수 없으면 보내겠다고 적지 않고 직접 켜는 버튼을 둔다.
    @Test
    fun `알림 권한이 없으면 알리겠다고 적지 않고 알림 받기 버튼을 둔다`() = runComposeUiTest {
        var requested = 0
        val permission = NotificationPermission(missing = true) { requested++ }
        setContent { OvalitTheme { ImportScreen(OnboardingPreviewData.loading, {}, {}, notifications = permission) } }

        onNodeWithText("알림을 보내드릴게요", substring = true).assertDoesNotExist()
        onNodeWithText("앱을 닫아도 계속 불러와요.").assertExists()
        onNodeWithText("다 불러오면 알림 받기").performClick()

        assertEquals(1, requested)
    }

    @Test
    fun `알림을 보낼 수 있으면 다 불러오면 알린다고 적는다`() = runComposeUiTest {
        setContent { OvalitTheme { ImportScreen(OnboardingPreviewData.loading, {}, {}, notifications = NotificationPermission.NotNeeded) } }

        onNodeWithText("앱을 닫아도 계속 불러와요. 다 불러오면 알림을 보내드릴게요.").assertExists()
        onNodeWithText("다 불러오면 알림 받기").assertDoesNotExist()
    }

    // 설정에서 분석 완료 알림을 끈 건 사용자가 고른 것이라 켜자고 하지 않는다
    @Test
    fun `분석 완료 알림을 껐으면 알리겠다고 적지 않고 켜자고 하지도 않는다`() = runComposeUiTest {
        val state = OnboardingPreviewData.loading.copy(notifyWhenDone = false)
        setContent { OvalitTheme { ImportScreen(state, {}, {}, notifications = NotificationPermission(missing = true) {}) } }

        onNodeWithText("앱을 닫아도 계속 불러와요.").assertExists()
        onNodeWithText("다 불러오면 알림 받기").assertDoesNotExist()
    }

    @Test
    fun `다 받으면 리포트로 넘어간다`() = runComposeUiTest {
        var opened = false
        setContent { OvalitTheme { ImportScreen(OnboardingPreviewData.done, {}, { opened = true }) } }

        onNodeWithText("50경기를 다 불러왔어요").assertExists()
        onNodeWithText("리포트 보기").performClick()

        assertTrue(opened)
    }

    @Test
    fun `관심사를 고르면 저장하고 고른 칸이 선택된다`() = runComposeUiTest {
        var chosen: Focus? = null
        setContent { OvalitTheme { ImportScreen(OnboardingPreviewData.done, { chosen = it }, {}) } }

        onNodeWithText("라운드 운영 다듬기", substring = true).assertIsSelected()
        onNodeWithText("에임 올리기", substring = true).performClick()

        assertEquals(Focus.AIM, chosen)
    }
}
