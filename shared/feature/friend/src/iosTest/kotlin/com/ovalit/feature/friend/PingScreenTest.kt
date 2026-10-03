package com.ovalit.feature.friend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Friend
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PingMember
import com.ovalit.core.model.PingPerson
import com.ovalit.core.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val Seoul = TimeZone.of("Asia/Seoul")
private val Now = LocalDateTime(2026, 10, 3, 20, 25).toInstant(Seoul)
private val Nine = LocalDateTime(2026, 10, 3, 21, 0).toInstant(Seoul)
private val NineThirty = LocalDateTime(2026, 10, 3, 21, 30).toInstant(Seoul)

private val Me = PingPerson(PlayerId("me"), "오발러#KR1")
private val Minseok = PingPerson(PlayerId("minseok"), "민석#KR3")
private val Junho = PingPerson(PlayerId("junho"), "준호#KR1")
private val Jaehyun = PingPerson(PlayerId("jaehyun"), "재현#KR2")

@OptIn(ExperimentalTestApi::class)
class PingScreenTest {

    // 사용자 요청(2026-10-03): 친구 탭에 초대를 펼쳐 두면 자리를 크게 차지해서 한 줄씩 두고 누르면 들어간다
    @Test
    fun `친구 탭은 초대마다 한 줄을 두고 누르면 그 초대를 연다`() = runComposeUiTest {
        var opened: PingId? = null
        setContent { Friends(pings = listOf(received(), sent()), actions = PingActions(open = { opened = it })) }

        onNodeWithText("내 초대 · 21:00").assertExists()
        onNodeWithText("민석의 초대 · 21:00").performClick()

        assertEquals(received().id, opened)
    }

    @Test
    fun `답하지 않은 초대는 아직 답하지 않았다고 적는다`() = runComposeUiTest {
        setContent { Friends(pings = listOf(received(), sent())) }

        onNodeWithText("아직 답하지 않았어요").assertExists()
        onNodeWithText("3명 중 1명 참석").assertExists()
    }

    // 사용자 요청(2026-10-03): "봉봉이"에 "이"를 붙이면 "봉봉이이"로 읽힌다. 닉네임 뒤에는 받침과 상관없는 조사만 쓴다.
    @Test
    fun `닉네임 뒤에는 받침에 따라 바뀌는 조사를 붙이지 않는다`() = runComposeUiTest {
        setContent { Friends(pings = listOf(received().copy(host = PingPerson(PlayerId("bong"), "봉봉이#KR1")))) }

        onNodeWithText("봉봉이의 초대 · 21:00").assertExists()
    }

    @Test
    fun `초대가 없으면 카드에 부르는 법을 적는다`() = runComposeUiTest {
        setContent { Friends(pings = emptyList()) }

        onNodeWithText("오발있?").assertExists()
        onNodeWithText("파티 모집").assertExists()
        onNodeWithText("친구 4명까지 시간을 정해 한 번에 불러요").assertExists()
    }

    // 사용자 요청(2026-10-03): 친구 탭을 크게 차지하지 않게 부르기는 시트로 띄운다
    @Test
    fun `부르기를 누르면 시트에서 친구와 시각을 골라 부른다`() = runComposeUiTest {
        var sentTo: List<PlayerId>? = null
        var at: Instant? = null
        val slots = listOf(Nine, NineThirty)
        setContent {
            Friends(
                pings = emptyList(),
                actions = PingActions(slots = { slots }, send = { friends, startsAt, _ -> sentTo = friends; at = startsAt }),
            )
        }

        onNodeWithText("누구랑 할까요?").assertDoesNotExist()
        onNodeWithText("부르기").performClick()

        onNodeWithText("친구를 골라 주세요").assertIsNotEnabled()
        onNodeWithText("준호").performClick()
        onNodeWithText("21:30").performClick()
        onNodeWithText("1명 부르기").performScrollTo().performClick()

        assertEquals(listOf(PlayerId("junho")), sentTo)
        assertEquals(NineThirty, at)
    }

    // 한 번에 하나만 보낸다
    @Test
    fun `보낸 초대가 끝나기 전에는 부르기 버튼을 두지 않는다`() = runComposeUiTest {
        setContent { Friends(pings = listOf(sent())) }

        onNodeWithText("부르기").assertDoesNotExist()
        // 버튼이 없어도 무슨 기능인지는 제목 옆에서 알린다
        onNodeWithText("파티 모집").assertExists()
    }

    @Test
    fun `친구가 없으면 오발있 카드를 두지 않는다`() = runComposeUiTest {
        setContent { OvalitTheme { FriendsScreen(FriendPreviewData.noFriends.copy(me = Me.id, now = Now), {}, {}, {}, {}, timeZone = Seoul) } }

        onNodeWithText("오발있?").assertDoesNotExist()
    }

    @Test
    fun `받은 초대 화면은 시각과 친구마다 답을 적는다`() = runComposeUiTest {
        setContent { Detail(received()) }

        onNodeWithText("민석의 초대").assertExists()
        onNodeWithText("21:00").assertExists()
        onNodeWithText("오늘 · 35분 뒤").assertExists()
        // 부른 친구는 참석으로 치고 나는 세지 않는다. 내 답은 맨 밑 버튼에 있다.
        onNodeWithText("2명 참석 · 1명 응답 전").assertExists()
        onNodeWithText("준호").assertExists()
        onNodeWithText("오발러").assertDoesNotExist()
    }

    // 사용자 요청(2026-10-03): 날짜를 작은 글자에만 두면 큰 시각이 오늘처럼 읽힌다
    @Test
    fun `오늘이 아니면 날짜를 큰 시각 앞에 두고 옆에는 남은 시간만 적는다`() = runComposeUiTest {
        setContent { Detail(received().copy(startsAt = LocalDateTime(2026, 10, 4, 16, 0).toInstant(Seoul))) }

        onNodeWithText("내일").assertExists()
        onNodeWithText("16:00").assertExists()
        onNodeWithText("19시간 35분 뒤").assertExists()
    }

    @Test
    fun `자정을 넘긴 오늘 밤은 새벽이라고 크게 적는다`() = runComposeUiTest {
        setContent { Detail(received().copy(startsAt = LocalDateTime(2026, 10, 4, 0, 30).toInstant(Seoul))) }

        onNodeWithText("새벽").assertExists()
        onNodeWithText("00:30").assertExists()
        onNodeWithText("4시간 5분 뒤").assertExists()
    }

    // 사용자 결정(2026-10-03): 누르면 확정하고 버튼 줄을 접는다. 남겨 두면 누를 때마다 알림이 간다.
    @Test
    fun `답하면 버튼 줄을 접고 바꾸기를 눌러야 다시 고른다`() = runComposeUiTest {
        val replies = mutableListOf<PingAnswer>()
        setContent { AnsweringDetail(received(), onReply = { replies += it }) }

        onNodeWithText("갈게요").performClick()

        onNodeWithText("참석으로 답했어요").assertExists()
        onNodeWithText("못 가요").assertDoesNotExist()

        onNodeWithText("바꾸기").performClick()
        onNodeWithText("못 가요").performClick()

        assertEquals(listOf(PingAnswer.YES, PingAnswer.NO), replies)
        onNodeWithText("불참으로 답했어요").assertExists()
    }

    @Test
    fun `바꾸기에서 같은 답을 누르면 보내지 않고 접는다`() = runComposeUiTest {
        val replies = mutableListOf<PingAnswer>()
        setContent { AnsweringDetail(received().answeredByMe(PingAnswer.YES), onReply = { replies += it }) }

        onNodeWithText("바꾸기").performClick()
        onNodeWithText("갈게요").performClick()

        assertTrue(replies.isEmpty())
        onNodeWithText("참석으로 답했어요").assertExists()
    }

    @Test
    fun `다른 시간으로 답했으면 낸 시각을 적고 바꾸기에서도 그 시각을 보인다`() = runComposeUiTest {
        setContent { Detail(received().answeredByMe(PingAnswer.OTHER_TIME, NineThirty)) }

        onNodeWithText("21:30 제안했어요").assertExists()
        onNodeWithText("바꾸기").performClick()

        onNodeWithText("21:30 제안").assertExists()
    }

    // 사용자 요청(2026-10-03): 제안이 여럿 와도 줄이 사람 수만큼 쌓이지 않게 같은 시각끼리 모은다
    @Test
    fun `보낸 초대 화면은 같은 시각을 낸 친구를 한 줄에 모아 수락받는다`() = runComposeUiTest {
        var moved: Instant? = null
        setContent { Detail(sent(), PingDetailActions(moveTo = { moved = it })) }

        onNodeWithText("내 초대").assertExists()
        onNodeWithText("21:30 어때요?").assertExists()
        onNodeWithText("준호, 재현의 제안").assertExists()
        onNodeWithText("1명 참석 · 2명 시간 제안").assertExists()
        onNodeWithText("수락").performClick()

        assertEquals(NineThirty, moved)
    }

    // 사용자 요청(2026-10-03): 누가 못 간다고 하거나 깜빡 빠뜨린 친구를 더 부른다
    @Test
    fun `보낸 초대 화면에서 부르지 않은 친구를 더 부른다`() = runComposeUiTest {
        var invited: List<PlayerId>? = null
        setContent { Detail(sent(), PingDetailActions(invite = { invited = it }), invitable = listOf(Seoyeon)) }

        onNodeWithText("친구 더 부르기").performScrollTo().performClick()
        onNodeWithText("1명 더 부를 수 있어요. 못 간다고 한 친구는 세지 않아요.").assertExists()
        onNodeWithText("서연").performClick()
        onNodeWithText("1명 더 부르기").performClick()

        assertEquals(listOf(PlayerId("seoyeon")), invited)
    }

    @Test
    fun `자리가 없으면 친구 더 부르기를 두지 않는다`() = runComposeUiTest {
        val full = sent().copy(members = sent().members + PingMember(PingPerson(PlayerId("x"), "엑스#KR1"), PingAnswer.PENDING))
        setContent { Detail(full, invitable = listOf(Seoyeon)) }

        onNodeWithText("친구 더 부르기").assertDoesNotExist()
    }

    @Test
    fun `못 간다고 한 친구가 있으면 그 자리에 더 부를 수 있다`() = runComposeUiTest {
        val full = sent().copy(members = sent().members + PingMember(PingPerson(PlayerId("x"), "엑스#KR1"), PingAnswer.NO))
        setContent { Detail(full, invitable = listOf(Seoyeon)) }

        onNodeWithText("친구 더 부르기").assertExists()
    }

    @Test
    fun `보낸 초대 화면에서 초대를 취소한다`() = runComposeUiTest {
        var cancelled = false
        setContent { Detail(sent(), PingDetailActions(cancel = { cancelled = true })) }

        onNodeWithText("초대 취소").performClick()

        assertTrue(cancelled)
    }

    // 알림을 늦게 누르면 이미 끝난 초대로 들어온다
    @Test
    fun `없어진 초대는 끝났다고 적는다`() = runComposeUiTest {
        setContent { OvalitTheme { PingDetailScreen(PingDetailUiState.Gone, onBack = {}, timeZone = Seoul) } }

        onNodeWithText("끝났거나 취소된 초대예요").assertExists()
    }
}

private fun received() = Ping(
    id = PingId("received"),
    host = Minseok,
    startsAt = Nine,
    createdAt = Now - 5.minutes,
    members = listOf(
        PingMember(Me, PingAnswer.PENDING),
        PingMember(Junho, PingAnswer.PENDING),
        PingMember(Jaehyun, PingAnswer.YES),
    ),
)

private fun sent() = Ping(
    id = PingId("sent"),
    host = Me,
    startsAt = Nine,
    createdAt = Now - 5.minutes,
    members = listOf(
        PingMember(Junho, PingAnswer.OTHER_TIME, NineThirty),
        PingMember(Jaehyun, PingAnswer.OTHER_TIME, NineThirty),
        PingMember(Minseok, PingAnswer.YES),
    ),
)

private fun Ping.answeredByMe(answer: PingAnswer, proposedAt: Instant? = null) =
    copy(members = members.map { if (it.person == Me) it.copy(answer = answer, proposedAt = proposedAt) else it })

private val Seoyeon = Friend(PlayerId("seoyeon"), "서연#KR7", playerCard = null, statsPublic = true, matches = emptyList())

@Composable
private fun Detail(ping: Ping, actions: PingDetailActions = PingDetailActions(), invitable: List<Friend> = emptyList()) {
    OvalitTheme {
        PingDetailScreen(PingDetailUiState.Success(ping, Me.id, Now, invitable), onBack = {}, timeZone = Seoul, actions = actions)
    }
}

// 답하면 저장소처럼 내 답을 바꿔 다시 그린다
@Composable
private fun AnsweringDetail(initial: Ping, onReply: (PingAnswer) -> Unit) {
    var ping by remember { mutableStateOf(initial) }
    Detail(
        ping = ping,
        actions = PingDetailActions(
            reply = { answer, proposedAt ->
                onReply(answer)
                ping = ping.answeredByMe(answer, proposedAt)
            },
        ),
    )
}

@Composable
private fun Friends(pings: List<Ping>, actions: PingActions = PingActions()) {
    OvalitTheme {
        FriendsScreen(
            uiState = FriendPreviewData.friends.copy(pings = pings, me = Me.id, now = Now),
            onOpenFriend = {},
            onInvite = {},
            onAccept = {},
            onDecline = {},
            timeZone = Seoul,
            ping = actions,
        )
    }
}
