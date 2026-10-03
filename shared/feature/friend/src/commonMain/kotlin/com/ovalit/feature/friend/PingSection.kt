package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPickerTitle
import com.ovalit.core.designsystem.component.OvalitPressOutset
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitWheelPicker
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Friend
import com.ovalit.core.model.MAX_PING_FRIENDS
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingMember
import com.ovalit.core.model.PlayerId
import com.ovalit.core.ui.PingSummaryRow
import com.ovalit.core.ui.pingAnswerText
import com.ovalit.core.ui.pingClockText
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.ping_answer_other
import com.ovalit.core.ui.resources.ping_time_now
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.ping_change_answer
import com.ovalit.feature.friend.resources.ping_compose_hint
import com.ovalit.feature.friend.resources.ping_compose_limit
import com.ovalit.feature.friend.resources.ping_compose_open
import com.ovalit.feature.friend.resources.ping_compose_title
import com.ovalit.feature.friend.resources.ping_compose_when
import com.ovalit.feature.friend.resources.ping_compose_who
import com.ovalit.feature.friend.resources.ping_count_no
import com.ovalit.feature.friend.resources.ping_count_other
import com.ovalit.feature.friend.resources.ping_count_pending
import com.ovalit.feature.friend.resources.ping_count_yes
import com.ovalit.feature.friend.resources.ping_invite_body
import com.ovalit.feature.friend.resources.ping_invite_more
import com.ovalit.feature.friend.resources.ping_invite_send
import com.ovalit.feature.friend.resources.ping_invite_title
import com.ovalit.feature.friend.resources.ping_move
import com.ovalit.feature.friend.resources.ping_pick_friends
import com.ovalit.feature.friend.resources.ping_proposal
import com.ovalit.feature.friend.resources.ping_proposal_by
import com.ovalit.feature.friend.resources.ping_reply_no
import com.ovalit.feature.friend.resources.ping_reply_other
import com.ovalit.feature.friend.resources.ping_reply_yes
import com.ovalit.feature.friend.resources.ping_send_to
import com.ovalit.feature.friend.resources.ping_time_move_confirm
import com.ovalit.feature.friend.resources.ping_time_move_title
import com.ovalit.feature.friend.resources.ping_time_reply_confirm
import com.ovalit.feature.friend.resources.ping_time_reply_title
import com.ovalit.feature.friend.resources.ping_title
import com.ovalit.feature.friend.resources.ping_title_note
import com.ovalit.feature.friend.resources.ping_too_many
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

private val StackOverlap = 8.dp
private val RowAvatarSize = 36.dp
private val PickAvatarSize = 52.dp
// 고르는 칸 사이 간격의 절반이다
private val PickPressOutset = 6.dp
private val BadgeSize = 15.dp
private val BarShape = RoundedCornerShape(12.dp)
private val BarItemShape = RoundedCornerShape(9.dp)
private val BarInset = 3.dp
private val BarItemHeight = 40.dp

/**
 * 친구 탭 맨 위의 "오발있?" 카드입니다. 받은 초대와 보낸 초대를 한 줄씩 두고, 누르면 초대 화면으로 들어가 답하거나 시각을
 * 옮깁니다. 카드 안에 시각, 참석자, 버튼을 다 펼치면 친구 탭이 초대로 꽉 차서 한 단계 들어가게 했습니다(사용자 요청,
 * 2026-10-03). 줄은 홈 카드와 같은 [PingSummaryRow]입니다.
 *
 * @param pings 받은 초대가 먼저이고 내가 보낸 것이 맨 뒤입니다.
 * @param canCompose 보낸 초대가 끝나기 전이면 `false`라 부르기 버튼을 두지 않습니다. 한 번에 하나만 보냅니다.
 */
@Composable
internal fun PingListCard(
    pings: List<Ping>,
    me: PlayerId,
    now: Instant,
    timeZone: TimeZone,
    canCompose: Boolean,
    onOpen: (Ping) -> Unit,
    onCompose: () -> Unit,
) {
    OvalitCard {
        // "오발있?"은 앱 이름이라 처음 보는 사람은 무슨 기능인지 모른다. 옆에 작게 "파티 모집"을 붙여 알려 준다.
        val title = @Composable {
            Row {
                OvalitText(
                    text = stringResource(Res.string.ping_title),
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.bodyStrong,
                )
                Spacer(Modifier.width(6.dp))
                OvalitText(
                    text = stringResource(Res.string.ping_title_note),
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.caption,
                    color = OvalitTheme.colors.t3,
                )
            }
        }
        Box(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            if (canCompose) {
                OvalitPickerTitle(
                    title = title,
                    picker = { SmallButton(text = stringResource(Res.string.ping_compose_open), filled = false, onClick = onCompose) },
                )
            } else {
                title()
            }
        }
        if (pings.isEmpty()) {
            OvalitText(
                text = stringResource(Res.string.ping_compose_hint, MAX_PING_FRIENDS),
                modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.sm),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
        } else {
            Spacer(Modifier.height(OvalitSpacing.xs))
            pings.forEachIndexed { index, ping ->
                key(ping.id.value) {
                    if (index > 0) {
                        OvalitDivider(
                            modifier = Modifier.padding(start = OvalitSpacing.gutter + RowAvatarSize + OvalitSpacing.md),
                            color = OvalitTheme.colors.lineWeak,
                        )
                    }
                    PingSummaryRow(
                        ping = ping,
                        me = me,
                        now = now,
                        timeZone = timeZone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable(role = Role.Button) { onOpen(ping) }
                            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.sm),
                    )
                }
            }
        }
    }
}

/** "2명 참석 · 1명 응답 전"입니다. 초대 화면의 참석자 묶음 제목입니다. */
@Composable
internal fun crowdCounts(crowd: List<PingMember>): String = listOfNotNull(
    crowd.count { it.answer == PingAnswer.YES }.takeIf { it > 0 }?.let { stringResource(Res.string.ping_count_yes, it) },
    crowd.count { it.answer == PingAnswer.OTHER_TIME }.takeIf { it > 0 }?.let { stringResource(Res.string.ping_count_other, it) },
    crowd.count { it.answer == PingAnswer.PENDING }.takeIf { it > 0 }?.let { stringResource(Res.string.ping_count_pending, it) },
    crowd.count { it.answer == PingAnswer.NO }.takeIf { it > 0 }?.let { stringResource(Res.string.ping_count_no, it) },
).joinToString(" · ")

// 같은 시각을 낸 친구들이다. 받은 요청 줄처럼 아바타, 두 줄 글, 오른쪽 버튼으로 두고 그 자리에서 받는다.
@Composable
internal fun ProposalRow(members: List<PingMember>, time: String, onAccept: () -> Unit) {
    val colors = OvalitTheme.colors
    val names = members.joinToString(", ") { it.person.riotId.substringBefore('#') }
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        val step = RowAvatarSize - StackOverlap
        Box(modifier = Modifier.width(RowAvatarSize + step * (members.size - 1)).height(RowAvatarSize)) {
            // 앞사람이 위로 오게 뒤에서부터 그린다
            members.asReversed().forEachIndexed { reversed, member ->
                val index = members.lastIndex - reversed
                StatusAvatar(member, size = RowAvatarSize, ring = true, modifier = Modifier.padding(start = step * index))
            }
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OvalitText(text = stringResource(Res.string.ping_proposal, time), style = OvalitTheme.typography.bodyStrong)
            OvalitText(
                text = stringResource(Res.string.ping_proposal_by, names),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        SmallButton(text = stringResource(Res.string.ping_move), filled = true, onClick = onAccept)
    }
}

// 참석자 줄 맨 밑에서 친구를 더 부르는 줄이다. 아바타 자리에 + 동그라미를 둬서 위 줄들과 이름 자리가 맞는다.
@Composable
internal fun InviteMoreRow(onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(
                interactionSource = null,
                indication = pressIndication(horizontalOutset = OvalitPressOutset),
                role = Role.Button,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(RowAvatarSize).background(colors.fill, CircleShape), contentAlignment = Alignment.Center) {
            OvalitIcon(OvalitIcons.Plus, contentDescription = null, tint = colors.t2, size = 16.dp)
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(text = stringResource(Res.string.ping_invite_more), style = OvalitTheme.typography.body, color = colors.t2)
    }
}

/**
 * 보낸 ㅇㅂㅇ에 친구를 더 부르는 시트입니다. 부르기 시트와 같은 아바타 고르기이고 시각은 이미 정해져 있어 없습니다.
 *
 * @param seats 더 부를 수 있는 자리입니다. 못 간다고 한 친구는 세지 않습니다.
 */
@Composable
internal fun PingInviteSheet(friends: List<Friend>, seats: Int, onInvite: (List<PlayerId>) -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberOvalitHaptics()
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    OvalitBottomSheet(
        title = stringResource(Res.string.ping_invite_title),
        body = stringResource(Res.string.ping_invite_body, seats),
        onDismiss = onDismiss,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
        ) {
            friends.forEach { friend ->
                val selected = friend.id.value in picked
                FriendPick(
                    friend = friend,
                    selected = selected,
                    enabled = selected || picked.size < seats,
                    onToggle = { on -> picked = if (on) picked + friend.id.value else picked - friend.id.value },
                )
            }
        }
        Spacer(Modifier.height(OvalitSpacing.xl))
        OvalitPrimaryButton(
            text = if (picked.isEmpty()) {
                stringResource(Res.string.ping_pick_friends)
            } else {
                stringResource(Res.string.ping_invite_send, picked.size)
            },
            enabled = picked.isNotEmpty(),
            onClick = {
                haptics.confirm()
                onInvite(picked.map(::PlayerId))
            },
        )
    }
}

// 친구 한 사람의 답이다. 강조는 색이 아니라 밝기로 한다. 참석이 가장 밝고 응답 전이 가장 옅다.
@Composable
internal fun MemberLine(member: PingMember, now: Instant, timeZone: TimeZone) {
    val colors = OvalitTheme.colors
    val (color, weight) = when (member.answer) {
        PingAnswer.YES -> colors.t1 to FontWeight.SemiBold
        PingAnswer.OTHER_TIME -> colors.t2 to FontWeight.Medium
        PingAnswer.NO -> colors.t3 to FontWeight.Normal
        PingAnswer.PENDING -> colors.t4 to FontWeight.Normal
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusAvatar(member, size = RowAvatarSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(
            text = member.person.riotId.substringBefore('#'),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitText(
            text = pingAnswerText(member, now, timeZone),
            style = OvalitTheme.typography.label.copy(fontWeight = weight),
            color = color,
            maxLines = 1,
        )
    }
}

// 참석은 체크, 다른 시간은 시계를 아바타 오른쪽 아래에 붙인다. 불참과 응답 전은 표시 없이 옅게 둔다.
@Composable
private fun StatusAvatar(member: PingMember, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val colors = OvalitTheme.colors
    val badge: ImageVector? = when (member.answer) {
        PingAnswer.YES -> OvalitIcons.Check
        PingAnswer.OTHER_TIME -> OvalitIcons.Clock
        else -> null
    }
    Box(modifier = modifier.size(size)) {
        Avatar(
            riotId = member.person.riotId,
            size = size,
            modifier = Modifier
                .alpha(
                    when (member.answer) {
                        PingAnswer.NO -> 0.35f
                        PingAnswer.PENDING -> 0.55f
                        else -> 1f
                    },
                )
                .then(if (ring) Modifier.border(2.dp, colors.bg, CircleShape) else Modifier),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(BadgeSize)
                    .background(colors.t1, CircleShape)
                    .border(1.5.dp, colors.bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                OvalitIcon(badge, contentDescription = null, tint = colors.bg, size = 9.dp)
            }
        }
    }
}

/**
 * 갈게요, 다른 시간, 못 가요를 한 덩어리로 둔 버튼입니다. 고른 답은 액센트 면입니다. 다른 시간을 골랐으면 그 칸에 낸 시각을
 * 적습니다("15:00 제안").
 */
@Composable
internal fun AnswerBar(
    answer: PingAnswer,
    otherLabel: String?,
    onYes: () -> Unit,
    onOther: () -> Unit,
    onNo: () -> Unit,
) {
    Row(modifier = barModifier().selectableGroup()) {
        BarItem(stringResource(Res.string.ping_reply_yes), selected = answer == PingAnswer.YES, onClick = onYes, modifier = Modifier.weight(1f))
        BarItem(
            otherLabel?.let { stringResource(CoreUiRes.string.ping_answer_other, it) } ?: stringResource(Res.string.ping_reply_other),
            selected = answer == PingAnswer.OTHER_TIME,
            onClick = onOther,
            modifier = Modifier.weight(1f),
        )
        BarItem(stringResource(Res.string.ping_reply_no), selected = answer == PingAnswer.NO, onClick = onNo, modifier = Modifier.weight(1f))
    }
}

// 답한 뒤의 줄이다. 버튼 줄과 높이와 면이 같아서 접혀도 화면 높이가 그대로다.
@Composable
internal fun AnsweredLine(text: String, attending: Boolean, onChange: () -> Unit) {
    val colors = OvalitTheme.colors
    Row(modifier = barModifier(), verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = text,
            modifier = Modifier.weight(1f).padding(start = OvalitSpacing.md),
            style = OvalitTheme.typography.label.copy(fontWeight = if (attending) FontWeight.SemiBold else FontWeight.Medium),
            color = if (attending) colors.t1 else colors.t2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BarItem(
            text = stringResource(Res.string.ping_change_answer),
            selected = null,
            onClick = onChange,
            modifier = Modifier.padding(start = OvalitSpacing.xs),
            horizontalPadding = OvalitSpacing.md,
        )
    }
}

/** 보낸 초대 맨 밑 버튼입니다. 받은 초대의 답 버튼과 같은 모양이고 고르는 게 아니라서 액센트 면이 없습니다. */
@Composable
internal fun ActionBar(vararg items: Pair<String, () -> Unit>) {
    Row(modifier = barModifier()) {
        items.forEach { (text, onClick) -> BarItem(text, selected = null, onClick = onClick, modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun barModifier(): Modifier =
    Modifier.fillMaxWidth().clip(BarShape).background(OvalitTheme.colors.fill).padding(BarInset)

/** @param selected 고르는 버튼이면 고른지 아닌지, 누르기만 하는 버튼이면 `null`입니다. */
@Composable
private fun BarItem(
    text: String,
    selected: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
) {
    val colors = OvalitTheme.colors
    val on = selected == true
    val press = pressIndication(BarItemShape, if (on) colors.onAccent else colors.t2)
    Box(
        modifier = modifier
            .heightIn(min = BarItemHeight)
            .then(
                if (selected == null) {
                    Modifier.clickable(interactionSource = null, indication = press, role = Role.Button, onClick = onClick)
                } else {
                    Modifier.selectable(selected = on, interactionSource = null, indication = press, role = Role.RadioButton, onClick = onClick)
                },
            )
            .clip(BarItemShape)
            .then(if (on) Modifier.background(colors.accent) else Modifier)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        OvalitText(
            text = text,
            style = OvalitTheme.typography.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
            color = if (on) colors.onAccent else colors.t1,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 친구와 시각을 골라 부르는 시트입니다. 친구 탭을 크게 차지하지 않게 시트로 띄웁니다. 인스타그램 공유처럼 아바타를 눌러 고르고,
 * 시각은 휠로 고릅니다.
 *
 * @param tooMany 오늘 보낼 수 있는 만큼 이미 보냈으면 `true`입니다. 버튼 위에 그 말을 적습니다.
 */
@Composable
internal fun PingComposeSheet(
    friends: List<Friend>,
    slots: List<Instant>,
    now: Instant,
    timeZone: TimeZone,
    tooMany: Boolean,
    onSend: (List<PlayerId>, Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OvalitTheme.colors
    val haptics = rememberOvalitHaptics()
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    // 휠 맨 위가 "지금"이고 그 밑이 [slots]다
    var slotIndex by remember { mutableStateOf(0) }
    val startsAt = slots.getOrNull(slotIndex - 1)

    OvalitBottomSheet(
        title = stringResource(Res.string.ping_compose_title),
        body = stringResource(Res.string.ping_compose_hint, MAX_PING_FRIENDS),
        onDismiss = onDismiss,
    ) {
        PickLabel(stringResource(Res.string.ping_compose_who))
        // 옆으로 미는 줄은 누른 면을 줄 끝에서 자른다. 친구가 많으면 다음 줄로 넘긴다.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
        ) {
            friends.forEach { friend ->
                val selected = friend.id.value in picked
                FriendPick(
                    friend = friend,
                    selected = selected,
                    enabled = selected || picked.size < MAX_PING_FRIENDS,
                    onToggle = { on -> picked = if (on) picked + friend.id.value else picked - friend.id.value },
                )
            }
        }
        if (picked.size >= MAX_PING_FRIENDS) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(Res.string.ping_compose_limit, MAX_PING_FRIENDS),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
        Spacer(Modifier.height(OvalitSpacing.lg))
        PickLabel(stringResource(Res.string.ping_compose_when))
        // 30분 단위면 약속 시각으로 충분하다. 다음 날 같은 시각 전까지라 칩으로 늘어놓으면 벽이 돼서 휠로 고른다.
        OvalitWheelPicker(
            items = listOf(stringResource(CoreUiRes.string.ping_time_now)) + slots.map { pingClockText(it, now, timeZone) },
            selected = slotIndex,
            onSelect = { slotIndex = it },
        )
        Spacer(Modifier.height(OvalitSpacing.xl))
        if (tooMany) {
            OvalitText(
                text = stringResource(Res.string.ping_too_many),
                modifier = Modifier.padding(bottom = OvalitSpacing.sm),
                style = OvalitTheme.typography.caption,
                color = colors.t2,
            )
        }
        OvalitPrimaryButton(
            text = if (picked.isEmpty()) {
                stringResource(Res.string.ping_pick_friends)
            } else {
                stringResource(Res.string.ping_send_to, picked.size)
            },
            enabled = picked.isNotEmpty() && !tooMany,
            onClick = {
                haptics.confirm()
                onSend(picked.map(::PlayerId), startsAt ?: now)
            },
        )
    }
}

// 고른 친구는 액센트 테두리와 체크를 단다. S0-4에서 관심사를 고르는 칸과 같은 표시다.
@Composable
private fun FriendPick(friend: Friend, selected: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = OvalitTheme.colors
    Column(
        modifier = Modifier
            .width(PickAvatarSize + OvalitSpacing.sm)
            .toggleable(
                value = selected,
                enabled = enabled,
                interactionSource = null,
                indication = pressIndication(horizontalOutset = PickPressOutset, verticalOutset = PickPressOutset),
                role = Role.Checkbox,
                onValueChange = onToggle,
            )
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(PickAvatarSize)) {
            Avatar(
                riotId = friend.riotId,
                size = PickAvatarSize,
                modifier = if (selected) Modifier.border(2.dp, colors.accent, CircleShape) else Modifier,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(colors.accent, CircleShape)
                        // 시트 바탕과 같은 색으로 테두리를 둘러 아바타와 떼어 낸다
                        .border(2.dp, if (colors.isDark) colors.raised else colors.bg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    OvalitIcon(OvalitIcons.Check, contentDescription = null, tint = colors.onAccent, size = 10.dp)
                }
            }
        }
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(
            text = friend.riotId.substringBefore('#'),
            style = OvalitTheme.typography.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) colors.t1 else colors.t2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickLabel(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(bottom = OvalitSpacing.sm),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
    )
}

/** 다른 시간으로 답하거나 보낸 초대의 시각을 옮길 때 띄우는 시트입니다. 지금 시각은 빼고 띄웁니다. */
@Composable
internal fun PingTimeSheet(
    moving: Boolean,
    current: Instant,
    slots: List<Instant>,
    now: Instant,
    timeZone: TimeZone,
    onPick: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberOvalitHaptics()
    val choices = remember(slots, current) { slots.filterNot { it == current } }
    var index by remember { mutableStateOf(0) }
    OvalitBottomSheet(
        title = stringResource(if (moving) Res.string.ping_time_move_title else Res.string.ping_time_reply_title),
        onDismiss = onDismiss,
    ) {
        OvalitWheelPicker(items = choices.map { pingClockText(it, now, timeZone) }, selected = index, onSelect = { index = it })
        Spacer(Modifier.height(OvalitSpacing.xl))
        OvalitPrimaryButton(
            text = stringResource(if (moving) Res.string.ping_time_move_confirm else Res.string.ping_time_reply_confirm),
            enabled = choices.isNotEmpty(),
            onClick = {
                haptics.confirm()
                choices.getOrNull(index)?.let(onPick)
            },
        )
    }
}
