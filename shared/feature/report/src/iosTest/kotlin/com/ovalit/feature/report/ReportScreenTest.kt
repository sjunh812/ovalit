package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReportScreenTest {

    @Test
    fun `불러오는 중에는 숫자를 띄우지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Loading, onSelectQueue = {}) } }

        onNodeWithText("전투점수", substring = true).assertDoesNotExist()
    }

    @Test
    fun `고정 지표는 전투점수 K_D 피해량 헤드샷 순서로 한 줄에 놓는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val bounds = listOf("전투점수", "K/D", "피해량", "헤드샷").map { onNodeWithText(it).getBoundsInRoot() }

        assertTrue(bounds.all { it.top == bounds.first().top })
        assertEquals(bounds.sortedBy { it.left }, bounds)
    }

    // 역할만 적으면 그 기간에 그 역할만 한 것처럼 읽힌다
    @Test
    fun `기간 줄의 역할은 가장 많이 한 역할이라고 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("주로 타격대 · ", substring = true).assertExists()
    }

    @Test
    fun `움직인 지표가 없으면 큰 변화가 없다고 알려준다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.steady) }

        onNodeWithText("최근 2주는 큰 변화가 없어요").assertExists()
    }

    // 새 액트 첫 주처럼 비교할 기록이 없으면 변화가 없는 게 아니라 모르는 것이다
    @Test
    fun `판단을 보류한 칸만 있으면 큰 변화가 없다고 하지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.unknown) }

        onNodeWithText("큰 변화가 없어요", substring = true).assertDoesNotExist()
        onNodeWithText("아직 비교할 기록이 모자라요").assertExists()
    }

    @Test
    fun `이번 주에 뛴 경기가 없으면 지난주 리포트라고 알려준다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.lastWeek) }

        onNodeWithText("지난주").assertExists()
        onNodeWithText("이번 주는 아직 경기가 없어요").assertExists()
    }

    @Test
    fun `기타 모드는 K_D와 헤드샷만 보여준다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("K/D").assertExists()
        onNodeWithText("헤드샷").assertExists()
        onNodeWithText("전투점수").assertDoesNotExist()
        onNodeWithText("피해량").assertDoesNotExist()
    }

    @Test
    fun `큐 칩을 누르면 그 큐를 고른다`() = runComposeUiTest {
        var selected: QueueFilter? = null
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = { selected = it },
                )
            }
        }

        onNodeWithText("기타").performClick()

        assertEquals(QueueFilter.OTHER, selected)
    }

    @Test
    fun `경기가 모자라면 리포트까지 남은 경기 수를 알려준다`() = runComposeUiTest {
        setContent { Report(WeeklyReport.NotEnoughMatches(played = 3)) }

        onNodeWithText("리포트까지 2경기 남았어요").assertExists()
    }

    // 복귀한 사람에게 "리포트까지 5경기 남았어요"는 맥락 없는 말이다
    @Test
    fun `4주 동안 뛴 경기가 없으면 쉬었다는 걸 먼저 알려준다`() = runComposeUiTest {
        setContent { Report(WeeklyReport.NotEnoughMatches(played = 0)) }

        onNodeWithText("최근 4주 동안 뛴 경기가 없어요").assertExists()
    }
}

@Composable
private fun Report(report: WeeklyReport, queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED) {
    OvalitTheme { ReportScreen(ReportUiState.Success(queueFilter, report), onSelectQueue = {}) }
}
