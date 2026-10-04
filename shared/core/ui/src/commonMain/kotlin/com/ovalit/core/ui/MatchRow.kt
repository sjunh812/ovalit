package com.ovalit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.myHighlights
import com.ovalit.core.model.myPlacement
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.match_adr
import com.ovalit.core.ui.resources.match_kda
import com.ovalit.core.ui.resources.match_result_draw
import com.ovalit.core.ui.resources.match_result_loss
import com.ovalit.core.ui.resources.match_result_short_draw
import com.ovalit.core.ui.resources.match_result_short_loss
import com.ovalit.core.ui.resources.match_result_short_win
import com.ovalit.core.ui.resources.match_result_win
import com.ovalit.core.ui.resources.match_score
import org.jetbrains.compose.resources.stringResource

enum class MatchRowStyle {
    /** S2 경기 목록. 맵 썸네일에 요원 얼굴을 겹쳐 둡니다. */
    LIST,

    /** 프로필 최근 경기와 친구 경기 목록. 요원 얼굴만 둡니다. */
    COMPACT,
}

/**
 * 경기 한 줄입니다. 스코어는 이겼으면 `--pos`, 졌으면 `--neg`로 칠하고, 낭독기에는 승패를 말로 읽어 줍니다. 숫자는 게임
 * 스코어보드와 같게 응답의 K/D/A를 그대로 씁니다.
 *
 * @param onClick `null`이면 누를 수 없는 줄입니다. 친구 경기는 다른 사람 기록이 섞여 있어서 열지 않습니다.
 */
@Composable
fun MatchRow(
    match: Match,
    catalog: ContentCatalog,
    timeLabel: String,
    style: MatchRowStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = OvalitTheme.colors
    val line = match.myScoreline
    val score = match.score
    val mapName = catalog.mapName(match.map)
    val kda = line?.let { stringResource(Res.string.match_kda, it.kills, it.deaths, it.assists) }
    val adr = line?.adr?.let { value ->
        val digits = MetricFormat.INTEGER.format(value)
        if (style == MatchRowStyle.LIST) stringResource(Res.string.match_adr, digits) else digits
    }
    val result = resultText(match.myTeamWon)
    val compact = style == MatchRowStyle.COMPACT
    val placement = remember(match) { match.myPlacement }
    val highlights = remember(match) { match.myHighlights }
    val caption = OvalitTheme.typography.caption
    val small = OvalitTheme.typography.metricS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { stateDescription = result }
            .padding(horizontal = OvalitSpacing.gutter, vertical = if (compact) 11.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (style) {
            MatchRowStyle.LIST -> MapWithAgent(match, catalog)
            MatchRowStyle.COMPACT -> AgentImage(
                agent = match.myAgent,
                name = catalog.agentName(match.myAgent),
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
            // 맵 이름 옆에 큐와 시각을 흐리게 붙인다. 폭이 모자라면 큐와 시각부터 줄인다.
            Row(verticalAlignment = Alignment.Bottom) {
                OvalitText(text = mapName, style = OvalitTheme.typography.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                OvalitText(
                    text = stringResource(match.queue.label) + SEPARATOR + timeLabel,
                    modifier = Modifier.weight(1f, fill = false).padding(bottom = 1.dp),
                    style = caption,
                    color = colors.t3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 밑 줄에 칩을 한 줄로 모은다. 그 판의 자리(MVP, 팀 MVP, 등수), 에이스와 클러치 순이다(사용자 요청, 2026-10-04). op.gg처럼
            // 목록만 봐도 얼마나 보탰는지, 큰 장면이 있었는지 보인다. 폭이 모자라면 뒤 칩부터 뺀다.
            ChipsThatFit(
                listOfNotNull<@Composable () -> Unit>(placement?.let { { PlacementLabel(it) } }) +
                    highlightChips(highlights).map { text -> { MatchChip(text, MatchChipTone.HIGHLIGHT) } },
            )
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        // 글자를 키우면 오른쪽 숫자가 폭을 다 가져가 맵 이름이 잘린다. 폭을 반씩 나눠 갖는다. 자기 몫을 다 채워야
        // 숫자가 오른쪽 여백에 붙는다. 채우지 않으면 왼쪽 칸 바로 뒤에 붙어 줄 가운데에 뜬다.
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
        ) {
            val scoreStyle = OvalitTheme.typography.metricS.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            // 색만으로는 승패를 못 읽어서 스코어 앞에 "승", "패"를 결과 색을 옅게 깐 칸에 적는다(사용자 요청, 2026-10-03). op.gg처럼
            // 줄 전체를 칠하면 목록이 빨강과 초록 띠가 된다.
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResultTile(match.myTeamWon)
                Spacer(Modifier.width(7.dp))
                OvalitText(
                    text = stringResource(Res.string.match_score, score.myTeam, score.enemyTeam),
                    modifier = Modifier.weight(1f, fill = false),
                    style = scoreStyle,
                    color = resultColor(match.myTeamWon),
                    maxLines = 1,
                    autoSize = shrinkToFit(scoreStyle.fontSize),
                )
            }
            if (kda != null) {
                SeparatedRow(
                    items = listOfNotNull(
                        { OvalitText(text = kda, style = small, color = colors.t2) },
                        adr?.let { { OvalitText(text = it, style = small, color = colors.t2) } },
                    ),
                    separator = { SeparatorDot(small, colors.t2) },
                    alignEnd = true,
                )
            }
        }
    }
}

/** 결과 색을 옅게 깐 작은 네모에 "승", "패", "무"를 적습니다. 알약 모양은 쓰지 않습니다(CLAUDE.md 디자인). */
@Composable
fun ResultTile(won: Boolean?, modifier: Modifier = Modifier) {
    val color = resultColor(won)
    Box(
        modifier = modifier
            .size(ResultTileSize)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = RESULT_TILE_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        OvalitText(
            text = resultShortText(won),
            style = OvalitTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}

private val ResultTileSize = 22.dp
private const val RESULT_TILE_ALPHA = 0.14f

/** 경기 줄 스코어 앞의 "승", "패", "무"입니다. */
@Composable
fun resultShortText(won: Boolean?): String = stringResource(
    when (won) {
        true -> Res.string.match_result_short_win
        false -> Res.string.match_result_short_loss
        null -> Res.string.match_result_short_draw
    },
)

/** 결과를 다 쓴 "승리", "패배", "무승부"입니다. S3 머리와 낭독기에 씁니다. */
@Composable
fun resultText(won: Boolean?): String = stringResource(
    when (won) {
        true -> Res.string.match_result_win
        false -> Res.string.match_result_loss
        null -> Res.string.match_result_draw
    },
)

/** 이기면 `--pos`, 지면 `--neg`, 비기면 `--t2`입니다. 경기 결과라 변화량 색 규칙과 같은 쌍을 씁니다. */
@Composable
fun resultColor(won: Boolean?): Color = when (won) {
    true -> OvalitTheme.colors.pos
    false -> OvalitTheme.colors.neg
    null -> OvalitTheme.colors.t2
}

// 목업처럼 맵 썸네일 왼쪽 아래에 요원 얼굴을 걸쳐 둔다. 바탕색 테두리로 둘을 떼어 놓는다.
@Composable
private fun MapWithAgent(match: Match, catalog: ContentCatalog) {
    Box(modifier = Modifier.size(width = 54.dp, height = 38.dp)) {
        MapImage(match.map, MapImageStyle.THUMBNAIL, Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)))
        AgentImage(
            agent = match.myAgent,
            name = catalog.agentName(match.myAgent),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-5).dp, y = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, OvalitTheme.colors.bg, CircleShape),
        )
    }
}

/**
 * [chips]를 6dp 간격으로 한 줄에 들어가는 만큼만 둡니다. 맨 앞 칩은 늘 두고, 하나가 안 들어가면 그 뒤 칩은 모두 뺍니다.
 */
@Composable
private fun ChipsThatFit(chips: List<@Composable () -> Unit>) {
    Layout(contents = chips) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gap = 6.dp.roundToPx()
        val placeables = measurables.map { it.first().measure(loose) }
        var x = 0
        val shown = mutableListOf<Pair<Placeable, Int>>()
        placeables.forEachIndexed { index, chip ->
            val at = if (index == 0) 0 else x + gap
            if (index > 0 && at + chip.width > constraints.maxWidth) return@forEachIndexed
            if (shown.size < index) return@forEachIndexed
            shown += chip to at
            x = at + chip.width
        }
        val height = shown.maxOfOrNull { it.first.height } ?: 0
        layout(x.coerceAtMost(constraints.maxWidth), height) {
            shown.forEach { (chip, at) -> chip.place(at, (height - chip.height) / 2) }
        }
    }
}
