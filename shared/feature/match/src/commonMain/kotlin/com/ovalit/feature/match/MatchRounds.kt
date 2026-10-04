package com.ovalit.feature.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.RoundKill
import com.ovalit.core.model.RoundSummary
import com.ovalit.core.model.WinRecord
import com.ovalit.core.model.roundsOverview
import com.ovalit.core.ui.AgentImage
import com.ovalit.core.ui.ResultLabel
import com.ovalit.core.ui.SEPARATOR
import com.ovalit.core.ui.agentName
import com.ovalit.core.ui.resultColor
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withThousands
import com.ovalit.feature.match.resources.Res
import com.ovalit.feature.match.resources.buy_record
import com.ovalit.feature.match.resources.buy_title
import com.ovalit.feature.match.resources.economy_note
import com.ovalit.feature.match.resources.economy_upsets
import com.ovalit.feature.match.resources.enemy_team
import com.ovalit.feature.match.resources.me
import com.ovalit.feature.match.resources.my_team
import com.ovalit.feature.match.resources.overview_attack
import com.ovalit.feature.match.resources.overview_defense
import com.ovalit.feature.match.resources.overview_first_blood_ours
import com.ovalit.feature.match.resources.overview_first_blood_theirs
import com.ovalit.feature.match.resources.overview_record
import com.ovalit.feature.match.resources.overview_win_of
import com.ovalit.feature.match.resources.round_ace
import com.ovalit.feature.match.resources.round_clutch
import com.ovalit.feature.match.resources.round_first_death
import com.ovalit.feature.match.resources.round_first_kill
import com.ovalit.feature.match.resources.round_kill_ability
import com.ovalit.feature.match.resources.round_kill_order
import com.ovalit.feature.match.resources.round_kills
import com.ovalit.feature.match.resources.round_loadout
import com.ovalit.feature.match.resources.round_my_damage
import com.ovalit.feature.match.resources.round_no_kills
import com.ovalit.feature.match.resources.round_not_played
import com.ovalit.feature.match.resources.round_pick
import com.ovalit.feature.match.resources.round_title
import org.jetbrains.compose.resources.stringResource

/**
 * S3 라운드 탭입니다. op.gg와 tracker.gg처럼 위에 그 판의 흐름을 요약하고, 라운드를 옆으로 넘겨 고르면 그 라운드의 장비와 킬
 * 순서를 보여 줍니다(사용자 요청, 2026-10-04). 줄마다 승패만 늘어놓으면 스물네 줄을 내려 봐도 무슨 일이 있었는지 알 수 없었습니다.
 * 상대 한 사람 한 사람의 라운드별 피해와 남은 크레드는 응답에 없어 두지 않습니다.
 */
@Composable
internal fun RoundList(uiState: MatchDetailUiState.Success) {
    val half = uiState.match.queue.halfRounds ?: return
    var picked by rememberSaveable { mutableIntStateOf(1) }
    val round = uiState.rounds.firstOrNull { it.number == picked } ?: uiState.rounds.firstOrNull() ?: return

    Column {
        Overview(uiState)
        Spacer(Modifier.height(OvalitSpacing.lg))
        RoundPicker(uiState.rounds, half = half, picked = round.number, onPick = { picked = it })
        Spacer(Modifier.height(OvalitSpacing.md))
        OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
        RoundDetail(round, uiState)
        if (uiState.match.queue.hasEconomy && uiState.buys.isNotEmpty()) {
            Spacer(Modifier.height(OvalitSpacing.sm))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
            BuyTable(uiState)
        }
    }
}

// 첫 킬을 낸 쪽이 라운드를 얼마나 가져갔는지와 공수 성적이다. 두 칸씩 두 줄이다.
@Composable
private fun Overview(uiState: MatchDetailUiState.Success) {
    val overview = uiState.match.roundsOverview()
    val cells = listOf(
        stringResource(Res.string.overview_first_blood_ours) to winOf(overview.ourFirstBlood),
        stringResource(Res.string.overview_first_blood_theirs) to winOf(overview.theirFirstBlood),
        stringResource(Res.string.overview_attack) to record(overview.attack),
        stringResource(Res.string.overview_defense) to record(overview.defense),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
    ) {
        cells.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OvalitText(text = label, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t3, maxLines = 1)
                        OvalitText(text = value, style = OvalitTheme.typography.bodyStrong, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun winOf(record: WinRecord): String = stringResource(Res.string.overview_win_of, record.rounds, record.wins)

@Composable
private fun record(record: WinRecord): String = stringResource(Res.string.overview_record, record.wins, record.losses)

private val PickerCellWidth = 34.dp
private val PickerBarHeight = 22.dp

/**
 * 옆으로 넘기며 라운드를 고르는 줄입니다. 칸마다 위에 우리 팀과 상대 팀의 평균 장비를 작은 막대 둘로, 밑에 라운드 번호를 승패
 * 색을 옅게 깐 네모로 둡니다. 전반과 후반 사이에 세로선을 긋습니다.
 */
@Composable
private fun RoundPicker(rounds: List<RoundSummary>, half: Int, picked: Int, onPick: (Int) -> Unit) {
    val colors = OvalitTheme.colors
    val most = rounds.maxOfOrNull { maxOf(it.economy?.teamLoadout ?: 0, it.economy?.enemyLoadout ?: 0) }?.coerceAtLeast(1) ?: 1
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (picked - 3).coerceAtLeast(0))
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = OvalitSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        itemsIndexed(rounds, key = { _, round -> round.number }) { index, round ->
            Row(verticalAlignment = Alignment.Bottom) {
                // 전반과 후반, 후반과 연장 사이
                if (index > 0 && (round.number - 1) % half == 0 && round.number - 1 <= half * 2) {
                    Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(PickerBarHeight + 28.dp).background(colors.line))
                }
                PickerCell(round, most = most, selected = round.number == picked, onClick = { onPick(round.number) })
            }
        }
    }
}

@Composable
private fun PickerCell(round: RoundSummary, most: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    val label = stringResource(Res.string.round_pick, round.number)
    val name = stringResource(Res.string.round_title, round.number)
    Column(
        modifier = Modifier
            .width(PickerCellWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.fill else Color.Transparent)
            .clickable(onClickLabel = label, role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = name
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val economy = round.economy
        val ours = colors.t1
        val theirs = colors.t4
        Canvas(Modifier.size(width = 14.dp, height = PickerBarHeight)) {
            val bar = 5.dp.toPx()
            fun draw(x: Float, value: Int, color: Color) {
                val height = (size.height * value / most).coerceAtLeast(2.dp.toPx())
                drawRoundRect(color, topLeft = Offset(x, size.height - height), size = Size(bar, height), cornerRadius = CornerRadius(1.5.dp.toPx()))
            }
            if (economy != null) {
                draw(0f, economy.teamLoadout, ours)
                draw(size.width - bar, economy.enemyLoadout, theirs)
            }
        }
        Spacer(Modifier.height(4.dp))
        val result = resultColor(round.won)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (round.played) result.copy(alpha = 0.14f) else colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            OvalitText(
                text = round.number.toString(),
                style = OvalitTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = if (round.played) result else colors.t4,
                maxLines = 1,
            )
        }
    }
}

// 고른 라운드의 머리, 두 팀 장비, 내 기록, 킬 순서다
@Composable
private fun RoundDetail(round: RoundSummary, uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(text = stringResource(Res.string.round_title, round.number), style = OvalitTheme.typography.titleM)
            Spacer(Modifier.width(OvalitSpacing.sm))
            ResultLabel(round.won)
            Spacer(Modifier.width(OvalitSpacing.sm))
            OvalitText(
                text = if (round.played) {
                    listOfNotNull(round.side?.let { sideName(it) }, round.ending?.let { endingName(it) }).joinToString(SEPARATOR)
                } else {
                    stringResource(Res.string.round_not_played)
                },
                modifier = Modifier.weight(1f),
                style = caption,
                color = colors.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val economy = round.economy
        if (economy != null) {
            Spacer(Modifier.height(OvalitSpacing.xs))
            OvalitText(
                text = stringResource(
                    Res.string.round_loadout,
                    round.buyType?.let { buyName(it) }.orEmpty(),
                    economy.teamLoadout.withThousands(),
                    round.enemyBuyType?.let { buyName(it) }.orEmpty(),
                    economy.enemyLoadout.withThousands(),
                ),
                style = caption,
                color = colors.t2,
            )
        }
        if (round.played) {
            MyRoundLine(round)
        }
        Spacer(Modifier.height(OvalitSpacing.lg))
        OvalitText(text = stringResource(Res.string.round_kill_order), style = OvalitTheme.typography.label, color = colors.t3)
        Spacer(Modifier.height(OvalitSpacing.xs))
        if (round.kills.isEmpty()) {
            OvalitText(text = stringResource(Res.string.round_no_kills), style = caption, color = colors.t3)
        } else {
            round.kills.forEach { KillRow(it, uiState) }
        }
    }
}

// "준 피해 187 · 2킬 · 첫 킬"이다. 에이스와 이긴 클러치는 앞에 굵게 적는다.
@Composable
private fun MyRoundLine(round: RoundSummary) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    val clutch = round.highlight?.clutch
    val highlight = when {
        round.highlight?.ace == true -> stringResource(Res.string.round_ace)
        clutch != null && clutch.won -> stringResource(Res.string.round_clutch, clutch.against)
        else -> null
    }
    val mine = listOfNotNull(
        round.myDamage?.let { stringResource(Res.string.round_my_damage, it.withThousands()) },
        round.myKills.takeIf { it > 0 }?.let { stringResource(Res.string.round_kills, it) },
        stringResource(Res.string.round_first_kill).takeIf { round.firstKill },
        stringResource(Res.string.round_first_death).takeIf { round.firstDeath },
    ).joinToString(SEPARATOR)
    Spacer(Modifier.height(OvalitSpacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (highlight != null) {
            OvalitText(text = highlight, style = caption.copy(fontWeight = FontWeight.SemiBold), color = colors.t1, maxLines = 1)
            Spacer(Modifier.width(OvalitSpacing.sm))
        }
        OvalitText(text = mine, style = caption, color = colors.t2, maxLines = 1)
    }
}

// 킬 한 줄이다. "0:23 [얼굴] 나 → [얼굴] kite · 밴달". 우리 팀이 낸 킬은 이름을 밝고 굵게, 상대가 낸 킬은 흐리게 둔다.
@Composable
private fun KillRow(kill: RoundKill, uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    val lines = uiState.match.players.associateBy { it.player }
    val me = uiState.match.me
    val myName = stringResource(Res.string.me)
    fun name(player: PlayerId) = if (player == me) myName else lines[player]?.riotId?.substringBefore('#').orEmpty()
    val weapon = kill.weapon?.let { uiState.catalog.weaponName(it) } ?: stringResource(Res.string.round_kill_ability)
    val time = "${kill.atMillis / 60_000}:${((kill.atMillis / 1_000) % 60).toString().padStart(2, '0')}"
    val killerStyle = OvalitTheme.typography.label.copy(fontWeight = if (kill.byMyTeam) FontWeight.SemiBold else FontWeight.Normal)
    // 글자를 키우면 시각이 얼굴에 붙어서 시각 칸도 같이 넓힌다
    val timeWidth = 34.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.6f) + 4.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$time ${name(kill.killer)} → ${name(kill.victim)} $weapon" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(text = time, modifier = Modifier.width(timeWidth), style = OvalitTheme.typography.metricS, color = colors.t3, maxLines = 1)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            lines[kill.killer]?.let { AgentImage(it.agent, uiState.catalog.agentName(it.agent), Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))) }
            Spacer(Modifier.width(6.dp))
            OvalitText(
                text = name(kill.killer),
                modifier = Modifier.weight(1f, fill = false),
                style = killerStyle,
                color = if (kill.byMyTeam) colors.t1 else colors.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OvalitText(text = "→", modifier = Modifier.padding(horizontal = 6.dp), style = caption, color = colors.t4)
            lines[kill.victim]?.let { AgentImage(it.agent, uiState.catalog.agentName(it.agent), Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))) }
            Spacer(Modifier.width(6.dp))
            OvalitText(
                text = name(kill.victim),
                modifier = Modifier.weight(1f, fill = false),
                style = OvalitTheme.typography.label,
                color = colors.t3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        if (kill.firstBlood) {
            OvalitText(text = stringResource(Res.string.round_first_kill), style = caption.copy(fontWeight = FontWeight.SemiBold), color = colors.t1, maxLines = 1)
            Spacer(Modifier.width(6.dp))
        }
        OvalitText(text = weapon, style = caption, color = colors.t3, maxLines = 1, textAlign = TextAlign.End)
    }
}

/**
 * 라운드 탭 맨 밑의 구매 유형 표입니다. 구매 유형마다 두 팀이 몇 라운드를 사서 몇 번 이겼는지를 둡니다. 우리 팀만 적던 표로는
 * 상대가 언제 무너졌는지 안 보였습니다. 라운드마다 두 팀의 평균 장비를 그리던 그래프는 라운드 고르는 줄의 장비 막대와 겹쳐서
 * 뺐습니다(사용자 요청, 2026-10-04).
 */
@Composable
private fun BuyTable(uiState: MatchDetailUiState.Success) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    val overview = uiState.match.roundsOverview()
    Column {
        // 열 제목은 맨 위에 한 번만 둔다.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.lg, bottom = OvalitSpacing.xs),
        ) {
            OvalitText(text = stringResource(Res.string.buy_title), modifier = Modifier.weight(1f), style = OvalitTheme.typography.label, color = colors.t3)
            OvalitText(text = stringResource(Res.string.my_team), modifier = Modifier.weight(1f), style = caption, color = colors.t3, textAlign = TextAlign.End)
            OvalitText(text = stringResource(Res.string.enemy_team), modifier = Modifier.weight(1f), style = caption, color = colors.t3, textAlign = TextAlign.End)
        }
        uiState.buys.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = OvalitSpacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OvalitText(text = buyName(record.type), modifier = Modifier.weight(1f), style = OvalitTheme.typography.body, maxLines = 1)
                OvalitText(
                    text = stringResource(Res.string.buy_record, record.rounds, record.wins),
                    modifier = Modifier.weight(1f),
                    style = OvalitTheme.typography.bodyStrong,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    autoSize = shrinkToFit(OvalitTheme.typography.bodyStrong.fontSize),
                )
                OvalitText(
                    text = stringResource(Res.string.buy_record, record.enemyRounds, record.enemyWins),
                    modifier = Modifier.weight(1f),
                    style = OvalitTheme.typography.body,
                    color = colors.t2,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    autoSize = shrinkToFit(OvalitTheme.typography.body.fontSize),
                )
            }
        }
        if (overview.upsets > 0) {
            OvalitText(
                text = stringResource(Res.string.economy_upsets, overview.upsets),
                modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.sm),
                style = OvalitTheme.typography.bodyStrong,
            )
        }
        OvalitText(
            text = stringResource(Res.string.economy_note),
            modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, top = OvalitSpacing.md),
            style = caption,
            color = colors.t3,
        )
    }
}
