package com.ovalit.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

// 화면은 OvalitTheme.colors와 typography를 쓴다. Material의 primary, surface 같은 색 이름에 맞춰 화면을
// 짜면 테두리 대신 배경 밝기로 영역을 나눈다는 우리 규칙이 흐려진다. 다만 바텀시트, 물결, 글자 선택처럼
// Material 컴포넌트가 스스로 고르는 색과 글꼴이 우리 것과 맞도록 안쪽에 MaterialTheme을 같이 깐다.
@Composable
fun OvalitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamily: FontFamily = ovalitFontFamily(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) OvalitDarkColors else OvalitLightColors
    val typography = remember(fontFamily) { ovalitTypography(fontFamily) }
    val materialColors = remember(colors) { colors.toMaterial() }
    val materialTypography = remember(typography) { typography.toMaterial() }

    MaterialTheme(colorScheme = materialColors, typography = materialTypography) {
        // 물결은 기본으로 글자색(LocalContentColor)을 따르는데, 우리 화면은 Surface를 안 써서 그 값이 기본값인
        // 검정이다. 그대로 두면 다크 테마에서 물결이 안 보여서 우리 회색으로 맞춘다.
        val indication = remember(colors) { ripple(color = colors.t2) }
        CompositionLocalProvider(
            LocalOvalitColors provides colors,
            LocalOvalitTypography provides typography,
            LocalIndication provides indication,
            content = content,
        )
    }
}

object OvalitTheme {
    val colors: OvalitColors
        @Composable @ReadOnlyComposable get() = LocalOvalitColors.current

    val typography: OvalitTypography
        @Composable @ReadOnlyComposable get() = LocalOvalitTypography.current
}

internal val LocalOvalitColors = staticCompositionLocalOf { OvalitDarkColors }

internal val LocalOvalitTypography = staticCompositionLocalOf { ovalitTypography() }
