package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 원시 hex는 이 파일 밖으로 나가지 않는다. 화면에서는 Color 리터럴 대신 토큰을 쓴다.
@Immutable
data class OvalitColors(
    val bg: Color,
    val raised: Color,
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
    val isDark: Boolean,
)

// 배경이 회색이 아니라 따뜻한 쪽인 것은 의도다. 게임 에셋의 붉은 기와 나란히 놓으면 색이 맞는다.
internal val OvalitDarkColors = OvalitColors(
    bg = Color(0xFF100E0C),
    raised = Color(0xFF1A1714),
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
    accent = Color(0xFFE0B252),
    accentInk = Color(0xFFE0B252),
    onAccent = Color(0xFF100E0C),
    isDark = true,
)

// 다크를 그대로 뒤집은 것이 아니다. pos와 neg를 흰 바탕에 올리면 대비가 2:1 근처까지
// 떨어지므로 둘 다 어둡게 내렸고, accent는 글자로 쓸 수 없어 accentInk를 따로 뒀다.
internal val OvalitLightColors = OvalitColors(
    bg = Color(0xFFFDFCFA),
    raised = Color(0xFFF7F4F0),
    lineWeak = Color(0xFFEFEAE4),
    line = Color(0xFFE5DFD7),
    fill = Color(0xFFF2EDE7),
    bar = Color(0xFFEAE5DE),
    t1 = Color(0xFF191510),
    t2 = Color(0xFF6E675E),
    t3 = Color(0xFF928B82),
    t4 = Color(0xFFACA49A),
    t5 = Color(0xFFC8C1B8),
    pos = Color(0xFF0E8A52),
    neg = Color(0xFFC2262C),
    accent = Color(0xFFE0B252),
    accentInk = Color(0xFF8A6410),
    onAccent = Color(0xFF100E0C),
    isDark = false,
)
