package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.BuyType
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.RoundSummary
import com.ovalit.core.model.Side
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.TierEmblem
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.match_kda
import com.ovalit.core.ui.resultColor
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.withThousands
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.buy_eco
import com.ovalit.feature.match.resources.buy_force
import com.ovalit.feature.match.resources.buy_full
import com.ovalit.feature.match.resources.buy_pistol
import com.ovalit.feature.match.resources.buy_record
import com.ovalit.feature.match.resources.close
import com.ovalit.feature.match.resources.column_adr
import com.ovalit.feature.match.resources.column_kda
import com.ovalit.feature.match.resources.economy_note
import com.ovalit.feature.match.resources.economy_rounds_title
import com.ovalit.feature.match.resources.economy_versus
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
import com.ovalit.feature.match.resources.player_request_sent
import com.ovalit.feature.match.resources.player_requested_me
import com.ovalit.feature.match.resources.player_send_request
import com.ovalit.feature.match.resources.round_ace
import com.ovalit.feature.match.resources.round_clutch
import com.ovalit.feature.match.resources.round_first_death
import com.ovalit.feature.match.resources.round_first_kill
import com.ovalit.feature.match.resources.round_kills
import com.ovalit.feature.match.resources.round_lost
import com.ovalit.feature.match.resources.round_not_played
import com.ovalit.feature.match.resources.round_won
import com.ovalit.feature.match.resources.scoreboard_note
import com.ovalit.feature.match.resources.section_first_half
import com.ovalit.feature.match.resources.section_overtime
import com.ovalit.feature.match.resources.section_second_half
import com.ovalit.feature.match.resources.side_attack
import com.ovalit.feature.match.resources.side_defense
import org.jetbrains.compose.resources.stringResource

private val ChevronWidth = 16.dp

// 숫자 열은 글자 크기를 따라 넓힌다. 고정 폭이면 글자를 키웠을 때 "14/16/4"가 "14/1"로 잘린다.
// 대신 이름 칸이 줄고 이름은 말줄임표로 끝난다.
private class ColumnWidths(val kda: Dp, val adr: Dp)

@Composable
private fun columnWidths(): ColumnWidths {
    val scale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    return ColumnWidths(kda = 64.dp * scale, adr = 38.dp * scale)
}

@Composable
internal fun Scoreboard(uiState: MatchDetailUiState.Success, onOpenPlayer: (ScoreboardRow) -> Unit) {
    val colors = OvalitTheme.colors
    Column {
        TeamHeader(stringResource(Res.string.my_team), resultColor(uiState.match.myTeamWon), showColumns = true)
        uiState.myTeam.forEach { PlayerRow(it, uiState, onOpenPlayer) }
        TeamHeader(stringResource(Res.string.enemy_team), colors.t2, showColumns = false)
        uiState.enemyTeam.forEach { PlayerRow(it, uiState, onOpenPlayer) }
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
            ColumnLabel(stringResource(Res.string.column_kda), widths.kda)
            ColumnLabel(stringResource(Res.string.column_adr), widths.adr)
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

@Composable
private fun PlayerRow(row: ScoreboardRow, uiState: MatchDetailUiState.Success, onOpenPlayer: (ScoreboardRow) -> Unit) {
    val colors = OvalitTheme.colors
    val line = row.line
    val isMe = row.relation == PlayerRelation.ME
    val isFriend = row.relation == PlayerRelation.FRIEND
    val emphasized = isMe || isFriend
    val numberStyle = OvalitTheme.typography.metricS.copy(fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Medium)
    val numberColor = if (isMe) colors.t1 else colors.t2
    val widths = columnWidths()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .then(if (isMe) Modifier.background(colors.raised) else Modifier.clickable(role = Role.Button) { onOpenPlayer(row) })
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = OvalitSpacing.gutter, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentImage(line.agent, uiState.catalog.agentName(line.agent), Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(10.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
            if (isFriend) {
                OvalitText(text = stringResource(Res.string.friend_badge), style = OvalitTheme.typography.caption, color = colors.accentInk)
            }
            line.tier?.let { TierEmblem(it, Modifier.size(15.dp)) }
        }
        OvalitText(
            text = stringResource(CoreUiRes.string.match_kda, line.kills, line.deaths, line.assists),
            modifier = Modifier.width(widths.kda),
            style = numberStyle,
            color = numberColor,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        OvalitText(
            text = line.adr?.let { MetricFormat.INTEGER.format(it) } ?: "–",
            modifier = Modifier.width(widths.adr),
            style = numberStyle,
            color = numberColor,
            textAlign = TextAlign.End,
        )
        Box(modifier = Modifier.width(ChevronWidth), contentAlignment = Alignment.CenterEnd) {
            if (isFriend) OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t3, size = 14.dp)
        }
    }
}

@Composable
internal fun RoundList(uiState: MatchDetailUiState.Success) {
    val half = uiState.match.queue.halfRounds ?: return
    val sections = uiState.rounds.groupBy { minOf((it.number - 1) / half, 2) }
    Column {
        sections.forEach { (part, rounds) ->
            SectionLabel(
                stringResource(
                    when (part) {
                        0 -> Res.string.section_first_half
                        1 -> Res.string.section_second_half
                        else -> Res.string.section_overtime
                    },
                ),
            )
            rounds.forEach { RoundRow(it) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs),
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
private fun RoundRow(round: RoundSummary) {
    val colors = OvalitTheme.colors
    val result = stringResource(if (round.won) Res.string.round_won else Res.string.round_lost)
    val detail = if (!round.played) {
        stringResource(Res.string.round_not_played)
    } else {
        listOfNotNull(round.side?.let { sideName(it) }, round.ending?.let { endingName(it) }).joinToString(" · ")
    }
    val mine = if (round.played) {
        listOfNotNull(
            round.myKills.takeIf { it > 0 }?.let { stringResource(Res.string.round_kills, it) },
            stringResource(Res.string.round_first_kill).takeIf { round.firstKill },
            stringResource(Res.string.round_first_death).takeIf { round.firstDeath },
        ).joinToString(" · ")
    } else {
        ""
    }
    // 이긴 클러치만 적는다. 나만 남았다가 진 라운드까지 적으면 진 라운드마다 꼬리표가 붙는다.
    val highlight = round.highlight?.let { scene ->
        val clutch = scene.clutch
        when {
            scene.ace -> stringResource(Res.string.round_ace)
            clutch != null && clutch.won -> stringResource(Res.string.round_clutch, clutch.against)
            else -> null
        }
    }
    val description = listOfNotNull("${round.number}", result, detail, highlight, mine).filter { it.isNotEmpty() }.joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = OvalitSpacing.gutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = round.number.toString(),
            modifier = Modifier.width(26.dp),
            style = OvalitTheme.typography.metricS,
            color = colors.t3,
        )
        ResultMark(round.won)
        Spacer(Modifier.width(OvalitSpacing.md))
        DetailWithTrailing(
            detail = {
                OvalitText(
                    text = detail,
                    style = OvalitTheme.typography.body,
                    color = if (round.played) colors.t1 else colors.t3,
                    maxLines = 1,
                )
            },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 에이스와 클러치는 그 라운드에서 가장 눈에 띄는 일이라 한 단계 밝고 굵게 둔다
                    if (highlight != null) {
                        OvalitText(
                            text = highlight,
                            style = OvalitTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.t1,
                            maxLines = 1,
                        )
                        if (mine.isNotEmpty()) Spacer(Modifier.width(OvalitSpacing.sm))
                    }
                    OvalitText(text = mine, style = OvalitTheme.typography.caption, color = colors.t2, maxLines = 1)
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 라운드 줄의 설명과 오른쪽 끝 꼬리(에이스, 킬, 퍼블)입니다. 한 줄에 들어가면 꼬리를 오른쪽 끝에 두고, 안 들어가면
 * 꼬리를 설명 밑 오른쪽에 내립니다. 한 줄에 억지로 넣으면 "공격 · 스파이크 폭발"이 "공격 ·"에서 잘립니다.
 */
@Composable
private fun DetailWithTrailing(detail: @Composable () -> Unit, trailing: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(detail, trailing), modifier = modifier) { (detailMeasurables, trailingMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gap = OvalitSpacing.sm.roundToPx()
        val trailingPlaceable = trailingMeasurables.first().measure(loose)
        val detailMeasurable = detailMeasurables.first()
        val beside = trailingPlaceable.width == 0 ||
            detailMeasurable.maxIntrinsicWidth(constraints.maxHeight) + gap + trailingPlaceable.width <= constraints.maxWidth
        if (beside) {
            val besideWidth = if (trailingPlaceable.width == 0) 0 else trailingPlaceable.width + gap
            val detailPlaceable = detailMeasurable.measure(loose.copy(maxWidth = (constraints.maxWidth - besideWidth).coerceAtLeast(0)))
            val height = maxOf(detailPlaceable.height, trailingPlaceable.height)
            layout(constraints.maxWidth, height) {
                detailPlaceable.place(0, (height - detailPlaceable.height) / 2)
                trailingPlaceable.place(constraints.maxWidth - trailingPlaceable.width, (height - trailingPlaceable.height) / 2)
            }
        } else {
            val detailPlaceable = detailMeasurable.measure(loose)
            layout(constraints.maxWidth, detailPlaceable.height + trailingPlaceable.height) {
                detailPlaceable.place(0, 0)
                trailingPlaceable.place(constraints.maxWidth - trailingPlaceable.width, detailPlaceable.height)
            }
        }
    }
}

// 라운드 막대와 같은 모양이다. 이기면 높고 색이 있고, 지면 낮고 흐리다.
@Composable
private fun ResultMark(won: Boolean) {
    Box(modifier = Modifier.size(width = 4.dp, height = 18.dp), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (won) 18.dp else 11.dp)
                .background(if (won) resultColor(true) else OvalitTheme.colors.bar),
        )
    }
}

@Composable
private fun sideName(side: Side): String = stringResource(if (side == Side.ATTACK) Res.string.side_attack else Res.string.side_defense)

@Composable
private fun endingName(ending: RoundEnding): String = stringResource(
    when (ending) {
        RoundEnding.ELIMINATION -> Res.string.ending_elimination
        RoundEnding.SPIKE_DETONATED -> Res.string.ending_detonated
        RoundEnding.SPIKE_DEFUSED -> Res.string.ending_defused
        RoundEnding.TIME_EXPIRED -> Res.string.ending_time_expired
        RoundEnding.SURRENDERED -> Res.string.ending_surrendered
    },
)

@Composable
private fun buyName(type: BuyType): String = stringResource(
    when (type) {
        BuyType.PISTOL -> Res.string.buy_pistol
        BuyType.ECO -> Res.string.buy_eco
        BuyType.FORCE_BUY -> Res.string.buy_force
        BuyType.FULL_BUY -> Res.string.buy_full
    },
)

@Composable
internal fun EconomyList(uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    Column {
        Spacer(Modifier.height(OvalitSpacing.sm))
        uiState.buys.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = OvalitSpacing.gutter, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 좁은 화면에서 글자를 키우면 승률이 "65 / %"로 끊겼다. 이름과 승률은 한 줄로 제 폭을 쓰고,
                // 남는 폭이 모자라면 가운데 라운드 수부터 줄인다.
                OvalitText(text = buyName(record.type), style = OvalitTheme.typography.body, maxLines = 1)
                Spacer(Modifier.width(OvalitSpacing.md))
                OvalitText(
                    text = stringResource(Res.string.buy_record, record.rounds, record.wins),
                    modifier = Modifier.weight(1f),
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    autoSize = shrinkToFit(OvalitTheme.typography.caption.fontSize),
                )
                Spacer(Modifier.width(OvalitSpacing.md))
                OvalitText(
                    text = if (record.rounds > 0) MetricFormat.PERCENT.valueText(record.wins.toDouble() / record.rounds) else "–",
                    modifier = Modifier.widthIn(min = 44.dp),
                    style = OvalitTheme.typography.bodyStrong,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
        OvalitText(
            text = stringResource(Res.string.economy_note),
            modifier = Modifier.padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xs),
            style = OvalitTheme.typography.caption,
            color = colors.t3,
        )
        SectionLabel(stringResource(Res.string.economy_rounds_title))
        uiState.rounds.filter { it.played && it.economy != null }.forEach { round ->
            val economy = round.economy ?: return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = OvalitSpacing.gutter, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OvalitText(
                    text = round.number.toString(),
                    modifier = Modifier.width(26.dp),
                    style = OvalitTheme.typography.metricS,
                    color = colors.t3,
                )
                ResultMark(round.won)
                Spacer(Modifier.width(OvalitSpacing.md))
                OvalitText(
                    text = round.buyType?.let { buyName(it) }.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = OvalitTheme.typography.body,
                    maxLines = 1,
                    autoSize = shrinkToFit(OvalitTheme.typography.body.fontSize),
                )
                OvalitText(
                    text = stringResource(Res.string.economy_versus, economy.teamLoadout.withThousands(), economy.enemyLoadout.withThousands()),
                    style = OvalitTheme.typography.metricS,
                    color = colors.t2,
                    maxLines = 1,
                )
            }
        }
    }
}

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
            PlayerRelation.NOT_APP_USER -> OvalitPrimaryButton(
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
