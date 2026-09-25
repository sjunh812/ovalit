package com.ovalit.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfileDarkPreview() {
    OvalitThemePreview(darkTheme = true) { ProfileScreen(ProfilePreviewData.success, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfileLightPreview() {
    OvalitThemePreview(darkTheme = false) { ProfileScreen(ProfilePreviewData.success, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun AgentsDarkPreview() {
    OvalitThemePreview(darkTheme = true) { AgentsScreen(ProfilePreviewData.success, onBack = {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun AgentsLightPreview() {
    OvalitThemePreview(darkTheme = false) { AgentsScreen(ProfilePreviewData.success, onBack = {}) }
}

// 타격대가 주 역할이면 오른쪽 두 열이 퍼블 쪽 지표로 바뀐다
@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun AgentsDuelistPreview() {
    OvalitThemePreview {
        AgentsScreen(ProfilePreviewData.success.copy(agents = ProfilePreviewData.duelistAgents), onBack = {})
    }
}

// 요원별 표는 이름과 세 열이 한 줄이라 가장 빡빡하다
@Preview(widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun AgentsSmallLargeFontPreview() {
    OvalitThemePreview { AgentsScreen(ProfilePreviewData.success, onBack = {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun WeaponsDarkPreview() {
    OvalitThemePreview(darkTheme = true) { WeaponsScreen(ProfilePreviewData.success, onBack = {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun WeaponsLightPreview() {
    OvalitThemePreview(darkTheme = false) { WeaponsScreen(ProfilePreviewData.success, onBack = {}) }
}

@Preview(widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun WeaponsSmallLargeFontPreview() {
    OvalitThemePreview { WeaponsScreen(ProfilePreviewData.success, onBack = {}) }
}
