package com.ovalit.core.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.match_adr
import com.ovalit.core.ui.resources.match_kda
import com.ovalit.core.ui.resources.match_score
import org.jetbrains.compose.resources.stringResource

private const val SEPARATOR = " · "

enum class MatchRowStyle {
    /** S2 경기 목록. 맵 썸네일에 요원 얼굴을 겹쳐 둡니다. */
    LIST,

    /** S5 친구의 최근 경기. 요원 얼굴만 둡니다. */
    COMPACT,
}

/**
 * 경기 한 줄입니다. 스코어는 이겼으면 `--pos`, 졌으면 `--neg`로 칠합니다. 숫자는 게임 스코어보드와 같게
 * 응답의 K/D/A를 그대로 씁니다.
 *
 * @param onClick 없으면 누를 수 없는 줄입니다. 친구 경기는 다른 사람 기록이 섞여 있어서 열지 않습니다.
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
    val compact = style == MatchRowStyle.COMPACT
    val caption = OvalitTheme.typography.caption
    val small = OvalitTheme.typography.metricS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)) {
            OvalitText(text = mapName, style = OvalitTheme.typography.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // 글자를 키워 한 줄에 안 들어가면 시각이 다음 줄로 내려간다. 점은 줄 맨 앞에 두지 않는다.
            SeparatedRow(
                items = listOf(
                    { OvalitText(text = stringResource(match.queue.label), style = caption, color = colors.t3) },
                    { OvalitText(text = timeLabel, style = caption, color = colors.t3) },
                ),
                separator = { OvalitText(text = SEPARATOR, style = caption, color = colors.t3) },
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
            OvalitText(
                text = stringResource(Res.string.match_score, score.myTeam, score.enemyTeam),
                style = scoreStyle,
                color = resultColor(match.myTeamWon),
                maxLines = 1,
                autoSize = shrinkToFit(scoreStyle.fontSize),
            )
            if (kda != null) {
                SeparatedRow(
                    items = listOfNotNull(
                        { OvalitText(text = kda, style = small, color = colors.t2) },
                        adr?.let { { OvalitText(text = it, style = small, color = colors.t2) } },
                    ),
                    separator = { OvalitText(text = SEPARATOR, style = small, color = colors.t2) },
                    alignEnd = true,
                )
            }
        }
    }
}

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
