package com.ovalit.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

// MaterialTheme은 쓰지 않는다. Material은 primary, surface 같은 자기 색 이름에 값을
// 채워 넣으라고 요구하는데, 거기 맞추다 보면 테두리 대신 배경 밝기로 영역을 나눈다는
// 우리 규칙을 못 지킨다.
@Composable
fun OvalitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamily: FontFamily = ovalitFontFamily(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) OvalitDarkColors else OvalitLightColors
    val typography = remember(fontFamily) { ovalitTypography(fontFamily) }

    CompositionLocalProvider(
        LocalOvalitColors provides colors,
        LocalOvalitTypography provides typography,
        content = content,
    )
}

object OvalitTheme {
    val colors: OvalitColors
        @Composable @ReadOnlyComposable get() = LocalOvalitColors.current

    val typography: OvalitTypography
        @Composable @ReadOnlyComposable get() = LocalOvalitTypography.current
}

internal val LocalOvalitColors = staticCompositionLocalOf { OvalitDarkColors }

internal val LocalOvalitTypography = staticCompositionLocalOf { ovalitTypography() }
