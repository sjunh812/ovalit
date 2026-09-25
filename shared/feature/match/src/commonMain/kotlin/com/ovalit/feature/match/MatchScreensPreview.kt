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

// 스코어와 K/D/A가 오른쪽에 두 줄로 붙어 가장 빡빡한 줄이다
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
