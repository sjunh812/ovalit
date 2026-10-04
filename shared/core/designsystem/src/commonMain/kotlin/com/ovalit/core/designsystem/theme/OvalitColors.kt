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

// 바탕과 글자는 색 기미 없는 회색이다(사용자 결정, 2026-10-03). 금색에 맞춘 따뜻한 베이지는 빨강 액센트와 겉돌았고, 토스처럼
// 무채색을 두니 빨강이 가장 깨끗하게 살았다.
// 카드(card)는 홈, 내 프로필, S5에서 큰 묶음을 담는 면이고 캔버스(canvas)는 그 뒤 바탕이다(사용자 결정, 2026-10-03). 다크는
// 바탕보다 한 단계 밝은 면을, 라이트는 토스처럼 옅은 바탕 위 흰 면을 쓴다. 라이트에서 bg 위에 raised를 올리는 식으로 뒤집지
// 않은 건 bg가 흰색이라 카드가 바탕보다 어두워지기 때문이다.
// MVP 칩은 옅은 금색 면(mvpContainer)에 짙은 금색 글자(mvp)를 올린 톤온톤이다. op.gg처럼 MVP 칩을 다른 칩과 갈라 달라는 요청이었고
// (사용자 요청, 2026-10-04), 빨강은 패배와 겹쳐서 1등 메달처럼 읽히는 금색을 골랐다. 갈색을 옅게 깐 면은 탁했고 금색 면을 그대로
// 깔면 너무 진해서(사용자 요청), 면은 노란 기가 도는 옅은 색으로 따로 정했다. 팀 MVP(teamMvp)는 은메달처럼 푸른 기가 도는 회색,
// 에이스(ace)는 주황, 클러치(clutch)는 파랑 톤온톤이다(사용자 요청, 2026-10-04). 같은 보라로 두니 둘이 갈리지 않았다. 주황은 MVP
// 금색과 빨강 액센트 사이에서 둘 다와 떨어지게 골랐다. 등수는 칩마다 같은 --fill이라 넷과 갈린다.
// 액센트는 발로란트 빨강이다(사용자 결정, 2026-10-03). 패배와 하락을 뜻하는 neg와 OKLab 거리가 5 남짓이라 거의 같은 색으로
// 보인다는 걸 알고 골랐다. 그 위 글자(onAccent)는 발로란트처럼 흰색이고 대비는 3.4:1이다(사용자 결정).
// KDA 구간 색(kda1~3)은 초록, 파랑, 빨강이다(사용자 결정, 2026-10-04). fow.kr와 op.gg의 옛 KDA 색과 같은 순서다. 파랑·보라는
// 둘 다 차가운 색이라 어느 쪽이 높은지 헷갈렸다. 오르내림의 초록·빨강과 섞이지 않게 톤을 낮췄다. 다크는 밝고 옅게, 라이트는
// 짙고 탁하게 두어 pos, neg와 OKLab 11~18 떨어진다. 빨강은 액센트 글자색과 15 넘게 떨어뜨렸다.
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
    kda1 = Color(0xFFB4E3B4),
    kda2 = Color(0xFF8AB5E6),
    kda3 = Color(0xFFF19E9B),
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
    kda1 = Color(0xFF25532E),
    kda2 = Color(0xFF315C92),
    kda3 = Color(0xFF89312F),
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
