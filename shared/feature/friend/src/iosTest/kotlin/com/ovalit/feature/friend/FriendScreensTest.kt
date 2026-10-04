package com.ovalit.feature.friend

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.model.PlayerId
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

// 앱은 Dispatchers.Default에서 세지만 테스트는 값을 바로 읽으려고 부르는 쪽에서 센다
private val SameThread = EmptyCoroutineContext

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

    // CLAUDE.md: S5는 나와의 관계에서 시작하는 친구 프로필이다. 머리 바로 밑에 같이 한 경기를 둔다.
    @Test
    fun `친구 프로필은 같이 한 경기와 나와 비교를 보여준다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) } }

        onNodeWithText("12경기").assertExists()
        onNodeWithText("8승 4패").assertExists()
        onNodeWithText("이번 주 · 나 · 민석").assertExists()
    }

    // 사용자 결정(2026-09-29): 나와 비교도 홈 고정 칸처럼 KDA까지 다섯을 견준다
    @Test
    fun `나와 비교는 KDA까지 견준다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) } }

        val compare = onNodeWithText("나와 비교", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val hitShots = onNodeWithText("맞힌 부위", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 칸이 카드에 잘려 화면 밖 줄은 잘린 자리가 0이 되므로 잘리기 전 자리로 본다
        val kda = onAllNodesWithText("KDA", useUnmergedTree = true)
        val kdaRows = kda.fetchSemanticsNodes().indices
            .map { kda[it].getUnclippedBoundsInRoot().top }
            .filter { it > compare.bottom && it < hitShots.top }
        assertEquals(1, kdaRows.size, "나와 비교에 KDA 줄이 없다")
    }

    // 사용자 요청: 같이 한 경기 다음에 내 프로필과 같은 칸을 두고, 나와 비교는 통계 다음이다
    @Test
    fun `전적을 공개한 친구는 내 프로필과 같은 칸을 정한 순서로 보여준다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) } }

        val order = listOf("같이 한 경기", "다이아몬드 2", "통계", "나와 비교", "맞힌 부위", "요원", "무기", "민석의 최근 경기")
            // 화면 밖으로 내려간 칸도 잘리기 전 자리로 본다
            .map { onNodeWithText(it, useUnmergedTree = true).getUnclippedBoundsInRoot().top }
        assertEquals(order.sorted(), order)
    }

    // 요원과 무기 칸은 내 프로필과 같다
    @Test
    fun `친구의 요원과 무기 칸에는 KDA와 킬 수와 헤드샷을 적는다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) } }

        // 제트는 (330 + 72) ÷ 260이다. 요원 칸에는 합계 없이 KDA만 둔다.
        onNodeWithText("KDA 1.55", useUnmergedTree = true).assertExists()
        onNodeWithText("330/260/72", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("260킬", useUnmergedTree = true).assertExists()
        onNodeWithText("헤드샷 28%", useUnmergedTree = true).assertExists()
    }

    // 사용자 요청: 친구의 요원과 무기도 내 프로필처럼 눌러서 S7과 S6으로 들어간다
    @Test
    fun `친구의 요원과 무기 칸을 누르면 친구 기록으로 들어간다`() = runComposeUiTest {
        var agentsOpened = false
        var weaponsOpened = false
        setContent {
            Themed {
                FriendProfileScreen(
                    FriendPreviewData.profile, {}, {}, {},
                    onOpenAgents = { agentsOpened = true },
                    onOpenWeapons = { weaponsOpened = true },
                )
            }
        }

        onNodeWithText("요원").performScrollTo().performClick()
        onNodeWithText("무기").performScrollTo().performClick()

        assertTrue(agentsOpened && weaponsOpened)
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

    // 끊기 버튼과 Gone 상태가 둘 다 화면을 닫으면 그 앞 화면까지 빠진다
    @Test
    fun `친구를 끊으면 화면을 한 번만 닫는다`() = runComposeUiTest {
        val minseok = PlayerId("fake-minseok")
        val viewModel = FriendProfileViewModel(
            minseok, FakeFriendRepository(), FakeMatchRepository(), FakeContentRepository(), Clock.System, TimeZone.of("Asia/Seoul"),
            computation = SameThread,
        )
        var backs = 0
        setContent {
            Themed {
                FriendProfileRoute(minseok, onBack = { backs++ }, onOpenMatches = {}, onOpenAgents = {}, onOpenWeapons = {}, viewModel = viewModel)
            }
        }

        onNodeWithContentDescription("더보기").performClick()
        onNodeWithText("친구 끊기").performClick()
        waitForIdle()

        assertEquals(1, backs)
        // 닫히며 밀려나는 동안에도 마지막 모습을 그린다
        assertTrue(onAllNodesWithText("민석", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    // 친구 경기에는 내가 안 뛴 경기의 다른 사람 기록이 섞여 있어서 줄을 눌러도 열지 않는다
    @Test
    fun `친구의 최근 경기는 세 판만 보여주고 전체 보기로 넘어간다`() = runComposeUiTest {
        var openedAll = false
        setContent { Themed { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}, onOpenMatches = { openedAll = true }) } }

        onNodeWithText("민석의 최근 경기").assertExists()
        onNodeWithText("로터스").assertExists()
        onAllNodesWithText("어제", substring = true).assertCountEquals(2)
        onAllNodesWithText("어센트").assertCountEquals(1)
        onNodeWithText("전체 보기").performScrollTo().performClick()

        assertTrue(openedAll)
    }

    @Test
    fun `전적을 공개하지 않은 친구는 최근 경기를 보여주지 않는다`() = runComposeUiTest {
        setContent { Themed { FriendProfileScreen(FriendPreviewData.privateProfile, {}, {}, {}) } }

        onNodeWithText("최근 경기", substring = true).assertDoesNotExist()
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) {
    OvalitTheme(content = content)
}
