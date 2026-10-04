package com.ovalit.feature.match

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MatchesDarkPreview() {
    OvalitThemePreview(darkTheme = true) { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MatchesLightPreview() {
    OvalitThemePreview(darkTheme = false) { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 400)
@Composable
private fun MatchesEmptyPreview() {
    OvalitThemePreview { MatchesScreen(MatchPreviewData.empty, {}, {}, {}) }
}

// 좁은 폭과 큰 글씨에서 경기 줄을 본다. 오른쪽에 칩, 스코어, K/D/A가 몰려 가장 빡빡하다.
@Preview(widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Composable
private fun MatchesSmallLargeFontPreview() {
    OvalitThemePreview { MatchesScreen(MatchPreviewData.matches, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun MatchDetailDarkPreview() {
    OvalitThemePreview(darkTheme = true) { MatchDetailScreen(MatchPreviewData.detail, {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun MatchDetailLightPreview() {
    OvalitThemePreview(darkTheme = false) { MatchDetailScreen(MatchPreviewData.detail, {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 320, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun MatchDetailSmallLargeFontPreview() {
    OvalitThemePreview { MatchDetailScreen(MatchPreviewData.detail, {}, {}, {}, {}, {}) }
}
