package com.ovalit.feature.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.MatchId
import com.ovalit.core.ui.MatchRow
import com.ovalit.core.ui.MatchRowStyle
import com.ovalit.core.ui.recentMatchTimeLabel
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.profile_recent
import com.ovalit.feature.profile.resources.profile_recent_all
import org.jetbrains.compose.resources.stringResource

// 티어 카드, 통계, 맞힌 부위, 요원, 무기는 S5 친구 프로필과 같이 쓰려고 core/ui의 ProfileSections.kt에 있다.

/** S5와 같은 짧은 줄 세 개입니다. 내 경기라 누르면 S3가 열리고, "전체 보기"는 경기 탭으로 갑니다. */
@Composable
internal fun RecentMatchesSection(
    uiState: ProfileUiState.Success,
    onOpenMatch: (MatchId) -> Unit,
    onOpenMatches: () -> Unit,
) {
    if (uiState.recentMatches.isEmpty()) return
    OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = stringResource(Res.string.profile_recent),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        if (uiState.hasMoreMatches) {
            OvalitTextButton(text = stringResource(Res.string.profile_recent_all), onClick = onOpenMatches)
        }
    }
    uiState.recentMatches.forEachIndexed { index, match ->
        if (index > 0) OvalitDivider(Modifier.padding(start = 67.dp), color = OvalitTheme.colors.lineWeak)
        MatchRow(
            match = match,
            catalog = uiState.catalog,
            timeLabel = recentMatchTimeLabel(match.startedAt, uiState.now, uiState.timeZone),
            style = MatchRowStyle.COMPACT,
            onClick = { onOpenMatch(match.id) },
        )
    }
}
