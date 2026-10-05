package com.ovalit.feature.onboarding.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ovalit.core.designsystem.component.OvalitDisclaimer
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.currentMaxHeight
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.onboarding.resources.Res
import com.ovalit.feature.onboarding.resources.intro_headline
import com.ovalit.feature.onboarding.resources.intro_hook
import com.ovalit.feature.onboarding.resources.intro_start
import com.ovalit.feature.onboarding.resources.intro_subtitle_what
import com.ovalit.feature.onboarding.resources.intro_subtitle_when
import org.jetbrains.compose.resources.stringResource

private val LogoWidth = 88.dp

// 괄호 안 글자 크기. em이라 바깥 글자 크기를 따라간다.
private val ParenthesisScale = 0.7.em

/**
 * 괄호와 그 안의 글자를 한 단계 흐리고 작고 가늘게 그립니다. "오발있? (오늘 발로란트 할 사람 있어?)"에서 어디까지가 앱
 * 이름인지 먼저 읽히게 합니다. 바깥 글자의 SemiBold를 그대로 두면 작아져도 굵어서 탁해 보여 보통 굵기로 내립니다. 문구는
 * 리소스에 그대로 두니 번역할 때도 괄호만 지키면 됩니다.
 */
@Composable
private fun dimParentheses(text: String): AnnotatedString {
    val dimmed = SpanStyle(
        color = OvalitTheme.colors.t3,
        fontSize = ParenthesisScale,
        fontWeight = FontWeight.Normal,
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
    // 고지는 빼면 키가 회수되는 문구라 어떤 기기에서도 끝까지 보여야 한다. 글자를 키운 작은 기기에서는 스크롤되게 두고,
    // 화면이 넉넉하면 최소 높이를 화면 높이로 잡아 버튼과 고지를 아래에 붙인다.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.bg)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = currentMaxHeight)
                .padding(horizontal = OvalitSpacing.xl),
            // 로고와 문구 덩어리를 버튼 위 공간의 가운데에 둔다. 스크롤 안에서는 weight가 듣지 않아 세 덩어리를
            // SpaceBetween으로 벌린다.
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(OvalitSpacing.xl))

            Column {
                OvalitLogo(modifier = Modifier.width(LogoWidth))

                Spacer(Modifier.height(OvalitSpacing.xl))

                // 로고 ㅇㅂㅇ을 소리 내어 읽어 주는 줄이라 로고 바로 밑에 둔다(CLAUDE.md 용어). 헤드라인과 같은 크기로
                // 둬서 두 줄이 한 덩어리로 읽히게 한다.
                OvalitText(
                    text = dimParentheses(stringResource(Res.string.intro_hook)),
                    style = OvalitTheme.typography.display,
                )

                OvalitText(
                    text = stringResource(Res.string.intro_headline),
                    style = OvalitTheme.typography.display,
                )

                Spacer(Modifier.height(OvalitSpacing.md))

                // "움직였는지"에서 줄을 나눈다. 한 문자열이면 기기 폭에 따라 "짚어드려요"만 다음 줄에 떨어져서 둘로 나눴다.
                // 좁은 화면에서는 줄마다 알아서 꺾인다.
                OvalitText(
                    text = stringResource(Res.string.intro_subtitle_what),
                    style = OvalitTheme.typography.body,
                    color = OvalitTheme.colors.t2,
                )
                OvalitText(
                    text = stringResource(Res.string.intro_subtitle_when),
                    style = OvalitTheme.typography.body,
                    color = OvalitTheme.colors.t2,
                )
            }

            Column {
                Spacer(Modifier.height(OvalitSpacing.xl))

                OvalitPrimaryButton(
                    text = stringResource(Res.string.intro_start),
                    onClick = onStart,
                )

                Spacer(Modifier.height(OvalitSpacing.xl))

                OvalitDisclaimer(modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(OvalitSpacing.md))
            }
        }
    }
}
