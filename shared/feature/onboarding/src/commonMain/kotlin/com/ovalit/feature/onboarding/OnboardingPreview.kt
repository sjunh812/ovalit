package com.ovalit.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview
import com.ovalit.core.model.Focus
import com.ovalit.core.model.ImportProgress
import com.ovalit.feature.onboarding.consent.ConsentScreen
import com.ovalit.feature.onboarding.importing.ImportScreen
import com.ovalit.feature.onboarding.importing.ImportUiState

internal object OnboardingPreviewData {
    // 목업 S0-4처럼 50경기 중 37경기를 받은 때다
    val loading = ImportUiState.Success(
        progress = ImportProgress(total = 50, results = List(37) { it % 3 != 1 }),
        focus = Focus.NONE,
    )

    val done = loading.copy(progress = ImportProgress(total = 50, results = List(50) { it % 3 != 1 }), focus = Focus.ROUND_PLAY)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ConsentDarkPreview() {
    OvalitThemePreview(darkTheme = true) { ConsentScreen(onBack = {}, onContinue = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ConsentLightPreview() {
    OvalitThemePreview(darkTheme = false) { ConsentScreen(onBack = {}, onContinue = {}) }
}

@Preview(widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Composable
private fun ConsentSmallLargeFontPreview() {
    OvalitThemePreview { ConsentScreen(onBack = {}, onContinue = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ImportDarkPreview() {
    OvalitThemePreview(darkTheme = true) { ImportScreen(OnboardingPreviewData.loading, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ImportLightDonePreview() {
    OvalitThemePreview(darkTheme = false) { ImportScreen(OnboardingPreviewData.done, {}, {}) }
}

// 선택지 네 개와 아래 진행 막대가 한 화면에 있어 작은 화면에서 서로 밀린다
@Preview(widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Composable
private fun ImportSmallLargeFontPreview() {
    OvalitThemePreview { ImportScreen(OnboardingPreviewData.loading, {}, {}) }
}
