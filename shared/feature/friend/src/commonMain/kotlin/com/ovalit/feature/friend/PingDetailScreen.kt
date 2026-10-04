package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.component.rememberContentShown
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Friend
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingMember
import com.ovalit.core.model.PlayerId
import com.ovalit.core.ui.pingCalledBy
import com.ovalit.core.ui.pingClockText
import com.ovalit.core.ui.pingHeroDay
import com.ovalit.core.ui.pingHeroTime
import com.ovalit.core.ui.pingSummary
import com.ovalit.core.ui.pingWhenText
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.loading
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.ping_cancel
import com.ovalit.feature.friend.resources.ping_change_time
import com.ovalit.feature.friend.resources.ping_gone
import com.ovalit.feature.friend.resources.ping_proposals_title
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PingDetailRoute(
    pingId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PingDetailViewModel = koinViewModel(key = "ping-$pingId") { parametersOf(pingId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PingDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
        timeZone = viewModel.timeZone,
        actions = PingDetailActions(
            slots = viewModel::slots,
            reply = viewModel::reply,
            moveTo = viewModel::moveTo,
            invite = viewModel::invite,
            cancel = {
                viewModel.cancel()
                onBack()
            },
        ),
    )
}

/** 초대 화면의 동작입니다. 테스트와 프리뷰는 비워 둡니다. */
internal class PingDetailActions(
    val slots: () -> List<Instant> = { emptyList() },
    val reply: (PingAnswer, Instant?) -> Unit = { _, _ -> },
    val moveTo: (Instant) -> Unit = {},
    val invite: (List<PlayerId>) -> Unit = {},
    val cancel: () -> Unit = {},
)

/**
 * 초대 하나의 화면입니다. 맨 위에 시각을 크게, 그 밑에 (보낸 초대면) 다른 시간 제안과 친구마다 답을 두고, 맨 밑에 고정한
 * 버튼으로 답하거나 시각을 옮깁니다. 친구 탭과 홈에서 줄을 누르면 들어옵니다.
 */
@Composable
internal fun PingDetailScreen(
    uiState: PingDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    actions: PingDetailActions = PingDetailActions(),
) {
    val colors = OvalitTheme.colors
    // 밀려 들어오는 중에 도착하면 다 들어올 때까지 자리 틀을 둔다. 빈 바탕으로 들어오다 전환 한가운데서 시각과 친구 줄이 튀어나왔다.
    val shown = rememberContentShown(loaded = uiState != PingDetailUiState.Loading)
    Column(modifier = modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) {
        OvalitBackTopBar(
            onBack = onBack,
            title = (uiState as? PingDetailUiState.Success)?.takeIf { shown }?.let { pingCalledBy(it.ping, it.me) },
        )
        when {
            !shown || uiState == PingDetailUiState.Loading -> PingDetailSkeleton()
            uiState == PingDetailUiState.Gone -> OvalitText(
                text = stringResource(Res.string.ping_gone),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xl),
                style = OvalitTheme.typography.body,
                color = colors.t2,
            )
            uiState is PingDetailUiState.Success -> PingDetailContent(uiState.ping, uiState.me, uiState.now, uiState.invitable, timeZone, actions)
        }
    }
}

// 맨 위 큰 시각과 친구 줄 셋의 자리다
@Composable
private fun PingDetailSkeleton() {
    OvalitSkeleton(
        description = stringResource(CoreUiRes.string.loading),
        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
    ) {
        Column {
            Spacer(Modifier.height(OvalitSpacing.lg))
            SkeletonBlock(width = 150.dp, height = 40.dp)
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(width = 110.dp, height = 14.dp)
            Spacer(Modifier.height(32.dp))
            repeat(3) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(width = 40.dp, height = 40.dp, radius = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    SkeletonBlock(width = 90.dp, height = 15.dp)
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(width = 44.dp, height = 15.dp)
                }
            }
        }
    }
}

// 다른 시간으로 답할 때와 시각을 옮길 때 같은 시간 시트를 쓴다. 친구 더 부르기도 시트다.
private enum class DetailSheet { REPLY, MOVE, INVITE }

@Composable
private fun ColumnScope.PingDetailContent(
    ping: Ping,
    me: PlayerId,
    now: Instant,
    invitable: List<Friend>,
    timeZone: TimeZone,
    actions: PingDetailActions,
) {
    val colors = OvalitTheme.colors
    val hosting = ping.isHostedBy(me)
    var sheet by remember { mutableStateOf<DetailSheet?>(null) }
    // 받은 초대면 부른 친구를 참석으로 맨 앞에 두고 나는 뺀다. 내 답은 맨 밑 버튼에 있다.
    val crowd = if (hosting) {
        ping.members
    } else {
        listOf(PingMember(ping.host, PingAnswer.YES)) + ping.members.filterNot { it.person.id == me }
    }
    // 같은 시각을 낸 친구는 한 줄에 모은 뒤 많이 낸 시각부터 둔다. 제안이 여럿 와도 줄이 사람 수만큼 쌓이지 않는다.
    val proposals = if (!hosting) {
        emptyList()
    } else {
        ping.members
            .filter { it.answer == PingAnswer.OTHER_TIME && it.proposedAt != null }
            .groupBy { it.proposedAt!! }
            .entries
            .sortedWith(compareByDescending<Map.Entry<Instant, List<PingMember>>> { it.value.size }.thenBy { it.key })
    }
    val scroll = rememberScrollState()
    val haptics = rememberOvalitHaptics()

    Column(modifier = Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = OvalitSpacing.gutter)) {
        Spacer(Modifier.height(OvalitSpacing.sm))
        // 시각은 초대장의 주인공이다. 지표 숫자와 같은 글꼴로 크게 적는다.
        Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
            // 오늘이 아니면 "내일"을 시각과 같이 크게 둔다. 숫자보다 한 단계 작게 해서 시각이 먼저 읽힌다.
            pingHeroDay(ping, now, timeZone)?.let { day ->
                OvalitText(text = day, modifier = Modifier.alignByBaseline(), style = OvalitTheme.typography.display)
                Spacer(Modifier.width(OvalitSpacing.sm))
            }
            OvalitText(text = pingHeroTime(ping, timeZone), modifier = Modifier.alignByBaseline(), style = OvalitTheme.typography.metricXl)
            Spacer(Modifier.width(OvalitSpacing.sm))
            OvalitText(
                text = pingWhenText(ping, now, timeZone),
                modifier = Modifier.alignByBaseline(),
                style = OvalitTheme.typography.label,
                color = colors.t2,
            )
        }
        if (proposals.isNotEmpty()) {
            SectionTitle(stringResource(Res.string.ping_proposals_title))
            proposals.forEach { (proposed, members) ->
                ProposalRow(
                    members = members,
                    time = pingClockText(proposed, now, timeZone),
                    onAccept = {
                        haptics.confirm()
                        actions.moveTo(proposed)
                    },
                )
            }
        }
        SectionTitle(crowdCounts(crowd))
        crowd.forEach { MemberLine(it, now, timeZone) }
        // 누가 못 간다고 했거나 깜빡 빠뜨린 친구를 더 부른다(사용자 요청, 2026-10-03)
        if (hosting && ping.openSeats > 0 && invitable.isNotEmpty()) {
            InviteMoreRow(onClick = { sheet = DetailSheet.INVITE })
        }
        Spacer(Modifier.height(OvalitSpacing.xl))
    }

    // 본문이 버튼 밑으로 이어질 때만 선을 긋는다(CLAUDE.md 디자인)
    OvalitDivider(color = if (scroll.canScrollForward) colors.line else Color.Transparent)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.md)) {
        if (hosting) {
            ActionBar(
                stringResource(Res.string.ping_change_time) to { sheet = DetailSheet.MOVE },
                stringResource(Res.string.ping_cancel) to actions.cancel,
            )
        } else {
            MyAnswer(ping, me, now, timeZone, actions, onOtherTime = { sheet = DetailSheet.REPLY })
        }
    }

    when (val open = sheet) {
        DetailSheet.INVITE -> PingInviteSheet(
            friends = invitable,
            seats = ping.openSeats,
            onInvite = {
                actions.invite(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        DetailSheet.REPLY, DetailSheet.MOVE -> PingTimeSheet(
            moving = open == DetailSheet.MOVE,
            current = ping.startsAt,
            proposed = ping.memberOf(me)?.takeIf { open == DetailSheet.REPLY && it.answer == PingAnswer.OTHER_TIME }?.proposedAt,
            slots = remember(open) { actions.slots() },
            now = now,
            timeZone = timeZone,
            onPick = {
                if (open == DetailSheet.MOVE) actions.moveTo(it) else actions.reply(PingAnswer.OTHER_TIME, it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

/**
 * 받은 초대의 내 답입니다. 답은 누르는 즉시 보내고 버튼 줄을 "참석으로 답했어요 · 바꾸기" 한 줄로 접습니다(사용자 결정,
 * 2026-10-03). 버튼이 그대로 남아 있으면 누를 때마다 부른 친구에게 알림이 갑니다. 바꾸기를 눌러야 다시 고릅니다.
 */
@Composable
private fun MyAnswer(
    ping: Ping,
    me: PlayerId,
    now: Instant,
    timeZone: TimeZone,
    actions: PingDetailActions,
    onOtherTime: () -> Unit,
) {
    val haptics = rememberOvalitHaptics()
    val mine = ping.memberOf(me)
    val answer = mine?.answer ?: PingAnswer.PENDING
    // 답이 바뀌면 바꾸는 중이던 것도 끝난다
    var changing by rememberSaveable(ping.id.value, answer, mine?.proposedAt) { mutableStateOf(false) }
    if (answer == PingAnswer.PENDING || changing) {
        // 같은 답을 다시 누르면 보내지 않고 접기만 한다
        fun pick(picked: PingAnswer) {
            if (picked == answer) changing = false else actions.reply(picked, null)
        }
        AnswerBar(
            answer = answer,
            otherLabel = mine?.proposedAt?.takeIf { answer == PingAnswer.OTHER_TIME }?.let { pingClockText(it, now, timeZone) },
            onYes = {
                haptics.confirm()
                pick(PingAnswer.YES)
            },
            onOther = onOtherTime,
            onNo = {
                haptics.reject()
                pick(PingAnswer.NO)
            },
        )
    } else {
        AnsweredLine(
            text = pingSummary(ping, me, now, timeZone),
            attending = answer == PingAnswer.YES,
            onChange = { changing = true },
        )
    }
}

// 묶음 제목이다. 내용과는 12dp를 둔다(CLAUDE.md 디자인). 줄마다 위아래 여백이 있어 그만큼 줄인다.
@Composable
private fun SectionTitle(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(top = OvalitSpacing.xxl, bottom = OvalitSpacing.xs),
        style = OvalitTheme.typography.bodyStrong,
    )
}
