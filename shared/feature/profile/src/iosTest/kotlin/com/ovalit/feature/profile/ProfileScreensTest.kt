package com.ovalit.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
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

        onNodeWithText("전략가 65%", useUnmergedTree = true).assertExists()
        onNodeWithText("요원").performScrollTo().performClick()
        onNodeWithText("무기").performScrollTo().performClick()

        assertTrue(agentsOpened && weaponsOpened)
    }

    @Test
    fun `티어 카드에는 지금 티어와 경쟁전 승패를 둔다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, {}, {}) } }

        onNodeWithText("플래티넘 2").assertExists()
        onNodeWithText("경쟁 32판", useUnmergedTree = true).assertExists()
        onNodeWithText("18승 14패", useUnmergedTree = true).assertExists()
        onNodeWithText("56%", useUnmergedTree = true).assertExists()
    }

    // 티어 칸에 크게 두니 이름 줄에서는 뺀다. 경쟁전이 없으면 티어 칸이 없어 이름 줄에 남긴다.
    @Test
    fun `경쟁전이 없으면 티어를 이름 줄에 둔다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.noCompetitive, {}, {}, {}, {}, {}) } }

        onNodeWithText("18승 14패", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("플래티넘 2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `통계와 맞힌 부위는 이번 액트 합계로 보여준다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, {}, {}) } }

        onNodeWithText("이번 액트 50경기", useUnmergedTree = true).assertExists()
        onNodeWithText("188", useUnmergedTree = true).assertExists()
        onNodeWithText("1.12", useUnmergedTree = true).assertExists()
        onNodeWithText("17.2/15.4/6.2", useUnmergedTree = true).assertExists()
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

    // 주 역할에 맞춘 열 말고 다른 지표도 볼 수 있어야 한다. 시트에서 고르면 표의 두 열이 바뀐다.
    @Test
    fun `요원 표의 지표는 표 위 버튼으로 바꾼다`() = runComposeUiTest {
        setContent { Themed { AgentsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("관여율 · 생존율").performScrollTo().performClick()
        onNodeWithText("전략가 기준").assertExists()
        onNodeWithText("K/D · 피해량").performClick()

        onNodeWithText("K/D").assertExists()
        onNodeWithText("피해량").assertExists()
        onNodeWithText("생존").assertDoesNotExist()
        onNodeWithText("K/D · 피해량").assertExists()
    }

    @Test
    fun `통계에 에이스 횟수와 클러치 성공을 둔다`() = runComposeUiTest {
        setContent { Themed { ProfileScreen(ProfilePreviewData.success, {}, {}, {}, {}, {}) } }

        onNodeWithText("에이스", useUnmergedTree = true).performScrollTo().assertExists()
        onNodeWithText("2번", useUnmergedTree = true).assertExists()
        onNodeWithText("7번 중 3번", useUnmergedTree = true).assertExists()
    }

    // 무기를 잘 쓰는지 보려면 킬과 헤드샷만으로는 모자란다
    @Test
    fun `무기 표는 킬 데스 어시스트와 라운드당 피해량과 헤드샷을 둔다`() = runComposeUiTest {
        setContent { Themed { WeaponsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("K/D/A", useUnmergedTree = true).assertExists()
        onNodeWithText("254/180/70", useUnmergedTree = true).assertExists()
        onNodeWithText("142", useUnmergedTree = true).assertExists()
        onNodeWithText("피해량", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `위쪽 주력 무기는 셋이다`() = runComposeUiTest {
        setContent { Themed { WeaponsScreen(ProfilePreviewData.success, onBack = {}) } }

        onNodeWithText("단일 무기 12라운드", useUnmergedTree = true).assertExists()
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

        // 고스트는 위쪽 주력 무기에도 있어서 이름 대신 표 줄의 K/D/A로 본다
        onNodeWithText("46/30/18", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("권총").performClick()

        onNodeWithText("46/30/18", useUnmergedTree = true).assertExists()
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
