package com.ovalit.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ripple
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

    // MaterialTheme이 없으면 clickable이 물결 대신 회색 사각형을 깐다. 디자인 시스템 컴포넌트는 물결을
    // 직접 달지만, 기능 화면의 줄과 아바타는 clickable만 쓰므로 여기서 기본값을 물결로 바꾼다.
    val indication = remember(colors) { ripple(color = colors.t2) }
    CompositionLocalProvider(
        LocalOvalitColors provides colors,
        LocalOvalitTypography provides typography,
        LocalIndication provides indication,
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
