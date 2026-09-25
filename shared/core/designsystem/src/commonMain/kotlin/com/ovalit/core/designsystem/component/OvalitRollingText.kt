package com.ovalit.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val ENTER_MILLIS = 220
private const val EXIT_MILLIS = 150

/**
 * 숫자가 바뀌면 짧게 굴러가듯 바꿉니다. 큐나 기간을 바꿨을 때 어느 숫자가 달라졌는지 눈에 들어오게 합니다. 커지면
 * 아래에서 올라오고 작아지면 위에서 내려옵니다. 처음 그릴 때는 움직이지 않고, 기기에서 애니메이션을 끄면 바로
 * 바뀝니다.
 */
@Composable
fun OvalitRollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OvalitTheme.typography.body,
    color: Color = OvalitTheme.colors.t1,
    maxLines: Int = 1,
    autoSize: TextAutoSize? = null,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            val rising = (numberIn(targetState) ?: 0.0) >= (numberIn(initialState) ?: 0.0)
            val enter = slideInVertically(tween(ENTER_MILLIS)) { height -> if (rising) height / 3 else -height / 3 } +
                fadeIn(tween(ENTER_MILLIS))
            val exit = slideOutVertically(tween(EXIT_MILLIS)) { height -> if (rising) -height / 3 else height / 3 } +
                fadeOut(tween(EXIT_MILLIS))
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.CenterStart,
        label = "rolling",
    ) { value ->
        OvalitText(text = value, style = style, color = color, maxLines = maxLines, autoSize = autoSize)
    }
}

// "+0.13", "−5", "30%"처럼 기호와 단위가 붙은 글자에서 숫자만 읽는다. 변화량의 빼기는 유니코드 마이너스다.
private fun numberIn(text: String): Double? =
    text.replace('−', '-').filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
