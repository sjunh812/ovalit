package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.report.component.MetricSheetBody
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import androidx.compose.ui.unit.dp
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

    // 동적 칸과 개선 포인트가 이 역할에 맞춰 골라지니 한눈에 들어와야 한다
    @Test
    fun `기간 줄에서 역할 이름만 굵게 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val caption = onNodeWithText("주로 타격대 · ", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].first()
        val bold = caption.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }

        assertEquals(listOf("타격대"), bold.map { caption.text.substring(it.start, it.end) })
    }

    // 칸 폭에 간격을 넣으면 가운데 칸만 좁아져 첫 칸이 넓어 보인다
    @Test
    fun `고정 칸은 네 칸 모두 폭이 같다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val widths = listOf("전투점수", "K/D", "피해량", "헤드샷").map { onNodeWithText(it).getBoundsInRoot().let { bounds -> bounds.right - bounds.left } }

        // 남는 픽셀 하나는 어느 칸엔가 붙는다
        assertTrue(widths.max() - widths.min() <= 1.dp, "$widths")
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

    @Test
    fun `고정 칸을 누르면 그 지표 설명 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("어떻게 계산하나요?").assertDoesNotExist()
        onNodeWithText("피해량").performClick()

        onNodeWithText("ADR").assertExists()
        onNodeWithText("어떻게 계산하나요?").assertExists()
    }

    // CLAUDE.md: 총량을 더한 뒤 나눈다
    @Test
    fun `계산식에는 기간 합계를 그대로 적는다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        onNodeWithText("20,108 피해 ÷ 146라운드 = 138").assertExists()
    }

    @Test
    fun `헤드샷 계산식은 맞힌 탄 기준이다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.HEADSHOT_RATE, ReportPreviewData.moved) }

        onNodeWithText("머리 92발 ÷ 맞힌 탄 429발 = 21%").assertExists()
    }

    // CLAUDE.md: 비교 대상은 본인의 과거뿐이다
    @Test
    fun `평소 범위는 내 지난 주간 값으로 말한다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        onNodeWithText("지난 7주 동안 주마다 126~135 사이였어요.").assertExists()
    }

    @Test
    fun `액트가 바뀌어 앞선 주가 모자라면 평소 범위를 말하지 않는다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.newAct) }

        onNodeWithText("이번 액트 기록이 4주 이상 쌓이면 평소 범위를 알려 드려요.").assertExists()
        onNodeWithText("세로선은 액트가 바뀐 곳이에요").assertExists()
    }

    @Test
    fun `데스가 없으면 K_D 계산식 대신 비워 둔 이유를 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(metrics = it.metrics.copy(deaths = 0)) }
        setContent { Sheet(FixedMetric.KD, report) }

        onNodeWithText("데스가 없어서", substring = true).assertExists()
        onNodeWithText("킬 ÷", substring = true).assertDoesNotExist()
    }

    @Test
    fun `개선 포인트는 낮은 쪽 진영부터 사실만 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("수비 라운드 첫 교전 승률이 공격보다 26%p 낮아요.").assertExists()
        onNodeWithText("공격 71%, 수비 45%예요. 타격대에게 첫 교전 승률은 먼저 보는 지표예요.").assertExists()
    }

    @Test
    fun `우선 지표가 아니면 역할 문장을 붙이지 않는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(insight = it.insight?.copy(isRolePriority = false)) }
        setContent { Report(report) }

        onNodeWithText("공격 71%, 수비 45%예요.").assertExists()
    }

    @Test
    fun `공격이 더 낮으면 공격 라운드부터 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { base ->
            val insight = base.insight!!
            base.copy(insight = insight.copy(attack = insight.defense, defense = insight.attack))
        }
        setContent { Report(report) }

        onNodeWithText("공격 라운드 첫 교전 승률이 수비보다 26%p 낮아요.").assertExists()
    }

    @Test
    fun `공수 격차가 없으면 개선 포인트를 비운다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(insight = null)) }

        onNodeWithText("낮아요.", substring = true).assertDoesNotExist()
    }

    @Test
    fun `기타 모드에는 개선 포인트가 없다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("낮아요.", substring = true).assertDoesNotExist()
    }
}

@Composable
private fun Report(report: WeeklyReport, queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED) {
    OvalitTheme { ReportScreen(ReportUiState.Success(queueFilter, report), onSelectQueue = {}) }
}

@Composable
private fun Sheet(metric: FixedMetric, report: WeeklyReport.Ready) {
    OvalitTheme { MetricSheetBody(metric, report) }
}
