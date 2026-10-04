package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDisclosureIcon
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitExpandable
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.BuyType
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Side
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.PlacementLabel
import com.ovalit.core.ui.TierEmblem
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.annotated
import com.ovalit.core.ui.kdaText
import com.ovalit.core.ui.columnLabel
import com.ovalit.core.ui.label
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.resultColor
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.close
import com.ovalit.feature.match.resources.ending_defused
import com.ovalit.feature.match.resources.ending_detonated
import com.ovalit.feature.match.resources.ending_elimination
import com.ovalit.feature.match.resources.ending_surrendered
import com.ovalit.feature.match.resources.ending_time_expired
import com.ovalit.feature.match.resources.enemy_team
import com.ovalit.feature.match.resources.friend_badge
import com.ovalit.feature.match.resources.me
import com.ovalit.feature.match.resources.my_team
import com.ovalit.feature.match.resources.player_accept
import com.ovalit.feature.match.resources.player_app_user
import com.ovalit.feature.match.resources.player_invite
import com.ovalit.feature.match.resources.player_not_app_user
import com.ovalit.feature.match.resources.player_open_profile
import com.ovalit.feature.match.resources.player_request_sent
import com.ovalit.feature.match.resources.player_requested_me
import com.ovalit.feature.match.resources.player_send_request
import com.ovalit.feature.match.resources.player_stat_first_deaths
import com.ovalit.feature.match.resources.player_stat_first_kills
import com.ovalit.feature.match.resources.player_stat_multi_kills
import com.ovalit.feature.match.resources.player_stats_collapse
import com.ovalit.feature.match.resources.player_stats_expand
import com.ovalit.feature.match.resources.player_unknown
import com.ovalit.feature.match.resources.scoreboard_note
import com.ovalit.feature.match.resources.side_attack
import com.ovalit.feature.match.resources.side_defense
import org.jetbrains.compose.resources.stringResource

private val ChevronWidth = 16.dp

// 숫자 열은 글자 크기를 따라 넓힌다. 고정 폭이면 글자를 키웠을 때 "14/16/4"가 "14/1"로 잘린다.
// 대신 이름 칸이 줄고 이름은 말줄임표로 끝난다.
private class ColumnWidths(val kda: Dp, val acs: Dp)

@Composable
private fun columnWidths(): ColumnWidths {
    val scale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    return ColumnWidths(kda = 96.dp * scale, acs = 52.dp * scale)
}

@Composable
internal fun Scoreboard(uiState: MatchDetailUiState.Success, onOpenPlayer: (ScoreboardRow) -> Unit) {
    val colors = OvalitTheme.colors
    // 한 번에 한 사람만 펼친다. 여럿을 펼치면 스코어보드가 길어져 팀 머리가 화면 밖으로 밀린다.
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val row = @Composable { line: ScoreboardRow ->
        val id = line.line.player.value
        PlayerRow(line, uiState, onOpenPlayer, expanded = expanded == id, onToggle = { expanded = if (expanded == id) null else id })
    }
    Column {
        TeamHeader(stringResource(Res.string.my_team), resultColor(uiState.match.myTeamWon), showColumns = true)
        uiState.myTeam.forEach { row(it) }
        TeamHeader(stringResource(Res.string.enemy_team), colors.t2, showColumns = false)
        uiState.enemyTeam.forEach { row(it) }
        OvalitText(
            text = stringResource(Res.string.scoreboard_note),
            modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 14.dp),
            style = OvalitTheme.typography.caption,
            color = colors.t4,
        )
    }
}

@Composable
private fun TeamHeader(title: String, color: Color, showColumns: Boolean) {
    val colors = OvalitTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(text = title, modifier = Modifier.weight(1f), style = OvalitTheme.typography.label, color = color)
        if (showColumns) {
            val widths = columnWidths()
            ColumnLabel(stringResource(FixedMetric.KDA.label), widths.kda)
            ColumnLabel(stringResource(FixedMetric.COMBAT_SCORE.label), widths.acs)
            Spacer(Modifier.width(ChevronWidth))
        }
    }
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = colors.lineWeak)
}

@Composable
private fun ColumnLabel(text: String, width: Dp) {
    OvalitText(
        text = text,
        modifier = Modifier.width(width),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

/**
 * 스코어보드 한 줄입니다. 얼굴과 이름을 누르면 나와 친구는 프로필, 다른 사람은 친구 요청이나 초대 시트가 뜨고, 나머지를
 * 누르면 그 판 기록을 펼칩니다(사용자 요청, 2026-10-04). op.gg처럼 K/D/A와 ADR 밖의 기록도 볼 수 있습니다. 오른쪽 끝 화살표는
 * 아래를 가리키고 펼치면 위로 돕니다.
 */
@Composable
private fun PlayerRow(
    row: ScoreboardRow,
    uiState: MatchDetailUiState.Success,
    onOpenPlayer: (ScoreboardRow) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = OvalitTheme.colors
    val line = row.line
    val isMe = row.relation == PlayerRelation.ME
    val isFriend = row.relation == PlayerRelation.FRIEND
    val emphasized = isMe || isFriend
    val numberStyle = OvalitTheme.typography.metricS.copy(fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Medium)
    val numberColor = if (isMe) colors.t1 else colors.t2
    val widths = columnWidths()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isMe) Modifier.background(colors.raised) else Modifier)
            .clickable(
                onClickLabel = stringResource(if (expanded) Res.string.player_stats_collapse else Res.string.player_stats_expand),
                role = Role.Button,
                onClick = onToggle,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .padding(horizontal = OvalitSpacing.gutter, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // 얼굴과 이름만 따로 눌린다. 빈 자리까지 넓히면 펼치려고 누른 줄이 프로필로 넘어간다.
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(
                            onClickLabel = if (emphasized) stringResource(Res.string.player_open_profile) else null,
                            role = Role.Button,
                        ) { onOpenPlayer(row) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 친구는 얼굴 오른쪽 아래에 작은 표시를 단다. 이름 옆 글자로 두니 칩 사이에서 떠 보였다(사용자 요청, 2026-10-04).
                    Box {
                        AgentImage(line.agent, uiState.catalog.agentName(line.agent), Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
                        if (isFriend) FriendMark(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp), border = if (isMe) colors.raised else colors.bg)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        OvalitText(
                            text = if (isMe) stringResource(Res.string.me) else line.riotId,
                            modifier = Modifier.weight(1f, fill = false),
                            style = OvalitTheme.typography.label.copy(
                                fontWeight = when {
                                    isMe -> FontWeight.Bold
                                    isFriend -> FontWeight.Medium
                                    else -> FontWeight.Normal
                                },
                            ),
                            color = if (emphasized) colors.t1 else colors.t2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = shrinkToFit(OvalitTheme.typography.label.fontSize),
                        )
                        // MVP와 팀 MVP는 네모로, 나머지는 "3등"으로 적는다(사용자 요청, 2026-10-04)
                        row.placement?.let { PlacementLabel(it) }
                        line.tier?.let { TierEmblem(it, Modifier.size(15.dp)) }
                    }
                }
            }
            // 무기 표처럼 KDA를 굵게 구간 색으로 두고 옆에 K/D/A 합계를 흐리게 붙인다("1.83 (17/15/3)", 사용자 요청, 2026-10-04)
            val kda = line.deaths.takeIf { it > 0 }?.let { (line.kills + line.assists).toDouble() / it }
            OvalitText(
                text = kdaText(kda, line.kills, line.deaths, line.assists).annotated(),
                modifier = Modifier.width(widths.kda),
                style = numberStyle,
                color = numberColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                autoSize = shrinkToFit(numberStyle.fontSize),
            )
            // 게임 스코어보드처럼 K/D/A 옆에는 전투점수를 둔다. 줄도 이 숫자 순이다. ADR은 펼친 기록에 있다(사용자 요청, 2026-10-04).
            OvalitText(
                text = line.acs?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
                modifier = Modifier.width(widths.acs),
                style = numberStyle,
                color = numberColor,
                textAlign = TextAlign.End,
            )
            Box(modifier = Modifier.width(ChevronWidth), contentAlignment = Alignment.CenterEnd) {
                OvalitDisclosureIcon(expanded = expanded, pointsDown = true)
            }
        }
        OvalitExpandable(visible = expanded) { PlayerStats(row) }
    }
}

// 펼친 줄의 그 판 기록이다. K/D/A, KDA, 전투점수는 줄에 있어 나머지 다섯을 한 줄에 놓는다. 이름 밑으로 들여 쓰면 왼쪽이
// 비어 칸이 좁아져서 줄 왼쪽 끝부터 폭을 다 쓴다(사용자 요청, 2026-10-04).
@Composable
private fun PlayerStats(row: ScoreboardRow) {
    val colors = OvalitTheme.colors
    val line = row.line
    val stats = row.stats
    // 분모가 0이면 비운다(CLAUDE.md 계산)
    val headshot = line.shots?.takeIf { it.total > 0 }?.let { it.head.toDouble() / it.total }
    val cells = listOf(
        stringResource(FixedMetric.DAMAGE.label) to (line.adr?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE),
        stringResource(FixedMetric.HEADSHOT_RATE.columnLabel) to (headshot?.let { percentText(it) } ?: NO_VALUE),
        stringResource(Res.string.player_stat_first_kills) to (stats?.firstKills?.toString() ?: NO_VALUE),
        stringResource(Res.string.player_stat_first_deaths) to (stats?.firstDeaths?.toString() ?: NO_VALUE),
        stringResource(Res.string.player_stat_multi_kills) to (stats?.multiKillRounds?.toString() ?: NO_VALUE),
    )
    val label = OvalitTheme.typography.caption
    val value = OvalitTheme.typography.metricS.copy(fontSize = 15.sp, lineHeight = 20.sp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = 2.dp, bottom = 12.dp),
    ) {
        cells.forEach { (name, shown) ->
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                OvalitText(text = name, style = label, color = colors.t3, maxLines = 1, autoSize = shrinkToFit(label.fontSize))
                OvalitText(text = shown, style = value, color = colors.t1, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs),
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
internal fun sideName(side: Side): String = stringResource(if (side == Side.ATTACK) Res.string.side_attack else Res.string.side_defense)

@Composable
internal fun endingName(ending: RoundEnding): String = stringResource(
    when (ending) {
        RoundEnding.ELIMINATION -> Res.string.ending_elimination
        RoundEnding.SPIKE_DETONATED -> Res.string.ending_detonated
        RoundEnding.SPIKE_DEFUSED -> Res.string.ending_defused
        RoundEnding.TIME_EXPIRED -> Res.string.ending_time_expired
        RoundEnding.SURRENDERED -> Res.string.ending_surrendered
    },
)

@Composable
internal fun buyName(type: BuyType): String = stringResource(type.label)

@Composable
internal fun PlayerSheet(
    row: ScoreboardRow,
    onSendRequest: () -> Unit,
    onAccept: () -> Unit,
    onShareInvite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberOvalitHaptics()
    val body = stringResource(
        when (row.relation) {
            PlayerRelation.APP_USER -> Res.string.player_app_user
            PlayerRelation.REQUEST_SENT -> Res.string.player_request_sent
            PlayerRelation.REQUESTED_ME -> Res.string.player_requested_me
            PlayerRelation.UNKNOWN -> Res.string.player_unknown
            else -> Res.string.player_not_app_user
        },
    )
    OvalitBottomSheet(title = row.line.riotId, body = body, onDismiss = onDismiss) {
        when (row.relation) {
            PlayerRelation.APP_USER -> OvalitPrimaryButton(
                text = stringResource(Res.string.player_send_request),
                onClick = {
                    haptics.confirm()
                    onSendRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            PlayerRelation.REQUESTED_ME -> OvalitPrimaryButton(
                text = stringResource(Res.string.player_accept),
                onClick = {
                    haptics.confirm()
                    onAccept()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            // 앱을 쓰는지 몰라도 초대 링크로는 친구를 맺을 수 있다
            PlayerRelation.NOT_APP_USER, PlayerRelation.UNKNOWN -> OvalitPrimaryButton(
                text = stringResource(Res.string.player_invite),
                onClick = {
                    onShareInvite()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Unit
        }
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitTextButton(text = stringResource(Res.string.close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

// 얼굴에 붙이는 친구 표시다. 줄 바탕색 테두리로 얼굴과 떼어 놓는다.
@Composable
private fun FriendMark(modifier: Modifier, border: Color) {
    val label = stringResource(Res.string.friend_badge)
    Box(
        modifier = modifier
            .size(16.dp)
            .background(OvalitTheme.colors.accent, CircleShape)
            .border(1.5.dp, border, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        OvalitIcon(OvalitIcons.Friends, contentDescription = null, tint = OvalitTheme.colors.onAccent, size = 10.dp)
    }
}
