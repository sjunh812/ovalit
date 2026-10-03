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

// 바탕과 글자는 색 기미 없는 회색이다(사용자 결정, 2026-10-03). 금색에 맞춘 따뜻한 베이지는 빨강 액센트와 겉돌았고, 토스처럼
// 무채색을 두니 빨강이 가장 깨끗하게 살았다.
// 카드(card)는 홈, 내 프로필, S5에서 큰 묶음을 담는 면이고 캔버스(canvas)는 그 뒤 바탕이다(사용자 결정, 2026-10-03). 다크는
// 바탕보다 한 단계 밝은 면을, 라이트는 토스처럼 옅은 바탕 위 흰 면을 쓴다. 라이트에서 bg 위에 raised를 올리는 식으로 뒤집지
// 않은 건 bg가 흰색이라 카드가 바탕보다 어두워지기 때문이다.
// 액센트는 발로란트 빨강이다(사용자 결정, 2026-10-03). 패배와 하락을 뜻하는 neg와 OKLab 거리가 5 남짓이라 거의 같은 색으로
// 보인다는 걸 알고 골랐다. 그 위 글자(onAccent)는 발로란트처럼 흰색이고 대비는 3.4:1이다(사용자 결정).
// KDA 구간 색(kda1~3)은 파랑, 보라, 호박색이다. op.gg와 tracker.gg처럼 게임 아이템 등급 순서로 올라가되 채도를 낮춰 옆의
// 오르내림 색보다 튀지 않게 했다. 처음 쓴 원색 파랑·보라·주황은 화면에서 혼자 튀어 이질감이 들었다(사용자 요청, 2026-10-03).
// 파랑과 보라는 pos, neg, 액센트, 호박색과 모두 OKLab 15 넘게 떨어진다. 맨 위 칸을 주황이 아니라 호박색으로 둔 건 빨강
// 액센트와 떨어뜨리려는 것이다.
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
    kda1 = Color(0xFF86C5FA),
    kda2 = Color(0xFFBA87D2),
    kda3 = Color(0xFFFCB26F),
    isDark = true,
)

// 다크를 그대로 뒤집은 것이 아니다. pos와 neg를 흰 바탕에 올리면 대비가 2:1 근처까지
// 떨어지므로 둘 다 어둡게 내렸고, accent는 글자로 쓸 수 없어 accentInk를 따로 뒀다. KDA 구간 색도 같은 이유로
// 어둡게 내려 bg와 raised 위에서 4.5:1을 넘긴다. pos와 t3도 작은 글자가 bg 위에서 4.5:1을 넘기게 목업보다 한 단계
// 내렸다. t3를 더 내리면 t2와 거의 같아져 밝기 단계가 흐려져서 raised 위에서는 4.2:1이다.
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
    kda1 = Color(0xFF3573A4),
    kda2 = Color(0xFF683C7B),
    kda3 = Color(0xFF956510),
    isDark = false,
)
