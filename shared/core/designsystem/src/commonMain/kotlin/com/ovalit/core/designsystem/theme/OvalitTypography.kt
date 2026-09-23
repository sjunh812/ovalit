package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

private const val TABULAR_FIGURES = "tnum"

// 그냥 두면 한글이 글자 단위로 잘려서 "공식적으 / 로"처럼 어절 한가운데서 줄이 바뀐다.
// Heading을 깔면 두 가지가 같이 해결된다. 어절 경계에서만 끊고, 첫 줄을 꽉 채우는 대신
// 줄 길이를 고르게 나눠서 마지막 한 단어만 다음 줄에 남는 것도 막는다.
//
// 이름이 Heading이지만 제목 전용은 아니다. 짧은 UI 문구 전반에 맞는 조합이라 본문까지
// 같이 쓴다. 이 앱에는 긴 문단이 없다.
//
// 조합을 직접 지정하는 LineBreak(...) 생성자는 안드로이드에만 있어서 commonMain에서는
// 못 쓴다. 그래서 미리 정의된 값을 가져다 쓴다.
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

    fun text(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        lineBreak = KoreanLineBreak,
        localeList = KoreanLocale,
    )

    return OvalitTypography(
        metricXl = metric(size = 44, lineHeight = 48, tracking = -1.4),
        metricL = metric(size = 30, lineHeight = 34, tracking = -0.8),
        metricM = metric(size = 19, lineHeight = 24, tracking = -0.3),
        titleL = text(size = 24, lineHeight = 32, tracking = -0.6, weight = FontWeight.SemiBold),
        titleM = text(size = 17, lineHeight = 24, tracking = -0.3, weight = FontWeight.SemiBold),
        body = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.Normal),
        bodyStrong = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.SemiBold),
        label = text(size = 13, lineHeight = 18, tracking = 0.0, weight = FontWeight.Medium),
        caption = text(size = 12, lineHeight = 17, tracking = 0.1, weight = FontWeight.Normal),
    )
}
