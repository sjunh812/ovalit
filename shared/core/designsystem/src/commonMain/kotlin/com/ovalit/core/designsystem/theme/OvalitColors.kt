package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 원시 hex는 이 파일 밖으로 나가지 않는다. 화면에서는 Color 리터럴 대신 토큰을 쓴다.
@Immutable
data class OvalitColors(
    val bg: Color,
    val raised: Color,
    val canvas: Color,
    val card: Color,
    val lineWeak: Color,
    val line: Color,
    val fill: Color,
    val bar: Color,
    val t1: Color,
    val t2: Color,
    val t3: Color,
    val t4: Color,
    val t5: Color,
    val pos: Color,
    val neg: Color,
    val accent: Color,
    val accentInk: Color,
    val onAccent: Color,
    val kda1: Color,
    val kda2: Color,
    val kda3: Color,
    val isDark: Boolean,
)

// 카드(card)는 홈, 내 프로필, S5에서 큰 묶음을 담는 면이고 캔버스(canvas)는 그 뒤 바탕이다(사용자 결정, 2026-10-03). 다크는
// 바탕보다 한 단계 밝은 면을, 라이트는 토스처럼 옅은 바탕 위 흰 면을 쓴다. 라이트에서 bg 위에 raised를 올리는 식으로 뒤집지
// 않은 건 bg가 이미 흰색에 가까워 카드가 바탕보다 어두워지기 때문이다.
// 액센트는 발로란트 빨강이다(사용자 결정, 2026-10-03). 패배와 하락을 뜻하는 neg와 OKLab 거리가 5 남짓이라 거의 같은 색으로
// 보인다는 걸 알고 골랐다. 그 위 글자(onAccent)는 발로란트처럼 흰색이고 대비는 3.4:1이다(사용자 결정).
// KDA 구간 색(kda1~3)은 파랑, 보라, 주황이다. 게임 아이템 등급처럼 올라갈수록 뜨거운 색으로 읽힌다. 처음 쓴 청록은
// 오르내림의 pos와 OKLab 거리가 10이 안 돼 "지난주보다 올랐다"는 초록으로 보였다(사용자 요청, 2026-09-29). 파랑과 보라는
// pos, neg, 액센트와 모두 15 넘게 떨어진다. 주황은 라이트에서 neg와 8 남짓이라 가깝지만 등급의 맨 위를 알리는 색이라 두었고,
// 옆에 붙는 변화량은 부호와 크기로 갈린다.
internal val OvalitDarkColors = OvalitColors(
    bg = Color(0xFF100E0C),
    raised = Color(0xFF1A1714),
    canvas = Color(0xFF100E0C),
    card = Color(0xFF1A1714),
    lineWeak = Color(0xFF1C1917),
    line = Color(0xFF23201C),
    fill = Color(0xFF2A2621),
    bar = Color(0xFF322D27),
    t1 = Color(0xFFF5F2ED),
    t2 = Color(0xFF9A9289),
    t3 = Color(0xFF6D665E),
    t4 = Color(0xFF57514A),
    t5 = Color(0xFF443F39),
    pos = Color(0xFF3FCF8E),
    neg = Color(0xFFE5484D),
    accent = Color(0xFFFF4655),
    accentInk = Color(0xFFFF4655),
    onAccent = Color(0xFFFFFFFF),
    kda1 = Color(0xFF5B9BFF),
    kda2 = Color(0xFFC772F6),
    kda3 = Color(0xFFFF9433),
    isDark = true,
)

// 다크를 그대로 뒤집은 것이 아니다. pos와 neg를 흰 바탕에 올리면 대비가 2:1 근처까지
// 떨어지므로 둘 다 어둡게 내렸고, accent는 글자로 쓸 수 없어 accentInk를 따로 뒀다. KDA 구간 색도 같은 이유로
// 어둡게 내려 bg와 raised 위에서 4.5:1을 넘긴다. pos와 t3도 작은 글자가 bg 위에서 4.5:1을 넘기게 목업보다 한 단계
// 내렸다. t3를 더 내리면 t2와 거의 같아져 밝기 단계가 흐려진다.
internal val OvalitLightColors = OvalitColors(
    bg = Color(0xFFFDFCFA),
    raised = Color(0xFFF7F4F0),
    canvas = Color(0xFFF7F4F0),
    card = Color(0xFFFFFFFF),
    lineWeak = Color(0xFFEFEAE4),
    line = Color(0xFFE5DFD7),
    fill = Color(0xFFF2EDE7),
    bar = Color(0xFFEAE5DE),
    t1 = Color(0xFF191510),
    t2 = Color(0xFF6E675E),
    t3 = Color(0xFF78726B),
    t4 = Color(0xFFACA49A),
    t5 = Color(0xFFC8C1B8),
    pos = Color(0xFF0A7D4A),
    neg = Color(0xFFC2262C),
    accent = Color(0xFFFF4655),
    accentInk = Color(0xFFDA1638),
    onAccent = Color(0xFFFFFFFF),
    kda1 = Color(0xFF1F63D6),
    kda2 = Color(0xFF8E38BA),
    kda3 = Color(0xFFB85200),
    isDark = false,
)
