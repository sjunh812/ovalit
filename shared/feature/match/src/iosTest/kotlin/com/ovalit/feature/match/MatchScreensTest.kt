package com.ovalit.feature.match

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MatchScreensTest {

    @Test
    fun `경기 목록은 날짜로 묶고 줄을 누르면 경기를 연다`() = runComposeUiTest {
        var opened: MatchId? = null
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, { opened = it }) } }

        onNodeWithText("오늘").assertExists()
        onNodeWithText("어제").assertExists()
        onNodeWithText("ADR 174").performClick()

        assertEquals(MatchId("ascent"), opened)
    }

    @Test
    fun `필터 시트에서 요원을 고르면 필터가 바뀐다`() = runComposeUiTest {
        var filter: MatchFilter? = null
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, { filter = it }, {}) } }

        onNodeWithContentDescription("필터").performClick()
        onNodeWithText("제트").performClick()

        assertEquals(MatchFilter(agent = MatchPreviewData.matches.agents.first()), filter)
    }

    @Test
    fun `경기 상세는 스코어와 전후반을 보여준다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithContentDescription("13 대 9").assertExists()
        onNodeWithText("전반 8–4 · 후반 5–5").assertExists()
    }

    // CLAUDE.md: 프로필은 서로 수락한 친구끼리만 본다
    @Test
    fun `친구를 누르면 프로필을 열고 다른 사람은 시트로 안내한다`() = runComposeUiTest {
        var friend: PlayerId? = null
        var requested: PlayerId? = null
        setContent { Themed { Detail(onOpenFriend = { friend = it }, onSendRequest = { requested = it }) } }

        onNodeWithText("준호#KR1", substring = true).performClick()
        assertEquals(MatchPreviewData.junho, friend)

        onNodeWithText("bloom#1004", substring = true).performClick()
        onNodeWithText("친구 요청 보내기").performClick()
        assertEquals(MatchPreviewData.bloom, requested)
    }

    @Test
    fun `앱을 안 쓰는 사람에게는 요청 대신 초대 링크를 권한다`() = runComposeUiTest {
        var invited = false
        setContent { Themed { Detail(onShareInvite = { invited = true }) } }

        onNodeWithText("rev#9922", substring = true).performClick()
        onNodeWithText("오발있을 쓰지 않는 플레이어예요", substring = true).assertExists()
        onNodeWithText("초대 링크 보내기").performClick()

        assertTrue(invited)
    }

    @Test
    fun `라운드 탭은 전반과 후반으로 나눠 보여준다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("라운드").performClick()

        onNodeWithText("전반").assertExists()
        onNodeWithText("후반").assertExists()
    }

    // 진 클러치는 적지 않는다. 적으면 진 라운드마다 꼬리표가 붙는다.
    @Test
    fun `라운드 탭에 에이스와 이긴 클러치를 적는다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("라운드").performClick()

        onNodeWithText("에이스", useUnmergedTree = true).assertExists()
        onNodeWithText("1대4 클러치", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `이코노미 탭은 유형마다 몇 라운드 이겼는지와 라운드별 장비를 보여준다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("이코노미").performClick()

        onNodeWithText("13라운드 9승", substring = true).assertExists()
        onAllNodesWithText("피스톨", substring = true).assertCountEquals(2)
    }

    @Composable
    private fun Detail(
        onOpenFriend: (PlayerId) -> Unit = {},
        onSendRequest: (PlayerId) -> Unit = {},
        onShareInvite: () -> Unit = {},
    ) {
        MatchDetailScreen(MatchPreviewData.detail, {}, onOpenFriend, onSendRequest, {}, onShareInvite)
    }

    @Test
    fun `목록을 끌어내리면 새 경기를 받는다`() = runComposeUiTest {
        var refreshed = false
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, {}, onRefresh = { refreshed = true }) } }

        onRoot().performTouchInput { swipeDown(startY = top + 40f, endY = bottom, durationMillis = 500) }
        waitForIdle()

        assertTrue(refreshed)
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) = OvalitTheme(darkTheme = true, content = content)
