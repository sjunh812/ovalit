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
    val mvp: Color,
    val mvpContainer: Color,
    val teamMvp: Color,
    val teamMvpContainer: Color,
    val ace: Color,
    val aceContainer: Color,
    val clutch: Color,
    val clutchContainer: Color,
    val isDark: Boolean,
)

// 값마다 고른 이유는 docs/design.md 색 토큰 항목에 있다. 토큰을 바꾸면 그 표도 같은 커밋에서 고친다.
// - card와 canvas를 bg, raised와 따로 둔다. 라이트는 bg가 흰색이라 bg 위에 raised 카드를 올리면 카드가 바탕보다 어둡다.
// - accent는 neg와 거의 같은 빨강이고, 그 위 흰 글자(onAccent) 대비가 3.4:1인 걸 알고 고른 값이다.
// - KDA 구간 색은 오르내림 색(pos, neg)과 거리가 아니라 색상으로 가른다. 초록은 연두 쪽, 빨강은 산호 쪽이다.
internal val OvalitDarkColors = OvalitColors(
    bg = Color(0xFF101012),
    raised = Color(0xFF1C1C1F),
    canvas = Color(0xFF101012),
    card = Color(0xFF1C1C1F),
    lineWeak = Color(0xFF1F1F22),
    line = Color(0xFF26262A),
    fill = Color(0xFF2A2A2E),
    bar = Color(0xFF34343A),
    t1 = Color(0xFFF2F2F4),
    t2 = Color(0xFF9E9EA6),
    t3 = Color(0xFF6E6E76),
    t4 = Color(0xFF58585F),
    t5 = Color(0xFF45454B),
    pos = Color(0xFF3FCF8E),
    neg = Color(0xFFE5484D),
    accent = Color(0xFFFF4655),
    accentInk = Color(0xFFFF4655),
    onAccent = Color(0xFFFFFFFF),
    kda1 = Color(0xFFADDB88),
    kda2 = Color(0xFF7FB9F9),
    kda3 = Color(0xFFF8875A),
    mvp = Color(0xFFF5C04F),
    mvpContainer = Color(0xFF3A3020),
    teamMvp = Color(0xFFC3CDD9),
    teamMvpContainer = Color(0xFF2B3139),
    ace = Color(0xFFFF9147),
    aceContainer = Color(0xFF3D261A),
    clutch = Color(0xFF7EB6FF),
    clutchContainer = Color(0xFF1D2A3D),
    isDark = true,
)

// 라이트는 다크를 뒤집은 값이 아니다. pos, neg, KDA 구간 색은 흰 바탕에서 읽히게 어둡게 내렸고, accent는 글자로 쓰면
// 대비가 모자라 accentInk를 따로 둔다. pos와 t3는 목업보다 한 단계 어둡다. t3를 더 내리면 t2와 거의 같아져서
// raised 위에서는 4.2:1에 머문다.
internal val OvalitLightColors = OvalitColors(
    bg = Color(0xFFFFFFFF),
    raised = Color(0xFFF2F4F6),
    canvas = Color(0xFFF2F4F6),
    card = Color(0xFFFFFFFF),
    lineWeak = Color(0xFFF0F2F4),
    line = Color(0xFFE5E8EB),
    fill = Color(0xFFEBEEF1),
    bar = Color(0xFFE5E8EB),
    t1 = Color(0xFF191F28),
    t2 = Color(0xFF5F6873),
    t3 = Color(0xFF6B7684),
    t4 = Color(0xFFA9B0B8),
    t5 = Color(0xFFC9CED4),
    pos = Color(0xFF0A7D4A),
    neg = Color(0xFFC2262C),
    accent = Color(0xFFFF4655),
    accentInk = Color(0xFFDA1638),
    onAccent = Color(0xFFFFFFFF),
    kda1 = Color(0xFF447924),
    kda2 = Color(0xFF336ABB),
    kda3 = Color(0xFFB34917),
    mvp = Color(0xFF8A5A00),
    mvpContainer = Color(0xFFFFF1CC),
    teamMvp = Color(0xFF3B4A5C),
    teamMvpContainer = Color(0xFFE4E9F0),
    ace = Color(0xFFA84400),
    aceContainer = Color(0xFFFFEADB),
    clutch = Color(0xFF1D5BAA),
    clutchContainer = Color(0xFFE4EEFB),
    isDark = false,
)
