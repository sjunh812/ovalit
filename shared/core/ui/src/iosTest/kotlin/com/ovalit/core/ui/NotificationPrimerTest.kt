package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NotificationPrimerTest {

    @Test
    fun `알림이 꺼져 있으면 한 번 묻고 알림 받기를 누르면 권한을 묻는다`() = runComposeUiTest {
        var requested = 0
        var seen by mutableStateOf(false)
        var primer: NotificationPrimer? = null
        setContent {
            primer = Primer(NotificationPermission(missing = true) { requested++ }, seen = seen, onSeen = { seen = true })
        }

        runOnIdle { primer!!.offer() }
        onNodeWithText("친구가 부르면 알려드릴까요?").assertExists()
        // 띄우자마자 물었다고 적는다.
        // 대답하지 않고 닫아도 다시 묻지 않는다.
        assertEquals(true, seen)

        onNodeWithText("알림 받기").performClick()
        waitForIdle()

        assertEquals(1, requested)
        onNodeWithText("친구가 부르면 알려드릴까요?").assertDoesNotExist()
    }

    @Test
    fun `나중에를 누르면 권한을 묻지 않고 닫고 다시 띄우지 않는다`() = runComposeUiTest {
        var requested = 0
        var seen by mutableStateOf(false)
        var primer: NotificationPrimer? = null
        setContent {
            primer = Primer(NotificationPermission(missing = true) { requested++ }, seen = seen, onSeen = { seen = true })
        }

        runOnIdle { primer!!.offer() }
        onNodeWithText("나중에").performClick()
        waitForIdle()
        runOnIdle { primer!!.offer() }
        waitForIdle()

        assertEquals(0, requested)
        onNodeWithText("친구가 부르면 알려드릴까요?").assertDoesNotExist()
    }

    @Test
    fun `알림을 받을 수 있거나 전에 물었으면 묻지 않는다`() = runComposeUiTest {
        var permission by mutableStateOf(NotificationPermission(missing = false) {})
        var seen by mutableStateOf(false)
        var primer: NotificationPrimer? = null
        setContent { primer = Primer(permission, seen = seen, onSeen = {}) }

        runOnIdle { primer!!.offer() }
        waitForIdle()
        onNodeWithText("친구가 부르면 알려드릴까요?").assertDoesNotExist()

        permission = NotificationPermission(missing = true) {}
        seen = true
        waitForIdle()
        runOnIdle { primer!!.offer() }
        waitForIdle()
        onNodeWithText("친구가 부르면 알려드릴까요?").assertDoesNotExist()
    }

    // 기기에 적은 값이 돌아오기 전에 두 번 불려도 한 번만 띄운다
    @Test
    fun `물었다고 적기 전에 또 불려도 한 번만 띄운다`() = runComposeUiTest {
        var shown = 0
        var primer: NotificationPrimer? = null
        setContent { primer = Primer(NotificationPermission(missing = true) {}, seen = false, onSeen = { shown++ }) }

        runOnIdle {
            primer!!.offer()
            primer!!.offer()
        }

        assertEquals(1, shown)
    }

    @Composable
    private fun Primer(permission: NotificationPermission, seen: Boolean, onSeen: () -> Unit): NotificationPrimer {
        val primer = rememberNotificationPrimer(permission, seen, onSeen)
        OvalitTheme { NotificationPrimerHost(primer) }
        return primer
    }
}
