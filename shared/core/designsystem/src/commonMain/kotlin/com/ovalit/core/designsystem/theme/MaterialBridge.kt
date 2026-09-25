package com.ovalit.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 우리 토큰을 Material 색 이름에 옮겨 담는다. 화면 코드는 계속 OvalitTheme.colors를 쓰고, 이 값은
// 바텀시트나 글자 선택처럼 Material 컴포넌트가 스스로 색을 고를 때만 쓰인다. 면을 나누는 규칙은 토큰에
// 있으니 여기서는 가장 가까운 자리에 넣기만 한다.
internal fun OvalitColors.toMaterial(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = fill,
        onPrimaryContainer = t1,
        inversePrimary = accentInk,
        secondary = t2,
        onSecondary = bg,
        secondaryContainer = fill,
        onSecondaryContainer = t1,
        tertiary = t2,
        onTertiary = bg,
        background = bg,
        onBackground = t1,
        surface = bg,
        onSurface = t1,
        surfaceVariant = raised,
        onSurfaceVariant = t2,
        surfaceTint = Color.Transparent,
        surfaceBright = raised,
        surfaceDim = bg,
        surfaceContainerLowest = bg,
        surfaceContainerLow = raised,
        surfaceContainer = raised,
        surfaceContainerHigh = fill,
        surfaceContainerHighest = bar,
        inverseSurface = t1,
        inverseOnSurface = bg,
        outline = line,
        outlineVariant = lineWeak,
        error = neg,
        onError = bg,
        errorContainer = fill,
        onErrorContainer = neg,
        scrim = Color.Black,
    )
}

internal fun OvalitTypography.toMaterial(): Typography = Typography(
    displayLarge = metricXl,
    displayMedium = metricL,
    displaySmall = metricM,
    headlineLarge = titleL,
    headlineMedium = titleL,
    headlineSmall = titleM,
    titleLarge = titleL,
    titleMedium = titleM,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = label,
    labelSmall = caption,
)
