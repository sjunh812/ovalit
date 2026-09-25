package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

private const val TABULAR_FIGURES = "tnum"

// 그냥 두면 한글이 글자 단위로 잘려서 "공식적으 / 로"처럼 어절 한가운데서 줄이 바뀐다. 그래서 두 가지를 나눠 쓴다.
//
// 제목과 숫자는 Heading이다. 어절 경계에서만 끊고 줄 길이를 고르게 나눠서, 마지막 한 단어만 다음 줄에 남는 걸
// 막는다. 본문과 설명은 BodyLineBreak다. 두세 줄짜리 설명까지 고르게 나누면 첫 줄이 짧게 끊겨 "볼 / 수 없어요"처럼
// 말이 어색하게 갈리므로, 줄을 끝까지 채우고 어절만 지킨다. 조합을 고르는 생성자가 안드로이드에만 있어서
// 플랫폼마다 따로 정한다.
private val KoreanLineBreak = LineBreak.Heading

// 위 줄바꿈 규칙은 "이 글자는 한국어"라고 알려줘야만 동작한다. 안 알려주면 기기 설정
// 언어를 따라가는데, 폰을 영어로 쓰는 한국 유저가 흔해서 그 경우 다시 깨진다.
// 지금 문구가 전부 한국어라 여기서 못박는다. 다른 언어를 넣을 때 바꾼다.
private val KoreanLocale = LocaleList("ko-KR")

@Immutable
data class OvalitTypography(
    val metricXl: TextStyle,
    val metricL: TextStyle,
    val metricM: TextStyle,
    val metricS: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

fun ovalitTypography(fontFamily: FontFamily = FontFamily.Default): OvalitTypography {
    fun metric(size: Int, lineHeight: Int, tracking: Double) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        lineBreak = KoreanLineBreak,
        localeList = KoreanLocale,
    )

    fun text(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight, lineBreak: LineBreak = BodyLineBreak) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        lineBreak = lineBreak,
        localeList = KoreanLocale,
    )

    return OvalitTypography(
        metricXl = metric(size = 44, lineHeight = 48, tracking = -1.4),
        metricL = metric(size = 30, lineHeight = 34, tracking = -0.8),
        metricM = metric(size = 24, lineHeight = 28, tracking = -0.6),
        metricS = metric(size = 12, lineHeight = 16, tracking = 0.0),
        titleL = text(size = 24, lineHeight = 32, tracking = -0.6, weight = FontWeight.SemiBold, lineBreak = KoreanLineBreak),
        titleM = text(size = 17, lineHeight = 24, tracking = -0.3, weight = FontWeight.SemiBold, lineBreak = KoreanLineBreak),
        body = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.Normal),
        bodyStrong = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.SemiBold),
        label = text(size = 13, lineHeight = 18, tracking = 0.0, weight = FontWeight.Medium),
        caption = text(size = 12, lineHeight = 17, tracking = 0.1, weight = FontWeight.Normal),
    )
}
