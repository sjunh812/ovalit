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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.myHighlights
import com.ovalit.core.model.myPlacement
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.loading
import com.ovalit.core.ui.resources.match_acs
import com.ovalit.core.ui.resources.match_kda
import com.ovalit.core.ui.resources.match_result_draw
import com.ovalit.core.ui.resources.match_result_loss
import com.ovalit.core.ui.resources.match_result_win
import com.ovalit.core.ui.resources.match_score
import org.jetbrains.compose.resources.stringResource

// 왼쪽 칸을 1로 볼 때 오른쪽 칸의 몫이다. MVP와 클러치 칩, 스코어가 보통 폭 기기에서 한 줄에 들어간다.
private const val RIGHT_COLUMN_WEIGHT = 1.3f

enum class MatchRowStyle {
    /** S2 경기 목록. 맵 썸네일에 요원 얼굴을 겹쳐 둡니다. */
    LIST,

    /** 프로필 최근 경기와 친구 경기 목록. 요원 얼굴만 둡니다. */
    COMPACT,
}

private fun rowPadding(style: MatchRowStyle) = if (style == MatchRowStyle.COMPACT) 11.dp else 16.dp

/**
 * 경기 한 줄입니다. 스코어는 이겼으면 `--pos`, 졌으면 `--neg`로 칠하고, 화면 읽기 프로그램에는 승패를 말로 읽어 줍니다. 숫자는 게임
 * 스코어보드와 같게 응답의 K/D/A와 전투점수를 그대로 씁니다.
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
    // K/D/A 옆은 ADR이 아니라 전투점수다. 등수와 MVP도 이걸로 센다. 목록은 폭이 좁아 한글 라벨 규칙의 예외로
    // "ACS 243"처럼 약어를 쓴다.
    val acs = line?.acs?.let { value ->
        val digits = MetricFormat.INTEGER.format(value)
        if (style == MatchRowStyle.LIST) stringResource(Res.string.match_acs, digits) else digits
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
            .padding(horizontal = OvalitSpacing.gutter, vertical = rowPadding(style)),
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp)) {
            OvalitText(text = mapName, style = OvalitTheme.typography.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // 색만으로는 승패를 못 읽어서 글자로 적는다. 아랫줄 맨 앞이어야 줄마다 같은 자리라 위아래로 훑어 읽힌다.
            val resultStyle = SpanStyle(color = resultColor(match.myTeamWon), fontWeight = FontWeight.SemiBold)
            OvalitText(
                text = buildAnnotatedString {
                    withStyle(resultStyle) { append(result) }
                    append(SEPARATOR + stringResource(match.queue.label) + SEPARATOR + timeLabel)
                },
                style = caption,
                color = colors.t3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(OvalitSpacing.sm))
        // 글자를 키우면 오른쪽 숫자가 폭을 다 가져가 맵 이름이 잘려서 weight로 나눠 갖는다. 오른쪽 칸은 제 몫을 다 채워야
        // 숫자가 오른쪽 여백에 붙는다. 안 채우면 왼쪽 칸 바로 뒤에 붙어 줄 가운데에 뜬다.
        Column(
            modifier = Modifier.weight(RIGHT_COLUMN_WEIGHT),
            horizontalAlignment = Alignment.End,
            // 칩이 스코어보다 높아 왼쪽보다 넓게 띄워야 두 칸의 아랫줄이 같은 높이에 온다
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 7.dp),
        ) {
            val scoreStyle = OvalitTheme.typography.metricS.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            // 결과 색은 스코어에만 칠한다. 줄 전체를 칠하면 목록이 빨강과 초록 띠가 된다.
            // 그 판의 자리와 에이스·클러치 칩은 스코어 앞에 둔다. 자리는 바로 밑 전투점수로 정해져 같이 읽힌다.
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipsThatFit(
                    chips = listOfNotNull<@Composable () -> Unit>(placement?.let { { PlacementLabel(it) } }) +
                        highlightChips(highlights).map { (text, tone) -> { MatchChip(text, tone) } },
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp),
                )
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
                // 맵 이름, 승패, 칩, 스코어가 이미 굵어서 K/D/A는 진한 색에 Medium으로만 올리고 ACS는 보통 굵기로 내린다
                val regular = small.copy(fontWeight = FontWeight.Normal)
                SeparatedRow(
                    items = listOfNotNull(
                        { OvalitText(text = kda, style = small.copy(fontWeight = FontWeight.Medium), color = colors.t1) },
                        acs?.let { { OvalitText(text = it, style = regular, color = colors.t2) } },
                    ),
                    separator = { SeparatorDot(regular, colors.t2) },
                    alignEnd = true,
                )
            }
        }
    }
}

/** 결과 색으로 적은 "승리", "패배", "무승부"입니다. 칩에 담지 않습니다. 칩은 그 판의 자리와 큰 장면에만 씁니다. */
@Composable
fun ResultLabel(won: Boolean?, modifier: Modifier = Modifier) {
    OvalitText(
        text = resultText(won),
        modifier = modifier,
        style = OvalitTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
        color = resultColor(won),
        maxLines = 1,
    )
}

/** "승리", "패배", "무승부"입니다. */
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
 * [chips]를 6dp 간격으로 한 줄에 들어가는 만큼만 둡니다. 하나가 안 들어가면 그 칩부터 뒤는 모두 뺍니다.
 */
@Composable
private fun ChipsThatFit(chips: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    if (chips.isEmpty()) return
    Layout(contents = chips, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gap = 6.dp.roundToPx()
        val placeables = measurables.map { it.first().measure(loose) }
        var x = 0
        val shown = mutableListOf<Pair<Placeable, Int>>()
        placeables.forEachIndexed { index, chip ->
            val at = if (index == 0) 0 else x + gap
            if (at + chip.width > constraints.maxWidth) return@forEachIndexed
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

/**
 * 경기 줄을 받기 전의 모양입니다. [MatchRow]와 같은 높이로 [rows]줄을 잡아 두어 내용이 나타날 때 줄이 움직이지 않습니다.
 */
@Composable
fun MatchRowsSkeleton(rows: Int, style: MatchRowStyle, modifier: Modifier = Modifier) {
    val compact = style == MatchRowStyle.COMPACT
    OvalitSkeleton(description = stringResource(Res.string.loading), modifier = modifier) {
        Column {
            repeat(rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OvalitSpacing.gutter, vertical = rowPadding(style)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (compact) SkeletonBlock(width = 34.dp, height = 34.dp, radius = 9.dp) else SkeletonBlock(width = 54.dp, height = 38.dp, radius = 8.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBlock(width = 64.dp, height = 15.dp)
                        SkeletonBlock(width = 96.dp, height = 12.dp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBlock(width = 52.dp, height = 15.dp)
                        SkeletonBlock(width = 84.dp, height = 12.dp)
                    }
                }
            }
        }
    }
}
