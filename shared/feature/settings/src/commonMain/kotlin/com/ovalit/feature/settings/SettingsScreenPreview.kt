package com.ovalit.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview
import com.ovalit.core.model.Account
import com.ovalit.core.model.UserPreferences
import kotlinx.datetime.LocalDate

internal object SettingsPreviewData {
    val linked = SettingsUiState.Success(
        account = Account(riotId = "오발러#KR1", linkedOn = LocalDate(2026, 9, 19)),
        preferences = UserPreferences.Default,
        storedMatches = 127,
    )

    val allOff = linked.copy(
        preferences = UserPreferences.Default.copy(
            statsPublic = false,
            notifyAnalysisDone = false,
            notifyWeeklyReport = false,
        ),
    )
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun SettingsDarkPreview() {
    SettingsPreview(SettingsPreviewData.linked, darkTheme = true)
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun SettingsLightPreview() {
    SettingsPreview(SettingsPreviewData.linked, darkTheme = false)
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun SettingsAllOffPreview() {
    SettingsPreview(SettingsPreviewData.allOff)
}

// 주간 리포트 줄은 제목, 시각, 스위치가 한 줄이라 가장 빡빡하다
@Preview(widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Composable
private fun SettingsSmallLargeFontPreview() {
    SettingsPreview(SettingsPreviewData.linked)
}

@Composable
private fun SettingsPreview(uiState: SettingsUiState, darkTheme: Boolean = true) {
    OvalitThemePreview(darkTheme = darkTheme) {
        SettingsScreen(uiState = uiState, appVersion = "1.0.0", actions = SettingsActions())
    }
}
