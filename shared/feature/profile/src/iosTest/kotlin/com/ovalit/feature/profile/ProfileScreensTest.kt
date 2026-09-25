package com.ovalit.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileScreensTest {

    @Test
    fun `내 프로필은 요원과 무기 칸을 누르면 들어간다`() = runComposeUiTest {
        var agentsOpened = false
        var weaponsOpened = false
        setContent {
            Themed { ProfileScreen(ProfilePreviewData.success, {}, { agentsOpened = true }, { weaponsOpened = true }, {}, {}) }
        }

        onNodeWithText("주로 전략가", useUnmergedTree = true).assertExists()
        onNodeWithText("전략가 65%").performScrollTo().performClick()
        onNodeWithText("무기").performScrollTo().performClick()

        assertTrue(agentsOpened && weaponsOpened)
    }

    @Test
    fun `티어 칸에는 경쟁전 승패와 티어 흐름을 둔다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, {}, {}) } }

        onNodeWithText("플래티넘 2").assertExists()
        onNodeWithText("경쟁 32판 · 18승 14패").assertExists()
        onNodeWithText("56%", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("이번 액트 경쟁전 티어 흐름. 첫 경기 플래티넘 1, 최고 플래티넘 3, 지금 플래티넘 2")
            .assertExists()
    }

    // 티어 칸에 크게 두니 이름 줄에서는 뺀다. 경쟁전이 없으면 티어 칸이 없어 이름 줄에 남긴다.
    @Test
    fun `경쟁전이 없으면 티어를 이름 줄에 둔다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.noCompetitive, {}, {}, {}, {}, {}) } }

        onNodeWithText("경쟁 32판 · 18승 14패").assertDoesNotExist()
        onNodeWithText("플래티넘 2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `통계와 맞힌 부위는 이번 액트 합계로 보여준다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, {}, {}) } }

        onNodeWithText("경쟁 + 일반 50경기").assertExists()
        onNodeWithText("188", useUnmergedTree = true).assertExists()
        onNodeWithText("1.12", useUnmergedTree = true).assertExists()
        onNodeWithText("17.2", useUnmergedTree = true).assertExists()
        onNodeWithText("31시간", useUnmergedTree = true).assertExists()
        onNodeWithText("맞힌 탄 1,842발").assertExists()
        onNodeWithText("24%", useUnmergedTree = true).assertExists()
        onNodeWithText("442", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `최근 경기를 누르면 그 경기가 열리고 전체 보기는 경기 탭을 연다`() = runComposeUiTest {
        var opened: MatchId? = null
        var allOpened = false
        setContent {
            Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, { opened = it }, { allOpened = true }) }
        }

        onNodeWithText("전체 보기").performScrollTo().performClick()
        onAllNodesWithText("13 – 9", substring = true)[0].performScrollTo().performClick()

        assertTrue(allOpened)
        assertEquals(MatchId("ascent"), opened)
    }

    @Test
    fun `요원 화면은 주 역할을 무엇으로 보는지 알려준다`() = runComposeUiTest {
        setContent { Themed { AgentsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("전략가는 관여율과 생존율로 봐요. 퍼블이 적은 건 역할상 자연스러워요.").assertExists()
    }

    @Test
    fun `주 역할이 전략가면 관여율과 생존 열을 둔다`() = runComposeUiTest {
        setContent { Themed { AgentsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("관여율").assertExists()
        onNodeWithText("생존").assertExists()
        onNodeWithText("퍼블 관여").assertDoesNotExist()
    }

    @Test
    fun `주 역할이 타격대면 퍼블 쪽 열로 바꾼다`() = runComposeUiTest {
        setContent {
            Themed { AgentsScreen(ProfilePreviewData.success.copy(agents = ProfilePreviewData.duelistAgents), onBack = {}) }
        }

        onNodeWithText("퍼블 관여").assertExists()
        onNodeWithText("첫 교전").assertExists()
        onNodeWithText("생존").assertDoesNotExist()
    }

    @Test
    fun `5판에 못 미친 요원은 숫자 대신 표본 부족이라고 적는다`() = runComposeUiTest {
        setContent { Themed { AgentsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("킬조이", substring = true).assertExists()
        onNodeWithText("표본 부족").assertExists()
    }

    // 카탈로그는 패치 뒤에 사람이 갱신해서 새 요원이 한동안 빠져 있을 수 있다
    @Test
    fun `카탈로그에 없는 요원과 무기는 알 수 없다고 적는다`() = runComposeUiTest {
        setContent { Themed { AgentsScreen(ProfilePreviewData.success, onBack = {}) } }
        onNodeWithText("알 수 없는 요원").assertExists()
    }

    @Test
    fun `평소보다 크게 움직인 무기에만 문구를 붙인다`() = runComposeUiTest {
        setContent { Themed { WeaponsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("요즘 잘 맞아요").assertExists()
        onNodeWithText("최근 떨어졌어요").assertDoesNotExist()
    }

    @Test
    fun `가장 킬이 많은 계열만 펼쳐 두고 누르면 다른 계열을 펼친다`() = runComposeUiTest {
        setContent { Themed { WeaponsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("고스트").assertDoesNotExist()
        onNodeWithText("권총").performClick()

        onNodeWithText("고스트").assertExists()
        onNodeWithText("그 밖의 무기").assertExists()
    }

    @Test
    fun `한 무기만 쓴 라운드가 모자라면 헤드샷 대신 표본 부족이다`() = runComposeUiTest {
        setContent { Themed { WeaponsScreen(ProfilePreviewData.success, onBack = {}) } }
        onNodeWithText("권총").performClick()

        onNodeWithText("표본 부족").assertExists()
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) {
    OvalitTheme(content = content)
}
