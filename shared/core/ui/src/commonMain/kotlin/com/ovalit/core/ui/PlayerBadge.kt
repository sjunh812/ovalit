package com.ovalit.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Match
import com.ovalit.core.model.latestTier

/**
 * 홈 오른쪽 위와 프로필 머리에 띄우는 사람 정보입니다.
 *
 * @property tierName 카탈로그에 그 번호가 없거나 경쟁전을 안 뛰었으면 `null`이고, 그러면 티어 줄을 두지 않습니다.
 */
data class PlayerBadge(
    val riotId: String,
    val tier: Int?,
    val tierName: String?,
)

fun playerBadge(riotId: String, matches: List<Match>, catalog: ContentCatalog): PlayerBadge {
    val tier = matches.latestTier()
    return PlayerBadge(
        riotId = riotId,
        tier = tier,
        tierName = tier?.let { catalog.tiers[it] },
    )
}

/** 엠블럼과 "플래티넘 2"를 나란히 둡니다. 이름을 모르면 아무것도 그리지 않습니다. */
@Composable
fun TierLabel(
    badge: PlayerBadge,
    style: TextStyle,
    color: Color,
    emblemSize: Dp,
    modifier: Modifier = Modifier,
) {
    val tier = badge.tier ?: return
    val name = badge.tierName ?: return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TierEmblem(tier, Modifier.size(emblemSize))
        OvalitText(text = name, style = style, color = color, maxLines = 1)
    }
}
