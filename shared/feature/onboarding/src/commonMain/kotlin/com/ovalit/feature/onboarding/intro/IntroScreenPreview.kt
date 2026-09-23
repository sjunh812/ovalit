package com.ovalit.feature.onboarding.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun IntroScreenDarkPreview() {
    OvalitThemePreview(darkTheme = true) {
        IntroScreen(onStart = {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun IntroScreenLightPreview() {
    OvalitThemePreview(darkTheme = false) {
        IntroScreen(onStart = {})
    }
}

// 작은 기기에서 헤드라인과 버튼이 서로 밀지 않는지 본다.
@Preview(widthDp = 320, heightDp = 568)
@Composable
private fun IntroScreenSmallPreview() {
    OvalitThemePreview(darkTheme = true) {
        IntroScreen(onStart = {})
    }
}

// 글자 크기를 키운 접근성 설정에서 줄이 넘치지 않는지 본다.
@Preview(widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun IntroScreenLargeFontPreview() {
    OvalitThemePreview(darkTheme = true) {
        IntroScreen(onStart = {})
    }
}
