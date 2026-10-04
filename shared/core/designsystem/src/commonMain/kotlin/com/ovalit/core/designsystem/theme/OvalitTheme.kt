package com.ovalit.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import com.ovalit.core.designsystem.component.OvalitPressIndication
import com.ovalit.core.designsystem.resources.Res
import com.ovalit.core.designsystem.resources.text_locale
import org.jetbrains.compose.resources.stringResource

// 화면은 OvalitTheme.colors와 typography만 쓴다. 안쪽의 MaterialTheme은 바텀시트처럼 Material 컴포넌트가 스스로
// 고르는 색과 글꼴을 우리 것에 맞추려고 깐다(MaterialBridge.kt).
@Composable
fun OvalitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamily: FontFamily = ovalitFontFamily(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) OvalitDarkColors else OvalitLightColors
    val textLocale = stringResource(Res.string.text_locale)
    val typography = remember(fontFamily, textLocale) { ovalitTypography(fontFamily, LocaleList(textLocale)) }
    val materialColors = remember(colors) { colors.toMaterial() }
    val materialTypography = remember(typography) { typography.toMaterial() }

    MaterialTheme(colorScheme = materialColors, typography = materialTypography) {
        // MaterialTheme이 까는 물결을 우리 누름 효과로 바꾼다. clickable만 단 줄과 아바타도 같은 효과를 받는다.
        val indication = remember(colors) { OvalitPressIndication(colors.t2) }
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
