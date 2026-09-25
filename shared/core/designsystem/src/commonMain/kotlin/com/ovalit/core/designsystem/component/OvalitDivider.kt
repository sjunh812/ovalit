package com.ovalit.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme

@Composable
fun OvalitDivider(
    modifier: Modifier = Modifier,
    color: Color = OvalitTheme.colors.line,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
