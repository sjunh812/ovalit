package com.ovalit.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 프리뷰에서 테마와 배경을 한 번에 깔아 줍니다. 배경을 안 깔면 다크 토큰이 흰 캔버스 위에
 * 떠서 실제와 다르게 보입니다.
 */
@Composable
fun OvalitThemePreview(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    OvalitTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.background(OvalitTheme.colors.bg)) {
            content()
        }
    }
}
