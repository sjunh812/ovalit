package com.ovalit.feature.match

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalTestApi::class)
class MatchScreensTest {

    @Test
    fun `경기 목록은 날짜로 묶고 줄을 누르면 경기를 연다`() = runComposeUiTest {
        var opened: MatchId? = null
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, { opened = it }) } }

        onNodeWithText("오늘").assertExists()
        onNodeWithText("어제").assertExists()
        onNodeWithText("ACS 237").performClick()

        assertEquals(MatchId("ascent"), opened)
    }

    // 날짜 머리와 같은 글자로 오른쪽 끝에 둔다
    @Test
    fun `날짜 머리 오른쪽에 그날 승패를 적는다`() = runComposeUiTest {
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) } }

        val today = onNodeWithText("오늘", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val record = onNodeWithText("1승 1패", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(today.top, record.top)
        assertTrue(record.left > today.right)
        onNodeWithText("2승 0패", useUnmergedTree = true).assertExists()
        // 화면 읽기 프로그램이 날짜와 승패를 한 머리로 읽는다
        onNode(hasText("오늘") and hasText("1승 1패")).assertExists()
    }

    @Test
    fun `비긴 판이 있으면 승패 뒤에 무를 붙이고 등수로 끝난 경기는 세지 않는다`() = runComposeUiTest {
        val won = MatchPreviewData.detailMatch
        val days = listOf(
            MatchDay(LocalDate(2026, 9, 24), listOf(won, won.copy(id = MatchId("draw"), myTeamWon = null), MatchPreviewData.deathmatch)),
            MatchDay(LocalDate(2026, 9, 23), listOf(MatchPreviewData.gauntlet)),
        )
        setContent { Themed { MatchesScreen(MatchPreviewData.matches.copy(days = days), {}, {}, {}) } }

        onNodeWithText("1승 0패 1무", useUnmergedTree = true).assertExists()
        onNode(hasText("어제") and hasText("승", substring = true)).assertDoesNotExist()
    }

    // 좁은 화면에서 글자를 키우면 한 줄에 안 들어간다. 날짜를 꺾지 않고 승패를 다음 줄로 내린다.
    @Test
    fun `날짜 머리가 한 줄에 안 들어가면 승패를 다음 줄로 내린다`() = runComposeUiTest {
        val day = MatchDay(LocalDate(2026, 9, 20), listOf(MatchPreviewData.detailMatch))
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Themed {
                    Box(Modifier.width(240.dp)) { MatchesScreen(MatchPreviewData.matches.copy(days = listOf(day)), {}, {}, {}) }
                }
            }
        }

        val date = onNodeWithText("9월 20일 일요일", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val record = onNodeWithText("1승 0패", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(record.top >= date.bottom, "승패가 날짜와 같은 줄에 겹쳐 있다")
        assertTrue(record.right <= 240.dp, "승패가 화면 밖으로 넘친다")
    }

    // 스코어 색만으로는 화면 읽기 프로그램 사용자가 이겼는지 모른다
    @Test
    fun `경기 줄은 낭독기에 승패를 말로 알린다`() = runComposeUiTest {
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) } }

        onNode(hasText("ACS 237") and hasStateDescription("승리")).assertExists()
        onAllNodes(hasStateDescription("패배")).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    // 색만으로는 승패가 갈리지 않는다. 줄마다 같은 자리에 두어 위아래로 훑으면 보이게 한다.
    @Test
    fun `경기 줄은 아랫줄 맨 앞에 승패를 적는다`() = runComposeUiTest {
        setContent { Themed { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) } }

        onNodeWithText("승리 · 경쟁 · 2시간 전", useUnmergedTree = true).assertExists()
        onAllNodesWithText("패배 · ", substring = true, useUnmergedTree = true).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
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
        // 맵 이름 옆의 승패
        onNodeWithText("승리").assertExists()
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
    fun `내 줄을 누르면 내 프로필을 연다`() = runComposeUiTest {
        var opened = false
        setContent { Themed { Detail(onOpenMe = { opened = true }) } }

        onNodeWithText("나").performClick()

        assertTrue(opened)
        onNodeWithText("친구 요청 보내기").assertDoesNotExist()
    }

    @Test
    fun `이름 밖을 누르면 그 판 기록을 펼치고 다시 누르면 접는다`() = runComposeUiTest {
        var friend: PlayerId? = null
        setContent { Themed { Detail(onOpenFriend = { friend = it }) } }

        val first = MatchPreviewData.detail.rows.first().line
        val kda = "${first.kills}/${first.deaths}/${first.assists}"

        onNodeWithText("첫 킬").assertDoesNotExist()
        onNodeWithText(kda, substring = true).performClick()
        onNodeWithText("첫 킬").assertExists()
        onNodeWithText("멀티킬").assertExists()
        assertEquals(null, friend)

        onNodeWithText(kda, substring = true).performClick()
        waitForIdle()
        onNodeWithText("첫 킬").assertDoesNotExist()
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

    // 초대 링크는 앱을 쓰든 안 쓰든 통하니 그대로 권한다
    @Test
    fun `앱을 쓰는지 모르면 안 쓴다고 하지 않고 초대 링크를 권한다`() = runComposeUiTest {
        var invited = false
        val row = MatchPreviewData.detail.groups.last().rows.first().copy(relation = PlayerRelation.UNKNOWN)
        setContent { Themed { PlayerSheet(row, onSendRequest = {}, onAccept = {}, onShareInvite = { invited = true }, onDismiss = {}) } }

        onNodeWithText("확인하지 못했어요", substring = true).assertExists()
        onNodeWithText("오발있을 쓰지 않는", substring = true).assertDoesNotExist()
        onNodeWithText("친구 요청 보내기").assertDoesNotExist()
        onNodeWithText("초대 링크 보내기").performClick()

        assertTrue(invited)
    }

    @Test
    fun `라운드 탭은 흐름을 요약하고 고른 라운드의 킬 순서를 보여준다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("라운드").performClick()

        onNodeWithText("우리 팀 첫 킬").assertExists()
        onNodeWithText("1라운드").assertExists()
        onNodeWithText("킬 순서").assertExists()

        onNodeWithContentDescription("2라운드").performClick()
        onNodeWithText("2라운드").assertExists()
    }

    // docs/screens.md S3: 진 클러치는 적지 않는다. 이 테스트는 적는 쪽만 본다.
    @Test
    fun `라운드 탭에 에이스와 이긴 클러치를 적는다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("라운드").performClick()

        onNodeWithText("에이스", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("2라운드").performClick()
        onNodeWithText("1대4 클러치", useUnmergedTree = true).assertExists()
    }

    // 이코노미 탭은 내 기록 탭으로 바뀌었다
    @Test
    fun `내 기록 탭은 맞힌 부위와 상대별 맞대결을 보여준다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("이코노미").assertDoesNotExist()
        onNodeWithText("내 기록").performClick()

        onNodeWithText("상대별 맞대결").assertExists()
        onNodeWithText("민석").assertExists()
        onNodeWithText("pixel").assertExists()
    }

    @Test
    fun `구매 유형 표는 라운드 탭 맨 밑에 두 팀을 나란히 둔다`() = runComposeUiTest {
        setContent { Themed { Detail() } }

        onNodeWithText("라운드").performClick()

        onNodeWithText("13라운드 9승", substring = true).assertExists()
        onAllNodesWithText("상대 팀").fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    // 데스매치와 건틀릿은 승패 대신 등수로 끝난다. 라운드가 없어 전투점수도 없다.
    @Test
    fun `등수로 끝나는 경기 줄은 스코어 대신 등수를 적고 아랫줄에 승패를 두지 않는다`() = runComposeUiTest {
        setContent { Themed { MatchesScreen(MatchPreviewData.otherMatches, {}, {}, {}) } }

        onNode(hasText("14명 중 3등") and hasStateDescription("14명 중 3등")).assertExists()
        onNodeWithText("데스매치 · 50분 전", useUnmergedTree = true).assertExists()
        onNodeWithText("8팀 중 2등").assertExists()
        onNodeWithText("31/26/0").assertExists()
        // 리포트에 넣는 스파이크 돌격만 전투점수가 있다
        onAllNodesWithText("ACS", substring = true).assertCountEquals(1)
    }

    @Test
    fun `팀 데스매치 줄은 라운드가 아니라 팀 점수를 스코어로 적는다`() = runComposeUiTest {
        setContent { Themed { MatchesScreen(MatchPreviewData.otherMatches, {}, {}, {}) } }

        onNodeWithText("100 – 87").assertExists()
        onNodeWithText("승리 · 팀 데스매치", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `데스매치 상세는 등수를 크게 두고 탭 없이 한 순위로 보여준다`() = runComposeUiTest {
        setContent { Themed { MatchDetailScreen(MatchPreviewData.deathmatchDetail, {}, {}, {}, {}, {}) } }

        onNodeWithContentDescription("14명 중 3등").assertExists()
        onNodeWithText("순위").assertExists()
        onNodeWithText("우리 팀").assertDoesNotExist()
        onNodeWithText("라운드").assertDoesNotExist()
        onNodeWithText("내 기록").assertDoesNotExist()
        onNodeWithText("전투점수").assertDoesNotExist()
        // 맨 위 등수와 내 줄의 자리 칩
        assertTrue(onAllNodesWithText("3등").fetchSemanticsNodes().size >= 2)
    }

    @Test
    fun `건틀릿 상세는 팀마다 등수를 머리에 두고 둘씩 묶는다`() = runComposeUiTest {
        setContent { Themed { MatchDetailScreen(MatchPreviewData.gauntletDetail, {}, {}, {}, {}, {}) } }

        onNodeWithContentDescription("8팀 중 2등").assertExists()
        onNodeWithText("알 수 없는 맵").assertExists()
        onNodeWithText("우리 팀 · 2등").assertExists()
        onNodeWithText("1등").assertExists()
        onNodeWithText("8등").assertExists()
        onNodeWithText("준호#KR1", substring = true).assertExists()
        onNodeWithText("승리").assertDoesNotExist()
        onNodeWithText("패배").assertDoesNotExist()
    }

    @Composable
    private fun Detail(
        onOpenFriend: (PlayerId) -> Unit = {},
        onSendRequest: (PlayerId) -> Unit = {},
        onShareInvite: () -> Unit = {},
        onOpenMe: () -> Unit = {},
    ) {
        MatchDetailScreen(MatchPreviewData.detail, {}, onOpenFriend, onSendRequest, {}, onShareInvite, onOpenMe = onOpenMe)
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
