package com.ovalit.feature.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.BuyType
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.DynamicMetric
import com.ovalit.core.model.DynamicSlot
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Focus
import com.ovalit.core.model.Insight
import com.ovalit.core.model.InsightMetric
import com.ovalit.core.model.InsightPart
import com.ovalit.core.model.InsightSubject
import com.ovalit.core.model.MapId
import com.ovalit.core.model.MixGroup
import com.ovalit.core.model.MixShift
import com.ovalit.core.model.MovedMetric
import com.ovalit.core.model.Movement
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.Role
import com.ovalit.core.model.Side
import com.ovalit.core.model.SteadyPart
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponId
import com.ovalit.core.model.WeaponInfo
import com.ovalit.core.model.WeekNote
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.WRAPPING_SEPARATOR
import com.ovalit.core.ui.joinKeepingParts
import com.ovalit.feature.report.component.DynamicMetricSheetBody
import com.ovalit.feature.report.component.MetricSheetBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    // 글씨를 키운 좁은 화면에서 승패 글자가 먼저 자리를 잡으면 칸이 몇 dp만 남아 바코드처럼 보였다
    @Test
    fun `좁은 화면에서 칸이 너무 좁아지면 승패 글자를 칸 밑으로 내린다`() = runComposeUiTest {
        val many = ReportPreviewData.moved.copy(results = List(30) { it % 3 != 0 })
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                Box(Modifier.width(320.dp)) { Report(many) }
            }
        }

        val cells = onNodeWithContentDescription("최근 경기부터", substring = true, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val record = onNodeWithText("20승 10패", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(record.top >= cells.bottom)
    }

    // 전략가·감시자의 빈칸을 채우는 "라운드당 어시스트"는 좁은 칸에서 가장 작은 글자로도 한 줄에 안 들어갔다.
    // 잘리지 않게 꺾고, 한 칸만 꺾여 그 칸 숫자만 내려가지 않게 모든 칸을 같이 꺾는다.
    @Test
    fun `달라진 점 칸 이름이 좁은 칸에 안 들어가면 모든 칸을 같이 꺾는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.copy(
            dynamic = listOf(
                DynamicSlot(DynamicMetric.KAST, Movement.STEADY),
                DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.STEADY),
                DynamicSlot(DynamicMetric.ASSISTS_PER_ROUND, Movement.STEADY),
            ),
        )
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                Box(Modifier.width(320.dp)) { Report(report) }
            }
        }

        val assists = onNodeWithText("라운드당 어시스트", useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        assists.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertFalse(layouts.single().hasVisualOverflow, "칸 이름이 잘렸다")
        val kast = onNodeWithText("관여율", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(assists.getUnclippedBoundsInRoot().bottom, kast.bottom, "칸마다 이름 높이가 다르다")
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

    // 사용자 결정(2026-09-27): KDA를 먼저, 한 단계 크게 두고 판당 K/D/A를 옆에 둔다. (118 + 30) ÷ 88 = 1.68
    @Test
    fun `KDA를 판당 K와 D와 A보다 먼저 크게 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val kda = onNodeWithText("KDA 1.68", substring = true, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val perMatch = onNodeWithText("판당", substring = true, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(kda.right <= perMatch.left)
        // 제목 크기(줄 높이 24)라서 설명 글자(17)보다 줄이 확실히 높다
        assertTrue(kda.bottom - kda.top >= perMatch.bottom - perMatch.top + 5.dp)
    }

    // 동적 칸과 개선 포인트가 이 역할에 맞춰 골라지니 한눈에 들어와야 한다
    @Test
    fun `기간 줄에서 역할 이름만 굵게 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val caption = onNodeWithText("타격대\u00a078%$WRAPPING_SEPARATOR", substring = true)
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

        onNodeWithText("타격대\u00a078%$WRAPPING_SEPARATOR", substring = true).assertExists()
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

    // 사용자 결정(2026-09-27): 칸 밑 작은 글씨는 한 줄만 둔다. 네 칸의 평균을 늘어놓으면 어느 숫자가 어느 칸 것인지 읽히지 않았다.
    @Test
    fun `고정 칸 밑에는 무엇과 견준 변화량인지만 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(note = null)) }

        onNodeWithText("변화량은 지난 4주 평균과 비교했어요").assertExists()
        onNodeWithText("146라운드", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("4주 평균 ", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `고정 칸 시트에는 칸 밑에서 뺀 평균을 적는다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        onNodeWithText("지난 4주 평균", substring = true).assertExists()
    }

    // 사용자 결정(2026-09-27): "라운드 153" 같은 표본은 칸에서 빼고 시트에서 풀어 적는다
    @Test
    fun `달라진 점 칸에는 평소 값만 두고 표본은 적지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(note = null)) }

        onAllNodesWithText("평소 ", substring = true, useUnmergedTree = true).assertCountEquals(3)
        onNodeWithText("146라운드", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("4주 평균 ", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `달라진 점 칸을 누르면 표본과 판단 근거를 적은 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("몇 번으로 셌나요?").assertDoesNotExist()
        onNodeWithText("생존율", substring = true).performScrollTo().performClick()

        onNodeWithText("몇 번으로 셌나요?").assertExists()
        onNodeWithText("146라운드").assertExists()
        onNodeWithText("위에 적은 표본이 이 비율의 분모예요. 이번 기간과 비교하는 주가 모두 40라운드 이상이어야 비교해요.").assertExists()
        onNodeWithText("이번 변화가 지난 8주 동안 주마다 흔들린 폭의 1.5배를 넘어서 달라졌다고 봤어요.").assertExists()
    }

    // 판단을 보류한 칸은 달라졌다고도, 그대로라고도 하지 않는다. 퍼블 승률은 moved의 동적 칸에 없는 지표라 판단 보류로 연다.
    @Test
    fun `판단하지 않은 칸의 시트는 기록이 모자라다고 적는다`() = runComposeUiTest {
        setContent { OvalitTheme { DynamicMetricSheetBody(DynamicMetric.FIRST_KILL_WIN_RATE, ReportPreviewData.moved) } }

        onNodeWithText("달라졌는지 판단하지 않았어요", substring = true).assertExists()
        onNodeWithText("퍼블\u00a010번 이상이어야", substring = true).assertExists()
    }

    // 프리뷰 데이터는 피해량이 128 → 138로 올랐다. 그 변화를 밴달과 제트가 가장 많이 끌었다.
    @Test
    fun `짚을 점은 움직인 지표와 같은 쪽 무기와 요원을 숫자로 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올랐어요").assertExists()
        // 사용자 요청(2026-09-27): 평균이 얼마였는지는 고정 칸에 이미 있어 다시 적지 않는다
        onNodeWithText("→ 이번 주", substring = true).assertDoesNotExist()
        onNodeWithText("가장 크게 끌어올린 무기", substring = true).assertExists()
        onNodeWithText("밴달 피해량 118 → 140 · 44라운드", substring = true).assertExists()
        onNodeWithText("가장 크게 끌어올린 요원", substring = true).assertExists()
        onNodeWithText("제트 피해량 124 → 146 · 4판", substring = true).assertExists()
        // 사용자 결정(2026-09-27): 이긴 판이 더 많았던 요원은 바로 밑 이번 주 요원 칸의 승패와 겹쳐 적지 않는다
        onNodeWithText("이긴 판이 더 많았던", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 이코 라운드가 늘어 떨어진 피해량을 무기 하나가 끌어내린 것처럼 적으면 틀린 얘기가 된다
    @Test
    fun `변화가 비중에서 왔으면 무기와 요원 대신 비중과 비중에 휘둘리지 않은 묶음을 적는다`() = runComposeUiTest {
        val note = WeekNote(
            moved = MovedMetric(FixedMetric.DAMAGE, current = 128.0, usual = 146.0),
            mix = MixShift(MixGroup.Buy(BuyType.ECO), share = 0.31, usualShare = 0.16, steady = SteadyPart(MixGroup.Buy(BuyType.FULL_BUY), current = 156.0, usual = 158.0)),
        )
        setContent { Report(ReportPreviewData.moved.copy(note = note), catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 18 떨어졌어요").assertExists()
        onNodeWithText("비중이 늘어난 라운드", useUnmergedTree = true).assertExists()
        onNodeWithText("이코 16% → 31%", useUnmergedTree = true).assertExists()
        onNodeWithText("풀바이 라운드만 보면", useUnmergedTree = true).assertExists()
        onNodeWithText("피해량 158 → 156", useUnmergedTree = true).assertExists()
        onNodeWithText("끌어내린", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 무기는 "을/를 든", 요원은 "(으)로 뛴"이다. 이름에 받침이 있는지에 맞춘다.
    @Test
    fun `비중 줄은 무기와 요원 이름에 맞는 조사를 붙인다`() = runComposeUiTest {
        fun note(group: MixGroup, steady: MixGroup) = WeekNote(
            moved = MovedMetric(FixedMetric.HEADSHOT_RATE, current = 0.17, usual = 0.21),
            mix = MixShift(group, share = 0.27, usualShare = 0.06, steady = SteadyPart(steady, current = 0.23, usual = 0.24)),
        )
        var shown by mutableStateOf(note(MixGroup.Weapon(Phantom), MixGroup.Weapon(Vandal)))
        setContent { Report(ReportPreviewData.moved.copy(note = shown), catalog = NamedCatalog) }

        onNodeWithText("비중이 늘어난 무기", useUnmergedTree = true).assertExists()
        onNodeWithText("팬텀 6% → 27%", useUnmergedTree = true).assertExists()
        onNodeWithText("밴달을 든 라운드만 보면", useUnmergedTree = true).assertExists()
        shown = note(MixGroup.Agent(Jett), MixGroup.Agent(Raze))
        onNodeWithText("비중이 늘어난 요원", useUnmergedTree = true).assertExists()
        onNodeWithText("레이즈로 뛴 판만 보면", useUnmergedTree = true).assertExists()
        onNodeWithText("헤드샷 24% → 23%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `비중이 줄었으면 줄어든 쪽으로 적는다`() = runComposeUiTest {
        val note = WeekNote(
            moved = MovedMetric(FixedMetric.DAMAGE, current = 131.0, usual = 147.0),
            mix = MixShift(MixGroup.Buy(BuyType.FULL_BUY), share = 0.52, usualShare = 0.68, steady = null),
        )
        setContent { Report(ReportPreviewData.moved.copy(note = note)) }

        onNodeWithText("비중이 줄어든 라운드", useUnmergedTree = true).assertExists()
        onNodeWithText("풀바이 68% → 52%", useUnmergedTree = true).assertExists()
        onNodeWithText("만 보면", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 사용자 결정(2026-09-27): KDA에도 고정 칸처럼 보이는 두 자리끼리 뺀 변화량을 붙인다
    @Test
    fun `KDA 옆에 지난 평균과의 변화량을 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        val kda = assertNotNull(report.metrics.kda)
        val usual = assertNotNull(report.baseline?.metrics?.kda)
        setContent { Report(report) }

        onNodeWithText(MetricFormat.TWO_DECIMALS.formatChange(kda, usual), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `비교할 기록이 없으면 KDA에 변화량을 붙이지 않는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.copy(baseline = null)
        val kda = assertNotNull(report.metrics.kda)
        val usual = assertNotNull(ReportPreviewData.moved.baseline?.metrics?.kda)
        setContent { Report(report) }

        onNodeWithText(MetricFormat.TWO_DECIMALS.formatChange(kda, usual), useUnmergedTree = true).assertDoesNotExist()
    }

    // "멀티킬 라운드 비율"도 받침에 맞는 조사를 붙인다
    @Test
    fun `멀티킬 개선 포인트는 조사가 맞는 문장으로 적는다`() = runComposeUiTest {
        val insight = sideInsight(InsightMetric.MULTI_KILL_RATE, lead = 0.30 to 40, other = 0.05 to 40, focus = Focus.AIM)
        setContent { Report(ReportPreviewData.moved.copy(insight = insight)) }

        onNodeWithText("공격에서 멀티킬 라운드 비율이 수비보다 25%p 높아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "공격 40라운드 30%", "수비 40라운드 5%"))).assertExists()
        onNodeWithText("에임 올리기를 고르셔서 먼저 봤어요.").assertExists()
    }

    // 사용자 요청(2026-09-27): 공수만 견주지 않는다. 두 쪽 모두 이름이 있으면 높은 쪽이 주어다. "10판 뛴 레이즈보다 5판
    // 뛴 제트가 높을 때는 제트를 중심으로"
    @Test
    fun `요원끼리 견준 개선 포인트는 높은 쪽을 주어로 판 수와 함께 적는다`() = runComposeUiTest {
        val insight = Insight(
            metric = InsightMetric.SURVIVAL_RATE,
            lead = InsightPart(InsightSubject.OnAgent(Jett), value = 0.75, matches = 5, rounds = 120),
            other = InsightPart(InsightSubject.OnAgent(Raze), value = 0.5, matches = 10, rounds = 240),
            leadIsHigher = true,
            isRolePriority = false,
        )
        setContent { Report(ReportPreviewData.moved.copy(insight = insight), catalog = NamedCatalog) }

        onNodeWithText("제트로 뛴 판은 생존율이 레이즈보다 25%p 높아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "제트 5판 75%", "레이즈 10판 50%"))).assertExists()
    }

    @Test
    fun `같은 역할 요원이 여럿이면 다른 요원으로 묶어 적는다`() = runComposeUiTest {
        val insight = Insight(
            metric = InsightMetric.KAST,
            lead = InsightPart(InsightSubject.OnAgent(Raze), value = 0.6, matches = 12, rounds = 280),
            other = InsightPart(InsightSubject.OtherAgents(Role.DUELIST, listOf(Jett, AgentId("neon"))), value = 0.74, matches = 30, rounds = 700),
            leadIsHigher = false,
            isRolePriority = false,
        )
        setContent { Report(ReportPreviewData.moved.copy(insight = insight), catalog = NamedCatalog) }

        onNodeWithText("레이즈로 뛴 판은 관여율이 다른 타격대 요원보다 14%p 낮아요.").assertExists()
    }

    // 사용자 결정(2026-09-27): 역할끼리는 승률만 견주고, 우연을 넘을 만큼 차이가 클 때만 나온다. "추천"은 쓰지 않는다.
    @Test
    fun `역할끼리 견준 개선 포인트는 승률을 사실로만 적는다`() = runComposeUiTest {
        val insight = Insight(
            metric = InsightMetric.WIN_RATE,
            lead = InsightPart(InsightSubject.OnRole(Role.CONTROLLER), value = 0.78, matches = 18, rounds = 420),
            other = InsightPart(InsightSubject.OnRole(Role.DUELIST), value = 0.34, matches = 32, rounds = 760),
            leadIsHigher = true,
            isRolePriority = false,
        )
        setContent { Report(ReportPreviewData.moved.copy(insight = insight)) }

        onNodeWithText("전략가로 뛴 판은 승률이 타격대보다 44%p 높아요.").assertExists()
        onNodeWithText("추천", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `맵과 무기끼리 견준 개선 포인트도 이름에 맞는 조사로 적는다`() = runComposeUiTest {
        val map = Insight(
            metric = InsightMetric.KAST,
            lead = InsightPart(InsightSubject.OnMap(Haven), value = 0.58, matches = 9, rounds = 200),
            other = InsightPart(InsightSubject.OtherMaps(listOf(MapId("ascent"), MapId("bind"))), value = 0.72, matches = 41, rounds = 900),
            leadIsHigher = false,
            isRolePriority = false,
        )
        val rifles = Insight(
            metric = InsightMetric.HEADSHOT_RATE,
            lead = InsightPart(InsightSubject.WithWeapon(Vandal), value = 0.14, matches = 0, rounds = 180),
            other = InsightPart(InsightSubject.OtherWeapons(WeaponCategory.RIFLE, listOf(Phantom, AgentlessRifle)), value = 0.25, matches = 0, rounds = 212),
            leadIsHigher = false,
            isRolePriority = false,
        )
        val twoRifles = rifles.copy(
            lead = InsightPart(InsightSubject.WithWeapon(Phantom), value = 0.25, matches = 0, rounds = 212),
            other = InsightPart(InsightSubject.WithWeapon(Vandal), value = 0.14, matches = 0, rounds = 180),
            leadIsHigher = true,
        )
        var insight by mutableStateOf(map)
        setContent { Report(ReportPreviewData.moved.copy(insight = insight), catalog = NamedCatalog) }

        onNodeWithText("헤이븐에서는 관여율이 다른 맵보다 14%p 낮아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "헤이븐 9판 58%", "다른 맵 41판 72%"))).assertExists()
        insight = rifles
        onNodeWithText("밴달을 든 라운드는 헤드샷이 다른 소총보다 11%p 낮아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "밴달 180라운드 14%", "다른 소총 212라운드 25%"))).assertExists()
        insight = twoRifles
        onNodeWithText("팬텀을 든 라운드는 헤드샷이 밴달보다 11%p 높아요.").assertExists()
    }

    // 헤드라인과 첫 줄이 붙으면 한 덩어리로 뭉개진다. 개선 포인트 문장과 같은 간격이다.
    @Test
    fun `짚을 점 헤드라인과 첫 줄은 6dp 띄운다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        val headline = onNodeWithText("피해량이 평소보다 10 올랐어요").getUnclippedBoundsInRoot()
        val first = onNodeWithText("가장 크게 끌어올린 무기", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(6.dp, first.top - headline.bottom)
    }

    // CLAUDE.md 지켜야 할 선: 게임 결정을 대신하지 않는다
    @Test
    fun `짚을 점은 무엇을 쓰라고 하지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        onNodeWithText("추천", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("쓰세요", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 사용자 요청(2026-09-27): 짚을 점은 바로 위 숫자를 풀어 말하는 문장이라 제목과 선 없이 고정 칸 밑에 붙인다
    @Test
    fun `짚을 점은 제목 없이 고정 칸 바로 밑에 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("짚을 점", substring = true).assertDoesNotExist()
        val summary = onNodeWithText("변화량은 지난 4주 평균과 비교했어요").getUnclippedBoundsInRoot()
        val note = onNodeWithText("피해량이 평소보다 10 올랐어요").getUnclippedBoundsInRoot()
        val dynamic = onNodeWithText("달라진 점").getUnclippedBoundsInRoot()
        assertTrue(summary.bottom <= note.top && note.bottom <= dynamic.top)
    }

    @Test
    fun `기타 모드에는 짚을 점이 없다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("평소보다", substring = true).assertDoesNotExist()
    }

    // 피해량은 정수라 "121예요"처럼 숫자 뒤 조사가 틀린다
    @Test
    fun `피해량 개선 포인트는 숫자 뒤에 조사를 붙이지 않는다`() = runComposeUiTest {
        val damage = sideInsight(InsightMetric.DAMAGE, lead = 143.0 to 70, other = 121.0 to 76)
        setContent { Report(ReportPreviewData.moved.copy(insight = damage)) }

        onNodeWithText("공격에서 피해량이 수비보다 22 높아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "공격 70라운드 143", "수비 76라운드 121"))).assertExists()
    }

    // 무기 값도 보이는 자릿수로 같으면 "140 → 140"이 돼서 무기 줄을 두지 않는다
    @Test
    fun `보이는 자릿수로 같은 무기는 짚지 않는다`() = runComposeUiTest {
        val note = assertNotNull(ReportPreviewData.moved.note)
        val flat = note.copy(weapon = note.weapon?.copy(current = 140.2, usual = 139.8))
        setContent { Report(ReportPreviewData.moved.copy(note = flat), catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올랐어요").assertExists()
        onNodeWithText("가장 크게 끌어올린 무기", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("가장 크게 끌어올린 요원", useUnmergedTree = true).assertExists()
    }

    // 21.4%와 21.2%는 둘 다 21%로 보인다. "0%p 올랐어요"라고 쓰지 않고, 빈 칸 몫의 간격도 남기지 않는다.
    @Test
    fun `보이는 자릿수로 차이가 없으면 짚을 점을 두지 않는다`() = runComposeUiTest {
        val flat = WeekNote(MovedMetric(FixedMetric.HEADSHOT_RATE, current = 0.214, usual = 0.212))
        var note by mutableStateOf<WeekNote?>(flat)
        setContent { Report(ReportPreviewData.moved.copy(note = note)) }

        onNodeWithText("평소보다", substring = true).assertDoesNotExist()
        val withFlat = onNodeWithText("달라진 점").getUnclippedBoundsInRoot().top
        note = null
        assertEquals(withFlat, onNodeWithText("달라진 점").getUnclippedBoundsInRoot().top)
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
        onNodeWithText("세로선은 액트가 바뀐 곳이에요.").assertExists()
    }

    @Test
    fun `데스가 없으면 K_D 계산식 대신 비워 둔 이유를 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(metrics = it.metrics.copy(deaths = 0)) }
        setContent { Sheet(FixedMetric.KD, report) }

        onNodeWithText("데스가 없어서", substring = true).assertExists()
        onNodeWithText("킬 ÷", substring = true).assertDoesNotExist()
    }

    // 한 주 경기를 둘로 나누면 우연한 차이가 대부분이라 이번 액트 경기로 견주고 밑 줄 맨 앞에 적는다
    @Test
    fun `개선 포인트는 이번 액트 공수를 높은 쪽부터 사실만 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("공격에서 첫 교전 승률이 수비보다 14%p 높아요.").assertExists()
        onNodeWithText(joinKeepingParts(listOf("이번 액트", "공격 388라운드 58%", "수비 388라운드 44%"))).assertExists()
        onNodeWithText("타격대에게 첫 교전 승률은 먼저 보는 지표예요.").assertExists()
    }

    @Test
    fun `우선 지표가 아니면 역할 문장을 붙이지 않는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(insight = it.insight?.copy(isRolePriority = false)) }
        setContent { Report(report) }

        onNodeWithText("먼저 보는 지표예요", substring = true).assertDoesNotExist()
    }

    // 관심사로 골라 앞에 둔 지표면 역할이 아니라 관심사로 까닭을 말한다
    @Test
    fun `관심사 지표로 고른 문장은 관심사를 까닭으로 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(insight = it.insight?.copy(isRolePriority = false, focus = Focus.AIM)) }
        setContent { Report(report) }

        onNodeWithText("에임 올리기를 고르셔서 먼저 봤어요.").assertExists()
    }

    @Test
    fun `수비가 더 높으면 수비부터 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { base ->
            val insight = base.insight!!
            base.copy(
                insight = insight.copy(
                    lead = insight.lead.copy(subject = InsightSubject.OnSide(Side.DEFENSE)),
                    other = insight.other.copy(subject = InsightSubject.OnSide(Side.ATTACK)),
                ),
            )
        }
        setContent { Report(report) }

        onNodeWithText("수비에서 첫 교전 승률이 공격보다 14%p 높아요.").assertExists()
    }

    @Test
    fun `견줄 만한 격차가 없으면 개선 포인트를 비운다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(insight = null)) }

        onNodeWithText("이번 액트", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `기타 모드에는 개선 포인트가 없다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("이번 액트", substring = true, useUnmergedTree = true).assertDoesNotExist()
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

    // 수집 중에는 숫자를 띄우지 않고 자리만 잡는다. 화면 읽기 프로그램에는 칸마다가 아니라 한 줄로 알린다.
    @Test
    fun `리포트를 만들기 전에는 자리만 잡고 숫자를 띄우지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { ReportScreen(ReportUiState.Loading, onSelectQueue = {}) } }

        onNodeWithContentDescription("리포트를 불러오고 있어요").assertExists()
        onNodeWithText("전투점수").assertDoesNotExist()
    }
}

@Composable
private fun Report(
    report: WeeklyReport,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
    catalog: ContentCatalog = ContentCatalog.Empty,
) {
    OvalitTheme { ReportScreen(ReportUiState.Success(queueFilter, report), onSelectQueue = {}, catalog = catalog) }
}

// 짚을 점 문장에 이름이 들어가서 프리뷰 데이터의 요원과 무기에 이름을 붙였다
private val Jett = AgentId("add6443a-41bd-e414-f6ad-e58d267f4e95")
private val Raze = AgentId("f94c3b30-42be-e959-889c-5aa313dba261")
private val Haven = MapId("2bee0dc9-4ffe-519b-1cbd-7fbe763a6047")
private val Vandal = WeaponId("9C82E19D-4575-0200-1A81-3EACF00CF872")
private val Phantom = WeaponId("EE8E8D15-496B-07AC-E5F6-8FAE5D4C7B1A")
private val AgentlessRifle = WeaponId("rifle-without-name")

private val NamedCatalog = ContentCatalog.Empty.copy(
    agents = mapOf(Jett to "제트", Raze to "레이즈"),
    weapons = mapOf(Vandal to WeaponInfo("밴달", WeaponCategory.RIFLE), Phantom to WeaponInfo("팬텀", WeaponCategory.RIFLE)),
    maps = mapOf(Haven to "헤이븐"),
)

// 높은 공격과 낮은 수비를 (값, 라운드)로 받아 공수 개선 포인트를 만든다
private fun sideInsight(metric: InsightMetric, lead: Pair<Double, Int>, other: Pair<Double, Int>, focus: Focus? = null) = Insight(
    metric = metric,
    lead = InsightPart(InsightSubject.OnSide(Side.ATTACK), value = lead.first, matches = 7, rounds = lead.second),
    other = InsightPart(InsightSubject.OnSide(Side.DEFENSE), value = other.first, matches = 7, rounds = other.second),
    leadIsHigher = true,
    isRolePriority = false,
    focus = focus,
)

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
