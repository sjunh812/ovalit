package com.ovalit.feature.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerAvatar
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.report.FriendStanding
import com.ovalit.feature.report.HomeNudge
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.nudge_invite_body
import com.ovalit.feature.report.resources.nudge_invite_title
import com.ovalit.feature.report.resources.nudge_rival_body
import com.ovalit.feature.report.resources.nudge_rival_title
import com.ovalit.feature.report.resources.rival_pick_body
import com.ovalit.feature.report.resources.rival_pick_matches
import com.ovalit.feature.report.resources.rival_pick_no_matches
import com.ovalit.feature.report.resources.rival_pick_title
import org.jetbrains.compose.resources.stringResource

private val LeadingSize = 40.dp
private val StackedAvatarSize = 32.dp
private val StackedAvatarOverlap = 8.dp
private const val STACKED_AVATARS = 2

/**
 * 친구나 라이벌이 없을 때 라이벌 칸 자리에 두는 칸입니다. 비워 두면 홈이 거기서 끝난 것처럼 보입니다. 누르면
 * 초대 링크를 보내거나 라이벌을 고르는 시트를 엽니다.
 */
@Composable
internal fun NudgeBanner(
    nudge: HomeNudge,
    candidates: List<FriendStanding>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    val (title, body) = when (nudge) {
        HomeNudge.INVITE_FRIEND -> Res.string.nudge_invite_title to Res.string.nudge_invite_body
        HomeNudge.PICK_RIVAL -> Res.string.nudge_rival_title to Res.string.nudge_rival_body
    }

    Row(
        modifier = modifier
            .padding(horizontal = OvalitSpacing.gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.raised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = OvalitSpacing.lg, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (nudge) {
            HomeNudge.INVITE_FRIEND -> Box(
                modifier = Modifier.size(LeadingSize).background(colors.fill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                OvalitIcon(OvalitIcons.Friends, contentDescription = null, tint = colors.t1)
            }
            // 고를 수 있는 친구 얼굴을 겹쳐 보여서 누구와 겨룰지 먼저 떠오르게 한다
            HomeNudge.PICK_RIVAL -> StackedAvatars(candidates.take(STACKED_AVATARS).map { it.riotId })
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            OvalitText(text = stringResource(title), style = OvalitTheme.typography.bodyStrong)
            Spacer(Modifier.height(2.dp))
            OvalitText(text = stringResource(body), style = OvalitTheme.typography.caption, color = colors.t2)
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 16.dp)
    }
}

// 아바타가 Riot ID 첫 글자라 많이 겹치면 글자가 가려진다. 둘까지만, 앞사람이 위로 오게 조금만 겹친다.
@Composable
private fun StackedAvatars(riotIds: List<String>) {
    val colors = OvalitTheme.colors
    val step = StackedAvatarSize - StackedAvatarOverlap
    Box(modifier = Modifier.width(StackedAvatarSize + step * (riotIds.size - 1).coerceAtLeast(0))) {
        riotIds.asReversed().forEachIndexed { reversedIndex, riotId ->
            val index = riotIds.lastIndex - reversedIndex
            PlayerAvatar(
                riotId = riotId,
                modifier = Modifier
                    .padding(start = step * index)
                    .size(StackedAvatarSize)
                    .border(2.dp, colors.raised, CircleShape),
            )
        }
    }
}

/** 전적을 공개한 친구 중에서 라이벌을 고릅니다. 고르면 시트가 닫히고 그 자리에 라이벌 대결이 뜹니다. */
@Composable
internal fun RivalPickerSheet(
    report: WeeklyReport.Ready,
    candidates: List<FriendStanding>,
    onPick: (PlayerId) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OvalitTheme.colors
    val haptics = rememberOvalitHaptics()
    val period = periodLabel(report.period)

    OvalitBottomSheet(
        title = stringResource(Res.string.rival_pick_title),
        body = stringResource(Res.string.rival_pick_body),
        onDismiss = onDismiss,
    ) {
        candidates.forEachIndexed { index, friend ->
            if (index > 0) OvalitDivider(Modifier.padding(start = 52.dp), color = colors.lineWeak)
            val caption = friend.metrics?.let { stringResource(Res.string.rival_pick_matches, period, it.matches) }
                ?: stringResource(Res.string.rival_pick_no_matches, period)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(role = Role.Button) {
                        haptics.confirm()
                        onPick(friend.id)
                    }
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerAvatar(riotId = friend.riotId, modifier = Modifier.size(LeadingSize))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    OvalitText(
                        text = friend.riotId,
                        style = OvalitTheme.typography.bodyStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OvalitText(text = caption, style = OvalitTheme.typography.caption, color = colors.t3)
                }
            }
        }
    }
}
