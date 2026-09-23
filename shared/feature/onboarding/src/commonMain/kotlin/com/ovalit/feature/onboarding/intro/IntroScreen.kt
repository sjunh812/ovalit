package com.ovalit.feature.onboarding.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ovalit.core.designsystem.component.OvalitDisclaimer
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.onboarding.resources.Res
import com.ovalit.feature.onboarding.resources.intro_headline
import com.ovalit.feature.onboarding.resources.intro_hook
import com.ovalit.feature.onboarding.resources.intro_start
import com.ovalit.feature.onboarding.resources.intro_subtitle
import org.jetbrains.compose.resources.stringResource

private val LogoWidth = 104.dp

// 괄호 안 글자 크기. em이라 바깥 글자 크기에 따라간다. titleL을 키워도 비율이 유지된다.
private val ParenthesisScale = 0.7.em

/**
 * 괄호와 그 안의 글자를 색과 크기로 한 단계 눌러 줍니다.
 *
 * "오발있? (오늘 발로 있어?)"에서 앱 이름을 먼저 읽히게 하고 뜻풀이는 뒤로 물립니다.
 * 전부 같은 색과 크기면 어디까지가 이름인지 구분이 안 됩니다.
 *
 * 문구는 리소스에 그대로 두고 보여주는 방법만 여기서 정합니다. 번역할 때도 괄호만 그대로
 * 쓰면 됩니다.
 */
@Composable
private fun dimParentheses(text: String): AnnotatedString {
    val dimmed = SpanStyle(
        color = OvalitTheme.colors.t3,
        fontSize = ParenthesisScale,
    )

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val open = text.indexOf('(', cursor)
            val close = if (open == -1) -1 else text.indexOf(')', open)
            if (close == -1) {
                append(text.substring(cursor))
                return@buildAnnotatedString
            }
            append(text.substring(cursor, open))
            withStyle(dimmed) { append(text.substring(open, close + 1)) }
            cursor = close + 1
        }
    }
}

/**
 * S0-1 인트로입니다. RSO 인증 전이라 여기서는 아무 데이터도 요청하지 않습니다.
 *
 * @param onStart 다음은 S0-2 연동 동의입니다.
 */
@Composable
fun IntroScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.bg)
            .safeDrawingPadding()
            .padding(horizontal = OvalitSpacing.xl),
    ) {
        // 로고와 문구 덩어리를 버튼 위 공간의 가운데에 둔다. 위로 붙이면 화면 한복판이
        // 통째로 비어서 안 채운 것처럼 보인다.
        Spacer(Modifier.weight(1f))

        OvalitLogo(modifier = Modifier.width(LogoWidth))

        Spacer(Modifier.height(OvalitSpacing.xl))

        // 로고가 초성 ㅇㅂㅇ라 처음 보는 사람은 읽는 법을 모른다. 바로 밑에서 소리 내어
        // 읽어주는 자리다. 헤드라인과 같은 크기로 둬서 두 줄이 한 덩어리로 읽히게 한다.
        OvalitText(
            text = dimParentheses(stringResource(Res.string.intro_hook)),
            style = OvalitTheme.typography.titleL,
        )

        OvalitText(
            text = stringResource(Res.string.intro_headline),
            style = OvalitTheme.typography.titleL,
        )

        Spacer(Modifier.height(OvalitSpacing.md))

        OvalitText(
            text = stringResource(Res.string.intro_subtitle),
            style = OvalitTheme.typography.body,
            color = OvalitTheme.colors.t2,
        )

        Spacer(Modifier.weight(1f))

        OvalitPrimaryButton(
            text = stringResource(Res.string.intro_start),
            onClick = onStart,
        )

        Spacer(Modifier.height(OvalitSpacing.xl))

        OvalitDisclaimer(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(OvalitSpacing.md))
    }
}
