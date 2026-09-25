package com.ovalit.feature.report

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.WeeklyReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReportScreenTest {

    // 지우면 프로덕션 키가 회수되는 문구다. 리포트 화면과 경기가 모자란 화면 둘 다 본다.
    @Test
    fun `비공식 고지가 홈 화면에 있다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Success(ReportPreviewData.moved)) } }

        onNodeWithText("Riot Games", substring = true).assertExists()
    }

    @Test
    fun `경기가 모자란 화면에도 비공식 고지가 있다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Success(ReportPreviewData.notEnough)) } }

        onNodeWithText("Riot Games", substring = true).assertExists()
    }

    @Test
    fun `불러오는 중에는 숫자를 띄우지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Loading) } }

        onNodeWithText("전투점수", substring = true).assertDoesNotExist()
    }

    @Test
    fun `고정 지표는 전투점수 K_D 피해량 헤드샷 순서로 놓는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Success(ReportPreviewData.moved)) } }

        val combatScore = onNodeWithText("전투점수", substring = true).getBoundsInRoot()
        val kd = onNodeWithText("K/D", substring = true).getBoundsInRoot()
        val damage = onNodeWithText("피해량", substring = true).getBoundsInRoot()
        val headshot = onNodeWithText("헤드샷", substring = true).getBoundsInRoot()

        assertEquals(combatScore.top, kd.top)
        assertTrue(combatScore.left < kd.left)
        assertEquals(damage.top, headshot.top)
        assertTrue(damage.left < headshot.left)
        assertTrue(combatScore.top < damage.top)
    }

    @Test
    fun `움직인 지표가 없으면 큰 변화가 없다고 알려준다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Success(ReportPreviewData.steady)) } }

        onNodeWithText("최근 2주는 큰 변화가 없어요").assertExists()
    }

    // 새 액트 첫 주처럼 비교할 기록이 없으면 변화가 없는 게 아니라 모르는 것이다
    @Test
    fun `판단을 보류한 칸만 있으면 큰 변화가 없다고 하지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Success(ReportPreviewData.unknown)) } }

        onNodeWithText("큰 변화가 없어요", substring = true).assertDoesNotExist()
        onNodeWithText("아직 비교할 기록이 모자라요").assertExists()
    }

    @Test
    fun `경기가 모자라면 리포트까지 남은 경기 수를 알려준다`() = runComposeUiTest {
        setContent {
            OvalitTheme { ReportScreen(ReportUiState.Success(WeeklyReport.NotEnoughMatches(played = 3))) }
        }

        onNodeWithText("리포트까지 2경기 남았어요").assertExists()
    }
}
