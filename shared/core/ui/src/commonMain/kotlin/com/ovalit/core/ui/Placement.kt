package com.ovalit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchAward
import com.ovalit.core.model.MatchPlacement
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.placement_mvp
import com.ovalit.core.ui.resources.placement_rank
import com.ovalit.core.ui.resources.placement_rank_of
import com.ovalit.core.ui.resources.placement_team_mvp
import org.jetbrains.compose.resources.stringResource

/** "MVP", "팀 MVP"입니다. MVP가 아니면 `null`입니다. */
@Composable
fun awardText(award: MatchAward?): String? = when (award) {
    MatchAward.MVP -> stringResource(Res.string.placement_mvp)
    MatchAward.TEAM_MVP -> stringResource(Res.string.placement_team_mvp)
    null -> null
}

/** "3등", 또는 [withPlayers]면 "10명 중 3등"입니다. */
@Composable
fun rankText(placement: MatchPlacement, withPlayers: Boolean = false): String =
    if (withPlayers) {
        stringResource(Res.string.placement_rank_of, placement.rank, placement.players)
    } else {
        stringResource(Res.string.placement_rank, placement.rank)
    }

/**
 * 그 판에서의 자리입니다. MVP와 팀 MVP는 `--fill`을 깐 작은 네모에 굵게 적고, 나머지는 "3등"을 흐리게 적습니다. 색이 아니라
 * 밝기와 굵기로 가릅니다(CLAUDE.md 디자인). 승패 칸과 같은 네모라 알약 모양은 쓰지 않습니다.
 */
@Composable
fun PlacementLabel(placement: MatchPlacement, modifier: Modifier = Modifier) {
    val award = awardText(placement.award)
    if (award != null) {
        AwardTile(award, modifier)
    } else {
        OvalitText(
            text = rankText(placement),
            modifier = modifier,
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
            maxLines = 1,
        )
    }
}

/** "MVP", "팀 MVP"를 적은 작은 네모입니다. 스코어보드 이름 옆에도 씁니다. */
@Composable
fun AwardTile(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(OvalitTheme.colors.fill)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        OvalitText(
            text = text,
            style = OvalitTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = OvalitTheme.colors.t1,
            maxLines = 1,
        )
    }
}
