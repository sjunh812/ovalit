package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDisclosureIcon
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitExpandable
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.component.currentMaxWidth
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.BuyType
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.acsOf
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Side
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.PlacementLabel
import com.ovalit.core.model.MatchPlacement
import com.ovalit.core.ui.TierEmblem
import com.ovalit.core.ui.annotated
import com.ovalit.core.ui.columnLabel
import com.ovalit.core.ui.kdaText
import com.ovalit.core.ui.label
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.rankText
import com.ovalit.core.ui.resultColor
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.standingColor
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
import com.ovalit.feature.match.resources.my_team_rank
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
import com.ovalit.feature.match.resources.scoreboard_everyone
import com.ovalit.feature.match.resources.scoreboard_note
import com.ovalit.feature.match.resources.scoreboard_note_names
import com.ovalit.feature.match.resources.side_attack
import com.ovalit.feature.match.resources.side_defense
import org.jetbrains.compose.resources.stringResource

private val ChevronWidth = 16.dp
private val TierSize = 16.dp
private val TierGap = 6.dp
private val FaceSize = 32.dp
private val FaceGap = 10.dp

// "닉네임#KR1"의 태그는 이름보다 덜 중요해서 한 단계 작고 옅게 둔다.
// 글자 크기를 키우면 같이 커지게 em으로 준다.
private fun riotIdText(riotId: String, tagColor: Color): AnnotatedString = buildAnnotatedString {
    append(riotId.substringBefore('#'))
    val tag = riotId.substringAfter('#', missingDelimiterValue = "")
    if (tag.isNotEmpty()) {
        withStyle(SpanStyle(fontSize = 0.85.em, color = tagColor, fontWeight = FontWeight.Normal)) { append("#$tag") }
    }
}

// 숫자 열은 글자 크기만큼 넓힌다.
// 고정 폭이면 큰 글씨에서 "14/16/4"가 "14/1"로 잘린다.
// 줄어든 폭은 이름 칸이 내준다.
// 이름 칸이 [MinNameWidth]도 남지 않으면 숫자 열을 이름 밑 줄로 내린다([stacked]).
// 큰 글꼴의 좁은 폰에서 이름이 사라지고 "우리 팀"이 한 자씩 꺾였다.
private class ColumnWidths(val kda: Dp, val acs: Dp, val stacked: Boolean)

private val MinNameWidth = 72.dp

@Composable
private fun columnWidths(rowWidth: Dp, tierSlot: Boolean, roundBased: Boolean): ColumnWidths {
    val scale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val kda = 96.dp * scale
    val acs = 52.dp * scale
    val numbers = kda + if (roundBased) acs + ChevronWidth else 0.dp
    val name = FaceSize + FaceGap + (if (tierSlot) TierSize + TierGap else 0.dp) + MinNameWidth
    return ColumnWidths(kda = kda, acs = acs, stacked = rowWidth - OvalitSpacing.screen * 2 < name + numbers)
}

/**
 * S3 스코어보드입니다.
 * 두 팀 모드는 우리 팀과 상대 팀, 데스매치는 모두를 한 순위로, 건틀릿은 팀마다 등수 순으로 묶습니다.
 * 라운드가 없는 모드는 전투 점수 열을 두지 않고 줄도 펼치지 않습니다.
 * 둘 다 라운드로 나누거나 라운드 기록으로 세는 값입니다.
 */
@Composable
internal fun Scoreboard(uiState: MatchDetailUiState.Success, onOpenPlayer: (ScoreboardRow) -> Unit) {
    val colors = OvalitTheme.colors
    val roundBased = uiState.match.format == MatchFormat.ROUNDS
    // 한 번에 한 사람만 펼친다.
    // 여럿을 펼치면 스코어보드가 길어져 팀 머리가 화면 밖으로 밀린다.
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    // 티어가 없는 사람이 섞여도 이름이 같은 자리에서 시작하게, 한 사람이라도 티어가 있으면 모든 줄이 그 자리를 비운다
    val tierSlot = uiState.rows.any { it.line.tier != null }
    BoxWithConstraints {
        val widths = columnWidths(currentMaxWidth, tierSlot = tierSlot, roundBased = roundBased)
        val row = @Composable { line: ScoreboardRow ->
            val id = line.line.player.value
            PlayerRow(
                row = line,
                uiState = uiState,
                onOpenPlayer = onOpenPlayer,
                tierSlot = tierSlot,
                roundBased = roundBased,
                widths = widths,
                expanded = expanded == id,
                onToggle = { expanded = if (expanded == id) null else id },
            )
        }
        Column {
            uiState.groups.forEachIndexed { index, group ->
                val (title, color) = groupTitle(group, uiState)
                TeamHeader(title, color, showColumns = index == 0, roundBased = roundBased, widths = widths)
                group.rows.forEach { row(it) }
            }
            OvalitText(
                text = stringResource(if (roundBased) Res.string.scoreboard_note else Res.string.scoreboard_note_names),
                modifier = Modifier.padding(start = OvalitSpacing.screen, end = OvalitSpacing.screen, top = 14.dp),
                style = OvalitTheme.typography.caption,
                // 읽어야 하는 안내라 S6, S7의 안내처럼 t3다
                color = colors.t3,
            )
        }
    }
}

// 우리 팀은 결과 색, 등수로 끝나는 경기는 등수 색이다.
// 다른 묶음은 흐리게 둔다.
@Composable
private fun groupTitle(group: ScoreboardGroup, uiState: MatchDetailUiState.Success): Pair<String, Color> {
    val colors = OvalitTheme.colors
    val rank = group.rank
    return when (group.side) {
        ScoreboardSide.MY_TEAM -> if (rank == null) {
            stringResource(Res.string.my_team) to resultColor(uiState.match.myTeamWon)
        } else {
            stringResource(Res.string.my_team_rank, rankText(rank)) to standingColor(rank)
        }
        ScoreboardSide.ENEMY_TEAM -> stringResource(Res.string.enemy_team) to colors.t2
        ScoreboardSide.EVERYONE -> stringResource(Res.string.scoreboard_everyone) to colors.t2
        ScoreboardSide.OTHER_TEAM -> rankText(rank ?: 0) to colors.t2
    }
}

@Composable
private fun TeamHeader(title: String, color: Color, showColumns: Boolean, roundBased: Boolean, widths: ColumnWidths) {
    val colors = OvalitTheme.colors
    val labels = @Composable {
        ColumnLabel(stringResource(FixedMetric.KDA.label), widths.kda)
        if (roundBased) {
            ColumnLabel(stringResource(FixedMetric.COMBAT_SCORE.columnLabel), widths.acs)
            Spacer(Modifier.width(ChevronWidth))
        }
    }
    val padding = Modifier.fillMaxWidth().padding(start = OvalitSpacing.screen, end = OvalitSpacing.screen, top = OvalitSpacing.lg, bottom = 6.dp)
    if (widths.stacked) {
        // 숫자 열을 이름 밑으로 내린 줄과 맞춰 열 이름도 팀 이름 밑에 오른쪽으로 붙인다
        Column(modifier = padding) {
            OvalitText(text = title, style = OvalitTheme.typography.label, color = color)
            if (showColumns) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { labels() }
            }
        }
    } else {
        Row(modifier = padding, verticalAlignment = Alignment.CenterVertically) {
            OvalitText(text = title, modifier = Modifier.weight(1f), style = OvalitTheme.typography.label, color = color)
            if (showColumns) labels()
        }
    }
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.screen), color = colors.lineWeak)
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

/** 얼굴과 이름을 누르면 [onOpenPlayer]를 부르고, 라운드제 모드면 줄의 나머지를 누르면 그 판 기록을 펼칩니다. */
@Composable
private fun PlayerRow(
    row: ScoreboardRow,
    uiState: MatchDetailUiState.Success,
    onOpenPlayer: (ScoreboardRow) -> Unit,
    tierSlot: Boolean,
    roundBased: Boolean,
    widths: ColumnWidths,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = OvalitTheme.colors
    val line = row.line
    val isMe = row.relation == PlayerRelation.ME
    val isFriend = row.relation == PlayerRelation.FRIEND
    val emphasized = isMe || isFriend
    // 숫자는 모두 Medium이다.
    // 내 줄은 면(`--raised`)과 이름 굵기로 이미 가려서 숫자는 밝기만 올린다.
    val numberStyle = OvalitTheme.typography.metricS.copy(fontWeight = FontWeight.Medium)
    val numberColor = if (isMe) colors.t1 else colors.t2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isMe) Modifier.background(colors.raised) else Modifier)
            .then(
                if (roundBased) {
                    Modifier.clickable(
                        onClickLabel = stringResource(if (expanded) Res.string.player_stats_collapse else Res.string.player_stats_expand),
                        role = Role.Button,
                        onClick = onToggle,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val nameBlock = @Composable { modifier: Modifier ->
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                // 얼굴과 이름만 따로 눌린다.
                // 빈 자리까지 넓히면 펼치려고 누른 줄이 프로필로 넘어간다.
                // 눌리는 높이는 줄 높이와 같은 48dp다(안드로이드 눌림 영역 기준).
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = 48.dp)
                        .clickable(
                            onClickLabel = if (emphasized) stringResource(Res.string.player_open_profile) else null,
                            role = Role.Button,
                        ) { onOpenPlayer(row) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        AgentImage(line.agent, uiState.catalog.agents[line.agent], Modifier.size(FaceSize).clip(RoundedCornerShape(9.dp)))
                        if (isFriend) FriendMark(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp), border = if (isMe) colors.raised else colors.bg)
                    }
                    Spacer(Modifier.width(FaceGap))
                    // 티어는 이름 앞에 둔다.
                    // 줄마다 같은 자리라 위아래로 훑으면 한 줄로 읽힌다.
                    if (tierSlot) {
                        Box(modifier = Modifier.size(TierSize)) { line.tier?.let { TierEmblem(it, Modifier.fillMaxSize()) } }
                        Spacer(Modifier.width(TierGap))
                    }
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // 이름이 길면 작고 옅게 둔 태그부터 잘리고, 자리 칩은 늘 남는다
                        OvalitText(
                            text = if (isMe) AnnotatedString(stringResource(Res.string.me)) else riotIdText(line.riotId, colors.t3),
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
                        )
                        // MVP와 팀 MVP만 칩이고 등수는 옅은 글자다(사용자 결정).
                        // 열 줄 모두 칩이면 칩이 이름보다 먼저 보인다.
                        row.placement?.let { if (it.award == null) RankHint(it) else PlacementLabel(it) }
                    }
                }
            }
        }
        val numbers = @Composable {
            // "1.83 (17/15/3)"처럼 KDA 뒤에 합계를 붙인다
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
            // ADR이 아니라 전투 점수를 둔다.
            // 줄도 이 숫자 순이고 ADR은 펼친 기록에 있다.
            if (roundBased) {
                OvalitText(
                    text = uiState.match.acsOf(line)?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
                    modifier = Modifier.width(widths.acs),
                    style = numberStyle,
                    color = numberColor,
                    textAlign = TextAlign.End,
                )
                Box(modifier = Modifier.width(ChevronWidth), contentAlignment = Alignment.CenterEnd) {
                    OvalitDisclosureIcon(expanded = expanded, pointsDown = true)
                }
            }
        }
        val rowPadding = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.screen)
        if (widths.stacked) {
            // 이름 칸이 모자라면 이름을 한 줄 다 쓰고 숫자는 그 밑에 열 이름과 같은 자리로 둔다
            Column(modifier = rowPadding.padding(bottom = OvalitSpacing.sm)) {
                nameBlock(Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    numbers()
                }
            }
        } else {
            Row(modifier = rowPadding.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                nameBlock(Modifier.weight(1f))
                numbers()
            }
        }
        if (roundBased) OvalitExpandable(visible = expanded) { PlayerStats(row) }
    }
}

/**
 * 스코어보드 이름 옆의 등수("4등")입니다.
 * 칩 없이 작은 `--t3` 글자로 둡니다.
 * 읽어야 하는 글자라 `--t4`(2.2:1)로 두지 않습니다.
 */
@Composable
private fun RankHint(placement: MatchPlacement) {
    OvalitText(
        text = rankText(placement),
        style = OvalitTheme.typography.caption,
        color = OvalitTheme.colors.t3,
        maxLines = 1,
    )
}

// 펼친 줄의 그 판 기록이다.
// 줄에 없는 다섯 가지를 한 줄에 놓는다.
// 이름 밑으로 들여 쓰면 칸이 좁아져서 줄 왼쪽 끝부터 쓴다.
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
            .padding(start = OvalitSpacing.screen, end = OvalitSpacing.screen, top = 2.dp, bottom = 12.dp),
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
        modifier = Modifier.padding(start = OvalitSpacing.screen, end = OvalitSpacing.screen, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs),
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

// 얼굴에 붙이는 친구 표시다.
// 줄 바탕색 테두리로 얼굴과 떼어 놓는다.
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
