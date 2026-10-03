package com.ovalit.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Focus
import com.ovalit.core.model.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun `불러오는 중에는 아무 줄도 그리지 않는다`() = runComposeUiTest {
        setContent { Settings(SettingsUiState.Loading) }

        onNodeWithText("전적 공개").assertDoesNotExist()
    }

    // CLAUDE.md: 비공식 고지를 인트로와 S4 설정 하단에 둔다
    @Test
    fun `비공식 고지와 버전을 보여준다`() = runComposeUiTest {
        setContent { Settings() }

        onNodeWithText("isn't endorsed by Riot Games", substring = true).assertExists()
        onNodeWithText("버전 1.0.0").assertExists()
    }

    @Test
    fun `연동한 계정과 연동한 날을 보여준다`() = runComposeUiTest {
        setContent { Settings() }

        onNodeWithText("오발러#KR1").assertExists()
        onNodeWithText("Riot 계정 연동됨 · 9월 19일").assertExists()
    }

    @Test
    fun `전적 공개는 켜져 있고 누르면 끈다`() = runComposeUiTest {
        var statsPublic: Boolean? = null
        setContent { Settings(actions = SettingsActions(onStatsPublicChange = { statsPublic = it })) }

        onNodeWithText("전적 공개").assertIsOn().performClick()

        assertEquals(false, statsPublic)
    }

    @Test
    fun `꺼 둔 알림은 꺼진 채로 그린다`() = runComposeUiTest {
        setContent { Settings(SettingsPreviewData.allOff) }

        onNodeWithText("분석 완료").assertIsOff()
    }

    // 서버가 월요일 9시에 FCM 토픽으로 보내게 되어 목업의 줄을 되살렸다(2026-10-03)
    @Test
    fun `알림에는 분석 완료 주간 리포트 게임 초대 스위치를 둔다`() = runComposeUiTest {
        var weekly: Boolean? = null
        var ping: Boolean? = null
        setContent {
            Settings(actions = SettingsActions(onNotifyWeeklyReportChange = { weekly = it }, onNotifyPingChange = { ping = it }))
        }

        onNodeWithText("분석 완료", substring = true).assertExists()
        onNodeWithText("주간 리포트", substring = true).performScrollTo().performClick()
        onNodeWithText("게임 초대", substring = true).performScrollTo().performClick()

        assertEquals(false, weekly)
        assertEquals(false, ping)
    }

    // 누를 곳이 없는 화살표는 두지 않는다
    @Test
    fun `저장된 경기 줄은 숫자만 보여주고 누를 수 없다`() = runComposeUiTest {
        setContent { Settings() }

        onNodeWithText("127경기").assertExists()
        onNodeWithText("저장된 경기").assertHasNoClickAction()
        onNodeWithText("테마").assertHasClickAction()
    }

    @Test
    fun `테마를 고르면 바로 바꾸고 시트를 닫는다`() = runComposeUiTest {
        var theme: ThemePreference? = null
        setContent { Settings(actions = SettingsActions(onThemeChange = { theme = it })) }

        onNodeWithText("테마").performClick()
        onNodeWithText("라이트").performClick()

        assertEquals(ThemePreference.LIGHT, theme)
        onNodeWithText("라이트").assertDoesNotExist()
    }

    // S0-4가 "나중에 설정에서 바꿔도 돼요"라고 약속한다
    @Test
    fun `관심사는 설정에서 다시 고를 수 있다`() = runComposeUiTest {
        var focus: Focus? = null
        setContent { Settings(actions = SettingsActions(onFocusChange = { focus = it })) }

        onNodeWithText("관심사").performClick()
        onNodeWithText("기복 줄이기").performClick()

        assertEquals(Focus.CONSISTENCY, focus)
    }

    // S0-4처럼 무엇을 먼저 보는지 적는다. 이름만으로는 어떤 지표가 올라오는지 모른다.
    @Test
    fun `관심사 시트는 고르면 먼저 볼 지표를 적는다`() = runComposeUiTest {
        setContent { Settings() }

        onNodeWithText("관심사").performClick()

        onNodeWithText("첫 교전 승률, 멀티킬 라운드", useUnmergedTree = true).assertExists()
        onNodeWithText("관여율, 생존율", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `연동 해제는 확인을 받은 뒤에 한다`() = runComposeUiTest {
        var unlinked = false
        setContent { Settings(actions = SettingsActions(onUnlink = { unlinked = true })) }

        onNodeWithText("Riot 계정 연동 해제").performScrollTo().performClick()

        assertFalse(unlinked)
        onNodeWithText("Riot 계정 연동을 해제할까요?").assertExists()

        onNodeWithText("연동 해제").performClick()

        assertTrue(unlinked)
    }

    @Test
    fun `삭제 확인에서 취소하면 지우지 않는다`() = runComposeUiTest {
        var deleted: Boolean? = null
        setContent { Settings(actions = SettingsActions(onDeleteData = { deleted = true })) }

        onNodeWithText("저장된 데이터 삭제").performClick()
        onNodeWithText("127경기와 리포트가 지워져요. Riot 계정 연동은 그대로예요.").assertExists()
        onNodeWithText("취소").performClick()

        assertNull(deleted)
        onNodeWithText("저장된 경기를 지울까요?").assertDoesNotExist()
    }
}

@Composable
private fun Settings(
    uiState: SettingsUiState = SettingsPreviewData.linked,
    actions: SettingsActions = SettingsActions(),
) {
    OvalitTheme { SettingsScreen(uiState = uiState, appVersion = "1.0.0", actions = actions) }
}
