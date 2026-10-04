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

/**
 * 그 판에서의 자리입니다. MVP는 금색, 팀 MVP는 은색 칩, "3등"은 흐린 칩입니다(사용자 요청, 2026-10-04). op.gg처럼 셋이 한눈에
 * 갈립니다. 승패 칸과 같은 네모라 알약 모양은 쓰지 않습니다.
 */
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
    /** 금색입니다. 경기 MVP입니다. */
    MVP,

    /** 푸른 기가 도는 은색입니다. 팀 MVP입니다. */
    TEAM_MVP,

    /** 보라입니다. 에이스와 클러치처럼 잘한 장면입니다. */
    HIGHLIGHT,

    /** `--fill` 면에 흐린 글자입니다. 등수입니다. */
    QUIET,
}

/** 작은 네모 칩입니다. */
@Composable
fun MatchChip(text: String, tone: MatchChipTone, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val (container, content) = when (tone) {
        MatchChipTone.MVP -> colors.mvpContainer to colors.mvp
        MatchChipTone.TEAM_MVP -> colors.teamMvpContainer to colors.teamMvp
        MatchChipTone.HIGHLIGHT -> colors.highlightContainer to colors.highlight
        MatchChipTone.QUIET -> colors.fill to colors.t2
    }
    // 승패 칸과 같은 높이라 한 줄에 놓아도 칩이 들쭉날쭉하지 않다
    Box(
        modifier = modifier
            .heightIn(min = 22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        OvalitText(
            text = text,
            style = OvalitTheme.typography.caption.copy(fontWeight = if (tone == MatchChipTone.QUIET) FontWeight.Medium else FontWeight.Bold),
            color = content,
            maxLines = 1,
        )
    }
}

/** 그 판에서 내가 낸 에이스와 클러치 칩입니다. 에이스가 먼저입니다. 없으면 빈 목록입니다. */
@Composable
fun highlightChips(highlights: MatchHighlights): List<String> = listOfNotNull(
    when (highlights.aces) {
        0 -> null
        1 -> stringResource(Res.string.highlight_ace)
        else -> stringResource(Res.string.highlight_aces, highlights.aces)
    },
    when (highlights.clutches) {
        0 -> null
        1 -> stringResource(Res.string.highlight_clutch)
        else -> stringResource(Res.string.highlight_clutches, highlights.clutches)
    },
)
