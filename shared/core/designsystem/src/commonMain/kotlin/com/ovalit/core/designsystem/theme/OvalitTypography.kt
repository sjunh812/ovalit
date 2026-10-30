package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

private const val TABULAR_FIGURES = "tnum"

// 그냥 두면 한글이 "공식적으 / 로"처럼 어절 한가운데서 끊긴다.
// 그래서 줄바꿈 규칙을 둘로 나눠 쓴다.
//
// 제목과 숫자는 Heading이다.
// 어절 경계에서 끊고 줄 길이를 고르게 나눠 마지막 한 단어만 남는 걸 막는다.
// 본문과 설명은 BodyLineBreak다.
// 두세 줄 설명까지 고르게 나누면 "볼 / 수 없어요"처럼 첫 줄이 짧게 갈려서 줄을 끝까지 채우고 어절만 지킨다.
// 이 조합을 고르는 생성자가 안드로이드에만 있어 플랫폼마다 따로 둔다.
private val KoreanLineBreak = LineBreak.Heading

// 위 줄바꿈은 글자의 언어를 알려줘야 동작한다.
// 기기 언어를 따르면 폰을 영어로 쓰는 한국 유저에게서 다시 깨져서 실제로 보이는 문구의 언어(text_locale 리소스)를 받는다.
// 일본어면 한자를 일본 자형으로 그리고 문절 단위로 끊는다.
private val KoreanLocale = LocaleList("ko-KR")

@Immutable
data class OvalitTypography(
    val metricXl: TextStyle,
    val metricL: TextStyle,
    val metricM: TextStyle,
    val metricS: TextStyle,
    /**
     * 변화량("+13", "−0.14")입니다.
     * 숫자 폭은 고정하되 토스처럼 보통 굵기로 둬 옆의 큰 숫자보다 가볍게 읽힙니다.
     * 이 앱이 가장 먼저 전하는 게 무엇이 달라졌는지라 옆의 "평소 138"(caption)보다 한 단계 크게 둡니다.
     */
    val delta: TextStyle,
    val display: TextStyle,
    val titleL: TextStyle,
    /** 홈 기간 이름("이번 주"), 시트 제목, 티어 이름입니다. */
    val titleM: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    /** 카드 제목("달라진 점", "요원")과 시트 안의 질문("어떻게 계산하나요?")입니다. */
    val cardTitle: TextStyle,
    /** 버튼 글자입니다. 제목보다 한 단계 가볍게 SemiBold입니다. */
    val button: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

fun ovalitTypography(fontFamily: FontFamily = FontFamily.Default, locale: LocaleList = KoreanLocale): OvalitTypography {
    fun metric(size: Int, lineHeight: Int, tracking: Double) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        lineBreak = KoreanLineBreak,
        localeList = locale,
    )

    fun text(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight, lineBreak: LineBreak = BodyLineBreak) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
        lineBreak = lineBreak,
        localeList = locale,
    )

    // 크기는 토스 TDS 단계(26, 22, 20, 17, 15, 13)를 따르고 제목은 토스와 당근 SEED처럼 Bold다(docs/design.md 글자).
    return OvalitTypography(
        // 시트와 프로필의 큰 숫자는 그 화면의 주인공이라 Bold다
        metricXl = metric(size = 44, lineHeight = 48, tracking = -1.4).copy(fontWeight = FontWeight.Bold),
        metricL = metric(size = 30, lineHeight = 34, tracking = -0.8).copy(fontWeight = FontWeight.Bold),
        // 홈 고정 칸과 달라진 점 숫자다.
        // 한 화면에 여덟 개가 모여서 Bold면 무겁고 홈 기간 제목(titleM)보다는 커야 한다.
        metricM = metric(size = 22, lineHeight = 26, tracking = -0.5),
        metricS = metric(size = 13, lineHeight = 18, tracking = 0.0),
        // 홈 칸의 숫자 밑 한 줄은 384dp 폰에서 칸이 93dp라 13보다 키우면 한 줄에 들지 않는다
        delta = metric(size = 13, lineHeight = 18, tracking = 0.0).copy(fontWeight = FontWeight.Normal),
        // 화면 제목과 프로필 이름은 titleL이다.
        // 그보다 큰 곳(온보딩 헤드라인, S3 맵 이름, S7 주 역할)만 display를 쓴다.
        display = text(size = 26, lineHeight = 35, tracking = -0.6, weight = FontWeight.Bold, lineBreak = KoreanLineBreak),
        titleL = text(size = 22, lineHeight = 30, tracking = -0.4, weight = FontWeight.Bold, lineBreak = KoreanLineBreak),
        titleM = text(size = 20, lineHeight = 28, tracking = -0.3, weight = FontWeight.Bold, lineBreak = KoreanLineBreak),
        body = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.Normal),
        bodyStrong = text(size = 15, lineHeight = 23, tracking = -0.1, weight = FontWeight.SemiBold),
        cardTitle = text(size = 17, lineHeight = 24, tracking = -0.2, weight = FontWeight.Bold, lineBreak = KoreanLineBreak),
        button = text(size = 17, lineHeight = 24, tracking = -0.2, weight = FontWeight.SemiBold, lineBreak = KoreanLineBreak),
        // 짚을 점 근거 줄처럼 반쪽 폭에 이름과 숫자를 나란히 두는 곳이 있어 13이다
        label = text(size = 13, lineHeight = 18, tracking = 0.0, weight = FontWeight.Medium),
        // 토스는 가장 작은 13을 안 읽어도 되는 글에 쓴다.
        // 이 앱의 작은 글씨(평소 값, 기간, 표본)는 읽어야 하는 글이라 12보다 작게 두지 않는다.
        caption = text(size = 13, lineHeight = 18, tracking = 0.1, weight = FontWeight.Normal),
    )
}
