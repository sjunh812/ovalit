package com.ovalit.feature.friend

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FriendScreensTest {

    // CLAUDE.md: 닉네임 검색이 없다. 친구는 스코어보드나 초대 링크로만 추가한다.
    @Test
    fun `친구 탭에는 닉네임 검색 없이 초대 링크만 있다`() = runComposeUiTest {
        var invited = false
        setContent { Themed { FriendsScreen(FriendPreviewData.friends, {}, { invited = true }, {}, {}) } }

        onNodeWithText("초대 링크 보내기").performClick()

        assertTrue(invited)
        onNodeWithText("닉네임으로는 찾을 수 없어요", substring = true).assertExists()
    }

    @Test
    fun `받은 요청은 어디서 왔는지 보여주고 수락할 수 있다`() = runComposeUiTest {
        var accepted: PlayerId? = null
        setContent { Themed { FriendsScreen(FriendPreviewData.friends, {}, {}, { accepted = it }, {}) } }

        onNodeWithText("같이 뛴 경기에서 보냈어요").assertExists()
        onNodeWithText("초대 링크로 보냈어요").assertExists()
        onNodeWithText("받은 요청 2").assertExists()
        onAllNodesWithText("수락")[0].performClick()

        assertEquals(PlayerId("jiwoo"), accepted)
    }

    @Test
    fun `전적 비공개 친구와 라이벌을 목록에서 알 수 있다`() = runComposeUiTest {
        setContent { Themed { FriendsScreen(FriendPreviewData.friends, {}, {}, {}, {}) } }

        onNodeWithText("전적 비공개").assertExists()
        onNodeWithText("라이벌").assertExists()
        onNodeWithText("이번 주 9경기").assertExists()
    }

    @Test
    fun `친구가 없으면 어떻게 추가하는지 알려준다`() = runComposeUiTest {
        setContent { Themed { FriendsScreen(FriendPreviewData.noFriends, {}, {}, {}, {}) } }

        onNodeWithText("아직 친구가 없어요").assertExists()
    }

    // CLAUDE.md: S5는 나와의 관계 페이지다. 같이 한 경기를 맨 위에 둔다.
    @Test
    fun `친구 프로필은 같이 한 경기와 나와 비교를 보여준다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) } }

        onNodeWithText("12경기").assertExists()
        onNodeWithText("8승 4패").assertExists()
        onNodeWithText("이번 주 · 나 · 민석").assertExists()
    }

    @Test
    fun `라이벌 지정 버튼을 누르면 라이벌로 고른다`() = runComposeUiTest {
        var toggled = false
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, { toggled = true }, {}) } }

        onNodeWithText("라이벌 지정").performClick()

        assertTrue(toggled)
    }

    @Test
    fun `전적 비공개 친구는 같이 한 경기만 보여주고 라이벌로 고를 수 없다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.privateProfile, {}, {}, {}) } }

        onNodeWithText("같이 한 경기만 볼 수 있어요", substring = true).assertExists()
        onNodeWithText("라이벌 지정").assertDoesNotExist()
        onNodeWithText("나와 비교").assertDoesNotExist()
    }

    @Test
    fun `친구 끊기는 확인을 받은 뒤에 한다`() = runComposeUiTest {
        var unfriended = false
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, { unfriended = true }) } }

        onNodeWithContentDescription("더보기").performClick()
        assertFalse(unfriended)
        onNodeWithText("민석님과 친구를 끊을까요?").assertExists()

        onNodeWithText("친구 끊기").performClick()

        assertTrue(unfriended)
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) {
    OvalitTheme(content = content)
}
