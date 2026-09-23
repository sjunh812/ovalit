package com.ovalit.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ovalit.core.designsystem.resources.Res
import com.ovalit.core.designsystem.resources.riot_disclaimer
import com.ovalit.core.designsystem.theme.OvalitTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 프로덕션 키 승인 조건이라 화면 하단에 반드시 있어야 합니다. 지우거나 접어 두지 않습니다.
 */
@Composable
fun OvalitDisclaimer(modifier: Modifier = Modifier) {
    OvalitText(
        text = stringResource(Res.string.riot_disclaimer),
        modifier = modifier,
        style = OvalitTheme.typography.caption,
        // 읽혀야 하는 문구라 t4까지 내리지 않는다. 라이트에서 대비가 2:1로 떨어진다.
        color = OvalitTheme.colors.t3,
        textAlign = TextAlign.Center,
    )
}
