package com.ovalit.feature.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ovalit.core.designsystem.component.LocalScreenEntering
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitColors
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
import com.ovalit.core.model.InsightRecent
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
import com.ovalit.core.ui.AdPlacement
import com.ovalit.core.ui.AdRenderer
import com.ovalit.core.ui.LocalAdRenderer
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.WRAPPING_SEPARATOR
import com.ovalit.core.ui.keepTogether
import com.ovalit.feature.report.component.DynamicMetricSheetBody
import com.ovalit.feature.report.component.MetricSheetBody
import com.ovalit.feature.report.component.TrendSheetBody
import com.ovalit.feature.report.component.changeColor
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

    // 첫 수집 뒤 홈으로 넘어올 때 리포트가 전환 한가운데 도착한다. 그때 홈 전체를 그리면 밀려 들어오던 화면이 멈춘다.
    @Test
    fun `밀려 들어오는 중에 리포트가 오면 다 들어온 뒤에 숫자를 띄운다`() = runComposeUiTest {
        var entering by mutableStateOf(true)
        var uiState by mutableStateOf<ReportUiState>(ReportUiState.Loading)
        setContent {
            CompositionLocalProvider(LocalScreenEntering provides entering) {
                OvalitTheme { ReportScreen(uiState, onSelectQueue = {}) }
            }
        }

        uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved)
        waitForIdle()
        onNodeWithText("전투점수").assertDoesNotExist()

        entering = false
        waitForIdle()
        onNodeWithText("전투점수").assertExists()
    }

    // 달라진 점 칸과 같은 격자다
    @Test
    fun `고정 지표는 한 줄에 세 칸씩 피해량 K_D KDA 전투점수 헤드샷 순서로 놓는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val (damage, kd, kda, combat, headshot) = listOf("피해량", "K/D", "KDA", "전투점수", "헤드샷")
            .map { onNodeWithText(it).getUnclippedBoundsInRoot() }

        val first = listOf(damage, kd, kda)
        assertTrue(first.all { it.top == damage.top })
        assertEquals(first.sortedBy { it.left }, first)
        assertTrue(combat.top >= damage.bottom && headshot.top == combat.top, "전투점수와 헤드샷이 둘째 줄에 없다")
        assertEquals(damage.left, combat.left)
        assertEquals(kd.left, headshot.left)
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

    // 글씨를 키운 좁은 화면에서 승패 글자가 먼저 자리를 잡으면 칸이 몇 dp만 남아 바코드처럼 보인다
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

    // 전략가·감시자의 빈칸을 채우는 "라운드당 어시스트"는 좁은 칸에서 가장 작은 글자로도 한 줄에 안 들어간다.
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

    @Test
    fun `KDA는 다른 고정 칸과 같은 숫자 크기로 둔다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        setContent { Report(report) }

        val kda = MetricFormat.TWO_DECIMALS.valueTextFor(assertNotNull(report.metrics.kda))
        val combatValue = MetricFormat.INTEGER.valueTextFor(assertNotNull(report.metrics.acs))
        assertEquals(fontSizeOf(combatValue), fontSizeOf(kda))
    }

    // 판당 K/D/A는 K/D에도 똑같이 필요한 풀이라 칸이 아니라 시트에 둔다
    @Test
    fun `판당 킬 데스 어시는 홈 대신 K_D와 KDA 시트에 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        val metrics = report.metrics
        var sheet by mutableStateOf<FixedMetric?>(null)
        setContent { sheet?.let { Sheet(it, report) } ?: Report(report) }
        fun perMatch(count: Int) = MetricFormat.ONE_DECIMAL.format(count.toDouble() / metrics.matches)

        onNodeWithText("판당", substring = true, useUnmergedTree = true).assertDoesNotExist()
        sheet = FixedMetric.KD
        onNodeWithText("판당 ${perMatch(metrics.kills)}킬 · ${perMatch(metrics.deaths)}데스").assertExists()
        sheet = FixedMetric.KDA
        onNodeWithText("판당 ${perMatch(metrics.kills)}킬 · ${perMatch(metrics.deaths)}데스 · ${perMatch(metrics.assists)}어시").assertExists()
    }

    // 변화량이 무엇과 견준 값인지 옆의 평소 값이 말한다
    @Test
    fun `고정 칸 숫자 밑에 변화량과 평소 값을 나란히 둔다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        setContent { Report(report) }

        val (change, usual) = combatSubLine(report)
        val changeBounds = onNodeWithText(change, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val usualBounds = onNodeWithText(usual, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = onNodeWithText(MetricFormat.INTEGER.valueTextFor(assertNotNull(report.metrics.acs)), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(changeBounds.top >= value.bottom, "변화량이 숫자 밑에 없다")
        assertTrue(usualBounds.left > changeBounds.right && usualBounds.top < changeBounds.bottom, "평소 값이 변화량 옆에 없다")
    }

    // 한 칸만 두 줄이 되면 그 칸만 높아져 줄이 어긋난다
    @Test
    fun `칸이 좁으면 변화량과 평소 값을 모든 칸에서 두 줄로 내린다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2f)) {
                Box(Modifier.width(300.dp)) { Report(report) }
            }
        }

        val (change, usual) = combatSubLine(report)
        val changeBounds = onNodeWithText(change, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val usualBounds = onNodeWithText(usual, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(usualBounds.top >= changeBounds.bottom, "좁은데 평소 값이 변화량 옆에 끼었다")
    }

    // 카드마다 따로 정하면 폭이 좁은 기기에서 글자가 한 자 긴 달라진 점만 두 줄로 내려가 위 고정 칸과 어긋난다
    @Test
    fun `고정 칸과 달라진 점은 숫자 밑 한 줄을 같은 크기 같은 줄 수로 둔다`() {
        listOf(360 to 1f, 320 to 1.15f, 300 to 1.3f, 300 to 2f).forEach { (width, fontScale) ->
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = fontScale)) {
                        Box(Modifier.width(width.dp)) { Report(ReportPreviewData.moved.copy(note = null)) }
                    }
                }

                // 화면 밖 칸도 재야 해서 잘리지 않은 자리를 쓴다
                val texts = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .map { node -> node.config[SemanticsProperties.Text].joinToString("") to Rect(node.positionInRoot, node.size.toSize()) }
                val changes = texts.filter { (text, _) -> ChangeText.matches(text) }.map { it.second }
                val usuals = texts.filter { (text, _) -> text.startsWith("평소 ") }.map { it.second }
                assertEquals(FixedMetric.entries.size + ReportPreviewData.moved.dynamic.size, usuals.size)
                val beside = usuals.map { usual ->
                    changes.any { change -> change.right <= usual.left && change.top < usual.bottom && change.bottom > usual.top }
                }
                assertEquals(1, beside.distinct().size, "${width}dp ×$fontScale: 어떤 칸만 두 줄이다")
                assertEquals(1, usuals.map { it.height }.distinct().size, "${width}dp ×$fontScale: 칸마다 글자 크기가 다르다")
            }
        }
    }

    // 평소 범위 안의 변화를 회색으로 두면 증감이 아니라 그냥 글자처럼 읽힌다(CLAUDE.md 변화량과 색)
    @Test
    fun `움직이지 않은 달라진 점 칸도 오르면 초록 내리면 빨강으로 칠한다`() = runComposeUiTest {
        var up: Color? = null
        var down: Color? = null
        var colors: OvalitColors? = null
        setContent {
            OvalitTheme {
                up = changeColor(DynamicSlot(DynamicMetric.KAST, Movement.STEADY), current = 0.88, usual = 0.86)
                down = changeColor(DynamicSlot(DynamicMetric.SURVIVAL_RATE, Movement.STEADY), current = 0.38, usual = 0.40)
                colors = OvalitTheme.colors
            }
        }

        assertEquals(colors?.pos, up)
        assertEquals(colors?.neg, down)
    }

    @Test
    fun `KDA 칸을 누르면 KDA 설명 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNode(SemanticsMatcher("KDA 칸") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "KDA 설명 보기" }).performClick()

        onNodeWithText("한 번 죽을 때마다 킬과 어시스트를 합쳐 몇 번 했는지예요. K/D와 달리 어시스트도 들어가요.").assertExists()
    }

    // CLAUDE.md: 총량을 더한 뒤 나눈다. 식에도 실제 합계를 적는다.
    @Test
    fun `KDA 시트는 킬과 어시를 더해 데스로 나눈 식을 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        val metrics = report.metrics
        setContent { Sheet(FixedMetric.KDA, report) }

        val kda = MetricFormat.TWO_DECIMALS.valueTextFor(assertNotNull(metrics.kda))
        onNodeWithText("(${metrics.kills}킬 + ${metrics.assists}어시) ÷ ${metrics.deaths}데스 = $kda").assertExists()
    }

    @Test
    fun `기타 모드는 K_D 헤드샷 KDA 세 칸을 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("전투점수", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("KDA", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `8주 흐름 입구를 누르면 흐름 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("지난 8주 흐름 한눈에 보기").performScrollTo().performClick()

        onNodeWithText("지난 8주 흐름").assertExists()
    }

    // 막대만으로는 전에 얼마였고 지금 얼마인지 안 보인다
    @Test
    fun `흐름 시트는 처음에 리포트 기간 값과 평소 값을 띄운다`() = runComposeUiTest {
        setContent { TrendSheet(ReportPreviewData.moved) }

        onNode(hasText("이번 주") and hasText("7경기 · 146라운드")).assertExists()
        onNode(hasText("피해량") and hasText("138") and hasText("평소 128")).assertExists()
    }

    @Test
    fun `흐름 막대를 누르면 모든 줄이 그 주 값으로 바뀐다`() = runComposeUiTest {
        setContent { TrendSheet(ReportPreviewData.moved) }

        tapWeek("피해량", index = 3)

        onNode(hasText("4주 전") and hasText("8/24 – 8/30") and hasText("7경기 · 150라운드")).assertExists()
        onNode(hasText("피해량") and hasText("126")).assertExists()
        onNode(hasText("K/D") and hasText("1.28")).assertExists()
        onNode(hasText("헤드샷") and hasText("20%")).assertExists()
    }

    @Test
    fun `고른 막대를 다시 누르면 리포트 기간으로 돌아간다`() = runComposeUiTest {
        setContent { TrendSheet(ReportPreviewData.moved) }

        tapWeek("K/D", index = 3)
        tapWeek("K/D", index = 3)

        onNode(hasText("피해량") and hasText("138")).assertExists()
        onNodeWithText("4주 전").assertDoesNotExist()
    }

    @Test
    fun `흐름 막대를 옆으로 끌면 손가락이 멈춘 주를 고른다`() = runComposeUiTest {
        setContent { TrendSheet(ReportPreviewData.moved) }

        onNodeWithContentDescription("주별 피해량", substring = true).performTouchInput {
            swipe(start = Offset(width * 0.5f / 8, centerY), end = Offset(width * 6.5f / 8, centerY))
        }

        onNodeWithText("지난주").assertExists()
        onNode(hasText("피해량") and hasText("133")).assertExists()
    }

    // 기록이 하나라도 있으면 그려야 견줄 수 있다. 내 기록이라 표본으로 가르지 않는다.
    @Test
    fun `라운드가 모자란 주도 다른 주처럼 그리고 고를 수 있다`() = runComposeUiTest {
        setContent { TrendSheet(ReportPreviewData.moved) }

        tapWeek("피해량", index = 2)

        onNode(hasText("5주 전") and hasText("1경기 · 22라운드")).assertExists()
        onNode(hasText("피해량") and hasText("142")).assertExists()
        onNodeWithText("40라운드", substring = true).assertDoesNotExist()
    }

    @Test
    fun `지표 설명 시트도 막대를 누르면 막대 위에 그 주 값을 띄운다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        tapWeek("피해량", index = 3)

        onNode(hasText("4주 전") and hasText("126") and hasText("7경기 · 150라운드")).assertExists()
        onNodeWithText("점선은 지난 4주 평균이에요.", substring = true).assertExists()
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
    fun `고정 칸은 모든 칸 폭이 같다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val widths = listOf("전투점수", "K/D", "피해량", "헤드샷", "KDA").map { onNodeWithText(it).getBoundsInRoot().let { bounds -> bounds.right - bounds.left } }

        // 남는 픽셀 하나는 어느 칸엔가 붙는다
        assertTrue(widths.max() - widths.min() <= 1.dp, "$widths")
    }

    // 오른쪽 위 아바타 하나로는 누르면 내 프로필이 열린다는 걸 알기 어렵다
    @Test
    fun `내 프로필 안내는 한 번 띄우고 누르면 닫는다`() = runComposeUiTest {
        var shown = 0
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = {},
                    badge = PlayerBadge("오발러#KR1", tier = 16, tierName = "플래티넘 2"),
                    profileHint = true,
                    onProfileHintShown = { shown++ },
                )
            }
        }

        onNodeWithText("내 프로필은 여기서 볼 수 있어요").assertExists()
        assertEquals(1, shown)

        onNodeWithText("내 프로필은 여기서 볼 수 있어요").performClick()
        onNodeWithText("내 프로필은 여기서 볼 수 있어요").assertDoesNotExist()
    }

    // 아바타가 위로 스크롤돼 가려지는 동안 잘린 폭이 0까지 줄면 화살표 자리가 음수가 되어 앱이 죽는다
    @Test
    fun `내 프로필 안내를 띄운 채 스크롤해도 머리 줄을 따라간다`() = runComposeUiTest {
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = {},
                    badge = PlayerBadge("오발러#KR1", tier = 16, tierName = "플래티넘 2"),
                    profileHint = true,
                )
            }
        }
        val before = onNodeWithText("내 프로필은 여기서 볼 수 있어요").getUnclippedBoundsInRoot().top

        repeat(3) { onRoot().performTouchInput { swipeUp() } }
        waitForIdle()

        val after = onNodeWithText("내 프로필은 여기서 볼 수 있어요").getUnclippedBoundsInRoot().top
        assertTrue(after < before, "안내가 머리 줄을 따라 올라가지 않았다")
    }

    @Test
    fun `내 프로필 안내를 띄운 적이 있으면 두지 않는다`() = runComposeUiTest {
        setContent {
            OvalitTheme {
                ReportScreen(
                    uiState = ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved),
                    onSelectQueue = {},
                    badge = PlayerBadge("오발러#KR1", tier = 16, tierName = "플래티넘 2"),
                )
            }
        }

        onNodeWithText("내 프로필은 여기서 볼 수 있어요").assertDoesNotExist()
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
        val fourth = onNodeWithText("첫 교전 관여율", useUnmergedTree = true).getBoundsInRoot()
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

    // 데스매치만 뛴 사람에게 "뛴 경기가 없어요"는 틀린 말이다. 리포트에 안 넣는 까닭을 같이 적는다.
    @Test
    fun `기타 칩에서 목록에만 두는 모드만 뛰었으면 뛴 경기가 없다고 하지 않는다`() = runComposeUiTest {
        setContent { Report(WeeklyReport.NotEnoughMatches(played = 0, notCounted = 3), queueFilter = QueueFilter.OTHER) }

        onNodeWithText("최근 4주 동안 뛴 경기가 없어요").assertDoesNotExist()
        onNodeWithText("리포트까지 5경기 남았어요").assertExists()
        onNodeWithText("규칙이 크게 다른 모드 3경기는 리포트에 넣지 않았어요", substring = true).assertExists()
    }

    @Test
    fun `고정 칸을 누르면 그 지표 설명 시트가 뜬다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("어떻게 계산하나요?").assertDoesNotExist()
        onNodeWithText("피해량").performClick()

        onNodeWithText("ADR").assertExists()
        onNodeWithText("어떻게 계산하나요?").assertExists()
    }

    // 칸마다 평소 값이 무엇과 견줬는지 말하니 칸 밖 안내는 두지 않는다. 비교할 기록이 없을 때만 그 까닭을 칸 밑에 적는다.
    @Test
    fun `고정 칸 밖에는 견준 기준을 따로 적지 않고 기준이 없을 때만 까닭을 적는다`() = runComposeUiTest {
        var report by mutableStateOf(ReportPreviewData.moved.copy(note = null))
        setContent { Report(report) }

        onNodeWithText("변화량은", substring = true).assertDoesNotExist()
        onNodeWithText("146라운드", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("비교할 기록이 모자라요").assertDoesNotExist()
        report = report.copy(baseline = null)
        onNodeWithText("비교할 기록이 모자라요").assertExists()
    }

    @Test
    fun `고정 칸 시트에는 칸 밑에서 뺀 평균을 적는다`() = runComposeUiTest {
        setContent { Sheet(FixedMetric.DAMAGE, ReportPreviewData.moved) }

        onNodeWithText("지난 4주 평균 128").assertExists()
    }

    // "라운드 153" 같은 표본은 칸에 두지 않고 시트에서 풀어 적는다
    @Test
    fun `달라진 점 칸에는 평소 값만 두고 표본은 적지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(note = null)) }

        // 고정 칸 다섯과 달라진 점 칸마다 하나씩이다
        onAllNodesWithText("평소 ", substring = true, useUnmergedTree = true)
            .assertCountEquals(FixedMetric.entries.size + ReportPreviewData.moved.dynamic.size)
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
        onNodeWithText("위 숫자는 이 비율을 셀 때 쓴 횟수예요. 이번 기간과 견주는 주가 모두 40라운드 이상이어야 비교해요.").assertExists()
        onNodeWithText("이번 변화가 지난 8주 동안 주마다 흔들린 폭의 1.5배를 넘어서 달라졌다고 봤어요.").assertExists()
    }

    // 판단을 보류한 칸은 달라졌다고도, 그대로라고도 하지 않는다. 첫 킬 승률은 moved의 동적 칸에 없는 지표라 판단 보류로 연다.
    @Test
    fun `판단하지 않은 칸의 시트는 기록이 모자라다고 적는다`() = runComposeUiTest {
        setContent { OvalitTheme { DynamicMetricSheetBody(DynamicMetric.FIRST_KILL_WIN_RATE, ReportPreviewData.moved) } }

        onNodeWithText("달라졌는지 판단하지 않았어요", substring = true).assertExists()
        onNodeWithText("첫\u00a0킬\u00a010번 이상이어야", substring = true).assertExists()
    }

    // 분모가 데스라 라운드가 아니라 데스 수를 표본으로 적는다. 우리 팀 움직임도 들어가는 숫자라는 걸 밝힌다.
    @Test
    fun `트레이드 받은 데스 시트는 데스 수를 표본으로 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        setContent { OvalitTheme { DynamicMetricSheetBody(DynamicMetric.TRADED_DEATH_RATE, report) } }

        onNodeWithText("데스\u00a0${report.metrics.deaths}번").assertExists()
        onNodeWithText("데스\u00a040번 이상이어야", substring = true).assertExists()
    }

    @Test
    fun `트레이드 받은 데스 비율이 움직이면 달라진 점 칸에 띄운다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(dynamic = it.dynamic + DynamicSlot(DynamicMetric.TRADED_DEATH_RATE, Movement.MOVED)) }
        setContent { Report(report) }

        onNodeWithText("트레이드 받은 데스", useUnmergedTree = true).assertExists()
    }

    // 프리뷰 데이터는 피해량이 128 → 138로 올랐다. 그 변화를 밴달과 제트가 가장 많이 끌었다.
    @Test
    fun `짚을 점은 움직인 지표와 같은 쪽 무기와 요원을 숫자로 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올랐어요").assertExists()
        // 평균이 얼마였는지는 고정 칸에 이미 있어 다시 적지 않는다
        onNodeWithText("→ 이번 주", substring = true).assertDoesNotExist()
        onNode(noteRow("밴달", "118 → 140")).assertExists()
        onNode(noteRow("제트", "124 → 146")).assertExists()
        // 줄마다 "끌어올린 무기" 같은 이름표나 지표 이름을 붙이지 않는다
        onNodeWithText("끌어올린", substring = true, useUnmergedTree = true).assertDoesNotExist()
        // 이긴 판이 더 많았던 요원은 바로 밑 이번 주 요원 칸의 승패와 겹쳐 적지 않는다
        onNodeWithText("이긴 판이 더 많았던", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 그 주에 무엇이 달라졌는지가 숫자보다 먼저 읽히게 짚을 점을 고정 칸 위에 둔다.
    // 근거 줄은 떼어 두면 무엇의 숫자인지 안 읽혀서 헤드라인에 붙인다.
    // 둘 다 제목과 선 없이 고정 칸과 한 카드에 둔다.
    @Test
    fun `짚을 점 헤드라인과 무기·요원 줄은 승패 칸과 고정 칸 사이에 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        onNodeWithText("짚을 점", substring = true).assertDoesNotExist()
        val record = onNodeWithText("5승 2패", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headline = onNodeWithText("피해량이 평소보다 10 올랐어요").getUnclippedBoundsInRoot()
        val firstCell = onNodeWithText("피해량").getUnclippedBoundsInRoot()
        val lastCell = onNodeWithText("헤드샷").getUnclippedBoundsInRoot()
        val weapon = onNode(noteRow("밴달", "118 → 140")).getUnclippedBoundsInRoot()
        val trend = onNodeWithText("지난 8주 흐름", substring = true).getUnclippedBoundsInRoot()

        assertTrue(headline.top >= record.bottom, "헤드라인이 승패 칸 위에 있다")
        assertTrue(weapon.top >= headline.bottom, "무기 줄이 헤드라인 위에 있다")
        assertTrue(firstCell.top >= weapon.bottom, "무기 줄이 고정 칸 밑에 있다")
        assertTrue(trend.top >= lastCell.bottom, "흐름 입구가 고정 칸 위에 있다")
        assertTrue(onNodeWithText("달라진 점").getUnclippedBoundsInRoot().top >= trend.bottom, "짚을 점이 고정 칸 카드 밖에 있다")
    }

    @Test
    fun `짚을 점이 없으면 승패 칸 바로 밑에 고정 칸을 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(note = null), catalog = NamedCatalog) }

        onNodeWithText("평소보다", substring = true).assertDoesNotExist()
        onNode(noteRow("밴달", "118 → 140")).assertDoesNotExist()
        onNodeWithText("피해량").assertExists()
    }

    // 최고 기록은 따로 줄을 두지 않고 헤드라인 한 문장에 넣는다
    @Test
    fun `이번 액트 주간 최고면 헤드라인에 적는다`() = runComposeUiTest {
        val note = assertNotNull(ReportPreviewData.moved.note).copy(previousBest = 135.4)
        setContent { Report(ReportPreviewData.moved.copy(note = note), catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올라 이번 액트 최고예요").assertExists()
        onNodeWithText("이전 최고", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 138과 137.6은 둘 다 138로 보인다. 그보다 높다고 하면 틀려 보인다.
    @Test
    fun `보이는 자릿수로 이전 최고와 같으면 최고라고 하지 않는다`() = runComposeUiTest {
        val note = assertNotNull(ReportPreviewData.moved.note).copy(previousBest = 137.6)
        setContent { Report(ReportPreviewData.moved.copy(note = note), catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올랐어요").assertExists()
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
        onNode(noteRow("이코 라운드 비중", "16% → 31%")).assertExists()
        onNode(noteRow("풀바이 라운드만 보면", "158 → 156")).assertExists()
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

        onNode(noteRow("팬텀 비중", "6% → 27%")).assertExists()
        onNode(noteRow("밴달을 든 라운드만 보면", "24% → 23%")).assertExists()
        shown = note(MixGroup.Agent(Jett), MixGroup.Agent(Raze))
        onNode(noteRow("제트 비중", "6% → 27%")).assertExists()
        onNode(noteRow("레이즈로 뛴 판만 보면", "24% → 23%")).assertExists()
    }

    @Test
    fun `비중이 줄었으면 숫자로 줄어든 걸 보인다`() = runComposeUiTest {
        val note = WeekNote(
            moved = MovedMetric(FixedMetric.DAMAGE, current = 131.0, usual = 147.0),
            mix = MixShift(MixGroup.Buy(BuyType.FULL_BUY), share = 0.52, usualShare = 0.68, steady = null),
        )
        setContent { Report(ReportPreviewData.moved.copy(note = note)) }

        onNode(noteRow("풀바이 라운드 비중", "68% → 52%")).assertExists()
        onNodeWithText("만\u00a0보면", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // KDA에도 고정 칸처럼 보이는 두 자리끼리 뺀 변화량을 붙인다
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

        onNodeWithText("공격에서 멀티킬 라운드 비율이 수비보다 25%p 높아요").assertExists()
        onNode(insightCell("공격", "30%", "40라운드")).assertExists()
        onNode(insightCell("수비", "5%", "40라운드")).assertExists()
        onNodeWithText("에임 올리기를 고르셔서 먼저 봤어요").assertExists()
    }

    // 두 쪽 모두 이름이 있으면 판 수와 상관없이 높은 쪽이 주어다
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

        onNodeWithText("제트로 뛴 판은 생존율이 레이즈보다 25%p 높아요").assertExists()
        onNode(insightCell("제트", "75%", "5판")).assertExists()
        onNode(insightCell("레이즈", "50%", "10판")).assertExists()
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

        onNodeWithText("레이즈로 뛴 판은 관여율이 다른 타격대 요원보다 14%p 낮아요").assertExists()
    }

    // 역할끼리는 승률만 견주고, 우연을 넘을 만큼 차이가 클 때만 나온다. "추천"은 쓰지 않는다.
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

        onNodeWithText("전략가로 뛴 판은 승률이 타격대보다 44%p 높아요").assertExists()
        onNodeWithText("추천", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // 연달아 뛸수록 어떤지 숫자로만 적는다. 쉬라고 하지 않는다.
    @Test
    fun `연달아 뛴 판을 견준 개선 포인트는 세 번째 판부터를 주어로 적는다`() = runComposeUiTest {
        val insight = Insight(
            metric = InsightMetric.KAST,
            lead = InsightPart(InsightSubject.LateInSession, value = 0.61, matches = 18, rounds = 420),
            other = InsightPart(InsightSubject.EarlyInSession, value = 0.73, matches = 32, rounds = 760),
            leadIsHigher = false,
            isRolePriority = false,
            recent = InsightRecent(lead = 0.55, other = 0.70),
        )
        setContent { Report(ReportPreviewData.moved.copy(insight = insight)) }

        onNodeWithText("연달아 뛴 세 번째 판부터는 관여율이 첫 두 판보다 12%p 낮아요").assertExists()
        onNode(insightCell("세 번째 판부터", "61%", "18판")).assertExists()
        onNode(insightCell("첫 두 판", "73%", "32판")).assertExists()
        onNodeWithText(recentText("이번 주", "55%"), useUnmergedTree = true).assertExists()
        onNodeWithText(recentText("이번 주", "70%"), useUnmergedTree = true).assertExists()
        onNodeWithText("쉬", substring = true, useUnmergedTree = true).assertDoesNotExist()
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

        onNodeWithText("헤이븐에서는 관여율이 다른 맵보다 14%p 낮아요").assertExists()
        onNode(insightCell("헤이븐", "58%", "9판")).assertExists()
        onNode(insightCell("다른 맵", "72%", "41판")).assertExists()
        insight = rifles
        onNodeWithText("밴달을 든 라운드는 헤드샷이 다른 소총보다 11%p 낮아요").assertExists()
        onNode(insightCell("밴달", "14%", "180라운드")).assertExists()
        onNode(insightCell("다른 소총", "25%", "212라운드")).assertExists()
        insight = twoRifles
        onNodeWithText("팬텀을 든 라운드는 헤드샷이 밴달보다 11%p 높아요").assertExists()
    }

    // 근거 두 칸을 한 줄에 나란히 두어 고정 칸이 덜 내려가게 한다
    @Test
    fun `짚을 점 무기와 요원은 한 줄에 나란히 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        val weapon = onNode(noteRow("밴달", "118 → 140")).getUnclippedBoundsInRoot()
        val agent = onNode(noteRow("제트", "124 → 146")).getUnclippedBoundsInRoot()
        assertTrue(agent.left > weapon.right, "요원 칸이 무기 칸 오른쪽에 있어야 한다")
        assertEquals((weapon.top + weapon.bottom) / 2, (agent.top + agent.bottom) / 2)
    }

    // 반쪽에 안 들어가면 한 줄에 하나씩 두고, 그림 폭이 달라도 이름은 같은 자리에서 시작한다
    @Test
    fun `짚을 점 칸이 반쪽에 안 들어가면 한 줄에 하나씩 두고 이름을 맞춘다`() = runComposeUiTest {
        setContent { Box(Modifier.width(280.dp)) { Report(ReportPreviewData.moved, catalog = NamedCatalog) } }

        val weaponName = onNode(noteName("밴달", "118 → 140"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val agentName = onNode(noteName("제트", "124 → 146"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val weaponChange = onNodeWithText(noteChange("118 → 140"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headline = onNodeWithText("피해량이 평소보다 10 올랐어요").getUnclippedBoundsInRoot()
        assertTrue(agentName.top >= weaponName.bottom, "요원 칸이 무기 칸 밑에 있어야 한다")
        assertEquals(weaponName.left, agentName.left)
        // 그림 자리를 두어 이름이 헤드라인보다 안쪽에서 시작한다
        assertTrue(weaponName.left > headline.left)
        assertTrue(weaponChange.left > weaponName.right, "숫자가 이름 옆에 있어야 한다")
    }

    // 오르내림은 헤드라인이 말한다. 줄은 모두 같은 쪽으로 움직인 것이라 색 대신 밝기와 굵기로 이번 값만 띄운다.
    @Test
    fun `짚을 점 숫자는 이번 값만 굵게 두고 색을 입히지 않는다`() = runComposeUiTest {
        lateinit var colors: OvalitColors
        setContent {
            OvalitTheme {
                colors = OvalitTheme.colors
                ReportScreen(ReportUiState.Success(QueueFilter.COMPETITIVE_AND_UNRATED, ReportPreviewData.moved), onSelectQueue = {}, catalog = NamedCatalog)
            }
        }

        val text = onNodeWithText(noteChange("118 → 140"), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val current = text.spanStyles.single { text.substring(it.start, it.end) == "140" }.item
        val usual = text.spanStyles.single { text.substring(it.start, it.end).startsWith("118") }.item
        assertEquals(FontWeight.SemiBold, current.fontWeight)
        assertEquals(colors.t1, current.color)
        assertEquals(colors.t3, usual.color)
        assertTrue(text.spanStyles.none { it.item.color == colors.pos || it.item.color == colors.neg })
    }

    // 좁은 화면이나 큰 글꼴에서 숫자가 이름을 덮지 않게 모든 줄의 숫자를 이름 밑으로 같이 내린다
    @Test
    fun `짚을 점 줄이 좁으면 모든 줄의 숫자를 이름 밑으로 내린다`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2f)) {
                Box(Modifier.width(260.dp)) { Report(ReportPreviewData.moved, catalog = NamedCatalog) }
            }
        }

        listOf("밴달" to "118 → 140", "제트" to "124 → 146")
            .forEach { (label, rawChange) ->
                val change = noteChange(rawChange)
                val name = onNode(noteName(label, rawChange), useUnmergedTree = true).getUnclippedBoundsInRoot()
                val value = onNodeWithText(change, useUnmergedTree = true).getUnclippedBoundsInRoot()
                assertTrue(value.top >= name.bottom, "$change 이 이름 밑으로 내려가지 않았다")
                assertEquals(name.left, value.left)
            }
    }

    // CLAUDE.md 지켜야 할 선: 게임 결정을 대신하지 않는다
    @Test
    fun `짚을 점은 무엇을 쓰라고 하지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved, catalog = NamedCatalog) }

        onNodeWithText("추천", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("쓰세요", substring = true, useUnmergedTree = true).assertDoesNotExist()
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

        onNodeWithText("공격에서 피해량이 수비보다 22 높아요").assertExists()
        onNode(insightCell("공격", "143", "70라운드")).assertExists()
        onNode(insightCell("수비", "121", "76라운드")).assertExists()
    }

    // 무기 값도 보이는 자릿수로 같으면 "140 → 140"이 돼서 무기 줄을 두지 않는다
    @Test
    fun `보이는 자릿수로 같은 무기는 짚지 않는다`() = runComposeUiTest {
        val note = assertNotNull(ReportPreviewData.moved.note)
        val flat = note.copy(weapon = note.weapon?.copy(current = 140.2, usual = 139.8))
        setContent { Report(ReportPreviewData.moved.copy(note = flat), catalog = NamedCatalog) }

        onNodeWithText("피해량이 평소보다 10 올랐어요").assertExists()
        onNode(noteRow("밴달", "140 → 140")).assertDoesNotExist()
        onNode(noteRow("제트", "124 → 146")).assertExists()
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
        onNodeWithText("세로선은 액트가 바뀐 곳이에요.", substring = true).assertExists()
    }

    @Test
    fun `데스가 없으면 K_D 계산식 대신 비워 둔 이유를 적는다`() = runComposeUiTest {
        val report = ReportPreviewData.moved.let { it.copy(metrics = it.metrics.copy(deaths = 0)) }
        setContent { Sheet(FixedMetric.KD, report) }

        onNodeWithText("데스가 없어서", substring = true).assertExists()
        onNodeWithText("킬 ÷", substring = true).assertDoesNotExist()
    }

    // 위 칸들은 모두 이번 주 숫자라, 이번 액트로 견준 문장은 제목으로 떼고 몇 경기로 견줬는지 적는다
    @Test
    fun `개선 포인트는 이번 액트 돌아보기 묶음으로 달라진 점과 이번 주 요원 사이에 둔다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val dynamic = onNodeWithText("달라진 점").getUnclippedBoundsInRoot()
        val title = onNodeWithText("이번 액트 돌아보기").getUnclippedBoundsInRoot()
        val headline = onNodeWithText("공격에서 첫 교전 승률이 수비보다 14%p 높아요").getUnclippedBoundsInRoot()
        val agents = onNodeWithText("이번 주 요원", useUnmergedTree = true).getUnclippedBoundsInRoot()
        onNodeWithText("33경기", useUnmergedTree = true).assertExists()
        assertTrue(dynamic.bottom <= title.top && title.bottom <= headline.top && headline.bottom <= agents.top)
    }

    // 광고는 이번 주 숫자를 다 본 바로 뒤에 둔다. 기간, 고정 칸, 달라진 점 사이에는 두지 않는다.
    @Test
    fun `홈 광고는 달라진 점과 이번 액트 돌아보기 사이에 둔다`() = runComposeUiTest {
        setContent { CompositionLocalProvider(LocalAdRenderer provides LabelAds) { Report(ReportPreviewData.moved) } }

        val fixed = onNodeWithText("전투점수", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val dynamic = onNodeWithText("달라진 점", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val ad = onNodeWithText("광고 HOME", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val insight = onNodeWithText("이번 액트 돌아보기", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(fixed.bottom <= dynamic.top && dynamic.bottom <= ad.top && ad.bottom <= insight.top)
    }

    // 한 주 경기를 둘로 나누면 우연한 차이가 대부분이라 이번 액트 경기로 견준다
    @Test
    fun `개선 포인트는 이번 액트 공수를 높은 쪽부터 사실만 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        onNodeWithText("공격에서 첫 교전 승률이 수비보다 14%p 높아요").assertExists()
        onNode(insightCell("공격", "58%", "388라운드")).assertExists()
        onNode(insightCell("수비", "44%", "388라운드")).assertExists()
        onNodeWithText("타격대에게 첫 교전 승률은 먼저 보는 지표예요").assertExists()
    }

    // 두 쪽을 칸으로 나란히 두고, 칸 안에서 액트 값 밑에 이번 주 값을 붙인다. 까닭 줄은 칸 밑이다.
    @Test
    fun `개선 포인트는 두 쪽을 나란한 칸에 두고 이번 주 값을 그 밑에 붙인다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved) }

        val attack = onNode(insightCell("공격", "58%", "388라운드")).getUnclippedBoundsInRoot()
        val defense = onNode(insightCell("수비", "44%", "388라운드")).getUnclippedBoundsInRoot()
        val act = onNodeWithText("공격\u00a058%", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val recent = onNodeWithText(recentText("이번 주", "71%"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val reason = onNodeWithText("타격대에게 첫 교전 승률은 먼저 보는 지표예요").getUnclippedBoundsInRoot()
        assertEquals(attack.top, defense.top)
        assertTrue(attack.right <= defense.left)
        assertTrue(act.bottom <= recent.top && recent.bottom <= attack.bottom)
        assertTrue(attack.bottom <= reason.top)
    }

    // 기간을 넓혔으면 그 기간 이름으로 적는다
    @Test
    fun `이번 주 값은 리포트 기간 이름으로 적는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.steady.copy(insight = ReportPreviewData.moved.insight)) }

        onNodeWithText(recentText("최근 2주", "71%"), useUnmergedTree = true).assertExists()

        onNodeWithText(recentText("최근 2주", "45%"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `이번 주 값이 없으면 그 줄을 두지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.let { it.copy(insight = it.insight?.copy(recent = null)) }) }

        onNodeWithText(recentText("이번 주", "71%"), useUnmergedTree = true).assertDoesNotExist()

        onNodeWithText(recentText("이번 주", "45%"), useUnmergedTree = true).assertDoesNotExist()
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

        onNodeWithText("에임 올리기를 고르셔서 먼저 봤어요").assertExists()
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

        onNodeWithText("수비에서 첫 교전 승률이 공격보다 14%p 높아요").assertExists()
    }

    @Test
    fun `견줄 만한 격차가 없으면 이번 액트 돌아보기 묶음을 두지 않는다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.moved.copy(insight = null)) }

        onNodeWithText("이번 액트 돌아보기", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `기타 모드에는 개선 포인트가 없다`() = runComposeUiTest {
        setContent { Report(ReportPreviewData.otherQueue, QueueFilter.OTHER) }

        onNodeWithText("이번 액트 돌아보기", useUnmergedTree = true).assertDoesNotExist()
    }

    // 화면에 보이는 자릿수로 겨룬다. 나는 K/D 1.34, 피해량 138, 헤드샷 21%다. 전투점수는 같아서 앞서지 않는다.
    @Test
    fun `라이벌 칸은 홈 고정 칸 다섯 지표 중 앞선 개수를 센다`() = runComposeUiTest {
        val rival = ReportPreviewData.moved.metrics.let { it.copy(kills = 100, damage = 21_000, shots = it.shots.copy(head = 60)) }
        setContent { Social(rival = FriendStanding(PlayerId("junho"), "준호#KR1", rival)) }

        onNodeWithText("라이벌 · 준호#KR1").assertExists()
        onNodeWithText("5개 중 3개 앞섬").assertExists()
        // 고정 칸의 KDA와 라이벌 줄의 KDA다
        onAllNodesWithText("KDA", useUnmergedTree = true).assertCountEquals(2)
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

    // 라이벌 줄처럼 11dp씩 띄우고, 제목 옆에 지표 버튼이 있어도 제목에서 첫 줄까지는 다른 묶음처럼 12dp다
    @Test
    fun `친구 비교는 다른 묶음과 같은 간격으로 줄을 세운다`() = runComposeUiTest {
        val strong = ReportPreviewData.moved.metrics.copy(damage = 30_000)
        setContent { Social(friends = listOf(FriendStanding(PlayerId("junho"), "준호#KR1", strong))) }

        val title = onNodeWithText("친구 비교").getUnclippedBoundsInRoot()
        val junho = onNodeWithText("준호", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val me = onNodeWithText("나", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(12f, (junho.top - title.bottom).value, absoluteTolerance = 0.5f)
        assertEquals(11f, (me.top - junho.bottom).value, absoluteTolerance = 0.5f)
    }

    // 제목 줄은 제목 글자 높이로 두고 버튼의 눌리는 영역(44dp)은 위아래로 넘친다. 넘친 쪽 끝을 눌러도 열려야 한다.
    @Test
    fun `친구 비교 지표 버튼은 제목 줄 밖으로 넘친 곳을 눌러도 열린다`() = runComposeUiTest {
        val strong = ReportPreviewData.moved.metrics.copy(damage = 30_000)
        setContent { Social(friends = listOf(FriendStanding(PlayerId("junho"), "준호#KR1", strong))) }
        val button = onNode(
            SemanticsMatcher("지표 버튼") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "피해량, 비교할 지표 바꾸기" },
        )

        button.performScrollTo()
        val title = onNodeWithText("친구 비교").getUnclippedBoundsInRoot()
        assertTrue(button.getUnclippedBoundsInRoot().top < title.top)
        button.performTouchInput { click(Offset(centerX, 2f)) }
        onNodeWithText("비교할 지표").assertExists()
    }

    // 친구 비교도 홈 고정 칸 다섯 가운데 골라 줄을 세운다
    @Test
    fun `친구 비교는 KDA로도 줄을 세운다`() = runComposeUiTest {
        val mine = ReportPreviewData.moved.metrics
        val strong = mine.copy(assists = mine.assists + 300)
        setContent { Social(friends = listOf(FriendStanding(PlayerId("junho"), "준호#KR1", strong))) }

        onNode(SemanticsMatcher("지표 버튼") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "피해량, 비교할 지표 바꾸기" })
            .performScrollTo()
            .performClick()
        onNode(hasText("KDA") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()

        val strongKda = MetricFormat.TWO_DECIMALS.format(assertNotNull(strong.kda))
        onNodeWithText(strongKda, useUnmergedTree = true).assertExists()
        val junho = onNodeWithText("준호", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        val me = onNodeWithText("나", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        assertTrue(junho < me, "KDA가 높은 준호가 위에 있어야 한다")
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

        onNodeWithText("같이 뛰는 친구를 초대해 보세요").performScrollTo().performClick()

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

    // 친구가 수백 명이어도 홈 카드는 다섯 줄과 내 줄로 끝난다(docs/screens.md). 나는 피해량 138이라 151등이다.
    @Test
    fun `친구가 많으면 친구 비교는 위 다섯 줄과 내 줄만 두고 전체 보기로 넘어간다`() = runComposeUiTest {
        var opened: Pair<QueueFilter, FixedMetric>? = null
        setContent { Social(friends = ManyFriends, onOpenFriendRanking = { queue, metric -> opened = queue to metric }) }

        onNodeWithText("친구5", useUnmergedTree = true).assertExists()
        onNodeWithText("친구6", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("151", useUnmergedTree = true).assertExists()
        val fifth = onNodeWithText("친구5", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val me = onNodeWithText("나", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 바로 붙이면 6등처럼 읽혀서 내 줄 앞을 한 줄 넘게 띄운다
        assertTrue(me.top - fifth.bottom > 11.dp * 2, "내 줄 앞이 띄워져 있지 않다")

        onNodeWithText("전체 151명 보기").performScrollTo().performClick()

        assertEquals(QueueFilter.COMPETITIVE_AND_UNRATED to FixedMetric.DAMAGE, opened)
    }

    @Test
    fun `친구 비교 줄이 다섯 줄과 내 줄 안이면 전체 보기를 두지 않는다`() = runComposeUiTest {
        setContent { Social(friends = ManyFriends.take(5)) }

        onNodeWithText("친구5", useUnmergedTree = true).assertExists()
        // 내 줄은 6등이고 빠진 줄이 없어 바로 붙는다
        onNode(hasText("나") and hasText("6")).assertExists()
        onNodeWithText("명 보기", substring = true).assertDoesNotExist()
    }

    @Test
    fun `전체 순위는 친구 150명을 모두 세우고 보이는 줄만 그린다`() = runComposeUiTest {
        val report = ReportPreviewData.moved
        setContent {
            OvalitTheme {
                FriendRankingScreen(
                    FriendRankingUiState.Success(report.period, report.metrics, ManyFriends),
                    initialMetric = FixedMetric.DAMAGE,
                    onBack = {},
                )
            }
        }

        onNodeWithText("이번 주 · 151명").assertExists()
        onNodeWithText("친구1", useUnmergedTree = true).assertExists()
        onNodeWithText("친구150", useUnmergedTree = true).assertDoesNotExist()

        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("나"))

        // 피해량이 151인 친구도 있어서 내 줄 안의 등수로 본다
        onNode(hasText("나") and hasText("151")).assertExists()
        onNodeWithText("친구150", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `라이벌 고르기 시트는 친구가 많아도 열리고 끝까지 내려 고른다`() = runComposeUiTest {
        var picked: PlayerId? = null
        setContent { Social(friends = ManyFriends, nudge = HomeNudge.PICK_RIVAL, onSelectRival = { picked = it }) }

        onNodeWithText("라이벌을 골라 보세요").performScrollTo().performClick()
        onNodeWithText("친구1#KR1").assertExists()
        onNodeWithText("친구150#KR1").assertDoesNotExist()

        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("친구150#KR1"))
        onNodeWithText("친구150#KR1").performClick()

        assertEquals(PlayerId("f150"), picked)
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

// 받은 광고 대신 자리 이름을 그린다
private object LabelAds : AdRenderer {
    @Composable
    override fun Render(placement: AdPlacement, key: String, frame: @Composable (content: @Composable () -> Unit) -> Unit) {
        frame { OvalitText("광고 ${placement.name}") }
    }

    override val canOfferAdFree = false

    override fun offerAdFree() = Unit
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

// 개선 포인트 밑 줄은 기간 이름(액트 줄은 제목에 있어 없음) 뒤에 두 쪽을 (이름, 값, 표본)으로 잇는다. 줄은 두 쪽 사이에서만 바뀐다.
// 개선 포인트 칸 하나는 "공격 58%", 표본, 이번 주 값을 묶어 화면 읽기 프로그램이 한 번에 읽는다
private fun insightCell(name: String, value: String, sample: String): SemanticsMatcher =
    hasText("$name\u00a0$value") and hasText(sample)

private fun recentText(period: String, value: String) = period.keepTogether() + "\u00a0" + value

// 짚을 점 한 줄은 이름과 표본을 한 글자로, "평소 → 이번" 숫자를 또 한 글자로 둔다. 화면 읽기 프로그램이 한 번에 읽게 한 줄로 묶는다.
private fun noteRow(name: String, change: String): SemanticsMatcher = hasText(noteLabel(name)) and hasText(noteChange(change))

private fun noteLabel(name: String) = name.keepTogether()

// 이번 주 무기·요원 칸에도 같은 이름이 있어서 짚을 점 칸의 이름은 옆의 숫자로 가린다
private fun noteName(name: String, change: String): SemanticsMatcher =
    hasText(noteLabel(name)) and hasAnySibling(hasText(noteChange(change)))

private fun noteChange(change: String) = change.keepTogether()

// 높은 공격과 낮은 수비를 (값, 라운드)로 받아 공수 개선 포인트를 만든다
private fun sideInsight(metric: InsightMetric, lead: Pair<Double, Int>, other: Pair<Double, Int>, focus: Focus? = null) = Insight(
    metric = metric,
    lead = InsightPart(InsightSubject.OnSide(Side.ATTACK), value = lead.first, matches = 7, rounds = lead.second),
    other = InsightPart(InsightSubject.OnSide(Side.DEFENSE), value = other.first, matches = 7, rounds = other.second),
    leadIsHigher = true,
    isRolePriority = false,
    focus = focus,
)

private fun SemanticsNodeInteractionsProvider.fontSizeOf(text: String): TextUnit {
    val layouts = mutableListOf<TextLayoutResult>()
    onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single().layoutInput.style.fontSize
}

// 전투점수 칸 숫자 밑 한 줄의 변화량과 평소 값이다
private fun combatSubLine(report: WeeklyReport.Ready): Pair<String, String> {
    val current = assertNotNull(report.metrics.acs)
    val usual = assertNotNull(report.baseline?.metrics?.acs)
    return MetricFormat.INTEGER.formatChange(current, usual) to "평소 ${MetricFormat.INTEGER.format(usual)}"
}

// 화면에 뜨는 자릿수로 적은 값이다. valueText는 컴포저블이라 테스트에서는 format으로 같은 글자를 만든다.
private fun MetricFormat.valueTextFor(value: Double): String = if (this == MetricFormat.PERCENT) "${format(value)}%" else format(value)

@Composable
private fun TrendSheet(report: WeeklyReport.Ready) {
    OvalitTheme { TrendSheetBody(report, FixedMetric.entries) }
}

// 막대 줄을 여덟 칸으로 나눠 [index]번째 칸 가운데를 누른다
private fun SemanticsNodeInteractionsProvider.tapWeek(label: String, index: Int) {
    onAllNodesWithContentDescription("주별 $label", substring = true).onFirst().performTouchInput {
        click(Offset(width * (index + 0.5f) / 8, centerY))
    }
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
    onOpenFriendRanking: (QueueFilter, FixedMetric) -> Unit = { _, _ -> },
) {
    val report = if (queueFilter == QueueFilter.OTHER) ReportPreviewData.otherQueue else ReportPreviewData.moved
    OvalitTheme {
        ReportScreen(
            uiState = ReportUiState.Success(queueFilter, report, rival = rival, friends = friends, nudge = nudge),
            onSelectQueue = {},
            onShareInvite = onShareInvite,
            onSelectRival = onSelectRival,
            onOpenFriendRanking = onOpenFriendRanking,
        )
    }
}

// 모두 나(피해량 138)보다 피해량이 높은 친구 150명이다. 친구1이 가장 높다.
private val ManyFriends = ReportPreviewData.manyFriends(150)

// "+13", "−0.10", "+3%p"처럼 부호로 시작하는 변화량 글자다
private val ChangeText = Regex("^[+−-]\\d.*")
