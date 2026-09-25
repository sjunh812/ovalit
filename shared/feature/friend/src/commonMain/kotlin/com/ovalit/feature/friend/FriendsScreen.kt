package com.ovalit.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitOutlinedButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.FriendRequestSource
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.friend.resources.Res
import com.ovalit.feature.friend.resources.accept
import com.ovalit.feature.friend.resources.decline
import com.ovalit.feature.friend.resources.empty_body
import com.ovalit.feature.friend.resources.empty_title
import com.ovalit.feature.friend.resources.friend_caption
import com.ovalit.feature.friend.resources.friend_caption_few
import com.ovalit.feature.friend.resources.friend_caption_resting
import com.ovalit.feature.friend.resources.friend_private
import com.ovalit.feature.friend.resources.friends_count
import com.ovalit.feature.friend.resources.friends_title
import com.ovalit.feature.friend.resources.invite
import com.ovalit.feature.friend.resources.invite_note
import com.ovalit.feature.friend.resources.requests_title
import com.ovalit.feature.friend.resources.request_from_invite
import com.ovalit.feature.friend.resources.request_from_scoreboard
import com.ovalit.feature.friend.resources.rival
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val AvatarSize = 40.dp
private val RequestTextMinWidth = 150.dp

/**
 * @param onShareInvite 초대 링크를 받습니다. 공유 시트는 플랫폼마다 달라서 앱 모듈이 띄웁니다.
 */
@Composable
fun FriendsRoute(
    onOpenFriend: (PlayerId) -> Unit,
    onShareInvite: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FriendsScreen(
        uiState = uiState,
        onOpenFriend = onOpenFriend,
        onInvite = { onShareInvite(viewModel.inviteLink()) },
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        modifier = modifier,
    )
}

@Composable
internal fun FriendsScreen(
    uiState: FriendsUiState,
    onOpenFriend: (PlayerId) -> Unit,
    onInvite: () -> Unit,
    onAccept: (PlayerId) -> Unit,
    onDecline: (PlayerId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is FriendsUiState.Success) return@Box

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(OvalitSpacing.gutter))
            OvalitText(
                text = stringResource(Res.string.friends_title),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.titleL,
            )
            Spacer(Modifier.height(OvalitSpacing.lg))
            Column(modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
                OvalitOutlinedButton(text = stringResource(Res.string.invite), onClick = onInvite)
                Spacer(Modifier.height(OvalitSpacing.sm))
                OvalitText(
                    text = stringResource(Res.string.invite_note),
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                )
            }

            if (uiState.requests.isNotEmpty()) {
                SectionHeader(stringResource(Res.string.requests_title, uiState.requests.size))
                uiState.requests.forEachIndexed { index, request ->
                    if (index > 0) RowDivider()
                    RequestRow(request, onAccept = { onAccept(request.id) }, onDecline = { onDecline(request.id) })
                }
            }

            if (uiState.friends.isEmpty()) {
                Spacer(Modifier.height(OvalitSpacing.xxl))
                Column(
                    modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(OvalitSpacing.sm),
                ) {
                    OvalitText(text = stringResource(Res.string.empty_title), style = OvalitTheme.typography.bodyStrong)
                    OvalitText(
                        text = stringResource(Res.string.empty_body),
                        style = OvalitTheme.typography.body,
                        color = colors.t2,
                    )
                }
            } else {
                SectionHeader(stringResource(Res.string.friends_count, uiState.friends.size))
                uiState.friends.forEachIndexed { index, row ->
                    if (index > 0) RowDivider()
                    FriendRowItem(row, isRival = row.friend.id == uiState.rivalId, onClick = { onOpenFriend(row.friend.id) })
                }
            }
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(
            start = OvalitSpacing.gutter,
            end = OvalitSpacing.gutter,
            top = OvalitSpacing.xl,
            bottom = OvalitSpacing.xs,
        ),
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
private fun RowDivider() {
    OvalitDivider(
        modifier = Modifier.padding(start = OvalitSpacing.gutter + AvatarSize + OvalitSpacing.md),
        color = OvalitTheme.colors.lineWeak,
    )
}

@Composable
private fun RequestRow(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(request.riotId, AvatarSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        // 글자를 키워 한 줄에 안 들어가면 버튼이 설명 아래로 내려간다. 설명을 세 줄로 꺾지 않는다.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            RequestText(request, Modifier.widthIn(min = RequestTextMinWidth).weight(1f))
            Row {
                SmallButton(text = stringResource(Res.string.decline), filled = false, onClick = onDecline)
                Spacer(Modifier.width(OvalitSpacing.xs))
                SmallButton(text = stringResource(Res.string.accept), filled = true, onClick = onAccept)
            }
        }
    }
}

@Composable
private fun RequestText(request: FriendRequest, modifier: Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        OvalitText(text = request.riotId, style = OvalitTheme.typography.bodyStrong, maxLines = 1)
        OvalitText(
            text = stringResource(
                when (request.source) {
                    FriendRequestSource.SCOREBOARD -> Res.string.request_from_scoreboard
                    FriendRequestSource.INVITE_LINK -> Res.string.request_from_invite
                },
            ),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
}

@Composable
private fun FriendRowItem(row: FriendRow, isRival: Boolean, onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    val friend = row.friend

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = SemanticsRole.Button, onClick = onClick)
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(friend.riotId, AvatarSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                OvalitText(
                    text = friend.riotId,
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.bodyStrong,
                    maxLines = 1,
                )
                if (isRival) {
                    Spacer(Modifier.width(6.dp))
                    OvalitText(
                        text = stringResource(Res.string.rival),
                        modifier = Modifier.alignByBaseline(),
                        style = OvalitTheme.typography.caption,
                        color = colors.accentInk,
                    )
                }
            }
            OvalitText(text = caption(row), style = OvalitTheme.typography.caption, color = colors.t3, maxLines = 1)
        }
        Spacer(Modifier.width(OvalitSpacing.xs))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 16.dp)
    }
}

@Composable
private fun caption(row: FriendRow): String = when (val report = row.report) {
    null -> stringResource(Res.string.friend_private)
    is WeeklyReport.Ready -> stringResource(Res.string.friend_caption, periodLabel(report.period), report.metrics.matches)
    is WeeklyReport.NotEnoughMatches -> if (report.played == 0) {
        stringResource(Res.string.friend_caption_resting)
    } else {
        stringResource(Res.string.friend_caption_few, report.played)
    }
}
