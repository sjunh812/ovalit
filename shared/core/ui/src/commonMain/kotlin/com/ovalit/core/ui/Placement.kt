package com.ovalit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchAward
import com.ovalit.core.model.MatchHighlights
import com.ovalit.core.model.MatchPlacement
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.highlight_ace
import com.ovalit.core.ui.resources.highlight_aces
import com.ovalit.core.ui.resources.highlight_clutch
import com.ovalit.core.ui.resources.highlight_clutches
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

/** 그 판에서의 자리입니다. MVP와 팀 MVP는 색 칩, 나머지는 "3등"을 적은 흐린 칩입니다. */
@Composable
fun PlacementLabel(placement: MatchPlacement, modifier: Modifier = Modifier) {
    val award = awardText(placement.award)
    when (placement.award) {
        MatchAward.MVP -> MatchChip(award.orEmpty(), MatchChipTone.MVP, modifier)
        MatchAward.TEAM_MVP -> MatchChip(award.orEmpty(), MatchChipTone.TEAM_MVP, modifier)
        null -> MatchChip(rankText(placement), MatchChipTone.QUIET, modifier)
    }
}

/** 경기 줄과 스코어보드의 작은 칩 모양입니다. 등수 말고는 모두 옅은 면에 짙은 같은 계열 글자를 올린 톤온톤입니다. */
enum class MatchChipTone {
    MVP,
    TEAM_MVP,
    ACE,

    /** 이긴 클러치입니다. */
    CLUTCH,

    /** 등수입니다. `--fill` 면에 흐린 글자입니다. */
    QUIET,
}

/** 알약 모양 대신 모서리만 살짝 둥글린 네모 칩입니다. */
@Composable
fun MatchChip(text: String, tone: MatchChipTone, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val (container, content) = when (tone) {
        MatchChipTone.MVP -> colors.mvpContainer to colors.mvp
        MatchChipTone.TEAM_MVP -> colors.teamMvpContainer to colors.teamMvp
        MatchChipTone.ACE -> colors.aceContainer to colors.ace
        MatchChipTone.CLUTCH -> colors.clutchContainer to colors.clutch
        MatchChipTone.QUIET -> colors.fill to colors.t2
    }
    // 이름 옆에 붙는 작은 표시라 글자보다 한 단계 작게 둔다. 더 키우면 이름보다 칩이 먼저 보인다.
    Box(
        modifier = modifier
            .heightIn(min = 19.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(container)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        OvalitText(
            text = text,
            style = OvalitTheme.typography.caption.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (tone == MatchChipTone.QUIET) FontWeight.Medium else FontWeight.SemiBold,
            ),
            color = content,
            maxLines = 1,
        )
    }
}

/** 그 판에서 내가 낸 에이스와 이긴 클러치의 글자와 칩 색입니다. 에이스가 먼저입니다. 없으면 빈 목록입니다. */
@Composable
fun highlightChips(highlights: MatchHighlights): List<Pair<String, MatchChipTone>> = listOfNotNull(
    when (highlights.aces) {
        0 -> null
        1 -> stringResource(Res.string.highlight_ace)
        else -> stringResource(Res.string.highlight_aces, highlights.aces)
    }?.let { it to MatchChipTone.ACE },
    when (highlights.clutches) {
        0 -> null
        1 -> stringResource(Res.string.highlight_clutch)
        else -> stringResource(Res.string.highlight_clutches, highlights.clutches)
    }?.let { it to MatchChipTone.CLUTCH },
)
