package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Focus
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.feature.report.component.DynamicMetricSheetBody
import com.ovalit.feature.report.component.MetricSheetBody
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
    fun `기간 경기의 승패를 적고 승률을 붙인다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("5승 2패", useUnmergedTree = true).assertExists()
        onNodeWithText("71%", useUnmergedTree = true).assertExists()
    }

    // 한 주에 수십 판을 뛰면 칸이 바코드처럼 줄어서 가장 최근 20경기만 칸으로 둔다. 승패 글자는 기간 전체다.
    @Test
    fun `기간 경기가 20을 넘으면 최근 20경기만 칸으로 두고 승패는 전부 센다`() = runComposeUiTest {
        val many = ReportPreviewData.moved.copy(results = List(30) { it % 3 != 0 })
        setContent { Report(many) }

        onNodeWithText("최근 20경기", useUnmergedTree = true).assertExists()
        onNodeWithText("20승 10패", useUnmergedTree = true).assertExists()
    }

    // 리포트는 오래된 경기부터 담지만 칸은 op.gg와 경기 탭처럼 최근 경기가 왼쪽이다
    @Test
    fun `승패 칸은 가장 최근 경기부터 왼쪽에 둔다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.copy(results = listOf(false, false, true, null, true))
        setContent { Report(report) }

        onNodeWithContentDescription("최근 경기부터 승, 무, 승, 패, 패", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `기간 경기가 20 이하면 최근 몇 경기라고 적지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("최근", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 요원은 판 수가 적어 승패로 적고, 5판을 못 넘겨도 그 기간 KDA는 적는다. (70 + 18) ÷ 48
    @Test
    fun `기간 요원과 무기 칸을 누르면 요원과 무기 화면을 연다`() = runComposeUiTest {
        var opened = ""
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = {},
                    onOpenAgents = { opened += "agents" },
                    onOpenWeapons = { opened += "weapons" },
                )
            }
        }

        onNodeWithText("3승 1패", useUnmergedTree = true).assertExists()
        onNodeWithText("KDA 1.83", useUnmergedTree = true).assertExists()
        onNodeWithText("64킬", useUnmergedTree = true).assertExists()
        onNodeWithText("이번 주 요원").performScrollTo().performClick()
        onNodeWithText("이번 주 무기").performScrollTo().performClick()

        assertEquals("agentsweapons", opened)
    }

    @Test
    fun `기타 모드에는 기간 요원과 무기 칸이 없다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, queueFilter = QueueFilter.OTHER) }

        onNodeWithText("이번 주 요원").assertDoesNotExist()
        onNodeWithText("이번 주 무기").assertDoesNotExist()
    }

    // 사용자 결정: KDA를 먼저, 한 단계 크게 두고 판당 K/D/A를 옆에 둔다. (118 + 30) ÷ 88
    @Test
    fun `KDA를 판당 K와 D와 A보다 먼저 크게 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val kda = onNodeWithText("KDA 1.68", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val perMatch = onNodeWithText("판당", substring = true, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(kda.right <= perMatch.left)
        // 제목 크기(줄 높이 24)라서 설명 글자(17)보다 줄이 확실히 높다
        assertTrue(kda.bottom - kda.top >= perMatch.bottom - perMatch.top + 5.dp)
    }

    @Test
    fun `기간 줄에서 역할 이름만 굵게 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val caption = onNodeWithText("타격대 78% · ", substring = true)
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

    @Test
    fun `오른쪽 위 아바타를 누르면 내 프로필을 연다`() = runComposeUiTest {
        var opened = false
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = {},
                    badge = PlayerBadge("오발러#KR1", tier = 16, tierName = "플래티넘 2"),
                    onOpenProfile = { opened = true },
                )
            }
        }

        onNodeWithText("플래티넘 2").performClick()

        assertTrue(opened)
    }

    // 역할만 적으면 그 기간에 그 역할만 한 것처럼 읽힌다
    @Test
    fun `기간 줄의 역할에는 그 역할로 뛴 비중을 붙인다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("타격대 78% · ", substring = true).assertExists()
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

    // 관심사 지표가 늘 앞에 있어서 움직인 지표까지 합치면 셋을 넘는다. 한 줄에 셋씩 놓고 나머지는 다음 줄로 넘긴다.
    @Test
    fun `동적 칸은 다섯 개까지 한 줄에 셋씩 놓는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.focused) }

        val first = onNodeWithText("포스바이 승률", useUnmergedTree = true).getBoundsInRoot()
        val third = onNodeWithText("풀바이 승률", useUnmergedTree = true).getBoundsInRoot()
        val fourth = onNodeWithText("퍼블 관여율", useUnmergedTree = true).getBoundsInRoot()
        val fifth = onNodeWithText("생존율", useUnmergedTree = true).getBoundsInRoot()

        assertEquals(first.top, third.top)
        assertEquals(first.left, fourth.left)
        assertTrue(fourth.top > first.top)
        assertEquals(fourth.top, fifth.top)
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

    // 사용자 결정: 칸 밑의 작은 글씨를 줄인다. 네 칸의 평균을 한 줄로 늘어놓으면 어느 숫자가 어느 칸인지 읽히지 않았다.
    @Test
    fun `고정 칸 밑에는 무엇과 견준 변화량인지만 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("변화량은 지난 4주 평균과 비교했어요").assertExists()
        onNodeWithText("146라운드", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("4주 평균 ", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `고정 칸 시트에는 칸 밑에서 뺀 평균을 적는다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        onNodeWithText("지난 4주 평균", substring = true).assertExists()
    }

    // 사용자 결정: "라운드 153" 같은 표본은 칸에서 빼고 시트에서 풀어 적는다
    @Test
    fun `달라진 점 칸에는 평소 값만 두고 표본은 적지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onAllNodesWithText("평소 ", substring = true, useUnmergedTree = true).assertCountEquals(3)
        onNodeWithText("라운드\u00a0146", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("4주 평균 ", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `달라진 점 칸을 누르면 표본과 판단 근거를 적은 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("몇 번으로 셌나요?").assertDoesNotExist()
        onNodeWithText("생존율", substring = true).performScrollTo().performClick()

        onNodeWithText("몇 번으로 셌나요?").assertExists()
        onNodeWithText("라운드\u00a0146").assertExists()
        onNodeWithText("비율의 분모예요. 이번 기간과 비교하는 주가 모두 라운드\u00a040 이상이어야 비교해요.").assertExists()
        onNodeWithText("이번 변화가 지난 8주 동안 주마다 흔들린 폭의 1.5배를 넘어서 달라졌다고 봤어요.").assertExists()
    }

    // 판단을 보류한 칸은 달라졌다고도, 그대로라고도 하지 않는다
    @Test
    fun `판단하지 않은 칸의 시트는 기록이 모자라다고 적는다`() = runComposeUiTest {
        setContent { OvalitTheme { DynamicMetricSheetBody(DynamicMetric.FIRST_KILL_WIN_RATE, ReportPreviewData.moved) } }

        onNodeWithText("비교할 기록이 모자라 달라졌는지 판단하지 않았어요", substring = true).assertExists()
        onNodeWithText("퍼블\u00a010번 이상이어야", substring = true).assertExists()
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

        onNodeWithText("이번 액트 기록이 4주 이상 쌓이면 평소 범위를 알려드려요.").assertExists()
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

    // 관심사로 골라 앞에 둔 지표면 역할이 아니라 관심사로 까닭을 말한다
    @Test
    fun `관심사 지표로 고른 문장은 관심사를 까닭으로 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(insight = it.insight?.copy(isRolePriority = false, focus = Focus.AIM)) }
        setContent { Report(report) }

        onNodeWithText("공격 71%, 수비 45%예요. 에임 올리기를 고르셔서 먼저 봤어요.").assertExists()
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

    // 화면에 보이는 자릿수로 겨룬다. 나는 K/D 1.34, 피해량 138, 헤드샷 21%다.
    @Test
    fun `라이벌 칸은 세 지표 중 앞선 개수를 센다`() = runComposeUiTest {
        val rival = ReportPreviewData.moved.metrics.let { it.copy(kills = 100, damage = 21_000, shots = it.shots.copy(head = 60)) }
        setContent { Social(rival = FriendStanding(PlayerId("junho"), "준호#KR1", rival)) }

        onNodeWithText("라이벌 · 준호#KR1").assertExists()
        onNodeWithText("3개 중 2개 앞섬").assertExists()
    }

    @Test
    fun `라이벌을 고르지 않았으면 라이벌 칸이 없다`() = runComposeUiTest {
        setContent { Social(rival = null) }

        onNodeWithText("라이벌", substring = true).assertDoesNotExist()
    }

    @Test
    fun `친구 비교는 나를 넣어 값이 큰 순서로 세우고 경기가 없는 친구는 뺀다`() = runComposeUiTest {
        val strong = ReportPreviewData.moved.metrics.copy(damage = 30_000)
        setContent {
            Social(
                friends = listOf(
                    FriendStanding(PlayerId("junho"), "준호#KR1", strong),
                    FriendStanding(PlayerId("minseok"), "민석#KR3", null),
                ),
            )
        }

        // 친구 비교는 화면 아래에 있어서 잘리기 전 자리로 본다
        val junho = onNodeWithText("준호", substring = true).getUnclippedBoundsInRoot().top
        val me = onNodeWithText("나", substring = true).getUnclippedBoundsInRoot().top
        assertTrue(junho < me)
        onNodeWithText("민석", substring = true).assertDoesNotExist()
    }

    @Test
    fun `기타 모드에는 라이벌과 친구 비교가 없다`() = runComposeUiTest {
        val friend = FriendStanding(PlayerId("junho"), "준호#KR1", ReportPreviewData.moved.metrics)
        setContent { Social(rival = friend, friends = listOf(friend), queueFilter = QueueFilter.OTHER) }

        onNodeWithText("친구 비교").assertDoesNotExist()
        onNodeWithText("라이벌", substring = true).assertDoesNotExist()
    }

    @Test
    fun `친구가 없으면 초대 칸을 누르면 초대 링크를 보낸다`() = runComposeUiTest {
        var shared = false
        setContent { Social(nudge = HomeNudge.INVITE_FRIEND, onShareInvite = { shared = true }) }

        onNodeWithText("같이 뛰는 친구를 불러 보세요").performScrollTo().performClick()

        assertTrue(shared)
    }

    @Test
    fun `라이벌 칸을 누르면 시트에서 친구를 골라 라이벌로 정한다`() = runComposeUiTest {
        var picked: PlayerId? = null
        val friends = listOf(
            FriendStanding(PlayerId("junho"), "준호#KR1", ReportPreviewData.moved.metrics),
            FriendStanding(PlayerId("minseok"), "민석#KR3", null),
        )
        setContent { Social(friends = friends, nudge = HomeNudge.PICK_RIVAL, onSelectRival = { picked = it }) }

        onNodeWithText("라이벌을 골라 보세요").performScrollTo().performClick()
        onNodeWithText("라이벌 고르기").assertExists()
        onNodeWithText("민석#KR3", substring = true).assertExists()
        onNodeWithText("준호#KR1", substring = true).performClick()

        assertEquals(PlayerId("junho"), picked)
    }

    // 수집 중에는 숫자를 띄우지 않고 자리만 잡는다. 낭독기에는 칸마다가 아니라 한 줄로 알린다.
    @Test
    fun `리포트를 만들기 전에는 자리만 잡고 숫자를 띄우지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Loading, onSelectQueue = {}) } }

        onNodeWithContentDescription("리포트를 불러오고 있어요").assertExists()
        onNodeWithText("전투점수").assertDoesNotExist()
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

@Composable
private fun Social(
    rival: FriendStanding? = null,
    friends: List<FriendStanding> = emptyList(),
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
    nudge: HomeNudge? = null,
    onShareInvite: () -> Unit = {},
    onSelectRival: (PlayerId) -> Unit = {},
) {
    val report = if (queueFilter == QueueFilter.OTHER) ReportPreviewData.otherQueue else ReportPreviewData.moved
    OvalitTheme {
        ReportScreen(
            uiState = ReportUiState.Success(queueFilter, report, rival = rival, friends = friends, nudge = nudge),
            onSelectQueue = {},
            onShareInvite = onShareInvite,
            onSelectRival = onSelectRival,
        )
    }
}
