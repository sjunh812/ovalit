package com.ovalit.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * @param autoSize 폭이 정해진 칸에 씁니다. 글자를 키운 사용자에게 `1.29`가 `1.`로 잘리지 않고
 * 글자가 작아집니다. [maxLines]를 1로 같이 줘야 줄을 바꾸지 않고 줄어듭니다.
 */
@Composable
fun OvalitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OvalitTheme.typography.body,
    color: Color = OvalitTheme.colors.t1,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    autoSize: TextAutoSize? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = overflow,
        autoSize = autoSize,
    )
}

/**
 * 한 문장 안에서 일부 글자만 다르게 보여야 할 때 씁니다. [color]는 스타일을 따로 주지 않은
 * 구간의 색입니다.
 */
@Composable
fun OvalitText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = OvalitTheme.typography.body,
    color: Color = OvalitTheme.colors.t1,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = overflow,
    )
}
